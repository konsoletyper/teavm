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
import org.teavm.jso.JSClass;

@JSClass
public class DataView extends ArrayBufferView {
    public DataView(ArrayBuffer buffer) {
    }

    public DataView(ArrayBufferView buffer) {
    }

    public DataView(ArrayBuffer buffer, int offset, int length) {
    }

    public DataView(ArrayBuffer buffer, int offset) {
    }

    public native byte getInt8(int byteOffset);

    public native short getUint8(int byteOffset);

    public native short getInt16(int byteOffset);

    public native short getInt16(int byteOffset, boolean littleEndian);

    public native int getUint16(int byteOffset);

    public native int getUint16(int byteOffset, boolean littleEndian);

    public native int getInt32(int byteOffset);

    public native int getInt32(int byteOffset, boolean littleEndian);

    public native int getUint32(int byteOffset);

    public native int getUint32(int byteOffset, boolean littleEndian);

    public native long getBigInt64(int byteOffset);

    public native long getBigInt64(int byteOffset, boolean littleEndian);

    public native long getBigUint64(int byteOffset);

    public native long getBigUint64(int byteOffset, boolean littleEndian);

    public native float getFloat32(int byteOffset);

    public native float getFloat32(int byteOffset, boolean littleEndian);

    public native double getFloat64(int byteOffset);

    public native double getFloat64(int byteOffset, boolean littleEndian);

    public native void setInt8(int byteOffset, int value);

    public native void setUint8(int byteOffset, int value);

    public native void setInt16(int byteOffset, int value);

    public native void setInt16(int byteOffset, int value, boolean littleEndian);

    public native void setUint16(int byteOffset, int value);

    public native void setUint16(int byteOffset, int value, boolean littleEndian);

    public native void setInt32(int byteOffset, int value);

    public native void setInt32(int byteOffset, int value, boolean littleEndian);

    public native void setBigInt64(int byteOffset, long value);

    public native void setBigInt64(int byteOffset, long value, boolean littleEndian);

    public native void setBigUint64(int byteOffset, long value);

    public native void setBigUint64(int byteOffset, long value, boolean littleEndian);

    public native void setUint32(int byteOffset, int value);

    public native void setUint32(int byteOffset, int value, boolean littleEndian);

    public native void setFloat32(int byteOffset, float value);

    public native void setFloat32(int byteOffset, float value, boolean littleEndian);

    public native void setFloat64(int byteOffset, double value);

    public native void setFloat64(int byteOffset, double value, boolean littleEndian);

    @JSBody(params = "buffer", script = "return new DataView(buffer);")
    @Deprecated
    public static native DataView create(ArrayBuffer buffer);

    @JSBody(params = "buffer", script = "return new DataView(buffer);")
    public static native DataView create(ArrayBufferView buffer);

    @JSBody(params = { "buffer", "offset", "length" }, script = "return new DataView(buffer, offset, length);")
    public static native DataView create(ArrayBuffer buffer, int offset, int length);

    @JSBody(params = { "buffer", "offset" }, script = "return new DataView(buffer, offset);")
    public static native DataView create(ArrayBuffer buffer, int offset);
}
