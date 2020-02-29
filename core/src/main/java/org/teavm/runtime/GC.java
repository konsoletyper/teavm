/*
 *  Copyright 2016 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.runtime;

import org.teavm.interop.Address;
import org.teavm.interop.Export;
import org.teavm.interop.Import;
import org.teavm.interop.StaticInit;
import org.teavm.interop.Structure;
import org.teavm.interop.Unmanaged;

@Unmanaged
@StaticInit
public final class GC {
    private GC() {
    }

    private static final byte CARD_VALID = 1;
    private static final byte CARD_YOUNG_GEN = 2;
    private static final byte CARD_GAP = 4;
    private static final byte CARD_RELOCATABLE = 8;
    private static final int MIN_CHUNK_SIZE = 8;

    static Address currentChunkLimit;
    static FreeChunk currentChunk;
    static FreeChunkHolder currentChunkPointer;
    static int freeChunks;
    static int totalChunks;
    static int freeMemory = (int) availableBytes();
    static RuntimeReference firstWeakReference;
    static FreeChunk lastChunk;

    static RelocationBlock lastRelocationBlock;
    static boolean isFullGC = true;
    private static int youngGCCount;

    static native Address gcStorageAddress();

    static native int gcStorageSize();

    public static native Address heapAddress();

    private static native Region regionsAddress();

    private static native Address cardTable();

    private static native int regionMaxCount();

    public static native long availableBytes();

    public static native long minAvailableBytes();

    public static native long maxAvailableBytes();

    public static native void resizeHeap(long size);

    private static native int regionSize();

    public static native void writeBarrier(RuntimeObject object);

    @Import(name = "teavm_outOfMemory")
    public static native void outOfMemory();

    public static int getFreeMemory() {
        return freeMemory;
    }

    static {
        currentChunk = heapAddress().toStructure();
        currentChunk.classReference = 0;
        currentChunk.size = (int) availableBytes();
        currentChunkLimit = currentChunk.toAddress().add(currentChunk.size);
        currentChunkPointer = gcStorageAddress().toStructure();
        currentChunkPointer.value = currentChunk;
        freeChunks = 1;
        totalChunks = 1;

        int regionCount = getRegionCount();
        Allocator.fill(cardTable(), CARD_VALID, regionCount);
    }

    private static int getRegionCount() {
        return (int) (availableBytes() / regionSize()) + 1;
    }

    public static RuntimeObject alloc(int size) {
        FreeChunk current = currentChunk;
        Address next = current.toAddress().add(size);
        if (!next.add(Structure.sizeOf(FreeChunk.class)).isLessThan(currentChunkLimit)) {
            getNextChunk(size);
            current = currentChunk;
            next = current.toAddress().add(size);
        }
        currentChunk = next.toStructure();
        freeMemory -= size;
        MemoryTrace.allocate(current.toAddress(), size);
        return current.toAddress().toStructure();
    }

    private static void getNextChunk(int size) {
        if (getNextChunkIfPossible(size)) {
            return;
        }
        collectGarbageImpl(size);
        if (currentChunk.size != size && currentChunk.size <= size + MIN_CHUNK_SIZE && !getNextChunkIfPossible(size)) {
            ExceptionHandling.printStack();
            outOfMemory();
        }
    }

    private static boolean getNextChunkIfPossible(int size) {
        while (true) {
            if (currentChunk.toAddress().isLessThan(currentChunkLimit)) {
                currentChunk.classReference = 0;
                currentChunk.size = (int) (currentChunkLimit.toLong() - currentChunk.toAddress().toLong());
            }
            if (--freeChunks == 0) {
                return false;
            }
            currentChunkPointer = Structure.add(FreeChunkHolder.class, currentChunkPointer, 1);
            currentChunk = currentChunkPointer.value;
            if (currentChunk.size >= size + MIN_CHUNK_SIZE || currentChunk.size == size) {
                currentChunkLimit = currentChunk.toAddress().add(currentChunk.size);
                break;
            }
            freeMemory -= currentChunk.size;
        }
        return true;
    }

    @Export(name = "teavm_gc_collect")
    public static void collectGarbage() {
        fixHeap();
        collectGarbageImpl(0);
    }

    @Export(name = "teavm_gc_collectFull")
    public static void collectGarbageFull() {
        fixHeap();
        collectGarbageFullImpl(0);
    }

    private static void collectGarbageFullImpl(int size) {
        triggerFullGC();
        collectGarbageImpl(size);
    }

    private static void triggerFullGC() {
        isFullGC = true;
        int regionsCount = getRegionCount();
        Allocator.fill(cardTable(), (byte) 0, getRegionCount());
        Allocator.fill(regionsAddress().toAddress(), (byte) 0, regionsCount * Structure.sizeOf(Region.class));
    }

    private static void collectGarbageImpl(int size) {
        doCollectGarbage();

        long minRequestedSize = 0;
        if (!hasAvailableChunk(size)) {
            minRequestedSize = computeMinRequestedSize(size);
        }

        if (!isFullGC) {
            if (++youngGCCount >= 8 && isAboutToExpand(minRequestedSize)) {
                triggerFullGC();
                doCollectGarbage();
                youngGCCount = 0;
            }
        } else {
            youngGCCount = 0;
        }
        isFullGC = false;

        resizeHeapIfNecessary(minRequestedSize);
        currentChunk = currentChunkPointer.value;
        currentChunkLimit = currentChunk.toAddress().add(currentChunk.size);

        Allocator.fill(cardTable(), CARD_VALID, getRegionCount());
    }

    private static void doCollectGarbage() {
        MemoryTrace.gcStarted(isFullGC);
        if (!isFullGC) {
            storeGapsInCardTable();
        }
        mark();
        processReferences();
        sweep();
        defragment();
        updateFreeMemory();
        MemoryTrace.gcCompleted();
        totalChunks = freeChunks;
    }

    private static boolean hasAvailableChunk(int size) {
        if (size == 0) {
            return true;
        }
        FreeChunkHolder ptr = currentChunkPointer;
        for (int i = 0; i < freeChunks; ++i) {
            if (size == ptr.value.size || size + MIN_CHUNK_SIZE <= ptr.value.size) {
                return true;
            }
            ptr = Structure.add(FreeChunkHolder.class, ptr, 1);
        }
        return false;
    }

    private static long computeMinRequestedSize(int size) {
        if (lastChunk.classReference == 0) {
            size -= lastChunk.size;
        }
        return availableBytes() + size;
    }

    @Export(name = "teavm_gc_fixHeap")
    public static void fixHeap() {
        if (freeChunks > 0) {
            currentChunk.classReference = 0;
            currentChunk.size = (int) (currentChunkLimit.toLong() - currentChunk.toAddress().toLong());
        }
    }

    @Export(name = "teavm_gc_tryShrink")
    public static void tryShrink() {
        long availableBytes = availableBytes();
        long occupiedMemory = availableBytes - freeMemory;
        if (occupiedMemory < availableBytes / 4) {
            collectGarbageFull();
        }
    }

    private static void mark() {
        MemoryTrace.markStarted();
        firstWeakReference = null;

        markFromStaticFields();
        markFromClasses();
        markFromStack();
        if (!isFullGC) {
            markFromOldGeneration();
        }

        MemoryTrace.markCompleted();
    }

    private static void markFromStaticFields() {
        Address staticRoots = Mutator.getStaticGCRoots();
        int staticCount = staticRoots.getInt();
        staticRoots = staticRoots.add(Address.sizeOf());
        while (staticCount-- > 0) {
            RuntimeObject object = staticRoots.getAddress().getAddress().toStructure();
            if (object != null) {
                mark(object);
            }
            staticRoots = staticRoots.add(Address.sizeOf());
        }
    }

    private static void markFromClasses() {
        int classCount = Mutator.getClassCount();
        Address classPtr = Mutator.getClasses();
        for (int i = 0; i < classCount; ++i) {
            RuntimeClass cls = classPtr.getAddress().toStructure();
            if (cls.simpleNameCache != null) {
                mark(cls.simpleNameCache);
            }
            if (cls.canonicalName != null) {
                mark(cls.canonicalName);
            }
            if (cls.nameCache != null) {
                mark(cls.nameCache);
            }
            classPtr = classPtr.add(Address.sizeOf());
        }
    }

    private static void markFromStack() {
        for (Address stackRoots = ShadowStack.getStackTop(); stackRoots != null;
             stackRoots = ShadowStack.getNextStackFrame(stackRoots)) {
            int count = ShadowStack.getStackRootCount(stackRoots);
            Address stackRootsPtr = ShadowStack.getStackRootPointer(stackRoots);
            while (count-- > 0) {
                RuntimeObject obj = stackRootsPtr.getAddress().toStructure();
                mark(obj);
                stackRootsPtr = stackRootsPtr.add(Address.sizeOf());
            }
        }
    }

    private static void markFromOldGeneration() {
        int validMask = CARD_VALID | (CARD_VALID << 8) | (CARD_VALID << 16) | (CARD_VALID << 24);
        int regionsCount = getRegionCount();
        int regionSize = regionSize();

        Address cardPtr = cardTable();
        Address regionPtr = heapAddress();
        int regionIndex;
        for (regionIndex = 0; regionIndex < regionsCount - 3; regionIndex += 4) {
            int n = cardPtr.getInt();
            if ((n & validMask) != validMask) {
                for (int i = 0; i < 4; ++i) {
                    n = cardPtr.add(i).getByte();
                    if ((n & CARD_VALID) == 0) {
                        markFromRegion(regionIndex + i);
                    }
                }
            }
            cardPtr = cardPtr.add(4);
            regionPtr = regionPtr.add(4 * regionSize);
        }

        for (; regionIndex < regionsCount; regionIndex++) {
            if ((cardPtr.getByte() & CARD_VALID) == 0) {
                markFromRegion(regionIndex);
            }
            cardPtr = cardPtr.add(1);
        }
    }

    private static void markFromRegion(int regionIndex) {
        Address card = cardTable().add(regionIndex);
        int regionOffset = Structure.add(Region.class, regionsAddress(), regionIndex).start;
        if (regionOffset == 0) {
            card.putByte((byte) (card.getByte() | CARD_VALID));
            return;
        }
        regionOffset--;

        int regionSize = regionSize();
        Address regionStart = heapAddress().add(regionIndex * regionSize);
        MemoryTrace.reportDirtyRegion(regionStart);
        Address regionEnd = regionStart.add(regionSize);
        FreeChunk object = regionStart.add(regionOffset).toStructure();
        Address heapLimit = heapAddress().add(availableBytes());
        if (heapLimit.isLessThan(regionEnd)) {
            regionEnd = heapLimit;
        }

        boolean objectMarked = false;
        while (object.toAddress().isLessThan(regionEnd)) {
            int header = object.classReference;

            if (header != 0 && (header & RuntimeObject.GC_OLD_GENERATION) != 0) {
                objectMarked |= doMarkOldGeneration(object.toAddress().toStructure());
            }

            object = object.toAddress().add(objectSize(object)).toStructure();
        }

        if (!objectMarked) {
            card.putByte((byte) (card.getByte() | CARD_VALID));
        }
    }

    private static void mark(RuntimeObject object) {
        if (object == null || isMarked(object)) {
            return;
        }
        MarkQueue.init();
        enqueueMark(object);
        doProcessMarkQueue();
    }

    private static boolean doMarkOldGeneration(RuntimeObject object) {
        MarkQueue.init();
        boolean hasObjectsFromYoungGen = markObjectData(object);
        doProcessMarkQueue();
        return hasObjectsFromYoungGen;
    }

    private static void doProcessMarkQueue() {
        while (!MarkQueue.isEmpty()) {
            RuntimeObject object = MarkQueue.dequeue();
            MemoryTrace.mark(object.toAddress());

            long offset = object.toAddress().toLong() - heapAddress().toLong();
            Region region = Structure.add(Region.class, regionsAddress(), (int) (offset /  regionSize()));
            short relativeOffset = (short) (offset % regionSize() + 1);
            if (region.start == 0 || region.start > relativeOffset) {
                region.start = relativeOffset;
            }
            Address cardTableItem = cardTable().add(offset / regionSize());
            cardTableItem.putByte((byte) (cardTableItem.getByte() | CARD_YOUNG_GEN));

            markObjectData(object);
        }
    }

    private static boolean markObjectData(RuntimeObject object) {
        RuntimeClass cls = RuntimeClass.getClass(object);
        if (cls.itemType == null) {
            return markObject(cls, object);
        } else {
            return markArray(cls, (RuntimeArray) object);
        }
    }

    private static boolean markObject(RuntimeClass cls, RuntimeObject object) {
        boolean hasObjectsFromYoungGen = false;
        while (cls != null) {
            int type = (cls.flags >> RuntimeClass.VM_TYPE_SHIFT) & RuntimeClass.VM_TYPE_MASK;
            switch (type) {
                case RuntimeClass.VM_TYPE_WEAKREFERENCE:
                    hasObjectsFromYoungGen |= markWeakReference((RuntimeReference) object);
                    break;

                case RuntimeClass.VM_TYPE_REFERENCEQUEUE:
                    hasObjectsFromYoungGen |= markReferenceQueue((RuntimeReferenceQueue) object);
                    break;

                default:
                    hasObjectsFromYoungGen |= markFields(cls, object);
                    break;
            }
            cls = cls.parent;
        }
        return hasObjectsFromYoungGen;
    }

    private static boolean markWeakReference(RuntimeReference object) {
        boolean hasObjectsFromYoungGen = false;
        if (object.queue != null) {
            hasObjectsFromYoungGen |= enqueueMark(object.queue);
            if (object.next != null && object.object != null) {
                hasObjectsFromYoungGen |= enqueueMark(object.object);
            }
        }
        if (object.next != null) {
            hasObjectsFromYoungGen |= enqueueMark(object.next);
        } else if (object.object != null) {
            object.next = firstWeakReference;
            firstWeakReference = object;
        }
        return hasObjectsFromYoungGen;
    }

    private static boolean markReferenceQueue(RuntimeReferenceQueue object) {
        RuntimeReference reference = object.first;
        boolean hasObjectsFromYoungGen = false;
        if (reference != null) {
            hasObjectsFromYoungGen |= enqueueMark(reference);
        }
        return hasObjectsFromYoungGen;
    }

    private static boolean markFields(RuntimeClass cls, RuntimeObject object) {
        Address layout = cls.layout;
        if (layout == null) {
            return false;
        }
        boolean hasObjectsFromYoungGen = false;
        short fieldCount = layout.getShort();
        while (fieldCount-- > 0) {
            layout = layout.add(2);
            int fieldOffset = layout.getShort();
            RuntimeObject reference = object.toAddress().add(fieldOffset).getAddress().toStructure();
            hasObjectsFromYoungGen |= enqueueMark(reference);
        }
        return hasObjectsFromYoungGen;
    }

    private static boolean markArray(RuntimeClass cls, RuntimeArray array) {
        if ((cls.itemType.flags & RuntimeClass.PRIMITIVE) != 0) {
            return false;
        }
        Address base = Address.align(array.toAddress().add(RuntimeArray.class, 1), Address.sizeOf());
        boolean hasObjectsFromYoungGen = false;
        for (int i = 0; i < array.size; ++i) {
            RuntimeObject reference = base.getAddress().toStructure();
            hasObjectsFromYoungGen |= enqueueMark(reference);
            base = base.add(Address.sizeOf());
        }
        return hasObjectsFromYoungGen;
    }

    private static boolean enqueueMark(RuntimeObject object) {
        if (object == null) {
            return false;
        }
        if (!isMarked(object)) {
            doEnqueueMark(object);
            return true;
        } else {
            return (object.classReference & RuntimeObject.GC_OLD_GENERATION) == 0;
        }
    }

    private static void doEnqueueMark(RuntimeObject object) {
        if (isFullGC) {
            object.classReference |= RuntimeObject.GC_MARKED | RuntimeObject.GC_OLD_GENERATION;
        } else {
            object.classReference |= RuntimeObject.GC_MARKED;
        }
        MarkQueue.enqueue(object);
    }

    private static void processReferences() {
        RuntimeReference reference = firstWeakReference;
        while (reference != null) {
            RuntimeReference next = reference.next;
            reference.next = null;
            if (!isMarked(reference.object)) {
                reference.object = null;
                RuntimeReferenceQueue queue = reference.queue;
                if (queue != null) {
                    if (queue.first == null) {
                        queue.first = reference;
                    } else {
                        queue.last.next = reference;
                        makeInvalid(queue.last);
                    }
                    queue.last = reference;
                    makeInvalid(queue);
                }
            }
            reference = next;
        }
    }

    private static void makeInvalid(RuntimeObject object) {
        long offset = object.toAddress().toLong() - heapAddress().toLong();
        Address cardTableItem = cardTable().add(offset / regionSize());
        cardTableItem.putByte((byte) (cardTableItem.getByte() & ~CARD_VALID));
    }

    private static void sweep() {
        MemoryTrace.sweepStarted();

        currentChunkPointer = gcStorageAddress().toStructure();
        freeChunks = 0;
        totalChunks = 0;

        FreeChunk object = heapAddress().toStructure();
        FreeChunk lastFreeSpace = null;
        long heapSize = availableBytes();
        int regionsCount = getRegionCount();
        Address currentRegionEnd = null;
        Address limit = heapAddress().add(heapSize);

        loop: while (object.toAddress().isLessThan(limit)) {
            int tag = object.classReference;
            boolean free;
            if (tag == 0) {
                free = true;
            } else {
                free = (tag & RuntimeObject.GC_MARKED) == 0;
                if (free && !isFullGC && (tag & RuntimeObject.GC_OLD_GENERATION) != 0) {
                    free = false;
                }
                if (!free) {
                    tag &= ~RuntimeObject.GC_MARKED;
                }
                object.classReference = tag;
            }

            if (free) {
                if (lastFreeSpace == null) {
                    lastFreeSpace = object;
                }

                if (!object.toAddress().isLessThan(currentRegionEnd)) {
                    int currentRegionIndex = (int) ((object.toAddress().toLong() - heapAddress().toLong())
                            / regionSize());
                    Region currentRegion = Structure.add(Region.class, regionsAddress(), currentRegionIndex);
                    currentRegionEnd = heapAddress().add((currentRegionIndex + 1) * regionSize());
                    if (currentRegion.start == 0) {
                        do {
                            if (++currentRegionIndex == regionsCount) {
                                object = limit.toStructure();
                                break loop;
                            }
                            currentRegion = Structure.add(Region.class, regionsAddress(), currentRegionIndex);
                        } while (currentRegion.start == 0);

                        Address newRegionStart = heapAddress().add(currentRegionIndex * regionSize());
                        object = newRegionStart.add(currentRegion.start - 1).toStructure();
                        currentRegionEnd = newRegionStart.add(regionSize());
                        continue;
                    }
                }
            } else {
                if (lastFreeSpace != null) {
                    freeMemory(lastFreeSpace, object);
                    lastFreeSpace = null;
                }

                if (!object.toAddress().isLessThan(currentRegionEnd)) {
                    int currentRegionIndex = (int) ((object.toAddress().toLong() - heapAddress().toLong())
                            / regionSize());
                    currentRegionEnd = heapAddress().add((currentRegionIndex + 1) * regionSize());
                    FreeChunk formerObject = object;

                    if (!isFullGC && (cardTable().add(currentRegionIndex).getByte() & CARD_YOUNG_GEN) == 0
                            && (cardTable().add(currentRegionIndex).getByte() & CARD_GAP) == 0) {
                        // We are collecting young generation and the new region is composed entirely of old
                        // generation objects, so skip this one and all subsequent ones

                        Region currentRegion;
                        do {
                            if (++currentRegionIndex == regionsCount) {
                                break;
                            }
                            currentRegion = Structure.add(Region.class, regionsAddress(), currentRegionIndex);
                        } while (currentRegion.start != 0
                                && (cardTable().add(currentRegionIndex).getByte() & CARD_YOUNG_GEN) == 0
                                && (cardTable().add(currentRegionIndex).getByte() & CARD_GAP) == 0);

                        --currentRegionIndex;
                        currentRegion = Structure.add(Region.class, regionsAddress(), currentRegionIndex);

                        Address newRegionStart = heapAddress().add(currentRegionIndex * regionSize());
                        object = newRegionStart.add(currentRegion.start - 1).toStructure();
                        currentRegionEnd = newRegionStart.add(regionSize());
                        if (formerObject.toAddress().isLessThan(object.toAddress())) {
                            continue;
                        }
                    }
                }
            }

            int size = objectSize(object);
            object = object.toAddress().add(size).toStructure();
        }

        if (lastFreeSpace != null) {
            freeMemory(lastFreeSpace, object);
        }

        currentChunkPointer = gcStorageAddress().toStructure();
        MemoryTrace.sweepCompleted();
    }

    private static void storeGapsInCardTable() {
        for (int i = 0; i < totalChunks; ++i) {
            FreeChunk freeChunkStart = FreeChunkHolder.add(FreeChunkHolder.class,
                    gcStorageAddress().toStructure(), i).value;
            long freeChunkOffset = freeChunkStart.toAddress().toLong() - heapAddress().toLong();
            long freeChunkEndOffset = freeChunkOffset + freeChunkStart.size;
            int startRegion = (int) (freeChunkOffset / regionSize());
            int endRegion = (int) (freeChunkEndOffset / regionSize());
            for (int region = startRegion; region <= endRegion; ++region) {
                Address card = cardTable().add(region);
                card.putByte((byte) (card.getByte() | CARD_GAP));
            }
        }
    }

    private static void clearGapsFromCardTable() {
        int gapMask = ~(CARD_GAP | (CARD_GAP << 8) | (CARD_GAP << 16) | (CARD_GAP << 24));
        int regionsCount = getRegionCount();

        Address cardPtr = cardTable();
        int regionIndex;
        for (regionIndex = 0; regionIndex < regionsCount - 3; regionIndex += 4) {
            cardPtr.putInt(cardPtr.getInt() & gapMask);
            cardPtr = cardPtr.add(4);
        }

        for (; regionIndex < regionsCount; regionIndex++) {
            cardPtr.putByte((byte) (cardPtr.getByte() & ~CARD_GAP));
            cardPtr = cardPtr.add(1);
        }
    }

    private static void freeMemory(FreeChunk from, FreeChunk to) {
        from.classReference = 0;
        from.size = (int) (to.toAddress().toLong() - from.toAddress().toLong());
        MemoryTrace.free(from.toAddress(), from.size);
        currentChunkPointer.value = from;
        currentChunkPointer = Structure.add(FreeChunkHolder.class, currentChunkPointer, 1);
        freeChunks++;
        totalChunks++;
    }

    private static void defragment() {
        MemoryTrace.defragStarted();
        clearGapsFromCardTable();
        storeGapsInCardTable();
        markStackRoots();
        moveNonRelocatableObjectsToOldGeneration();
        calculateRelocationTargets();
        updatePointersFromStaticRoots();
        updatePointersFromClasses();
        updatePointersFromObjects();
        restoreObjectHeaders();
        relocateObjects();
        putNewFreeChunks();
        MemoryTrace.defragCompleted();
    }

    private static void markStackRoots() {
        Address relocationThreshold = currentChunkPointer.value.toAddress();

        for (Address stackRoots = ShadowStack.getStackTop(); stackRoots != null;
             stackRoots = ShadowStack.getNextStackFrame(stackRoots)) {
            int count = ShadowStack.getStackRootCount(stackRoots);
            Address stackRootsPtr = ShadowStack.getStackRootPointer(stackRoots);
            while (count-- > 0) {
                RuntimeObject obj = stackRootsPtr.getAddress().toStructure();
                if (!obj.toAddress().isLessThan(relocationThreshold)) {
                    if (isFullGC || (obj.classReference & RuntimeObject.GC_OLD_GENERATION) == 0) {
                        obj.classReference |= RuntimeObject.GC_MARKED;
                    }
                }
                stackRootsPtr = stackRootsPtr.add(Address.sizeOf());
            }
        }
    }

    private static void moveNonRelocatableObjectsToOldGeneration() {
        Address limitAddress = currentChunkPointer.value.toAddress();
        long limit = limitAddress.toLong() - heapAddress().toLong();
        for (int region = 0; region * (long) regionSize() < limit; ++region) {
            if ((cardTable().add(region).getByte() & CARD_YOUNG_GEN) != 0) {
                moveObjectsToOldGenerationInRegion(region, limitAddress);
            }
        }
    }

    private static void moveObjectsToOldGenerationInRegion(int region, Address limit) {
        int regionOffset = Structure.add(Region.class, regionsAddress(), region).start - 1;

        int regionSize = regionSize();
        Address regionStart = heapAddress().add(region * regionSize);
        Address regionEnd = regionStart.add(regionSize);
        FreeChunk object = regionStart.add(regionOffset).toStructure();
        if (limit.isLessThan(regionEnd)) {
            regionEnd = limit;
        }

        while (object.toAddress().isLessThan(regionEnd)) {
            int classRef = object.classReference;
            if (classRef != 0 && (classRef & RuntimeObject.GC_OLD_GENERATION) == 0) {
                classRef |= RuntimeObject.GC_OLD_GENERATION;
                object.classReference = classRef;
            }
            int size = objectSize(object);
            object = object.toAddress().add(size).toStructure();
        }
    }

    private static void calculateRelocationTargets() {
        Address start = heapAddress();
        long heapSize = availableBytes();
        Address limit = start.add(heapSize);

        FreeChunkHolder freeChunkPointer = currentChunkPointer;
        int freeChunks = GC.freeChunks;
        FreeChunk freeChunk = currentChunkPointer.value;
        FreeChunk object = freeChunk.toAddress().add(freeChunk.size).toStructure();

        RelocationBlock relocationBlock = Structure.add(FreeChunkHolder.class, currentChunkPointer, freeChunks)
                .toAddress().toStructure();
        Address relocationTarget = freeChunk.toAddress();
        relocationBlock.start = relocationTarget;
        relocationBlock.end = limit;
        relocationBlock.count = 0;
        RelocationBlock lastRelocationBlock = relocationBlock;
        int countInCurrentRelocationBlock = 0;

        Address relocations = Structure.add(FreeChunk.class, freeChunk, 1).toAddress();
        Address relocationsLimit = freeChunk.toAddress().add(freeChunk.size);
        Address currentRegionEnd = null;
        lastChunk = heapAddress().toStructure();

        boolean lastWasLocked = false;
        objects: while (object.toAddress().isLessThan(limit)) {
            int size = objectSize(object);
            if (object.classReference != 0) {
                Address nextRelocationTarget = null;
                boolean shouldRelocateObject = shouldRelocateObject(object);
                object.classReference |= RuntimeObject.GC_OLD_GENERATION;
                if (shouldRelocateObject) {
                    while (true) {
                        nextRelocationTarget = relocationTarget.add(size);
                        if (nextRelocationTarget == relocationBlock.end
                                || nextRelocationTarget.add(MIN_CHUNK_SIZE - 1).isLessThan(relocationBlock.end)) {
                            break;
                        }

                        RelocationBlock nextRelocationBlock = Structure.add(RelocationBlock.class, relocationBlock, 1);
                        if (nextRelocationBlock.start == object.toAddress()) {
                            shouldRelocateObject = false;
                            break;
                        }
                        relocationBlock.count = countInCurrentRelocationBlock;
                        countInCurrentRelocationBlock = 0;
                        relocationBlock = nextRelocationBlock;
                        relocationTarget = relocationBlock.start;
                    }
                }

                if (!shouldRelocateObject) {
                    if (!lastWasLocked) {
                        lastRelocationBlock.end = object.toAddress();
                        lastRelocationBlock = Structure.add(RelocationBlock.class, lastRelocationBlock, 1);
                        lastRelocationBlock.end = limit;
                        lastWasLocked = true;
                    }

                    // Trying to skip continuous sequences of objects from old generation
                    if (!isFullGC && !object.toAddress().isLessThan(currentRegionEnd)) {
                        int region = (int) ((object.toAddress().toLong() - heapAddress().toLong()) / regionSize());
                        currentRegionEnd = heapAddress().add((long) regionSize() * (region + 1));
                        byte card = cardTable().add(region).getByte();
                        if ((card & CARD_YOUNG_GEN) == 0 && (card & CARD_GAP) == 0) {
                            while (true) {
                                region++;
                                card = cardTable().add(region).getByte();
                                if ((card & CARD_YOUNG_GEN) != 0 || (card & CARD_GAP) != 0) {
                                    break;
                                }
                                if (Structure.add(Region.class, regionsAddress(), region).start == 0) {
                                    break;
                                }
                            }
                            region--;
                            currentRegionEnd = heapAddress().add((long) regionSize() * (region + 1));

                            int offset = Structure.add(Region.class, regionsAddress(), region).start - 1;
                            object = heapAddress().add((long) regionSize() * region).add(offset).toStructure();
                            size = objectSize(object);
                        }
                    }

                    lastRelocationBlock.start = object.toAddress().add(size);
                    lastRelocationBlock.count = 0;
                    object.classReference &= ~RuntimeObject.GC_MARKED;
                    lastChunk = object;
                } else {
                    lastWasLocked = false;

                    while (!relocations.add(Structure.sizeOf(Relocation.class)).isLessThan(relocationsLimit)) {
                        if (--freeChunks == 0) {
                            lastRelocationBlock.end = object.toAddress();
                            break objects;
                        }
                        freeChunkPointer = Structure.add(FreeChunkHolder.class, freeChunkPointer, 1);
                        freeChunk = freeChunkPointer.value;
                        relocations = Structure.add(FreeChunk.class, freeChunk, 1).toAddress();
                        relocationsLimit = freeChunk.toAddress().add(freeChunk.size);
                    }

                    Relocation relocation = relocations.toStructure();
                    relocation.classBackup = object.classReference;
                    relocation.sizeBackup = object.size;
                    relocation.newAddress = relocationTarget;
                    countInCurrentRelocationBlock++;
                    relocations = relocations.add(Structure.sizeOf(Relocation.class));

                    long targetAddress = relocation.toAddress().toLong();
                    object.classReference = (int) (targetAddress >>> 33) | RuntimeObject.GC_MARKED;
                    object.size = (int) (targetAddress >> 1);
                    relocationTarget = nextRelocationTarget;

                    int region = (int) ((object.toAddress().toLong() - heapAddress().toLong()) / regionSize());
                    Address card = cardTable().add(region);
                    card.putByte((byte) (card.getByte() | CARD_RELOCATABLE));
                }
            } else {
                lastWasLocked = false;
            }
            object = object.toAddress().add(size).toStructure();
        }

        relocationBlock.count = countInCurrentRelocationBlock;

        while (object.toAddress().isLessThan(limit)) {
            int size = objectSize(object);
            if (object.classReference != 0) {
                int classRef = object.classReference;
                classRef &= ~RuntimeObject.GC_MARKED;
                classRef |= RuntimeObject.GC_OLD_GENERATION;
                object.classReference = classRef;
            } else {
                lastRelocationBlock = Structure.add(RelocationBlock.class, lastRelocationBlock, 1);
                lastRelocationBlock.start = object.toAddress();
                lastRelocationBlock.count = 0;
                lastRelocationBlock.end = lastRelocationBlock.start.add(size);
            }
            object = object.toAddress().add(size).toStructure();
        }

        GC.lastRelocationBlock = lastRelocationBlock;
    }

    private static boolean shouldRelocateObject(FreeChunk object) {
        return (object.classReference & RuntimeObject.GC_MARKED) == 0
                && (isFullGC || (object.classReference & RuntimeObject.GC_OLD_GENERATION) == 0);
    }

    private static void updatePointersFromStaticRoots() {
        Address staticRoots = Mutator.getStaticGCRoots();
        int staticCount = staticRoots.getInt();
        staticRoots = staticRoots.add(Address.sizeOf());
        while (staticCount-- > 0) {
            Address staticRoot = staticRoots.getAddress();
            staticRoot.putAddress(updatePointer(staticRoot.getAddress()));
            staticRoots = staticRoots.add(Address.sizeOf());
        }
    }

    private static void updatePointersFromClasses() {
        int classCount = Mutator.getClassCount();
        Address classPtr = Mutator.getClasses();
        for (int i = 0; i < classCount; ++i) {
            RuntimeClass cls = classPtr.getAddress().toStructure();
            if (cls.simpleNameCache != null) {
                cls.simpleNameCache = updatePointer(cls.simpleNameCache.toAddress()).toStructure();
            }
            if (cls.canonicalName != null) {
                cls.canonicalName = updatePointer(cls.canonicalName.toAddress()).toStructure();
            }
            if (cls.nameCache != null) {
                cls.nameCache = updatePointer(cls.nameCache.toAddress()).toStructure();
            }
            classPtr = classPtr.add(Address.sizeOf());
        }
    }

    private static void updatePointersFromObjects() {
        if (isFullGC) {
            updatePointersFromObjectsFull();
        } else {
            updatePointersFromObjectsYoung();
        }
    }

    private static void updatePointersFromObjectsFull() {
        Address start = heapAddress();
        long heapSize = availableBytes();
        Address limit = start.add(heapSize);

        FreeChunk object = heapAddress().toStructure();

        while (object.toAddress().isLessThan(limit)) {
            int classRef = object.classReference;
            int size;
            if (classRef != 0) {
                Relocation relocation = getRelocation(object.toAddress());
                if (relocation != null) {
                    classRef = relocation.classBackup;
                }
                RuntimeClass cls = RuntimeClass.unpack(classRef);
                RuntimeObject realObject = object.toAddress().toStructure();
                updatePointers(cls, realObject);
                size = objectSize(realObject, cls);
            } else {
                size = object.size;
            }

            object = object.toAddress().add(size).toStructure();
        }
    }

    private static void updatePointersFromObjectsYoung() {
        int validMask = CARD_VALID | (CARD_VALID << 8) | (CARD_VALID << 16) | (CARD_VALID << 24);
        int youngMask = CARD_YOUNG_GEN | (CARD_YOUNG_GEN << 8) | (CARD_YOUNG_GEN << 16) | (CARD_YOUNG_GEN << 24);
        int regionsCount = getRegionCount();
        int regionSize = regionSize();

        Address cardPtr = cardTable();
        Address regionPtr = heapAddress();
        int regionIndex;
        for (regionIndex = 0; regionIndex < regionsCount - 3; regionIndex += 4) {
            int n = cardPtr.getInt();
            if ((n & validMask) != validMask || (n & youngMask) != 0) {
                for (int i = 0; i < 4; ++i) {
                    n = cardPtr.add(i).getByte();
                    if ((n & CARD_VALID) == 0 || (n & CARD_YOUNG_GEN) != 0) {
                        updatePointersFromRegion(regionIndex + i);
                    }
                }
            }
            cardPtr = cardPtr.add(4);
            regionPtr = regionPtr.add(4 * regionSize);
        }

        for (; regionIndex < regionsCount; regionIndex++) {
            int n = cardPtr.getByte();
            if ((n & CARD_VALID) == 0 || (n & CARD_YOUNG_GEN) != 0) {
                updatePointersFromRegion(regionIndex);
            }
            cardPtr = cardPtr.add(1);
        }
    }

    private static void updatePointersFromRegion(int regionIndex) {
        int regionOffset = Structure.add(Region.class, regionsAddress(), regionIndex).start - 1;
        if (regionOffset < 0) {
            return;
        }

        int regionSize = regionSize();
        Address regionStart = heapAddress().add(regionIndex * regionSize);
        Address regionEnd = regionStart.add(regionSize);
        FreeChunk object = regionStart.add(regionOffset).toStructure();
        Address heapLimit = heapAddress().add(availableBytes());
        if (heapLimit.isLessThan(regionEnd)) {
            regionEnd = heapLimit;
        }

        while (object.toAddress().isLessThan(regionEnd)) {
            int classRef = object.classReference;
            int size;
            if (classRef != 0) {
                Relocation relocation = getRelocation(object.toAddress());
                if (relocation != null) {
                    classRef = relocation.classBackup;
                }
                RuntimeClass cls = RuntimeClass.unpack(classRef);
                updatePointers(cls, object.toAddress().toStructure());
                size = objectSize(object.toAddress().toStructure(), cls);
            } else {
                size = object.size;
            }

            object = object.toAddress().add(size).toStructure();
        }
    }

    private static void updatePointers(RuntimeClass cls, RuntimeObject object) {
        if (cls.itemType == null) {
            updatePointersInObject(cls, object);
        } else {
            updatePointersInArray(cls, (RuntimeArray) object);
        }
    }

    private static void updatePointersInObject(RuntimeClass cls, RuntimeObject object) {
        while (cls != null) {
            int type = (cls.flags >> RuntimeClass.VM_TYPE_SHIFT) & RuntimeClass.VM_TYPE_MASK;
            switch (type) {
                case RuntimeClass.VM_TYPE_WEAKREFERENCE:
                    updatePointersInWeakReference((RuntimeReference) object);
                    break;

                case RuntimeClass.VM_TYPE_REFERENCEQUEUE:
                    updatePointersInReferenceQueue((RuntimeReferenceQueue) object);
                    break;

                default:
                    updatePointersInFields(cls, object);
                    break;
            }
            cls = cls.parent;
        }
    }

    private static void updatePointersInWeakReference(RuntimeReference object) {
        object.queue = updatePointer(object.queue.toAddress()).toStructure();
        object.next = updatePointer(object.next.toAddress()).toStructure();
        object.object = updatePointer(object.object.toAddress()).toStructure();
    }

    private static void updatePointersInReferenceQueue(RuntimeReferenceQueue object) {
        object.first = updatePointer(object.first.toAddress()).toStructure();
        object.last = updatePointer(object.last.toAddress()).toStructure();
    }

    private static void updatePointersInFields(RuntimeClass cls, RuntimeObject object) {
        Address layout = cls.layout;
        if (layout != null) {
            short fieldCount = layout.getShort();
            while (fieldCount-- > 0) {
                layout = layout.add(2);
                int fieldOffset = layout.getShort();
                Address referenceHolder = object.toAddress().add(fieldOffset);
                referenceHolder.putAddress(updatePointer(referenceHolder.getAddress()));
            }
        }
    }

    private static void updatePointersInArray(RuntimeClass cls, RuntimeArray array) {
        if ((cls.itemType.flags & RuntimeClass.PRIMITIVE) != 0) {
            return;
        }
        Address base = Address.align(array.toAddress().add(RuntimeArray.class, 1), Address.sizeOf());
        int size = array.size;
        for (int i = 0; i < size; ++i) {
            base.putAddress(updatePointer(base.getAddress()));
            base = base.add(Address.sizeOf());
        }
    }

    private static Address updatePointer(Address address) {
        if (address == null) {
            return null;
        }
        Relocation relocation = getRelocation(address);
        return relocation != null ? relocation.newAddress : address;
    }

    private static Relocation getRelocation(Address address) {
        if (address.isLessThan(heapAddress()) || !address.isLessThan(heapAddress().add(availableBytes()))) {
            return null;
        }
        FreeChunk obj = address.toStructure();
        if ((obj.classReference & RuntimeObject.GC_MARKED) == 0) {
            return null;
        }
        long result = (((long) obj.classReference & 0xFFFFFFFFL) << 33) | (((long) obj.size & 0xFFFFFFFFL) << 1);
        return Address.fromLong(result).toStructure();
    }


    private static void restoreObjectHeaders() {
        int relocatableMask = CARD_RELOCATABLE | (CARD_RELOCATABLE << 8) | (CARD_RELOCATABLE << 16)
                | (CARD_RELOCATABLE << 24);
        int regionsCount = getRegionCount();

        Address cardPtr = cardTable();
        Address limit = heapAddress().add(availableBytes());
        int regionIndex;
        for (regionIndex = 0; regionIndex < regionsCount - 3; regionIndex += 4) {
            int n = cardPtr.getInt();
            if ((n & relocatableMask) != 0) {
                for (int i = 0; i < 4; ++i) {
                    n = cardPtr.add(i).getByte();
                    if ((n & CARD_RELOCATABLE) != 0) {
                        restoreObjectHeadersInRegion(regionIndex + i, limit);
                    }
                }
            }
            cardPtr = cardPtr.add(4);
        }

        for (; regionIndex < regionsCount; regionIndex++) {
            if ((cardPtr.getByte() & CARD_RELOCATABLE) != 0) {
                restoreObjectHeadersInRegion(regionIndex, limit);
            }
            cardPtr = cardPtr.add(1);
        }
    }

    private static void restoreObjectHeadersInRegion(int region, Address limit) {
        int regionOffset = Structure.add(Region.class, regionsAddress(), region).start - 1;

        int regionSize = regionSize();
        Address regionStart = heapAddress().add(region * regionSize);
        Address regionEnd = regionStart.add(regionSize);
        FreeChunk object = regionStart.add(regionOffset).toStructure();
        if (limit.isLessThan(regionEnd)) {
            regionEnd = limit;
        }

        restoreObjectHeadersInRange(object, regionEnd);
    }

    private static void restoreObjectHeadersInRange(FreeChunk object, Address limit) {
        while (object.toAddress().isLessThan(limit)) {
            Relocation relocation = getRelocation(object.toAddress());
            if (relocation != null) {
                object.classReference = relocation.classBackup | RuntimeObject.GC_MARKED;
                object.size = relocation.sizeBackup;
            }
            int size = objectSize(object);
            object = object.toAddress().add(size).toStructure();
        }
    }

    private static void relocateObjects() {
        Address start = heapAddress();
        long heapSize = availableBytes();
        Address limit = start.add(heapSize);

        int freeChunks = GC.freeChunks;
        FreeChunk freeChunk = currentChunkPointer.value;
        FreeChunk object = freeChunk.toAddress().add(freeChunk.size).toStructure();

        RelocationBlock relocationBlock = Structure.add(FreeChunkHolder.class, currentChunkPointer, freeChunks)
                .toAddress().toStructure();
        int countInRelocationBlock = relocationBlock.count;
        Address relocationTarget = relocationBlock.start;

        Address blockTarget = null;
        Address blockSource = null;
        int blockSize = 0;
        Address currentRegionEnd = null;
        int regionCount = getRegionCount();

        while (object.toAddress().isLessThan(limit)) {
            int size = objectSize(object);
            if ((object.classReference & RuntimeObject.GC_MARKED) != 0) {
                object.classReference &= ~RuntimeObject.GC_MARKED;

                while (countInRelocationBlock == 0) {
                    if (blockSize != 0) {
                        moveMemoryBlock(blockSource, blockTarget, blockSize);
                        blockSource = null;
                        blockSize = 0;
                    }

                    relocationBlock.start = relocationTarget;
                    relocationBlock = Structure.add(RelocationBlock.class, relocationBlock, 1);
                    countInRelocationBlock = relocationBlock.count;
                    relocationTarget = relocationBlock.start;
                }

                if (blockSource == null) {
                    blockSource = object.toAddress();
                    blockTarget = relocationTarget;
                }

                relocationTarget = relocationTarget.add(size);
                blockSize += size;
                --countInRelocationBlock;
            } else {
                if (blockSource != null) {
                    moveMemoryBlock(blockSource, blockTarget, blockSize);
                    blockSource = null;
                    blockSize = 0;
                }

                // Trying to skip continuous sequences of non-relocatable objects
                if (object.classReference != 0 && !object.toAddress().isLessThan(currentRegionEnd)) {
                    int region = (int) ((object.toAddress().toLong() - heapAddress().toLong()) / regionSize());
                    currentRegionEnd = heapAddress().add((long) regionSize() * (region + 1));
                    byte card = cardTable().add(region).getByte();
                    if ((card & CARD_RELOCATABLE) == 0 && (card & CARD_GAP) == 0) {
                        while (++region < regionCount) {
                            card = cardTable().add(region).getByte();
                            if ((card & CARD_RELOCATABLE) != 0 || (card & CARD_GAP) != 0) {
                                break;
                            }
                            if (Structure.add(Region.class, regionsAddress(), region).start == 0) {
                                break;
                            }
                        }
                        region--;
                        currentRegionEnd = heapAddress().add((long) regionSize() * (region + 1));

                        int offset = Structure.add(Region.class, regionsAddress(), region).start - 1;
                        object = heapAddress().add((long) regionSize() * region).add(offset).toStructure();
                        size = objectSize(object);
                    }
                }
            }

            object = object.toAddress().add(size).toStructure();
        }

        relocationBlock.start = relocationTarget;
        if (blockSource != null) {
            moveMemoryBlock(blockSource, blockTarget, blockSize);
        }
    }

    private static void moveMemoryBlock(Address blockSource, Address blockTarget, int blockSize) {
        long sourceStartOffset = blockSource.toLong() - heapAddress().toLong();
        int sourceStartRegionIndex = (int) (sourceStartOffset / regionSize());
        Region sourceStartRegion = Structure.add(Region.class, regionsAddress(), sourceStartRegionIndex);

        long sourceEndOffset = sourceStartOffset + blockSize;
        int sourceEndRegionIndex = (int) (sourceEndOffset / regionSize());
        Region sourceEndRegion = Structure.add(Region.class, regionsAddress(), sourceEndRegionIndex);

        if (sourceStartRegion != sourceEndRegion && sourceStartOffset % regionSize() + 1 == sourceStartRegion.start) {
            sourceStartRegion.start = 0;
        }
        for (int i = sourceStartRegionIndex + 1; i < sourceEndRegionIndex; ++i) {
            Structure.add(Region.class, regionsAddress(), i).start = 0;
        }

        if (sourceStartRegion != sourceEndRegion || sourceStartOffset % regionSize() + 1 == sourceStartRegion.start) {
            Address heapLimit = heapAddress().add(availableBytes());
            FreeChunk objectAfterSource = blockSource.add(blockSize).toStructure();

            if (objectAfterSource.toAddress().isLessThan(heapLimit)) {
                int objectRegionIndex = sourceEndRegionIndex;
                if (objectAfterSource.classReference == 0) {
                    objectAfterSource = objectAfterSource.toAddress().add(objectAfterSource.size).toStructure();
                    objectRegionIndex = (int) ((objectAfterSource.toAddress().toLong() - heapAddress().toLong())
                            / regionSize());
                }
                if (objectRegionIndex != sourceEndRegionIndex || !objectAfterSource.toAddress().isLessThan(heapLimit)) {
                    sourceEndRegion.start = 0;
                } else {
                    sourceEndRegion.start = (short) ((objectAfterSource.toAddress().toLong() - heapAddress().toLong())
                            % regionSize() + 1);
                }
            } else {
                sourceEndRegion.start = 0;
            }
        }

        Allocator.moveMemoryBlock(blockSource, blockTarget, blockSize);

        FreeChunk object = blockTarget.toStructure();
        Address blockTargetEnd = blockTarget.add(blockSize);
        Address currentRegionEnd = null;
        while (object.toAddress().isLessThan(blockTargetEnd)) {
            if (!object.toAddress().isLessThan(currentRegionEnd)) {
                long offset = object.toAddress().toLong() - heapAddress().toLong();
                int regionIndex = (int) (offset / regionSize());
                currentRegionEnd = heapAddress().add((long) regionSize() * (regionIndex + 1));
                Region region = Structure.add(Region.class, regionsAddress(), regionIndex);
                int offsetInRegion = (int) (offset % regionSize());
                if (region.start == 0 || region.start - 1 > offsetInRegion) {
                    region.start = (short) (offsetInRegion + 1);
                }
            }
            int size = objectSize(object);
            object = object.toAddress().add(size).toStructure();
        }

        MemoryTrace.move(blockSource, blockTarget, blockSize);
    }

    private static void putNewFreeChunks() {
        FreeChunkHolder freeChunkPointer = currentChunkPointer;
        RelocationBlock relocationBlock = Structure.add(FreeChunkHolder.class, currentChunkPointer, freeChunks)
                .toAddress().toStructure();
        freeChunks = 0;
        while (!lastRelocationBlock.toAddress().isLessThan(relocationBlock.toAddress())) {
            if (relocationBlock.start.isLessThan(relocationBlock.end)) {
                FreeChunk freeChunk = relocationBlock.start.toStructure();
                if (!freeChunk.toAddress().isLessThan(lastChunk.toAddress())) {
                    lastChunk = freeChunk;
                }
                freeChunk.size = (int) (relocationBlock.end.toLong() - relocationBlock.start.toLong());
                freeChunk.classReference = 0;
                MemoryTrace.assertFree(freeChunk.toAddress(), freeChunk.size);
                freeChunkPointer.value = freeChunk;
                freeChunkPointer = Structure.add(FreeChunkHolder.class, freeChunkPointer, 1);
                freeChunks++;
            }
            relocationBlock = Structure.add(RelocationBlock.class, relocationBlock, 1);
        }
        totalChunks = freeChunks;
    }

    private static void updateFreeMemory() {
        freeMemory = 0;
        FreeChunkHolder freeChunkPtr = currentChunkPointer;
        for (int i = 0; i < freeChunks; ++i) {
            freeMemory += freeChunkPtr.value.size;
            freeChunkPtr = Structure.add(FreeChunkHolder.class, freeChunkPtr, 1);
        }
    }

    private static void resizeHeapConsistent(long newSize) {
        long oldSize = availableBytes();
        if (newSize == oldSize) {
            return;
        }
        if (newSize > oldSize) {
            int previousRegionCount = getRegionCount();
            resizeHeap(newSize);
            currentChunkPointer = gcStorageAddress().toStructure();
            int newRegionCount = getRegionCount();
            for (int i = previousRegionCount; i < newRegionCount; ++i) {
                Structure.add(Region.class, regionsAddress(), i).start = 0;
            }

            if (lastChunk.classReference == 0) {
                lastChunk.size += (int) (newSize - oldSize);
            } else {
                int size = objectSize(lastChunk);
                lastChunk = lastChunk.toAddress().add(size).toStructure();
                lastChunk.classReference = 0;
                lastChunk.size = (int) (newSize - oldSize);
                Structure.add(FreeChunkHolder.class, currentChunkPointer, freeChunks).value = lastChunk;
                freeChunks++;
                totalChunks++;
            }
        } else {
            long minimumSize = lastChunk.toAddress().toLong() - heapAddress().toLong();
            if (lastChunk.classReference != 0) {
                minimumSize += objectSize(lastChunk);
            }
            if (newSize < minimumSize) {
                newSize = minimumSize;
                if (newSize == oldSize) {
                    return;
                }
            }
            if (newSize == minimumSize) {
                freeChunks--;
                totalChunks--;
            } else {
                lastChunk.size -= (int) (oldSize - newSize);
            }
            resizeHeap(newSize);

            currentChunkPointer = gcStorageAddress().toStructure();
        }
    }

    private static void resizeHeapIfNecessary(long requestedSize) {
        long availableBytes = availableBytes();
        long occupiedMemory = availableBytes - freeMemory;
        if (isAboutToExpand(requestedSize)) {
            long newSize = max(requestedSize, (availableBytes - freeMemory) * 2);
            newSize = min(newSize, maxAvailableBytes());
            if (newSize != availableBytes) {
                if (newSize % 8 != 0) {
                    newSize += 8 - newSize % 8;
                }
                resizeHeapConsistent(newSize);
            }
        } else if (occupiedMemory < availableBytes / 4) {
            long newSize = occupiedMemory * 3;
            newSize = max(newSize, minAvailableBytes());
            if (newSize % 8 != 0) {
                newSize -= newSize % 8;
            }
            resizeHeapConsistent(newSize);
        }
    }

    private static boolean isAboutToExpand(long requestedSize) {
        long availableBytes = availableBytes();
        long occupiedMemory = availableBytes - freeMemory;
        return requestedSize > availableBytes || occupiedMemory > availableBytes / 2;
    }

    private static long min(long a, long b) {
        return a < b ? a : b;
    }

    private static long max(long a, long b) {
        return a > b ? a : b;
    }

    private static int objectSize(FreeChunk object) {
        if (object.classReference == 0) {
            return object.size;
        } else {
            RuntimeObject realObject = object.toAddress().toStructure();
            RuntimeClass cls = RuntimeClass.getClass(realObject);
            return objectSize(realObject, cls);
        }
    }

    private static int objectSize(RuntimeObject object, RuntimeClass cls) {
        if (cls.itemType == null) {
            return cls.size;
        }
        int itemSize = (cls.itemType.flags & RuntimeClass.PRIMITIVE) == 0
                ? Address.sizeOf()
                : cls.itemType.size;
        RuntimeArray array = object.toAddress().toStructure();
        Address address = Address.fromInt(Structure.sizeOf(RuntimeArray.class));
        address = Address.align(address, itemSize);
        address = address.add(itemSize * array.size);
        address = Address.align(address, Address.sizeOf());
        return address.toInt();
    }

    private static boolean isMarked(RuntimeObject object) {
        return (object.classReference & RuntimeObject.GC_MARKED) != 0
                || (!isFullGC && (object.classReference & RuntimeObject.GC_OLD_GENERATION) != 0);
    }

    static class Region extends Structure {
        short start;
    }
}
