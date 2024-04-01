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
package org.teavm.jso.streams;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.dom.events.EventTarget;

public abstract class AbortSignal implements EventTarget {
    @JSProperty
    public native boolean isAborted();

    @JSProperty
    public native JSObject getReason();

    @JSBody(script = "return AbortSignal.abort();")
    public static native PipeOptions abort();

    @JSBody(params = "reason", script = "return AbortSignal.abort(reason);")
    public static native PipeOptions abort(JSObject reason);

    @JSBody(params = "iterable", script = "return AbortSignal(iterable);")
    public static native PipeOptions any(JSArray<?> iterable);

    @JSBody(params = "millis", script = "return AbortSignal.timeout(millis);")
    public static native PipeOptions timeout(double millis);
}
