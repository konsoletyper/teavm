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
import static org.teavm.jso.impl.JSMethods.JS_WRAPPER_CLASS;
import static org.teavm.jso.impl.JSMethods.OBJECT;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmExternConversionType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class WasmGCJSWrapperIntrinsic implements WasmGCIntrinsic {
    private WasmFunction wrapFunction;

    @Override
    public void apply(InvocationExpr invocation, WasmGCIntrinsicContext context, WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "wrap": {
                context.generate(builder, invocation.getArguments().get(0));
                builder.call(getWrapFunction(context));
                break;
            }
            case "isJava": {
                context.generate(builder, invocation.getArguments().get(0));
                builder.externConvert(WasmExternConversionType.EXTERN_TO_ANY);
                var objectType = (WasmType.Reference) context.typeMapper().mapType(ValueType.parse(Object.class));
                builder.test(objectType);
                break;
            }
            default:
                throw new IllegalArgumentException();
        }
    }

    private WasmFunction getWrapFunction(WasmGCIntrinsicContext context) {
        if (wrapFunction == null) {
            var objectType = context.typeMapper().mapType(ValueType.parse(Object.class));
            wrapFunction = new WasmFunction(context.functionTypes().of(objectType, WasmType.EXTERN));
            wrapFunction.setImportName("wrapObject");
            wrapFunction.setImportModule("teavmJso");
            context.module().functions.add(wrapFunction);

            var createWrapperFunction = context.functions().forStaticMethod(new MethodReference(
                    JS_WRAPPER_CLASS, "createWrapper", JS_OBJECT, OBJECT));
            createWrapperFunction.setExportName("teavm.jso.createWrapper");
        }
        return wrapFunction;
    }
}
