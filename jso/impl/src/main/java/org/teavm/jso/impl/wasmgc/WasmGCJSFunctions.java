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

import java.util.Arrays;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmType;

class WasmGCJSFunctions {
    private WasmFunction[] constructors = new WasmFunction[32];
    private WasmFunction[] binds = new WasmFunction[32];
    private WasmFunction[] callers = new WasmFunction[32];
    private WasmFunction getFunction;

    WasmFunction getFunctionConstructor(WasmGCJsoContext context, int index) {
        var function = constructors[index];
        if (function == null) {
            var extern = WasmType.SpecialReferenceKind.EXTERN.asNonNullType();
            var constructorParamTypes = new WasmType[index + 1];
            Arrays.fill(constructorParamTypes, WasmType.Reference.EXTERN);
            var functionType = context.functionTypes().of(extern, constructorParamTypes);
            function = new WasmFunction(functionType);
            function.setName(context.names().topLevel("teavm.js:createFunction" + index));
            function.setImportModule("teavmJso");
            function.setImportName("createFunction" + index);
            context.module().functions.add(function);
            constructors[index] = function;
        }
        return function;
    }

    WasmFunction getBind(WasmGCJsoContext context, int index) {
        var function = binds[index];
        if (function == null) {
            var extern = WasmType.SpecialReferenceKind.EXTERN.asNonNullType();
            var constructorParamTypes = new WasmType[index + 1];
            Arrays.fill(constructorParamTypes, WasmType.Reference.EXTERN);
            var functionType = context.functionTypes().of(extern, constructorParamTypes);
            function = new WasmFunction(functionType);
            function.setName(context.names().topLevel("teavm.js:bindFunction" + index));
            function.setImportModule("teavmJso");
            function.setImportName("bindFunction" + index);
            context.module().functions.add(function);
            binds[index] = function;
        }
        return function;
    }

    WasmFunction getGet(WasmGCJsoContext context) {
        var function = getFunction;
        if (function == null) {
            var functionType = context.functionTypes().of(WasmType.Reference.EXTERN, WasmType.Reference.EXTERN,
                    WasmType.Reference.EXTERN);
            function = new WasmFunction(functionType);
            function.setName(context.names().topLevel("teavm.js:getProperty"));
            function.setImportModule("teavmJso");
            function.setImportName("getProperty");
            context.module().functions.add(function);
            getFunction = function;
        }
        return function;
    }

    WasmFunction getFunctionCaller(WasmGCJsoContext context, int index) {
        var function = callers[index];
        if (function == null) {
            var paramTypes = new WasmType[index + 1];
            Arrays.fill(paramTypes, WasmType.Reference.EXTERN);
            var functionType = context.functionTypes().of(WasmType.Reference.EXTERN, paramTypes);
            function = new WasmFunction(functionType);
            function.setName(context.names().topLevel("teavm.js:callFunction" + index));
            function.setImportModule("teavmJso");
            function.setImportName("callFunction" + index);
            context.module().functions.add(function);
            callers[index] = function;
        }
        return function;
    }
}
