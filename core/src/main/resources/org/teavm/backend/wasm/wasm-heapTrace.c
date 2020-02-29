#define TEAVM_REFERENCE_SIZE 4

#if TEAVM_MEMORY_TRACE
    uint8_t* teavm_gc_heapMap = NULL;
    uint8_t* teavm_gc_markMap = NULL;
#endif

#if TEAVM_GC_STATS
    static int32_t teavm_gc_allocationCount = 0;
    static int32_t teavm_gc_freeCount = 0;
    static int32_t teavm_gc_freeByteCount = 0;
    static int32_t teavm_gc_markCount = 0;
    static int32_t teavm_gc_dirtyRegionCount = 0;
    static int32_t teavm_gc_relocatedBlocks = 0;
    static int32_t teavm_gc_relocatedBytes = 0;

    static int64_t teavm_gc_startTimeMillis;
    static int64_t teavm_gc_startTime;
    static int64_t teavm_gc_endTime;
    static int64_t teavm_gc_markStartTime;
    static int64_t teavm_gc_markEndTime;
    static int64_t teavm_gc_sweepStartTime;
    static int64_t teavm_gc_sweepEndTime;
    static int64_t teavm_gc_defragStartTime;
    static int64_t teavm_gc_defragEndTime;
    static int32_t teavm_gc_full;
#endif

#if TEAVM_MEMORY_TRACE
    static inline void teavmHeapTrace_assertSize(int32_t size) {
        if (size % TEAVM_REFERENCE_SIZE != 0) {
            abort();
        }
    }

    static inline void teavmHeapTrace_assertAddress(int32_t address) {
        if (address % TEAVM_REFERENCE_SIZE != 0) {
            abort();
        }
    }
#endif

int32_t teavm_javaHeapAddress();
int32_t teavm_availableBytes();
int32_t teavm_regionSize();
int32_t teavm_regionsAddress();

void teavmHeapTrace_init(int32_t maxHeap) {
    teavm_gc_heapMap = (uint8_t*) malloc(maxHeap / TEAVM_REFERENCE_SIZE);
    teavm_gc_markMap = (uint8_t*) malloc(maxHeap / TEAVM_REFERENCE_SIZE);
}

void teavmHeapTrace_allocate(int32_t address, int32_t size) {
    #if TEAVM_MEMORY_TRACE
        teavmHeapTrace_assertAddress(address);
        teavmHeapTrace_assertSize(size);

        address -= teavm_javaHeapAddress();
        size /= TEAVM_REFERENCE_SIZE;
        uint8_t* map = teavm_gc_heapMap + address / TEAVM_REFERENCE_SIZE;

        if (*map != 0) {
            fprintf(stderr, "[GC] trying allocate at memory in use at: %" PRId32 "\n", address);
            abort();
        }
        *map++ = 1;

        for (int32_t i = 1; i < size; ++i) {
            if (*map != 0) {
                fprintf(stderr, "[GC] trying allocate at memory in use at: %" PRId32 "\n", address);
                abort();
            }
            *map++ = 2;
        }
    #endif

    #if TEAVM_GC_STATS
        teavm_gc_allocationCount++;
    #endif
}

void teavmHeapTrace_free(int32_t address, int32_t size) {
    #if TEAVM_MEMORY_TRACE
        teavmHeapTrace_assertAddress(address);
        teavmHeapTrace_assertSize(size);

        address -= teavm_javaHeapAddress();
        int32_t offset = address / TEAVM_REFERENCE_SIZE;
        uint8_t* markMap = teavm_gc_markMap + offset;
        size /= TEAVM_REFERENCE_SIZE;
        for (int32_t i = 0; i < size; ++i) {
            if (markMap[i] != 0) {
                fprintf(stderr, "[GC] trying to release reachable object at: %" PRId32 "\n", address);
                abort();
            }
        }

        uint8_t* map = teavm_gc_heapMap + offset;
        memset(map, 0, size);
    #endif

    #if TEAVM_GC_STATS
        teavm_gc_freeCount++;
        teavm_gc_freeByteCount += size;
    #endif
}

void teavmHeapTrace_assertFree(int32_t address, int32_t size) {
    #if TEAVM_MEMORY_TRACE
        teavmHeapTrace_assertAddress(address);
        teavmHeapTrace_assertSize(size);

        address -= teavm_javaHeapAddress();
        int32_t offset = address / TEAVM_REFERENCE_SIZE;
        uint8_t* map = teavm_gc_heapMap + offset;
        size /= TEAVM_REFERENCE_SIZE;
        for (int32_t i = 0; i < size; ++i) {
            if (map[i] != 0) {
                fprintf(stderr, "[GC] memory supposed to be free at: %" PRId32 "\n", address);
                abort();
            }
        }
    #endif
}

