/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.wasm.generate;

import com.carrotsearch.hppc.ObjectIntMap;
import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.teavm.interop.Address;
import org.teavm.interop.Structure;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.classes.TagRegistry;
import org.teavm.model.classes.VirtualTable;
import org.teavm.model.classes.VirtualTableEntry;
import org.teavm.model.classes.VirtualTableProvider;
import org.teavm.runtime.RuntimeClass;
import org.teavm.wasm.binary.BinaryWriter;
import org.teavm.wasm.binary.DataArray;
import org.teavm.wasm.binary.DataPrimitives;
import org.teavm.wasm.binary.DataStructure;
import org.teavm.wasm.binary.DataType;
import org.teavm.wasm.binary.DataValue;

public class WasmClassGenerator {
    private ClassReaderSource classSource;
    private Map<ValueType, ClassBinaryData> binaryDataMap = new LinkedHashMap<>();
    private BinaryWriter binaryWriter;
    private Map<MethodReference, Integer> functions = new HashMap<>();
    private List<String> functionTable = new ArrayList<>();
    private VirtualTableProvider vtableProvider;
    private TagRegistry tagRegistry;
    private DataStructure classStructure = new DataStructure(
            (byte) 8,
            DataPrimitives.INT, /* size */
            DataPrimitives.INT, /* flags */
            DataPrimitives.INT, /* tag */
            DataPrimitives.INT, /* canary */
            DataPrimitives.ADDRESS, /* item type */
            DataPrimitives.ADDRESS  /* array type */);

    public WasmClassGenerator(ClassReaderSource classSource, VirtualTableProvider vtableProvider,
            TagRegistry tagRegistry, BinaryWriter binaryWriter) {
        this.classSource = classSource;
        this.vtableProvider = vtableProvider;
        this.tagRegistry = tagRegistry;
        this.binaryWriter = binaryWriter;
    }

    private void addClass(ValueType type) {
        if (binaryDataMap.containsKey(type)) {
            return;
        }

        ClassBinaryData binaryData = new ClassBinaryData();
        binaryData.type = type;
        binaryDataMap.put(type, binaryData);

        if (type instanceof ValueType.Primitive) {
            int size = 0;
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                    size = 1;
                    break;
                case SHORT:
                case CHARACTER:
                    size = 2;
                    break;
                case INTEGER:
                case FLOAT:
                    size = 4;
                    break;
                case LONG:
                case DOUBLE:
                    size = 5;
                    break;
            }

            binaryData.data = createPrimitiveClassData(size);
            binaryData.start = binaryWriter.append(binaryData.data);
        } else if (type == ValueType.VOID) {
            binaryData.data = createPrimitiveClassData(0);
            binaryData.start = binaryWriter.append(binaryData.data);
        } else if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            ClassReader cls = classSource.get(className);

