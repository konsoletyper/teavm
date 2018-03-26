#include <string.h>
#include <stdint.h>
#include <stddef.h>
#include <stdio.h>
#include <uchar.h>
#include <unistd.h>
#include <time.h>
#include <math.h>
#include <sys/mman.h>

struct JavaObject;
struct JavaArray;
struct JavaClass;
struct JavaString;
typedef struct JavaObject JavaObject;
typedef struct JavaArray JavaArray;
typedef struct JavaClass JavaClass;
typedef struct JavaString JavaString;

#define PACK_CLASS(cls) ((int32_t) (((uintptr_t) (cls)) >> 3))
#define UNPACK_CLASS(cls) ((JavaClass *) (uintptr_t) (uint32_t) (uintptr_t) (((int64_t*) NULL) + cls))
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

static void** stackTop;

static void* gc_gcStorageAddress = NULL;
static int32_t gc_gcStorageSize = INT32_C(0);
static void* gc_heapAddress = NULL;
static void* gc_regionsAddress = NULL;
static int32_t gc_regionSize = INT32_C(32768);
static int32_t gc_regionMaxCount = INT32_C(0);
static int64_t gc_availableBytes = INT64_C(0);

static void initHeap(long heapSize) {
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