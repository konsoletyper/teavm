#pragma once
#include <stdint.h>
#include <stdbool.h>
#include "core.h"
#include "core_defs.h"

typedef union {
    int16_t instance;
    void* memory;
} TeaVM_FieldLocation;

typedef void* TeaVM_MethodSignatureConverter(void* fn, TeaVM_Object* instance, TeaVM_Object** params);

typedef struct {
    TeaVM_MethodSignatureConverter* converter;
    bool forceDirect;
    union {
        void* directFnRef;
        int16_t vtOffset;
    } functionRef;
} TeaVM_MethodCaller;

typedef struct {
    TeaVM_Class* baseClass;
    int32_t arrayDegree;
} TeaVM_ClassPtr;

typedef void* TeaVM_AnnotationConstructor(void* data);

typedef struct {
    void* data;
    TeaVM_AnnotationConstructor* constructor;
} TeaVM_AnnotationInfo;

typedef struct {
    int32_t count;
    TeaVM_AnnotationInfo data[0];
} TeaVM_AnnotationInfoList;

#if TEAVM_METHOD_PARAMETER_ANNOTATIONS_USED || TEAVM_METHOD_GENERIC_TYPES_USED
    typedef struct {
        #if TEAVM_METHOD_PARAMETER_ANNOTATIONS_USED
            TeaVM_AnnotationInfoList* annotations;
        #endif
        #if TEAVM_METHOD_GENERIC_TYPES_USED
            struct TeaVM_GenericTypeInfo* genericType;
        #endif
    } TeaVM_ParameterInfo;

    typedef struct {
        int32_t count;
        TeaVM_ParameterInfo data[0];
    } TeaVM_ParameterInfoList;
#endif

#if TEAVM_METHOD_ANNOTATIONS_USED || TEAVM_METHOD_PARAMETER_ANNOTATIONS_USED \
        || TEAVM_METHOD_GENERIC_TYPES_USED || TEAVM_METHOD_TYPE_PARAMS_USED
    typedef struct {
        #if TEAVM_METHOD_GENERIC_TYPES_USED
            struct TeaVM_GenericTypeInfo* genericReturnType;
        #endif
        #if TEAVM_METHOD_ANNOTATIONS_USED
            TeaVM_AnnotationInfoList* annotations;
        #endif
        #if TEAVM_METHOD_PARAMETER_ANNOTATIONS_USED || TEAVM_METHOD_GENERIC_TYPES_USED
            TeaVM_ParameterInfoList* parameterInfos;
        #endif
        #if TEAVM_METHOD_TYPE_PARAMS_USED
            struct TeaVM_TypeVariableInfoList* typeParameters;
        #endif
    } TeaVM_MethodReflectionInfo;
#endif

#if TEAVM_FIELD_ANNOTATIONS_USED || TEAVM_FIELD_GENERIC_TYPE_USED
    typedef struct {
        #if TEAVM_FIELD_ANNOTATIONS_USED
            TeaVM_AnnotationInfoList* annotations;
        #endif
        #if TEAVM_FIELD_GENERIC_TYPE_USED
            struct TeaVM_GenericTypeInfo* genericType;
        #endif
    } TeaVM_FieldReflectionInfo;
#endif

typedef struct {
    TeaVM_String** name;
    int32_t modifiers;
    #if TEAVM_FIELD_ANNOTATIONS_USED || TEAVM_FIELD_GENERIC_TYPE_USED
        TeaVM_FieldReflectionInfo* reflection;
    #endif
    TeaVM_ClassPtr type;
    TeaVM_FieldLocation location;
} TeaVM_FieldInfo;

typedef struct {
    int32_t count;
    TeaVM_FieldInfo data[0];
} TeaVM_FieldInfoList;

typedef struct {
    int32_t count;
    TeaVM_ClassPtr data[0];
} TeaVM_ClassPtrList;

typedef struct {
    int32_t count;
    TeaVM_Class* data[0];
} TeaVM_ClassRefList;

typedef struct {
    TeaVM_String** name;
    int32_t modifiers;
    TeaVM_ClassPtr returnType;
    TeaVM_ClassPtrList* parameterTypes;
    #if TEAVM_METHOD_EXCEPTION_TYPES_USED
        TeaVM_ClassRefList* checkedExceptionTypes;
    #endif
    #if TEAVM_METHOD_ANNOTATIONS_USED || TEAVM_METHOD_PARAMETER_ANNOTATIONS_USED \
            || TEAVM_METHOD_GENERIC_TYPES_USED || TEAVM_METHOD_TYPE_PARAMS_USED
        TeaVM_MethodReflectionInfo* reflection;
    #endif
    TeaVM_MethodCaller* caller;
} TeaVM_MethodInfo;

