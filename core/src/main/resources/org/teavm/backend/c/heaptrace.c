#include "heaptrace.h"
#include "core.h"
#include "definitions.h"
#include "memory.h"
#include <string.h>
#include <stdint.h>
#include <inttypes.h>
#include <stdio.h>
#include <stdlib.h>
#include <wchar.h>
#include <wctype.h>

#if TEAVM_WINDOWS
    #include <Windows.h>
#endif

#if !TEAVM_WINDOWS_LOG
    #define TEAVM_OUTPUT_STRING(s) fprintf(stderr, s)
#else
    #define TEAVM_OUTPUT_STRING(s) OutputDebugStringW(L##s)
#endif

#if TEAVM_MEMORY_TRACE
    uint8_t* teavm_gc_heapMap = NULL;
    uint8_t* teavm_gc_markMap = NULL;
#endif

void teavm_outOfMemory() {
    TEAVM_OUTPUT_STRING("Application crashed due to lack of free memory\n");
    teavm_gc_writeHeapDump();
    abort();
}

static wchar_t* teavm_gc_dumpDirectory = NULL;

#if TEAVM_MEMORY_TRACE
    void teavm_gc_assertSize(int32_t size) {
        if (size % sizeof(void*) != 0) {
            abort();
        }
    }
#endif

void teavm_gc_allocate(void* address, int32_t size) {
    #if TEAVM_MEMORY_TRACE
        teavm_gc_assertAddress(address);
        teavm_gc_assertSize(size);

        size /= sizeof(void*);
        uint8_t* map = teavm_gc_heapMap + (((char*) address - (char*) teavm_gc_heapAddress) / sizeof(void*));

        if (*map != 0) {
            fprintf(stderr, "[GC] trying allocate at memory in use at: %d\n",
                    (int) ((char*) address - (char*) teavm_gc_heapAddress));
            abort();
        }
        *map++ = 1;

        for (int32_t i = 1; i < size; ++i) {
            if (*map != 0) {
                fprintf(stderr, "[GC] trying allocate at memory in use at: %d\n",
                        (int) ((char*) address - (char*) teavm_gc_heapAddress));
                abort();
            }
            *map++ = 2;
        }
    #endif
}

void teavm_gc_free(void* address, int32_t size) {
    #if TEAVM_MEMORY_TRACE
        teavm_gc_assertAddress(address);
        teavm_gc_assertSize(size);

        int32_t offset = (int32_t) (((char*) address - (char*) teavm_gc_heapAddress) / sizeof(void*));
        uint8_t* markMap = teavm_gc_markMap + offset;
        size /= sizeof(void*);
        for (int32_t i = 0; i < size; ++i) {
            if (markMap[i] != 0) {
                fprintf(stderr, "[GC] trying to release reachable object at: %d\n",
                        (int) ((char*) address - (char*) teavm_gc_heapAddress));
                abort();
            }
        }

        uint8_t* map = teavm_gc_heapMap + offset;
        memset(map, 0, size);
    #endif
}

void teavm_gc_assertFree(void* address, int32_t size) {
    #if TEAVM_MEMORY_TRACE
        teavm_gc_assertAddress(address);
        teavm_gc_assertSize(size);

        int32_t offset = (int32_t) (((char*) address - (char*) teavm_gc_heapAddress) / sizeof(void*));
        uint8_t* map = teavm_gc_heapMap + offset;
        size /= sizeof(void*);
        for (int32_t i = 0; i < size; ++i) {
            if (map[i] != 0) {
                fprintf(stderr, "[GC] memory supposed to be free at: %d\n",
                        (int) ((char*) address - (char*) teavm_gc_heapAddress));
                abort();
            }
        }
    #endif
}

void teavm_gc_initMark() {
    #if TEAVM_MEMORY_TRACE
        memset(teavm_gc_markMap, 0, teavm_gc_availableBytes / sizeof(void*));
    #endif
}

