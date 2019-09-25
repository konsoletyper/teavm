#include "heapdump.h"
#include <stdio.h>
#include <uchar.h>
#include <stdint.h>
#include <inttypes.h>
#include <stdlib.h>
#include "core.h"
#include "memory.h"
#include "heaptrace.h"
#include "stack.h"
#include "stringhash.h"

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
                    for (unsigned j = 0; j < cls->fieldDescriptors->count; ++j) {
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
                    for (unsigned j = 0; j < cls->staticFieldDescriptors->count; ++j) {
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
                    for (unsigned j = 0; j < cls->staticFieldDescriptors->count; ++j) {
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
                    for (unsigned j = 0; j < fieldDescriptors->count; ++j) {
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
