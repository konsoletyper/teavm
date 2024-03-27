/*
 *  Copyright 2024 ihromant.
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
package org.teavm.jso.file;

import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.streams.ReadableStream;
import org.teavm.jso.typedarrays.ArrayBuffer;

@JSClass
public class JSBlob implements JSObject {
    public JSBlob(JSArray<?> array) {

    }

    public JSBlob(JSArray<?> array, JSObject options) {

    }

    @JSProperty
    public native int getSize();

    @JSProperty
    public native String getType();

    public native JSPromise<ArrayBuffer> arrayBuffer();

    public native JSBlob slice();

    public native JSBlob slice(int start);

    public native JSBlob slice(int start, int end);

    public native JSBlob slice(int start, int end, String contentType);

    public native ReadableStream stream();

    public native JSPromise<String> text();
}
