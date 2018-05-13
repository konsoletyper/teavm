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

@StaticInit
public final class Allocator {
    private Allocator() {
    }

    public static Address allocate(RuntimeClass tag) {
        RuntimeObject object = GC.alloc(tag.size);
        fillZero(object.toAddress(), tag.size);
        object.classReference = tag.pack();
        return object.toAddress();
    }

    public static Address allocateArray(RuntimeClass tag, int size) {
        int itemSize = (tag.itemType.flags & RuntimeClass.PRIMITIVE) != 0 ? tag.itemType.size : Address.sizeOf();
        int sizeInBytes = Address.align(Address.fromInt(Structure.sizeOf(RuntimeArray.class)), itemSize).toInt();
        sizeInBytes += itemSize * size;
        sizeInBytes = Address.align(Address.fromInt(sizeInBytes), Address.sizeOf()).toInt();
        Address result = GC.alloc(sizeInBytes).toAddress();
        fillZero(result, sizeInBytes);

        RuntimeArray array = result.toStructure();
        array.classReference = tag.pack();
        array.size = size;

        return result;
    }

    @Unmanaged
    public static RuntimeArray allocateMultiArray(RuntimeClass tag, Address dimensions, int dimensionCount) {
        int size = dimensions.getInt();
        RuntimeArray array = allocateArray(tag, dimensions.getInt()).toStructure();
        if (dimensionCount > 1) {
            Address arrayData = Structure.add(RuntimeArray.class, array, 1).toAddress();
            arrayData = Address.align(arrayData, Address.sizeOf());
            for (int i = 0; i < size; ++i) {
                RuntimeArray innerArray = allocateMultiArray(tag.itemType, dimensions.add(4), dimensionCount - 1);
                arrayData.putAddress(innerArray.toAddress());
                arrayData = arrayData.add(Address.sizeOf());
            }
        }
        return array;
    }

    @Unmanaged
    public static native void fillZero(Address address, int count);

    @Unmanaged
    public static native void moveMemoryBlock(Address source, Address target, int count);

    @Unmanaged
    public static native boolean isInitialized(Class<?> cls);
}
