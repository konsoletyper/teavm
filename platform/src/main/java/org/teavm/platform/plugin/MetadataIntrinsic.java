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

import com.carrotsearch.hppc.ObjectIntHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.binary.DataPrimitives;
import org.teavm.backend.wasm.binary.DataStructure;
import org.teavm.backend.wasm.binary.DataType;
import org.teavm.backend.wasm.binary.DataValue;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsicManager;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.common.ServiceRepository;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.builders.ObjectResourceBuilder;
import org.teavm.platform.metadata.builders.ResourceArrayBuilder;
import org.teavm.platform.metadata.builders.ResourceMapBuilder;

class MetadataIntrinsic implements WasmIntrinsic {
    private ClassReaderSource classSource;
    private ClassLoader classLoader;
    private ServiceRepository services;
    private Properties properties;
    private Map<Class<?>, ResourceTypeDescriptor> descriptors = new HashMap<>();
    private Map<ResourceTypeDescriptor, DataStructure> resourceTypeCache = new HashMap<>();
    private MethodReference constructor;
    private MethodReference targetMethod;
    private MetadataGenerator generator;

    MetadataIntrinsic(ClassReaderSource classSource, ClassLoader classLoader,
            ServiceRepository services, Properties properties, MethodReference constructor,
            MethodReference targetMethod, MetadataGenerator generator) {
        this.classSource = classSource;
        this.classLoader = classLoader;
        this.services = services;
        this.properties = properties;
        this.constructor = constructor;
        this.targetMethod = targetMethod;
        this.generator = generator;
    }

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        return methodReference.equals(constructor);
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        var metadataContext = new DefaultMetadataGeneratorContext(classSource,
                manager.getResourceProvider(), classLoader, properties, services);
        var resource = generator.generateMetadata(metadataContext, targetMethod);
        int address = writeValue(manager, resource);

