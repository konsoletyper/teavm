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

    static Address currentChunkLimit;
    static FreeChunk currentChunk;
    static FreeChunkHolder currentChunkPointer;
    static int freeChunks;
    static int freeMemory = (int) availableBytes();
    static RuntimeReference firstWeakReference;
    static FreeChunk lastChunk;

    static RelocationBlock lastRelocationBlock;

    static native Address gcStorageAddress();

    static native int gcStorageSize();

    public static native Address heapAddress();

    private static native Region regionsAddress();

    private static native int regionMaxCount();

    public static native long availableBytes();

    public static native long minAvailableBytes();

    public static native long maxAvailableBytes();

    public static native void resizeHeap(long size);

    private static native int regionSize();

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
        if (currentChunk.size < size && !getNextChunkIfPossible(size)) {
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
            if (currentChunk.size >= size) {
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

    private static void collectGarbageImpl(int size) {
        MemoryTrace.gcStarted();
        mark();
        processReferences();
        sweep();
        MemoryTrace.sweepCompleted();
        defragment();
        MemoryTrace.defragCompleted();
        updateFreeMemory();
        long minRequestedSize = 0;
        if (!hasAvailableChunk(size)) {
            minRequestedSize = computeMinRequestedSize(size);
        }
        resizeHeapIfNecessary(minRequestedSize);
        currentChunk = currentChunkPointer.value;
        currentChunkLimit = currentChunk.toAddress().add(currentChunk.size);
    }

    private static boolean hasAvailableChunk(int size) {
        if (size == 0) {
            return true;
        }
        FreeChunkHolder ptr = currentChunkPointer;
        for (int i = 0; i < freeChunks; ++i) {
            if (size <= ptr.value.size) {
                return true;
            }
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
            collectGarbage();
        }
    }

    private static void mark() {
        MemoryTrace.initMark();
        firstWeakReference = null;
        int regionsCount = (int) ((availableBytes() - 1) / regionSize()) + 1;
        Allocator.fillZero(regionsAddress().toAddress(), regionsCount * Structure.sizeOf(Region.class));

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

    private static void mark(RuntimeObject object) {
        if (object == null || isMarked(object)) {
            return;
        }

        MarkQueue.init();
        MarkQueue.enqueue(object);
        object.classReference |= RuntimeObject.GC_MARKED;
        while (!MarkQueue.isEmpty()) {
            object = MarkQueue.dequeue();
            MemoryTrace.mark(object.toAddress());

            long offset = object.toAddress().toLong() - heapAddress().toLong();
            Region region = Structure.add(Region.class, regionsAddress(), (int) (offset /  regionSize()));
            short relativeOffset = (short) (offset % regionSize() + 1);
            if (region.start == 0 || region.start > relativeOffset) {
                region.start = relativeOffset;
            }

            RuntimeClass cls = RuntimeClass.getClass(object);
            if (cls.itemType == null) {
                markObject(cls, object);
            } else {
                markArray(cls, (RuntimeArray) object);
            }
        }
    }

    private static void markObject(RuntimeClass cls, RuntimeObject object) {
        while (cls != null) {
            int type = (cls.flags >> RuntimeClass.VM_TYPE_SHIFT) & RuntimeClass.VM_TYPE_MASK;
            switch (type) {
                case RuntimeClass.VM_TYPE_WEAKREFERENCE:
                    markWeakReference((RuntimeReference) object);
                    break;

                case RuntimeClass.VM_TYPE_REFERENCEQUEUE:
                    markReferenceQueue((RuntimeReferenceQueue) object);
                    break;

                default:
                    markFields(cls, object);
                    break;
            }
            cls = cls.parent;
        }
    }

    private static void markWeakReference(RuntimeReference object) {
        if (object.queue != null) {
            enqueueMark(object.queue);
            if (object.next != null && object.object != null) {
                enqueueMark(object.object);
            }
        }
        if (object.next == null && object.object != null) {
            object.next = firstWeakReference;
            firstWeakReference = object;
        }
    }

    private static void markReferenceQueue(RuntimeReferenceQueue object) {
        RuntimeReference reference = object.first;
        while (reference != null) {
            enqueueMark(reference);
            reference = reference.next;
        }
    }

    private static void markFields(RuntimeClass cls, RuntimeObject object) {
        Address layout = cls.layout;
        if (layout != null) {
            short fieldCount = layout.getShort();
            while (fieldCount-- > 0) {
                layout = layout.add(2);
                int fieldOffset = layout.getShort();
                RuntimeObject reference = object.toAddress().add(fieldOffset).getAddress().toStructure();
                enqueueMark(reference);
            }
        }
    }

    private static void markArray(RuntimeClass cls, RuntimeArray array) {
        if ((cls.itemType.flags & RuntimeClass.PRIMITIVE) != 0) {
            return;
        }
        Address base = Address.align(array.toAddress().add(RuntimeArray.class, 1), Address.sizeOf());
        for (int i = 0; i < array.size; ++i) {
            RuntimeObject reference = base.getAddress().toStructure();
            enqueueMark(reference);
            base = base.add(Address.sizeOf());
        }
    }

    private static void enqueueMark(RuntimeObject object) {
        if (object != null && !isMarked(object)) {
            object.classReference |= RuntimeObject.GC_MARKED;
            MarkQueue.enqueue(object);
        }
    }

    private static void processReferences() {
        RuntimeReference reference = firstWeakReference;
        while (reference != null) {
            RuntimeReference next = reference.next;
            reference.next = null;
            if ((reference.object.classReference & RuntimeObject.GC_MARKED) == 0) {
                reference.object = null;
                RuntimeReferenceQueue queue = reference.queue;
                if (queue != null) {
                    if (queue.first == null) {
                        queue.first = reference;
                    } else {
                        queue.last.next = reference;
                    }
                    queue.last = reference;
                }
            }
            reference = next;
        }
    }

    private static void sweep() {
        FreeChunkHolder freeChunkPtr = gcStorageAddress().toStructure();
        freeChunks = 0;

        FreeChunk object = heapAddress().toStructure();
        FreeChunk lastFreeSpace = null;
        long heapSize = availableBytes();
        int regionsCount = (int) ((heapSize - 1) / regionSize()) + 1;
        Address currentRegionEnd = null;
        Address limit = heapAddress().add(heapSize);

        loop: while (object.toAddress().isLessThan(limit)) {
            if (!object.toAddress().isLessThan(currentRegionEnd)) {
                int currentRegionIndex = (int) ((object.toAddress().toLong() - heapAddress().toLong()) / regionSize());
                Region currentRegion = Structure.add(Region.class, regionsAddress(), currentRegionIndex);
                if (currentRegion.start == 0) {
                    if (lastFreeSpace == null) {
                        lastFreeSpace = object;
                    }

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
                }
            }

            int tag = object.classReference;
            boolean free;
            if (tag == 0) {
                free = true;
            } else {
                free = (tag & RuntimeObject.GC_MARKED) == 0;
                if (!free) {
                    tag &= ~RuntimeObject.GC_MARKED;
                }
                object.classReference = tag;
            }

            if (free) {
                if (lastFreeSpace == null) {
                    lastFreeSpace = object;
                }
            } else {
                if (lastFreeSpace != null) {
                    lastFreeSpace.classReference = 0;
                    lastFreeSpace.size = (int) (object.toAddress().toLong() - lastFreeSpace.toAddress().toLong());
                    MemoryTrace.free(lastFreeSpace.toAddress(), lastFreeSpace.size);
                    freeChunkPtr.value = lastFreeSpace;
                    freeChunkPtr = Structure.add(FreeChunkHolder.class, freeChunkPtr, 1);
                    freeChunks++;
                    lastFreeSpace = null;
                }
            }

            int size = objectSize(object);
            object = object.toAddress().add(size).toStructure();
        }

        if (lastFreeSpace != null) {
            int freeSize = (int) (object.toAddress().toLong() - lastFreeSpace.toAddress().toLong());
            lastFreeSpace.classReference = 0;
            lastFreeSpace.size = freeSize;
            MemoryTrace.free(lastFreeSpace.toAddress(), lastFreeSpace.size);
            freeChunkPtr.value = lastFreeSpace;
            freeChunks++;
        }

        currentChunkPointer = gcStorageAddress().toStructure();
    }

    private static void defragment() {
        markStackRoots();
        calculateRelocationTargets();
        updatePointersFromStaticRoots();
        updatePointersFromClasses();
        updatePointersFromObjects();
        restoreObjectHeaders();
        relocateObjects();
        putNewFreeChunks();
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
                    obj.classReference |= RuntimeObject.GC_MARKED;
                }
                stackRootsPtr = stackRootsPtr.add(Address.sizeOf());
            }
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
        lastChunk = heapAddress().toStructure();

        boolean lastWasLocked = false;
        objects: while (object.toAddress().isLessThan(limit)) {
            int size = objectSize(object);
            if (object.classReference != 0) {
                Address nextRelocationTarget = null;
                boolean shouldRelocateObject = (object.classReference & RuntimeObject.GC_MARKED) == 0;
                if (shouldRelocateObject) {
                    while (true) {
                        nextRelocationTarget = relocationTarget.add(size);
                        if (!relocationBlock.end.isLessThan(nextRelocationTarget)) {
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
                object.classReference &= ~RuntimeObject.GC_MARKED;
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
        Address start = heapAddress();
        long heapSize = availableBytes();
        Address limit = start.add(heapSize);

        FreeChunk freeChunk = currentChunkPointer.value;
        FreeChunk object = freeChunk.toAddress().add(freeChunk.size).toStructure();

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

        while (object.toAddress().isLessThan(limit)) {
            int size = objectSize(object);
            if ((object.classReference & RuntimeObject.GC_MARKED) != 0) {
                object.classReference &= ~RuntimeObject.GC_MARKED;

                while (countInRelocationBlock == 0) {
                    if (blockSize != 0) {
                        Allocator.moveMemoryBlock(blockSource, blockTarget, blockSize);
                        MemoryTrace.move(blockSource, blockTarget, blockSize);
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
            } else if (blockSource != null) {
                Allocator.moveMemoryBlock(blockSource, blockTarget, blockSize);
                MemoryTrace.move(blockSource, blockTarget, blockSize);
                blockSource = null;
                blockSize = 0;
            }

            object = object.toAddress().add(size).toStructure();
        }

        relocationBlock.start = relocationTarget;
        if (blockSource != null) {
            Allocator.moveMemoryBlock(blockSource, blockTarget, blockSize);
            MemoryTrace.move(blockSource, blockTarget, blockSize);
        }
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
            resizeHeap(newSize);
            if (lastChunk.classReference == 0) {
                lastChunk.size += (int) (newSize - oldSize);
            } else {
                int size = objectSize(lastChunk);
                lastChunk = lastChunk.toAddress().add(size).toStructure();
                lastChunk.classReference = 0;
                lastChunk.size = (int) (newSize - oldSize);
                Structure.add(FreeChunkHolder.class, currentChunkPointer, freeChunks).value = lastChunk;
                freeChunks++;
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
            } else {
                lastChunk.size -= (int) (oldSize - newSize);
            }
            resizeHeap(newSize);
        }
    }

    private static void resizeHeapIfNecessary(long requestedSize) {
        long availableBytes = availableBytes();
        long occupiedMemory = availableBytes - freeMemory;
        if (requestedSize > availableBytes || occupiedMemory > availableBytes / 2) {
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
        return (object.classReference & RuntimeObject.GC_MARKED) != 0;
    }

    static class Region extends Structure {
        short start;
    }
}
