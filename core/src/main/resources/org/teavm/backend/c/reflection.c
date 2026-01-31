#include <stdint.h>
#include "core.h"
#include "reflection.h"
#include "exceptions.h"
#include "arrayclass.h"
#include "reflection_gen.h"

void* teavm_reflection_fieldPtr(void* obj, TeaVM_FieldLocation* location) {
    return location->isStatic ? location->offset.memory : (char*) obj + location->offset.instance;
}

static void teavm_reflection_initField(TeaVM_FieldReaderWriter* field) {
    if (field->initializer != NULL) {
        (*field->initializer)();
    }
}

void* teavm_reflection_readField(void* obj, TeaVM_FieldReaderWriter* field) {
    teavm_reflection_initField(field);
    return teavm_reflection_box(field->valueConv, teavm_reflection_fieldPtr(obj, &field->location));
}

void teavm_reflection_writeField(void* obj, TeaVM_FieldReaderWriter* field, void* value) {
    teavm_reflection_initField(field);
    teavm_reflection_unbox(field->valueConv, teavm_reflection_fieldPtr(obj, &field->location), value);
}

void* teavm_reflection_box(int32_t conv, void* ptr) {
    switch (conv) {
        case TEAVM_VALUE_CONV_REF:
            return *((void**) ptr);

        #ifdef TEAVM_REFLECTION_BOX_BOOLEAN
            case TEAVM_VALUE_CONV_BOOLEAN:
                return (void*) TEAVM_REFLECTION_BOX_BOOLEAN(*((int8_t*) ptr));
        #endif

        #ifdef TEAVM_REFLECTION_BOX_BYTE
            case TEAVM_VALUE_CONV_BYTE:
                return (void*) TEAVM_REFLECTION_BOX_BYTE(*((int8_t*) ptr));
        #endif

        #ifdef TEAVM_REFLECTION_BOX_SHORT
            case TEAVM_VALUE_CONV_SHORT:
                return (void*) TEAVM_REFLECTION_BOX_SHORT(*((int16_t*) ptr));
        #endif

        #ifdef TEAVM_REFLECTION_BOX_CHAR
            case TEAVM_VALUE_CONV_CHAR:
                return (void*) TEAVM_REFLECTION_BOX_CHAR(*((uint16_t*) ptr));
        #endif

        #ifdef TEAVM_REFLECTION_BOX_INT
            case TEAVM_VALUE_CONV_INT:
                return (void*) TEAVM_REFLECTION_BOX_INT(*((int32_t*) ptr));
        #endif

        #ifdef TEAVM_REFLECTION_BOX_LONG
            case TEAVM_VALUE_CONV_LONG:
                return (void*) TEAVM_REFLECTION_BOX_LONG(*((int64_t*) ptr));
        #endif

        #ifdef TEAVM_REFLECTION_BOX_FLOAT
            case TEAVM_VALUE_CONV_FLOAT:
                return (void*) TEAVM_REFLECTION_BOX_FLOAT(*((float*) ptr));
        #endif

        #ifdef TEAVM_REFLECTION_BOX_DOUBLE
            case TEAVM_VALUE_CONV_DOUBLE:
                return (void*) TEAVM_REFLECTION_BOX_DOUBLE(*((double*) ptr));
        #endif

        default:
            TEAVM_UNREACHABLE
    }
}

void teavm_reflection_unbox(int32_t conv, void* ptr, void* value) {
    switch (conv) {
        case TEAVM_VALUE_CONV_REF:
            *((void**) ptr) = value;
            break;

        #ifdef TEAVM_REFLECTION_UNBOX_BOOLEAN
            case TEAVM_VALUE_CONV_BOOLEAN:
                *((int8_t*) ptr) = TEAVM_REFLECTION_UNBOX_BOOLEAN(value);
                break;
        #endif

        #ifdef TEAVM_REFLECTION_UNBOX_BYTE
            case TEAVM_VALUE_CONV_BYTE:
                *((int8_t*) ptr) = (int8_t) TEAVM_REFLECTION_UNBOX_BYTE(value);
                break;
        #endif

        #ifdef TEAVM_REFLECTION_UNBOX_SHORT
            case TEAVM_VALUE_CONV_SHORT:
                *((int16_t*) ptr) = (int16_t) TEAVM_REFLECTION_UNBOX_SHORT(value);
                break;
        #endif

        #ifdef TEAVM_REFLECTION_UNBOX_CHAR
            case TEAVM_VALUE_CONV_CHAR:
                *((uint16_t*) ptr) = (uint16_t) TEAVM_REFLECTION_UNBOX_CHAR(value);
                break;
        #endif

        #ifdef TEAVM_REFLECTION_UNBOX_INT
            case TEAVM_VALUE_CONV_INT:
                *((int32_t*) ptr) = TEAVM_REFLECTION_UNBOX_INT(value);
                break;
        #endif

        #ifdef TEAVM_REFLECTION_UNBOX_LONG
            case TEAVM_VALUE_CONV_LONG:
                *((int64_t*) ptr) = TEAVM_REFLECTION_UNBOX_LONG(value);
                break;
        #endif

        #ifdef TEAVM_REFLECTION_UNBOX_FLOAT
            case TEAVM_VALUE_CONV_FLOAT:
                *((float*) ptr) = TEAVM_REFLECTION_UNBOX_FLOAT(value);
                break;
        #endif

        #ifdef TEAVM_REFLECTION_UNBOX_DOUBLE
            case TEAVM_VALUE_CONV_DOUBLE:
                *((double*) ptr) = TEAVM_REFLECTION_UNBOX_DOUBLE(value);
                break;
        #endif

        default:
            TEAVM_UNREACHABLE
    }
}

TeaVM_Class* teavm_reflection_extractType(TeaVM_ClassPtr* type) {
    TeaVM_Class* cls = type->baseClass;
    for (int32_t i = 0; i < type->arrayDegree; ++i) {
        cls = teavm_getArrayClass(cls);
    }
    return cls;
}