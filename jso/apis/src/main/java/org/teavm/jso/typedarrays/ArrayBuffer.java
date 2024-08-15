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
}
