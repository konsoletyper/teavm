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
package org.teavm.jso.abort;

import org.teavm.jso.JSClass;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 * The {@link AbortController} interface represents a controller object that allows you to abort one or more Web
 * requests as and when desired.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/AbortController">MDN Reference</a>
 */
@JSClass
public class AbortController implements JSObject {
    public AbortController() {
    }

    @JSProperty
    public native AbortSignal getSignal();

    public native void abort();

    public native void abort(String reason);

    public native void abort(JSObject reason);
}
