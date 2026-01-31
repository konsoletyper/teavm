#pragma once
#include <stdint.h>
#include <stdbool.h>
#include "core.h"

#define TEAVM_VALUE_CONV_REF     0
#define TEAVM_VALUE_CONV_BOOLEAN 1
#define TEAVM_VALUE_CONV_BYTE    2
#define TEAVM_VALUE_CONV_CHAR    3
#define TEAVM_VALUE_CONV_SHORT   4
#define TEAVM_VALUE_CONV_INT     5
#define TEAVM_VALUE_CONV_LONG    6
#define TEAVM_VALUE_CONV_FLOAT   7
#define TEAVM_VALUE_CONV_DOUBLE  8

typedef struct {
    bool isStatic;
    union {
        int16_t instance;
        void* memory;
    } offset;
} TeaVM_FieldLocation;

typedef struct {
    TeaVM_FieldLocation location;
    void (*initializer)();
    int32_t valueConv;
} TeaVM_FieldReaderWriter;

typedef struct {
    TeaVM_Class* baseClass;
    int32_t arrayDegree;
} TeaVM_ClassPtr;

typedef struct {
    TeaVM_String** name;
    int32_t modifiers;
    int32_t accessLevel;
    TeaVM_Array* annotations;
    TeaVM_ClassPtr type;
    TeaVM_Object* genericType;
    TeaVM_FieldReaderWriter readerWriter;
} TeaVM_FieldInfo;

typedef struct {
    int32_t count;
    TeaVM_FieldInfo data[0];
} TeaVM_FieldInfoList;

extern void* teavm_reflection_readField(void* obj, TeaVM_FieldReaderWriter* field);
extern void teavm_reflection_writeField(void* obj, TeaVM_FieldReaderWriter* field, void* value);

extern void* teavm_reflection_fieldPtr(void* obj, TeaVM_FieldLocation* location);

extern void* teavm_reflection_box(int32_t conv, void* ptr);

extern void teavm_reflection_unbox(int32_t conv, void* ptr, void* value);

extern TeaVM_Class* teavm_reflection_extractType(TeaVM_ClassPtr* type);