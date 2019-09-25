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
package org.teavm.jso.core;

import org.teavm.interop.NoSideEffects;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

public abstract class JSBoolean implements JSObject {
    private JSBoolean() {
    }

    public final boolean booleanValue() {
        return booleanValue(this);
    }

    @JSBody(params = "value", script = "return value;")
    @NoSideEffects
    private static native boolean booleanValue(JSBoolean value);

    @JSBody(params = "value", script = "return value;")
    @NoSideEffects
    public static native JSBoolean valueOf(boolean value);
}
