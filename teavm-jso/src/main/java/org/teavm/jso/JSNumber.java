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
package org.teavm.jso;

/**
 *
 * @author Alexey Andreev
 */
public abstract class JSNumber implements JSObject {
    private JSNumber() {
    }

    public final double doubleValue() {
        return doubleValue(this);
    }

    @JSExpression(params = "number", expr = "number")
    private static native double doubleValue(JSNumber number);

    public final int intValue() {
        return intValue(this);
    }

    @JSExpression(params = "number", expr = "number")
    private static native int intValue(JSNumber number);

    public final byte byteValue() {
        return byteValue(this);
    }

    @JSExpression(params = "number", expr = "number")
    private static native byte byteValue(JSNumber number);

    public final short shortValue() {
        return shortValue(this);
    }

    @JSExpression(params = "number", expr = "number")
    private static native short shortValue(JSNumber number);

    public final float floatValue() {
        return floatValue(this);
    }

    @JSExpression(params = "number", expr = "number")
    private static native float floatValue(JSNumber number);

    @JSExpression(params = "value", expr = "value")
    public static native JSNumber valueOf(byte value);

    @JSExpression(params = "value", expr = "value")
    public static native JSNumber valueOf(short value);

    @JSExpression(params = "value", expr = "value")
    public static native JSNumber valueOf(int value);

    @JSExpression(params = "value", expr = "value")
    public static native JSNumber valueOf(float value);

    @JSExpression(params = "value", expr = "value")
    public static native JSNumber valueOf(double value);
}
