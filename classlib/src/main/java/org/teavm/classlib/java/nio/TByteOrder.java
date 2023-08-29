/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.java.nio;

import org.teavm.classlib.PlatformDetector;
import org.teavm.interop.Address;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int16Array;
import org.teavm.jso.typedarrays.Int8Array;

public final class TByteOrder {
    public static final TByteOrder BIG_ENDIAN = new TByteOrder("BIG_ENDIAN");
    public static final TByteOrder LITTLE_ENDIAN = new TByteOrder("LITTLE_ENDIAN");
    private static TByteOrder nativeOrder;
    private String name;

    private TByteOrder(String name) {
        this.name = name;
    }

    public static TByteOrder nativeOrder() {
        if (nativeOrder == null) {
            if (PlatformDetector.isJavaScript()) {
                var buffer = ArrayBuffer.create(2);
                var shortArray = Int16Array.create(buffer);
                shortArray.set(0, (short) 1);
                var byteArray = Int8Array.create(buffer);
                nativeOrder = byteArray.get(0) == 0 ? BIG_ENDIAN : LITTLE_ENDIAN;
            } else {
                var array = new short[1];
                array[0] = 1;
                var firstByte = Address.ofData(array).getByte();
                nativeOrder = firstByte == 0 ? BIG_ENDIAN : LITTLE_ENDIAN;
            }
        }
        return nativeOrder;
    }

    @Override
    public String toString() {
        return name;
    }
}
