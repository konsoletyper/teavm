#include "memory.h"
#include "definitions.h"
#include <stdlib.h>

#if TEAVM_UNIX
    #include <unistd.h>
    #include <sys/mman.h>
#endif

#if TEAVM_WINDOWS
    #include <Windows.h>
#endif

#if TEAVM_MEMORY_TRACE
    #include "heaptrace.h"
#endif

void* teavm_gc_heapAddress = NULL;
void* teavm_gc_gcStorageAddress = NULL;
int32_t teavm_gc_gcStorageSize = INT32_C(0);
void* teavm_gc_regionsAddress = NULL;
int32_t teavm_gc_regionSize = INT32_C(32768);
int32_t teavm_gc_regionMaxCount = INT32_C(0);
int64_t teavm_gc_availableBytes = INT64_C(0);

#if TEAVM_UNIX
    static void* teavm_virtualAlloc(int size) {
        return mmap(NULL, size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, 0, 0);
    }

    static long teavm_pageSize() {
        return sysconf(_SC_PAGE_SIZE);
    }
#endif

#if TEAVM_WINDOWS
    static void* teavm_virtualAlloc(int size) {
        #if TEAVM_WINDOWS_UWP
            return VirtualAllocFromApp(
                    NULL,
                    size,
                    MEM_RESERVE | MEM_COMMIT,
                    PAGE_READWRITE
            );
        #else
            return VirtualAlloc(
                    NULL,
                    size,
                    MEM_RESERVE | MEM_COMMIT,
                    PAGE_READWRITE
            );
        #endif
    }

    static long teavm_pageSize() {
        SYSTEM_INFO systemInfo;
        GetSystemInfo(&systemInfo);
        return systemInfo.dwPageSize;
    }
#endif

static int teavm_pageCount(int64_t size, int64_t pageSize) {
    return (int) ((size + pageSize + 1) / pageSize * pageSize);
}

void teavm_initHeap(int64_t heapSize) {
    long workSize = (long) (heapSize / 16);
    long regionsSize = (long) (heapSize / teavm_gc_regionSize) + 1;
    long pageSize = teavm_pageSize();

    teavm_gc_heapAddress = teavm_virtualAlloc(teavm_pageCount(heapSize, pageSize));
    teavm_gc_gcStorageAddress = teavm_virtualAlloc(teavm_pageCount(workSize, pageSize));
    teavm_gc_regionsAddress = teavm_virtualAlloc(teavm_pageCount(regionsSize * 2, pageSize));

    #if TEAVM_MEMORY_TRACE
        int64_t heapMapSize = heapSize / sizeof(void*);
        teavm_gc_heapMap = teavm_virtualAlloc(teavm_pageCount(heapMapSize, pageSize));
        memset(teavm_gc_heapMap, 0, heapMapSize);
        teavm_gc_markMap = teavm_virtualAlloc(teavm_pageCount(heapMapSize, pageSize));
    #endif

    teavm_gc_gcStorageSize = (int) workSize;
    teavm_gc_regionMaxCount = regionsSize;
    teavm_gc_availableBytes = heapSize;
}

typedef struct TeaVM_StaticGcRootDescriptor {
    void*** roots;
    int count;
} TeaVM_StaticGcRootDescriptor;

typedef struct TeaVM_StaticGcRootDescriptorTable {
    struct TeaVM_StaticGcRootDescriptorTable* next;
    int size;
    TeaVM_StaticGcRootDescriptor data[256];
} TeaVM_StaticGcRootDescriptorTable;

static TeaVM_StaticGcRootDescriptorTable *teavm_staticGcRootsBuilder = NULL;
static int teavm_staticGcRootDataSize = 0;
void*** teavm_gc_staticRoots;


void teavm_registerStaticGcRoots(void*** roots, int count) {
    if (count == 0) {
        return;
    }

    TeaVM_StaticGcRootDescriptorTable* builder = teavm_staticGcRootsBuilder;
    if (builder == NULL || builder->size == 256) {
        builder = malloc(sizeof(TeaVM_StaticGcRootDescriptorTable));
        builder->size = 0;
        builder->next = teavm_staticGcRootsBuilder;
        teavm_staticGcRootsBuilder = builder;
    }

    int i = builder->size++;
    builder->data[i].roots = roots;
    builder->data[i].count = count;
    teavm_staticGcRootDataSize += count;
}

void teavm_initStaticGcRoots() {
    teavm_gc_staticRoots = malloc(sizeof(void**) * (teavm_staticGcRootDataSize + 1));

    void*** target = teavm_gc_staticRoots;
    *target++ = (void**) (intptr_t) teavm_staticGcRootDataSize;
    TeaVM_StaticGcRootDescriptorTable* builder = teavm_staticGcRootsBuilder;

    while (builder != NULL) {
        for (int j = 0; j < builder->size; ++j) {
            int count = builder->data[j].count;
            memcpy(target, builder->data[j].roots, count * sizeof(void**));
            target += count;
        }
        TeaVM_StaticGcRootDescriptorTable* next = builder->next;
        free(builder);
        builder = next;
    }
}