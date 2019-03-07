/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.cache;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationContainer;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassHolder;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.ReferenceCache;
import org.teavm.model.ValueType;

public class ClassIO {
    private static AccessLevel[] accessLevels = AccessLevel.values();
    private static ElementModifier[] elementModifiers = ElementModifier.values();
    private SymbolTable symbolTable;
    private ProgramIO programIO;

    public ClassIO(ReferenceCache referenceCache, SymbolTable symbolTable, SymbolTable fileTable,
            SymbolTable varTable) {
        this.symbolTable = symbolTable;
        programIO = new ProgramIO(referenceCache, symbolTable, fileTable, varTable);
    }

    public void writeClass(OutputStream stream, ClassHolder cls) throws IOException {
        VarDataOutput output = new VarDataOutput(stream);
        output.writeUnsigned(cls.getLevel().ordinal());
        output.writeUnsigned(packModifiers(cls.getModifiers()));
        output.writeUnsigned(cls.getParent() != null ? symbolTable.lookup(cls.getParent()) + 1 : 0);
        output.writeUnsigned(cls.getOwnerName() != null ? symbolTable.lookup(cls.getOwnerName()) + 1 : 0);
        output.writeUnsigned(cls.getInterfaces().size());
        for (String iface : cls.getInterfaces()) {
            output.writeUnsigned(symbolTable.lookup(iface));
        }
        writeAnnotations(output, cls.getAnnotations());
        output.writeUnsigned(cls.getFields().size());
        for (FieldHolder field : cls.getFields()) {
            writeField(output, field);
        }
        output.writeUnsigned(cls.getMethods().size());
        for (MethodHolder method : cls.getMethods()) {
            writeMethod(output, method);
        }
    }

    public ClassHolder readClass(InputStream stream, String name) throws IOException {
        VarDataInput input = new VarDataInput(stream);
        ClassHolder cls = new ClassHolder(name);
        cls.setLevel(accessLevels[input.readUnsigned()]);
        cls.getModifiers().addAll(unpackModifiers(input.readUnsigned()));
        int parentIndex = input.readUnsigned();
        cls.setParent(parentIndex > 0 ? symbolTable.at(parentIndex - 1) : null);
        int ownerIndex = input.readUnsigned();
        cls.setOwnerName(ownerIndex > 0 ? symbolTable.at(ownerIndex - 1) : null);
        int ifaceCount = input.readUnsigned();
        for (int i = 0; i < ifaceCount; ++i) {
            cls.getInterfaces().add(symbolTable.at(input.readUnsigned()));
        }
        readAnnotations(input, cls.getAnnotations());
        int fieldCount = input.readUnsigned();
        for (int i = 0; i < fieldCount; ++i) {
            cls.addField(readField(input));
        }
        int methodCount = input.readUnsigned();
        for (int i = 0; i < methodCount; ++i) {
            cls.addMethod(readMethod(input));
        }
        return cls;
    }

    private void writeField(VarDataOutput output, FieldHolder field) throws IOException {
        output.writeUnsigned(symbolTable.lookup(field.getName()));
        output.writeUnsigned(symbolTable.lookup(field.getType().toString()));
        output.writeUnsigned(field.getLevel().ordinal());
        output.writeUnsigned(packModifiers(field.getModifiers()));
        writeFieldValue(output, field.getInitialValue());
        writeAnnotations(output, field.getAnnotations());
    }

    private FieldHolder readField(VarDataInput input) throws IOException {
        FieldHolder field = new FieldHolder(symbolTable.at(input.readUnsigned()));
        field.setType(ValueType.parse(symbolTable.at(input.readUnsigned())));
        field.setLevel(accessLevels[input.readUnsigned()]);
        field.getModifiers().addAll(unpackModifiers(input.readUnsigned()));
        field.setInitialValue(readFieldValue(input));
        readAnnotations(input, field.getAnnotations());
        return field;
    }

