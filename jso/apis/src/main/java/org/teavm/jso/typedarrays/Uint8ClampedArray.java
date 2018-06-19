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

import org.teavm.jso.JSBody;
import org.teavm.jso.JSIndexer;

public abstract class Uint8ClampedArray extends ArrayBufferView {
    @JSIndexer
    public abstract short get(int index);

    @JSIndexer
    public abstract void set(int index, int value);

    @JSBody(params = "length", script = "return new Uint8ClampedArray(length);")
    public static native Uint8ClampedArray create(int length);

    @JSBody(params = "buffer", script = "return new Uint8ClampedArray(buffer);")
    public static native Uint8ClampedArray create(ArrayBuffer buffer);

    @JSBody(params = "buffer", script = "return new Uint8ClampedArray(buffer);")
    public static native Uint8ClampedArray create(ArrayBufferView buffer);

    @JSBody(params = { "buffer", "offset", "length" }, script = "return new "
            + "Uint8ClampedArray(buffer, offset, length);")
    public static native Uint8ClampedArray create(ArrayBuffer buffer, int offset, int length);

    @JSBody(params = { "buffer", "offset" }, script = "return new Uint8ClampedArray(buffer, offset);")
    public static native Uint8ClampedArray create(ArrayBuffer buffer, int offset);
}
