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

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.teavm.backend.lowlevel.generate.NameProvider;
import org.teavm.backend.wasm.binary.BinaryWriter;
import org.teavm.backend.wasm.binary.DataArray;
import org.teavm.backend.wasm.binary.DataPrimitives;
import org.teavm.backend.wasm.binary.DataStructure;
import org.teavm.backend.wasm.binary.DataType;
import org.teavm.backend.wasm.binary.DataValue;
import org.teavm.backend.wasm.debug.DebugClassLayout;
import org.teavm.backend.wasm.debug.info.FieldType;
import org.teavm.backend.wasm.render.WasmBinaryStatsCollector;
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
import org.teavm.model.analysis.ClassInitializerInfo;
import org.teavm.model.analysis.ClassMetadataRequirements;
import org.teavm.model.classes.TagRegistry;
import org.teavm.model.classes.VirtualTable;
import org.teavm.model.classes.VirtualTableEntry;
import org.teavm.model.classes.VirtualTableProvider;
import org.teavm.model.lowlevel.Characteristics;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeObject;

public class WasmClassGenerator {
    private ClassReaderSource processedClassSource;
    private ClassReaderSource classSource;
    private Characteristics characteristics;
    public final NameProvider names;
    private Map<ValueType, ClassBinaryData> binaryDataMap = new LinkedHashMap<>();
    private BinaryWriter binaryWriter;
    private Map<MethodReference, Integer> functions = new HashMap<>();
    private List<String> functionTable = new ArrayList<>();
    private ObjectIntMap<String> functionIdMap = new ObjectIntHashMap<>();
    private VirtualTableProvider vtableProvider;
    private TagRegistry tagRegistry;
    private WasmStringPool stringPool;
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
            DataPrimitives.ADDRESS, /* name */
            DataPrimitives.ADDRESS, /* name cache */
            DataPrimitives.ADDRESS, /* item type */
            DataPrimitives.ADDRESS, /* array type */
            DataPrimitives.ADDRESS, /* declaring class */
            DataPrimitives.ADDRESS, /* enclosing class */
            DataPrimitives.INT, /* isInstance function */
            DataPrimitives.INT, /* init function */
            DataPrimitives.ADDRESS, /* parent */
            DataPrimitives.INT, /* interface count */
            DataPrimitives.ADDRESS, /* interfaces */
            DataPrimitives.ADDRESS, /* enum values */
            DataPrimitives.ADDRESS, /* layout */
            DataPrimitives.ADDRESS,  /* simple name */
            DataPrimitives.ADDRESS,  /* simple name cache */
            DataPrimitives.ADDRESS   /* canonical name cache */);
    private IntegerArray staticGcRoots = new IntegerArray(1);
    private int staticGcRootsAddress;
    private int classesAddress;
    private int classCount;
    private ClassMetadataRequirements metadataRequirements;
    private ClassInitializerInfo classInitializerInfo;
    private DwarfClassGenerator dwarfClassGenerator;
    private WasmBinaryStatsCollector statsCollector;

    private static final int CLASS_SIZE = 1;
    private static final int CLASS_FLAGS = 2;
    private static final int CLASS_TAG = 3;
    private static final int CLASS_CANARY = 4;
    private static final int CLASS_NAME = 5;
    private static final int CLASS_ITEM_TYPE = 7;
    private static final int CLASS_ARRAY_TYPE = 8;
    private static final int CLASS_DECLARING_CLASS = 9;
    private static final int CLASS_ENCLOSING_CLASS = 10;
    private static final int CLASS_IS_INSTANCE = 11;
    private static final int CLASS_INIT = 12;
    private static final int CLASS_PARENT = 13;
    private static final int CLASS_ENUM_VALUES = 16;
    private static final int CLASS_LAYOUT = 17;
    private static final int CLASS_SIMPLE_NAME = 18;

    public WasmClassGenerator(ClassReaderSource processedClassSource, ClassReaderSource classSource,
            VirtualTableProvider vtableProvider, TagRegistry tagRegistry, BinaryWriter binaryWriter,
            NameProvider names, ClassMetadataRequirements metadataRequirements,
            ClassInitializerInfo classInitializerInfo, Characteristics characteristics,
            DwarfClassGenerator dwarfClassGenerator, WasmBinaryStatsCollector statsCollector) {
        this.processedClassSource = processedClassSource;
        this.classSource = classSource;
        this.vtableProvider = vtableProvider;
        this.tagRegistry = tagRegistry;
        this.binaryWriter = binaryWriter;
        this.stringPool = new WasmStringPool(this, binaryWriter, statsCollector);
        this.names = names;
        this.metadataRequirements = metadataRequirements;
        this.classInitializerInfo = classInitializerInfo;
        this.characteristics = characteristics;
        this.dwarfClassGenerator = dwarfClassGenerator;
        this.statsCollector = statsCollector;
    }

    public WasmStringPool getStringPool() {
        return stringPool;
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

            binaryData.data = classStructure.createValue();
            createPrimitiveClassData(binaryData.data, size, type);
            binaryData.start = binaryWriter.append(binaryData.data);
        } else if (type == ValueType.VOID) {
            binaryData.data = classStructure.createValue();
            createPrimitiveClassData(binaryData.data, 0, type);
            binaryData.start = binaryWriter.append(binaryData.data);
        } else if (type instanceof ValueType.Object) {
            String className = ((ValueType.Object) type).getClassName();
            var cls = classSource.get(className);

            if (cls != null) {
                DwarfClassGenerator.ClassType dwarfClass;
                if (dwarfClassGenerator != null) {
                    dwarfClass = dwarfClassGenerator.getClass(className);
                    dwarfClass.setSuperclass(cls.getParent() != null
                            ? dwarfClassGenerator.getClass(cls.getParent())
                            : null);
                } else {
                    dwarfClass = null;
                }
                calculateLayout(cls, binaryData, dwarfClass);
                if (binaryData.start >= 0) {
                    binaryData.start = binaryWriter.append(createStructure(binaryData));
                    var size = binaryWriter.getAddress() - binaryData.start;
                    statsCollector.addClassMetadataSize(className, size);
                }
                if (dwarfClass != null) {
                    dwarfClass.setSize(binaryData.size);
                    dwarfClass.setPointer(binaryData.start);
                }
            }
        } else if (type instanceof ValueType.Array) {
            ValueType itemType = ((ValueType.Array) type).getItemType();
            addClass(itemType);
            ClassBinaryData itemBinaryData = binaryDataMap.get(itemType);

            VirtualTable vtable = vtableProvider.lookup("java.lang.Object");
            int vtableSize = vtable != null ? vtable.size() : 0;
            DataType arrayType = new DataArray(DataPrimitives.INT, vtableSize);
            DataValue wrapper = new DataStructure((byte) 0, classStructure, arrayType).createValue();

            if (vtableSize > 0) {
                fillVirtualTable(vtable, wrapper.getValue(1));
            }

            binaryData.size = 4;
            binaryData.data = wrapper.getValue(0);
            binaryData.data.setInt(CLASS_SIZE, 4);
            binaryData.data.setAddress(CLASS_ITEM_TYPE, itemBinaryData.start);
            binaryData.data.setInt(CLASS_IS_INSTANCE, getFunctionPointer(names.forSupertypeFunction(type)));
            binaryData.data.setInt(CLASS_CANARY, RuntimeClass.computeCanary(4, 0));
            binaryData.data.setAddress(CLASS_NAME, stringPool.getStringPointer(type.toString().replace('/', '.')));
            binaryData.data.setAddress(CLASS_SIMPLE_NAME, 0);
            binaryData.data.setInt(CLASS_INIT, -1);
            binaryData.data.setAddress(CLASS_PARENT, getClassPointer(ValueType.object("java.lang.Object")));
            binaryData.start = binaryWriter.append(vtableSize > 0 ? wrapper : binaryData.data);

            itemBinaryData.data.setAddress(CLASS_ARRAY_TYPE, binaryData.start);
        }
    }

    private DataValue createPrimitiveClassData(DataValue value, int size, ValueType type) {
        value.setInt(CLASS_SIZE, size);
        value.setInt(CLASS_FLAGS, RuntimeClass.PRIMITIVE);
        value.setInt(CLASS_IS_INSTANCE, getFunctionPointer(names.forSupertypeFunction(type)));
        value.setAddress(CLASS_SIMPLE_NAME, 0);
        value.setInt(CLASS_INIT, -1);
        value.setInt(CLASS_TAG, Integer.MAX_VALUE);

        String name;
        if (type == ValueType.VOID) {
            name = "void";
        } else {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    name = "boolean";
                    break;
                case BYTE:
                    name = "byte";
                    break;
                case SHORT:
                    name = "short";
                    break;
                case CHARACTER:
                    name = "char";
                    break;
                case INTEGER:
                    name = "int";
                    break;
                case LONG:
                    name = "long";
                    break;
                case FLOAT:
                    name = "float";
                    break;
                case DOUBLE:
                    name = "double";
                    break;
                default:
                    name = "";
            }
        }

        value.setAddress(CLASS_NAME, stringPool.getStringPointer(name));

        return value;
    }

    public Iterable<? extends String> getFunctionTable() {
        return functionTable;
    }

    public int getFunctionPointer(String name) {
        var result = functionIdMap.getOrDefault(name, -1);
        if (result < 0) {
            result = functionTable.size();
            functionTable.add(name);
            functionIdMap.put(name, result);
        }
        return result;
    }

    private DataValue createStructure(ClassBinaryData binaryData) {
        String parent = binaryData.cls.getParent();
        int parentPtr = !binaryData.isInferface && parent != null
                ? getClassPointer(ValueType.object(binaryData.cls.getParent()))
                : 0;

        String name = ((ValueType.Object) binaryData.type).getClassName();
        ClassMetadataRequirements.Info requirements = metadataRequirements.getInfo(name);
        int flags = 0;

        VirtualTable vtable = vtableProvider.lookup(name);
        int vtableSize = vtable != null ? vtable.size() : 0;

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
        int nameAddress = requirements.name() ? stringPool.getStringPointer(name) : 0;
        header.setAddress(CLASS_NAME, nameAddress);
        header.setInt(CLASS_IS_INSTANCE, getFunctionPointer(names.forSupertypeFunction(ValueType.object(name))));
        header.setAddress(CLASS_PARENT, parentPtr);

        ClassReader cls = processedClassSource.get(name);

        if (cls != null) {
            if (cls.getSimpleName() != null && requirements.simpleName()) {
                header.setAddress(CLASS_SIMPLE_NAME, stringPool.getStringPointer(cls.getSimpleName()));
            }

            if (cls.getOwnerName() != null && processedClassSource.get(cls.getOwnerName()) != null
                    && requirements.enclosingClass()) {
                header.setAddress(CLASS_ENCLOSING_CLASS, getClassPointer(ValueType.object(cls.getOwnerName())));
            }
            if (cls.getDeclaringClassName() != null && processedClassSource.get(cls.getDeclaringClassName()) != null
                    && requirements.declaringClass()) {
                header.setAddress(CLASS_DECLARING_CLASS,
                        getClassPointer(ValueType.object(cls.getDeclaringClassName())));
            }
        }

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

        if (cls != null) {
            if (cls.hasModifier(ElementModifier.ENUM)) {
                header.setAddress(CLASS_ENUM_VALUES, generateEnumValues(cls, binaryData));
                flags |= RuntimeClass.ENUM;
            }
            if (cls.hasModifier(ElementModifier.SYNTHETIC)) {
                flags |= RuntimeClass.SYNTHETIC;
            }
        }

        if (cls != null && binaryData.start >= 0
                && cls.getMethod(new MethodDescriptor("<clinit>", ValueType.VOID)) != null
                && classInitializerInfo.isDynamicInitializer(name)) {
            header.setInt(CLASS_INIT, getFunctionPointer(names.forClassInitializer(name)));
        } else {
            header.setInt(CLASS_INIT, -1);
        }

        header.setInt(CLASS_FLAGS, flags);

        return vtable != null ? wrapper : header;
    }

    private int generateEnumValues(ClassReader cls, ClassBinaryData binaryData) {
        FieldReader[] fields = cls.getFields().stream()
                .filter(field -> field.hasModifier(ElementModifier.ENUM))
                .toArray(FieldReader[]::new);
        DataValue sizeValue = DataPrimitives.ADDRESS.createValue();
        sizeValue.setAddress(0, fields.length);
        int valuesAddress = binaryWriter.append(sizeValue);

        for (FieldReader field : fields) {
            DataValue fieldRefValue = DataPrimitives.ADDRESS.createValue();
            fieldRefValue.setAddress(0, binaryData.fieldLayout.get(field.getName()));
            binaryWriter.append(fieldRefValue);
        }

        return valuesAddress;
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
            var className = ((ValueType.Object) type).getClassName();
            return isManagedClass(className);
        } else {
            return true;
        }
    }

    private boolean isManagedClass(String className) {
        return !characteristics.isStructure(className)
                && !characteristics.isFunction(className)
                && !characteristics.isResource(className)
                && !className.equals(Address.class.getName());
    }

    private void fillVirtualTable(VirtualTable vtable, DataValue array) {
        int index = 0;
        List<VirtualTable> tables = new ArrayList<>();
        VirtualTable vt = vtable;
        while (vt != null) {
            tables.add(vt);
            vt = vt.getParent();
        }
        for (int i = tables.size() - 1; i >= 0; --i) {
            for (MethodDescriptor method : tables.get(i).getMethods()) {
                int methodIndex = -1;
                if (method != null) {
                    VirtualTableEntry entry = vtable.getEntry(method);
                    if (entry != null) {
                        methodIndex = functions.computeIfAbsent(entry.getImplementor(),
                                implementor -> getFunctionPointer(names.forMethod(implementor)));
                    }
                }

                array.setInt(index++, methodIndex);
            }
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

    private void calculateLayout(ClassReader cls, ClassBinaryData data, DwarfClassGenerator.ClassType dwarfClass) {
        if (cls.getName().equals(Structure.class.getName()) || cls.getName().equals(Address.class.getName())) {
            data.size = 0;
            data.start = -1;
            return;
        } else if (cls.getName().equals(Function.class.getName())) {
            data.size = 0;
            data.start = -1;
            data.function = true;
            return;
        } else if (cls.getParent() != null) {
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
            int desiredAlignment = getTypeSize(field.getType());
            if (field.hasModifier(ElementModifier.STATIC)) {
                DataType type = asDataType(field.getType());
                DataValue value = type.createValue();
                if (field.getInitialValue() != null) {
                    setInitialValue(field.getType(), value, field.getInitialValue());
                }
                var address = binaryWriter.append(value);
                data.fieldLayout.put(field.getName(), address);
                if (dwarfClass != null) {
                    dwarfClass.registerStaticField(field.getName(), field.getType(), address);
                    dwarfClassGenerator.getTypePtr(field.getType());
                }
            } else {
                int offset = align(data.size, desiredAlignment);
                data.fieldLayout.put(field.getName(), offset);
                data.size = offset + desiredAlignment;
                if (dwarfClass != null) {
                    dwarfClass.registerField(field.getName(), field.getType(), offset);
                    dwarfClassGenerator.getTypePtr(field.getType());
                }
            }
            if (data.alignment == 0) {
                data.alignment = desiredAlignment;
            }
        }
    }

    private void setInitialValue(ValueType type, DataValue data, Object value) {
        if (value instanceof Number) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BYTE:
                    data.setByte(0, ((Number) value).byteValue());
                    break;
                case SHORT:
                    data.setShort(0, ((Number) value).shortValue());
                    break;
                case CHARACTER:
                    data.setShort(0, ((Number) value).shortValue());
                    break;
                case INTEGER:
                    data.setInt(0, ((Number) value).intValue());
                    break;
                case LONG:
                    data.setLong(0, ((Number) value).longValue());
                    break;
                case FLOAT:
                    data.setFloat(0, ((Number) value).floatValue());
                    break;
                case DOUBLE:
                    data.setDouble(0, ((Number) value).doubleValue());
                    break;
                case BOOLEAN:
                    data.setByte(0, ((Number) value).byteValue());
                    break;
            }
        } else if (value instanceof Boolean) {
            data.setByte(0, (Boolean) value ? (byte) 1 : 0);
        } else if (value instanceof String) {
            data.setAddress(0, stringPool.getStringPointer((String) value));
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

    public static int getTypeSize(ValueType type) {
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
        writeClasses();
    }

    public int getStaticGcRootsAddress() {
        return staticGcRootsAddress;
    }

    public int getClassesAddress() {
        return classesAddress;
    }

    public int getClassCount() {
        return classCount;
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

    private void writeClasses() {
        for (ClassBinaryData cls : binaryDataMap.values()) {
            if (cls.start < 0) {
                continue;
            }
            DataValue value = DataPrimitives.ADDRESS.createValue();
            value.setAddress(0, cls.start);
            int address = binaryWriter.append(value);
            if (classesAddress == 0) {
                classesAddress = address;
            }
            ++classCount;
        }
    }

    public boolean hasClinit(String className) {
        if (isStructure(className) || className.equals(Address.class.getName())) {
            return false;
        }
        ClassReader cls = classSource.get(className);
        if (cls == null) {
            return false;
        }
        return cls.getMethod(new MethodDescriptor("<clinit>", ValueType.VOID)) != null;
    }

    public void writeDebug(DebugClassLayout debug) {
        var list = new ArrayList<>(binaryDataMap.values());
        var indexes = new ObjectIntHashMap<ValueType>();
        for (var i = 0; i < list.size(); ++i) {
            indexes.put(list.get(i).type, i);
        }
        for (var i = 0; i < list.size(); ++i) {
            var data = list.get(i);
            if (data.type instanceof ValueType.Primitive) {
                debug.writePrimitive(((ValueType.Primitive) data.type).getKind(), data.start);
            } else if (data.type instanceof ValueType.Array) {
                var itemType = ((ValueType.Array) data.type).getItemType();
                debug.writeArray(indexes.get(itemType), data.start);
            } else if (data.type instanceof ValueType.Object) {
                var className = ((ValueType.Object) data.type).getClassName();
                if (className.equals("java.lang.Class")) {
                    debug.startClass(className, indexes.get(ValueType.object("java.lang.Object")), data.start, 60);
                    debug.instanceField("size", 8, FieldType.INT);
                    debug.instanceField("flags", 12, FieldType.INT);
                    debug.instanceField("name", 24, FieldType.OBJECT);
                    debug.instanceField("itemType", 32, FieldType.OBJECT);
                    debug.instanceField("parent", 56, FieldType.OBJECT);
                    debug.endClass();
                } else if (isManagedClass(className)) {
                    var parent = data.cls.getParent() != null
                            ? indexes.get(ValueType.object(data.cls.getParent()))
                            : -1;
                    if (data.isInferface) {
                        debug.writeInterface(className, data.start);
                    } else {
                        debug.startClass(className, parent, data.start, data.size);
                        var fields = getFieldsWithOffset(data);
                        for (var entry : fields) {
                            if (entry.field.hasModifier(ElementModifier.STATIC)) {
                                debug.staticField(entry.field.getName(), entry.offset,
                                        asDebugType(entry.field.getType()));
                            }
                        }
                        for (var entry : fields) {
                            if (!entry.field.hasModifier(ElementModifier.STATIC)) {
                                debug.instanceField(entry.field.getName(), entry.offset,
                                        asDebugType(entry.field.getType()));
                            }
                        }
                        debug.endClass();
                    }
                } else {
                    debug.writeUnknown(data.start);
                }
            } else {
                debug.writeUnknown(data.start);
            }
        }
    }

    private List<FieldWithOffset> getFieldsWithOffset(ClassBinaryData data) {
        var result = new ArrayList<FieldWithOffset>();
        for (var field : data.fieldLayout) {
            var fieldReader = data.cls.getField(field.key);
            result.add(new FieldWithOffset(fieldReader, field.value));
        }
        return result;
    }

    private static class FieldWithOffset {
        private FieldReader field;
        private int offset;

        FieldWithOffset(FieldReader field, int offset) {
            this.field = field;
            this.offset = offset;
        }
    }

    private FieldType asDebugType(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return FieldType.BOOLEAN;
                case BYTE:
                    return FieldType.BYTE;
                case SHORT:
                    return FieldType.SHORT;
                case CHARACTER:
                    return FieldType.CHAR;
                case INTEGER:
                    return FieldType.INT;
                case LONG:
                    return FieldType.LONG;
                case FLOAT:
                    return FieldType.FLOAT;
                case DOUBLE:
                    return FieldType.DOUBLE;
                default:
                    return FieldType.UNDEFINED;
            }
        } else if (type instanceof ValueType.Object) {
            if (isManagedClass(((ValueType.Object) type).getClassName())) {
                return FieldType.OBJECT;
            } else {
                return FieldType.ADDRESS;
            }
        } else {
            return FieldType.OBJECT;
        }
    }

    static class ClassBinaryData {
        ValueType type;
        int size;
        int alignment;
        int start;
        boolean isInferface;
        ObjectIntMap<String> fieldLayout = new ObjectIntHashMap<>();
        DataValue data;
        ClassReader cls;
        boolean function;
    }
}
