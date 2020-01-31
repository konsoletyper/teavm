#include "heaptrace.h"
#include "core.h"
#include "log.h"
#include "definitions.h"
#include "memory.h"
#include "time.h"
#include "references.h"
#include <string.h>
#include <stdint.h>
#include <inttypes.h>
#include <stdio.h>
#include <stdlib.h>
#include <wchar.h>
#include <wctype.h>
#include <stdbool.h>

#define TEAVM_GC_LOG_BUFFER_SIZE 512

#if TEAVM_WINDOWS
    #include <Windows.h>
#endif

#if TEAVM_MEMORY_TRACE
    uint8_t* teavm_gc_heapMap = NULL;
    uint8_t* teavm_gc_markMap = NULL;
#endif

static inline void teavm_gc_print(wchar_t* s) {
    teavm_printWString(s);
}

void teavm_outOfMemory() {
    teavm_gc_print(L"Application crashed due to lack of free memory\n");
    teavm_gc_writeHeapDump();
    abort();
}

static wchar_t* teavm_gc_dumpDirectory = NULL;

#ifdef TEAVM_GC_STATS
    static int32_t teavm_gc_allocationCount = 0;
    static int32_t teavm_gc_freeCount = 0;
    static int32_t teavm_gc_freeByteCount = 0;
    static int32_t teavm_gc_markCount = 0;
    static int32_t teavm_gc_dirtyRegionCount = 0;
    static int32_t teavm_gc_relocatedBlocks = 0;
    static int32_t teavm_gc_relocatedBytes = 0;

    static int64_t teavm_gc_startTimeMillis;
    static int64_t teavm_gc_startTime;
    static int64_t teavm_gc_endTime;
    static int64_t teavm_gc_markStartTime;
    static int64_t teavm_gc_markEndTime;
    static int64_t teavm_gc_sweepStartTime;
    static int64_t teavm_gc_sweepEndTime;
    static int64_t teavm_gc_defragStartTime;
    static int64_t teavm_gc_defragEndTime;
    static bool teavm_gc_full;
#endif

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
            wchar_t buffer[TEAVM_GC_LOG_BUFFER_SIZE];
            swprintf(buffer, TEAVM_GC_LOG_BUFFER_SIZE, L"[GC] trying allocate at memory in use at: %d\n",
                    (int) ((char*) address - (char*) teavm_gc_heapAddress));
            teavm_gc_print(buffer);
            abort();
        }
        *map++ = 1;

        for (int32_t i = 1; i < size; ++i) {
            if (*map != 0) {
                wchar_t buffer[TEAVM_GC_LOG_BUFFER_SIZE];
                swprintf(buffer, TEAVM_GC_LOG_BUFFER_SIZE, L"[GC] trying allocate at memory in use at: %d\n",
                        (int) ((char*) address - (char*) teavm_gc_heapAddress));
                teavm_gc_print(buffer);
                abort();
            }
            *map++ = 2;
        }
    #endif

    #if TEAVM_GC_STATS
        teavm_gc_allocationCount++;
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
                wchar_t buffer[TEAVM_GC_LOG_BUFFER_SIZE];
                swprintf(buffer, TEAVM_GC_LOG_BUFFER_SIZE, L"[GC] trying to release reachable object at: %d\n",
                        (int) ((char*) address - (char*) teavm_gc_heapAddress));
                teavm_gc_print(buffer);
                abort();
            }
        }

        uint8_t* map = teavm_gc_heapMap + offset;
        memset(map, 0, size);
    #endif

    #if TEAVM_GC_STATS
        teavm_gc_freeCount++;
        teavm_gc_freeByteCount += size;
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
                wchar_t buffer[TEAVM_GC_LOG_BUFFER_SIZE];
                swprintf(buffer, TEAVM_GC_LOG_BUFFER_SIZE, L"[GC] memory supposed to be free at: %d\n",
                        (int) ((char*) address - (char*) teavm_gc_heapAddress));
                teavm_gc_print(buffer);
                abort();
            }
        }
    #endif
}

void teavm_gc_markStarted() {
    #if TEAVM_MEMORY_TRACE
        memset(teavm_gc_markMap, 0, teavm_gc_availableBytes / sizeof(void*));
    #endif

    #if TEAVM_GC_STATS
        teavm_gc_markStartTime = teavm_currentTimeNano();
    #endif
}

