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
import org.teavm.jso.JSIndexer;

public abstract class Int16Array extends ArrayBufferView {
    @JSIndexer
    public abstract short get(int index);

    @JSIndexer
    public abstract void set(int index, short value);

    @JSBody(params = "length", script = "return new Int16Array(length);")
    public static native Int16Array create(int length);

    @JSBody(params = "buffer", script = "return new Int16Array(buffer);")
    public static native Int16Array create(ArrayBuffer buffer);

    @JSBody(params = "buffer", script = "return new Int16Array(buffer);")
    public static native Int16Array create(ArrayBufferView buffer);

    @JSBody(params = { "buffer", "offset", "length" }, script = "return new Int16Array(buffer, offset, length);")
    public static native Int16Array create(ArrayBuffer buffer, int offset, int length);

    @JSBody(params = { "buffer", "offset" }, script = "return new Int16Array(buffer, offset);")
    public static native Int16Array create(ArrayBuffer buffer, int offset);
}
