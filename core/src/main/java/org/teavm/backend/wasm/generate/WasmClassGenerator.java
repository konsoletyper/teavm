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
package org.teavm.backend.wasm.generate;

import com.carrotsearch.hppc.ObjectIntMap;
import com.carrotsearch.hppc.ObjectIntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.teavm.backend.wasm.binary.BinaryWriter;
import org.teavm.backend.wasm.binary.DataArray;
import org.teavm.backend.wasm.binary.DataPrimitives;
import org.teavm.backend.wasm.binary.DataStructure;
import org.teavm.backend.wasm.binary.DataType;
import org.teavm.backend.wasm.binary.DataValue;
import org.teavm.common.IntegerArray;
import org.teavm.interop.Address;
import org.teavm.interop.Function;
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
import org.teavm.runtime.RuntimeObject;

public class WasmClassGenerator {
    private ClassReaderSource classSource;
    private Map<ValueType, ClassBinaryData> binaryDataMap = new LinkedHashMap<>();
    private BinaryWriter binaryWriter;
    private Map<MethodReference, Integer> functions = new HashMap<>();
    private List<String> functionTable = new ArrayList<>();
    private VirtualTableProvider vtableProvider;
    private TagRegistry tagRegistry;
    private DataStructure objectStructure = new DataStructure((byte) 0,
            DataPrimitives.INT, /* class */
            DataPrimitives.ADDRESS /* monitor/hash code */);
    private DataStructure classStructure = new DataStructure(
            (byte) 8,
            objectStructure,
            DataPrimitives.INT, /* size */
            DataPrimitives.INT, /* flags */
            DataPrimitives.INT, /* tag */
            DataPrimitives.INT, /* canary */
            DataPrimitives.ADDRESS, /* item type */
            DataPrimitives.ADDRESS, /* array type */
            DataPrimitives.INT, /* isInstance function */
            DataPrimitives.ADDRESS, /* parent */
            DataPrimitives.ADDRESS  /* layout */);
    private IntegerArray staticGcRoots = new IntegerArray(1);
    private int staticGcRootsAddress;