int32_t teavm_gc_objectSize(void* address) {
    TeaVM_Class* cls = TEAVM_CLASS_OF(address);
    if (cls->itemType == NULL) {
        return cls->size;
    }

    int32_t itemSize = cls->itemType->flags & 2 ? cls->itemType->size : sizeof(void*);
    TeaVM_Array* array = (TeaVM_Array*) address;
    char* size = TEAVM_ALIGN((void*) sizeof(TeaVM_Array), itemSize);
    size += array->size * itemSize;
    size = TEAVM_ALIGN(size, sizeof(void*));
    return (int32_t) (intptr_t) size;
}

void teavm_gc_mark(void* address) {
    #if TEAVM_MEMORY_TRACE
        if (address < teavm_gc_heapAddress
                || (char*) address >= (char*) teavm_gc_heapAddress + teavm_gc_availableBytes) {
            return;
        }

        teavm_gc_assertAddress(address);

        int32_t offset = (int32_t) (((char*) address - (char*) teavm_gc_heapAddress) / sizeof(void*));
        uint8_t* map = teavm_gc_heapMap + offset;
        uint8_t* markMap = teavm_gc_markMap + offset;

        int32_t size = teavm_gc_objectSize(address);
        teavm_gc_assertSize(size);
        size /= sizeof(void*);

        if (*map++ != 1 || *markMap != 0) {
            fprintf(stderr, "[GC] assertion failed marking object at: %d\n", (int) ((char*) address - (char*) teavm_gc_heapAddress));
            abort();
        }
        *markMap++ = 1;

        for (int32_t i = 1; i < size; ++i) {
            if (*map++ != 2 || *markMap != 0) {
                abort();
            }
            *markMap++ = 1;
        }
    #endif
}

void teavm_gc_move(void* from, void* to, int32_t size) {
    #if TEAVM_MEMORY_TRACE
        teavm_gc_assertAddress(from);
        teavm_gc_assertAddress(to);
        teavm_gc_assertSize(size);

        uint8_t* mapFrom = teavm_gc_heapMap + (((char*) from - (char*) teavm_gc_heapAddress) / sizeof(void*));
        uint8_t* mapTo = teavm_gc_heapMap + (((char*) to - (char*) teavm_gc_heapAddress) / sizeof(void*));
        size /= sizeof(void*);

        if (mapFrom > mapTo) {
            for (int32_t i = 0; i < size; ++i) {
                if (mapFrom[i] == 0 || mapTo[i] != 0) {
                    fprintf(stderr, "[GC] assertion failed moving object from: %d to %d\n",
                        (int) ((char*) from - (char*) teavm_gc_heapAddress), (int) ((char*) to - (char*) teavm_gc_heapAddress));
                    abort();
                }
                mapTo[i] = mapFrom[i];
                mapFrom[i] = 0;
            }
        } else {
            for (int32_t i = size - 1; i >= 0; --i) {
                if (mapFrom[i] == 0 || mapTo[i] != 0) {
                    abort();
                }
                mapTo[i] = mapFrom[i];
                mapFrom[i] = 0;
            }
        }
    #endif
}

static FILE* teavm_gc_traceFile = NULL;

FILE* teavm_gc_openDumpFile(wchar_t* name) {
    wchar_t* fullName = name;
    size_t fullNameLen = wcslen(name);
    if (teavm_gc_dumpDirectory != NULL) {
        size_t prefixLen = wcslen(teavm_gc_dumpDirectory);
        size_t nameLen = fullNameLen;
        fullNameLen = nameLen + prefixLen;
        fullName = malloc((prefixLen + nameLen + 1) * sizeof(wchar_t));
        memcpy(fullName, teavm_gc_dumpDirectory, prefixLen * sizeof(wchar_t));
        memcpy(fullName + prefixLen, name, (nameLen + 1) * sizeof(wchar_t));
    }

    FILE* result;
    #if TEAVM_WINDOWS
        _wfopen_s(&result, fullName, L"w");
    #else
        size_t fullNameMbSize = 3 * (fullNameLen + 1);
        char* fullNameMb = malloc(fullNameMbSize);
        mbstate_t state = { 0 };
        wcsrtombs(fullNameMb, (const wchar_t **) &fullName, fullNameMbSize, &state);
        result = fopen(fullNameMb, "w");
        free(fullNameMb);
    #endif

    if (fullName != name) {
        free(fullName);
    }

    return result;
}

