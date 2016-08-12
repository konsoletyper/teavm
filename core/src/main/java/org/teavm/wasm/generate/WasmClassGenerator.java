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
import java.util.Collections;
import java.util.Comparator;
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
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.classes.TagRegistry;
import org.teavm.model.classes.VirtualTable;
import org.teavm.model.classes.VirtualTableEntry;
import org.teavm.model.classes.VirtualTableProvider;
import org.teavm.runtime.RuntimeClass;
import org.teavm.wasm.model.expression.WasmExpression;
import org.teavm.wasm.model.expression.WasmInt32Constant;
import org.teavm.wasm.model.expression.WasmInt32Subtype;
import org.teavm.wasm.model.expression.WasmStoreInt32;

public class WasmClassGenerator {
    private ClassReaderSource classSource;
    private int address;
    private Map<String, ClassBinaryData> binaryDataMap = new LinkedHashMap<>();
    private ClassBinaryData arrayClassData;
    private List<WasmExpression> initializer;
    private Map<MethodReference, Integer> functions = new HashMap<>();
    private List<String> functionTable = new ArrayList<>();
    private VirtualTableProvider vtableProvider;
    private TagRegistry tagRegistry;

    public WasmClassGenerator(ClassReaderSource classSource, VirtualTableProvider vtableProvider,
            TagRegistry tagRegistry, List<WasmExpression> initializer) {
        this.classSource = classSource;
        this.vtableProvider = vtableProvider;
        this.tagRegistry = tagRegistry;
        this.initializer = initializer;
    }

    public void addClass(String className) {
        if (binaryDataMap.containsKey(className)) {
            return;
        }

        ClassReader cls = classSource.get(className);
        ClassBinaryData binaryData = new ClassBinaryData();
        binaryData.name = className;
        binaryDataMap.put(className, binaryData);

        calculateLayout(cls, binaryData);
        if (binaryData.start < 0) {
            return;
        }

        address = align(address, 8);
        binaryData.start = address;
        contributeToInitializer(binaryData);
    }

    public void addArrayClass() {
        if (arrayClassData != null) {
            return;
        }

        arrayClassData = new ClassBinaryData();
        arrayClassData.start = address;

        address += RuntimeClass.VIRTUAL_TABLE_OFFSET;
    }

    public int getAddress() {
        return address;
    }

    public void setAddress(int address) {
        this.address = address;
    }

    public List<String> getFunctionTable() {
        return functionTable;
    }

    private void contributeToInitializer(ClassBinaryData binaryData) {
        contributeInt32Value(binaryData.start + RuntimeClass.SIZE_OFFSET, binaryData.size);

        List<TagRegistry.Range> ranges = tagRegistry.getRanges(binaryData.name);
        int lower = ranges.stream().mapToInt(range -> range.lower).min().orElse(0);
        int upper = ranges.stream().mapToInt(range -> range.upper).max().orElse(0);
        contributeInt32Value(binaryData.start + RuntimeClass.LOWER_TAG_OFFSET, lower);
        contributeInt32Value(binaryData.start + RuntimeClass.UPPER_TAG_OFFSET, upper);
        contributeInt32Value(binaryData.start + RuntimeClass.CANARY_OFFSET,
                RuntimeClass.computeCanary(binaryData.size, lower, upper));

        address = binaryData.start + RuntimeClass.VIRTUAL_TABLE_OFFSET;
        VirtualTable vtable = vtableProvider.lookup(binaryData.name);
        if (vtable != null) {
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

                contributeInt32Value(address, methodIndex);
                address += 4;
            }
        }

        contributeInt32Value(binaryData.start + RuntimeClass.EXCLUDED_RANGE_COUNT_OFFSET, ranges.size() - 1);

        if (ranges.size() > 1) {
            contributeInt32Value(binaryData.start + RuntimeClass.EXCLUDED_RANGE_ADDRESS_OFFSET, address);

            Collections.sort(ranges, Comparator.comparingInt(range -> range.lower));
            for (int i = 1; i < ranges.size(); ++i) {
                contributeInt32Value(address, ranges.get(i - 1).upper);
                contributeInt32Value(address + 4, ranges.get(i).lower);
                address += 8;
            }
        }
    }

    private void contributeInt32Value(int index, int value) {
        initializer.add(new WasmStoreInt32(4, new WasmInt32Constant(index), new WasmInt32Constant(value),
                WasmInt32Subtype.INT32));
    }

    public int getClassPointer(String className) {
        ClassBinaryData data = binaryDataMap.get(className);
        return data.start;
    }

    public int getFieldOffset(FieldReference field) {
        ClassBinaryData data = binaryDataMap.get(field.getClassName());
        return data.fieldLayout.get(field.getFieldName());
    }

    public int getClassSize(String className) {
        ClassBinaryData data = binaryDataMap.get(className);
        return data.size;
    }

    public int getArrayClassPointer() {
        return arrayClassData.start;
    }

    public boolean isStructure(String className) {
        ClassBinaryData data = binaryDataMap.get(className);
        return data.start < 0;
    }

    private void calculateLayout(ClassReader cls, ClassBinaryData data) {
        if (cls.getName().equals(Structure.class.getName()) || cls.getName().equals(Address.class.getName())) {
            data.size = 0;
            data.start = -1;
        } else if (cls.getParent() != null && !cls.getParent().equals(cls.getName())) {
            addClass(cls.getParent());
            ClassBinaryData parentData = binaryDataMap.get(cls.getParent());
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
                int offset = align(address, desiredAlignment);
                data.fieldLayout.put(field.getName(), offset);
                address = offset + desiredAlignment;
            } else {
                int offset = align(data.size, desiredAlignment);
                data.fieldLayout.put(field.getName(), offset);
                data.size = offset + desiredAlignment;
            }
        }
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

    private class ClassBinaryData {
        String name;
        int size;
        int start;
        ObjectIntMap<String> fieldLayout = new ObjectIntOpenHashMap<>();
    }
}
