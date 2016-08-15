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
package org.teavm.wasm;

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
        byte a = data.getByte();
        byte b = data.add(1).getByte();
        byte c = data.add(2).getByte();
        byte d = data.add(3).getByte();
        return (a << 24) | (b << 16) | (c << 8) | d;
    }

    private static long getLong(Address data) {
        long a = data.getByte();
        long b = data.add(1).getByte();
        long c = data.add(2).getByte();
        long d = data.add(3).getByte();
        long e = data.add(4).getByte();
        long f = data.add(5).getByte();
        long g = data.add(6).getByte();
        long h = data.add(7).getByte();
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
        } while ((value & 0x80) != 0);
        offset = index;
        return result;
    }

    @Import(name = "print", module = "spectest")
    public static native void print(int a);
}
