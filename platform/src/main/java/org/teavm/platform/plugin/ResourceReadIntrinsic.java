/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.platform.plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.WasmRuntime;
import org.teavm.backend.wasm.binary.BinaryWriter;
import org.teavm.backend.wasm.generate.WasmClassGenerator;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsicManager;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmDrop;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmInt64Subtype;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat32;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat64;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.backend.wasm.model.expression.WasmLoadInt64;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.interop.Address;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.ResourceArray;
import org.teavm.platform.metadata.ResourceMap;

public class ResourceReadIntrinsic implements WasmIntrinsic {
    private static final MethodReference LOOKUP_METHOD = new MethodReference(WasmRuntime.class,
            "lookupResource", Address.class, String.class, Address.class);
    private static final MethodReference KEYS_METHOD = new MethodReference(WasmRuntime.class,
            "resourceMapKeys", Address.class, String[].class);

    private ClassReaderSource classSource;
    private ClassLoader classLoader;
    private Map<String, StructureDescriptor> typeDescriptorCache = new HashMap<>();

    public ResourceReadIntrinsic(ClassReaderSource classSource, ClassLoader classLoader) {
        this.classSource = classSource;
        this.classLoader = classLoader;
    }

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        return classSource.isSuperType(Resource.class.getTypeName(), methodReference.getClassName()).orElse(false);
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        if (invocation.getMethod().getClassName().equals(ResourceMap.class.getName())) {
            return applyForResourceMap(manager, invocation);
        } else if (invocation.getMethod().getClassName().equals(ResourceArray.class.getName())) {
            return applyForResourceArray(manager, invocation);
        }

        StructureDescriptor typeDescriptor = getTypeDescriptor(invocation.getMethod().getClassName());
        PropertyDescriptor property = typeDescriptor.layout.get(invocation.getMethod());

        WasmExpression base = manager.generate(invocation.getArguments().get(0));

