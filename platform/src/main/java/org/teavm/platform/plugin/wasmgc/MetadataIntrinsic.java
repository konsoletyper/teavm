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

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.common.HashUtils;
import org.teavm.common.ServiceRepository;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.ResourceArray;
import org.teavm.platform.metadata.ResourceMap;
import org.teavm.platform.metadata.builders.ObjectResourceBuilder;
import org.teavm.platform.metadata.builders.ResourceArrayBuilder;
import org.teavm.platform.metadata.builders.ResourceMapBuilder;
import org.teavm.platform.plugin.DefaultMetadataGeneratorContext;
import org.teavm.platform.plugin.ResourceTypeDescriptor;

class MetadataIntrinsic implements WasmGCIntrinsic {
    private Properties properties;
    private ServiceRepository services;
    private MetadataGenerator generator;
    private WasmGlobal global;
    private Map<Class<?>, ResourceTypeDescriptor> descriptors = new HashMap<>();
    private Map<Class<?>, ObjectIntMap<String>> fieldIndexMap = new HashMap<>();

    MetadataIntrinsic(Properties properties, ServiceRepository services, MetadataGenerator generator) {
        this.properties = properties;
        this.services = services;
        this.generator = generator;
    }

    @Override
    public void apply(InvocationExpr invocation, WasmGCIntrinsicContext context, WasmInstructionBuilder builder) {
        builder.getGlobal(getGlobal(invocation.getMethod(), context));
    }

    private WasmGlobal getGlobal(MethodReference method, WasmGCIntrinsicContext context) {
        if (global == null) {
            var genContext = new DefaultMetadataGeneratorContext(context.hierarchy().getClassSource(),
                    context.resources(), context.classLoader(), properties, services);
            var metadata = generator.generateMetadata(genContext, method);
            var type = context.typeMapper().mapType(method.getReturnType());
            var name = context.names().topLevel(context.names().suggestForMethod(method));
            global = new WasmGlobal(name, type);
            generateMetadata(context, metadata, type, global.getInitialValue().builder());
            context.module().globals.add(global);
        }
        return global;
    }

    private void generateMetadata(WasmGCIntrinsicContext context, Object value, WasmType expectedType,
            WasmInstructionBuilder builder) {
        if (value == null) {
            builder.nullConst((WasmType.Reference) expectedType);
        } else if (value instanceof String) {
            builder.getGlobal(context.strings().getStringConstant((String) value).global);
        } else if (value instanceof Boolean) {
            builder.i32Const((Boolean) value ? 1 : 0);
        } else if (value instanceof Integer) {
            builder.i32Const((Integer) value);
        } else if (value instanceof Long) {
            builder.i64Const((Long) value);
        } else if (value instanceof Byte) {
            builder.i32Const((Byte) value);
        } else if (value instanceof Short) {
            builder.i32Const((Short) value);
        } else if (value instanceof Character) {
            builder.i32Const((Character) value);
        } else if (value instanceof Float) {
            builder.f32Const((Float) value);
        } else if (value instanceof Double) {
            builder.f64Const((Double) value);
        } else if (value instanceof ResourceArrayBuilder) {
            var array = (ResourceArrayBuilder<?>) value;
            var type = (WasmType.CompositeReference) context.typeMapper().mapType(
                    ValueType.object(ResourceArray.class.getName()));
            var arrayType = (WasmArray) type.composite;
            for (var i = 0; i < array.values.size(); ++i) {
                generateMetadata(context, array.values.get(i),
                        arrayType.getElementType().asUnpackedType(), builder);
            }
            builder.arrayNewFixed(arrayType, array.values.size());
        } else if (value instanceof ResourceMapBuilder) {
            generateMapResource(context, (ResourceMapBuilder<?>) value, builder);
        } else if (value instanceof ObjectResourceBuilder) {
            var objBuilder = (ObjectResourceBuilder) value;
            var descriptor = getDescriptor(context.hierarchy(), objBuilder);
            generateObjectResource(context, objBuilder, descriptor, builder);
        } else {
            throw new IllegalArgumentException("Don't know how to write resource: " + value);
        }
    }

    private ResourceTypeDescriptor getDescriptor(ClassHierarchy hierarchy, ObjectResourceBuilder builder) {
        return descriptors.computeIfAbsent(builder.getOutputClass(), key ->
                new ResourceTypeDescriptor(hierarchy, hierarchy.getClassSource().get(key.getName())));
    }

    private void generateMapResource(WasmGCIntrinsicContext context, ResourceMapBuilder<?> map,
            WasmInstructionBuilder builder) {
        var hashTable = HashUtils.createHashTable(map.values.keySet().toArray(new String[0]));
        var type = (WasmType.CompositeReference) context.typeMapper().mapType(
                ValueType.object(ResourceMap.class.getName()));
        var arrayType = (WasmArray) type.composite;
        var entryType = (WasmType.CompositeReference) context.typeMapper().mapType(
                ValueType.object(ResourceMapEntry.class.getName()));
        var entryStruct = (WasmStructure) entryType.composite;

        for (var key : hashTable) {
            if (key == null) {
                builder.nullConst(entryType);
            } else {
                var value = map.values.get(key);
                var keyConstant = context.strings().getStringConstant(key);
                builder.getGlobal(keyConstant.global);
                generateMetadata(context, value, WasmType.EQ, builder);
                builder.structNew(entryStruct);
            }
        }
        builder.arrayNewFixed(arrayType, hashTable.length);
    }

    private void generateObjectResource(WasmGCIntrinsicContext context, ObjectResourceBuilder value,
            ResourceTypeDescriptor descriptor, WasmInstructionBuilder builder) {
        var javaItf = descriptor.getRootInterface();
        var cls = context.hierarchy().getClassSource().get(javaItf.getName());

        var wasmType = (WasmType.CompositeReference) context.typeMapper().mapType(ValueType.object(cls.getName()));
        var wasmStruct = (WasmStructure) wasmType.composite;
        var map = fieldIndexMap.computeIfAbsent(value.getOutputClass(), key -> {
            var result = new ObjectIntHashMap<String>();
            var names = value.fieldNames();
            for (var i = 0; i < names.length; ++i) {
                result.put(names[i], i);
            }
            return result;
        });
        for (var field : collectFields(context.hierarchy().getClassSource(), cls)) {
            var index = map.getOrDefault(field.name, -1);
            var fieldValue = value.getValue(index);
            generateMetadata(context, fieldValue, context.typeMapper().mapType(field.type), builder);
        }
        builder.structNew(wasmStruct);
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
