/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.backend.wasm;

import org.teavm.interop.Address;
import org.teavm.interop.Import;
import org.teavm.interop.StaticInit;
import org.teavm.interop.Unmanaged;

@StaticInit
@Unmanaged
public final class WasmHeap {
    public static final int PAGE_SIZE = 65536;
    public static final int DEFAULT_STACK_SIZE = PAGE_SIZE * 4;
    public static final int DEFAULT_REGION_SIZE = 1024;

    public static int minHeapSize;
    public static int maxHeapSize;
    public static Address storageAddress;
    public static int storageSize;
    public static Address regionsAddress;
    public static int regionsCount;
    public static int regionsSize;
    public static Address cardTable;
    public static Address heapAddress;
    public static int heapSize;
    public static int regionSize = DEFAULT_REGION_SIZE;
    public static Address memoryLimit;
    public static Address stackAddress;
    public static Address stack;
    public static int stackSize;

    private WasmHeap() {
    }

    public static int calculateStorageSize(int heapSize) {
        return heapSize / 16;
    }

    public static int calculateRegionsCount(int heapSize, int regionSize) {
        return (heapSize / regionSize) + 1;
    }

    public static int calculateRegionsSize(int regionsCount) {
        return regionsCount * 2;
    }

    public static native void growMemory(int amount);

    @Import(name = "init", module = "teavmHeapTrace")
    private static native void initHeapTrace(int maxHeap);

    public static void initHeap(Address start, int minHeap, int maxHeap, int stackSize) {
        initHeapTrace(maxHeap);
        stackAddress = start;
        stack = start;
        heapAddress = WasmRuntime.align(stackAddress.add(stackSize), 16);
        memoryLimit = WasmRuntime.align(start, PAGE_SIZE);
        minHeapSize = minHeap;
        maxHeapSize = maxHeap;
        WasmHeap.stackSize = stackSize;
        resizeHeap(minHeap);
    }

    public static void resizeHeap(int newHeapSize) {
        if (newHeapSize <= heapSize) {
            return;
        }

        int newStorageSize = calculateStorageSize(newHeapSize);
        int newRegionsCount = calculateRegionsCount(newHeapSize, regionSize);
        int newRegionsSize = calculateRegionsSize(newRegionsCount);

        Address newRegionsAddress = WasmRuntime.align(heapAddress.add(newHeapSize), 16);
        Address newCardTable = WasmRuntime.align(newRegionsAddress.add(newRegionsSize), 16);
        Address newStorageAddress = WasmRuntime.align(newCardTable.add(newRegionsCount), 16);
        Address newMemoryLimit = WasmRuntime.align(newStorageAddress.add(newStorageSize), PAGE_SIZE);
        if (newMemoryLimit != memoryLimit) {
            growMemory((int) (newMemoryLimit.toLong() - memoryLimit.toLong()) / PAGE_SIZE);
            memoryLimit = newMemoryLimit;
        }
        if (storageSize > 0) {
            WasmRuntime.moveMemoryBlock(storageAddress, newStorageAddress, storageSize);
        }
        if (regionsSize > 0) {
            WasmRuntime.moveMemoryBlock(cardTable, newCardTable, regionsCount);
            WasmRuntime.moveMemoryBlock(regionsAddress, newRegionsAddress, regionsSize);
        }

        storageAddress = newStorageAddress;
        regionsAddress = newRegionsAddress;
        cardTable = newCardTable;
        storageSize = newStorageSize;
        regionsCount = newRegionsCount;
        regionsSize = newRegionsSize;
        heapSize = newHeapSize;
    }
}
