/*
 *  Copyright 2023 Alexey Andreev.
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

import java.util.ServiceLoader;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.binary.DataPrimitives;
import org.teavm.backend.wasm.binary.DataStructure;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsicFactory;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsicFactoryContext;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsicManager;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.dependency.DependencyAnalyzer;
import org.teavm.interop.Address;
import org.teavm.interop.Structure;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.GC;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeObject;

public class ServiceLoaderWasmSupport implements WasmIntrinsicFactory {
    private static final DataStructure ENTRY = new DataStructure(
            (byte) 4,
            DataPrimitives.ADDRESS,
            DataPrimitives.INT
    );
    private static final MethodReference CREATE_SERVICES_METHOD = new MethodReference(
            ServiceLoaderWasmSupport.class, "createServices", Address.class, Address.class);

    @Override
    public WasmIntrinsic create(WasmIntrinsicFactoryContext context) {
        return new ServiceLoaderIntrinsic(context.getServices().getService(ServiceLoaderInformation.class));
    }

    @Override
    public void contributeDependencies(DependencyAnalyzer analyzer) {
        analyzer.linkMethod(CREATE_SERVICES_METHOD);
    }

    private static class ServiceLoaderIntrinsic implements WasmIntrinsic {
        private ServiceLoaderInformation information;

        ServiceLoaderIntrinsic(ServiceLoaderInformation information) {
            this.information = information;
        }

        @Override
        public boolean isApplicable(MethodReference methodReference) {
            return methodReference.getClassName().equals(ServiceLoader.class.getName())
                    && methodReference.getName().equals("loadServices");
        }

        @Override
        public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
            var table = createServiceData(manager);
            var tableArg = new WasmInt32Constant(table);
            var serviceClassAddress = manager.generate(invocation.getArguments().get(0));
            return new WasmCall(manager.getNames().forMethod(CREATE_SERVICES_METHOD), tableArg,
                    serviceClassAddress);
        }

        private int createServiceData(WasmIntrinsicManager manager) {
            var writer = manager.getBinaryWriter();
            return writer.writeMap(
                    information.serviceTypes().toArray(new String[0]),
                    cls -> manager.getClassPointer(ValueType.object(cls)),
                    cls -> manager.getClassPointer(ValueType.object(cls)),
                    cls -> {
                        var implementations = information.serviceImplementations(cls);
                        var result = writer.getAddress();
                        var count = DataPrimitives.INT.createValue();
                        writer.append(count);
                        count.setInt(0, implementations.size());

                        for (var implementation : implementations) {
                            var entry = ENTRY.createValue();
                            writer.append(entry);

                            var implPointer = manager.getClassPointer(ValueType.object(implementation));
                            entry.setAddress(0, implPointer);

                            var constructorName = manager.getNames().forMethod(new MethodReference(
                                    implementation, "<init>", ValueType.VOID
                            ));
                            entry.setInt(1, manager.getFunctionPointer(constructorName));
                        }

                        return result;
                    }
            );
        }
    }

    static RuntimeObject createServices(Address table, RuntimeClass cls) {
        var entry = WasmRuntime.lookupResource(table, cls.toAddress());
        if (entry == null) {
            return null;
        }
        entry = entry.add(Address.sizeOf()).getAddress();
        var size = entry.getInt();
        entry = entry.add(4);
        RuntimeArray result = Allocator.allocateArray(cls, size).toStructure();
        var resultData = WasmRuntime.align(result.toAddress().add(Structure.sizeOf(RuntimeArray.class)),
                Address.sizeOf());
        for (var i = 0; i < size; ++i) {
            RuntimeObject obj = Allocator.allocate(entry.getAddress().toStructure()).toStructure();
            entry = entry.add(Address.sizeOf());
            WasmRuntime.callFunctionFromTable(entry.getInt(), obj);
            entry = entry.add(4);
            resultData.putAddress(obj.toAddress());
            resultData = resultData.add(Address.sizeOf());
            GC.writeBarrier(result);
        }
        return result;
    }
}