typedef struct {
    int32_t count;
    TeaVM_MethodInfo data[0];
} TeaVM_MethodInfoList;

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

#if TEAVM_TYPE_VARIABLE_BOUNDS_USED || TEAVM_FIELD_GENERIC_TYPE_USED || TEAVM_METHOD_GENERIC_TYPES_USED
    typedef struct TeaVM_GenericTypeInfo {
        int32_t kind;
        union {
            struct {
                TeaVM_Class* rawType;
                int32_t actualTypeArgumentCount;
                struct TeaVM_GenericTypeInfo** actualTypeArguments;
                struct TeaVM_GenericTypeInfo* ownerType;
            } parameterized;
            struct {
                int16_t level;
                int16_t index;
            } typeVar;
            struct TeaVM_GenericTypeInfo* itemType;
            struct TeaVM_GenericTypeInfo* bound;
            TeaVM_ClassPtr classPtr;
        };
    } TeaVM_GenericTypeInfo;
#endif

#if TEAVM_CLASS_REFLECTION_TYPE_PARAMS_USED || TEAVM_METHOD_TYPE_PARAMS_USED
    typedef struct {
        TeaVM_String** name;
        #if TEAVM_TYPE_VARIABLE_BOUNDS_USED
            int32_t boundCount;
            TeaVM_GenericTypeInfo** bounds;
        #endif
    } TeaVM_TypeVariableInfo;

    typedef struct TeaVM_TypeVariableInfoList {
        int32_t count;
        TeaVM_TypeVariableInfo data[0];
    } TeaVM_TypeVariableInfoList;
#endif

#if TEAVM_CLASS_REFLECTION_FIELDS_USED \
  || TEAVM_CLASS_REFLECTION_ANNOTATIONS_USED \
  || TEAVM_CLASS_REFLECTION_METHODS_USED \
  || TEAVM_CLASS_REFLECTION_TYPE_PARAMS_USED \
  || TEAVM_CLASS_REFLECTION_INNER_CLASSES_USED
typedef struct {
    #if TEAVM_CLASS_REFLECTION_FIELDS_USED
        TeaVM_FieldInfoList* fields;
    #endif
    #if TEAVM_CLASS_REFLECTION_METHODS_USED
        TeaVM_MethodInfoList* methods;
    #endif
    #if TEAVM_CLASS_REFLECTION_ANNOTATIONS_USED
        TeaVM_AnnotationInfoList* annotations;
    #endif
    #if TEAVM_CLASS_REFLECTION_TYPE_PARAMS_USED
        TeaVM_TypeVariableInfoList* typeParameters;
    #endif
    #if TEAVM_CLASS_REFLECTION_INNER_CLASSES_USED
        TeaVM_ClassRefList* innerClasses;
    #endif
} TeaVM_ClassReflection;
#endif

extern void* teavm_reflection_readField(void* obj, TeaVM_FieldInfo* field);
extern int32_t teavm_reflection_readFieldAsByte(void* obj, TeaVM_FieldInfo* field);
extern int32_t teavm_reflection_readFieldAsShort(void* obj, TeaVM_FieldInfo* field);
extern int32_t teavm_reflection_readFieldAsChar(void* obj, TeaVM_FieldInfo* field);
extern int32_t teavm_reflection_readFieldAsInt(void* obj, TeaVM_FieldInfo* field);
extern int64_t teavm_reflection_readFieldAsLong(void* obj, TeaVM_FieldInfo* field);
extern float teavm_reflection_readFieldAsFloat(void* obj, TeaVM_FieldInfo* field);
extern double teavm_reflection_readFieldAsDouble(void* obj, TeaVM_FieldInfo* field);
extern void teavm_reflection_writeField(void* obj, TeaVM_FieldInfo* field, void* value);
extern void teavm_reflection_writeFieldAsByte(void* obj, TeaVM_FieldInfo* field, int32_t value);
extern void teavm_reflection_writeFieldAsShort(void* obj, TeaVM_FieldInfo* field, int32_t value);
extern void teavm_reflection_writeFieldAsChar(void* obj, TeaVM_FieldInfo* field, int32_t value);
extern void teavm_reflection_writeFieldAsInt(void* obj, TeaVM_FieldInfo* field, int32_t value);
extern void teavm_reflection_writeFieldAsLong(void* obj, TeaVM_FieldInfo* field, int64_t value);
extern void teavm_reflection_writeFieldAsFloat(void* obj, TeaVM_FieldInfo* field, float value);
extern void teavm_reflection_writeFieldAsDouble(void* obj, TeaVM_FieldInfo* field, double value);
extern void* teavm_reflection_callMethod(void* method, void* instance, void* args);

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
#ifdef TEAVM_CLASS_REFLECTION_METHODS_USED
    static inline int32_t teavm_reflection_methodCount(TeaVM_ClassReflection* cls) {
        return cls->methods != NULL ? cls->methods->count : 0;
    }
    static inline int32_t teavm_reflection_methodParameterCount(TeaVM_MethodInfo* method) {
        return method->parameterTypes != NULL ? method->parameterTypes->count : 0;
    }
