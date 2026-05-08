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
import java.util.ServiceLoader;
import org.teavm.backend.wasm.generate.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.generate.methods.WasmGCGenerationUtil;
import org.teavm.backend.wasm.generators.WasmGCCustomGenerator;
import org.teavm.backend.wasm.generators.WasmGCCustomGeneratorContext;
import org.teavm.backend.wasm.generators.WasmGCCustomGeneratorFactory;
import org.teavm.backend.wasm.generators.WasmGCCustomGeneratorFactoryContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmFunctionReferenceInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmNullCondition;
import org.teavm.backend.wasm.model.instruction.WasmSetGlobalInstruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class ServiceLoaderWasmGCSupport implements WasmGCCustomGeneratorFactory {
    static final MethodDescriptor INIT_METHOD = new MethodDescriptor("<init>", ValueType.VOID);

    @Override
    public WasmGCCustomGenerator createGenerator(MethodReference methodRef,
            WasmGCCustomGeneratorFactoryContext context) {
        if (methodRef.getClassName().equals(ServiceLoader.class.getName())
                && methodRef.getName().equals("loadServices")) {
            return new ServiceLoaderIntrinsic(context.services().getService(ServiceLoaderInformation.class));
        }
        return null;
    }

    static class ServiceLoaderIntrinsic implements WasmGCCustomGenerator {
        private ServiceLoaderInformation information;

        ServiceLoaderIntrinsic(ServiceLoaderInformation information) {
            this.information = information;
        }

        @Override
        public void apply(MethodReference method, WasmFunction function, WasmGCCustomGeneratorContext context) {
            var classInfoStruct = context.classInfoProvider().reflectionTypes().classInfo();

            var initializer = generateInitializer(context);
            var emptyInitializer = generateEmptyInitializer(context);
            var arrayType = (WasmType.Reference) context.typeMapper().mapType(ValueType.parse(Object[].class));
            var servicesFunctionType = context.functionTypes().of(arrayType);
            var classLocal = new WasmLocal(classInfoStruct.structure().getReference());
            function.add(classLocal);

            var initializerGlobalName = context.names().topLevel("teavm@initializeServicesRef");
            var global = new WasmGlobal(initializerGlobalName, initializer.getType().getReference());
            global.getInitialValue().add(new WasmFunctionReferenceInstruction(initializer));
            context.module().globals.add(global);

            var ref = new WasmFunctionReferenceInstruction(emptyInitializer);
            initializer.getBody().addFirst(ref);
            ref.insertNext(new WasmSetGlobalInstruction(global));

            var body = function.getBody().builder();
            body.getGlobal(global).callReference(initializer.getType());

            var blockBody = body.block();
            blockBody.getLocal(classLocal).structGet(classInfoStruct.structure(), classInfoStruct.servicesIndex())
                    .nullBranch(WasmNullCondition.NULL, blockBody);
            blockBody.callReference(servicesFunctionType).return_();

            body.nullConst(arrayType);
        }

        private WasmFunction generateInitializer(WasmGCCustomGeneratorContext context) {
            var classInfoStruct = context.classInfoProvider().reflectionTypes().classInfo();

            var function = new WasmFunction(context.functionTypes().of(null));
            function.setReferenced(true);
            function.setName(context.names().topLevel("teavm@initializeServices"));
            context.module().functions.add(function);

            var serviceTypes = information.serviceTypes();
            var fieldIndex = classInfoStruct.servicesIndex();
            var body = function.getBody().builder();

            for (var serviceType : serviceTypes) {
                var implementations = information.serviceImplementations(serviceType);
                var providerFunction = generateServiceProvider(context, serviceType, implementations);
                var classInfo = context.classInfoProvider().getClassInfo(serviceType);
                body.getGlobal(classInfo.getPointer())
                        .funcRef(providerFunction)
                        .structSet(classInfoStruct.structure(), fieldIndex);
            }

            return function;
        }

        private WasmFunction generateServiceProvider(WasmGCCustomGeneratorContext context,
                String interfaceName, Collection<? extends String> implementations) {
            var functionType = context.functionTypes().of(context.typeMapper().mapType(
                    ValueType.parse(Object[].class)));
            var function = new WasmFunction(functionType);
            function.setName(context.names().topLevel(context.names().suggestForClass(interfaceName) + "@services"));
            function.setReferenced(true);
            context.module().functions.add(function);
            var body = function.getBody().builder();
            WasmGCGenerationUtil.allocateArray(context.classInfoProvider(), ValueType.parse(Object.class), body,
                    (wasmArray, b) -> {
                        for (var implementationName : implementations) {
                            instantiateService(context, function, implementationName, b);
                        }
                        b.arrayNewFixed(wasmArray, implementations.size());
                    });

            return function;
        }

        private void instantiateService(WasmGCCustomGeneratorContext context,
                WasmFunction function, String implementationName, WasmInstructionBuilder builder) {
            var implementationInfo = context.classInfoProvider().getClassInfo(implementationName);
            var tmpVar = new WasmLocal(implementationInfo.getType());
            function.add(tmpVar);

            builder.structNewDefault(implementationInfo.getStructure()).setLocal(tmpVar);
            builder.getLocal(tmpVar).getGlobal(implementationInfo.getVirtualTablePointer())
                    .structSet(implementationInfo.getStructure(), WasmGCClassInfoProvider.VT_FIELD_OFFSET);

            var constructor = context.functions().forInstanceMethod(
                    new MethodReference(implementationName, INIT_METHOD));
            builder.getLocal(tmpVar).call(constructor);

            builder.getLocal(tmpVar);
        }

        private WasmFunction generateEmptyInitializer(WasmGCCustomGeneratorContext context) {
            var function = new WasmFunction(context.functionTypes().of(null));
            function.setReferenced(true);
            function.setName(context.names().topLevel("teavm@emptyServicesInitializer"));
            context.module().functions.add(function);
            return function;
        }
    }
}
