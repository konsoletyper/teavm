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
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.classes.WasmGCTypeMapper;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsicContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmExternConversionType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class WasmGCJSWrapperIntrinsic implements WasmGCInlineIntrinsic {
    private WasmGCTypeMapper typeMapper;
    private BaseWasmFunctionRepository functions;
    private WasmFunctionTypes functionTypes;
    private WasmModule module;

    private WasmFunction wrapFunction;

    WasmGCJSWrapperIntrinsic(WasmGCTypeMapper typeMapper, BaseWasmFunctionRepository functions,
            WasmFunctionTypes functionTypes, WasmModule module) {
        this.typeMapper = typeMapper;
        this.functions = functions;
        this.functionTypes = functionTypes;
        this.module = module;
    }

    @Override
    public void apply(InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "wrap": {
                context.generate(builder, invocation.getArguments().get(0));
                builder.call(getWrapFunction());
                break;
            }
            case "isJava": {
                context.generate(builder, invocation.getArguments().get(0));
                builder.externConvert(WasmExternConversionType.EXTERN_TO_ANY);
                var objectType = (WasmType.Reference) typeMapper.mapType(ValueType.parse(Object.class));
                builder.test(objectType);
                break;
            }
            default:
                throw new IllegalArgumentException();
        }
    }

    private WasmFunction getWrapFunction() {
        if (wrapFunction == null) {
            var objectType = typeMapper.mapType(ValueType.parse(Object.class));
            wrapFunction = new WasmFunction(functionTypes.of(objectType, WasmType.EXTERN));
            wrapFunction.setImportName("wrapObject");
            wrapFunction.setImportModule("teavmJso");
            module.functions.add(wrapFunction);

            var createWrapperFunction = functions.forStaticMethod(new MethodReference(
                    JS_WRAPPER_CLASS, "createWrapper", JS_OBJECT, OBJECT));
            createWrapperFunction.setExportName("teavm.jso.createWrapper");
        }
        return wrapFunction;
    }
}
