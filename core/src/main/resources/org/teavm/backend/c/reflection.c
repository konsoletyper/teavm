#include <stdint.h>
#include "core.h"
#include "reflection.h"
#include "exceptions.h"
#include "arrayclass.h"
#include "reflection_gen.h"

static inline int32_t teavm_reflection_getFieldPrimitiveType(TeaVM_FieldInfo* field) {
    if (field->type.arrayDegree > 0) {
        return TEAVM_PRIMITIVE_NONE;
    }
    return teavm_primitiveKind(field->type.baseClass);
}

static void* teavm_reflection_fieldPtr(void* obj, TeaVM_FieldInfo* field) {
    return field->modifiers & TEAVM_MODIFIER_STATIC
        ? field->location.memory
        : (char*) obj + field->location.instance;
}

void* teavm_reflection_readField(void* obj, TeaVM_FieldInfo* field) {
    int32_t primitiveType = teavm_reflection_getFieldPrimitiveType(field);
    return teavm_reflection_box(primitiveType, teavm_reflection_fieldPtr(obj, field));
}

void teavm_reflection_writeField(void* obj, TeaVM_FieldInfo* field, void* value) {
    int32_t primitiveType = teavm_reflection_getFieldPrimitiveType(field);
    teavm_reflection_unbox(primitiveType, teavm_reflection_fieldPtr(obj, field), value);
}

void* teavm_reflection_getItem(void* array, int32_t index) {
    TeaVM_Class* cls = TEAVM_CLASS_OF(array);
    int32_t size = cls->itemType->size;
    char* data = TEAVM_ALIGN(((TeaVM_Array*) array) + 1, size);
    return teavm_reflection_box(teavm_primitiveKind(cls->itemType), data + size * index);
}
void teavm_reflection_putItem(void* array, int32_t index, void* item) {
    TeaVM_Class* cls = TEAVM_CLASS_OF(array);
    int32_t size = cls->itemType->size;
    char* data = TEAVM_ALIGN(((TeaVM_Array*) array) + 1, size);
    teavm_reflection_unbox(teavm_primitiveKind(cls->itemType), data + size * index, item);
}

void* teavm_reflection_box(int32_t conv, void* ptr) {
    switch (conv) {
        case TEAVM_PRIMITIVE_NONE:
            return *((void**) ptr);

        #ifdef TEAVM_REFLECTION_BOX_BOOLEAN
            case TEAVM_PRIMITIVE_BOOLEAN:
                return (void*) TEAVM_REFLECTION_BOX_BOOLEAN(*((int8_t*) ptr));
        #endif

        #ifdef TEAVM_REFLECTION_BOX_BYTE
            case TEAVM_PRIMITIVE_BYTE:
                return (void*) TEAVM_REFLECTION_BOX_BYTE(*((int8_t*) ptr));
        #endif

        #ifdef TEAVM_REFLECTION_BOX_SHORT
            case TEAVM_PRIMITIVE_SHORT:
                return (void*) TEAVM_REFLECTION_BOX_SHORT(*((int16_t*) ptr));
        #endif

        #ifdef TEAVM_REFLECTION_BOX_CHAR
            case TEAVM_PRIMITIVE_CHAR:
                return (void*) TEAVM_REFLECTION_BOX_CHAR(*((uint16_t*) ptr));
        #endif

        #ifdef TEAVM_REFLECTION_BOX_INT
            case TEAVM_PRIMITIVE_INT:
                return (void*) TEAVM_REFLECTION_BOX_INT(*((int32_t*) ptr));
        #endif

        #ifdef TEAVM_REFLECTION_BOX_LONG
            case TEAVM_PRIMITIVE_LONG:
                return (void*) TEAVM_REFLECTION_BOX_LONG(*((int64_t*) ptr));
        #endif

        #ifdef TEAVM_REFLECTION_BOX_FLOAT
            case TEAVM_PRIMITIVE_FLOAT:
                return (void*) TEAVM_REFLECTION_BOX_FLOAT(*((float*) ptr));
        #endif

        #ifdef TEAVM_REFLECTION_BOX_DOUBLE
            case TEAVM_PRIMITIVE_DOUBLE:
                return (void*) TEAVM_REFLECTION_BOX_DOUBLE(*((double*) ptr));
        #endif

        default:
            TEAVM_UNREACHABLE
    }
}

void teavm_reflection_unbox(int32_t conv, void* ptr, void* value) {
    switch (conv) {
        case TEAVM_PRIMITIVE_NONE:
            *((void**) ptr) = value;
            break;

        #ifdef TEAVM_REFLECTION_UNBOX_BOOLEAN
            case TEAVM_PRIMITIVE_BOOLEAN:
                *((int8_t*) ptr) = TEAVM_REFLECTION_UNBOX_BOOLEAN(value);
                break;
        #endif

        #ifdef TEAVM_REFLECTION_UNBOX_BYTE
            case TEAVM_PRIMITIVE_BYTE:
                *((int8_t*) ptr) = (int8_t) TEAVM_REFLECTION_UNBOX_BYTE(value);
                break;
        #endif

        #ifdef TEAVM_REFLECTION_UNBOX_SHORT
            case TEAVM_PRIMITIVE_SHORT:
                *((int16_t*) ptr) = (int16_t) TEAVM_REFLECTION_UNBOX_SHORT(value);
                break;
        #endif

        #ifdef TEAVM_REFLECTION_UNBOX_CHAR
            case TEAVM_PRIMITIVE_CHAR:
                *((uint16_t*) ptr) = (uint16_t) TEAVM_REFLECTION_UNBOX_CHAR(value);
                break;
        #endif

        #ifdef TEAVM_REFLECTION_UNBOX_INT
            case TEAVM_PRIMITIVE_INT:
                *((int32_t*) ptr) = TEAVM_REFLECTION_UNBOX_INT(value);
                break;
        #endif

        #ifdef TEAVM_REFLECTION_UNBOX_LONG
            case TEAVM_PRIMITIVE_LONG:
                *((int64_t*) ptr) = TEAVM_REFLECTION_UNBOX_LONG(value);
                break;
        #endif

        #ifdef TEAVM_REFLECTION_UNBOX_FLOAT
            case TEAVM_PRIMITIVE_FLOAT:
                *((float*) ptr) = TEAVM_REFLECTION_UNBOX_FLOAT(value);
                break;
        #endif

        #ifdef TEAVM_REFLECTION_UNBOX_DOUBLE
            case TEAVM_PRIMITIVE_DOUBLE:
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