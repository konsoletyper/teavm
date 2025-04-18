/*
 *  Copyright 2015 Alexey Andreev.
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

import java.nio.Buffer;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSBuffer;
import org.teavm.jso.JSBufferType;
import org.teavm.jso.JSByRef;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSIndexer;

@JSClass
public class Uint32Array extends TypedArray {
    public Uint32Array(int length) {
    }

    public Uint32Array(ArrayBuffer buffer) {
    }

    public Uint32Array(TypedArray buffer) {
    }

    public Uint32Array(ArrayBuffer buffer, int offset, int length) {
    }

    public Uint32Array(ArrayBuffer buffer, int offset) {
    }

    @JSIndexer
    public native int get(int index);

    @JSIndexer
    public native void set(int index, int value);

    @Override
    public native void set(@JSByRef(optional = true) int[] data, int offset);

    @Override
    public native void set(@JSByRef(optional = true) int[] data);

    @JSBody(params = "array", script = "return new Uint32Array(array.buffer, array.byteOffset, array.length);")
    public static native Uint32Array fromJavaArray(@JSByRef int[] array);

    @JSBody(params = "array", script = "return new Uint32Array(array.buffer, array.byteOffset, array.length);")
    public static native Uint32Array fromJavaBuffer(@JSBuffer(JSBufferType.INT32) Buffer buffer);

    @JSBody(params = "array", script = "return new Uint32Array(array.buffer, array.byteOffset, array.length);")
    public static native Uint32Array copyFromJavaArray(@JSByRef(optional = true) int[] array);

    @JSBody(script = "return new Int32Array(this.buffer, this.byteOffset, this.length);")
    @JSByRef(optional = true)
    public native int[] copyToJavaArray();

    @JSBody(script = "return new Int32Array(this.buffer, this.byteOffset, this.length);")
    @JSByRef
    public native int[] toJavaArray();
}
