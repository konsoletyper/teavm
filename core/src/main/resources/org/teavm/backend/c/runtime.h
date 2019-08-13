#include <stdint.h>
#include <uchar.h>
#include <stdlib.h>
#include <stddef.h>
#include <math.h>

#ifdef TEAVM_USE_SETJMP
#include <setjmp.h>
#endif

#ifdef __GNUC__
#include <stdalign.h>
#include <signal.h>
#endif

#ifdef _MSC_VER
#define alignas(n) __declspec(align(n))
#pragma comment (lib,"uuid.lib")
#pragma warning(disable:4116)
#pragma warning(disable:4102)

#ifdef WINAPI_FAMILY
    #if WINAPI_FAMILY == 2 || WINAPI_FAMILY == 3 || WINAPI_FAMILY == 5
        #define _WINDOWS_UWP 1
    #endif
#endif

#endif

#ifndef TEAVM_MEMORY_TRACE
    #define TEAVM_MEMORY_TRACE 0
#endif

#if TEAVM_MEMORY_TRACE
    #ifndef TEAVM_HEAP_DUMP
        #define TEAVM_HEAP_DUMP 1
    #endif
#endif

#ifndef TEAVM_HEAP_DUMP
    #define TEAVM_HEAP_DUMP 0
#endif

#define TEAVM_FIELD_TYPE_OBJECT 0
#define TEAVM_FIELD_TYPE_ARRAY 1
#define TEAVM_FIELD_TYPE_BOOLEAN 2
#define TEAVM_FIELD_TYPE_BYTE 3
#define TEAVM_FIELD_TYPE_CHAR 4
#define TEAVM_FIELD_TYPE_SHORT 5
#define TEAVM_FIELD_TYPE_INT 6
#define TEAVM_FIELD_TYPE_LONG 7
#define TEAVM_FIELD_TYPE_FLOAT 8
#define TEAVM_FIELD_TYPE_DOUBLE 9

typedef struct {
    uint16_t offset;
    uint8_t type;
    char16_t* name;
} TeaVM_FieldDescriptor;

typedef struct {
    uint32_t count;
    TeaVM_FieldDescriptor data[65536];
} TeaVM_FieldDescriptors;

typedef struct {
    unsigned char* offset;
    uint8_t type;
    char16_t* name;
} TeaVM_StaticFieldDescriptor;

typedef struct {
    uint32_t count;
    TeaVM_StaticFieldDescriptor data[65536];
} TeaVM_StaticFieldDescriptors;

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

#define TEAVM_HASHTABLE_ENTRIES 512

typedef struct TeaVM_HashtableEntry {
    TeaVM_String* data;
    int32_t hash;
    struct TeaVM_HashtableEntry* next;
} TeaVM_HashtableEntry;

typedef struct TeaVM_HashtableEntrySet {
    TeaVM_HashtableEntry data[TEAVM_HASHTABLE_ENTRIES];
    int32_t size;
    struct TeaVM_HashtableEntrySet* next;
} TeaVM_HashtableEntrySet;

extern TeaVM_HashtableEntrySet* teavm_stringHashtableData;

typedef struct {
    TeaVM_String* value;
} TeaVM_StringPtr;

typedef struct TeaVM_MethodLocation {
    TeaVM_String** fileName;
    TeaVM_String** className;
    TeaVM_String** methodName;
} TeaVM_MethodLocation;

typedef struct TeaVM_CallSiteLocation {
    TeaVM_MethodLocation* method;
    int32_t lineNumber;
} TeaVM_CallSiteLocation;

typedef struct TeaVM_ExceptionHandler {
    int32_t id;
    TeaVM_Class* exceptionClass;
    struct TeaVM_ExceptionHandler* next;
} TeaVM_ExceptionHandler;

typedef struct TeaVM_CallSite {
    TeaVM_ExceptionHandler* firstHandler;
    TeaVM_CallSiteLocation* location;
} TeaVM_CallSite;

typedef struct TeaVM_StackFrame {
    struct TeaVM_StackFrame* next;
    #ifdef TEAVM_INCREMENTAL
        TeaVM_CallSite* callSites;
    #endif
    #ifdef TEAVM_USE_SETJMP
        jmp_buf* jmpTarget;
    #endif
    int32_t size;
    int32_t callSiteId;
} TeaVM_StackFrame;

