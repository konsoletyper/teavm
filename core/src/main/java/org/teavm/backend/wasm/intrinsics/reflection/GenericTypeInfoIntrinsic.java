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
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmExpressionToInstructionConverter;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmConditional;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmTest;
import org.teavm.model.MethodReference;
import org.teavm.runtime.reflect.GenericTypeInfo;

public class GenericTypeInfoIntrinsic implements WasmGCIntrinsic {
    private WasmFunction kindFunction;

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var reflectionTypes = context.classInfoProvider().reflectionTypes();
        switch (invocation.getMethod().getName()) {
            case "kind": {
                var function = getKindFunction(context);
                return new WasmCall(function, context.generate(invocation.getArguments().get(0)));
            }
            case "asParameterizedType": {
                var receiver = context.generate(invocation.getArguments().get(0));
                return new WasmCast(receiver, reflectionTypes.parameterizedTypeInfo().structure().getReference());
            }
            case "asTypeVariable": {
                var receiver = context.generate(invocation.getArguments().get(0));
                return new WasmCast(receiver, reflectionTypes.typeVariableReference().structure().getReference());
            }
            case "asGenericArray": {
                var receiver = context.generate(invocation.getArguments().get(0));
                return new WasmCast(receiver, reflectionTypes.genericArrayInfo().structure().getReference());
            }
            case "asWildcard": {
                var receiver = context.generate(invocation.getArguments().get(0));
                return new WasmCast(receiver, reflectionTypes.wildcardTypeInfo().structure().getReference());
            }
            case "asRawType": {
                var receiver = context.generate(invocation.getArguments().get(0));
                return new WasmCast(receiver, reflectionTypes.derivedClassInfo().structure().getReference());
            }
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }

    private WasmFunction getKindFunction(WasmGCIntrinsicContext context) {
        if (kindFunction == null) {
            kindFunction = new WasmFunction(context.functionTypes().of(WasmType.INT32, WasmType.STRUCT));
            kindFunction.setName(context.names().topLevel(context.names().suggestForMethod(new MethodReference(
                    GenericTypeInfo.class, "kind", int.class))));

            var reflectionTypes = context.classInfoProvider().reflectionTypes();

            var param = new WasmLocal(WasmType.STRUCT, "this");
            kindFunction.add(param);
            var converter = new WasmExpressionToInstructionConverter(kindFunction.getBody());

            var check = new WasmConditional(new WasmTest(new WasmGetLocal(param),
                    reflectionTypes.parameterizedTypeInfo().structure().getReference()));
            check.getThenBlock().getBody().add(new WasmReturn(new WasmInt32Constant(
                    GenericTypeInfo.Kind.PARAMETERIZED_TYPE)));
            converter.convert(check);

            check = new WasmConditional(new WasmTest(new WasmGetLocal(param),
                    reflectionTypes.typeVariableReference().structure().getReference()));
            check.getThenBlock().getBody().add(new WasmReturn(new WasmInt32Constant(
                    GenericTypeInfo.Kind.TYPE_VARIABLE)));
            converter.convert(check);

            check = new WasmConditional(new WasmTest(new WasmGetLocal(param),
                    reflectionTypes.genericArrayInfo().structure().getReference()));
            check.getThenBlock().getBody().add(new WasmReturn(new WasmInt32Constant(
                    GenericTypeInfo.Kind.GENERIC_ARRAY)));
            converter.convert(check);

            var wildcardStruct = reflectionTypes.wildcardTypeInfo().structure();
            check = new WasmConditional(new WasmTest(new WasmGetLocal(param), wildcardStruct.getReference()));
            var wildcard = new WasmCast(new WasmGetLocal(param), wildcardStruct.getReference());
            var kind = new WasmStructGet(wildcardStruct, wildcard, reflectionTypes.wildcardTypeInfo().kindIndex());
            var resultingKind = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD,
                    kind, new WasmInt32Constant(GenericTypeInfo.Kind.UPPER_BOUND_WILDCARD));
            check.getThenBlock().getBody().add(new WasmReturn(resultingKind));
            converter.convert(check);

            converter.convert(new WasmInt32Constant(GenericTypeInfo.Kind.RAW_TYPE));

            context.module().functions.add(kindFunction);
        }
        return kindFunction;
    }
}
