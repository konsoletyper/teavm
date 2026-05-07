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
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.generate.methods.WasmGCGenerationUtil;
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class WasmGCJSRuntimeIntrinsic implements WasmGCIntrinsic {
    private WasmGCJsoCommonGenerator commonGen;

    WasmGCJSRuntimeIntrinsic(WasmGCJsoCommonGenerator commonGen) {
        this.commonGen = commonGen;
    }

    @Override
    public void apply(InvocationExpr invocation, WasmGCIntrinsicContext context, WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "wrapObject": {
                var jsoContext = WasmGCJsoContext.wrap(context);
                var wrapperClass = commonGen.getDefaultWrapperClass(jsoContext);
                var wrapperFunction = commonGen.javaObjectToJSFunction(jsoContext);
                context.generate(builder, invocation.getArguments().get(0));
                builder.getGlobal(wrapperClass);
                builder.call(wrapperFunction);
                break;
            }
            case "of": {
                var stringCls = context.classInfoProvider().getClassInfo("java.lang.String");
                var fieldIndex = context.classInfoProvider().getFieldIndex(new FieldReference(
                        "java.lang.String", "characters"));
                context.generate(builder, invocation.getArguments().get(0));
                builder.structGet(stringCls.getStructure(), fieldIndex);
                var arrayCls = context.classInfoProvider().getClassInfo(ValueType.arrayOf(ValueType.CHARACTER));
                builder.structGet(arrayCls.getStructure(), WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
                break;
            }
            case "asString": {
                var arrayCls = context.classInfoProvider().getClassInfo(ValueType.arrayOf(ValueType.CHARACTER));
                var field = arrayCls.getStructure().getFields().get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
                WasmGCGenerationUtil.allocateArray(context.classInfoProvider(), ValueType.CHARACTER, builder,
                        (array, b) -> {
                            context.generate(b, invocation.getArguments().get(0));
                            b.cast((WasmType.Reference) field.getType().asUnpackedType());
                        });
                builder.call(context.functions().forStaticMethod(new MethodReference(String.class, "fromArray",
                        char[].class, String.class)));
                break;
            }
            case "create": {
                var arrayCls = context.classInfoProvider().getClassInfo(ValueType.arrayOf(ValueType.CHARACTER));
                var field = arrayCls.getStructure().getFields().get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
                var type = (WasmType.CompositeReference) field.getType().asUnpackedType();
                var array = (WasmArray) type.composite;
                context.generate(builder, invocation.getArguments().get(0));
                builder.arrayNewDefault(array);
                break;
            }
            case "toNullable":
                context.generate(builder, invocation.getArguments().get(0));
                break;
            case "put": {
                var arrayCls = context.classInfoProvider().getClassInfo(ValueType.arrayOf(ValueType.CHARACTER));
                var field = arrayCls.getStructure().getFields().get(WasmGCClassInfoProvider.ARRAY_DATA_FIELD_OFFSET);
                var type = (WasmType.CompositeReference) field.getType().asUnpackedType();
                var array = (WasmArray) type.composite;
                context.generate(builder, invocation.getArguments().get(0));
                context.generate(builder, invocation.getArguments().get(1));
                context.generate(builder, invocation.getArguments().get(2));
                builder.arraySet(array);
                break;
            }
            default:
                throw new IllegalArgumentException();
        }
    }
}