        if (property.type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) property.type).getKind()) {
                case BOOLEAN:
                case BYTE:
                    return new WasmLoadInt32(1, base, WasmInt32Subtype.INT8, property.offset);
                case SHORT:
                    return new WasmLoadInt32(2, base, WasmInt32Subtype.INT16, property.offset);
                case CHARACTER:
                    return new WasmLoadInt32(2, base, WasmInt32Subtype.UINT16, property.offset);
                case INTEGER:
                    return new WasmLoadInt32(4, base, WasmInt32Subtype.INT32, property.offset);
                case LONG:
                    return new WasmLoadInt64(8, base, WasmInt64Subtype.INT64, property.offset);
                case FLOAT:
                    return new WasmLoadFloat32(4, base, property.offset);
                case DOUBLE:
                    return new WasmLoadFloat64(8, base, property.offset);
            }
        }

        return new WasmLoadInt32(4, base, WasmInt32Subtype.INT32, property.offset);
    }

    private WasmExpression applyForResourceArray(WasmIntrinsicManager manager, InvocationExpr invocation) {
        switch (invocation.getMethod().getName()) {
            case "get": {
                WasmExpression map = manager.generate(invocation.getArguments().get(0));
                WasmExpression index = manager.generate(invocation.getArguments().get(1));
                WasmExpression offset = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.SHL,
                        index, new WasmInt32Constant(2));
                WasmExpression address = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD,
                        map, offset);
                return new WasmLoadInt32(4, address, WasmInt32Subtype.INT32, 4);
            }
            case "size":
                return new WasmLoadInt32(4, manager.generate(invocation.getArguments().get(0)),
                        WasmInt32Subtype.INT32, 0);
            default:
                throw new AssertionError();
        }
    }

    private WasmExpression applyForResourceMap(WasmIntrinsicManager manager, InvocationExpr invocation) {
        switch (invocation.getMethod().getName()) {
            case "keys": {
                WasmExpression map = manager.generate(invocation.getArguments().get(0));
                WasmCall call = new WasmCall(manager.getNames().forMethod(KEYS_METHOD));
                call.getArguments().add(map);
                return call;
            }
            case "has": {
                WasmExpression map = manager.generate(invocation.getArguments().get(0));
                WasmExpression key = manager.generate(invocation.getArguments().get(1));
                WasmCall call = new WasmCall(manager.getNames().forMethod(LOOKUP_METHOD));
                call.getArguments().add(map);
                call.getArguments().add(key);
                return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.NE, call,
                        new WasmInt32Constant(0));
            }
            case "get": {
                WasmBlock block = new WasmBlock(false);
                block.setType(WasmType.INT32);

                WasmExpression map = manager.generate(invocation.getArguments().get(0));
                WasmExpression key = manager.generate(invocation.getArguments().get(1));
                WasmCall call = new WasmCall(manager.getNames().forMethod(LOOKUP_METHOD));
                call.getArguments().add(map);
                call.getArguments().add(key);
                WasmLocal entryVar = manager.getTemporary(WasmType.INT32);
                block.getBody().add(new WasmSetLocal(entryVar, call));

                WasmBranch ifNull = new WasmBranch(new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ,
                        new WasmGetLocal(entryVar), new WasmInt32Constant(0)), block);
                ifNull.setResult(new WasmInt32Constant(0));
                block.getBody().add(new WasmDrop(ifNull));

                block.getBody().add(new WasmLoadInt32(4, new WasmGetLocal(entryVar), WasmInt32Subtype.INT32, 4));
                return block;
            }
            default:
                throw new AssertionError();
        }
    }

    private StructureDescriptor getTypeDescriptor(String className) {
        return typeDescriptorCache.computeIfAbsent(className, n -> {
            Class<?> cls;
            try {
                cls = Class.forName(className, false, classLoader);
            } catch (ClassNotFoundException e) {
                throw new AssertionError("Class " + className + " should exist", e);
            }

            StructureDescriptor structureDescriptor = new StructureDescriptor();
            structureDescriptor.typeDescriptor = new ResourceTypeDescriptor(cls);
            calculateLayout(structureDescriptor.typeDescriptor, structureDescriptor.layout);
            return structureDescriptor;
        });
    }

    private void calculateLayout(ResourceTypeDescriptor typeDescriptor,
            Map<MethodReference, PropertyDescriptor> layout) {
        Map<String, Integer> propertyIndexes = new HashMap<>();
        List<String> propertyNames = new ArrayList<>(typeDescriptor.getPropertyTypes().keySet());
        for (int i = 0; i < propertyNames.size(); ++i) {
            propertyIndexes.put(propertyNames.get(i), i);
        }

        Method[] methods = new Method[typeDescriptor.getPropertyTypes().size()];
        for (Method method : typeDescriptor.getMethods().keySet()) {
            ResourceMethodDescriptor methodDescriptor = typeDescriptor.getMethods().get(method);
            if (methodDescriptor.getType() == ResourceAccessorType.SETTER) {
                continue;
            }
            String propertyName = methodDescriptor.getPropertyName();
            int index = propertyIndexes.get(propertyName);
            methods[index] = method;
        }

        int currentOffset = 0;
        for (Method method : methods) {
            MethodReference methodRef = MethodReference.parse(method);
            ValueType propertyType = methodRef.getReturnType();
            int size = WasmClassGenerator.getTypeSize(propertyType);
            currentOffset = BinaryWriter.align(currentOffset, size);

            PropertyDescriptor propertyDescriptor = new PropertyDescriptor();
            propertyDescriptor.offset = currentOffset;
            propertyDescriptor.type = propertyType;
            layout.put(methodRef, propertyDescriptor);

            currentOffset += size;
        }
    }

    static class StructureDescriptor {
        ResourceTypeDescriptor typeDescriptor;
        Map<MethodReference, PropertyDescriptor> layout = new HashMap<>();
    }

    static class PropertyDescriptor {
        int offset;
        ValueType type;
    }
}
