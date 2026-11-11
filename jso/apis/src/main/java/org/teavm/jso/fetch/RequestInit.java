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
import org.teavm.jso.abort.AbortSignal;
import org.teavm.jso.core.JSMapLike;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.TypedArray;

/**
 * The properties that can be passed to {@link Fetch#fetch(String, RequestInit)}.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/RequestInit">MDN Reference</a>
 */
@JSClass(transparent = true)
public abstract class RequestInit implements JSObject {
    private RequestInit() {
    }

    public static RequestInit create() {
        return JSObjects.create();
    }

    @JSProperty
    public native void setMethod(String method);

    @JSProperty
    public native void setBody(String type);

    @JSProperty
    public native void setBody(ArrayBuffer body);

    @JSProperty
    public native void setBody(TypedArray body);

    @JSProperty
    public native void setHeaders(Headers headers);

    @JSProperty
    public native void setHeaders(JSMapLike<String> headers);

    @JSProperty
    public native void setSignal(AbortSignal signal);
}
