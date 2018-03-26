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

    static native Address gcStorageAddress();

    static native int gcStorageSize();

    private static native Address heapAddress();

    private static native Region regionsAddress();

    private static native int regionMaxCount();

    public static native long availableBytes();

    private static native int regionSize();

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
        getAvailableChunkIfPossible(0);
    }

    public static RuntimeObject alloc(int size) {
        FreeChunk current = currentChunk;
        Address next = current.toAddress().add(size);
        if (!next.add(Structure.sizeOf(FreeChunk.class)).isLessThan(currentChunkLimit)) {
            getAvailableChunk(size);
            current = currentChunk;
            next = currentChunk.toAddress().add(size);
        }
        int freeSize = current.size;
        freeSize -= size;
        if (freeSize > 0) {
            currentChunk = next.toStructure();
            currentChunk.size = freeSize;
        } else {
            freeMemory -= size;
            getAvailableChunkIfPossible(currentChunk.size + 1);
        }
        currentChunk.classReference = 0;
        freeMemory -= size;
        return current.toAddress().toStructure();
    }

    private static void getAvailableChunk(int size) {
        if (getAvailableChunkIfPossible(size)) {
            return;
        }
        collectGarbage(size);
        getAvailableChunkIfPossible(size);
    }

    private static boolean getAvailableChunkIfPossible(int size) {
        if (freeChunks == 0) {
            return false;
        }
        while (true) {
            if (currentChunk.toAddress().add(size) == currentChunkLimit) {
                break;
            }
            if (currentChunk.toAddress().add(size + Structure.sizeOf(FreeChunk.class)).isLessThan(currentChunkLimit)) {
                break;
            }
            if (--freeChunks == 0) {
                return false;
            }
            freeMemory -= currentChunk.size;
            currentChunkPointer = Structure.add(FreeChunkHolder.class, currentChunkPointer, 1);
            currentChunk = currentChunkPointer.value;
            currentChunkLimit = currentChunk.toAddress().add(currentChunk.size);
        }
        return true;
    }

    public static boolean collectGarbage(int size) {
        mark();
        sweep();
        updateFreeMemory();
        return true;
    }

    private static void mark() {
        Allocator.fillZero(regionsAddress().toAddress(), regionMaxCount() * Structure.sizeOf(Region.class));

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
        while (!MarkQueue.isEmpty()) {
            object = MarkQueue.dequeue();
            if (isMarked(object)) {
                continue;
            }
            object.classReference |= RuntimeObject.GC_MARKED;

            long offset = object.toAddress().toLong() - heapAddress().toLong();
            Region region = Structure.add(Region.class, regionsAddress(), (int) (offset /  regionSize()));
            short relativeOffset = (short) (offset % regionSize() + 1);
            if (region.start == 0 || region.start > relativeOffset) {
                region.start = relativeOffset;
            }

            RuntimeClass cls = RuntimeClass.getClass(object);
            if (cls.itemType == null) {
                while (cls != null) {
                    Address layout = cls.layout;
                    if (layout != null) {
                        short fieldCount = layout.getShort();
                        while (fieldCount-- > 0) {
                            layout = layout.add(2);
                            int fieldOffset = layout.getShort();
                            RuntimeObject reference = object.toAddress().add(fieldOffset).getAddress().toStructure();
                            if (reference != null && !isMarked(reference)) {
                                MarkQueue.enqueue(reference);
                            }
                        }
                    }
                    cls = cls.parent;
                }
            } else {
                if ((cls.itemType.flags & RuntimeClass.PRIMITIVE) == 0) {
                    RuntimeArray array = (RuntimeArray) object;
                    Address base = Address.align(array.toAddress().add(RuntimeArray.class, 1), Address.sizeOf());
                    for (int i = 0; i < array.size; ++i) {
                        RuntimeObject reference = base.getAddress().toStructure();
                        if (reference != null && !isMarked(reference)) {
                            MarkQueue.enqueue(reference);
                        }
                        base = base.add(Address.sizeOf());
                    }
                }
            }
        }
    }

    private static void sweep() {
        FreeChunkHolder freeChunkPtr = gcStorageAddress().toStructure();
        freeChunks = 0;

        FreeChunk object = heapAddress().toStructure();
        FreeChunk lastFreeSpace = null;
        long heapSize = availableBytes();
        long reclaimedSpace = 0;
        long maxFreeChunk = 0;
        int currentRegionIndex = 0;
        int regionsCount = (int) ((heapSize - 1) / regionSize()) + 1;
        Address currentRegionEnd = object.toAddress().add(regionSize());
        Address limit = heapAddress().add(heapSize);

        loop: while (object.toAddress().isLessThan(limit)) {
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

                if (!object.toAddress().isLessThan(currentRegionEnd)) {
                    currentRegionIndex = (int) ((object.toAddress().toLong() - heapAddress().toLong()) / regionSize());
                    Region currentRegion = Structure.add(Region.class, regionsAddress(), currentRegionIndex);
                    if (currentRegion.start == 0) {
                        do {
                            if (++currentRegionIndex == regionsCount) {
                                object = limit.toStructure();
                                break loop;
                            }
                            currentRegion = Structure.add(Region.class, regionsAddress(), currentRegionIndex);
                        } while (currentRegion.start == 0);
                    }
                    currentRegionEnd = currentRegion.toAddress().add(regionSize());
                }
            } else {
                if (lastFreeSpace != null) {
                    lastFreeSpace.classReference = 0;
                    lastFreeSpace.size = (int) (object.toAddress().toLong() - lastFreeSpace.toAddress().toLong());
                    freeChunkPtr.value = lastFreeSpace;
                    freeChunkPtr = Structure.add(FreeChunkHolder.class, freeChunkPtr, 1);
                    freeChunks++;
                    reclaimedSpace += lastFreeSpace.size;
                    if (maxFreeChunk < lastFreeSpace.size) {
                        maxFreeChunk = lastFreeSpace.size;
                    }
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
            freeChunkPtr.value = lastFreeSpace;
            freeChunkPtr = Structure.add(FreeChunkHolder.class, freeChunkPtr, 1);
            freeChunks++;
            reclaimedSpace += freeSize;
            if (maxFreeChunk < freeSize) {
                maxFreeChunk = freeSize;
            }
        }

        currentChunkPointer = gcStorageAddress().toStructure();
        sortFreeChunks(0, freeChunks - 1);
        currentChunk = currentChunkPointer.value;
        currentChunkLimit = currentChunk.toAddress().add(currentChunk.size);
    }

    private static void updateFreeMemory() {
        freeMemory = 0;
        FreeChunkHolder freeChunkPtr = currentChunkPointer;
        for (int i = 0; i < freeChunks; ++i) {
            freeMemory += freeChunkPtr.value.size;
            freeChunkPtr = Structure.add(FreeChunkHolder.class, freeChunkPtr, 1);
        }
    }

    private static void sortFreeChunks(int lower, int upper) {
        int start = lower;
        int end = upper;
        int mid = (lower + upper) / 2;

        FreeChunk midChunk = getFreeChunk(mid).value;
        outer: while (true) {
            while (true) {
                if (lower == upper) {
                    break outer;
                }
                if (getFreeChunk(lower).value.size <= midChunk.size) {
                    break;
                }
                ++lower;
            }
            while (true) {
                if (lower == upper) {
                    break outer;
                }
                if (getFreeChunk(upper).value.size > midChunk.size) {
                    break;
                }
                --upper;
            }
            FreeChunk tmp = getFreeChunk(lower).value;
            getFreeChunk(lower).value = getFreeChunk(upper).value;
            getFreeChunk(upper).value = tmp;
        }

        if (lower - start > 0) {
            sortFreeChunks(start, lower);
        }
        if (end - lower - 1 > 0) {
            sortFreeChunks(lower + 1, end);
        }
    }

    private static FreeChunkHolder getFreeChunk(int index) {
        return Structure.add(FreeChunkHolder.class, currentChunkPointer, index);
    }

    private static int objectSize(FreeChunk object) {
        if (object.classReference == 0) {
            return object.size;
        } else {
            RuntimeClass cls = RuntimeClass.getClass(object.toAddress().toStructure());
            if (cls.itemType == null) {
                return cls.size;
            } else {
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
        }
    }

    private static boolean isMarked(RuntimeObject object) {
        return (object.classReference & RuntimeObject.GC_MARKED) != 0;
    }

    static class Region extends Structure {
        short start;
    }
}
