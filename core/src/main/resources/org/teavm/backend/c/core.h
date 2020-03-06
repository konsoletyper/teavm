#pragma once
#include <stdint.h>
#include <uchar.h>
#include <math.h>
#include "definitions.h"
#include "heapdump.h"
#include "memory.h"
#include <stdlib.h>

#if TEAVM_MEMORY_TRACE
    #include "heaptrace.h"
    #include <stdlib.h>
#endif

typedef struct TeaVM_Object {
    int32_t header;
    int32_t hash;
} TeaVM_Object;

typedef struct TeaVM_Array {
    TeaVM_Object parent;
    int32_t size;
} TeaVM_Array;

typedef struct TeaVM_Class {
    TeaVM_Object parent;
    int32_t size;
    int32_t flags;
    int32_t tag;
    int32_t canary;
    TeaVM_Object** name;
    TeaVM_Object* nameCache;
    struct TeaVM_Class* itemType;
    struct TeaVM_Class* arrayType;
    struct TeaVM_Class* declaringClass;
    struct TeaVM_Class* enclosingClass;
    int32_t (*isSupertypeOf)(struct TeaVM_Class*);
    void (*init)();
    struct TeaVM_Class* superclass;
    int32_t superinterfaceCount;
    struct TeaVM_Class** superinterfaces;
    void* enumValues;
    void* layout;
    TeaVM_Object** simpleName;
    TeaVM_Object* simpleNameCache;
    TeaVM_Object* canonicalName;
    #if TEAVM_HEAP_DUMP
        TeaVM_FieldDescriptors* fieldDescriptors;
        TeaVM_StaticFieldDescriptors* staticFieldDescriptors;
    #endif
} TeaVM_Class;

typedef struct TeaVM_String {
    TeaVM_Object parent;
    TeaVM_Array* characters;
    int32_t hashCode;
} TeaVM_String;

extern char* teavm_beforeClasses;

extern void* teavm_throwClassCastException();
extern void teavm_throwNullPointerException();
extern void teavm_throwArrayIndexOutOfBoundsException();

#define TEAVM_PACK_CLASS(cls) ((int32_t) ((uintptr_t) ((char*) (cls) - teavm_beforeClasses) >> 3))
#define TEAVM_UNPACK_CLASS(cls) ((TeaVM_Class*) (teavm_beforeClasses + ((cls) << 3)))
#define TEAVM_CLASS_OF(obj) (TEAVM_UNPACK_CLASS(((TeaVM_Object*) (obj))->header))
#define TEAVM_AS(ptr, type) ((type*) (ptr))

#if TEAVM_MEMORY_TRACE
    static inline void teavm_gc_assertAddress(void* address) {
        if ((unsigned int) (uintptr_t) address % sizeof(void*) != 0) {
            abort();
        }
    }

    static inline void* teavm_verify(void* address) {
        if (address >= teavm_gc_heapAddress
                && (char*) address < (char*) teavm_gc_heapAddress + teavm_gc_availableBytes) {
            teavm_gc_assertAddress(address);
            uint8_t* map = teavm_gc_heapMap + (((char*) address - (char*) teavm_gc_heapAddress) / sizeof(void*));
            if (*map != 1) {
                abort();
            }
        }

        return address;
    }

    #define TEAVM_VERIFY(ptr) teavm_verify(ptr)
#else
    #define TEAVM_VERIFY(ptr) ptr
#endif

#define TEAVM_VTABLE(obj, type) (TEAVM_AS(TEAVM_CLASS_OF(obj), type))
#define TEAVM_METHOD(obj, type, method) (TEAVM_VTABLE(obj, type)->method)
#define TEAVM_FIELD(ptr, type, name) (TEAVM_AS(ptr, type)->name)
#define TEAVM_ALIGN(addr, alignment) ((void*) (((uintptr_t) (addr) + ((alignment) - 1)) / (alignment) * (alignment)))
#define TEAVM_ARRAY_LENGTH(array) (((TeaVM_Array*) (array))->size)
#define TEAVM_ARRAY_DATA(array, type) ((type*) TEAVM_ALIGN((((TeaVM_Array*) (array)) + 1), sizeof(type)))
#define TEAVM_ARRAY_DATAN(array, type) (array != NULL ? TEAVM_ARRAY_DATA(array, type) : NULL)
#define TEAVM_ARRAY_AT(array, type, index) (((type*) TEAVM_ARRAY_DATA(array, type))[index])
#define TEAVM_ADDRESS_ADD(address, offset) ((char *) (address) + (offset))
#define TEAVM_STRUCTURE_ADD(structure, address, offset) (((structure*) (address)) + offset)

