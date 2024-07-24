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

import java.util.HashMap;
import java.util.Map;
import org.teavm.backend.lowlevel.generate.NameProvider;
import org.teavm.backend.wasm.generate.WasmGeneratorUtil;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class WasmFunctionRepository implements BaseWasmFunctionRepository {
    private WasmModule module;
    private WasmFunctionTypes functionTypes;
    private NameProvider nameProvider;
    private Map<MethodReference, WasmFunction> staticMethods = new HashMap<>();
    private Map<MethodReference, WasmFunction> instanceMethods = new HashMap<>();
    private Map<String, WasmFunction> classInitializers = new HashMap<>();
    private Map<ValueType, WasmFunction> supertypes = new HashMap<>();

    public WasmFunctionRepository(WasmModule module, WasmFunctionTypes functionTypes, NameProvider nameProvider) {
        this.module = module;
        this.functionTypes = functionTypes;
        this.nameProvider = nameProvider;
    }

    public WasmFunction forMethod(MethodReader method) {
        return forMethod(method.getReference(), method.hasModifier(ElementModifier.STATIC));
    }

    public WasmFunction forMethod(MethodReference reference, boolean isStatic) {
        return isStatic ? forStaticMethod(reference) : forInstanceMethod(reference);
    }

    @Override
    public WasmFunction forStaticMethod(MethodReference reference) {
        return staticMethods.computeIfAbsent(reference, key -> {
            var wasmParams = new WasmType[key.parameterCount()];
            for (var i = 0; i < key.parameterCount(); ++i) {
                wasmParams[i] = WasmGeneratorUtil.mapType(reference.parameterType(i));
            }
            var wasmType = functionTypes.of(WasmGeneratorUtil.mapType(key.getReturnType()), wasmParams);
            var wasmFunction = new WasmFunction(wasmType);
            wasmFunction.setName(nameProvider.forMethod(key));
            wasmFunction.setJavaMethod(key);
            module.functions.add(wasmFunction);
            return wasmFunction;
        });
    }

    @Override
    public WasmFunction forInstanceMethod(MethodReference reference) {
        return instanceMethods.computeIfAbsent(reference, key -> {
            var wasmParams = new WasmType[key.parameterCount() + 1];
            wasmParams[0] = WasmGeneratorUtil.mapType(ValueType.object(reference.getClassName()));
            for (var i = 0; i < key.parameterCount(); ++i) {
                wasmParams[i + 1] = WasmGeneratorUtil.mapType(reference.parameterType(i));
            }
            var wasmType = functionTypes.of(WasmGeneratorUtil.mapType(key.getReturnType()), wasmParams);
            var wasmFunction = new WasmFunction(wasmType);
            wasmFunction.setName(nameProvider.forMethod(key));
            wasmFunction.setJavaMethod(key);
            module.functions.add(wasmFunction);
            return wasmFunction;
        });
    }

    public WasmFunction forClassInitializer(String className) {
        return classInitializers.computeIfAbsent(className, key -> {
            var wasmFunction = new WasmFunction(functionTypes.of(null));
            wasmFunction.setName(nameProvider.forClassInitializer(key));
            module.functions.add(wasmFunction);
            return wasmFunction;
        });
    }

    public WasmFunction forSupertype(ValueType type) {
        return supertypes.computeIfAbsent(type, key -> {
            var wasmFunction = new WasmFunction(functionTypes.of(WasmType.INT32, WasmType.INT32));
            wasmFunction.setName(nameProvider.forSupertypeFunction(key));
            module.functions.add(wasmFunction);
            return wasmFunction;
        });
    }
}
