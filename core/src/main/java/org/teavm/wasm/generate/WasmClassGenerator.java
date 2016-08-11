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
import org.teavm.model.classes.VirtualTable;
import org.teavm.model.classes.VirtualTableEntry;
import org.teavm.model.classes.VirtualTableProvider;
import org.teavm.wasm.model.WasmModule;
import org.teavm.wasm.model.expression.WasmExpression;
import org.teavm.wasm.model.expression.WasmInt32Constant;
import org.teavm.wasm.model.expression.WasmInt32Subtype;
import org.teavm.wasm.model.expression.WasmStoreInt32;

public class WasmClassGenerator {
    private ClassReaderSource classSource;
    private int address;
    private Map<String, ClassBinaryData> binaryDataMap = new LinkedHashMap<>();
    private VirtualTableProvider vtableProvider;

    public WasmClassGenerator(ClassReaderSource classSource, VirtualTableProvider vtableProvider, int address) {
        this.classSource = classSource;
        this.vtableProvider = vtableProvider;
        this.address = address;
    }

    public void addClass(String className) {
        if (binaryDataMap.containsKey(className)) {
            return;
        }

        ClassReader cls = classSource.get(className);
        ClassBinaryData binaryData = new ClassBinaryData();
        binaryDataMap.put(className, binaryData);

        calculateLayout(cls, binaryData);
        if (binaryData.start < 0) {
            return;
        }

        binaryData.start = align(address, 8);
        binaryData.vtable = vtableProvider.lookup(className);
        int vtableSize = binaryData.vtable != null ? binaryData.vtable.getEntries().size() : 0;
        binaryData.end = binaryData.start + 8 + vtableSize * 4;

        address = binaryData.end;
    }

    public int getAddress() {
        return address;
    }

    public void contributeToInitializer(List<WasmExpression> initializer, WasmModule module) {
        Map<MethodReference, Integer> functions = new HashMap<>();

        for (ClassBinaryData binaryData : binaryDataMap.values()) {
            if (binaryData.start < 0) {
                continue;
            }
            WasmExpression index = new WasmInt32Constant(binaryData.start);
            WasmExpression size = new WasmInt32Constant(binaryData.size);
            initializer.add(new WasmStoreInt32(4, index, size, WasmInt32Subtype.INT32));

            if (binaryData.vtable != null) {
                for (VirtualTableEntry vtableEntry : binaryData.vtable.getEntries().values()) {
                    index = new WasmInt32Constant(binaryData.start + 8 + vtableEntry.getIndex() * 4);
                    int methodIndex;
                    if (vtableEntry.getImplementor() == null) {
                        methodIndex = -1;
                    } else {
                        methodIndex = functions.computeIfAbsent(vtableEntry.getImplementor(), implementor -> {
                            int result = module.getFunctionTable().size();
                            String name = WasmMangling.mangleMethod(implementor);
                            module.getFunctionTable().add(module.getFunctions().get(name));
                            return result;
                        });
                    }

                    WasmExpression methodIndexExpr = new WasmInt32Constant(methodIndex);
                    initializer.add(new WasmStoreInt32(4, index, methodIndexExpr, WasmInt32Subtype.INT32));
                }
            }
        }
    }

    public int getClassPointer(String className) {
        ClassBinaryData data = binaryDataMap.get(className);
        return data.start;
    }

    public int getFieldOffset(FieldReference field) {
        ClassBinaryData data = binaryDataMap.get(field.getClassName());
        return data.fieldLayout.get(field.getFieldName());
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
        int start;
        int end;

        VirtualTable vtable;
        int size;
        ObjectIntMap<String> fieldLayout = new ObjectIntOpenHashMap<>();
    }
}