    private void writeFieldValue(VarDataOutput output, Object value) throws IOException {
        if (value == null) {
            output.writeUnsigned(0);
        } else if (value instanceof Integer) {
            output.writeUnsigned(1);
            output.writeUnsigned((Integer) value);
        } else if (value instanceof Long) {
            output.writeUnsigned(2);
            output.writeSigned((Long) value);
        } else if (value instanceof Float) {
            output.writeUnsigned(3);
            output.writeFloat((Float) value);
        } else if (value instanceof Double) {
            output.writeUnsigned(4);
            output.writeDouble((Double) value);
        } else if (value instanceof String) {
            output.writeUnsigned(5);
            output.write((String) value);
        }
    }

    private Object readFieldValue(VarDataInput input) throws IOException {
        int type = input.readUnsigned();
        switch (type) {
            case 0:
                return null;
            case 1:
                return input.readSigned();
            case 2:
                return input.readSignedLong();
            case 3:
                return input.readFloat();
            case 4:
                return input.readDouble();
            case 5:
                return input.read();
            default:
                throw new RuntimeException("Unexpected field value type: " + type);
        }
    }

    private void writeMethod(VarDataOutput output, MethodHolder method) throws IOException {
        output.writeUnsigned(symbolTable.lookup(method.getDescriptor().toString()));
        output.writeUnsigned(method.getLevel().ordinal());
        output.writeUnsigned(packModifiers(method.getModifiers()));
        writeAnnotations(output, method.getAnnotations());

        for (AnnotationContainer parameterAnnotation : method.getParameterAnnotations()) {
            writeAnnotations(output, parameterAnnotation);
        }

        if (method.getAnnotationDefault() != null) {
            output.writeUnsigned(1);
            writeAnnotationValue(output, method.getAnnotationDefault());
        } else {
            output.writeUnsigned(0);
        }

        if (method.getProgram() != null) {
            output.writeUnsigned(1);
            programIO.write(method.getProgram(), output);
        } else {
            output.writeUnsigned(0);
        }
    }

    private MethodHolder readMethod(VarDataInput input) throws IOException {
        MethodHolder method = new MethodHolder(MethodDescriptor.parse(symbolTable.at(input.readUnsigned())));
        method.setLevel(accessLevels[input.readUnsigned()]);
        method.getModifiers().addAll(unpackModifiers(input.readUnsigned()));
        readAnnotations(input, method.getAnnotations());

        for (int i = 0; i < method.parameterCount(); ++i) {
            readAnnotations(input, method.parameterAnnotation(i));
        }

        if (input.readUnsigned() != 0) {
            method.setAnnotationDefault(readAnnotationValue(input));
        }

        if (input.readUnsigned() != 0) {
            method.setProgram(programIO.read(input));
        }
        return method;
    }

    private void writeAnnotations(VarDataOutput output, AnnotationContainer annotations) throws IOException {
        List<AnnotationHolder> annotationList = new ArrayList<>();
        for (AnnotationHolder annot : annotations.all()) {
            annotationList.add(annot);
        }
        output.writeUnsigned(annotationList.size());
        for (AnnotationHolder annot : annotationList) {
            writeAnnotation(output, annot);
        }
    }

    private void readAnnotations(VarDataInput input, AnnotationContainer annotations) throws IOException {
        int annotCount = input.readUnsigned();
        for (int i = 0; i < annotCount; ++i) {
            AnnotationHolder annot = readAnnotation(input);
            annotations.add(annot);
        }
    }

    private void writeAnnotation(VarDataOutput output, AnnotationReader annotation) throws IOException {
        output.writeUnsigned(symbolTable.lookup(annotation.getType()));
        int fieldCount = 0;
        for (@SuppressWarnings("unused") String field : annotation.getAvailableFields()) {
            ++fieldCount;
        }
        output.writeUnsigned(fieldCount);
        for (String field : annotation.getAvailableFields()) {
            output.writeUnsigned(symbolTable.lookup(field));
            writeAnnotationValue(output, annotation.getValue(field));
        }
    }

    private AnnotationHolder readAnnotation(VarDataInput input) throws IOException {
        AnnotationHolder annotation = new AnnotationHolder(symbolTable.at(input.readUnsigned()));
        int valueCount = input.readUnsigned();
        for (int i = 0; i < valueCount; ++i) {
            String name = symbolTable.at(input.readUnsigned());
            AnnotationValue value = readAnnotationValue(input);
            annotation.getValues().put(name, value);
        }
        return annotation;
    }

