/*
 *  Copyright 2026 Jonathan Coates.
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
package org.teavm.jso.fetch;

import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.file.Blob;
import org.teavm.jso.streams.ReadableStream;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Uint8Array;

/**
 * The {@link Response} interface of the Fetch API represents the response to a request.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/Response">MDN Reference</a>
 */
@JSClass
public class Response implements JSObject {
    @JSProperty
    public native int getStatus();

    @JSProperty
    public native String getStatusText();

    @JSProperty
    public native Headers getHeaders();

    @JSProperty
    public native ReadableStream getBody();

    public native JSPromise<ArrayBuffer> arrayBuffer();

    public native JSPromise<Blob> blob();

    public native JSPromise<Uint8Array> bytes();

    public native JSPromise<JSObject> json();

    public native JSPromise<String> text();
}
