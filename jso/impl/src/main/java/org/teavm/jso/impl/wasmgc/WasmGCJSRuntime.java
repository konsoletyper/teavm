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

    static JSObject stringToJs(String str) {
        if (str == null) {
            return null;
        }
        return stringFromCharArray(CharArrayData.of(str), 0, str.length());
    }

    static String jsToString(JSObject obj) {
        if (obj == null || isUndefined(obj)) {
            return null;
        }
        var length = stringLength(obj);
        var chars = CharArrayData.create(length);
        stringIntoCharArray(obj, chars, 0);
        return chars.asString();
    }

    @Import(name = "length", module = "wasm:js-string")
    static native int stringLength(JSObject str);

    @Import(name = "fromCharCodeArray", module = "wasm:js-string")
    static native JSObject stringFromCharArray(CharArrayData data, int start, int end);

    @Import(name = "intoCharCodeArray", module = "wasm:js-string")
    static native int stringIntoCharArray(JSObject str, CharArrayData data, int start);

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
    }
}
