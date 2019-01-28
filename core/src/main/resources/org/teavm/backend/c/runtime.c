#include <string.h>
#include <stdint.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <uchar.h>
#include <wchar.h>
#include <wctype.h>
#include <time.h>
#include <math.h>

#ifdef __GNUC__
#include <stdalign.h>
#include <unistd.h>
#include <sys/mman.h>
#endif

#ifdef _MSC_VER
#define alignas(x)
#include <Windows.h>
#endif

struct JavaObject;
struct JavaArray;
struct JavaClass;
struct JavaString;
typedef struct JavaObject JavaObject;
typedef struct JavaArray JavaArray;
typedef struct JavaClass JavaClass;
typedef struct JavaString JavaString;

#define PACK_CLASS(cls) ((int32_t) ((uintptr_t) ((char*) (cls) - TeaVM_beforeClasses) >> 3))
#define UNPACK_CLASS(cls) ((JavaClass*) (TeaVM_beforeClasses + ((cls) << 3)))
#define CLASS_OF(obj) (UNPACK_CLASS(((JavaObject*) (obj))->header))
#define AS(ptr, type) ((type*) (ptr))

#define VTABLE(obj, type) (AS(CLASS_OF(obj), type))
#define METHOD(obj, type, method) (VTABLE(obj, type)->method)
#define FIELD(ptr, type, name) (AS(ptr, type)->name)

#define TO_BYTE(i) ((((i) << 24) >> 24))
#define TO_SHORT(i) ((((i) << 16) >> 16))
#define TO_CHAR(i) ((char16_t) (i))

static inline int32_t compare_i32(int32_t a, int32_t b) {
    return a > b ? INT32_C(1) : a < b ? INT32_C(-1) : INT32_C(0);
}
static inline int32_t compare_i64(int64_t a, int64_t b) {
    return a > b ? INT32_C(1) : a < b ? INT32_C(-1) : INT32_C(0);
}
static inline int32_t compare_float(float a, float b) {
    return a > b ? INT32_C(1) : a < b ? INT32_C(-1) : INT32_C(0);
}
static inline int32_t compare_double(double a, double b) {
    return a > b ? INT32_C(1) : a < b ? INT32_C(-1) : INT32_C(0);
}

#define ALIGN(addr, alignment) ((void*) (((uintptr_t) (addr) + ((alignment) - 1)) / (alignment) * (alignment)))
#define ARRAY_LENGTH(array) (((JavaArray*) (array))->size)
#define ARRAY_DATA(array, type) ((type*) ALIGN((((JavaArray*) (array)) + 1), sizeof(type)))
#define ARRAY_AT(array, type, index) (((type*) ARRAY_DATA(array, type))[index])

static void* throwClassCastException();
static inline int32_t instanceof(void*, int32_t (*)(JavaClass*));
static inline void* checkcast(void*, int32_t (*)(JavaClass*));

#define ALLOC_STACK(size) \
    void* __shadowStack__[(size) + 3]; \
    __shadowStack__[0] = stackTop; \
    __shadowStack__[2] = (void*) size; \
    stackTop = __shadowStack__

#define RELEASE_STACK stackTop = __shadowStack__[0]
#define GC_ROOT(index, ptr) __shadowStack__[3 + (index)] = ptr
#define GC_ROOT_RELEASE(index) __shadowStack__[3 + (index)] = NULL
#define CALL_SITE(id) (__shadowStack__[1] = (void*) (id))
#define EXCEPTION_HANDLER ((int32_t) (intptr_t) (__shadowStack__[1]))
#define SET_EXCEPTION_HANDLER(frame, id) (((void**) (frame))[1] = (void*) (intptr_t) (id))

#define ADDRESS_ADD(address, offset) ((char *) (address) + (offset))
#define STRUCTURE_ADD(structure, address, offset) (((structure*) (address)) + offset)

#define TEAVM_STRING(length, hash, s) { \
    .characters = (JavaArray*) & (struct { JavaArray hdr; char16_t data[(length) + 1]; }) { \
        .hdr = { .size = length }, \
        .data = s \
    }, \
    .hashCode = INT32_C(hash) \
}

#define TEAVM_STRING_FROM_CODES(length, hash, ...) { \
    .characters = (JavaArray*) & (struct { JavaArray hdr; char16_t data[(length) + 1]; }) { \
        .hdr = { .size = length }, \
        .data = { __VA_ARGS__ } \
    }, \
    .hashCode = INT32_C(hash) \
}

static void** stackTop;

static void* gc_gcStorageAddress = NULL;
static int32_t gc_gcStorageSize = INT32_C(0);
static void* gc_heapAddress = NULL;
static void* gc_regionsAddress = NULL;
static int32_t gc_regionSize = INT32_C(32768);
static int32_t gc_regionMaxCount = INT32_C(0);
static int64_t gc_availableBytes = INT64_C(0);

static char *TeaVM_beforeClasses;

static double TeaVM_rand() {
    return rand() / ((double) RAND_MAX + 1);
}

static inline float TeaVM_getNaN() {
    return NAN;
}

typedef struct {
    int32_t size;
    void* data[0];
} TeaVM_ResourceArray;

typedef struct {
    JavaString* key;
    void* value;
} TeaVM_ResourceMapEntry;

