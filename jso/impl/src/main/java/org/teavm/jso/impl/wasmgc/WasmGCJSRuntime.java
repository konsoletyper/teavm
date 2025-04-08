/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.jso.impl.wasmgc;

import org.teavm.interop.Import;
import org.teavm.jso.JSObject;

final class WasmGCJSRuntime {
    private WasmGCJSRuntime() {
    }

    private static boolean stringBuiltinsSupported = stringBuiltinsSupported();

    static JSObject stringToJs(String str) {
        if (str == null) {
            return null;
        }
        if (stringBuiltinsSupported) {
            return stringFromCharArray(CharArrayData.of(str), 0, str.length()).toNullable();
        } else {
            if (str.isEmpty()) {
                return substring(stringFromCharCode(32).toNullable(), 0, 0).toNullable();
            }
            var result = stringFromCharCode(str.charAt(0)).toNullable();
            for (var i = 1; i < str.length(); ++i) {
                result = concatStrings(result, stringFromCharCode(str.charAt(i)).toNullable()).toNullable();
            }
            return result;
        }
    }

    static String jsToString(JSObject obj) {
        if (obj == null || isUndefined(obj)) {
            return null;
        }
        var length = stringLength(obj);
        var chars = CharArrayData.create(length);
        if (stringBuiltinsSupported) {
            stringIntoCharArray(obj, chars, 0);
        } else {
            for (var i = 0; i < length; i++) {
                var code = (char) charCodeAt(obj, i);
                chars.put(i, code);
            }
        }
        return chars.asString();
    }

    @Import(name = "length", module = "wasm:js-string")
    static native int stringLength(JSObject str);

    @Import(name = "fromCharCodeArray", module = "wasm:js-string")
    static native NonNullExternal stringFromCharArray(CharArrayData data, int start, int end);

    @Import(name = "fromCharCode", module = "wasm:js-string")
    static native NonNullExternal stringFromCharCode(int charCode);

    @Import(name = "intoCharCodeArray", module = "wasm:js-string")
    static native int stringIntoCharArray(JSObject str, CharArrayData data, int start);

    @Import(name = "concat", module = "wasm:js-string")
    static native NonNullExternal concatStrings(JSObject first, JSObject second);

    @Import(name = "substring", module = "wasm:js-string")
    static native NonNullExternal substring(JSObject string, int start, int end);

    @Import(name = "charCodeAt", module = "wasm:js-string")
    static native int charCodeAt(JSObject string, int index);

    @Import(name = "stringBuiltinsSupported", module = "teavmJso")
    static native boolean stringBuiltinsSupported();

    @Import(name = "isUndefined", module = "teavmJso")
    static native boolean isUndefined(JSObject o);

    @Import(name = "emptyArray", module = "teavmJso")
    static native JSObject emptyArray();

    @Import(name = "appendToArray", module = "teavmJso")
    static native JSObject appendToArray(JSObject array, JSObject element);

    static native JSObject wrapObject(Object obj);

    static Throwable wrapException(JSObject obj) {
        return new WasmGCExceptionWrapper(obj);
    }

    static JSObject extractException(Throwable e) {
        return e instanceof WasmGCExceptionWrapper ? ((WasmGCExceptionWrapper) e).jsException : null;
    }

    static final class CharArrayData {
        static native CharArrayData of(String s);

        native String asString();

        static native CharArrayData create(int size);

        native void put(int index, char code);
    }

    static final class NonNullExternal {
        native JSObject toNullable();
    }
}
