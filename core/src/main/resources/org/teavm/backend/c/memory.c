#include "memory.h"
#include "heaptrace.h"
#include "definitions.h"
#include <stdlib.h>
#include <string.h>

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
void* teavm_gc_cardTable = NULL;
int32_t teavm_gc_regionMaxCount;
int64_t teavm_gc_availableBytes;
int64_t teavm_gc_minAvailableBytes;
int64_t teavm_gc_maxAvailableBytes;
static int64_t teavm_gc_pageSize;

#if TEAVM_UNIX
    static void* teavm_virtualAlloc(int64_t size) {
        return mmap(NULL, size, PROT_NONE, MAP_PRIVATE | MAP_ANONYMOUS, 0, 0);
    }

    static void teavm_virtualCommit(void* address, int64_t size) {
        mprotect(address, size, PROT_READ | PROT_WRITE);
    }

    static void teavm_virtualUncommit(void* address, int64_t size) {
        mprotect(address, size, PROT_NONE);
    }

    static int64_t teavm_pageSize() {
        return sysconf(_SC_PAGE_SIZE);
    }
#endif

#if TEAVM_WINDOWS
    static void* teavm_virtualAlloc(int64_t size) {
        #if TEAVM_WINDOWS_UWP
            return VirtualAllocFromApp(
                    NULL,
                    size,
                    MEM_RESERVE,
                    PAGE_NOACCESS
            );
        #else
            return VirtualAlloc(
                    NULL,
                    size,
                    MEM_RESERVE,
                    PAGE_NOACCESS
            );
        #endif
    }

    static void teavm_virtualCommit(void* address, int64_t size) {
        #if TEAVM_WINDOWS_UWP
            VirtualAllocFromApp(
                    address,
                    size,
                    MEM_COMMIT,
                    PAGE_READWRITE
            );
        #else
            VirtualAlloc(
                    address,
                    size,
                    MEM_COMMIT,
                    PAGE_READWRITE
            );
        #endif
    }

    static void teavm_virtualUncommit(void* address, int64_t size) {
        VirtualFree(address, size, MEM_DECOMMIT);
    }

    static int64_t teavm_pageSize() {
        SYSTEM_INFO systemInfo;
        GetSystemInfo(&systemInfo);
        return systemInfo.dwPageSize;
    }
#endif

static int64_t teavm_pageCount(int64_t size) {
    return (int64_t) ((size + teavm_gc_pageSize - 1) / teavm_gc_pageSize * teavm_gc_pageSize);
}

static int32_t teavm_gc_calculateWorkSize(int64_t heapSize) {
    return (int32_t) (heapSize / 16);
}

static int32_t teavm_gc_calculateRegionsSize(int64_t heapSize) {
    return (int32_t) (heapSize / teavm_gc_regionSize) + 1;
}

void teavm_gc_resizeHeap(int64_t newSize) {
    if (newSize == teavm_gc_availableBytes) {
        return;
    }

    teavm_gc_heapResized(newSize);

    int32_t workSize = teavm_gc_calculateWorkSize(newSize);
    int32_t regionsSize = teavm_gc_calculateRegionsSize(newSize);

    int64_t newSizeAligned = teavm_pageCount(newSize);
    int64_t oldSizeAligned = teavm_pageCount(teavm_gc_availableBytes);
    int64_t newWorkSizeAligned = teavm_pageCount(workSize);
    int64_t oldWorkSizeAligned = teavm_pageCount(teavm_gc_gcStorageSize);
    int64_t newRegionsSizeAligned = teavm_pageCount(regionsSize * 2);
    int64_t oldRegionsSizeAligned = teavm_pageCount(teavm_gc_regionMaxCount * 2);
    int64_t newCardTableSizeAligned = teavm_pageCount(regionsSize);
    int64_t oldCardTableSizeAligned = teavm_pageCount(teavm_gc_regionMaxCount);

    if (newSize > teavm_gc_availableBytes) {
        if (newSizeAligned > oldSizeAligned) {
            teavm_virtualCommit((char*) teavm_gc_heapAddress + oldSizeAligned, newSizeAligned - oldSizeAligned);
        }
        if (newWorkSizeAligned > oldWorkSizeAligned) {
            teavm_virtualCommit((char*) teavm_gc_gcStorageAddress + oldWorkSizeAligned,
                    newWorkSizeAligned - oldWorkSizeAligned);
        }
        if (newRegionsSizeAligned > oldRegionsSizeAligned) {
            teavm_virtualCommit((char*) teavm_gc_regionsAddress + oldRegionsSizeAligned,
                newRegionsSizeAligned - oldRegionsSizeAligned);
        }
        if (newCardTableSizeAligned > oldCardTableSizeAligned) {
            teavm_virtualCommit((char*) teavm_gc_cardTable + oldCardTableSizeAligned,
                newCardTableSizeAligned - oldCardTableSizeAligned);
        }
    } else {
        if (newSizeAligned < oldSizeAligned) {
            teavm_virtualUncommit((char*) teavm_gc_heapAddress + newSizeAligned, oldSizeAligned - newSizeAligned);
        }
        if (newWorkSizeAligned < oldWorkSizeAligned) {
            teavm_virtualUncommit((char*) teavm_gc_gcStorageAddress + newWorkSizeAligned,
                    oldWorkSizeAligned - newWorkSizeAligned);
        }
        if (newRegionsSizeAligned < oldRegionsSizeAligned) {
            teavm_virtualUncommit((char*) teavm_gc_regionsAddress + newRegionsSizeAligned,
                oldRegionsSizeAligned - newRegionsSizeAligned);
        }
        if (newCardTableSizeAligned < oldCardTableSizeAligned) {
            teavm_virtualUncommit((char*) teavm_gc_cardTable + newCardTableSizeAligned,
                oldCardTableSizeAligned - newCardTableSizeAligned);
        }
    }

    teavm_gc_gcStorageSize = workSize;
    teavm_gc_regionMaxCount = regionsSize;
    teavm_gc_availableBytes = newSize;
}

void teavm_initHeap(int64_t minHeap, int64_t maxHeap) {
    teavm_gc_pageSize = teavm_pageSize();
    int32_t workSize = teavm_gc_calculateWorkSize(maxHeap);
    int32_t regionsSize = teavm_gc_calculateRegionsSize(maxHeap);

    teavm_gc_heapAddress = teavm_virtualAlloc(teavm_pageCount(maxHeap));
    teavm_gc_gcStorageAddress = teavm_virtualAlloc(teavm_pageCount(workSize));
    teavm_gc_regionsAddress = teavm_virtualAlloc(teavm_pageCount(regionsSize * 2));
    teavm_gc_cardTable = teavm_virtualAlloc(teavm_pageCount(regionsSize));

    #if TEAVM_MEMORY_TRACE
        int64_t heapMapSize = maxHeap / sizeof(void*);
        teavm_gc_heapMap = teavm_virtualAlloc(teavm_pageCount(heapMapSize));
        teavm_virtualCommit(teavm_gc_heapMap, teavm_pageCount(heapMapSize));
        memset(teavm_gc_heapMap, 0, heapMapSize);
        teavm_gc_markMap = teavm_virtualAlloc(teavm_pageCount(heapMapSize));
        teavm_virtualCommit(teavm_gc_markMap, teavm_pageCount(heapMapSize));
    #endif

    teavm_gc_minAvailableBytes = minHeap;
    teavm_gc_maxAvailableBytes = maxHeap;
    teavm_gc_gcStorageSize = 0;
    teavm_gc_regionMaxCount = 0;
    teavm_gc_availableBytes = 0;
    teavm_gc_resizeHeap(minHeap);
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
