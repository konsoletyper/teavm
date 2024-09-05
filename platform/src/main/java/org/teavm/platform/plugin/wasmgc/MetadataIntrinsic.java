/*
 *  Copyright 2024 konsoletyper.
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
package org.teavm.platform.plugin.wasmgc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmFloat32Constant;
import org.teavm.backend.wasm.model.expression.WasmFloat64Constant;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt64Constant;
import org.teavm.backend.wasm.model.expression.WasmNullConstant;
import org.teavm.backend.wasm.model.expression.WasmStructNew;
import org.teavm.common.HashUtils;
import org.teavm.common.ServiceRepository;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.ResourceArray;
import org.teavm.platform.metadata.ResourceMap;
import org.teavm.platform.plugin.DefaultMetadataGeneratorContext;
import org.teavm.platform.plugin.ResourceAccessorType;
import org.teavm.platform.plugin.ResourceTypeDescriptor;
import org.teavm.platform.plugin.ResourceTypeDescriptorProvider;

class MetadataIntrinsic implements WasmGCIntrinsic {
    private Properties properties;
    private ServiceRepository services;
    private MetadataGenerator generator;
    private WasmGlobal global;

    MetadataIntrinsic(Properties properties, ServiceRepository services,
            MetadataGenerator generator) {
        this.properties = properties;
        this.services = services;
        this.generator = generator;
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var global = getGlobal(invocation.getMethod(), context);
        return new WasmGetGlobal(global);
    }

    private WasmGlobal getGlobal(MethodReference method, WasmGCIntrinsicContext context) {
        if (global == null) {
            var genContext = new DefaultMetadataGeneratorContext(context.hierarchy().getClassSource(),
                    context.classLoader(), properties, services);
            var metadata = generator.generateMetadata(genContext, method);
            var type = context.typeMapper().mapType(method.getReturnType());
            var name = context.names().topLevel(context.names().suggestForMethod(method));
            var initialValue = generateMetadata(context, metadata, type);
            global = new WasmGlobal(name, type, initialValue);
            context.module().globals.add(global);
        }
        return global;
    }

    private WasmExpression generateMetadata(WasmGCIntrinsicContext context, Object value, WasmType expectedType) {
        if (value == null) {
            return new WasmNullConstant((WasmType.Reference) expectedType);
        } else if (value instanceof String) {
            return new WasmGetGlobal(context.strings().getStringConstant((String) value).global);
        } else if (value instanceof Boolean) {
            return new WasmInt32Constant((Boolean) value ? 1 : 0);
        } else if (value instanceof Integer) {
            return new WasmInt32Constant((Integer) value);
        } else if (value instanceof Long) {
            return new WasmInt64Constant((Long) value);
        } else if (value instanceof Byte) {
            return new WasmInt32Constant((Byte) value);
        } else if (value instanceof Short) {
            return new WasmInt32Constant((Short) value);
        } else if (value instanceof Character) {
            return new WasmInt32Constant((Character) value);
        } else if (value instanceof Float) {
            return new WasmFloat32Constant((Float) value);
        } else if (value instanceof Double) {
            return new WasmFloat64Constant((Double) value);
        } else if (value instanceof ResourceArray) {
            var array = (ResourceArray<?>) value;
            var type = (WasmType.CompositeReference) context.typeMapper().mapType(
                    ValueType.object(ResourceArray.class.getName()));
            var arrayType = (WasmArray) type.composite;
            var result = new WasmArrayNewFixed(arrayType);
            for (var i = 0; i < array.size(); ++i) {
                result.getElements().add(generateMetadata(context, array.get(i),
                        arrayType.getElementType().asUnpackedType()));
            }
            return result;
        } else if (value instanceof ResourceMap) {
            return generateMapResource(context, (ResourceMap<?>) value);
        } else if (value instanceof ResourceTypeDescriptorProvider) {
            var descriptor = ((ResourceTypeDescriptorProvider) value).getDescriptor();
            return generateObjectResource(context, value, descriptor);
        } else {
            throw new IllegalArgumentException("Don't know how to write resource: " + value);
        }
    }

    private WasmExpression generateMapResource(WasmGCIntrinsicContext context, ResourceMap<?> map) {
        var hashTable = HashUtils.createHashTable(map.keys());
        var type = (WasmType.CompositeReference) context.typeMapper().mapType(
                ValueType.object(ResourceMap.class.getName()));
        var arrayType = (WasmArray) type.composite;
        var entryType = (WasmType.CompositeReference) context.typeMapper().mapType(
                ValueType.object(ResourceMapEntry.class.getName()));
        var entryStruct = (WasmStructure) entryType.composite;

        var expr = new WasmArrayNewFixed(arrayType);
        for (var key : hashTable) {
            if (key == null) {
                expr.getElements().add(new WasmNullConstant(entryType));
            } else {
                var value = map.get(key);
                var wasmValue = generateMetadata(context, value, WasmType.Reference.EQ);
                var entryExpr = new WasmStructNew(entryStruct);
                var keyConstant = context.strings().getStringConstant(key);
                entryExpr.getInitializers().add(new WasmGetGlobal(keyConstant.global));
                entryExpr.getInitializers().add(wasmValue);
                expr.getElements().add(entryExpr);
            }
        }
        return expr;
    }

    private WasmExpression generateObjectResource(WasmGCIntrinsicContext context, Object value,
            ResourceTypeDescriptor descriptor) {
        var javaItf = descriptor.getRootInterface();
        var cls = context.hierarchy().getClassSource().get(javaItf.getName());
        var getterMap = new HashMap<String, Method>();
        for (var entry : descriptor.getMethods().entrySet()) {
            if (entry.getValue().getType() == ResourceAccessorType.GETTER) {
                getterMap.put(entry.getValue().getPropertyName(), entry.getKey());
            }
        }

        var wasmType = (WasmType.CompositeReference) context.typeMapper().mapType(ValueType.object(cls.getName()));
        var wasmStruct = (WasmStructure) wasmType.composite;
        var expr = new WasmStructNew(wasmStruct);
        for (var field : collectFields(context.hierarchy().getClassSource(), cls)) {
            var getter = getterMap.get(field.name);
            Object fieldValue;
            try {
                fieldValue = getter.invoke(value);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            expr.getInitializers().add(generateMetadata(context, fieldValue,
                    context.typeMapper().mapType(field.type)));
        }

        return expr;
    }

    private List<FieldDescriptor> collectFields(ClassReaderSource classes, ClassReader cls) {
        var fields = new ArrayList<FieldDescriptor>();
        while (cls != null) {
            for (var method : cls.getMethods()) {
                var annot = method.getAnnotations().get(FieldMarker.class.getName());
                if (annot != null) {
                    fields.add(new FieldDescriptor(annot.getValue("index").getInt(),
                            annot.getValue("value").getString(), method.getResultType()));
                }
            }
            cls = classes.get(cls.getParent());
            if (cls.getName().equals(Resource.class.getName())) {
                break;
            }
        }
        fields.sort(Comparator.comparingInt(f -> f.index));
        return fields;
    }

    private static class FieldDescriptor {
        final int index;
        final String name;
        final ValueType type;

        FieldDescriptor(int index, String name, ValueType type) {
            this.index = index;
            this.name = name;
            this.type = type;
        }
    }
}
