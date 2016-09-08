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
import org.teavm.interop.NoGC;
import org.teavm.interop.StaticInit;
import org.teavm.interop.Structure;

@NoGC
@StaticInit
public final class GC {
    private GC() {
    }

    static Address currentChunkLimit;
    static FreeChunk currentChunk;
    static FreeChunkHolder currentChunkPointer;
    static int freeChunks;

    private static native Address gcStorageAddress();

    private static native Address heapAddress();

    private static native Region regionsAddress();

    private static native int regionMaxCount();

    private static native int availableBytes();

    private static native int regionSize();

    static {
        currentChunk = heapAddress().toStructure();
        currentChunk.classReference = 0;
        currentChunk.size = availableBytes();
        currentChunkLimit = currentChunk.toAddress().add(currentChunk.size);
        currentChunkPointer = gcStorageAddress().toStructure();
        currentChunkPointer.value = currentChunk;
        freeChunks = 1;
        getAvailableChunkIfPossible(0);
    }

    public static RuntimeObject alloc(int size) {
        FreeChunk current = currentChunk;
        Address next = currentChunk.toAddress().add(size);
        if (!next.add(Structure.sizeOf(FreeChunk.class) + 1).isLessThan(currentChunkLimit)) {
            getAvailableChunk(size);
        }
        int oldSize = current.size;
        Address result = current.toAddress();
        currentChunk = next.toStructure();
        currentChunk.classReference = 0;
        currentChunk.size = oldSize - size;
        return result.toStructure();
    }

    private static void getAvailableChunk(int size) {
        if (getAvailableChunkIfPossible(size)) {
            return;
        }
        collectGarbage(size);
        getAvailableChunkIfPossible(size);
    }

    private static boolean getAvailableChunkIfPossible(int size) {
        while (!currentChunk.toAddress().add(size).isLessThan(currentChunkLimit)) {
            if (--size == 0) {
                return false;
            }
            currentChunkPointer = currentChunkPointer.toAddress().add(FreeChunkHolder.class, 1).toStructure();
            currentChunk = currentChunkPointer.value;
            currentChunkLimit = currentChunk.toAddress().add(currentChunk.size);
        }
        return false;
    }

    private static boolean collectGarbage(int size) {
        mark();
        return false;
    }

    private static void mark() {
        MarkQueue.init();
        Allocator.fillZero(regionsAddress().toAddress(), regionMaxCount() * Structure.sizeOf(Region.class));

        Address staticRoots = Mutator.getStaticGcRoots();
        int staticCount = staticRoots.getInt();
        staticRoots.add(8);
        while (staticCount-- > 0) {
            RuntimeObject object = staticRoots.getAddress().toStructure();
            if (object != null) {
                mark(object);
            }
        }

        for (Address stackRoots = Mutator.getStackGcRoots(); stackRoots != null;
                stackRoots = Mutator.getNextStackRoots(stackRoots)) {
            int count = Mutator.getStackRootCount(stackRoots);
            Address stackRootsPtr = Mutator.getStackRootPointer(stackRoots);
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

        MarkQueue.enqueue(object);
        while (!MarkQueue.isEmpty()) {
            object = MarkQueue.dequeue();
            if (isMarked(object)) {
                continue;
            }
            object.classReference |= RuntimeObject.GC_MARKED;

            long offset = object.toAddress().toLong() - heapAddress().toLong();
            Region region = regionsAddress().toAddress().add(Region.class, (int) (offset /  regionSize()))
                    .toStructure();
            short relativeOffset = (short) (offset % regionSize() + 1);
            if (region.start == 0 || region.start > relativeOffset) {
                region.start = relativeOffset;
            }

            RuntimeClass cls = RuntimeClass.getClass(object);
            while (cls != null) {
                Address layout = cls.layout;
                if (layout != null) {
                    short fieldCount = layout.getShort();
                    while (fieldCount-- > 0) {
                        layout = layout.add(2);
                        int fieldOffset = layout.getShort();
                        RuntimeObject reference = object.toAddress().add(fieldOffset).toStructure();
                        if (reference != null && !isMarked(reference)) {
                            MarkQueue.enqueue(reference);
                        }
                    }
                }
                cls = cls.parent;
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