void teavm_gc_markCompleted() {
    #if TEAVM_GC_STATS
        teavm_gc_markEndTime = teavm_currentTimeNano();
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
            wchar_t buffer[TEAVM_GC_LOG_BUFFER_SIZE];
            swprintf(buffer, TEAVM_GC_LOG_BUFFER_SIZE, L"[GC] assertion failed marking object at: %d\n",
                    (int) ((char*) address - (char*) teavm_gc_heapAddress));
            teavm_gc_print(buffer);
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

    #if TEAVM_GC_STATS
        teavm_gc_markCount++;
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
                    wchar_t buffer[TEAVM_GC_LOG_BUFFER_SIZE];
                    swprintf(buffer, TEAVM_GC_LOG_BUFFER_SIZE,
                            L"[GC] assertion failed moving object from: %d to %d\n",
                            (int) ((char*) from - (char*) teavm_gc_heapAddress),
                            (int) ((char*) to - (char*) teavm_gc_heapAddress));
                    teavm_gc_print(buffer);
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

    #if TEAVM_GC_STATS
        teavm_gc_relocatedBlocks++;
        teavm_gc_relocatedBytes += size;
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

    void teavm_gc_checkHeapConsistency(bool oldGen, bool offsets) {
        int32_t lastCheckedRegion = -1;
        TeaVM_Object* obj = teavm_gc_heapAddress;
        uint16_t* regions = (uint16_t*) teavm_gc_regionsAddress;
        while ((char*) obj < (char*) teavm_gc_heapAddress + teavm_gc_availableBytes) {
            int32_t size;
            if (obj->header == 0) {
                size = obj->hash;
                teavm_gc_assertFree(obj, size);
            } else {
                teavm_verify(obj);
                if (offsets) {
                    int64_t offset = (int64_t) ((char*) obj - (char*) teavm_gc_heapAddress);
                    int32_t objRegion = (int32_t) (offset / teavm_gc_regionSize);
                    if (objRegion != lastCheckedRegion) {
                        while (++lastCheckedRegion < objRegion) {
                            if (regions[lastCheckedRegion] != 0) {
                                abort();
                            }
                        }
                        int32_t offsetInRegion = (int32_t) (offset % teavm_gc_regionSize);
                        if (regions[objRegion] != offsetInRegion + 1) {
                            abort();
                        }
                    }
                }
                if (oldGen && !(obj->header & 0x40000000)) {
                    abort();
                }
                TeaVM_Class* cls = TEAVM_CLASS_OF(obj);
                if (cls->itemType != NULL) {
                    if (!(cls->itemType->flags & 2)) {
                        char* offset = NULL;
                        offset += sizeof(TeaVM_Array);
                        offset = TEAVM_ALIGN(offset, sizeof(void*));
                        void** data = (void**) ((char*) obj + (uintptr_t) offset);
                        int32_t size = ((TeaVM_Array*) obj)->size;
                        for (int32_t i = 0; i < size; ++i) {
                            teavm_verify(data[i]);
                        }
                    }
                } else {
                    while (cls != NULL) {
                        int32_t kind = (cls->flags >> 7) & 7;
                        if (kind == 1) {
                            TeaVM_Reference* reference = (TeaVM_Reference*) obj;
                            teavm_verify(reference->next);
                            teavm_verify(reference->object);
                            teavm_verify(reference->queue);
                        } else if (kind == 2) {
                            TeaVM_ReferenceQueue* queue = (TeaVM_ReferenceQueue*) obj;
                            teavm_verify(queue->first);
                            teavm_verify(queue->last);
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

        if (offsets) {
            int32_t lastRegion = (int32_t) (teavm_gc_availableBytes / teavm_gc_regionSize);
            while (++lastCheckedRegion <= lastRegion) {
                if (regions[lastCheckedRegion] != 0) {
                    abort();
                }
            }
        }
    }
#endif

void teavm_gc_gcStarted(int32_t full) {
    #if TEAVM_MEMORY_TRACE
        teavm_writeHeapMemory("start");
        teavm_gc_checkHeapConsistency(false, false);
    #endif

    #if TEAVM_GC_STATS
        teavm_gc_startTime = teavm_currentTimeNano();
        teavm_gc_startTimeMillis = teavm_currentTimeMillis();
        teavm_gc_full = full;
    #endif
}

void teavm_gc_sweepStarted() {
    #if TEAVM_GC_STATS
        teavm_gc_sweepStartTime = teavm_currentTimeNano();
    #endif
}

void teavm_gc_sweepCompleted() {
    #if TEAVM_MEMORY_TRACE
        teavm_writeHeapMemory("sweep");
        teavm_gc_checkHeapConsistency(false, true);
    #endif

    #if TEAVM_GC_STATS
        teavm_gc_sweepEndTime = teavm_currentTimeNano();
    #endif
}

void teavm_gc_defragStarted() {
    #if TEAVM_GC_STATS
        teavm_gc_defragStartTime = teavm_currentTimeNano();
    #endif
}

void teavm_gc_defragCompleted() {
    #if TEAVM_MEMORY_TRACE
        teavm_writeHeapMemory("defrag");
        teavm_gc_checkHeapConsistency(true, true);
    #endif

    #if TEAVM_GC_STATS
        teavm_gc_defragEndTime = teavm_currentTimeNano();
    #endif
}

#if TEAVM_GC_STATS
    static void teavm_gc_printStats() {
        wchar_t buffer[TEAVM_GC_LOG_BUFFER_SIZE];

        swprintf(buffer, TEAVM_GC_LOG_BUFFER_SIZE, L"[GC] Garbage collection (%ls) performed at %" PRIu64 ", took %"
                PRIu64 " ns\n", teavm_gc_full ? L"full" : L"young", teavm_gc_startTimeMillis,
                 teavm_gc_endTime - teavm_gc_startTime);
        teavm_gc_print(buffer);

        swprintf(buffer, TEAVM_GC_LOG_BUFFER_SIZE, L"[GC]   Allocations performed before GC: %" PRIu32 "\n",
                teavm_gc_allocationCount);
        teavm_gc_print(buffer);

        swprintf(buffer, TEAVM_GC_LOG_BUFFER_SIZE, L"[GC]   Mark phase took %" PRIu64 " ns, %" PRIu32
                " objects reached\n", teavm_gc_markEndTime - teavm_gc_markStartTime, teavm_gc_markCount);
        teavm_gc_print(buffer);

        if (!teavm_gc_full) {
            swprintf(buffer, TEAVM_GC_LOG_BUFFER_SIZE, L"[GC]     Regions scanned from remembered set: %" PRIu32 "\n",
                    teavm_gc_dirtyRegionCount);
            teavm_gc_print(buffer);
        }

        swprintf(buffer, TEAVM_GC_LOG_BUFFER_SIZE, L"[GC]   Sweep phase took %" PRIu64 " ns, %" PRIu32 " regions of %"
                PRIu32 " bytes freed\n", teavm_gc_sweepEndTime - teavm_gc_sweepStartTime, teavm_gc_freeCount,
                teavm_gc_freeByteCount);
        teavm_gc_print(buffer);

        swprintf(buffer, TEAVM_GC_LOG_BUFFER_SIZE, L"[GC]   Defrag phase took %" PRIu64 " ns\n",
                teavm_gc_defragEndTime - teavm_gc_defragStartTime);
        teavm_gc_print(buffer);

        swprintf(buffer, TEAVM_GC_LOG_BUFFER_SIZE, L"[GC]     Blocks relocated %" PRId32 " of total %" PRId32 " bytes\n",
            teavm_gc_relocatedBlocks, teavm_gc_relocatedBytes);
        teavm_gc_print(buffer);
    }

    static void teavm_gc_resetStats() {
        teavm_gc_allocationCount = 0;
        teavm_gc_markCount = 0;
        teavm_gc_dirtyRegionCount = 0;
        teavm_gc_freeCount = 0;
        teavm_gc_freeByteCount = 0;
        teavm_gc_relocatedBlocks = 0;
        teavm_gc_relocatedBytes = 0;
    }
#endif

void teavm_gc_gcCompleted() {
    #if TEAVM_GC_STATS
        teavm_gc_endTime = teavm_currentTimeNano();
        teavm_gc_printStats();
        teavm_gc_resetStats();
    #endif
}

void teavm_gc_heapResized(int64_t newSize) {
    #if TEAVM_GC_STATS
        wchar_t buffer[TEAVM_GC_LOG_BUFFER_SIZE];

        swprintf(buffer, TEAVM_GC_LOG_BUFFER_SIZE, L"[GC] Heap resized to %" PRIu64 " bytes\n", newSize);
        teavm_gc_print(buffer);
    #endif
}

void teavm_gc_reportDirtyRegion(void* address) {
    #if TEAVM_GC_STATS
        teavm_gc_dirtyRegionCount++;
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
