/*
 *  Copyright 2014 Alexey Andreev.
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

import java.io.*;
import java.util.*;
import org.teavm.model.*;
import org.teavm.parsing.ClassDateProvider;

public class DiskCachedClassHolderSource implements ClassHolderSource {
    private static AccessLevel[] accessLevels = AccessLevel.values();
    private static ElementModifier[] elementModifiers = ElementModifier.values();
    private File directory;
    private SymbolTable symbolTable;
    private ClassHolderSource innerSource;
    private ClassDateProvider classDateProvider;
    private Map<String, Item> cache = new LinkedHashMap<>();
    private Set<String> newClasses = new HashSet<>();
    private ProgramIO programIO;

    public DiskCachedClassHolderSource(File directory, SymbolTable symbolTable, SymbolTable fileTable,
            ClassHolderSource innerSource, ClassDateProvider classDateProvider) {
        this.directory = directory;
        this.symbolTable = symbolTable;
        this.innerSource = innerSource;
        this.classDateProvider = classDateProvider;
        programIO = new ProgramIO(symbolTable, fileTable);
    }

    @Override
    public ClassHolder get(String name) {
        Item item = cache.get(name);
        if (item == null) {
            item = new Item();
            cache.put(name, item);
            File classFile = new File(directory, name.replace('.', '/') + ".teavm-cls");
            if (classFile.exists()) {
                Date classDate = classDateProvider.getModificationDate(name);
                if (classDate != null && classDate.before(new Date(classFile.lastModified()))) {
                    try (InputStream input = new BufferedInputStream(new FileInputStream(classFile))) {
                        item.cls = readClass(input, name);
                    } catch (IOException e) {
                        // We could not access cache file, so let's parse class file
                        item.cls = null;
                    }
                }
            }
            if (item.cls == null) {
                item.cls = innerSource.get(name);
                newClasses.add(name);
            }
        }
        return item.cls;
    }

    private static class Item {
        ClassHolder cls;
    }

    public void flush() throws IOException {
        for (String className : newClasses) {
            Item item = cache.get(className);
            if (item.cls != null) {
                File classFile = new File(directory, className.replace('.', '/') + ".teavm-cls");
                classFile.getParentFile().mkdirs();
                try (OutputStream output = new BufferedOutputStream(new FileOutputStream(classFile))) {
                    writeClass(output, item.cls);
                }
            }
        }
    }

    private void writeClass(OutputStream stream, ClassHolder cls) throws IOException {
        DataOutput output = new DataOutputStream(stream);
        output.writeByte(cls.getLevel().ordinal());
        output.writeInt(packModifiers(cls.getModifiers()));
        output.writeInt(cls.getParent() != null ? symbolTable.lookup(cls.getParent()) : -1);
        output.writeInt(cls.getOwnerName() != null ? symbolTable.lookup(cls.getOwnerName()) : -1);
        output.writeByte(cls.getInterfaces().size());
        for (String iface : cls.getInterfaces()) {
            output.writeInt(symbolTable.lookup(iface));
        }
        writeAnnotations(output, cls.getAnnotations());
        output.writeShort(cls.getFields().size());
        for (FieldHolder field : cls.getFields()) {
            writeField(output, field);
        }
        output.writeShort(cls.getMethods().size());
        for (MethodHolder method : cls.getMethods()) {
            writeMethod(stream, method);
        }
    }

    private ClassHolder readClass(InputStream stream, String name) throws IOException {
        DataInput input = new DataInputStream(stream);
        ClassHolder cls = new ClassHolder(name);
        cls.setLevel(accessLevels[input.readByte()]);
        cls.getModifiers().addAll(unpackModifiers(input.readInt()));
        int parentIndex = input.readInt();
        cls.setParent(parentIndex >= 0 ? symbolTable.at(parentIndex) : null);
        int ownerIndex = input.readInt();
        cls.setOwnerName(ownerIndex >= 0 ? symbolTable.at(ownerIndex) : null);
        int ifaceCount = input.readByte();
        for (int i = 0; i < ifaceCount; ++i) {
            cls.getInterfaces().add(symbolTable.at(input.readInt()));
        }
        readAnnotations(input, cls.getAnnotations());
        int fieldCount = input.readShort();
        for (int i = 0; i < fieldCount; ++i) {
            cls.addField(readField(input));
        }
        int methodCount = input.readShort();
        for (int i = 0; i < methodCount; ++i) {
            cls.addMethod(readMethod(stream));
        }
        return cls;
    }

    private void writeField(DataOutput output, FieldHolder field) throws IOException {
        output.writeInt(symbolTable.lookup(field.getName()));
        output.writeInt(symbolTable.lookup(field.getType().toString()));
        output.writeByte(field.getLevel().ordinal());
        output.writeInt(packModifiers(field.getModifiers()));
        writeFieldValue(output, field.getInitialValue());
        writeAnnotations(output, field.getAnnotations());
    }

    private FieldHolder readField(DataInput input) throws IOException {
        FieldHolder field = new FieldHolder(symbolTable.at(input.readInt()));
        field.setType(ValueType.parse(symbolTable.at(input.readInt())));
        field.setLevel(accessLevels[input.readByte()]);
        field.getModifiers().addAll(unpackModifiers(input.readInt()));
        field.setInitialValue(readFieldValue(input));
        readAnnotations(input, field.getAnnotations());
        return field;
    }

    private void writeFieldValue(DataOutput output, Object value) throws IOException {
        if (value == null) {
            output.writeByte(0);
        } else if (value instanceof Integer) {
            output.writeByte(1);
            output.writeInt((Integer) value);
        } else if (value instanceof Long) {
            output.writeByte(2);
            output.writeLong((Long) value);
        } else if (value instanceof Float) {
            output.writeByte(3);
            output.writeFloat((Float) value);
        } else if (value instanceof Double) {
            output.writeByte(4);
            output.writeDouble((Double) value);
        } else if (value instanceof String) {
            output.writeByte(5);
            output.writeUTF((String) value);
        }
    }

    private Object readFieldValue(DataInput input) throws IOException {
        int type = input.readByte();
        switch (type) {
            case 0:
                return null;
            case 1:
                return input.readInt();
            case 2:
                return input.readLong();
            case 3:
                return input.readFloat();
            case 4:
                return input.readDouble();
            case 5:
                return input.readUTF();
            default:
                throw new RuntimeException("Unexpected field value type: " + type);
        }
    }

    private void writeMethod(OutputStream stream, MethodHolder method) throws IOException {
        DataOutputStream output = new DataOutputStream(stream);
        output.writeInt(symbolTable.lookup(method.getDescriptor().toString()));
        output.writeByte(method.getLevel().ordinal());
        output.writeInt(packModifiers(method.getModifiers()));
        writeAnnotations(output, method.getAnnotations());

        for (AnnotationContainer parameterAnnotation : method.getParameterAnnotations()) {
            writeAnnotations(output, parameterAnnotation);
        }

        if (method.getAnnotationDefault() != null) {
            output.writeBoolean(true);
            writeAnnotationValue(output, method.getAnnotationDefault());
        } else {
            output.writeBoolean(false);
        }

        if (method.getProgram() != null) {
            output.writeBoolean(true);
            programIO.write(method.getProgram(), output);
        } else {
            output.writeBoolean(false);
        }
    }

    private MethodHolder readMethod(InputStream stream) throws IOException {
        DataInputStream input = new DataInputStream(stream);
        MethodHolder method = new MethodHolder(MethodDescriptor.parse(symbolTable.at(input.readInt())));
        method.setLevel(accessLevels[input.readByte()]);
        method.getModifiers().addAll(unpackModifiers(input.readInt()));
        readAnnotations(input, method.getAnnotations());

        for (int i = 0; i < method.parameterCount(); ++i) {
            readAnnotations(input, method.parameterAnnotation(i));
        }

        if (input.readBoolean()) {
            method.setAnnotationDefault(readAnnotationValue(input));
        }

        boolean hasProgram = input.readBoolean();
        if (hasProgram) {
            method.setProgram(programIO.read(input));
        }
        return method;
    }

    private void writeAnnotations(DataOutput output, AnnotationContainer annotations) throws IOException {
        List<AnnotationHolder> annotationList = new ArrayList<>();
        for (AnnotationHolder annot : annotations.all()) {
            annotationList.add(annot);
        }
        output.writeShort(annotationList.size());
        for (AnnotationHolder annot : annotationList) {
            writeAnnotation(output, annot);
        }
    }

    private void readAnnotations(DataInput input, AnnotationContainer annotations) throws IOException {
        int annotCount = input.readShort();
        for (int i = 0; i < annotCount; ++i) {
            AnnotationHolder annot = readAnnotation(input);
            annotations.add(annot);
        }
    }

    private void writeAnnotation(DataOutput output, AnnotationReader annotation) throws IOException {
        output.writeInt(symbolTable.lookup(annotation.getType()));
        int fieldCount = 0;
        for (@SuppressWarnings("unused") String field : annotation.getAvailableFields()) {
            ++fieldCount;
        }
        output.writeShort(fieldCount);
        for (String field : annotation.getAvailableFields()) {
            output.writeInt(symbolTable.lookup(field));
            writeAnnotationValue(output, annotation.getValue(field));
        }
    }

    private AnnotationHolder readAnnotation(DataInput input) throws IOException {
        AnnotationHolder annotation = new AnnotationHolder(symbolTable.at(input.readInt()));
        int valueCount = input.readShort();
        for (int i = 0; i < valueCount; ++i) {
            String name = symbolTable.at(input.readInt());
            AnnotationValue value = readAnnotationValue(input);
            annotation.getValues().put(name, value);
        }
        return annotation;
    }

    private void writeAnnotationValue(DataOutput output, AnnotationValue value) throws IOException {
        output.writeByte(value.getType());
        switch (value.getType()) {
            case AnnotationValue.ANNOTATION:
                writeAnnotation(output, value.getAnnotation());
                break;
            case AnnotationValue.BOOLEAN:
                output.writeBoolean(value.getBoolean());
                break;
            case AnnotationValue.BYTE:
                output.writeByte(value.getByte());
                break;
            case AnnotationValue.CLASS:
                output.writeInt(symbolTable.lookup(value.getJavaClass().toString()));
                break;
            case AnnotationValue.DOUBLE:
                output.writeDouble(value.getDouble());
                break;
            case AnnotationValue.ENUM:
                output.writeInt(symbolTable.lookup(value.getEnumValue().getClassName()));
                output.writeInt(symbolTable.lookup(value.getEnumValue().getFieldName()));
                break;
            case AnnotationValue.FLOAT:
                output.writeFloat(value.getFloat());
                break;
            case AnnotationValue.INT:
                output.writeInt(value.getInt());
                break;
            case AnnotationValue.LIST: {
                List<AnnotationValue> list = value.getList();
                output.writeShort(list.size());
                for (AnnotationValue item : list) {
                    writeAnnotationValue(output, item);
                }
                break;
            }
            case AnnotationValue.LONG:
                output.writeLong(value.getLong());
                break;
            case AnnotationValue.SHORT:
                output.writeShort(value.getShort());
                break;
            case AnnotationValue.STRING:
                output.writeUTF(value.getString());
                break;
        }
    }

    private AnnotationValue readAnnotationValue(DataInput input) throws IOException {
        byte type = input.readByte();
        switch (type) {
            case AnnotationValue.ANNOTATION:
                return new AnnotationValue(readAnnotation(input));
            case AnnotationValue.BOOLEAN:
                return new AnnotationValue(input.readBoolean());
            case AnnotationValue.BYTE:
                return new AnnotationValue(input.readByte());
            case AnnotationValue.CLASS:
                return new AnnotationValue(ValueType.parse(symbolTable.at(input.readInt())));
            case AnnotationValue.DOUBLE:
                return new AnnotationValue(input.readDouble());
            case AnnotationValue.ENUM: {
                String className = symbolTable.at(input.readInt());
                String fieldName = symbolTable.at(input.readInt());
                return new AnnotationValue(new FieldReference(className, fieldName));
            }
            case AnnotationValue.FLOAT:
                return new AnnotationValue(input.readFloat());
            case AnnotationValue.INT:
                return new AnnotationValue(input.readInt());
            case AnnotationValue.LIST: {
                List<AnnotationValue> list = new ArrayList<>();
                int sz = input.readShort();
                for (int i = 0; i < sz; ++i) {
                    list.add(readAnnotationValue(input));
                }
                return new AnnotationValue(list);
            }
            case AnnotationValue.LONG:
                return new AnnotationValue(input.readLong());
            case AnnotationValue.SHORT:
                return new AnnotationValue(input.readShort());
            case AnnotationValue.STRING:
                return new AnnotationValue(input.readUTF());
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
