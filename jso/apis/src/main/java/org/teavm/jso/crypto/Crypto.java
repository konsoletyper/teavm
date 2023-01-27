/*
 *  Copyright 2022 ihromant.
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
package org.teavm.jso.crypto;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.typedarrays.Int16Array;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.typedarrays.Uint16Array;
import org.teavm.jso.typedarrays.Uint8Array;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

public abstract class Crypto implements JSObject {
    @JSBody(script = "return window.crypto != null;")
    public static native boolean isSupported();

    @JSBody(script = "return window.crypto;")
    public static native Crypto current();

    public abstract String randomUUID();

    public abstract void getRandomValues(Int8Array arr);

    public abstract void getRandomValues(Uint8Array arr);

    public abstract void getRandomValues(Uint8ClampedArray arr);

    public abstract void getRandomValues(Int16Array arr);

    public abstract void getRandomValues(Uint16Array arr);

    public abstract void getRandomValues(Int32Array arr);
}