        return new WasmInt32Constant(address);
    }

    private int writeValue(WasmIntrinsicManager manager, Object value) {
        var stringPool = manager.getStringPool();
        var writer = manager.getBinaryWriter();
        if (value instanceof String) {
            return stringPool.getStringPointer((String) value);
        } else if (value instanceof Boolean) {
            DataValue dataValue = DataPrimitives.BYTE.createValue();
            dataValue.setByte(0, (Boolean) value ? (byte) 1 : 0);
            return writer.append(dataValue);
        } else if (value instanceof Integer) {
            DataValue dataValue = DataPrimitives.INT.createValue();
            dataValue.setInt(0, (Integer) value);
            return writer.append(dataValue);
        } else if (value instanceof Long) {
            DataValue dataValue = DataPrimitives.LONG.createValue();
            dataValue.setLong(0, (Long) value);
            return writer.append(dataValue);
        } else if (value instanceof ResourceMapBuilder) {
            return writeResource(manager, (ResourceMapBuilder<?>) value);
        } else if (value instanceof ResourceArrayBuilder) {
            return writeResource(manager, (ResourceArrayBuilder<?>) value);
        } else if (value instanceof ObjectResourceBuilder) {
            return writeResource(manager, (ObjectResourceBuilder) value);
        } else {
            throw new IllegalArgumentException("Don't know how to write resource: " + value);
        }
    }

    private int writeResource(WasmIntrinsicManager manager, ObjectResourceBuilder resource) {
        var writer = manager.getBinaryWriter();
        var descriptor = getDescriptor(manager.getClassHierarchy(), resource.getOutputClass());
        var structure = getDataStructure(descriptor);
        DataValue value = structure.createValue();
        int address = writer.append(value);

        var map = new ObjectIntHashMap<String>();
        var fieldNames = resource.fieldNames();
        for (var i = 0; i < fieldNames.length; ++i) {
            map.put(fieldNames[i], i);
        }

        var entries = List.copyOf(descriptor.getPropertyTypes().entrySet());
        for (var i = 0; i < entries.size(); ++i) {
            var entry = entries.get(i);
            var propertyName = entry.getKey();
            var propertyType = entry.getValue();
            int index = map.getOrDefault(propertyName, -1);
            Object propertyValue = resource.getValue(index);
            writeValueTo(manager, propertyType, value, i, propertyValue);
        }

        return address;
    }

    private int writeResource(WasmIntrinsicManager manager, ResourceMapBuilder<?> resourceMap) {
        var writer = manager.getBinaryWriter();
        var stringPool = manager.getStringPool();
        return writer.writeMap(
                resourceMap.values.keySet().toArray(new String[0]),
                String::hashCode,
                stringPool::getStringPointer,
                key -> writeValue(manager, resourceMap.values.get(key))
        );
    }

    private int writeResource(WasmIntrinsicManager manager, ResourceArrayBuilder<?> resourceArray) {
        var writer = manager.getBinaryWriter();

        DataValue sizeValue = DataPrimitives.ADDRESS.createValue();
        int start = writer.append(sizeValue);
        sizeValue.setAddress(0, resourceArray.values.size());

        DataValue[] arrayValues = new DataValue[resourceArray.values.size()];
        for (int i = 0; i < resourceArray.values.size(); ++i) {
            arrayValues[i] = DataPrimitives.ADDRESS.createValue();
            writer.append(arrayValues[i]);
        }

        for (int i = 0; i < resourceArray.values.size(); ++i) {
            arrayValues[i].setAddress(0, writeValue(manager, resourceArray.values.get(i)));
        }

        return start;
    }

    private void writeValueTo(WasmIntrinsicManager manager, ValueType type, DataValue target,
            int index, Object value) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    target.setByte(index, (boolean) value ? (byte) 1 : 0);
                    break;
                case BYTE:
                    target.setByte(index, (byte) value);
                    break;
                case SHORT:
                    target.setShort(index, (short) value);
                    break;
                case CHARACTER:
                    target.setShort(index, (short) (char) value);
                    break;
                case INTEGER:
                    target.setInt(index, (int) value);
                    break;
                case LONG:
                    target.setLong(index, (long) value);
                    break;
                case FLOAT:
                    target.setFloat(index, (float) value);
                    break;
                case DOUBLE:
                    target.setDouble(index, (double) value);
                    break;
            }
        } else if (type instanceof ValueType.Object) {
            var className = ((ValueType.Object) type).getClassName();
            switch (className) {
                case "java.lang.String":
                    target.setAddress(index,
                            value != null ? manager.getStringPool().getStringPointer((String) value) : 0);
                    break;
                case "org.teavm.platform.metadata.ResourceMap":
                    target.setAddress(index, writeResource(manager, (ResourceMapBuilder<?>) value));
                    break;
                case "org.teavm.platform.metadata.ResourceArray":
                    target.setAddress(index, writeResource(manager, (ResourceArrayBuilder<?>) value));
                    break;
                default:
                    int address = writeResource(manager, (ObjectResourceBuilder) value);
                    target.setAddress(index, address);
                    break;
            }
        }
    }

    private ResourceTypeDescriptor getDescriptor(ClassHierarchy hierarchy, Class<?> cls) {
        return descriptors.computeIfAbsent(cls, t -> {
            var classReader = classSource.get(t.getName());
            return new ResourceTypeDescriptor(hierarchy, classReader);
        });
    }

    private DataStructure getDataStructure(ResourceTypeDescriptor desc) {
        return resourceTypeCache.computeIfAbsent(desc, t -> {
            var propertyNames = new ArrayList<>(t.getPropertyTypes().keySet());
            var propertyDataTypes = new DataType[propertyNames.size()];
            for (int i = 0; i < propertyNames.size(); i++) {
                String propertyName = propertyNames.get(i);
                propertyDataTypes[i] = getDataType(t.getPropertyTypes().get(propertyName));
            }

            return new DataStructure((byte) 4, propertyDataTypes);
        });
    }

    private static DataType getDataType(ValueType cls) {
        if (cls instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) cls).getKind()) {
                case BOOLEAN:
                case BYTE:
                    return DataPrimitives.BYTE;
                case SHORT:
                case CHARACTER:
                    return DataPrimitives.SHORT;
                case INTEGER:
                    return DataPrimitives.INT;
                case LONG:
                    return DataPrimitives.LONG;
                case FLOAT:
                    return DataPrimitives.FLOAT;
                case DOUBLE:
                    return DataPrimitives.DOUBLE;
            }
        }
        return DataPrimitives.ADDRESS;
    }
}
