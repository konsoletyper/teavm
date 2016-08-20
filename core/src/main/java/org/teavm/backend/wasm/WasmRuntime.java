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
package org.teavm.backend.wasm;

import org.teavm.interop.Address;
import org.teavm.interop.Import;

public final class WasmRuntime {
    private static Address offset;

    private WasmRuntime() {
    }

    public static int compare(int a, int b) {
        return gt(a, b) ? 1 : lt(a, b) ? -1 : 0;
    }

    public static int compare(long a, long b) {
        return gt(a, b) ? 1 : lt(a, b) ? -1 : 0;
    }

    public static int compare(float a, float b) {
        return gt(a, b) ? 1 : lt(a, b) ? -1 : 0;
    }

    public static int compare(double a, double b) {
        return gt(a, b) ? 1 : lt(a, b) ? -1 : 0;
    }

    public static float remainder(float a, float b) {
        return a - (float) (int) (a / b) * b;
    }

    public static double remainder(double a, double b) {
        return a - (double) (long) (a / b) * b;
    }

    private static native boolean lt(int a, int b);

    private static native boolean gt(int a, int b);

    private static native boolean lt(long a, long b);

    private static native boolean gt(long a, long b);

    private static native boolean lt(float a, float b);

    private static native boolean gt(float a, float b);

    private static native boolean lt(double a, double b);

    private static native boolean gt(double a, double b);

    public static void decodeData(Address data, Address metadata) {
        offset = metadata;
        int count = decodeLeb();

        while (count-- > 0) {
            data = decodeDataRec(data);
        }
    }

    private static Address decodeDataRec(Address data) {
        Address metadata = offset;
        int type = metadata.getByte();
        metadata = metadata.add(1);

        switch (type) {
            case 0:
                data = data.add(1);
                break;
            case 1: {
                data = align(data, 2);
                byte a = data.getByte();
                byte b = data.add(1).getByte();
                int value = (a << 8) | b;
                data.putShort((short) value);
                data = data.add(2);
                break;
            }
            case 2:
            case 4:
            case 6: {
                data = align(data, 4);
                int value = getInt(data);
                data.putInt(value);
                data = data.add(4);
                break;
            }
            case 3:
            case 5: {
                data = align(data, 8);
                data.putLong(getLong(data));
                data = data.add(8);
                break;
            }
            case 7: {
                offset = metadata;
                int size = decodeLeb();
                metadata = offset;
                while (size-- > 0) {
                    offset = metadata;
                    data = decodeDataRec(data);
                }
                metadata = offset;
                break;
            }
            case 8: {
                int alignment = metadata.getByte();
                offset = metadata.add(1);
                int size = decodeLeb();
                if (alignment > 0) {
                    data = align(data, alignment);
                }
                while (size-- > 0) {
                    data = decodeDataRec(data);
                }
                metadata = offset;
                break;
            }
            case 9: {
                Address start = metadata.add(-1);
                offset = metadata;
                int back = decodeLeb();
                metadata = offset;
                offset = start.add(-back);
                data = decodeDataRec(data);
                break;
            }
        }

        offset = metadata;
        return data;
    }

    private static int getInt(Address data) {
        int a = data.getByte() & 0xFF;
        int b = data.add(1).getByte() & 0xFF;
        int c = data.add(2).getByte() & 0xFF;
        int d = data.add(3).getByte() & 0xFF;
        return (a << 24) | (b << 16) | (c << 8) | d;
    }

    private static long getLong(Address data) {
        long a = data.getByte() & 0xFF;
        long b = data.add(1).getByte() & 0xFF;
        long c = data.add(2).getByte() & 0xFF;
        long d = data.add(3).getByte() & 0xFF;
        long e = data.add(4).getByte() & 0xFF;
        long f = data.add(5).getByte() & 0xFF;
        long g = data.add(6).getByte() & 0xFF;
        long h = data.add(7).getByte() & 0xFF;
        return (a << 56) | (b << 48) | (c << 40) | (d << 32) | (e << 24) | (f << 16) | (g << 8) | h;
    }

    private static Address align(Address address, int alignment) {
        int value = address.toInt();
        if (value == 0) {
            return address;
        }
        value = ((value - 1) / alignment + 1) * alignment;
        return Address.fromInt(value);
    }

    private static int decodeLeb() {
        Address index = offset;
        int result = 0;
        int shift = 0;
        int value;
        do {
            value = index.getByte();
            index = index.add(1);
            result |= (value & 0x7F) << shift;
            shift += 7;
        } while ((value & 0x80) != 0);
        offset = index;
        return result;
    }

    @Import(name = "print", module = "spectest")
    public static native void print(int a);

    public static void fillZero(Address address, int count) {
        int start = address.toInt();

        int alignedStart = start >>> 2 << 2;
        address = Address.fromInt(alignedStart);
        switch (start - alignedStart) {
            case 0:
                address.putInt(0);
                break;
            case 1:
                address.add(1).putByte((byte) 0);
                address.add(2).putShort((short) 0);
                break;
            case 2:
                address.add(2).putShort((short) 0);
                break;
            case 3:
                address.add(3).putByte((byte) 0);
                break;
        }

        int end = start + count;
        int alignedEnd = end >>> 2 << 2;
        address = Address.fromInt(alignedEnd);
        switch (end - alignedEnd) {
            case 0:
                break;
            case 1:
                address.putByte((byte) 0);
                break;
            case 2:
                address.putShort((short) 0);
                break;
            case 3:
                address.putShort((short) 0);
                address.add(2).putByte((byte) 0);
                break;
        }

        for (address = Address.fromInt(alignedStart); address.toInt() < alignedEnd; address = address.add(4)) {
            address.putInt(0);
        }
    }
}