void teavmHeapTrace_markStarted() {
    #if TEAVM_MEMORY_TRACE
        memset(teavm_gc_markMap, 0, teavm_availableBytes() / TEAVM_REFERENCE_SIZE);
    #endif

    #if TEAVM_GC_STATS
        teavm_gc_markStartTime = teavm_nanoTime();
    #endif
}

void teavmHeapTrace_markCompleted() {
    #if TEAVM_GC_STATS
        teavm_gc_markEndTime = teavm_nanoTime();
    #endif
}

#define TEAVM_ALIGN(addr, alignment) ((((addr) + ((int32_t) (alignment) - 1)) / (alignment) * (alignment)))
#define TEAVM_OBJECT_HEADER(address) (*(int32_t*) (wasm_heap + (address)))
#define TEAVM_OBJECT_HASH(address) (*(int32_t*) (wasm_heap + (address) + 4))
#define TEAVM_CLASS_OF(address) (TEAVM_OBJECT_HEADER(address) << 3);

#define teavm_class_size(address) (*(int32_t*) (wasm_heap + ((address) + 8)))
#define teavm_class_flags(address) (*(int32_t*) (wasm_heap + ((address) + 12)))
#define teavm_class_itemType(address) (*(int32_t*) (wasm_heap + ((address) + 32)))
#define teavm_class_superclass(address) (*(int32_t*) (wasm_heap + ((address) + 56)))
#define teavm_class_layout(address) (*(int32_t*) (wasm_heap + ((address) + 72)))
#define teavm_array_size(address) (*(int32_t*) (wasm_heap + ((address) + 8)))
#define teavm_reference_queue(address) (*(int32_t*) (wasm_heap + ((address) + 8)))
#define teavm_reference_object(address) (*(int32_t*) (wasm_heap + ((address) + 12)))
#define teavm_reference_next(address) (*(int32_t*) (wasm_heap + ((address) + 16)))
#define teavm_referenceQueue_first(address) (*(int32_t*) (wasm_heap + ((address) + 8)))
#define teavm_referenceQueue_last(address) (*(int32_t*) (wasm_heap + ((address) + 12)))
#define TEAVM_ARRAY_STRUCT_SIZE 12

int32_t teavmHeapTrace_objectSize(int32_t address) {
    int32_t cls = TEAVM_CLASS_OF(address);
    int32_t itemType = teavm_class_itemType(cls);
    if (itemType == 0) {
        return teavm_class_size(cls);
    }

    int32_t itemSize = teavm_class_flags(itemType) & 2 ? teavm_class_size(itemType) : TEAVM_REFERENCE_SIZE;
    int32_t size = TEAVM_ALIGN(TEAVM_ARRAY_STRUCT_SIZE, itemSize);
    size += teavm_array_size(address) * itemSize;
    size = TEAVM_ALIGN(size, TEAVM_REFERENCE_SIZE);
    return size;
}

void teavmHeapTrace_mark(int32_t address) {
    #if TEAVM_MEMORY_TRACE
        if (address < teavm_javaHeapAddress() || address >= teavm_javaHeapAddress() + teavm_availableBytes()) {
            return;
        }

        teavmHeapTrace_assertAddress(address);

        int32_t offset = (address - teavm_javaHeapAddress()) / TEAVM_REFERENCE_SIZE;
        uint8_t* map = teavm_gc_heapMap + offset;
        uint8_t* markMap = teavm_gc_markMap + offset;

        int32_t size = teavmHeapTrace_objectSize(address);
        teavmHeapTrace_assertSize(size);
        size /= TEAVM_REFERENCE_SIZE;

        if (*map++ != 1 || *markMap != 0) {
            fprintf(stderr, "[GC] assertion failed marking object at: %" PRId32 "\n",
                    address - teavm_javaHeapAddress());
            abort();
        }
        *markMap++ = 1;

        for (int32_t i = 1; i < size; ++i) {
            if (*map++ != 2 || *markMap != 0) {
                abort();
            }
            *markMap++ = 1;
        }
    #endif

    #if TEAVM_GC_STATS
        teavm_gc_markCount++;
    #endif
}

