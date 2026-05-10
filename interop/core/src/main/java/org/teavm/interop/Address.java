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
    @Intrinsified
    public native Address add(int offset);

    @Intrinsified
    public native Address add(long offset);

    @Intrinsified
    public native boolean isLessThan(Address other);

    @Intrinsified
    public native int toInt();

    @Intrinsified
    public native long toLong();

    @Intrinsified
    public native <T extends Structure> T toStructure();

    @Intrinsified
    public native byte getByte();

    @Intrinsified
    public native void putByte(byte value);

    @Intrinsified
    public native char getChar();

    @Intrinsified
    public native void putChar(char value);

    @Intrinsified
    public native short getShort();

    @Intrinsified
    public native void putShort(short value);

    @Intrinsified
    public native int getInt();

    @Intrinsified
    public native void putInt(int value);

    @Intrinsified
    public native long getLong();

    @Intrinsified
    public native void putLong(long value);

    @Intrinsified
    public native float getFloat();

    @Intrinsified
    public native void putFloat(float value);

    @Intrinsified
    public native double getDouble();

    @Intrinsified
    public native void putDouble(double value);

    @Intrinsified
    public native Address getAddress();

    @Intrinsified
    public native void putAddress(Address value);

    @Intrinsified
    public static native Address fromInt(int value);

    @Intrinsified
    public static native Address fromLong(long value);

    @Intrinsified
    public static native Address ofObject(Object obj);

    @Intrinsified
    public static native Address ofData(byte[] data);

    @Intrinsified
    public static native Address ofData(char[] data);

    @Intrinsified
    public static native Address ofData(short[] data);

    @Intrinsified
    public static native Address ofData(int[] data);

    @Intrinsified
    public static native Address ofData(long[] data);

    @Intrinsified
    public static native Address ofData(float[] data);

    @Intrinsified
    public static native Address ofData(double[] data);

    @Intrinsified
    public static native Address ofData(Object[] data);

    @Intrinsified
    public static native Address align(Address address, int alignment);

    @Intrinsified
    public static native int sizeOf();

    @Intrinsified
    public native Address add(Class<? extends Structure> type, int offset);

    @Intrinsified
    public long diff(Address that) {
        return toLong() - that.toLong();
    }

    @Intrinsified
    public static native void pin(Object obj);

    @Intrinsified
    public static native void fillZero(Address address, int count);

    @Intrinsified
    public static native void fill(Address address, byte value, int count);

    @Intrinsified
    public static native void moveMemoryBlock(Address source, Address target, int count);
}
