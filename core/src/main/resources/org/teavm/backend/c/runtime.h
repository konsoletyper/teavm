#include <stdint.h>
#include <uchar.h>
#include <stdlib.h>
#include <stddef.h>
#include <math.h>

#ifdef __GNUC__
#include <stdalign.h>
#include <signal.h>
#endif

#ifdef _MSC_VER
#define alignas(x)
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
    TeaVM_Object* name;
    struct TeaVM_Class* itemType;
    struct TeaVM_Class* arrayType;
    int32_t (*isSupertypeOf)(struct TeaVM_Class*);
    void (*init)();
    struct TeaVM_Class* superclass;
    int32_t superinterfaceCount;
    struct TeaVM_Class** superinterfaces;
    void* enumValues;
    void* layout;
    TeaVM_Object* simpleName;
} TeaVM_Class;

typedef struct TeaVM_String {
    TeaVM_Object parent;
    TeaVM_Array* characters;
    int32_t hashCode;
} TeaVM_String;

extern void* teavm_gc_heapAddress;
extern char *teavm_beforeClasses;

#define TEAVM_PACK_CLASS(cls) ((int32_t) ((uintptr_t) ((char*) (cls) - teavm_beforeClasses) >> 3))
#define TEAVM_UNPACK_CLASS(cls) ((TeaVM_Class*) (teavm_beforeClasses + ((cls) << 3)))
#define TEAVM_CLASS_OF(obj) (TEAVM_UNPACK_CLASS(((TeaVM_Object*) (obj))->header))
#define TEAVM_AS(ptr, type) ((type*) (ptr))

#define TEAVM_VTABLE(obj, type) (TEAVM_AS(TEAVM_CLASS_OF(obj), type))
#define TEAVM_METHOD(obj, type, method) (TEAVM_VTABLE(obj, type)->method)
#define TEAVM_FIELD(ptr, type, name) (TEAVM_AS(ptr, type)->name)

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
    return a > b ? INT32_C(1) : a < b ? INT32_C(-1) : INT32_C(0);
}
static inline int32_t teavm_compare_double(double a, double b) {
    return a > b ? INT32_C(1) : a < b ? INT32_C(-1) : INT32_C(0);
}

#define TEAVM_ALIGN(addr, alignment) ((void*) (((uintptr_t) (addr) + ((alignment) - 1)) / (alignment) * (alignment)))
#define TEAVM_ARRAY_LENGTH(array) (((TeaVM_Array*) (array))->size)
#define TEAVM_ARRAY_DATA(array, type) ((type*) TEAVM_ALIGN((((TeaVM_Array*) (array)) + 1), sizeof(type)))
#define TEAVM_ARRAY_DATAN(array, type) (array != NULL ? TEAVM_ARRAY_DATA(array, type) : NULL)
#define TEAVM_ARRAY_AT(array, type, index) (((type*) TEAVM_ARRAY_DATA(array, type))[index])

extern void* teavm_throwClassCastException();

static inline int32_t teavm_instanceof(void* obj, int32_t (*cls)(TeaVM_Class*)) {
    return obj != NULL && cls(TEAVM_CLASS_OF(obj));
}
static inline void* teavm_checkcast(void* obj, int32_t (*cls)(TeaVM_Class*)) {
    return obj == NULL || cls(TEAVM_CLASS_OF(obj)) ? obj : teavm_throwClassCastException();
}

#ifdef TEAVM_INCREMENTAL

    #define TEAVM_ALLOC_STACK_DEF(size, callSites) \
        void* teavm__shadowStack__[(size) + 4]; \
        teavm__shadowStack__[0] = teavm_stackTop; \
        teavm__shadowStack__[2] = (void*) size; \
        teavm__shadowStack__[3] = (void*) (callSites); \
        teavm_stackTop = teavm__shadowStack__

    #define TEAVM_STACK_HEADER_ADD_SIZE 1

#else

    #define TEAVM_ALLOC_STACK(size) \
        void* teavm__shadowStack__[(size) + 3]; \
        teavm__shadowStack__[0] = teavm_stackTop; \
        teavm__shadowStack__[2] = (void*) size; \
        teavm_stackTop = teavm__shadowStack__

    #define TEAVM_STACK_HEADER_ADD_SIZE 0

#endif


