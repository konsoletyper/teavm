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

import static org.teavm.jso.impl.JSMethods.JS_OBJECT;
import static org.teavm.jso.impl.JSMethods.STRING;
import static org.teavm.jso.impl.JSMethods.WASM_GC_JS_RUNTIME_CLASS;
import org.teavm.model.MethodReference;

final class WasmGCJSConstants {
    private WasmGCJSConstants() {
    }

    static final MethodReference STRING_TO_JS = new MethodReference(WASM_GC_JS_RUNTIME_CLASS,
            "stringToJs", STRING, JS_OBJECT);
    static final MethodReference JS_TO_STRING = new MethodReference(WASM_GC_JS_RUNTIME_CLASS,
            "jsToString", JS_OBJECT, STRING);
}
