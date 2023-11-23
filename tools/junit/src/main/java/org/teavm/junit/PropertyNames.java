/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.junit;

final class PropertyNames {
    static final String PATH_PARAM = "teavm.junit.target";
    static final String JS_RUNNER = "teavm.junit.js.runner";
    static final String WASM_RUNNER = "teavm.junit.wasm.runner";
    static final String JS_ENABLED = "teavm.junit.js";
    static final String JS_DECODE_STACK = "teavm.junit.js.decodeStack";
    static final String C_ENABLED = "teavm.junit.c";
    static final String WASM_ENABLED = "teavm.junit.wasm";
    static final String WASI_ENABLED = "teavm.junit.wasi";
    static final String WASI_RUNNER = "teavm.junit.wasi.runner";
    static final String C_COMPILER = "teavm.junit.c.compiler";
    static final String C_LINE_NUMBERS = "teavm.junit.c.lineNumbers";
    static final String MINIFIED = "teavm.junit.minified";
    static final String OPTIMIZED = "teavm.junit.optimized";
    static final String SOURCE_DIRS = "teavm.junit.sourceDirs";

    private PropertyNames() {
    }
}
