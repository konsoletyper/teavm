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
        if (str.isEmpty()) {
            return emptyString();
        }
        var jsStr = stringFromCharCode(str.charAt(0));
        for (var i = 1; i < str.length(); ++i) {
            jsStr = concatStrings(jsStr, stringFromCharCode(str.charAt(i)));
        }
        return jsStr;
    }

    static String jsToString(JSObject obj) {
        var length = stringLength(obj);
        if (length == 0) {
            return "";
        }
        var chars = new char[length];
        for (var i = 0; i < length; ++i) {
            chars[i] = charAt(obj, i);
        }
        return new String(chars);
    }

    @Import(name = "emptyString", module = "teavmJso")
    static native JSObject emptyString();

    @Import(name = "stringFromCharCode", module = "teavmJso")
    static native JSObject stringFromCharCode(char c);

    @Import(name = "concatStrings", module = "teavmJso")
    static native JSObject concatStrings(JSObject a, JSObject b);

    @Import(name = "emptyArray", module = "teavmJso")
    static native JSObject emptyArray();

    @Import(name = "appendToArray", module = "teavmJso")
    static native JSObject appendToArray(JSObject array, JSObject element);

    @Import(name = "stringLength", module = "teavmJso")
    static native int stringLength(JSObject str);

    @Import(name = "charAt", module = "teavmJso")
    static native char charAt(JSObject str, int index);

    static native JSObject wrapObject(Object obj);

    static Throwable wrapException(JSObject obj) {
        return new WasmGCExceptionWrapper(obj);
    }

    static JSObject extractException(Throwable e) {
        return e instanceof WasmGCExceptionWrapper ? ((WasmGCExceptionWrapper) e).jsException : null;
    }
}