void teavmHeapTrace_move(int32_t from, int32_t to, int32_t size) {
    #if TEAVM_MEMORY_TRACE
        teavmHeapTrace_assertAddress(from);
        teavmHeapTrace_assertAddress(to);
        teavmHeapTrace_assertSize(size);

        uint8_t* mapFrom = teavm_gc_heapMap + ((from - teavm_javaHeapAddress()) / TEAVM_REFERENCE_SIZE);
        uint8_t* mapTo = teavm_gc_heapMap + ((to - teavm_javaHeapAddress()) / TEAVM_REFERENCE_SIZE);
        size /= TEAVM_REFERENCE_SIZE;

        if (mapFrom > mapTo) {
            for (int32_t i = 0; i < size; ++i) {
                if (mapFrom[i] == 0 || mapTo[i] != 0) {
                    fprintf(stderr, "[GC] assertion failed moving object from: %" PRId32 " to %" PRId32 "\n",
                            from - teavm_javaHeapAddress(), to - teavm_javaHeapAddress());
                    abort();
                }
                mapTo[i] = mapFrom[i];
                mapFrom[i] = 0;
            }
        } else {
            for (int32_t i = size - 1; i >= 0; --i) {
                if (mapFrom[i] == 0 || mapTo[i] != 0) {
                    abort();
                }
                mapTo[i] = mapFrom[i];
                mapFrom[i] = 0;
            }
        }
    #endif

    #if TEAVM_GC_STATS
        teavm_gc_relocatedBlocks++;
        teavm_gc_relocatedBytes += size;
    #endif
}

#if TEAVM_MEMORY_TRACE

    static inline int32_t teavm_verify(int32_t address) {
        if (address >= teavm_javaHeapAddress() && address < teavm_javaHeapAddress() + teavm_availableBytes()) {
            teavmHeapTrace_assertAddress(address);
            uint8_t* map = teavm_gc_heapMap + ((address - teavm_javaHeapAddress()) / TEAVM_REFERENCE_SIZE);
            if (*map != 1) {
                abort();
            }
        }

        return address;
    }

    void teavmHeapTrace_checkHeapConsistency(int32_t oldGen, int32_t offsets) {
        int32_t lastCheckedRegion = -1;
        int32_t obj = teavm_javaHeapAddress();
        uint16_t* regions = (uint16_t*) (wasm_heap + teavm_regionsAddress());
        while (obj < teavm_javaHeapAddress() + teavm_availableBytes()) {
            int32_t size;
            int32_t header = TEAVM_OBJECT_HEADER(obj);
            if (header == 0) {
                size = TEAVM_OBJECT_HASH(obj);
                teavmHeapTrace_assertFree(obj, size);
            } else {
                teavm_verify(obj);
                if (offsets) {
                    int32_t offset = obj - teavm_javaHeapAddress();
                    int32_t objRegion = offset / teavm_regionSize();
                    if (objRegion != lastCheckedRegion) {
                        while (++lastCheckedRegion < objRegion) {
                            if (regions[lastCheckedRegion] != 0) {
                                abort();
                            }
                        }
                        int32_t offsetInRegion = offset % teavm_regionSize();
                        if (regions[objRegion] != offsetInRegion + 1) {
                            abort();
                        }
                    }
                }
                if (oldGen && !(header & 0x40000000)) {
                    abort();
                }
                int32_t cls = TEAVM_CLASS_OF(obj);
                int32_t itemType = teavm_class_itemType(cls);
                if (itemType != 0) {
                    if (!(teavm_class_flags(itemType) & 2)) {
                        int32_t offset = 0;
                        offset += TEAVM_ARRAY_STRUCT_SIZE;
                        offset = TEAVM_ALIGN(offset, TEAVM_REFERENCE_SIZE);
                        int32_t data = obj + offset;
                        int32_t size = teavm_array_size(obj);
                        for (int32_t i = 0; i < size; ++i) {
                            teavm_verify(((int32_t*) (wasm_heap + data))[i]);
                        }
                    }
                } else {
                    while (cls != 0) {
                        int32_t kind = (teavm_class_flags(cls) >> 7) & 7;
                        if (kind == 1) {
                            teavm_verify(teavm_reference_next(obj));
                            teavm_verify(teavm_reference_object(obj));
                            teavm_verify(teavm_reference_queue(obj));
                        } else if (kind == 2) {
                            teavm_verify(teavm_referenceQueue_first(obj));
                            teavm_verify(teavm_referenceQueue_last(obj));
                        } else {
                            int32_t layoutOffset = teavm_class_layout(cls);
                            if (layoutOffset != 0) {
                                int16_t* layout = (int16_t*) (wasm_heap + layoutOffset);
                                int16_t size = *layout++;
                                for (int32_t i = 0; i < size; ++i) {
                                    int32_t ptr = obj + *layout++;
                                    teavm_verify(*(int32_t*) (wasm_heap + ptr));
                                }
                            }
                        }

                        cls = teavm_class_superclass(cls);
                    }
                }
                size = teavmHeapTrace_objectSize(obj);
            }

            if (size == 0) {
                abort();
            }
            obj += size;
        }

        if (offsets) {
            int32_t lastRegion = teavm_availableBytes() / teavm_regionSize();
            while (++lastCheckedRegion <= lastRegion) {
                if (regions[lastCheckedRegion] != 0) {
                    abort();
                }
            }
        }
    }