#if TEAVM_MEMORY_TRACE
    static void teavm_writeHeapMemory(char* name) {
        #if TEAVM_GC_LOG
            if (teavm_gc_traceFile == NULL) {
                teavm_gc_traceFile = teavm_gc_openDumpFile(L"teavm-gc-trace.txt");
            }
            FILE* file = teavm_gc_traceFile;
            fprintf(file, "%s:", name);

            int32_t numbers = 4096;
            int64_t mapSize = teavm_gc_availableBytes / sizeof(void*);
            for (int i = 0; i < numbers; ++i) {
                int64_t start = mapSize * i / numbers;
                int64_t end = mapSize * (i + 1) / numbers;
                int count = 0;
                for (int j = start; j < end; ++j) {
                    if (teavm_gc_heapMap[j] != 0) {
                        count++;
                    }
                }
                int rate = count * 4096 / (end - start);
                fprintf(file, " %d", rate);
            }
            fprintf(file, "\n");
            fflush(file);
        #endif
    }

    void teavm_gc_checkHeapConsistency() {
        TeaVM_Object* obj = teavm_gc_heapAddress;
        while ((char*) obj < (char*) teavm_gc_heapAddress + teavm_gc_availableBytes) {
            int32_t size;
            if (obj->header == 0) {
                size = obj->hash;
                teavm_gc_assertFree(obj, size);
            } else {
                teavm_verify(obj);
                TeaVM_Class* cls = TEAVM_CLASS_OF(obj);
                if (cls->itemType != NULL) {
                    if (!(cls->itemType->flags & 2)) {
                        char* offset = NULL;
                        offset += sizeof(TeaVM_Array);
                        offset = TEAVM_ALIGN(offset, sizeof(void*));
                        void** data = (void**)((char*)obj + (uintptr_t)offset);
                        int32_t size = ((TeaVM_Array*)obj)->size;
                        for (int32_t i = 0; i < size; ++i) {
                            teavm_verify(data[i]);
                        }
                    }
                } else {
                    while (cls != NULL) {
                        int32_t kind = (cls->flags >> 7) & 7;
                        if (kind == 1) {

                        } else if (kind == 2) {

                        } else {
                            int16_t* layout = cls->layout;
                            if (layout != NULL) {
                                int16_t size = *layout++;
                                for (int32_t i = 0; i < size; ++i) {
                                    void** ptr = (void**) ((char*) obj + *layout++);
                                    teavm_verify(*ptr);
                                }
                            }
                        }

                        cls = cls->superclass;
                    }
                }
                size = teavm_gc_objectSize(obj);
            }
            obj = (TeaVM_Object*) ((char*) obj + size);
        }
    }
#endif

void teavm_gc_gcStarted() {
    #if TEAVM_MEMORY_TRACE
        teavm_writeHeapMemory("start");
        teavm_gc_checkHeapConsistency();
    #endif
}

void teavm_gc_sweepCompleted() {
    #if TEAVM_MEMORY_TRACE
        teavm_writeHeapMemory("sweep");
        teavm_gc_checkHeapConsistency();
    #endif
}

void teavm_gc_defragCompleted() {
    #if TEAVM_MEMORY_TRACE
        teavm_writeHeapMemory("defrag");
    #endif
}

void teavm_gc_setDumpDirectory(const wchar_t* path) {
    if (teavm_gc_dumpDirectory != NULL) {
        free(teavm_gc_dumpDirectory);
    }
    size_t pathLen = wcslen(path);
    size_t bytesLen = sizeof(wchar_t) * (pathLen + 1);
    teavm_gc_dumpDirectory = malloc(bytesLen);
    memcpy(teavm_gc_dumpDirectory, path, bytesLen);
}

