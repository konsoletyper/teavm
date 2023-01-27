/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.wasm.debug.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import org.teavm.backend.wasm.debug.DebugConstants;
import org.teavm.backend.wasm.debug.info.ArrayLayout;
import org.teavm.backend.wasm.debug.info.ClassInfo;
import org.teavm.backend.wasm.debug.info.ClassLayout;
import org.teavm.backend.wasm.debug.info.ClassLayoutInfo;
import org.teavm.backend.wasm.debug.info.FieldInfo;
import org.teavm.backend.wasm.debug.info.FieldType;
import org.teavm.backend.wasm.debug.info.InterfaceLayout;
import org.teavm.backend.wasm.debug.info.PrimitiveLayout;
import org.teavm.backend.wasm.debug.info.TypeLayout;
import org.teavm.backend.wasm.debug.info.UnknownLayout;
import org.teavm.model.PrimitiveType;

public class DebugClassLayoutParser extends DebugSectionParser {
    private DebugStringParser strings;
    private DebugClassParser classes;
    private ArrayList<TypeLayoutImpl> types = new ArrayList<>();
    private List<List<Consumer<TypeLayoutImpl>>> forwardReferences = new ArrayList<>();
    private int lastAddress;
    private int lastFieldOffset;
    private ClassLayoutInfoImpl classLayoutInfo;

    public DebugClassLayoutParser(DebugStringParser strings, DebugClassParser classes) {
        super(DebugConstants.SECTION_CLASS_LAYOUT, strings, classes);
        this.strings = strings;
        this.classes = classes;
    }

    @Override
    protected void doParse() {
        while (ptr < data.length) {
            var kind = data[ptr++];
            switch (kind) {
                case DebugConstants.CLASS_ROOT:
                    parseRootClass();
                    break;
                case DebugConstants.CLASS_CLASS:
                    parseClass();
                    break;
                case DebugConstants.CLASS_INTERFACE:
                    parseInterface();
                    break;
                case DebugConstants.CLASS_ARRAY:
                    parseArray();
                    break;
                case DebugConstants.CLASS_BOOLEAN:
                    parsePrimitive(PrimitiveType.BOOLEAN);
                    break;
                case DebugConstants.CLASS_BYTE:
                    parsePrimitive(PrimitiveType.BYTE);
                    break;
                case DebugConstants.CLASS_SHORT:
                    parsePrimitive(PrimitiveType.SHORT);
                    break;
                case DebugConstants.CLASS_CHAR:
                    parsePrimitive(PrimitiveType.CHARACTER);
                    break;
                case DebugConstants.CLASS_INT:
                    parsePrimitive(PrimitiveType.INTEGER);
                    break;
                case DebugConstants.CLASS_LONG:
                    parsePrimitive(PrimitiveType.LONG);
                    break;
                case DebugConstants.CLASS_FLOAT:
                    parsePrimitive(PrimitiveType.FLOAT);
                    break;
                case DebugConstants.CLASS_DOUBLE:
                    parsePrimitive(PrimitiveType.DOUBLE);
                    break;
                case DebugConstants.CLASS_UNKNOWN:
                    parseUnknown();
                    break;
            }
        }
        types.trimToSize();
        classLayoutInfo = new ClassLayoutInfoImpl(Collections.unmodifiableList(types));
    }

    public ClassLayoutInfo getInfo() {
        return classLayoutInfo;
    }

    private void parseRootClass() {
        var classPtr = classes.getClass(readLEB());
        var address = readAddress();
        var size = readLEB();
        var type = new ClassLayoutImpl(address, classPtr, size);
        addType(type);
        parseClassFields(type);
    }

    private void parseClass() {
        var classPtr = classes.getClass(readLEB());
        var superclassIndex = types.size() - readSignedLEB();
        var address = readAddress();
        var size = readLEB();
        var type = new ClassLayoutImpl(address, classPtr, size);
        addType(type);
        ref(superclassIndex, superclass -> type.superclass = (ClassLayoutImpl) superclass);
        parseClassFields(type);
    }

    private void parseClassFields(ClassLayoutImpl type) {
        lastFieldOffset = 0;
        var staticFields = new ArrayList<FieldInfoImpl>();
        while (true) {
            var fieldType = data[ptr++];
            if (fieldType == DebugConstants.FIELD_END) {
                staticFields.trimToSize();
                type.staticFields = staticFields.isEmpty() ? Collections.emptyList()
                        : Collections.unmodifiableList(staticFields);
                type.instanceFields = Collections.emptyList();
                return;
            }
            if (fieldType == DebugConstants.FIELD_END_SEQUENCE) {
                break;
            }
            staticFields.add(parseField(fieldType));
        }

        lastFieldOffset = 0;
        var instanceFields = new ArrayList<FieldInfoImpl>();
        while (true) {
            var fieldType = data[ptr++];
            if (fieldType == DebugConstants.FIELD_END || fieldType == DebugConstants.FIELD_END_SEQUENCE) {
                break;
            }
            instanceFields.add(parseField(fieldType));
        }

        staticFields.trimToSize();
        instanceFields.trimToSize();
        type.staticFields = staticFields.isEmpty() ? Collections.emptyList()
                : Collections.unmodifiableList(staticFields);
        type.instanceFields = instanceFields.isEmpty() ? Collections.emptyList()
                : Collections.unmodifiableList(instanceFields);
    }

