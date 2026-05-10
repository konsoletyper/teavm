/*
 *  Copyright 2021 Alexey Andreev.
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
package org.teavm.classlib.impl;

import java.util.Collection;
import org.teavm.backend.wasm.BaseWasmFunctionRepository;
import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.generate.WasmGCNameProvider;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.generate.classes.WasmGCTypeMapper;
import org.teavm.backend.wasm.generate.methods.WasmGCGenerationUtil;
import org.teavm.backend.wasm.intrinsics.WasmGCBodyIntrinsic;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmFunctionReference;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmNullCondition;
import org.teavm.backend.wasm.model.instruction.WasmSetGlobal;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class ServiceLoaderWasmGCSupport implements WasmGCBodyIntrinsic {
    static final MethodDescriptor INIT_METHOD = new MethodDescriptor("<init>", ValueType.VOID);

    private ServiceLoaderInformation information;
    private WasmGCClassInfoProvider classInfoProvider;
    private BaseWasmFunctionRepository functions;
    private WasmFunctionTypes functionTypes;
    private WasmGCNameProvider names;
    private WasmGCTypeMapper typeMapper;
    private WasmModule module;

    public ServiceLoaderWasmGCSupport(ServiceLoaderInformation information, WasmGCClassInfoProvider classInfoProvider,
            BaseWasmFunctionRepository functions, WasmFunctionTypes functionTypes, WasmGCNameProvider names,
            WasmGCTypeMapper typeMapper, WasmModule module) {
        this.information = information;
        this.classInfoProvider = classInfoProvider;
        this.functions = functions;
        this.functionTypes = functionTypes;
        this.names = names;
        this.typeMapper = typeMapper;
        this.module = module;
    }

    @Override
    public void apply(MethodReference method, WasmFunction function) {

        var classInfoStruct = classInfoProvider.reflectionTypes().classInfo();

        var initializer = generateInitializer();
        var emptyInitializer = generateEmptyInitializer();
        var arrayType = (WasmType.Reference) typeMapper.mapType(ValueType.parse(Object[].class));
        var servicesFunctionType = functionTypes.of(arrayType);
        var classLocal = new WasmLocal(classInfoStruct.structure().getReference());
        function.add(classLocal);

        var initializerGlobalName = names.topLevel("teavm@initializeServicesRef");
        var global = new WasmGlobal(initializerGlobalName, initializer.getType().getReference());
        global.getInitialValue().add(new WasmFunctionReference(initializer));
        module.globals.add(global);

        var ref = new WasmFunctionReference(emptyInitializer);
        initializer.getBody().addFirst(ref);
        ref.insertNext(new WasmSetGlobal(global));

        var body = function.getBody().builder();
        body.getGlobal(global).callReference(initializer.getType());

        var blockBody = body.block();
        blockBody.getLocal(classLocal).structGet(classInfoStruct.structure(), classInfoStruct.servicesIndex())
                .nullBranch(WasmNullCondition.NULL, blockBody);
        blockBody.callReference(servicesFunctionType).return_();

        body.nullConst(arrayType);
    }

    private WasmFunction generateInitializer() {
        var classInfoStruct = classInfoProvider.reflectionTypes().classInfo();

        var function = new WasmFunction(functionTypes.of(null));
        function.setReferenced(true);
        function.setName(names.topLevel("teavm@initializeServices"));
        module.functions.add(function);

        var serviceTypes = information.serviceTypes();
        var fieldIndex = classInfoStruct.servicesIndex();
        var body = function.getBody().builder();

        for (var serviceType : serviceTypes) {
            var implementations = information.serviceImplementations(serviceType);
            var providerFunction = generateServiceProvider(serviceType, implementations);
            var classInfo = classInfoProvider.getClassInfo(serviceType);
            body.getGlobal(classInfo.getPointer())
                    .funcRef(providerFunction)
                    .structSet(classInfoStruct.structure(), fieldIndex);
        }

        return function;
    }

    private WasmFunction generateServiceProvider(String interfaceName, Collection<? extends String> implementations) {
        var functionType = functionTypes.of(typeMapper.mapType(ValueType.parse(Object[].class)));
        var function = new WasmFunction(functionType);
        function.setName(names.topLevel(names.suggestForClass(interfaceName) + "@services"));
        function.setReferenced(true);
        module.functions.add(function);
        var body = function.getBody().builder();
        WasmGCGenerationUtil.allocateArray(classInfoProvider, ValueType.parse(Object.class), body,
                (wasmArray, b) -> {
                    for (var implementationName : implementations) {
                        instantiateService(function, implementationName, b);
                    }
                    b.arrayNewFixed(wasmArray, implementations.size());
                });

        return function;
    }

    private void instantiateService(WasmFunction function, String implementationName, WasmInstructionBuilder builder) {
        var implementationInfo = classInfoProvider.getClassInfo(implementationName);
        var tmpVar = new WasmLocal(implementationInfo.getType());
        function.add(tmpVar);

        builder.structNewDefault(implementationInfo.getStructure()).setLocal(tmpVar);
        builder.getLocal(tmpVar).getGlobal(implementationInfo.getVirtualTablePointer())
                .structSet(implementationInfo.getStructure(), WasmGCClassInfoProvider.VT_FIELD_OFFSET);

        var constructor = functions.forInstanceMethod(
                new MethodReference(implementationName, INIT_METHOD));
        builder.getLocal(tmpVar).call(constructor);

        builder.getLocal(tmpVar);
    }

    private WasmFunction generateEmptyInitializer() {
        var function = new WasmFunction(functionTypes.of(null));
        function.setReferenced(true);
        function.setName(names.topLevel("teavm@emptyServicesInitializer"));
        module.functions.add(function);
        return function;
    }
}
