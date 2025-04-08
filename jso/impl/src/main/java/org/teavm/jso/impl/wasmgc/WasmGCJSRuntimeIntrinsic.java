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
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.generate.gc.methods.WasmGCGenerationUtil;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayNewDefault;
import org.teavm.backend.wasm.model.expression.WasmArraySet;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCast;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class WasmGCJSRuntimeIntrinsic implements WasmGCIntrinsic {
    private WasmGCJsoCommonGenerator commonGen;

    WasmGCJSRuntimeIntrinsic(WasmGCJsoCommonGenerator commonGen) {
        this.commonGen = commonGen;
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "wrapObject": {
                var jsoContext = WasmGCJsoContext.wrap(context);
                var wrapperClass = commonGen.getDefaultWrapperClass(jsoContext);
                var wrapperFunction = commonGen.javaObjectToJSFunction(jsoContext);
                return new WasmCall(wrapperFunction, context.generate(invocation.getArguments().get(0)),
                        new WasmGetGlobal(wrapperClass));
            }
            case "of": {
                var stringCls = context.classInfoProvider().getClassInfo("java.lang.String");
                var fieldIndex = context.classInfoProvider().getFieldIndex(new FieldReference(
                        "java.lang.String", "characters"));
                var arg = context.generate(invocation.getArguments().get(0));
                var stringField = new WasmStructGet(stringCls.getStructure(), arg, fieldIndex);

                var arrayCls = context.classInfoProvider().getClassInfo(ValueType.arrayOf(ValueType.CHARACTER));
                return new WasmStructGet(arrayCls.getStructure(), stringField,
                        WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
            }
            case "asString": {
                var genUtil = new WasmGCGenerationUtil(context.classInfoProvider());
                var arrayCls = context.classInfoProvider().getClassInfo(ValueType.arrayOf(ValueType.CHARACTER));
                var field = arrayCls.getStructure().getFields().get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
                var array = genUtil.allocateArray(ValueType.CHARACTER, arrayType ->
                        new WasmCast(context.generate(invocation.getArguments().get(0)),
                                (WasmType.Reference) field.getType().asUnpackedType()));
                var fn = context.functions().forStaticMethod(new MethodReference(String.class, "fromArray",
                        char[].class, String.class));
                return new WasmCall(fn, array);
            }
            case "create": {
                var arrayCls = context.classInfoProvider().getClassInfo(ValueType.arrayOf(ValueType.CHARACTER));
                var field = arrayCls.getStructure().getFields().get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
                var type = (WasmType.CompositeReference) field.getType().asUnpackedType();
                var array = (WasmArray) type.composite;
                return new WasmArrayNewDefault(array, context.generate(invocation.getArguments().get(0)));
            }
            case "toNullable":
                return context.generate(invocation.getArguments().get(0));
            case "put": {
                var arrayCls = context.classInfoProvider().getClassInfo(ValueType.arrayOf(ValueType.CHARACTER));
                var field = arrayCls.getStructure().getFields().get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
                var type = (WasmType.CompositeReference) field.getType().asUnpackedType();
                var array = (WasmArray) type.composite;
                return new WasmArraySet(array, context.generate(invocation.getArguments().get(0)),
                        context.generate(invocation.getArguments().get(1)),
                        context.generate(invocation.getArguments().get(2)));
            }
            default:
                throw new IllegalArgumentException();
        }
    }
}
