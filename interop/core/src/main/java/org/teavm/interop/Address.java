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

public final class Address {
    public final native Address add(int offset);

    public final native Address add(long offset);

    public final native int toInt();

    public final native long toLong();

    public final native <T extends Structure> T toStructure();

    public final native byte getByte();

    public final native void putByte(byte value);

    public final native char getChar();

    public final native void putChar(char value);

    public final native short getShort();

    public final native void putShort(short value);

    public final native int getInt();

    public final native void putInt(int value);

    public final native long getLong();

    public final native void putLong(long value);

    public final native float getFloat();

    public final native void putFloat(float value);

    public final native double getDouble();

    public final native void putDouble(double value);

    public static native Address fromInt(int value);

    public static native Address fromLong(long value);
}