#ifndef TEAVM_INCREMENTAL
    extern TeaVM_CallSite teavm_callSites[];
    #define TEAVM_FIND_CALLSITE(id, frame) (teavm_callSites + id)
#else
    #define TEAVM_FIND_CALLSITE(id, frame) (((TeaVM_StackFrame*) (frame))->callSites + id)
#endif

extern void* teavm_gc_heapAddress;
extern void* teavm_gc_gcStorageAddress;
extern int32_t teavm_gc_gcStorageSize;
extern void* teavm_gc_regionsAddress;
extern int32_t teavm_gc_regionSize;
extern int32_t teavm_gc_regionMaxCount;
extern int64_t teavm_gc_availableBytes;
extern void*** teavm_gc_staticRoots;
extern char* teavm_beforeClasses;

#if TEAVM_MEMORY_TRACE
    extern uint8_t* teavm_gc_heapMap;
    extern uint8_t* teavm_gc_markMap;
#endif

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

    #define TEAVM_ALLOC_STACK_DEF(sz, cs) \
        struct { TeaVM_StackFrame header; void* data[(sz)]; } teavm_shadowStack; \
        teavm_shadowStack.header.next = teavm_stackTop; \
        teavm_shadowStack.header.callSites = (cs); \
        teavm_shadowStack.header.size = (sz); \
        teavm_stackTop = &teavm_shadowStack.header

    #define TEAVM_STACK_HEADER_ADD_SIZE 1

#else

    #define TEAVM_ALLOC_STACK(sz) \
        struct { TeaVM_StackFrame header; void* data[(sz)]; } teavm_shadowStack; \
        teavm_shadowStack.header.next = teavm_stackTop; \
        teavm_shadowStack.header.size = (sz); \
        teavm_stackTop = &teavm_shadowStack.header

    #define TEAVM_STACK_HEADER_ADD_SIZE 0

#endif


#define TEAVM_RELEASE_STACK (teavm_stackTop = teavm_shadowStack.header.next)
#define TEAVM_GC_ROOT(index, ptr) teavm_shadowStack.data[index] = ptr
#define TEAVM_GC_ROOT_RELEASE(index) teavm_shadowStack.data[index] = NULL
#define TEAVM_GC_ROOTS_COUNT(ptr) (((TeaVM_StackFrame*) (ptr))->size);
#define TEAVM_GET_GC_ROOTS(ptr) &((struct { TeaVM_StackFrame header; void* data[1]; }*) (ptr))->data;
#define TEAVM_CALL_SITE(id) (teavm_shadowStack.header.callSiteId = (id))
#define TEAVM_WITH_CALL_SITE_ID(id, expr) (TEAVM_CALL_SITE(id), (expr))
#define TEAVM_EXCEPTION_HANDLER (teavm_shadowStack.header.callSiteId)
#define TEAVM_SET_EXCEPTION_HANDLER(frame, id) (((TeaVM_StackFrame*) (frame))->callSiteId = (id))
#define TEAVM_GET_NEXT_FRAME(frame) (((TeaVM_StackFrame*) (frame))->next)
#define TEAVM_GET_CALL_SITE_ID(frame) (((TeaVM_StackFrame*) (frame))->callSiteId)

#define TEAVM_ADDRESS_ADD(address, offset) ((char *) (address) + (offset))
#define TEAVM_STRUCTURE_ADD(structure, address, offset) (((structure*) (address)) + offset)

#define TEAVM_STRING(length, hash, s) &(TeaVM_String) { \
    .characters = (TeaVM_Array*) & (struct { TeaVM_Array hdr; char16_t data[(length) + 1]; }) { \
        .hdr = { .size = length }, \
        .data = s \
    }, \
    .hashCode = INT32_C(hash) \
}

#define TEAVM_STRING_FROM_CODES(length, hash, ...) &(TeaVM_String) { \
    .characters = (TeaVM_Array*) & (struct { TeaVM_Array hdr; char16_t data[(length) + 1]; }) { \
        .hdr = { .size = length }, \
        .data = { __VA_ARGS__ } \
    }, \
    .hashCode = INT32_C(hash) \
}

extern TeaVM_StackFrame* teavm_stackTop;

extern double teavm_rand();

static inline float teavm_getNaN() {
    return NAN;
}

typedef struct {
    int32_t size;
    void* data[1];
} TeaVM_ResourceArray;

typedef struct {
    TeaVM_String** key;
    void* value;
} TeaVM_ResourceMapEntry;

