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
package org.teavm.backend.wasm;

import org.teavm.backend.wasm.generate.gc.WasmGCDeclarationsGenerator;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmDrop;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmFunctionReference;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.runtime.WasmGCSupport;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class WasmGCModuleGenerator {
    private WasmGCDeclarationsGenerator declarationsGenerator;
    private WasmFunction initializer;
    private WasmGlobal initializerRef;

    public WasmGCModuleGenerator(WasmGCDeclarationsGenerator declarationsGenerator) {
        this.declarationsGenerator = declarationsGenerator;
    }

    public void generate() {
        declarationsGenerator.generate();
        if (initializer != null) {
            fillInitializer();
        }
    }

    private void fillInitializer() {
        declarationsGenerator.contributeToInitializer(initializer);
        initializer.getBody().add(new WasmReturn());
    }

    public WasmFunction generateMainFunction(String entryPoint) {
        var mainFunction = declarationsGenerator.functions().forStaticMethod(new MethodReference(entryPoint,
                "main", ValueType.parse(String[].class), ValueType.VOID));
        var stringArrayType = declarationsGenerator.typeMapper()
                .mapType(ValueType.parse(String[].class));
        var mainFunctionCaller = new WasmFunction(declarationsGenerator.functionTypes.of(null, stringArrayType));
        var argsLocal = new WasmLocal(stringArrayType, "args");
        declarationsGenerator.module.functions.add(mainFunctionCaller);
        mainFunctionCaller.getBody().add(callInitializer());

        var callToMainFunction = new WasmCall(mainFunction, new WasmGetLocal(argsLocal));
        mainFunctionCaller.getBody().add(callToMainFunction);
        mainFunctionCaller.getBody().add(new WasmReturn());

        return mainFunctionCaller;
    }

    public WasmFunction generateCreateStringBuilderFunction() {
        var function = declarationsGenerator.functions().forStaticMethod(new MethodReference(
                WasmGCSupport.class, "createStringBuilder", StringBuilder.class));
        var caller = new WasmFunction(function.getType());
        caller.getBody().add(callInitializer());
        caller.getBody().add(new WasmReturn(new WasmCall(function)));
        declarationsGenerator.module.functions.add(caller);
        return caller;
    }

    public WasmFunction generateCreateStringArrayFunction() {
        var function = declarationsGenerator.functions().forStaticMethod(new MethodReference(
                WasmGCSupport.class, "createStringArray", int.class, String[].class));
        var caller = new WasmFunction(function.getType());
        var sizeLocal = new WasmLocal(WasmType.INT32, "length");
        caller.add(sizeLocal);
        caller.getBody().add(callInitializer());
        caller.getBody().add(new WasmReturn(new WasmCall(function, new WasmGetLocal(sizeLocal))));
        declarationsGenerator.module.functions.add(caller);
        return caller;
    }

    public WasmFunction generateAppendCharFunction() {
        var function = declarationsGenerator.functions().forInstanceMethod(new MethodReference(
                StringBuilder.class, "append", char.class, StringBuilder.class));
        var stringBuilderType = declarationsGenerator.typeMapper().mapType(ValueType.parse(StringBuilder.class));
        var caller = new WasmFunction(declarationsGenerator.functionTypes.of(null, stringBuilderType, WasmType.INT32));
        var stringBuilderLocal = new WasmLocal(stringBuilderType, "stringBuilder");
        var codeLocal = new WasmLocal(WasmType.INT32, "charCode");
        caller.add(stringBuilderLocal);
        caller.add(codeLocal);
        caller.getBody().add(callInitializer());
        caller.getBody().add(new WasmDrop(new WasmCall(function, new WasmGetLocal(stringBuilderLocal),
                new WasmGetLocal(codeLocal))));
        caller.getBody().add(new WasmReturn());
        declarationsGenerator.module.functions.add(caller);
        return caller;
    }

    public WasmFunction generateBuildStringFunction() {
        var function = declarationsGenerator.functions().forInstanceMethod(new MethodReference(
                StringBuilder.class, "toString", String.class));
        var stringBuilderType = declarationsGenerator.typeMapper().mapType(ValueType.parse(StringBuilder.class));
        var stringType = declarationsGenerator.typeMapper().mapType(ValueType.parse(String.class));
        var caller = new WasmFunction(declarationsGenerator.functionTypes.of(stringType, stringBuilderType));
        var stringBuilderLocal = new WasmLocal(stringBuilderType, "stringBuilder");
        caller.add(stringBuilderLocal);
        caller.getBody().add(callInitializer());
        caller.getBody().add(new WasmReturn(new WasmCall(function, new WasmGetLocal(stringBuilderLocal))));
        declarationsGenerator.module.functions.add(caller);
        return caller;
    }

    public WasmFunction generateSetToStringArrayFunction() {
        var function = declarationsGenerator.functions().forStaticMethod(new MethodReference(
                WasmGCSupport.class, "setToStringArray", String[].class, int.class, String.class, void.class));
        var stringArrayType = declarationsGenerator.typeMapper().mapType(ValueType.parse(String[].class));
        var stringType = declarationsGenerator.typeMapper().mapType(ValueType.parse(String.class));
        var caller = new WasmFunction(function.getType());
        var arrayLocal = new WasmLocal(stringArrayType, "array");
        var indexLocal = new WasmLocal(WasmType.INT32, "index");
        var valueLocal = new WasmLocal(stringType, "string");
        caller.add(arrayLocal);
        caller.add(indexLocal);
        caller.add(valueLocal);
        caller.getBody().add(callInitializer());
        caller.getBody().add(new WasmReturn(new WasmCall(function, new WasmGetLocal(arrayLocal),
                new WasmGetLocal(indexLocal), new WasmGetLocal(valueLocal))));
        declarationsGenerator.module.functions.add(caller);
        return caller;
    }

    public WasmFunction generateStringLengthFunction() {
        var function = declarationsGenerator.functions().forInstanceMethod(new MethodReference(
                String.class, "length", int.class));
        var stringType = declarationsGenerator.typeMapper().mapType(ValueType.parse(String.class));
        var caller = new WasmFunction(function.getType());
        var stringLocal = new WasmLocal(stringType, "string");
        caller.add(stringLocal);
        caller.getBody().add(callInitializer());
        caller.getBody().add(new WasmReturn(new WasmCall(function, new WasmGetLocal(stringLocal))));
        declarationsGenerator.module.functions.add(caller);
        return caller;
    }

    public WasmFunction generateCharAtFunction() {
        var function = declarationsGenerator.functions().forInstanceMethod(new MethodReference(
                String.class, "charAt", int.class, char.class));
        var stringType = declarationsGenerator.typeMapper().mapType(ValueType.parse(String.class));
        var caller = new WasmFunction(function.getType());
        var stringLocal = new WasmLocal(stringType, "string");
        var indexLocal = new WasmLocal(WasmType.INT32, "index");
        caller.add(stringLocal);
        caller.add(indexLocal);
        caller.getBody().add(callInitializer());
        caller.getBody().add(new WasmReturn(new WasmCall(function, new WasmGetLocal(stringLocal),
                new WasmGetLocal(indexLocal))));
        declarationsGenerator.module.functions.add(caller);
        return caller;
    }

    private void createInitializer() {
        if (initializer != null) {
            return;
        }
        var functionType = declarationsGenerator.functionTypes.of(null);
        initializer = new WasmFunction(functionType);
        initializer.setReferenced(true);
        declarationsGenerator.module.functions.add(initializer);
        initializerRef = new WasmGlobal("teavm@initializer", functionType.getNonNullReference(),
                new WasmFunctionReference(initializer));
        declarationsGenerator.module.globals.add(initializerRef);
        initializer.getBody().add(new WasmSetGlobal(initializerRef,
                new WasmFunctionReference(declarationsGenerator.dummyInitializer())));
    }

    private WasmExpression callInitializer() {
        createInitializer();
        return new WasmCallReference(new WasmGetGlobal(initializerRef), declarationsGenerator.functionTypes.of(null));
    }
}