    private void writeAnnotationValue(VarDataOutput output, AnnotationValue value) throws IOException {
        output.writeUnsigned(value.getType());
        switch (value.getType()) {
            case AnnotationValue.ANNOTATION:
                writeAnnotation(output, value.getAnnotation());
                break;
            case AnnotationValue.BOOLEAN:
                output.writeUnsigned(value.getBoolean() ? 1 : 0);
                break;
            case AnnotationValue.BYTE:
                output.writeSigned(value.getByte());
                break;
            case AnnotationValue.CLASS:
                output.writeUnsigned(symbolTable.lookup(value.getJavaClass().toString()));
                break;
            case AnnotationValue.DOUBLE:
                output.writeDouble(value.getDouble());
                break;
            case AnnotationValue.ENUM:
                output.writeUnsigned(symbolTable.lookup(value.getEnumValue().getClassName()));
                output.writeUnsigned(symbolTable.lookup(value.getEnumValue().getFieldName()));
                break;
            case AnnotationValue.FLOAT:
                output.writeFloat(value.getFloat());
                break;
            case AnnotationValue.INT:
                output.writeSigned(value.getInt());
                break;
            case AnnotationValue.LIST: {
                List<AnnotationValue> list = value.getList();
                output.writeUnsigned(list.size());
                for (AnnotationValue item : list) {
                    writeAnnotationValue(output, item);
                }
                break;
            }
            case AnnotationValue.LONG:
                output.writeSigned(value.getLong());
                break;
            case AnnotationValue.SHORT:
                output.writeSigned(value.getShort());
                break;
            case AnnotationValue.STRING:
                output.write(value.getString());
                break;
        }
    }

    private AnnotationValue readAnnotationValue(VarDataInput input) throws IOException {
        int type = input.readUnsigned();
        switch (type) {
            case AnnotationValue.ANNOTATION:
                return new AnnotationValue(readAnnotation(input));
            case AnnotationValue.BOOLEAN:
                return new AnnotationValue(input.readUnsigned() != 0);
            case AnnotationValue.BYTE:
                return new AnnotationValue((byte) input.readSigned());
            case AnnotationValue.CLASS:
                return new AnnotationValue(ValueType.parse(symbolTable.at(input.readUnsigned())));
            case AnnotationValue.DOUBLE:
                return new AnnotationValue(input.readDouble());
            case AnnotationValue.ENUM: {
                String className = symbolTable.at(input.readUnsigned());
                String fieldName = symbolTable.at(input.readUnsigned());
                return new AnnotationValue(new FieldReference(className, fieldName));
            }
            case AnnotationValue.FLOAT:
                return new AnnotationValue(input.readFloat());
            case AnnotationValue.INT:
                return new AnnotationValue(input.readSigned());
            case AnnotationValue.LIST: {
                List<AnnotationValue> list = new ArrayList<>();
                int sz = input.readUnsigned();
                for (int i = 0; i < sz; ++i) {
                    list.add(readAnnotationValue(input));
                }
                return new AnnotationValue(list);
            }
            case AnnotationValue.LONG:
                return new AnnotationValue(input.readSignedLong());
            case AnnotationValue.SHORT:
                return new AnnotationValue((short) input.readSigned());
            case AnnotationValue.STRING:
                return new AnnotationValue(input.read());
            default:
                throw new RuntimeException("Unexpected annotation value type: " + type);
        }
    }

    private int packModifiers(Set<ElementModifier> modifiers) {
        int result = 0;
        for (ElementModifier modifier : modifiers) {
            result |= 1 << modifier.ordinal();
        }
        return result;
    }

    private Set<ElementModifier> unpackModifiers(int packed) {
        Set<ElementModifier> modifiers = EnumSet.noneOf(ElementModifier.class);
        while (packed != 0) {
            int n = Integer.numberOfTrailingZeros(packed);
            packed ^= 1 << n;
            modifiers.add(elementModifiers[n]);
        }
        return modifiers;
    }
}