#endif

void teavmHeapTrace_gcStarted(int32_t full) {
    #if TEAVM_MEMORY_TRACE
        teavmHeapTrace_checkHeapConsistency(0, 0);
    #endif

    #if TEAVM_GC_STATS
        teavm_gc_startTime = teavm_nanoTime();
        teavm_gc_startTimeMillis = teavm_currentTimeMillis();
        teavm_gc_full = full;
    #endif
}

void teavmHeapTrace_sweepStarted() {
    #if TEAVM_GC_STATS
        teavm_gc_sweepStartTime = teavm_nanoTime();
    #endif
}

void teavmHeapTrace_sweepCompleted() {
    #if TEAVM_MEMORY_TRACE
        teavmHeapTrace_checkHeapConsistency(0, 1);
    #endif

    #if TEAVM_GC_STATS
        teavm_gc_sweepEndTime = teavm_nanoTime();
    #endif
}

void teavmHeapTrace_defragStarted() {
    #if TEAVM_GC_STATS
        teavm_gc_defragStartTime = teavm_nanoTime();
    #endif
}

void teavmHeapTrace_defragCompleted() {
    #if TEAVM_MEMORY_TRACE
        teavmHeapTrace_checkHeapConsistency(1, 1);
    #endif

    #if TEAVM_GC_STATS
        teavm_gc_defragEndTime = teavm_nanoTime();
    #endif
}

#if TEAVM_GC_STATS
    static void teavmHeapTrace_printStats() {
        fprintf(stderr, "[GC] Garbage collection (%s) performed at %" PRIu64 ", took %"
                PRIu64 " ns\n", teavm_gc_full ? "full" : "young", teavm_gc_startTimeMillis,
                teavm_gc_endTime - teavm_gc_startTime);

        fprintf(stderr, "[GC]   Allocations performed before GC: %" PRIu32 "\n", teavm_gc_allocationCount);

        fprintf(stderr, "[GC]   Mark phase took %" PRIu64 " ns, %" PRIu32 " objects reached\n",
                teavm_gc_markEndTime - teavm_gc_markStartTime, teavm_gc_markCount);

        if (!teavm_gc_full) {
            fprintf(stderr, "[GC]     Regions scanned from remembered set: %" PRIu32 "\n", teavm_gc_dirtyRegionCount);
        }

        fprintf(stderr, "[GC]   Sweep phase took %" PRIu64 " ns, %" PRIu32 " regions of %"
                PRIu32 " bytes freed\n", teavm_gc_sweepEndTime - teavm_gc_sweepStartTime, teavm_gc_freeCount,
                teavm_gc_freeByteCount);

        fprintf(stderr, "[GC]   Defrag phase took %" PRIu64 " ns\n",
                teavm_gc_defragEndTime - teavm_gc_defragStartTime);

        fprintf(stderr, "[GC]     Blocks relocated %" PRId32 " of total %" PRId32 " bytes\n",
                teavm_gc_relocatedBlocks, teavm_gc_relocatedBytes);
    }

    static void teavmHeapTrace_resetStats() {
        teavm_gc_allocationCount = 0;
        teavm_gc_markCount = 0;
        teavm_gc_dirtyRegionCount = 0;
        teavm_gc_freeCount = 0;
        teavm_gc_freeByteCount = 0;
        teavm_gc_relocatedBlocks = 0;
        teavm_gc_relocatedBytes = 0;
    }
#endif

void teavmHeapTrace_gcCompleted() {
    #if TEAVM_GC_STATS
        teavm_gc_endTime = teavm_nanoTime();
        teavmHeapTrace_printStats();
        teavmHeapTrace_resetStats();
    #endif
}

void teavmHeapTrace_heapResized(int64_t newSize) {
    #if TEAVM_GC_STATS
        fprintf(stderr, "[GC] Heap resized to %" PRIu64 " bytes\n", newSize);
    #endif
}

void teavmHeapTrace_reportDirtyRegion(int32_t address) {
    #if TEAVM_GC_STATS
        teavm_gc_dirtyRegionCount++;
    #endif
}
