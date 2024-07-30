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

import org.teavm.backend.wasm.generate.TemporaryVariablePool;
import org.teavm.backend.wasm.generate.gc.WasmGCDeclarationsGenerator;
import org.teavm.backend.wasm.generate.gc.methods.WasmGCGenerationUtil;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmFunctionReference;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
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
        var mainFunctionCaller = new WasmFunction(declarationsGenerator.functionTypes.of(null));
        mainFunctionCaller.getBody().add(callInitializer());

        var tempVars = new TemporaryVariablePool(mainFunctionCaller);
        var genUtil = new WasmGCGenerationUtil(declarationsGenerator.classInfoProvider(), tempVars);
        var stringArrayType = declarationsGenerator.typeMapper()
                .mapType(ValueType.parse(String[].class));
        var arrayVar = tempVars.acquire(stringArrayType);
        genUtil.allocateArray(ValueType.parse(String.class), new WasmInt32Constant(0), null,
                arrayVar, mainFunctionCaller.getBody());
        var callToMainFunction = new WasmCall(mainFunction, new WasmGetLocal(arrayVar));
        mainFunctionCaller.getBody().add(callToMainFunction);
        mainFunctionCaller.getBody().add(new WasmReturn());
        tempVars.release(arrayVar);

        return mainFunctionCaller;
    }

    private void createInitializer() {
        if (initializer != null) {
            return;
        }
        var functionType = declarationsGenerator.functionTypes.of(null);
        initializer = new WasmFunction(functionType);
        declarationsGenerator.module.functions.add(initializer);
        initializerRef = new WasmGlobal("teavm_initializer", functionType.getReference(),
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
