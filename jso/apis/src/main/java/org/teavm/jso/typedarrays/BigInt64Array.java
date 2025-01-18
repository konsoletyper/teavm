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

import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.jso.JSClass;
import org.teavm.jso.JSIndexer;

@JSClass
public class BigInt64Array extends ArrayBufferView {
    public BigInt64Array(int length) {
    }

    public BigInt64Array(ArrayBuffer buffer) {
    }

    public BigInt64Array(ArrayBufferView buffer) {
    }

    public BigInt64Array(ArrayBuffer buffer, int offset, int length) {
    }

    public BigInt64Array(ArrayBuffer buffer, int offset) {
    }

    @JSIndexer
    public native long get(int index);

    @JSIndexer
    public native void set(int index, long value);

    @Override
    public native void set(@JSByRef long[] data, int offset);

    @Override
    public native void set(@JSByRef long[] data);

    @JSBody(params = "array", script = "return array;")
    public static native BigInt64Array fromJavaArray(@JSByRef long[] array);
}