typedef struct {
    int32_t size;
    TeaVM_ResourceMapEntry entries[1];
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
extern char16_t* teavm_stringToC16(void*);
extern TeaVM_String* teavm_c16ToString(char16_t*);
extern char16_t* teavm_mbToChar16(char*, int32_t*);
extern char* teavm_char16ToMb(char16_t*, int32_t);
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
extern void teavm_printString(char16_t*);
extern void teavm_printInt(int32_t);

extern TeaVM_Array* teavm_parseArguments(int, char**);

extern void teavm_registerStaticGcRoots(void***, int);

extern TeaVM_String* teavm_registerString(TeaVM_String*);

static inline TeaVM_Object* teavm_dereferenceNullable(TeaVM_Object** o) {
    return o != NULL ? *o : NULL;
}

struct TeaVM_ReferenceQueue;

typedef struct TeaVM_Reference {
    TeaVM_Object parent;
    struct TeaVM_ReferenceQueue* queue;
    TeaVM_Object* object;
    struct TeaVM_Reference* next;
} TeaVM_Reference;

typedef struct TeaVM_ReferenceQueue {
    TeaVM_Object parent;
    TeaVM_Reference* first;
    TeaVM_Reference* last;
} TeaVM_ReferenceQueue;

extern int32_t teavm_reference_enqueue(TeaVM_Reference*);

extern int32_t teavm_reference_isEnqueued(TeaVM_Reference*);

extern void teavm_reference_clear(TeaVM_Reference*);

extern TeaVM_Object* teavm_reference_get(TeaVM_Reference*);

extern TeaVM_Reference* teavm_reference_poll(TeaVM_ReferenceQueue*);

extern void teavm_reference_init(TeaVM_Reference*, TeaVM_Object*, TeaVM_ReferenceQueue*);

extern void teavm_date_init();
extern int64_t teavm_date_timeToTimestamp(time_t);
extern time_t teavm_date_timestampToTime(int64_t);
extern int64_t teavm_date_create(int32_t,int32_t,int32_t,int32_t,int32_t,int32_t);
extern int64_t teavm_date_createUtc(int32_t,int32_t,int32_t,int32_t,int32_t,int32_t);
extern int64_t teavm_date_parse(char*);
extern int32_t teavm_date_getYear(int64_t);
extern int64_t teavm_date_setYear(int64_t,int32_t);
extern int32_t teavm_date_getMonth(int64_t);
extern int64_t teavm_date_setMonth(int64_t,int32_t);
extern int32_t teavm_date_getDate(int64_t);
extern int64_t teavm_date_setDate(int64_t,int32_t);
extern int32_t teavm_date_getDay(int64_t);
extern int32_t teavm_date_getHours(int64_t);
extern int64_t teavm_date_setHours(int64_t,int32_t);
extern int32_t teavm_date_getMinutes(int64_t);
extern int64_t teavm_date_setMinutes(int64_t,int32_t);
extern int32_t teavm_date_getSeconds(int64_t);
extern int64_t teavm_date_setSeconds(int64_t,int32_t);
extern char* teavm_date_format(int64_t);

#define TEAVM_SURROGATE_BIT_MASK 0xFC00
#define TEAVM_SURROGATE_INV_BIT_MASK 0x03FF
#define TEAVM_HIGH_SURROGATE_BITS 0xD800
#define TEAVM_LOW_SURROGATE_BITS 0xDC00
#define TEAVM_MIN_SUPPLEMENTARY_CODE_POINT 0x010000

//extern int32_t teavm_utf8_encode(char16_t*, int32_t, char*);
//extern int32_t teavm_utf8_decode(char*, int32_t, char16_t*);

typedef struct TeaVM_StringList {
    char16_t* data;
    int32_t length;
    struct TeaVM_StringList* next;
} TeaVM_StringList;

extern void teavm_disposeStringList(TeaVM_StringList*);
extern TeaVM_StringList* teavm_appendString(TeaVM_StringList*, char16_t*, int32_t);

extern int32_t teavm_file_homeDirectory(char16_t**);
extern int32_t teavm_file_workDirectory(char16_t**);
extern int32_t teavm_file_tempDirectory(char16_t**);
extern int32_t teavm_file_isFile(char16_t*, int32_t);
extern int32_t teavm_file_isDir(char16_t*, int32_t);
extern int32_t teavm_file_canRead(char16_t*, int32_t);
extern int32_t teavm_file_canWrite(char16_t*, int32_t);
extern TeaVM_StringList* teavm_file_listFiles(char16_t*, int32_t);
extern int32_t teavm_file_createDirectory(char16_t*, int32_t);
extern int32_t teavm_file_createFile(char16_t*, int32_t);
extern int32_t teavm_file_delete(char16_t*, int32_t);
extern int32_t teavm_file_rename(char16_t*, int32_t, char16_t*, int32_t);
extern int64_t teavm_file_lastModified(char16_t*, int32_t);
extern int32_t teavm_file_setLastModified(char16_t*, int32_t, int64_t);
extern int32_t teavm_file_length(char16_t*, int32_t);
extern int64_t teavm_file_open(char16_t*, int32_t, int32_t);
extern int32_t teavm_file_close(int64_t);
extern int32_t teavm_file_flush(int64_t);
extern int32_t teavm_file_seek(int64_t, int32_t, int32_t);
extern int32_t teavm_file_tell(int64_t);
extern int32_t teavm_file_read(int64_t, int8_t*, int32_t, int32_t);
extern int32_t teavm_file_write(int64_t, int8_t*, int32_t, int32_t);
extern int32_t teavm_file_isWindows();
extern int32_t teavm_file_canonicalize(char16_t*, int32_t, char16_t**);

#ifdef _MSC_VER
extern int64_t teavm_unixTimeOffset;
#endif

extern void teavm_logchar(int32_t);

#ifdef TEAVM_USE_SETJMP
#define TEAVM_JUMP_SUPPORTED 1
#define TEAVM_TRY \
    do { \
        jmp_buf teavm_tryBuffer; \
        jmp_buf* teavm_oldTryBuffer = teavm_shadowStack.header.jmpTarget; \
        teavm_shadowStack.header.jmpTarget = &teavm_tryBuffer; \
        int teavm_exceptionHandler = setjmp(teavm_tryBuffer); \
        switch (teavm_exceptionHandler) { \
            case 0: {
#define TEAVM_CATCH \
                break; \
            } \
            default: { \
                longjmp(*teavm_oldTryBuffer, teavm_exceptionHandler); \
                break; \
            }
#define TEAVM_END_TRY \
        } \
        teavm_shadowStack.header.jmpTarget = teavm_oldTryBuffer; \
    } while (0);

#define TEAVM_JUMP_TO_FRAME(frame, id) \
    teavm_stackTop = (TeaVM_StackFrame*) (frame); \
    longjmp(*teavm_stackTop->jmpTarget, id)
extern void teavm_throwNullPointerException();
inline static void* teavm_nullCheck(void* o) {
    if (o == NULL) {
        teavm_throwNullPointerException();
        #ifdef __GNUC__
            __builtin_unreachable();
        #endif
        #ifdef _MSC_VER
            __assume(0);
        #endif
    }
    return o;
}

#ifdef __GNUC__
    #define TEAVM_UNREACHABLE __builtin_unreachable();
#endif
#ifdef _MSC_VER
    #define TEAVM_UNREACHABLE __assume(0);
#endif
#ifndef TEAVM_UNREACHABLE
    #define TEAVM_UNREACHABLE return;
#endif

#else
#define TEAVM_JUMP_SUPPORTED 0
#define TEAVM_JUMP_TO_FRAME(frame, id)
#endif

extern void* teavm_catchException();

extern void teavm_gc_allocate(void* address, int32_t size);
extern void teavm_gc_free(void* address, int32_t size);
extern void teavm_gc_assertFree(void* address, int32_t size);
extern void teavm_gc_initMark();
extern void teavm_gc_mark(void* address);
extern void teavm_gc_move(void* from, void* to, int32_t size);
extern void teavm_gc_gcStarted();
extern void teavm_gc_sweepCompleted();
extern void teavm_gc_defragCompleted();
extern void teavm_gc_setDumpDirectory(const wchar_t* path);
extern void teavm_gc_fixHeap();
extern void teavm_gc_writeHeapDump();

extern TeaVM_Class* teavm_classReferences[];
extern TeaVM_Class* teavm_classClass;
extern TeaVM_Class* teavm_objectClass;
extern TeaVM_Class* teavm_stringClass;
extern TeaVM_Class* teavm_charArrayClass;
extern int32_t teavm_classReferencesCount;
extern void teavm_initClasses();