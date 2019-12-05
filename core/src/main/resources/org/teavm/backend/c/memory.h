#pragma once
#include <stdint.h>

extern void* teavm_gc_heapAddress;
extern void* teavm_gc_gcStorageAddress;
extern int32_t teavm_gc_gcStorageSize;
extern void* teavm_gc_regionsAddress;
extern void* teavm_gc_cardTable;
#define teavm_gc_regionSize INT32_C(2048)
extern int32_t teavm_gc_regionMaxCount;
extern int64_t teavm_gc_availableBytes;
extern int64_t teavm_gc_minAvailableBytes;
extern int64_t teavm_gc_maxAvailableBytes;
extern void*** teavm_gc_staticRoots;

extern void teavm_initHeap(int64_t minHeap, int64_t maxHeap);
extern void teavm_gc_resizeHeap(int64_t newSize);

extern void teavm_registerStaticGcRoots(void***, int);
extern void teavm_initStaticGcRoots();