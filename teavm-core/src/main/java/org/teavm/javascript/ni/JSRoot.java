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
package org.teavm.javascript.ni;

/**
 *
 * @author Alexey Andreev
 */
public final class JSRoot {
    private JSRoot() {
    }

    public static JSType getType(JSObject obj) {
        switch (getTypeName(obj).asString()) {
            case "boolean":
                return JSType.OBJECT;
            case "number":
                return JSType.NUMBER;
            case "string":
                return JSType.STRING;
            case "function":
                return JSType.FUNCTION;
            case "object":
                return JSType.OBJECT;
            case "undefined":
                return JSType.UNDEFINED;
        }
        throw new AssertionError("Unexpected type");
    }

    @GeneratedBy(JSRootNativeGenerator.class)
    public static native JSString getTypeName(JSObject obj);

    @GeneratedBy(JSRootNativeGenerator.class)
    public static native JSObject getGlobal();

    @GeneratedBy(JSRootNativeGenerator.class)
    public static native JSString wrap(String str);

    @GeneratedBy(JSRootNativeGenerator.class)
    public static native JSNumber wrap(int num);

    @GeneratedBy(JSRootNativeGenerator.class)
    public static native JSNumber wrap(float num);

    @GeneratedBy(JSRootNativeGenerator.class)
    public static native JSNumber wrap(double num);
}
