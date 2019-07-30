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
package org.teavm.interop;

@StaticInit
@Unmanaged
public final class Address {
    public native Address add(int offset);

    public native Address add(long offset);

    public native boolean isLessThan(Address other);

    public native int toInt();

    public native long toLong();

    public native <T extends Structure> T toStructure();

    public native byte getByte();

    public native void putByte(byte value);

    public native char getChar();

    public native void putChar(char value);

    public native short getShort();

    public native void putShort(short value);

    public native int getInt();

    public native void putInt(int value);

    public native long getLong();

    public native void putLong(long value);

    public native float getFloat();

    public native void putFloat(float value);

    public native double getDouble();

    public native void putDouble(double value);

    public native Address getAddress();

    public native void putAddress(Address value);

    public static native Address fromInt(int value);

    public static native Address fromLong(long value);

    public static native Address ofObject(Object obj);

    public static native Address ofData(byte[] data);

    public static native Address ofData(char[] data);

    public static native Address ofData(short[] data);

    public static native Address ofData(int[] data);

    public static native Address ofData(long[] data);

    public static native Address ofData(float[] data);

    public static native Address ofData(double[] data);

    public static native Address ofData(Object[] data);

    public static native Address align(Address address, int alignment);

    public static native int sizeOf();

    public native Address add(Class<? extends Structure> type, int offset);

    public long diff(Address that) {
        return toLong() - that.toLong();
    }
}
