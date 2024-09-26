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

import java.util.ArrayList;
import java.util.Collection;
import java.util.ServiceLoader;
import org.teavm.backend.wasm.generate.gc.classes.WasmGCClassInfoProvider;
import org.teavm.backend.wasm.generate.gc.methods.WasmGCGenerationUtil;
import org.teavm.backend.wasm.generators.gc.WasmGCCustomGenerator;
import org.teavm.backend.wasm.generators.gc.WasmGCCustomGeneratorContext;
import org.teavm.backend.wasm.generators.gc.WasmGCCustomGeneratorFactory;
import org.teavm.backend.wasm.generators.gc.WasmGCCustomGeneratorFactoryContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmCallReference;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmFunctionReference;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmNullBranch;
import org.teavm.backend.wasm.model.expression.WasmNullCondition;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSetGlobal;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmStructGet;
import org.teavm.backend.wasm.model.expression.WasmStructNewDefault;
import org.teavm.backend.wasm.model.expression.WasmStructSet;
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
            var initializer = generateInitializer(context);
            var emptyInitializer = generateEmptyInitializer(context);
            var arrayType = (WasmType.Reference) context.typeMapper().mapType(ValueType.parse(Object[].class));
            var servicesFunctionType = context.functionTypes().of(arrayType);
            var classLocal = new WasmLocal(context.typeMapper().mapType(ValueType.parse(Class.class)));
            function.add(classLocal);

            var classStruct = context.classInfoProvider().getClassInfo("java.lang.Class").getStructure();

            var initializerGlobalName = context.names().topLevel("teavm@initializeServicesRef");
            var global = new WasmGlobal(initializerGlobalName, initializer.getType().getReference(),
                    new WasmFunctionReference(initializer));
            context.module().globals.add(global);
            initializer.getBody().add(0, new WasmSetGlobal(global, new WasmFunctionReference(emptyInitializer)));

            function.getBody().add(new WasmCallReference(new WasmGetGlobal(global), initializer.getType()));

            var block = new WasmBlock(false);
            var servicesFunctionRef = new WasmStructGet(classStruct, new WasmGetLocal(classLocal),
                    context.classInfoProvider().getServicesOffset());
            var nullCheckedRef = new WasmNullBranch(WasmNullCondition.NULL, servicesFunctionRef, block);
            var getServices = new WasmCallReference(nullCheckedRef, servicesFunctionType);
            block.getBody().add(new WasmReturn(getServices));
            function.getBody().add(block);

            function.getBody().add(new WasmNullConstant(arrayType));
        }

        private WasmFunction generateInitializer(WasmGCCustomGeneratorContext context) {
            var function = new WasmFunction(context.functionTypes().of(null));
            function.setReferenced(true);
            function.setName(context.names().topLevel("teavm@initializeServices"));
            context.module().functions.add(function);

            var serviceTypes = information.serviceTypes();
            var classStruct = context.classInfoProvider().getClassInfo("java.lang.Class").getStructure();
            var fieldIndex = context.classInfoProvider().getServicesOffset();

            for (var serviceType : serviceTypes) {
                var implementations = information.serviceImplementations(serviceType);
                var providerFunction = generateServiceProvider(context, serviceType, implementations);
                var classInfo = context.classInfoProvider().getClassInfo(serviceType);
                var classRef = new WasmGetGlobal(classInfo.getPointer());
                var providerRef = new WasmFunctionReference(providerFunction);
                function.getBody().add(new WasmStructSet(classStruct, classRef, fieldIndex, providerRef));
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
            var util = new WasmGCGenerationUtil(context.classInfoProvider());
            function.getBody().add(util.allocateArrayWithElements(ValueType.parse(Object.class), () -> {
                var items = new ArrayList<WasmExpression>();
                for (var implementationName : implementations) {
                    items.add(instantiateService(context, function, implementationName));
                }
                return items;
            }));

            return function;
        }

        private WasmExpression instantiateService(WasmGCCustomGeneratorContext context,
                WasmFunction function, String implementationName) {
            var implementationInfo = context.classInfoProvider().getClassInfo(implementationName);
            var block = new WasmBlock(false);
            block.setType(context.typeMapper().mapType(ValueType.parse(Object.class)));
            var tmpVar = new WasmLocal(implementationInfo.getType());
            function.add(tmpVar);
            var structNew = new WasmSetLocal(tmpVar, new WasmStructNewDefault(
                    implementationInfo.getStructure()));
            block.getBody().add(structNew);

            var initClassField = new WasmStructSet(implementationInfo.getStructure(), new WasmGetLocal(tmpVar),
                    WasmGCClassInfoProvider.CLASS_FIELD_OFFSET, new WasmGetGlobal(implementationInfo.getPointer()));
            block.getBody().add(initClassField);

            var constructor = context.functions().forInstanceMethod(
                    new MethodReference(implementationName, INIT_METHOD));
            block.getBody().add(new WasmCall(constructor, new WasmGetLocal(tmpVar)));
            block.getBody().add(new WasmGetLocal(tmpVar));

            return block;
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
