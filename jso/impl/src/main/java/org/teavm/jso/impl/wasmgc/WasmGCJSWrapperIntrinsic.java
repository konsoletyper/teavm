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

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmExternConversion;
import org.teavm.backend.wasm.model.expression.WasmExternConversionType;
import org.teavm.backend.wasm.model.expression.WasmTest;
import org.teavm.jso.JSObject;
import org.teavm.jso.impl.JSWrapper;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class WasmGCJSWrapperIntrinsic implements WasmGCIntrinsic {
    private WasmFunction wrapFunction;

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "wrap": {
                var function = getWrapFunction(context);
                return new WasmCall(function, context.generate(invocation.getArguments().get(0)));
            }
            case "dependencyJavaToJs":
            case "directJavaToJs":
                return new WasmExternConversion(WasmExternConversionType.ANY_TO_EXTERN,
                        context.generate(invocation.getArguments().get(0)));
            case "dependencyJsToJava":
            case "directJsToJava": {
                var any = new WasmExternConversion(WasmExternConversionType.EXTERN_TO_ANY,
                        context.generate(invocation.getArguments().get(0)));
                var objectType = context.typeMapper().mapType(ValueType.parse(Object.class));
                return new WasmCast(any, (WasmType.Reference) objectType);
            }
            case "isJava": {
                var convert = new WasmExternConversion(WasmExternConversionType.EXTERN_TO_ANY,
                        context.generate(invocation.getArguments().get(0)));
                var objectType = context.typeMapper().mapType(ValueType.parse(Object.class));
                return new WasmTest(convert, (WasmType.Reference) objectType);
            }

            default:
                throw new IllegalArgumentException();
        }
    }

    private WasmFunction getWrapFunction(WasmGCIntrinsicContext context) {
        if (wrapFunction == null) {
            var objectType = context.typeMapper().mapType(ValueType.parse(Object.class));
            wrapFunction = new WasmFunction(context.functionTypes().of(objectType, WasmType.Reference.EXTERN));
            wrapFunction.setImportName("wrapObject");
            wrapFunction.setImportModule("teavmJso");
            context.module().functions.add(wrapFunction);

            var createWrapperFunction = context.functions().forStaticMethod(new MethodReference(
                    JSWrapper.class, "createWrapper", JSObject.class, Object.class));
            createWrapperFunction.setExportName("teavm.jso.createWrapper");
        }
        return wrapFunction;
    }
}
