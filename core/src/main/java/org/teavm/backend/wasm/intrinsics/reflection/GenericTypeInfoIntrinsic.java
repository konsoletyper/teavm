/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.backend.wasm.intrinsics.reflection;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCInlineIntrinsicContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.model.MethodReference;
import org.teavm.runtime.reflect.GenericTypeInfo;

public class GenericTypeInfoIntrinsic implements WasmGCInlineIntrinsic {
    private final WasmGCClassInfoProvider classInfoProvider;
    private final WasmFunctionTypes functionTypes;
    private final WasmGCNameProvider names;
    private final WasmModule module;
    private WasmFunction kindFunction;

    public GenericTypeInfoIntrinsic(WasmGCClassInfoProvider classInfoProvider, WasmFunctionTypes functionTypes,
            WasmGCNameProvider names, WasmModule module) {
        this.classInfoProvider = classInfoProvider;
        this.functionTypes = functionTypes;
        this.names = names;
        this.module = module;
    }

    @Override
    public void apply(InvocationExpr invocation, WasmGCInlineIntrinsicContext context,
            WasmInstructionBuilder builder) {
        var reflectionTypes = classInfoProvider.reflectionTypes();
        switch (invocation.getMethod().getName()) {
            case "kind":
                context.generate(builder, invocation.getArguments().get(0));
                builder.call(getKindFunction());
                break;
            case "asParameterizedType":
                context.generate(builder, invocation.getArguments().get(0));
                builder.cast(reflectionTypes.parameterizedTypeInfo().structure().getReference());
                break;
            case "asTypeVariable":
                context.generate(builder, invocation.getArguments().get(0));
                builder.cast(reflectionTypes.typeVariableReference().structure().getReference());
                break;
            case "asGenericArray":
                context.generate(builder, invocation.getArguments().get(0));
                builder.cast(reflectionTypes.genericArrayInfo().structure().getReference());
                break;
            case "asWildcard":
                context.generate(builder, invocation.getArguments().get(0));
                builder.cast(reflectionTypes.wildcardTypeInfo().structure().getReference());
                break;
            case "asRawType":
                context.generate(builder, invocation.getArguments().get(0));
                builder.cast(reflectionTypes.derivedClassInfo().structure().getReference());
                break;
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }

    private WasmFunction getKindFunction() {
        if (kindFunction == null) {
            kindFunction = new WasmFunction(functionTypes.of(WasmType.INT32, WasmType.STRUCT));
            kindFunction.setName(names.topLevel(names.suggestForMethod(new MethodReference(
                    GenericTypeInfo.class, "kind", int.class))));

            var reflectionTypes = classInfoProvider.reflectionTypes();

            var param = new WasmLocal(WasmType.STRUCT, "this");
            kindFunction.add(param);
            var body = kindFunction.getBody().builder();

            body.getLocal(param).test(reflectionTypes.parameterizedTypeInfo().structure().getReference());
            body.conditional().getThenBlock().builder()
                    .i32Const(GenericTypeInfo.Kind.PARAMETERIZED_TYPE).return_();

            body.getLocal(param).test(reflectionTypes.typeVariableReference().structure().getReference());
            body.conditional().getThenBlock().builder()
                    .i32Const(GenericTypeInfo.Kind.TYPE_VARIABLE).return_();

            body.getLocal(param).test(reflectionTypes.genericArrayInfo().structure().getReference());
            body.conditional().getThenBlock().builder()
                    .i32Const(GenericTypeInfo.Kind.GENERIC_ARRAY).return_();

            var wildcardStruct = reflectionTypes.wildcardTypeInfo().structure();
            body.getLocal(param).test(wildcardStruct.getReference());
            body.conditional().getThenBlock().builder()
                    .getLocal(param).cast(wildcardStruct.getReference())
                    .structGet(wildcardStruct, reflectionTypes.wildcardTypeInfo().kindIndex())
                    .i32Const(GenericTypeInfo.Kind.UPPER_BOUND_WILDCARD)
                    .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD)
                    .return_();

            body.i32Const(GenericTypeInfo.Kind.RAW_TYPE);

            module.functions.add(kindFunction);
        }
        return kindFunction;
    }
}
