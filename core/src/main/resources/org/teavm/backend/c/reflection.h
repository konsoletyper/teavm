#pragma once
#include <stdint.h>
#include <stdbool.h>
#include "core.h"
#include "core_defs.h"

typedef union {
    int16_t instance;
    void* memory;
} TeaVM_FieldLocation;

typedef struct {
    TeaVM_Class* baseClass;
    int32_t arrayDegree;
} TeaVM_ClassPtr;

typedef struct {
    TeaVM_String** name;
    int32_t modifiers;
    #if TEAVM_FIELD_ANNOTATIONS_USED
        TeaVM_Array* annotations;
    #endif
    TeaVM_ClassPtr type;
    #if TEAVM_FIELD_GENERIC_TYPE_USED
        TeaVM_Object* genericType;
    #endif
    TeaVM_FieldLocation location;
} TeaVM_FieldInfo;

typedef struct {
    int32_t count;
    TeaVM_FieldInfo data[0];
} TeaVM_FieldInfoList;

typedef void* TeaVM_AnnotationConstructor(void* data);

typedef struct {
    void* data;
    TeaVM_AnnotationConstructor* constructor;
} TeaVM_AnnotationInfo;

typedef struct {
    int32_t count;
    TeaVM_AnnotationInfo data[0];
} TeaVM_AnnotationInfoList;

typedef struct {
    int32_t count;
    int8_t data[];
} TeaVM_ByteArray;

typedef struct {
    int32_t count;
    int16_t data[];
} TeaVM_ShortArray;

typedef struct {
    int32_t count;
    uint16_t data[];
} TeaVM_CharArray;

typedef struct {
    int32_t count;
    int32_t data[];
} TeaVM_IntArray;

typedef struct {
    int32_t count;
    int64_t data[];
} TeaVM_LongArray;

typedef struct {
    int32_t count;
    float data[];
} TeaVM_FloatArray;

typedef struct {
    int32_t count;
    double data[];
} TeaVM_DoubleArray;

typedef struct {
    int32_t count;
    void* data[];
} TeaVM_RefArray;

typedef struct {
    int32_t count;
    TeaVM_ClassPtr data[];
} TeaVM_ClassArray;

#if TEAVM_CLASS_REFLECTION_FIELDS_USED || TEAVM_CLASS_REFLECTION_ANNOTATIONS_USED
typedef struct {
    #if TEAVM_CLASS_REFLECTION_FIELDS_USED
        TeaVM_FieldInfoList* fields;
    #endif
    #if TEAVM_CLASS_REFLECTION_ANNOTATIONS_USED
        TeaVM_AnnotationInfoList* annotations;
    #endif
} TeaVM_ClassReflection;
#endif

extern void* teavm_reflection_readField(void* obj, TeaVM_FieldInfo* field);
extern void teavm_reflection_writeField(void* obj, TeaVM_FieldInfo* field, void* value);

extern void* teavm_reflection_box(int32_t conv, void* ptr);
extern void teavm_reflection_unbox(int32_t conv, void* ptr, void* value);

extern void* teavm_reflection_getItem(void* array, int32_t index);
extern void teavm_reflection_putItem(void* array, int32_t index, void* item);

extern TeaVM_Class* teavm_reflection_extractType(TeaVM_ClassPtr* type);

#ifdef TEAVM_CLASS_REFLECTION_FIELDS_USED
    static inline int32_t teavm_reflection_fieldCount(TeaVM_ClassReflection* cls) {
        return cls->fields != NULL ? cls->fields->count : 0;
    }
#endif
#ifdef TEAVM_CLASS_REFLECTION_ANNOTATIONS_USED
    static inline int32_t teavm_reflection_annotationCount(TeaVM_ClassReflection* cls) {
        return cls->annotations != NULL ? cls->annotations->count : 0;
    }
#endif
