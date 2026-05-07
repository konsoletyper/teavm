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

import java.util.function.ToIntFunction;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.methods.WasmGCGenerationUtil;
import org.teavm.backend.wasm.generate.reflection.ClassReflectionInfoStruct;
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;

public class ClassReflectionInfoIntrinsic implements WasmGCIntrinsic {
    @Override
    public void apply(InvocationExpr invocation, WasmGCIntrinsicContext context, WasmInstructionBuilder builder) {
        switch (invocation.getMethod().getName()) {
            case "annotationCount":
                collectionCount(invocation, context, builder, ClassReflectionInfoStruct::annotationsIndex);
                break;
            case "annotation": {
                var array = context.classInfoProvider().reflectionTypes().annotationInfo().array();
                collectionElement(invocation, context, builder, array, ClassReflectionInfoStruct::annotationsIndex);
                break;
            }
            case "fieldCount":
                collectionCount(invocation, context, builder, ClassReflectionInfoStruct::fieldsIndex);
                break;
            case "field": {
                var array = context.classInfoProvider().reflectionTypes().fieldInfo().array();
                collectionElement(invocation, context, builder, array, ClassReflectionInfoStruct::fieldsIndex);
                break;
            }
            case "methodCount":
                collectionCount(invocation, context, builder, ClassReflectionInfoStruct::methodsIndex);
                break;
            case "method": {
                var array = context.classInfoProvider().reflectionTypes().methodInfo().array();
                collectionElement(invocation, context, builder, array, ClassReflectionInfoStruct::methodsIndex);
                break;
            }
            case "typeParameterCount":
                collectionCount(invocation, context, builder, ClassReflectionInfoStruct::typeParametersIndex);
                break;
            case "typeParameter": {
                var array = context.classInfoProvider().reflectionTypes().typeVariableInfo().array();
                collectionElement(invocation, context, builder, array,
                        ClassReflectionInfoStruct::typeParametersIndex);
                break;
            }
            case "innerClassCount":
                collectionCount(invocation, context, builder, ClassReflectionInfoStruct::innerClassesIndex);
                break;
            case "innerClass": {
                var array = context.classInfoProvider().reflectionTypes().classInfo().array();
                collectionElement(invocation, context, builder, array, ClassReflectionInfoStruct::innerClassesIndex);
                break;
            }
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }

    private void collectionCount(InvocationExpr invocation, WasmGCIntrinsicContext context,
            WasmInstructionBuilder builder, ToIntFunction<ClassReflectionInfoStruct> fieldIndex) {
        var infoStruct = context.classInfoProvider().reflectionTypes().classReflectionInfo();
        WasmGCGenerationUtil.getArrayLengthOfNullable(builder, b -> {
            context.generate(b, invocation.getArguments().get(0));
            b.structGet(infoStruct.structure(), fieldIndex.applyAsInt(infoStruct));
        });
    }

    private void collectionElement(InvocationExpr invocation, WasmGCIntrinsicContext context,
            WasmInstructionBuilder builder, WasmArray array,
            ToIntFunction<ClassReflectionInfoStruct> fieldIndex) {
        var infoStruct = context.classInfoProvider().reflectionTypes().classReflectionInfo();
        context.generate(builder, invocation.getArguments().get(0));
        builder.structGet(infoStruct.structure(), fieldIndex.applyAsInt(infoStruct));
        context.generate(builder, invocation.getArguments().get(1));
        builder.arrayGet(array);
    }
}