#define TEAVM_RELEASE_STACK teavm_stackTop = teavm__shadowStack__[0]
#define TEAVM_GC_ROOT(index, ptr) teavm__shadowStack__[3 + TEAVM_STACK_HEADER_ADD_SIZE + (index)] = ptr
#define TEAVM_GC_ROOT_RELEASE(index) teavm__shadowStack__[3 + TEAVM_STACK_HEADER_ADD_SIZE + (index)] = NULL
#define TEAVM_GC_ROOTS_COUNT(ptr) ((int32_t) (intptr_t) ((void**) (ptr))[2])
#define TEAVM_GET_GC_ROOTS(ptr) (((void**) (ptr)) + 3 + TEAVM_STACK_HEADER_ADD_SIZE)
#define TEAVM_CALL_SITE(id) (teavm__shadowStack__[1] = (void*) (id))
#define TEAVM_EXCEPTION_HANDLER ((int32_t) (intptr_t) (teavm__shadowStack__[1]))
#define TEAVM_SET_EXCEPTION_HANDLER(frame, id) (((void**) (frame))[1] = (void*) (intptr_t) (id))

#define TEAVM_ADDRESS_ADD(address, offset) ((char *) (address) + (offset))
#define TEAVM_STRUCTURE_ADD(structure, address, offset) (((structure*) (address)) + offset)

#define TEAVM_NULL_STRING { \
    .characters = NULL, \
    .hashCode = 0 \
}

#define TEAVM_STRING(length, hash, s) { \
    .characters = (TeaVM_Array*) & (struct { TeaVM_Array hdr; char16_t data[(length) + 1]; }) { \
        .hdr = { .size = length }, \
        .data = s \
    }, \
    .hashCode = INT32_C(hash) \
}

#define TEAVM_STRING_FROM_CODES(length, hash, ...) { \
    .characters = (TeaVM_Array*) & (struct { TeaVM_Array hdr; char16_t data[(length) + 1]; }) { \
        .hdr = { .size = length }, \
        .data = { __VA_ARGS__ } \
    }, \
    .hashCode = INT32_C(hash) \
}

extern void** teavm_stackTop;

extern void* teavm_gc_gcStorageAddress;
extern int32_t teavm_gc_gcStorageSize;
extern void* teavm_gc_regionsAddress;
extern int32_t teavm_gc_regionSize;
extern int32_t teavm_gc_regionMaxCount;
extern int64_t teavm_gc_availableBytes;
extern void*** teavm_gc_staticRoots;

extern double teavm_rand();

static inline float teavm_getNaN() {
    return NAN;
}

typedef struct {
    int32_t size;
    void* data[0];
} TeaVM_ResourceArray;

typedef struct {
    TeaVM_String* key;
    void* value;
} TeaVM_ResourceMapEntry;

typedef struct {
    int32_t size;
    TeaVM_ResourceMapEntry entries[0];
} TeaVM_ResourceMap;

extern int32_t teavm_hashCode(TeaVM_String*);
extern int32_t teavm_equals(TeaVM_String*, TeaVM_String*);
extern TeaVM_Array* teavm_allocateStringArray(int32_t size);
extern TeaVM_Array* teavm_allocateCharArray(int32_t size);
extern TeaVM_String* teavm_createString(TeaVM_Array* chars);

extern TeaVM_ResourceMapEntry* teavm_lookupResource(TeaVM_ResourceMap *map, TeaVM_String* string);

static inline void* teavm_lookupResourceValue(TeaVM_ResourceMap *map, TeaVM_String* string) {
    TeaVM_ResourceMapEntry *entry = teavm_lookupResource(map, string);
    return entry != NULL ? entry->value : NULL;
}

extern TeaVM_Array* teavm_resourceMapKeys(TeaVM_ResourceMap *);

extern void teavm_beforeInit();
extern void teavm_initHeap(int64_t heapSize);
extern void teavm_afterInitClasses();

extern int64_t teavm_currentTimeMillis();
extern int64_t teavm_currentTimeNano();
extern int32_t teavm_timeZoneOffset();

extern char* teavm_stringToC(void*);
extern TeaVM_String* teavm_cToString(char*);
static inline void teavm_free(void* s) {
    if (s != NULL) {
        free(s);
    }
}

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

extern void teavm_waitFor(int64_t timeout);
extern void teavm_interrupt();

extern void teavm_outOfMemory();
extern void teavm_printString(char* s);
extern void teavm_printInt(int32_t i);

extern TeaVM_Array* teavm_parseArguments(int argc, char** argv);

extern void teavm_registerStaticGcRoots(void***, int count);