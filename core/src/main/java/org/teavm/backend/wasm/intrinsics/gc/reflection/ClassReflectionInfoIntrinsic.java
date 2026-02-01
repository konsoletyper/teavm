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
package org.teavm.backend.wasm.intrinsics.gc.reflection;

import java.util.function.ToIntFunction;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.generate.gc.methods.WasmGCGenerationUtil;
import org.teavm.backend.wasm.generate.gc.reflection.ClassReflectionInfoStruct;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.expression.WasmArrayGet;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmStructGet;

public class ClassReflectionInfoIntrinsic implements WasmGCIntrinsic {
    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        switch (invocation.getMethod().getName()) {
            case "annotationCount":
                return collectionCount(invocation, context, ClassReflectionInfoStruct::annotationsIndex);
            case "annotation": {
                var array = context.classInfoProvider().reflectionTypes().annotationInfo().array();
                return collectionElement(invocation, context, array, ClassReflectionInfoStruct::annotationsIndex);
            }
            case "fieldCount":
                return collectionCount(invocation, context, ClassReflectionInfoStruct::fieldsIndex);
            case "field": {
                var array = context.classInfoProvider().reflectionTypes().fieldInfo().array();
                return collectionElement(invocation, context, array, ClassReflectionInfoStruct::fieldsIndex);
            }
            case "methodCount":
                return collectionCount(invocation, context, ClassReflectionInfoStruct::methodsIndex);
            case "method": {
                var array = context.classInfoProvider().reflectionTypes().methodInfo().array();
                return collectionElement(invocation, context, array, ClassReflectionInfoStruct::methodsIndex);
            }
            case "typeParameterCount":
                return collectionCount(invocation, context, ClassReflectionInfoStruct::typeParametersIndex);
            case "typeParameter": {
                var array = context.classInfoProvider().reflectionTypes().typeVariableInfo().array();
                return collectionElement(invocation, context, array, ClassReflectionInfoStruct::typeParametersIndex);
            }

            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }

    private WasmExpression collectionCount(InvocationExpr invocation, WasmGCIntrinsicContext context,
            ToIntFunction<ClassReflectionInfoStruct> fieldIndex) {
        var receiver = context.generate(invocation.getArguments().get(0));
        var infoStruct = context.classInfoProvider().reflectionTypes().classReflectionInfo();
        var annotations = new WasmStructGet(infoStruct.structure(), receiver, fieldIndex.applyAsInt(infoStruct));
        return WasmGCGenerationUtil.getArrayLengthOfNullable(annotations);
    }

    private WasmExpression collectionElement(InvocationExpr invocation, WasmGCIntrinsicContext context,
            WasmArray array, ToIntFunction<ClassReflectionInfoStruct> fieldIndex) {
        var infoStruct = context.classInfoProvider().reflectionTypes().classReflectionInfo();
        var receiver = context.generate(invocation.getArguments().get(0));
        var index = context.generate(invocation.getArguments().get(1));
        var annotations = new WasmStructGet(infoStruct.structure(), receiver, fieldIndex.applyAsInt(infoStruct));
        return new WasmArrayGet(array, annotations, index);
    }
}
