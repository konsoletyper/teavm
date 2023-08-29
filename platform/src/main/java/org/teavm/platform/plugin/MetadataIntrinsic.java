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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.binary.BinaryWriter;
import org.teavm.backend.wasm.binary.DataPrimitives;
import org.teavm.backend.wasm.binary.DataStructure;
import org.teavm.backend.wasm.binary.DataType;
import org.teavm.backend.wasm.binary.DataValue;
import org.teavm.backend.wasm.generate.WasmStringPool;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsicManager;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.common.ServiceRepository;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReference;
import org.teavm.platform.metadata.MetadataGenerator;
import org.teavm.platform.metadata.Resource;
import org.teavm.platform.metadata.ResourceArray;
import org.teavm.platform.metadata.ResourceMap;

class MetadataIntrinsic implements WasmIntrinsic {
    private ClassReaderSource classSource;
    private ClassLoader classLoader;
    private ServiceRepository services;
    private Properties properties;
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
        DefaultMetadataGeneratorContext metadataContext = new DefaultMetadataGeneratorContext(classSource,
                classLoader, properties, services);
        Resource resource = generator.generateMetadata(metadataContext, targetMethod);
        int address = writeValue(manager.getBinaryWriter(), manager.getStringPool(), resource);

        return new WasmInt32Constant(address);
    }

    private int writeValue(BinaryWriter writer, WasmStringPool stringPool, Object value) {
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
        } else if (value instanceof ResourceMap) {
            return writeResource(writer, stringPool, (ResourceMap<?>) value);
        } else if (value instanceof ResourceArray) {
            return writeResource(writer, stringPool, (ResourceArray<?>) value);
        } else if (value instanceof ResourceTypeDescriptorProvider && value instanceof Resource) {
            return writeResource(writer, stringPool, (ResourceTypeDescriptorProvider) value);
        } else {
            throw new IllegalArgumentException("Don't know how to write resource: " + value);
        }
    }

    private int writeResource(BinaryWriter writer, WasmStringPool stringPool,
            ResourceTypeDescriptorProvider resourceType) {
        DataStructure structure = getDataStructure(resourceType.getDescriptor());
        DataValue value = structure.createValue();
        int address = writer.append(value);
        Object[] propertyValues = resourceType.getValues();

        for (String propertyName : resourceType.getDescriptor().getPropertyTypes().keySet()) {
            Class<?> propertyType = resourceType.getDescriptor().getPropertyTypes().get(propertyName);
            int index = resourceType.getPropertyIndex(propertyName);
            Object propertyValue = propertyValues[index];
            writeValueTo(writer, stringPool, propertyType, value, index, propertyValue);
        }

        return address;
    }

    private int writeResource(BinaryWriter writer, WasmStringPool stringPool, ResourceMap<?> resourceMap) {
        return writer.writeMap(
                resourceMap.keys(),
                String::hashCode,
                stringPool::getStringPointer,
                key -> writeValue(writer, stringPool, resourceMap.get(key))
        );
    }

    private int writeResource(BinaryWriter writer, WasmStringPool stringPool, ResourceArray<?> resourceArray) {
        DataValue sizeValue = DataPrimitives.ADDRESS.createValue();
        int start = writer.append(sizeValue);
        sizeValue.setAddress(0, resourceArray.size());

        DataValue[] arrayValues = new DataValue[resourceArray.size()];
        for (int i = 0; i < resourceArray.size(); ++i) {
            arrayValues[i] = DataPrimitives.ADDRESS.createValue();
            writer.append(arrayValues[i]);
        }

        for (int i = 0; i < resourceArray.size(); ++i) {
            arrayValues[i].setAddress(0, writeValue(writer, stringPool, resourceArray.get(i)));
        }

        return start;
    }

    private void writeValueTo(BinaryWriter writer, WasmStringPool stringPool, Class<?> type, DataValue target,
            int index, Object value) {
        if (type == String.class) {
            target.setAddress(index, value != null ? stringPool.getStringPointer((String) value) : 0);
        } else if (type == boolean.class) {
            target.setByte(index, (boolean) value ? (byte) 1 : 0);
        } else if (type == byte.class) {
            target.setByte(index, (byte) value);
        } else if (type == short.class) {
            target.setShort(index, (short) value);
        } else if (type == char.class) {
            target.setShort(index, (short) (char) value);
        } else if (type == int.class) {
            target.setInt(index, (int) value);
        } else if (type == long.class) {
            target.setLong(index, (long) value);
        } else if (type == float.class) {
            target.setFloat(index, (float) value);
        } else if (type == double.class) {
            target.setDouble(index, (double) value);
        } else if (value instanceof ResourceTypeDescriptorProvider && value instanceof Resource) {
            int address = writeResource(writer, stringPool, (ResourceTypeDescriptorProvider) value);
            target.setAddress(index, address);
        } else if (value == null) {
            target.setAddress(index, 0);
        } else if (value instanceof ResourceMap) {
            target.setAddress(index, writeResource(writer, stringPool, (ResourceMap<?>) value));
        } else if (value instanceof ResourceArray) {
            target.setAddress(index, writeResource(writer, stringPool, (ResourceArray<?>) value));
        } else {
            throw new IllegalArgumentException("Don't know how to write resource: " + value);
        }
    }

    private DataStructure getDataStructure(ResourceTypeDescriptor descriptor) {
        return resourceTypeCache.computeIfAbsent(descriptor, t -> {
            List<String> propertyNames = new ArrayList<>(descriptor.getPropertyTypes().keySet());
            DataType[] propertyDataTypes = new DataType[propertyNames.size()];
            for (int i = 0; i < propertyNames.size(); i++) {
                String propertyName = propertyNames.get(i);
                propertyDataTypes[i] = getDataType(descriptor.getPropertyTypes().get(propertyName));
            }

            return new DataStructure((byte) 4, propertyDataTypes);
        });
    }

    private static DataType getDataType(Class<?> cls) {
        if (cls == boolean.class || cls == byte.class) {
            return DataPrimitives.BYTE;
        } else if (cls == short.class || cls == char.class) {
            return DataPrimitives.SHORT;
        } else if (cls == int.class) {
            return DataPrimitives.INT;
        } else if (cls == float.class) {
            return DataPrimitives.FLOAT;
        } else if (cls == long.class) {
            return DataPrimitives.LONG;
        } else if (cls == double.class) {
            return DataPrimitives.DOUBLE;
        } else {
            return DataPrimitives.ADDRESS;
        }
    }
}