    private static final int CLASS_SIZE = 1;
    private static final int CLASS_FLAGS = 2;
    private static final int CLASS_TAG = 3;
    private static final int CLASS_CANARY = 4;
    private static final int CLASS_ITEM_TYPE = 5;
    private static final int CLASS_ARRAY_TYPE = 6;
    private static final int CLASS_IS_INSTANCE = 7;
    private static final int CLASS_PARENT = 8;
    private static final int CLASS_LAYOUT = 9;

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
                    size = 8;
                    break;
            }

            binaryData.data = createPrimitiveClassData(size, type);
            binaryData.start = binaryWriter.append(binaryData.data);
        } else if (type == ValueType.VOID) {
            binaryData.data = createPrimitiveClassData(0, type);
            binaryData.start = binaryWriter.append(binaryData.data);
        } else if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            ClassReader cls = classSource.get(className);

            if (cls != null) {
                calculateLayout(cls, binaryData);
                if (binaryData.start >= 0) {
                    binaryData.start = binaryWriter.append(createStructure(binaryData));
                }
            }
        } else if (type instanceof ValueType.Array) {
            ValueType itemType = ((ValueType.Array) type).getItemType();
            addClass(itemType);
            ClassBinaryData itemBinaryData = binaryDataMap.get(itemType);

            VirtualTable vtable = vtableProvider.lookup("java.lang.Object");
            int vtableSize = vtable != null ? vtable.getEntries().size() : 0;
            DataType arrayType = new DataArray(DataPrimitives.INT, vtableSize);
            DataValue wrapper = new DataStructure((byte) 0, classStructure, arrayType).createValue();

            if (vtableSize > 0) {
                fillVirtualTable(vtable, wrapper.getValue(1));
            }

            binaryData.size = 4;
            binaryData.data = wrapper.getValue(0);
            binaryData.data.setInt(CLASS_SIZE, 4);
            binaryData.data.setAddress(CLASS_ITEM_TYPE, itemBinaryData.start);
            binaryData.data.setInt(CLASS_IS_INSTANCE, functionTable.size());
            binaryData.data.setInt(CLASS_CANARY, RuntimeClass.computeCanary(4, 0));
            functionTable.add(WasmMangling.mangleIsSupertype(type));
            binaryData.start = binaryWriter.append(vtableSize > 0 ? wrapper : binaryData.data);

            itemBinaryData.data.setAddress(CLASS_ARRAY_TYPE, binaryData.start);
        }
    }

    private DataValue createPrimitiveClassData(int size, ValueType type) {
        DataValue value = classStructure.createValue();
        value.setInt(CLASS_SIZE, size);
        value.setInt(CLASS_FLAGS, RuntimeClass.PRIMITIVE);
        value.setInt(CLASS_IS_INSTANCE, functionTable.size());
        functionTable.add(WasmMangling.mangleIsSupertype(type));
        return value;
    }

    public List<String> getFunctionTable() {
        return functionTable;
    }

    private DataValue createStructure(ClassBinaryData binaryData) {
        String parent = binaryData.cls.getParent();
        int parentPtr = !binaryData.isInferface && parent != null
                ? getClassPointer(ValueType.object(binaryData.cls.getParent()))
                : 0;

        String name = ((ValueType.Object) binaryData.type).getClassName();

        VirtualTable vtable = vtableProvider.lookup(name);
        int vtableSize = vtable != null ? vtable.getEntries().size() : 0;

        DataType arrayType = new DataArray(DataPrimitives.INT, vtableSize);
        DataValue wrapper = new DataStructure((byte) 0, classStructure, arrayType).createValue();
        DataValue array = wrapper.getValue(1);
        DataValue header = wrapper.getValue(0);
        binaryData.data = header;

        int occupiedSize = binaryData.size;
        if ((occupiedSize & 3) != 0) {
            occupiedSize = occupiedSize >> 2 << 2 + 1;
        }
        header.setInt(CLASS_SIZE, occupiedSize);
        List<TagRegistry.Range> ranges = tagRegistry.getRanges(name);
        int tag = ranges.stream().mapToInt(range -> range.lower).min().orElse(0);
        header.setInt(CLASS_TAG, tag);
        header.setInt(CLASS_CANARY, RuntimeClass.computeCanary(occupiedSize, tag));
        header.setInt(CLASS_IS_INSTANCE, functionTable.size());
        functionTable.add(WasmMangling.mangleIsSupertype(ValueType.object(name)));
        header.setAddress(CLASS_PARENT, parentPtr);

        if (vtable != null) {
            fillVirtualTable(vtable, array);
        }

        List<FieldReference> fields = getReferenceFields(binaryData.cls);
        if (!fields.isEmpty()) {
            DataValue layoutSize = DataPrimitives.SHORT.createValue();
            layoutSize.setShort(0, (short) fields.size());
            header.setAddress(CLASS_LAYOUT, binaryWriter.append(layoutSize));
            for (FieldReference field : fields) {
                DataValue layoutElement = DataPrimitives.SHORT.createValue();
                int offset = binaryData.fieldLayout.get(field.getFieldName());
                layoutElement.setShort(0, (short) offset);
                binaryWriter.append(layoutElement);
            }
        }

        for (FieldReference field : getStaticReferenceFields(binaryData.cls)) {
            staticGcRoots.add(binaryData.fieldLayout.get(field.getFieldName()));
        }

        return vtable != null ? wrapper : header;
    }

    private List<FieldReference> getReferenceFields(ClassReader cls) {
        return cls.getFields().stream()
                .filter(field -> !field.hasModifier(ElementModifier.STATIC))
                .filter(field -> isReferenceType(field.getType()))
                .filter(field -> !field.getOwnerName().equals("java.lang.Object")
                        && !field.getName().equals("monitor"))
                .map(field -> field.getReference())
                .collect(Collectors.toList());
    }

    private List<FieldReference> getStaticReferenceFields(ClassReader cls) {
        return cls.getFields().stream()
                .filter(field -> field.hasModifier(ElementModifier.STATIC))
                .filter(field -> isReferenceType(field.getType()))
                .map(field -> field.getReference())
                .collect(Collectors.toList());
    }

    private boolean isReferenceType(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            return false;
        } else if (type instanceof ValueType.Object) {
            ClassReader cls = classSource.get(((ValueType.Object) type).getClassName());
            if (cls == null) {
                return true;
            }
            if (cls.getName().equals(Address.class.getName())) {
                return false;
            }
            while (cls != null) {
                if (cls.getName().equals(Structure.class.getName()) || cls.getName().equals(Function.class.getName())) {
                    return false;
                }
                if (cls.getParent() == null || cls.getParent().equals(cls.getName())) {
                    return true;
                }
                cls = classSource.get(cls.getParent());
            }
            return true;
        } else {
            return true;
        }
    }

    private void fillVirtualTable(VirtualTable vtable, DataValue array) {
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
    }

    public Collection<ValueType> getRegisteredClasses() {
        return binaryDataMap.keySet();
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

    public int getClassAlignment(String className) {
        ValueType type = ValueType.object(className);
        addClass(type);
        ClassBinaryData data = binaryDataMap.get(type);
        return data.alignment;
    }

    public boolean isStructure(String className) {
        ValueType type = ValueType.object(className);
        addClass(type);
        ClassBinaryData data = binaryDataMap.get(type);
        return data.start < 0;
    }

    public boolean isFunctionClass(String className) {
        ValueType type = ValueType.object(className);
        addClass(type);
        return binaryDataMap.get(type).function;
    }

    private void calculateLayout(ClassReader cls, ClassBinaryData data) {
        if (cls.getName().equals(Structure.class.getName()) || cls.getName().equals(Address.class.getName())) {
            data.size = 0;
            data.start = -1;
            return;
        } else if (cls.getName().equals(Function.class.getName())) {
            data.size = 0;
            data.start = -1;
            data.function = true;
            return;
        } else if (cls.getParent() != null && !cls.getParent().equals(cls.getName())) {
            addClass(ValueType.object(cls.getParent()));
            ClassBinaryData parentData = binaryDataMap.get(ValueType.object(cls.getParent()));
            data.size = parentData.size;
            data.alignment = parentData.alignment;
            if (parentData.start == -1) {
                data.start = -1;
            }
            if (parentData.function) {
                data.function = true;
                return;
            }
        } else {
            data.size = 4;
            data.alignment = 4;
        }

        data.isInferface = cls.hasModifier(ElementModifier.INTERFACE);
        data.cls = cls;

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
            if (data.alignment == 0) {
                data.alignment = desiredAlignment;
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

    public static int align(int base, int alignment) {
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

    public void postProcess() {
        ClassBinaryData classClassData = binaryDataMap.get(ValueType.object("java.lang.Class"));
        if (classClassData != null) {
            int tag = (classClassData.start >> 3) | RuntimeObject.GC_MARKED;
            for (ClassBinaryData classData : binaryDataMap.values()) {
                if (classData.data != null) {
                    classData.data.getValue(0).setInt(0, tag);
                }
            }
        }
        writeStaticGcRoots();
    }

    public int getStaticGcRootsAddress() {
        return staticGcRootsAddress;
    }

    private void writeStaticGcRoots() {
        DataValue sizeValue = DataPrimitives.LONG.createValue();
        sizeValue.setLong(0, staticGcRoots.size());
        staticGcRootsAddress = binaryWriter.append(sizeValue);
        for (int gcRoot : staticGcRoots.getAll()) {
            DataValue value = DataPrimitives.ADDRESS.createValue();
            value.setAddress(0, gcRoot);
            binaryWriter.append(value);
        }
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
        int alignment;
        int start;
        boolean isInferface;
        ObjectIntMap<String> fieldLayout = new ObjectIntOpenHashMap<>();
        DataValue data;
        ClassReader cls;
        boolean function;
    }
}