typedef struct {
    int32_t size;
    TeaVM_ResourceMapEntry entries[0];
} TeaVM_ResourceMap;

static int32_t teavm_hashCode(JavaString*);
static int32_t teavm_equals(JavaString*, JavaString*);
static JavaArray* teavm_allocateStringArray(int32_t size);

static TeaVM_ResourceMapEntry* teavm_lookupResource(TeaVM_ResourceMap *map, JavaString* string) {
    uint32_t hashCode = teavm_hashCode(string);
    for (int32_t i = 0; i < map->size; ++i) {
        uint32_t index = (hashCode + i) % map->size;
        if (map->entries[index].key == NULL) {
            return NULL;
        }
        if (teavm_equals(map->entries[index].key, string)) {
            return &map->entries[index];
        }
    }
    return NULL;
}

static inline void* teavm_lookupResourceValue(TeaVM_ResourceMap *map, JavaString* string) {
    TeaVM_ResourceMapEntry *entry = teavm_lookupResource(map, string);
    return entry != NULL ? entry->value : NULL;
}

static JavaArray* teavm_resourceMapKeys(TeaVM_ResourceMap *);

static void TeaVM_beforeInit() {
    srand(time(NULL));
}

#ifdef __GNUC__
static void initHeap(int64_t heapSize) {
    long workSize = heapSize / 16;
    long regionsSize = (long) (heapSize / gc_regionSize);

    long pageSize = sysconf(_SC_PAGE_SIZE);
    int heapPages = (int) ((heapSize + pageSize + 1) / pageSize * pageSize);
    int workPages = (int) ((workSize + pageSize + 1) / pageSize * pageSize);
    int regionsPages = (int) ((regionsSize * 2 + pageSize + 1) / pageSize * pageSize);

    gc_heapAddress = mmap(
            NULL,
            heapPages,
            PROT_READ | PROT_WRITE,
            MAP_PRIVATE | MAP_ANONYMOUS,
            0, 0);
    gc_gcStorageAddress = mmap(
            NULL,
            workPages,
            PROT_READ | PROT_WRITE,
            MAP_PRIVATE | MAP_ANONYMOUS,
            0, 0);
    gc_regionsAddress = mmap(
            NULL,
            regionsPages,
            PROT_READ | PROT_WRITE,
            MAP_PRIVATE | MAP_ANONYMOUS,
            0, 0);

    gc_gcStorageSize = (int) workSize;
    gc_regionMaxCount = regionsSize;
    gc_availableBytes = heapSize;
}

static int64_t currentTimeMillis() {
    struct timespec time;
    clock_gettime(CLOCK_REALTIME, &time);

    return time.tv_sec * 1000 + (int64_t) round(time.tv_nsec / 1000000);
}
#endif

#ifdef _MSC_VER
static void initHeap(int64_t heapSize) {
    long workSize = heapSize / 16;
    long regionsSize = (long) (heapSize / gc_regionSize);

    SYSTEM_INFO systemInfo;
    GetSystemInfo(&systemInfo);
    long pageSize = systemInfo.dwPageSize;
    int heapPages = (int) ((heapSize + pageSize + 1) / pageSize * pageSize);
    int workPages = (int) ((workSize + pageSize + 1) / pageSize * pageSize);
    int regionsPages = (int) ((regionsSize * 2 + pageSize + 1) / pageSize * pageSize);

    gc_heapAddress = VirtualAlloc(
            NULL,
            heapPages,
            MEM_RESERVE | MEM_COMMIT,
            PAGE_READWRITE
    );
    gc_gcStorageAddress = VirtualAlloc(
            NULL,
            workPages,
            MEM_RESERVE | MEM_COMMIT,
            PAGE_READWRITE
    );
    gc_regionsAddress = VirtualAlloc(
            NULL,
            regionsPages,
            MEM_RESERVE | MEM_COMMIT,
            PAGE_READWRITE
    );

    gc_gcStorageSize = (int) workSize;
    gc_regionMaxCount = regionsSize;
    gc_availableBytes = heapSize;
}

static SYSTEMTIME unixEpochStart = {
    .wYear = 1970,
    .wMonth = 1,
    .wDayOfWeek = 3,
    .wDay = 1,
    .wHour = 0,
    .wMinute = 0,
    .wSecond = 0,
    .wMilliseconds = 0
};

static int64_t currentTimeMillis() {
    SYSTEMTIME time;
    FILETIME fileTime;
    GetSystemTime(&time);
    SystemTimeToFileTime(&time, &fileTime);

    FILETIME fileTimeStart;
    SystemTimeToFileTime(&unixEpochStart, &fileTimeStart);

    uint64_t current = fileTime.dwLowDateTime | ((uint64_t) fileTime.dwHighDateTime << 32);
    uint64_t start = fileTimeStart.dwLowDateTime | ((uint64_t) fileTimeStart.dwHighDateTime << 32);

    return (int64_t) ((current - start) / 10000);
}
#endif

static int32_t teavm_timeZoneOffset() {
    time_t t = time(NULL);
    time_t local = mktime(localtime(&t));
    time_t utc = mktime(gmtime(&t));
    return difftime(utc, local) / 60;
}

static char* teavm_stringToC(void*);
static inline void teavm_free(void*);

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