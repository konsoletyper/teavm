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
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmType;

class WasmGCJSFunctions {
    private WasmFunction[] constructors = new WasmFunction[32];
    private WasmFunction[] callers = new WasmFunction[32];

    WasmFunction getFunctionConstructor(WasmGCIntrinsicContext context, int index) {
        var function = constructors[index];
        if (function == null) {
            var extern = WasmType.SpecialReferenceKind.EXTERN.asNonNullType();
            var constructorParamTypes = new WasmType[index + 1];
            Arrays.fill(constructorParamTypes, extern);
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

    WasmFunction getFunctionCaller(WasmGCIntrinsicContext context, int index) {
        var function = callers[index];
        if (function == null) {
            var extern = WasmType.SpecialReferenceKind.EXTERN.asNonNullType();
            var paramTypes = new WasmType[index + 1];
            Arrays.fill(paramTypes, extern);
            paramTypes[0] = WasmType.Reference.EXTERN;
            var functionType = context.functionTypes().of(extern, paramTypes);
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