            calculateLayout(cls, binaryData);
            if (binaryData.start >= 0) {
                binaryData.start = binaryWriter.append(createStructure(binaryData));
            }
        } else if (type instanceof ValueType.Array) {
            ValueType itemType = ((ValueType.Array) type).getItemType();
            addClass(itemType);
            ClassBinaryData itemBinaryData = binaryDataMap.get(itemType);

            binaryData.size = 4;
            binaryData.data = classStructure.createValue();
            binaryData.data.setInt(0, 4);
            binaryData.data.setAddress(4, itemBinaryData.start);
            binaryData.start = binaryWriter.append(binaryData.data);

            itemBinaryData.data.setAddress(5, binaryData.start);
        }
    }

    private DataValue createPrimitiveClassData(int size) {
        DataValue value = classStructure.createValue();
        value.setInt(0, size);
        value.setInt(1, RuntimeClass.PRIMITIVE);
        return value;
    }

    public List<String> getFunctionTable() {
        return functionTable;
    }

    private DataValue createStructure(ClassBinaryData binaryData) {
        String name = ((ValueType.Object) binaryData.type).getClassName();

        VirtualTable vtable = vtableProvider.lookup(name);
        int vtableSize = vtable != null ? vtable.getEntries().size() : 0;

        DataType arrayType = new DataArray(DataPrimitives.INT, vtableSize);
        DataValue wrapper = new DataStructure((byte) 0, classStructure, arrayType).createValue();
        DataValue array = wrapper.getValue(1);
        DataValue header = wrapper.getValue(0);
        binaryData.data = header;

        header.setInt(0, binaryData.size);
        List<TagRegistry.Range> ranges = tagRegistry.getRanges(name);
        int tag = ranges.stream().mapToInt(range -> range.lower).min().orElse(0);
        header.setInt(2, tag);
        header.setInt(3, RuntimeClass.computeCanary(binaryData.size, tag));
        if (vtable == null) {
            return header;
        }

        int index = 0;
        for (VirtualTableEntry vtableEntry : vtable.getEntries().values()) {
            int methodIndex;
            if (vtableEntry.getImplementor() == null) {
                methodIndex = -1;
            } else {
                methodIndex = functions.computeIfAbsent(vtableEntry.getImplementor(), implementor -> {
                    int result = functionTable.size();
                    functionTable.add(WasmMangling.mangleMethod(implementor));
                    return result;
                });
            }

            array.setInt(index++, methodIndex);
        }

        return wrapper;
    }

    public int getClassPointer(ValueType type) {
        addClass(type);
        ClassBinaryData data = binaryDataMap.get(type);
        return data.start;
    }

    public int getFieldOffset(FieldReference field) {
        ValueType type = ValueType.object(field.getClassName());
        addClass(type);
        ClassBinaryData data = binaryDataMap.get(type);
        return data.fieldLayout.get(field.getFieldName());
    }

    public int getClassSize(String className) {
        ValueType type = ValueType.object(className);
        addClass(type);
        ClassBinaryData data = binaryDataMap.get(type);
        return data.size;
    }

    public boolean isStructure(String className) {
        ValueType type = ValueType.object(className);
        addClass(type);
        ClassBinaryData data = binaryDataMap.get(type);
        return data.start < 0;
    }

    private void calculateLayout(ClassReader cls, ClassBinaryData data) {
        if (cls.getName().equals(Structure.class.getName()) || cls.getName().equals(Address.class.getName())) {
            data.size = 0;
            data.start = -1;
        } else if (cls.getParent() != null && !cls.getParent().equals(cls.getName())) {
            addClass(ValueType.object(cls.getParent()));
            ClassBinaryData parentData = binaryDataMap.get(ValueType.object(cls.getParent()));
            data.size = parentData.size;
            if (parentData.start == -1) {
                data.start = -1;
            }
        } else {
            data.size = 4;
        }

        for (FieldReader field : cls.getFields()) {
            int desiredAlignment = getDesiredAlignment(field.getType());
            if (field.hasModifier(ElementModifier.STATIC)) {
                DataType type = asDataType(field.getType());
                data.fieldLayout.put(field.getName(), binaryWriter.append(type.createValue()));
            } else {
                int offset = align(data.size, desiredAlignment);
                data.fieldLayout.put(field.getName(), offset);
                data.size = offset + desiredAlignment;
            }
        }
    }

    private static DataType asDataType(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
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

    private static int align(int base, int alignment) {
        if (base == 0) {
            return 0;
        }
        return ((base - 1) / alignment + 1) * alignment;
    }

    private int getDesiredAlignment(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                    return 1;
                case SHORT:
                case CHARACTER:
                    return 2;
                case INTEGER:
                case FLOAT:
                    return 4;
                case LONG:
                case DOUBLE:
                    return 8;
            }
        }
        return 4;
    }

    public boolean hasClinit(String className) {
        if (isStructure(className)) {
            return false;
        }
        ClassReader cls = classSource.get(className);
        if (cls == null) {
            return false;
        }
        return cls.getMethod(new MethodDescriptor("<clinit>", ValueType.VOID)) != null;
    }

    private class ClassBinaryData {
        ValueType type;
        int size;
        int start;
        ObjectIntMap<String> fieldLayout = new ObjectIntOpenHashMap<>();
        DataValue data;
    }
}
