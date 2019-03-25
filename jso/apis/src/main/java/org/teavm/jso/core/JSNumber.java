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
package org.teavm.jso.core;

import org.teavm.interop.NoSideEffects;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

public abstract class JSNumber implements JSObject {
    private JSNumber() {
    }

    public final double doubleValue() {
        return doubleValue(this);
    }

    @JSBody(params = "number", script = "return number;")
    @NoSideEffects
    private static native double doubleValue(JSNumber number);

    public final int intValue() {
        return intValue(this);
    }

    @JSBody(params = "number", script = "return number;")
    @NoSideEffects
    private static native int intValue(JSNumber number);

    public final char charValue() {
        return charValue(this);
    }

    @JSBody(params = "number", script = "return number;")
    @NoSideEffects
    private static native char charValue(JSNumber number);

    public final byte byteValue() {
        return byteValue(this);
    }

    @JSBody(params = "number", script = "return number;")
    @NoSideEffects
    private static native byte byteValue(JSNumber number);

    public final short shortValue() {
        return shortValue(this);
    }

    @JSBody(params = "number", script = "return number;")
    @NoSideEffects
    private static native short shortValue(JSNumber number);

    public final float floatValue() {
        return floatValue(this);
    }

    @JSBody(params = "number", script = "return number;")
    @NoSideEffects
    private static native float floatValue(JSNumber number);

    @JSBody(params = "value", script = "return value;")
    @NoSideEffects
    public static native JSNumber valueOf(byte value);

    @JSBody(params = "value", script = "return value;")
    @NoSideEffects
    public static native JSNumber valueOf(short value);

    @JSBody(params = "value", script = "return value;")
    @NoSideEffects
    public static native JSNumber valueOf(int value);

    @JSBody(params = "value", script = "return value;")
    @NoSideEffects
    public static native JSNumber valueOf(char value);

    @JSBody(params = "value", script = "return value;")
    @NoSideEffects
    public static native JSNumber valueOf(float value);

    @JSBody(params = "value", script = "return value;")
    @NoSideEffects
    public static native JSNumber valueOf(double value);
}
