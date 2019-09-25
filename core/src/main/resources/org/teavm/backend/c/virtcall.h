#pragma once
#include "core.h"
#include "definitions.h"
#include <uchar.h>

struct TeaVM_DynamicClass;

typedef struct {
    int32_t capacity;
    int32_t size;
    struct TeaVM_DynamicClass* data[];
} TeaVM_DynamicClassCopy;

typedef struct {
    void* method;
    int32_t id;
    unsigned char hasNext;
    unsigned char permanent;
} TeaVM_DynamicClassEntry;

typedef struct TeaVM_DynamicClass {
    TeaVM_Class parent;
    int32_t capacity;
    int32_t size;
    int32_t threshold;
    TeaVM_DynamicClassEntry* data;
    TeaVM_DynamicClassCopy* copy;
} TeaVM_DynamicClass;

extern int32_t teavm_vc_getMethodId(const char16_t* restrict name);
extern void teavm_vc_registerMethod(TeaVM_DynamicClass* restrict cls, int32_t id, void* method);
extern void* teavm_vc_lookupMethod(const TeaVM_DynamicClass* restrict cls, int32_t id);
extern void teavm_vc_copyMethods(TeaVM_DynamicClass* restrict from, TeaVM_DynamicClass* restrict to);
extern void teavm_vc_done();

#define TEAVM_VC_METHOD(obj, id, returnType, parameters) \
    ((returnType (*)parameters) teavm_vc_lookupMethod((TeaVM_DynamicClass*) TEAVM_CLASS_OF(obj), id))