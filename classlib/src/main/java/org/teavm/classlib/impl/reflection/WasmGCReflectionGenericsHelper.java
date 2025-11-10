/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.classlib.impl.reflection;

import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.expression.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
import org.teavm.model.ClassReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class WasmGCReflectionGenericsHelper {
    private WasmGCIntrinsicContext context;
    private WasmFunction function;
    private WasmFunction variableConstructor;

    WasmGCReflectionGenericsHelper(WasmGCIntrinsicContext context, WasmFunction function) {
        this.context = context;
        this.function = function;
    }

    void initReflectionGenerics() {
        for (var className : context.dependency().getReachableClasses()) {
            var cls = context.hierarchy().getClassSource().get(className);
            if (cls != null) {
                initReflectionGenerics(cls);
            }
        }
    }

    private void initReflectionGenerics(ClassReader cls) {
        var params = cls.getGenericParameters();
        if (params == null || params.length == 0) {
            return;
        }
        var arrayType = context.classInfoProvider().getObjectArrayType();
        var array = new WasmArrayNewFixed(arrayType);
        for (var param : params) {
            var nameRef = new WasmGetGlobal(context.strings().getStringConstant(param.getName()).global);
            array.getElements().add(new WasmCall(getVariableConstructor(), nameRef));
        }
        var fieldOffset = context.classInfoProvider().getClassTypeParametersOffset();
        var clsInfo = context.classInfoProvider().getClassInfo(cls.getName());
        var clsCls = context.classInfoProvider().getClassInfo("java.lang.Class");
        function.getBody().add(new WasmStructSet(clsCls.getStructure(), new WasmGetGlobal(clsInfo.getPointer()),
                fieldOffset, array));
    }

    private WasmFunction getVariableConstructor() {
        if (variableConstructor == null) {
            variableConstructor = context.functions().forStaticMethod(new MethodReference(
                    "java.lang.reflect.TypeVariableImpl", "create", ValueType.object("java.lang.String"),
                    ValueType.object("java.lang.reflect.TypeVariableImpl")));
        }
        return variableConstructor;
    }
}
