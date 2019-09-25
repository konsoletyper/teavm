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
package org.teavm.jso.typedarrays;

import org.teavm.jso.JSBody;

public abstract class DataView extends ArrayBufferView {
    public abstract byte getInt8(int byteOffset);

    public abstract short getUint8(int byteOffset);

    public abstract short getInt16(int byteOffset);

    public abstract short getInt16(int byteOffset, boolean littleEndian);

    public abstract int getUint16(int byteOffset);

    public abstract int getUint16(int byteOffset, boolean littleEndian);

    public abstract int getInt32(int byteOffset);

    public abstract int getInt32(int byteOffset, boolean littleEndian);

    public abstract int getUint32(int byteOffset);

    public abstract int getUint32(int byteOffset, boolean littleEndian);

    public abstract float getFloat32(int byteOffset);

    public abstract float getFloat32(int byteOffset, boolean littleEndian);

    public abstract double getFloat64(int byteOffset);

    public abstract double getFloat64(int byteOffset, boolean littleEndian);

    public abstract void setInt8(int byteOffset, int value);

    public abstract void setUint8(int byteOffset, int value);

    public abstract void setInt16(int byteOffset, int value);

    public abstract void setInt16(int byteOffset, int value, boolean littleEndian);

    public abstract void setUint16(int byteOffset, int value);

    public abstract void setUint16(int byteOffset, int value, boolean littleEndian);

    public abstract void setInt32(int byteOffset, int value);

    public abstract void setInt32(int byteOffset, int value, boolean littleEndian);

    public abstract void setUint32(int byteOffset, int value);

    public abstract void setUint32(int byteOffset, int value, boolean littleEndian);

    public abstract void setFloat32(int byteOffset, float value);

    public abstract void setFloat32(int byteOffset, float value, boolean littleEndian);

    public abstract void setFloat64(int byteOffset, double value);

    public abstract void setFloat64(int byteOffset, double value, boolean littleEndian);

    @JSBody(params = "buffer", script = "return new DataView(buffer);")
    public static native DataView create(ArrayBuffer buffer);

    @JSBody(params = "buffer", script = "return new DataView(buffer);")
    public static native DataView create(ArrayBufferView buffer);

    @JSBody(params = { "buffer", "offset", "length" }, script = "return new DataView(buffer, offset, length);")
    public static native DataView create(ArrayBuffer buffer, int offset, int length);

    @JSBody(params = { "buffer", "offset" }, script = "return new DataView(buffer, offset);")
    public static native DataView create(ArrayBuffer buffer, int offset);
}
