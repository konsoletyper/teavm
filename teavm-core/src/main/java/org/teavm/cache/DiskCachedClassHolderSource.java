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

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class DiskCachedClassHolderSource implements ClassHolderSource {
    private File directory;
    private SymbolTable symbolTable;
    private ClassHolderSource innerSource;
    private ClassDateProvider classDateProvider;
    private Map<String, Item> cache = new HashMap<>();
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
            item.cls = innerSource.get(name);
            newClasses.add(name);
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
                try (OutputStream output = new FileOutputStream(classFile)) {
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
        for (FieldHolder field : cls.getFields()) {
            writeField(output, field);
        }
        for (MethodHolder method : cls.getMethods()) {
            writeMethod(stream, method);
        }
    }

    private void writeField(DataOutput output, FieldHolder field) throws IOException {
        output.writeInt(symbolTable.lookup(field.getName()));
        output.writeInt(symbolTable.lookup(field.getType().toString()));
        output.writeByte(field.getLevel().ordinal());
        output.writeInt(packModifiers(field.getModifiers()));
        writeFieldValue(output, field.getInitialValue());
        writeAnnotations(output, field.getAnnotations());
    }

    private void writeFieldValue(DataOutput output, Object value) throws IOException {
        if (value == null) {
            output.writeByte(0);
        } else if (value instanceof Integer) {
            output.writeByte(1);
            output.writeInt((Integer)value);
        } else if (value instanceof Long) {
            output.writeByte(2);
            output.writeLong((Long)value);
        } else if (value instanceof Float) {
            output.writeByte(3);
            output.writeFloat((Float)value);
        } else if (value instanceof Double) {
            output.writeByte(4);
            output.writeDouble((Double)value);
        } else if (value instanceof String) {
            output.writeByte(5);
            output.writeUTF((String)value);
        }
    }

    private void writeMethod(OutputStream stream, MethodHolder method) throws IOException {
        DataOutputStream output = new DataOutputStream(stream);
        output.writeInt(symbolTable.lookup(method.getName()));
        output.writeInt(symbolTable.lookup(method.getDescriptor().toString()));
        output.writeByte(method.getLevel().ordinal());
        output.writeInt(packModifiers(method.getModifiers()));
        writeAnnotations(output, method.getAnnotations());
        if (method.getProgram() != null) {
            output.writeByte(1);
            programIO.write(method.getProgram(), output);
        } else {
            output.writeByte(0);
        }
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

    private void writeAnnotation(DataOutput output, AnnotationHolder annotation) throws IOException {
        output.writeInt(symbolTable.lookup(annotation.getType()));
        output.writeShort(annotation.getValues().size());
        for (Map.Entry<String, AnnotationValue> entry : annotation.getValues().entrySet()) {
            output.writeInt(symbolTable.lookup(entry.getKey()));
            writeAnnotationValue(output, entry.getValue());
        }
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
                output.writeInt(symbolTable.lookup(value.getEnumValue().getClassName()));
                break;
            case AnnotationValue.FLOAT:
                output.writeDouble(value.getFloat());
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

    private int packModifiers(Set<ElementModifier> modifiers) {
        int result = 0;
        for (ElementModifier modifier : modifiers) {
            result |= 1 << modifier.ordinal();
        }
        return result;
    }
}
