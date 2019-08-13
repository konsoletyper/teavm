#include "runtime.h"
#include <string.h>
#include <stdint.h>
#include <inttypes.h>
#include <stdio.h>
#include <stdlib.h>
#include <wchar.h>
#include <wctype.h>
#include <uchar.h>

#ifndef TEAVM_WINDOWS_LOG
    #define TEAVM_OUTPUT_STRING(s) fprintf(stderr, s)
#else
    #define TEAVM_OUTPUT_STRING(s) OutputDebugStringW(L##s)
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

static FILE* teavm_gc_openDumpFile(wchar_t* name) {
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
    #ifdef _MSC_VER
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
        #ifdef TEAVM_GC_LOG
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
    #if TEAVM_MEMORY_TRACE
        if (teavm_gc_dumpDirectory != NULL) {
            free(teavm_gc_dumpDirectory);
        }
        size_t pathLen = wcslen(path);
        size_t bytesLen = sizeof(wchar_t) * (pathLen + 1);
        teavm_gc_dumpDirectory = malloc(bytesLen);
        memcpy(teavm_gc_dumpDirectory, path, bytesLen);
    #endif
}

#if TEAVM_HEAP_DUMP
    static char* teavm_hexChars = "0123456789abcdef";

    static void teavm_gc_escapeJsonString(FILE* out, char16_t* str) {
        while (1) {
            char16_t c = (char32_t) *str++;
            if (c == 0) {
                break;
            }

            switch (c) {
                case '\n':
                    fputc('\\', out);
                    fputc('n', out);
                    break;
                case '\r':
                    fputc('\\', out);
                    fputc('r', out);
                    break;
                case '\t':
                    fputc('\\', out);
                    fputc('t', out);
                    break;
                case '\b':
                    fputc('\\', out);
                    fputc('b', out);
                    break;
                case '\f':
                    fputc('\\', out);
                    fputc('f', out);
                    break;
                case '\"':
                    fputc('\\', out);
                    fputc('\"', out);
                    break;
                case '\\':
                    fputc('\\', out);
                    fputc('\\', out);
                    break;
                default:
                    if (c < ' ' || c >= 127) {
                        fputc('\\', out);
                        fputc('u', out);
                        fputc(teavm_hexChars[(c >> 12) & 15], out);
                        fputc(teavm_hexChars[(c >> 8) & 15], out);
                        fputc(teavm_hexChars[(c >> 4) & 15], out);
                        fputc(teavm_hexChars[c & 15], out);
                    } else {
                        fputc(c, out);
                    }
                    break;
            }
        }
    }

    static uint8_t teavm_gc_fieldTypeSize(uint8_t type) {
        switch (type) {
            case TEAVM_FIELD_TYPE_OBJECT:
            case TEAVM_FIELD_TYPE_ARRAY:
                return sizeof(void*);
            case TEAVM_FIELD_TYPE_BOOLEAN:
            case TEAVM_FIELD_TYPE_BYTE:
                return 1;
            case TEAVM_FIELD_TYPE_CHAR:
            case TEAVM_FIELD_TYPE_SHORT:
                return 2;
            case TEAVM_FIELD_TYPE_INT:
            case TEAVM_FIELD_TYPE_FLOAT:
                return 4;
            case TEAVM_FIELD_TYPE_LONG:
            case TEAVM_FIELD_TYPE_DOUBLE:
                return 8;
            default:
                return 0;
        }
    }

    static void teavm_gc_writeHeapDumpData(FILE* out, unsigned char* base, uint8_t size) {
        uint64_t value = *(uint64_t*) base;
        size *= 2;
        for (int i = size - 1; i >= 0; --i) {
            int shift = i * 4;
            fputc(teavm_hexChars[(value >> shift) & 15], out);
        }
    }

    static char* teavm_gc_fieldTypeName(uint8_t type) {
        switch (type) {
            case TEAVM_FIELD_TYPE_OBJECT:
                return "object";
            case TEAVM_FIELD_TYPE_ARRAY:
                return "array";
            case TEAVM_FIELD_TYPE_BOOLEAN:
                return "boolean";
            case TEAVM_FIELD_TYPE_BYTE:
                return "byte";
            case TEAVM_FIELD_TYPE_CHAR:
                return "char";
            case TEAVM_FIELD_TYPE_SHORT:
                return "short";
            case TEAVM_FIELD_TYPE_INT:
                return "int";
            case TEAVM_FIELD_TYPE_LONG:
                return "long";
            case TEAVM_FIELD_TYPE_FLOAT:
                return "float";
            case TEAVM_FIELD_TYPE_DOUBLE:
                return "double";
            default:
                return "unknown";
        }
    }

    static char* teavm_gc_primitiveTypeName(int32_t type) {
        switch (type) {
            case 0:
                return "boolean";
            case 1:
                return "byte";
            case 2:
                return "short";
            case 3:
                return "char";
            case 4:
                return "int";
            case 5:
                return "long";
            case 6:
                return "float";
            case 7:
                return "double";
            default:
                return "unknown";
        }
    }

    static void teavm_gc_writeHeapDumpClasses(FILE* out) {
        fprintf(out, "\"classes\":[");
        for (int i = 0; i < teavm_classReferencesCount; ++i) {
            if (i > 0) {
                fprintf(out, ",\n");
            }
            TeaVM_Class* cls = teavm_classReferences[i];
            fprintf(out, "{\"id\":%" PRIuPTR, (uintptr_t) cls);

            if (cls->name != NULL && !(cls->flags & 2) && cls->itemType == NULL) {
                fprintf(out, ",\"name\":");
                char16_t* name = teavm_stringToC16(*cls->name);
                fprintf(out, "\"");
                teavm_gc_escapeJsonString(out, name);
                fprintf(out, "\"");
                free(name);
            }

            if (cls->flags & 2) {
                fprintf(out, ",\"primitive\":\"%s\"", teavm_gc_primitiveTypeName((cls->flags >> 3) & 7));
            } else if (cls->itemType != NULL) {
                fprintf(out, ",\"item\":%" PRIuPTR, (uintptr_t) cls->itemType);
            } else {
                fprintf(out, ",\"size\":%" PRId32, cls->size);
                fprintf(out, ",\"super\":");
                if (cls->superclass != NULL) {
                    fprintf(out, "%" PRIuPTR, (uintptr_t) cls->superclass);
                } else {
                    fprintf(out, "null");
                }

                if (cls->fieldDescriptors != NULL) {
                    fprintf(out, ",\"fields\":[");
                    for (int j = 0; j < cls->fieldDescriptors->count; ++j) {
                        if (j > 0) {
                            fprintf(out, ",");
                        }
                        TeaVM_FieldDescriptor* field = &cls->fieldDescriptors->data[j];
                        fprintf(out, "{\"name\":\"");
                        teavm_gc_escapeJsonString(out, field->name);
                        fprintf(out, "\",\"type\":\"%s\"}", teavm_gc_fieldTypeName(field->type));
                    }
                    fprintf(out, "]");
                }

                if (cls->staticFieldDescriptors != NULL) {
                    fprintf(out, ",\"staticFields\":[");
                    for (int j = 0; j < cls->staticFieldDescriptors->count; ++j) {
                        if (j > 0) {
                            fprintf(out, ",");
                        }
                        TeaVM_StaticFieldDescriptor* field = &cls->staticFieldDescriptors->data[j];
                        fprintf(out, "{\"name\":\"");
                        teavm_gc_escapeJsonString(out, field->name);
                        fprintf(out, "\",\"type\":\"%s\"}", teavm_gc_fieldTypeName(field->type));
                    }
                    fprintf(out, "]");

                    fprintf(out, ",\"data\":\"");
                    for (int j = 0; j < cls->staticFieldDescriptors->count; ++j) {
                        TeaVM_StaticFieldDescriptor* field = &cls->staticFieldDescriptors->data[j];
                        teavm_gc_writeHeapDumpData(out, field->offset, teavm_gc_fieldTypeSize(field->type));
                    }
                    fprintf(out, "\"");
                }
            }

            fprintf(out, "}");
        }
        fprintf(out, "\n]");
    }

    static int teavm_gc_classDepth(TeaVM_Class* cls) {
        int depth = 0;
        while (cls) {
            depth++;
            cls = cls->superclass;
        }
        return depth;
    }

    static void teavm_gc_writeHeapDumpObject(FILE* out, TeaVM_Object* obj) {
        TeaVM_Class* cls = TEAVM_CLASS_OF(obj);
        fprintf(out, "{\"id\":%" PRIuPTR ",\"class\":%" PRIuPTR, (uintptr_t) obj, (uintptr_t) cls);

        if (cls->itemType != NULL) {
            int32_t itemSize;
            if (cls->itemType->flags & 2) {
                itemSize = cls->itemType->size;
            } else {
                itemSize = sizeof(void*);
            }
            char* offset = NULL;
            offset += sizeof(TeaVM_Array);
            offset = TEAVM_ALIGN(offset, itemSize);
            unsigned char* data = (unsigned char*) obj + (uintptr_t) offset;
            int32_t size = ((TeaVM_Array*) obj)->size;

            fprintf(out, ",\"data\":\"");
            int32_t limit = size * itemSize;
            for (int32_t i = 0; i < limit; i += itemSize) {
                teavm_gc_writeHeapDumpData(out, data + i, itemSize);
            }
            fprintf(out, "\"");
        } else {
            fprintf(out, ",\"data\":\"");
            int classDepth = teavm_gc_classDepth(cls);
            TeaVM_Class** classes = malloc(classDepth * sizeof(TeaVM_Class*));
            int i = classDepth;
            while (cls != NULL) {
                classes[--i] = cls;
                cls = cls->superclass;
            }
            for (; i < classDepth; ++i) {
                cls = classes[i];
                if (cls->fieldDescriptors != NULL) {
                    TeaVM_FieldDescriptors* fieldDescriptors = cls->fieldDescriptors;
                    for (int j = 0; j < fieldDescriptors->count; ++j) {
                        TeaVM_FieldDescriptor* field = &fieldDescriptors->data[j];
                        teavm_gc_writeHeapDumpData(out, (unsigned char*) obj + field->offset,
                            teavm_gc_fieldTypeSize(field->type));
                    }
                }
            }
            fprintf(out, "\"");
        }

        fprintf(out, "}");
    }

    static void teavm_gc_writeHeapDumpObjects(FILE* out) {
        fprintf(out, "\"objects\":[");

        int first = 1;
        TeaVM_Object* obj = teavm_gc_heapAddress;
        while ((char*) obj < (char*) teavm_gc_heapAddress + teavm_gc_availableBytes) {
            int32_t size;
            if (obj->header == 0) {
                size = obj->hash;
            } else {
                if (!first) {
                    fprintf(out, ",");
                }
                first = 0;
                fprintf(out, "\n");
                teavm_gc_writeHeapDumpObject(out, obj);
                size = teavm_gc_objectSize(obj);
            }
            obj = (TeaVM_Object*) ((char*) obj + size);
        }

        TeaVM_HashtableEntrySet* strings = teavm_stringHashtableData;
        while (strings != NULL) {
            for (int32_t i = 0; i < strings->size; ++i) {
                TeaVM_String* str = strings->data[i].data;
                if ((char*) str >= (char*) teavm_gc_heapAddress
                        && (char*) str < (char*) teavm_gc_heapAddress + teavm_gc_availableBytes) {
                    break;
                }
                if (!first) {
                    fprintf(out, ",");
                }
                first = 0;
                fprintf(out, "\n");
                teavm_gc_writeHeapDumpObject(out, (TeaVM_Object*) str);
                fprintf(out, ",\n");
                teavm_gc_writeHeapDumpObject(out, (TeaVM_Object*) str->characters);
            }
            strings = strings->next;
        }

        fprintf(out, "\n]");
    }

    static void teavm_gc_writeHeapDumpStack(FILE* out) {
        fprintf(out, "\"stack\":[");

        TeaVM_StackFrame* frame = teavm_stackTop;
        int first = 1;
        while (frame != NULL) {
            if (!first) {
                fprintf(out, ",");
            }
            first = 0;
            fprintf(out, "\n{");

            void** data = &((struct { TeaVM_StackFrame frame; void* data; }*) frame)->data;
            TeaVM_CallSite* callSite = TEAVM_FIND_CALLSITE(frame->callSiteId, frame);

            if (callSite->location != NULL) {
                TeaVM_MethodLocation* method = callSite->location->method;
                if (method != NULL) {
                    if (method->fileName != NULL) {
                        fprintf(out, "\"file\":\"");
                        char16_t* mbName = teavm_stringToC16(*method->fileName);
                        teavm_gc_escapeJsonString(out, mbName);
                        fprintf(out, "\",");
                        free(mbName);
                    }
                    if (method->className != NULL) {
                        fprintf(out, "\"class\":\"");
                        char16_t* mbName = teavm_stringToC16(*method->className);
                        teavm_gc_escapeJsonString(out, mbName);
                        fprintf(out, "\",");
                        free(mbName);
                    }
                    if (method->methodName != NULL) {
                        fprintf(out, "\"method\":\"");
                        char16_t* mbName = teavm_stringToC16(*method->methodName);
                        teavm_gc_escapeJsonString(out, mbName);
                        fprintf(out, "\",");
                        free(mbName);
                    }
                }
                if (callSite->location->lineNumber >= 0) {
                    fprintf(out, "\"line\":%" PRId32 ",", callSite->location->lineNumber);
                }
            }

            fprintf(out, "\"roots\":[");
            int rootsFirst = 1;
            for (int32_t i = 0; i < frame->size; ++i) {
                if (data[i] != NULL) {
                    if (!rootsFirst) {
                        fprintf(out, ",");
                    }
                    rootsFirst = 0;
                    fprintf(out, "%" PRIuPTR, (uintptr_t) data[i]);
                }
            }
            fprintf(out, "]}");
            frame = frame->next;
        }

        fprintf(out, "\n]");
    }

    static void teavm_gc_writeHeapDumpTo(FILE* out) {
        fprintf(out, "{\n");
        fprintf(out, "\"pointerSize\":%u,\n", (unsigned int) sizeof(void*));
        teavm_gc_writeHeapDumpClasses(out);
        fprintf(out, ",\n");
        teavm_gc_writeHeapDumpObjects(out);
        fprintf(out, ",\n");
        teavm_gc_writeHeapDumpStack(out);
        fprintf(out, "\n}");
    }
#endif

void teavm_gc_writeHeapDump() {
    #if TEAVM_HEAP_DUMP
        teavm_gc_fixHeap();
        FILE* out = teavm_gc_openDumpFile(L"teavm-heap-dump.json");
        if (out == NULL) {
            fprintf(stdout, "Error: could not write heap dump");
            return;
        }
        teavm_gc_writeHeapDumpTo(out);
        fclose(out);
    #endif
}
