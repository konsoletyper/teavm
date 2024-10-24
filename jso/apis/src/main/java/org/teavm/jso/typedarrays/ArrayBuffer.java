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
package org.teavm.jso.typedarrays;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

@JSClass
public class ArrayBuffer implements JSObject {
    public ArrayBuffer() {
    }

    public ArrayBuffer(int length) {
    }

    @JSProperty
    public native int getByteLength();

    public native ArrayBuffer slice(int begin, int end);

    @JSBody(params = "length", script = "return new ArrayBuffer(length);")
    @Deprecated
    public static native ArrayBuffer create(int length);

    @JSBody(script = "return new Int8Array(this);")
    @JSByRef
    public native byte[] asByteArray();

    @JSBody(script = "return new Int16Array(this);")
    @JSByRef
    public native short[] asShortArray();

    @JSBody(script = "return new Int32Array(this);")
    @JSByRef
    public native int[] asIntArray();

    @JSBody(script = "return new Int64Array(this);")
    @JSByRef
    public native long[] asLongArray();

    @JSBody(script = "return new Float32Array(this);")
    @JSByRef
    public native float[] asFloatArray();

    @JSBody(script = "return new Double32Array(this);")
    @JSByRef
    public native double[] asDoubleArray();

    @JSBody(params = "array", script = "return array.buffer;")
    public static native ArrayBuffer from(@JSByRef byte[] array);

    @JSBody(params = "array", script = "return array.buffer;")
    public static native ArrayBuffer from(@JSByRef short[] array);

    @JSBody(params = "array", script = "return array.buffer;")
    public static native ArrayBuffer from(@JSByRef int[] array);

    @JSBody(params = "array", script = "return array.buffer;")
    public static native ArrayBuffer from(@JSByRef long[] array);

    @JSBody(params = "array", script = "return array.buffer;")
    public static native ArrayBuffer from(@JSByRef float[] array);

    @JSBody(params = "array", script = "return array.buffer;")
    public static native ArrayBuffer from(@JSByRef double[] array);

    public static ArrayBuffer from(@JSByRef ByteBuffer buffer) {
        return from(buffer.array());
    }

    public static ArrayBuffer from(@JSByRef ShortBuffer buffer) {
        return from(buffer.array());
    }

    public static ArrayBuffer from(@JSByRef IntBuffer buffer) {
        return from(buffer.array());
    }

    public static ArrayBuffer from(@JSByRef LongBuffer buffer) {
        return from(buffer.array());
    }

    public static ArrayBuffer from(@JSByRef FloatBuffer buffer) {
        return from(buffer.array());
    }

    public static ArrayBuffer from(@JSByRef DoubleBuffer buffer) {
        return from(buffer.array());
    }
}
