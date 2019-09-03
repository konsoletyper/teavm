#pragma once
#include <stdint.h>

extern void* teavm_gc_heapAddress;
extern void* teavm_gc_gcStorageAddress;
extern int32_t teavm_gc_gcStorageSize;
extern void* teavm_gc_regionsAddress;
extern int32_t teavm_gc_regionSize;
extern int32_t teavm_gc_regionMaxCount;
extern int64_t teavm_gc_availableBytes;
extern void*** teavm_gc_staticRoots;

extern void teavm_initHeap(int64_t heapSize);

extern void teavm_registerStaticGcRoots(void***, int);
extern void teavm_initStaticGcRoots();