#define TEAVM_TO_BYTE(i) ((((i) << 24) >> 24))
#define TEAVM_TO_SHORT(i) ((((i) << 16) >> 16))
#define TEAVM_TO_CHAR(i) ((char16_t) (i))

#define TEAVM_PACK_MONITOR(ref) (((int32_t) ((uintptr_t) (ref) - (uintptr_t) teavm_gc_heapAddress) / sizeof(int)) \
    | 0x80000000)
#define TEAVM_UNPACK_MONITOR(ref) ((ref & 0x80000000) != 0 \
    ? (void*) (((uintptr_t) ((ref & 0x7FFFFFFF) * sizeof(int)) + (uintptr_t) teavm_gc_heapAddress)) \
    : NULL)

static inline int32_t teavm_compare_i32(int32_t a, int32_t b) {
    return a > b ? INT32_C(1) : a < b ? INT32_C(-1) : INT32_C(0);
}
static inline int32_t teavm_compare_i64(int64_t a, int64_t b) {
    return a > b ? INT32_C(1) : a < b ? INT32_C(-1) : INT32_C(0);
}
static inline int32_t teavm_compare_float(float a, float b) {
    return a > b ? INT32_C(1) : a < b ? INT32_C(-1) : a == b ? INT32_C(0) : INT32_C(1);
}
static inline int32_t teavm_compare_double(double a, double b) {
    return a > b ? INT32_C(1) : a < b ? INT32_C(-1) : a == b ? INT32_C(0) : INT32_C(1);
}

static inline int32_t teavm_instanceof(void* obj, int32_t (*cls)(TeaVM_Class*)) {
    return obj != NULL && cls(TEAVM_CLASS_OF(obj));
}
static inline void* teavm_checkcast(void* obj, int32_t (*cls)(TeaVM_Class*)) {
    return obj == NULL || cls(TEAVM_CLASS_OF(obj)) ? obj : teavm_throwClassCastException();
}

extern double teavm_rand();

static inline float teavm_getNaN() {
    return NAN;
}

extern void teavm_beforeInit();
extern void teavm_afterInitClasses();


static inline int64_t teavm_reinterpretDoubleToLong(double v) {
    union { int64_t longValue; double doubleValue; } conv;
    conv.doubleValue = v;
    return conv.longValue;
}
static inline double teavm_reinterpretLongToDouble(int64_t v) {
    union { int64_t longValue; double doubleValue; } conv;
    conv.longValue = v;
    return conv.doubleValue;
}
static inline int32_t teavm_reinterpretFloatToInt(float v) {
    union { int32_t intValue; float floatValue; } conv;
    conv.floatValue = v;
    return conv.intValue;
}
static inline float teavm_reinterpretIntToFloat(int32_t v) {
    union { int32_t intValue; float floatValue; } conv;
    conv.intValue = v;
    return conv.floatValue;
}

extern void teavm_outOfMemory();

extern TeaVM_Array* teavm_parseArguments(int, char**);

static inline TeaVM_Object* teavm_dereferenceNullable(TeaVM_Object** o) {
    return o != NULL ? *o : NULL;
}


extern TeaVM_Class* teavm_classReferences[];
extern TeaVM_Class* teavm_classClass;
extern TeaVM_Class* teavm_objectClass;
extern TeaVM_Class* teavm_stringClass;
extern TeaVM_Class* teavm_charArrayClass;
extern int32_t teavm_classReferencesCount;
extern void teavm_initClasses();


inline static void teavm_gc_writeBarrier(void* object) {
    intptr_t offset = (intptr_t) ((char*) object - (char*) teavm_gc_heapAddress) / teavm_gc_regionSize;
    ((char*) teavm_gc_cardTable)[offset] = 0;
}

extern void* teavm_fillArray(void* array, ...);
extern void* teavm_fillBooleanArray(void* array, ...);
extern void* teavm_fillByteArray(void* array, ...);
extern void* teavm_fillShortArray(void* array, ...);
extern void* teavm_fillCharArray(void* array, ...);
extern void* teavm_fillIntArray(void* array, ...);
extern void* teavm_fillLongArray(void* array, ...);
extern void* teavm_fillFloatArray(void* array, ...);
extern void* teavm_fillDoubleArray(void* array, ...);