#endif
#ifdef TEAVM_METHOD_EXCEPTION_TYPES_USED
    static inline int32_t teavm_reflection_methodCheckedExceptionCount(TeaVM_MethodInfo* method) {
        return method->checkedExceptionTypes != NULL ? method->checkedExceptionTypes->count : 0;
    }
#endif
#ifdef TEAVM_METHOD_ANNOTATIONS_USED
    static inline int32_t teavm_reflection_methodReflectionAnnotationCount(TeaVM_MethodReflectionInfo* info) {
        return info != NULL && info->annotations != NULL ? info->annotations->count : 0;
    }
#endif
#if TEAVM_METHOD_PARAMETER_ANNOTATIONS_USED || TEAVM_METHOD_GENERIC_TYPES_USED
    static inline int32_t teavm_reflection_methodReflectionParameterInfoCount(TeaVM_MethodReflectionInfo* info) {
        return info != NULL && info->parameterInfos != NULL ? info->parameterInfos->count : 0;
    }
#endif
#ifdef TEAVM_METHOD_PARAMETER_ANNOTATIONS_USED
    static inline int32_t teavm_reflection_parameterAnnotationCount(TeaVM_ParameterInfo* info) {
        return info->annotations != NULL ? info->annotations->count : 0;
    }
#endif
#ifdef TEAVM_METHOD_GENERIC_TYPES_USED
    static inline TeaVM_GenericTypeInfo* teavm_reflection_methodGenericReturnType(TeaVM_MethodReflectionInfo* info) {
        return info != NULL ? info->genericReturnType : NULL;
    }
#endif
#ifdef TEAVM_METHOD_TYPE_PARAMS_USED
    static inline int32_t teavm_reflection_methodTypeParameterCount(TeaVM_MethodReflectionInfo* info) {
        return info != NULL && info->typeParameters != NULL ? info->typeParameters->count : 0;
    }
#endif
#ifdef TEAVM_FIELD_ANNOTATIONS_USED
    static inline int32_t teavm_reflection_fieldReflectionAnnotationCount(TeaVM_FieldReflectionInfo* info) {
        return info != NULL && info->annotations != NULL ? info->annotations->count : 0;
    }
#endif
#ifdef TEAVM_FIELD_GENERIC_TYPE_USED
    static inline TeaVM_GenericTypeInfo* teavm_reflection_fieldGenericType(TeaVM_FieldReflectionInfo* info) {
        return info != NULL ? info->genericType : NULL;
    }
#endif
#ifdef TEAVM_CLASS_REFLECTION_TYPE_PARAMS_USED
    static inline int32_t teavm_reflection_typeParameterCount(TeaVM_ClassReflection* cls) {
        return cls->typeParameters != NULL ? cls->typeParameters->count : 0;
    }
#endif
#if TEAVM_TYPE_VARIABLE_BOUNDS_USED
    static inline int32_t teavm_reflection_typeVariableBoundCount(TeaVM_TypeVariableInfo* param) {
        return param->bounds != NULL ? param->boundCount : 0;
    }
#endif
#if TEAVM_TYPE_VARIABLE_BOUNDS_USED || TEAVM_FIELD_GENERIC_TYPE_USED || TEAVM_METHOD_GENERIC_TYPES_USED
    static inline int32_t teavm_reflection_parameterizedTypeArgumentCount(
            TeaVM_GenericTypeInfo* type) {
        return type->parameterized.actualTypeArguments != NULL
                ? type->parameterized.actualTypeArgumentCount : 0;
    }
#endif
#ifdef TEAVM_CLASS_REFLECTION_INNER_CLASSES_USED
    static inline int32_t teavm_reflection_innerClassCount(TeaVM_ClassReflection* cls) {
        return cls->innerClasses != NULL ? cls->innerClasses->count : 0;
    }
#endif
