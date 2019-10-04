#pragma once
#include <stdint.h>
#include <stdio.h>
#include <stddef.h>
#include "definitions.h"

#if TEAVM_MEMORY_TRACE
    extern uint8_t* teavm_gc_heapMap;
    extern uint8_t* teavm_gc_markMap;
#endif

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
extern FILE* teavm_gc_openDumpFile(wchar_t* name);
extern int32_t teavm_gc_objectSize(void* address);