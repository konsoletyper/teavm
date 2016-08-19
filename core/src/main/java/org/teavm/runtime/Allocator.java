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

@StaticInit
public final class Allocator {
    private static Address address = initialize();

    private static native Address initialize();

    public static Address allocate(RuntimeClass tag) {
        Address result = address;
        address = result.add(tag.size);
        fillZero(result, tag.size);
        RuntimeObject object = result.toStructure();
        object.classReference = tag.toAddress().toInt() >> 3;
        return result;
    }

    public static Address allocateArray(RuntimeClass tag, int size) {
        Address result = address;
        int sizeInBytes = tag.itemType.size * size + Structure.sizeOf(RuntimeArray.class);
        address = result.add(sizeInBytes);
        fillZero(result, sizeInBytes);

        RuntimeArray array = result.toStructure();
        array.classReference = tag.toAddress().toInt() >> 3;
        array.size = size;

        return result;
    }

    public static native void fillZero(Address address, int count);
}