    private FieldInfoImpl parseField(byte type) {
        FieldType fieldType;
        switch (type) {
            case DebugConstants.FIELD_BOOLEAN:
                fieldType = FieldType.BOOLEAN;
                break;
            case DebugConstants.FIELD_BYTE:
                fieldType = FieldType.BYTE;
                break;
            case DebugConstants.FIELD_SHORT:
                fieldType = FieldType.SHORT;
                break;
            case DebugConstants.FIELD_CHAR:
                fieldType = FieldType.CHAR;
                break;
            case DebugConstants.FIELD_INT:
                fieldType = FieldType.INT;
                break;
            case DebugConstants.FIELD_LONG:
                fieldType = FieldType.LONG;
                break;
            case DebugConstants.FIELD_FLOAT:
                fieldType = FieldType.FLOAT;
                break;
            case DebugConstants.FIELD_DOUBLE:
                fieldType = FieldType.DOUBLE;
                break;
            case DebugConstants.FIELD_OBJECT:
                fieldType = FieldType.OBJECT;
                break;
            case DebugConstants.FIELD_ADDRESS:
                fieldType = FieldType.ADDRESS;
                break;
            default:
                fieldType = FieldType.UNDEFINED;
                break;
        }
        var name = strings.getString(readLEB());
        var offset = lastFieldOffset + readSignedLEB();
        lastFieldOffset = offset;
        return new FieldInfoImpl(offset, name, fieldType);
    }

    private void parseInterface() {
        var classRef = classes.getClass(readLEB());
        var address = readAddress();
        addType(new InterfaceLayoutImpl(address, classRef));
    }

    private void parseArray() {
        var elementPtr = types.size() - readSignedLEB();
        var address = readAddress();
        var type = new ArrayLayoutImpl(address);
        addType(type);
        ref(elementPtr, element -> type.elementType = element);
    }

    private void parsePrimitive(PrimitiveType primitiveType) {
        var address = readAddress();
        addType(new PrimitiveLayoutImpl(address, primitiveType));
    }

    private void parseUnknown() {
        var address = readAddress();
        addType(new UnknownLayoutImpl(address));
    }

    private int readAddress() {
        var result = readSignedLEB() + lastAddress;
        lastAddress = result;
        return result;
    }

    private void addType(TypeLayoutImpl type) {
        var index = types.size();
        types.add(type);
        if (index < forwardReferences.size()) {
            var refs = forwardReferences.get(index);
            forwardReferences.set(index, null);
            for (var ref : refs) {
                ref.accept(type);
            }
        }
    }

    private void ref(int index, Consumer<TypeLayoutImpl> handler) {
        if (index < types.size()) {
            handler.accept(types.get(index));
        } else {
            if (index >= forwardReferences.size()) {
                forwardReferences.addAll(Collections.nCopies(index + 1 - forwardReferences.size(), null));
            }
            var refs = forwardReferences.get(index);
            if (refs == null) {
                refs = new ArrayList<>();
                forwardReferences.set(index, refs);
            }
            refs.add(handler);
        }
    }

    private static abstract class TypeLayoutImpl implements TypeLayout {
        private int address;

        private TypeLayoutImpl(int address) {
            this.address = address;
        }

        @Override
        public int address() {
            return address;
        }
    }

    private static class ClassLayoutImpl extends TypeLayoutImpl implements ClassLayout {
        private ClassInfo classRef;
        private List<? extends FieldInfoImpl> instanceFields;
        private List<? extends FieldInfoImpl> staticFields;
        private ClassLayoutImpl superclass;
        private int size;

         ClassLayoutImpl(int address, ClassInfo classRef, int size) {
            super(address);
            this.classRef = classRef;
            this.size = size;
        }

        @Override
        public ClassInfo classRef() {
            return classRef;
        }

        @Override
        public ClassLayout superclass() {
            return superclass;
        }

        @Override
        public Collection<? extends FieldInfo> instanceFields() {
            return instanceFields;
        }

        @Override
        public Collection<? extends FieldInfo> staticFields() {
            return staticFields;
        }

        @Override
        public int size() {
            return size;
        }
    }

    private static class FieldInfoImpl extends FieldInfo {
        private int address;
        private String name;
        private FieldType type;

        FieldInfoImpl(int address, String name, FieldType type) {
            this.address = address;
            this.name = name;
            this.type = type;
        }

        @Override
        public int address() {
            return address;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public FieldType type() {
            return type;
        }
    }

    private static class InterfaceLayoutImpl extends TypeLayoutImpl implements InterfaceLayout {
        private ClassInfo classRef;

        InterfaceLayoutImpl(int address, ClassInfo classRef) {
            super(address);
            this.classRef = classRef;
        }

        @Override
        public ClassInfo classRef() {
            return classRef;
        }
    }

    private static class ArrayLayoutImpl extends TypeLayoutImpl implements ArrayLayout {
        private TypeLayoutImpl elementType;

        ArrayLayoutImpl(int address) {
            super(address);
        }

        @Override
        public TypeLayout elementType() {
            return elementType;
        }
    }

    private static class PrimitiveLayoutImpl extends TypeLayoutImpl implements PrimitiveLayout {
        private PrimitiveType primitiveType;

        PrimitiveLayoutImpl(int address, PrimitiveType primitiveType) {
            super(address);
            this.primitiveType = primitiveType;
        }

        @Override
        public PrimitiveType primitiveType() {
            return primitiveType;
        }
    }

    private static class UnknownLayoutImpl extends TypeLayoutImpl implements UnknownLayout {
        UnknownLayoutImpl(int address) {
            super(address);
        }
    }

    private static class ClassLayoutInfoImpl extends ClassLayoutInfo {
        private List<TypeLayoutImpl> types;

        ClassLayoutInfoImpl(List<TypeLayoutImpl> types) {
            this.types = types;
        }

        @Override
        public List<? extends TypeLayout> types() {
            return types;
        }
    }
}
