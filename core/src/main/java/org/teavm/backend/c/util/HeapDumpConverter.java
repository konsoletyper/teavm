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
package org.teavm.backend.c.util;

import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.LongObjectHashMap;
import com.carrotsearch.hppc.LongObjectMap;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.c.util.json.JsonAllErrorVisitor;
import org.teavm.backend.c.util.json.JsonArrayVisitor;
import org.teavm.backend.c.util.json.JsonErrorReporter;
import org.teavm.backend.c.util.json.JsonObjectVisitor;
import org.teavm.backend.c.util.json.JsonParser;
import org.teavm.backend.c.util.json.JsonPropertyVisitor;
import org.teavm.backend.c.util.json.JsonVisitingConsumer;
import org.teavm.backend.c.util.json.JsonVisitor;

public final class HeapDumpConverter {
    private static byte[] buffer = new byte[8];
    private static int idSize;

    private HeapDumpConverter() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Converts TeaVM/C heap dump into HotSpot compatible format (hprof)");
            System.err.println("Two arguments expected: input file (JSON), output file (hprof)");
            System.exit(-1);
        }

        SymbolTable symbolTable;
        try (Reader reader = createReader(args[0])) {
            symbolTable = fillSymbolTable(reader);
        }

        try (Reader reader = createReader(args[0]);
                RandomAccessFile output = new RandomAccessFile(args[1], "rw")) {
            generateHprofFile(reader, output, symbolTable);
        }
    }

    private static Reader createReader(String fileName) throws IOException {
        return new InputStreamReader(new BufferedInputStream(new FileInputStream(fileName)), StandardCharsets.UTF_8);
    }

    private static SymbolTable fillSymbolTable(Reader reader) throws IOException {
        SymbolTable symbolTable = new SymbolTable();
        JsonPropertyVisitor rootObjectVisitor = new JsonPropertyVisitor(true);
        rootObjectVisitor.addProperty("pointerSize", pointerSizeVisitor);
        rootObjectVisitor.addProperty("classes", new JsonArrayVisitor(new SymbolTableClassVisitor(symbolTable)));
        rootObjectVisitor.addProperty("stack", new JsonArrayVisitor(new SymbolTableStackVisitor(symbolTable)));
        JsonParser parser = new JsonParser(new JsonVisitingConsumer(new JsonObjectVisitor(rootObjectVisitor)));
        parser.parse(reader);

        if (symbolTable.classLoaderClassId == 0) {
            addFakeClass(1, symbolTable, "java.lang.ClassLoader", symbolTable.objectClassId);
        }

        if (symbolTable.referenceClassId == 0) {
            addFakeClass(2, symbolTable, "java.lang.ref.Reference", symbolTable.objectClassId);
            symbolTable.referenceClassId = 2;
        }
        if (symbolTable.weakReferenceClassId == 0) {
            addFakeClass(3, symbolTable, "java.lang.ref.WeakReference", symbolTable.referenceClassId);
        }
        if (symbolTable.softReferenceClassId == 0) {
            addFakeClass(4, symbolTable, "java.lang.ref.SoftReference", symbolTable.referenceClassId);
        }
        if (symbolTable.phantomReferenceClassId == 0) {
            addFakeClass(5, symbolTable, "java.lang.ref.PhantomReference", symbolTable.referenceClassId);
        }
        if (symbolTable.finalReferenceClassId == 0) {
            addFakeClass(6, symbolTable, "java.lang.ref.FinalReference", symbolTable.referenceClassId);
        }

        fixClassNames(symbolTable);
        return symbolTable;
    }

    private static void addFakeClass(long id, SymbolTable symbolTable, String name, long parent) {
        ClassDescriptor cls = new ClassDescriptor();
        cls.id = id;
        cls.superClassId = parent;
        cls.name = name;
        symbolTable.classesById.put(cls.id, cls);
        symbolTable.classList.add(cls);
    }

    private static void fixClassNames(SymbolTable symbolTable) {
        int serialIdGen = 1;
        int anonymousIdGen = 0;

        for (ClassDescriptor descriptor : symbolTable.classList) {
            if (descriptor.primitiveType != null) {
                continue;
            }
            descriptor.serialId = serialIdGen++;

            if (descriptor.itemClassId != 0) {
                continue;
            }

            if (descriptor.name == null) {
                do {
                    descriptor.name = "anonymous_" + anonymousIdGen++;
                } while (symbolTable.getClass(descriptor.name) != null);
            } else {
                descriptor.name = descriptor.name.replace('.', '/');
            }
        }

        List<ClassDescriptor> arrayClasses = new ArrayList<>();
        for (ClassDescriptor descriptor : symbolTable.classList) {
            if (descriptor.itemClassId == 0 || descriptor.name != null) {
                continue;
            }

            while (descriptor.itemClassId != 0) {
                arrayClasses.add(descriptor);
                descriptor = symbolTable.classesById.get(descriptor.itemClassId);
                if (descriptor.name != null) {
                    break;
                }
            }

            String name = descriptor.name;
            if (descriptor.primitiveType != null) {
                switch (descriptor.primitiveType) {
                    case BOOLEAN:
                        name = "Z";
                        break;
                    case BYTE:
                        name = "B";
                        break;
                    case SHORT:
                        name = "S";
                        break;
                    case CHAR:
                        name = "C";
                        break;
                    case INT:
                        name = "I";
                        break;
                    case LONG:
                        name = "J";
                        break;
                    case FLOAT:
                        name = "F";
                        break;
                    case DOUBLE:
                        name = "D";
                        break;
                    case OBJECT:
                    case ARRAY:
                        assert false;
                        break;
                }
            } else if (descriptor.itemClassId == 0) {
                name = "L" + name + ";";
            }

            for (int i = arrayClasses.size() - 1; i >= 0; --i) {
                name = "[" + name;
                arrayClasses.get(i).name = name;
            }

            arrayClasses.clear();
        }

        for (ClassDescriptor descriptor : symbolTable.classList) {
            if (descriptor.name != null) {
                symbolTable.lookup(descriptor.name);
                symbolTable.classes.put(descriptor.name, descriptor);
            }
        }
    }

    private static JsonVisitor pointerSizeVisitor = new JsonAllErrorVisitor() {
        @Override
        public void intValue(JsonErrorReporter reporter, long value) {
            idSize = (int) value;
        }
    };

    private static void generateHprofFile(Reader reader, RandomAccessFile output, SymbolTable symbolTable)
            throws IOException {
        output.write("JAVA PROFILE 1.0.2\0".getBytes(StandardCharsets.UTF_8));
        output.writeInt(idSize);
        output.writeLong(System.currentTimeMillis());

        BufferedFile bufferedOutput = new BufferedFile(output);
        writeSymbols(bufferedOutput, symbolTable);
        writeStack(bufferedOutput, symbolTable);
        writeHeapDump(reader, bufferedOutput, symbolTable);
        bufferedOutput.flush();

        output.write(0x2C);
        output.writeInt(0);
        output.writeInt(0);
        output.setLength(output.getFilePointer());
    }

    private static void writeSymbols(BufferedFile output, SymbolTable symbolTable) throws IOException {
        List<String> strings = symbolTable.getStrings();
        for (int i = 0; i < strings.size(); ++i) {
            byte[] bytes = strings.get(i).getBytes(StandardCharsets.UTF_8);
            output.write(1);
            output.writeInt(0);
            output.writeInt(idSize + bytes.length);

            writeId(output, i + 1);
            output.write(bytes);
        }
    }

    private static void writeStack(BufferedFile output, SymbolTable symbolTable) throws IOException {
        for (int i = 0; i < symbolTable.stack.size(); ++i) {
            Frame frame = symbolTable.stack.get(i);
            output.write(4);
            output.writeInt(0);
            output.writeInt(4 * idSize + 8);
            writeId(output, i + 1);
            writeId(output, frame.methodName != null ? symbolTable.lookup(frame.methodName) : 0);
            writeId(output, 0);
            writeId(output, frame.fileName != null ? symbolTable.lookup(frame.fileName) : 0);
            int classSerialId = 0;
            if (frame.className != null) {
                ClassDescriptor cls = symbolTable.getClass(frame.className);
                if (cls != null) {
                    classSerialId = cls.serialId;
                }
            }
            output.writeInt(classSerialId);
            output.writeInt(frame.lineNumber);
        }

        output.write(5);
        output.writeInt(0);
        output.writeInt(12 + idSize * symbolTable.stack.size());

        output.writeInt(1);
        output.writeInt(0);
        output.writeInt(symbolTable.stack.size());
        for (int i = 0; i < symbolTable.stack.size(); ++i) {
            writeId(output, i + 1);
        }
    }

    private static void writeGcRoots(BufferedFile output, SymbolTable symbolTable) throws IOException {
        List<Frame> stack = symbolTable.stack;
        for (int i = 0; i < stack.size(); i++) {
            Frame frame = stack.get(i);
            if (frame.roots == null) {
                continue;
            }
            for (long rootId : frame.roots) {
                output.write(3);
                writeId(output, rootId);
                output.writeInt(0);
                output.writeInt(i);
            }
        }

        for (ClassDescriptor classDescriptor : symbolTable.getClasses()) {
            if (classDescriptor.primitiveType != null) {
                continue;
            }
            output.write(5);
            writeId(output, classDescriptor.id);
        }
    }

    private static void writeHeapDump(Reader reader, BufferedFile output, SymbolTable symbolTable)
            throws IOException {
        for (ClassDescriptor classDescriptor : symbolTable.getClasses()) {
            if (classDescriptor.primitiveType != null) {
                continue;
            }
            output.write(2);
            output.writeInt(0);
            output.writeInt(8 + 2 * idSize);
            output.writeInt(classDescriptor.serialId);
            writeId(output, classDescriptor.id);
            output.writeInt(1);
            writeId(output, symbolTable.lookup(classDescriptor.name));
        }

        output.write(0x0C);
        output.writeInt(0);
        output.writeInt(0);
        long mark = output.getFilePointer();

        writeGcRoots(output, symbolTable);
        writeClassObjects(output, symbolTable);

        JsonPropertyVisitor rootPropertyVisitor = new JsonPropertyVisitor(true);
        rootPropertyVisitor.addProperty("objects", new JsonArrayVisitor(new ObjectDumpVisitor(output, symbolTable)));
        JsonParser parser = new JsonParser(new JsonVisitingConsumer(new JsonObjectVisitor(rootPropertyVisitor)));
        parser.parse(reader);

        long pointerBackup = output.getFilePointer();
        int size = (int) (output.getFilePointer() - mark);
        output.seek(mark - 4);
        output.writeInt(size);
        output.seek(pointerBackup);
    }

    private static void writeClassObjects(BufferedFile output, SymbolTable symbolTable) throws IOException {
        Collection<ClassDescriptor> classes = symbolTable.getClasses();
        for (ClassDescriptor cls : classes) {
            if (cls.primitiveType == null) {
                writeClassDump(output, symbolTable, cls);
            }
        }
    }

    private static void writeClassDump(BufferedFile output, SymbolTable symbolTable, ClassDescriptor cls)
            throws IOException {
        output.write(0x20);
        writeId(output, cls.id);
        output.writeInt(1);

        long superClassId = cls.superClassId;
        if (cls.itemClassId != 0) {
            superClassId = symbolTable.objectClassId;
        }
        writeId(output, superClassId);
        writeId(output, 0);
        writeId(output, 0);
        writeId(output, 0);
        writeId(output, 0);
        writeId(output, 0);

        output.writeInt(cls.size);
        output.writeShort((short) 0);

        output.writeShort((short) cls.staticFields.size());
        int dataPtr = 0;
        for (FieldDescriptor field : cls.staticFields) {
            writeId(output, symbolTable.lookup(field.name));
            output.write(typeToInt(field.type));
            int sz = typeSize(field.type);
            output.write(cls.data, dataPtr, sz);
            dataPtr += sz;
        }

        output.writeShort((short) cls.fields.size());
        for (FieldDescriptor field : cls.fields) {
            writeId(output, symbolTable.lookup(field.name));
            output.write(typeToInt(field.type));
        }
    }

    static class ObjectDumpVisitor extends JsonAllErrorVisitor {
        private BufferedFile output;
        private SymbolTable symbolTable;
        private JsonPropertyVisitor propertyVisitor = new JsonPropertyVisitor(true);
        private long id;
        private long classId;
        private byte[] data;

        ObjectDumpVisitor(BufferedFile output, SymbolTable symbolTable) {
            this.output = output;
            this.symbolTable = symbolTable;
            propertyVisitor.addProperty("id", idVisitor);
            propertyVisitor.addProperty("class", classVisitor);
            propertyVisitor.addProperty("data", dataVisitor);
        }

        @Override
        public JsonVisitor object(JsonErrorReporter reporter) {
            id = 0;
            classId = 0;
            data = null;
            return propertyVisitor;
        }

        @Override
        public void end(JsonErrorReporter reporter) {
            try {
                ClassDescriptor cls = symbolTable.getClassById(classId);
                if (cls == null) {
                    reporter.error("Unknown class: " + classId);
                }
                if (cls.itemClassId == 0) {
                    output.write(0x21);
                    writeId(output, id);
                    output.writeInt(1);
                    writeId(output, classId);
                    output.writeInt(data.length);
                    int dataPtr = data.length;
                    while (cls != null) {
                        for (FieldDescriptor field : cls.fields) {
                            dataPtr -= typeSize(field.type);
                        }
                        int ptr = dataPtr;
                        for (FieldDescriptor field : cls.fields) {
                            int size = typeSize(field.type);
                            output.write(data, ptr, size);
                            ptr += size;
                        }
                        cls = cls.superClassId != 0 ? symbolTable.getClassById(cls.superClassId) : null;
                    }
                } else {
                    ClassDescriptor itemCls = symbolTable.getClassById(cls.itemClassId);
                    output.write(itemCls.primitiveType == null ? 0x22 : 0x23);
                    writeId(output, id);
                    output.writeInt(1);
                    int itemSize = itemCls.primitiveType != null ? typeSize(itemCls.primitiveType) : idSize;
                    int size = data.length / itemSize;
                    output.writeInt(size);
                    if (itemCls.primitiveType == null) {
                        writeId(output, classId);
                    } else {
                        output.write(typeToInt(itemCls.primitiveType));
                    }
                    for (int i = 0; i < size; ++i) {
                        int ptr = i * itemSize;
                        output.write(data, ptr, itemSize);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private JsonVisitor idVisitor = new JsonAllErrorVisitor() {
            @Override
            public void intValue(JsonErrorReporter reporter, long value) {
                id = value;
            }
        };

        private JsonVisitor classVisitor = new JsonAllErrorVisitor() {
            @Override
            public void intValue(JsonErrorReporter reporter, long value) {
                classId = value;
            }
        };

        private JsonVisitor dataVisitor = new JsonAllErrorVisitor() {
            @Override
            public void stringValue(JsonErrorReporter reporter, String value) {
                data = parseData(reporter, value);
            }
        };
    }

    static class SymbolTableClassVisitor extends JsonAllErrorVisitor {
        SymbolTable symbolTable;
        ClassDescriptor descriptor;
        FieldDescriptor fieldDescriptor;
        private JsonPropertyVisitor propertyVisitor;
        private List<FieldDescriptor> fields;

        SymbolTableClassVisitor(SymbolTable symbolTable) {
            this.symbolTable = symbolTable;
            propertyVisitor = new JsonPropertyVisitor(true);
            propertyVisitor.addProperty("id", idVisitor);
            propertyVisitor.addProperty("name", nameVisitor);
            propertyVisitor.addProperty("super", superVisitor);
            propertyVisitor.addProperty("size", sizeVisitor);
            propertyVisitor.addProperty("primitive", primitiveVisitor);
            propertyVisitor.addProperty("item", itemVisitor);
            propertyVisitor.addProperty("fields", fieldsVisitor);
            propertyVisitor.addProperty("staticFields", staticFieldsVisitor);
            propertyVisitor.addProperty("data", dataVisitor);
        }

        @Override
        public JsonVisitor object(JsonErrorReporter reporter) {
            descriptor = new ClassDescriptor();
            return propertyVisitor;
        }

        @Override
        public void end(JsonErrorReporter reporter) {
            if (descriptor.id == 0) {
                reporter.error("Required property 'id' was not set");
            }
            symbolTable.classList.add(descriptor);
            if (symbolTable.classesById.put(descriptor.id, descriptor) != null) {
                reporter.error("Duplicate class id: " + descriptor.id);
            }
            if (descriptor.name != null) {
                switch (descriptor.name) {
                    case "java.lang.Object":
                        symbolTable.objectClassId = descriptor.id;
                        break;
                    case "java.lang.ClassLoader":
                        symbolTable.classLoaderClassId = descriptor.id;
                        break;
                    case "java.lang.ref.Reference":
                        symbolTable.referenceClassId = descriptor.id;
                        break;
                    case "java.lang.ref.WeakReference":
                        symbolTable.weakReferenceClassId = descriptor.id;
                        break;
                    case "java.lang.ref.SoftReference":
                        symbolTable.softReferenceClassId = descriptor.id;
                        break;
                    case "java.lang.ref.PhantomReference":
                        symbolTable.phantomReferenceClassId = descriptor.id;
                        break;
                    case "java.lang.ref.FinalReference":
                        symbolTable.finalReferenceClassId = descriptor.id;
                        break;
                }
            }
        }

        JsonVisitor idVisitor = new JsonAllErrorVisitor() {
            @Override
            public void intValue(JsonErrorReporter reporter, long value) {
                descriptor.id = value;
            }
        };

        JsonVisitor nameVisitor = new JsonAllErrorVisitor() {
            @Override
            public void stringValue(JsonErrorReporter reporter, String value) {
                descriptor.name = value;
            }
        };

        JsonVisitor superVisitor = new JsonAllErrorVisitor() {
            @Override
            public void intValue(JsonErrorReporter reporter, long value) {
                descriptor.superClassId = value;
            }

            @Override
            public void nullValue(JsonErrorReporter reporter) {
            }
        };

        JsonVisitor sizeVisitor = new JsonAllErrorVisitor() {
            @Override
            public void intValue(JsonErrorReporter reporter, long value) {
                descriptor.size = (int) value;
            }
        };

        JsonVisitor primitiveVisitor = new JsonAllErrorVisitor() {
            @Override
            public void stringValue(JsonErrorReporter reporter, String value) {
                descriptor.primitiveType = parseType(reporter, value);
            }
        };

        JsonVisitor itemVisitor = new JsonAllErrorVisitor() {
            @Override
            public void intValue(JsonErrorReporter reporter, long value) {
                descriptor.itemClassId = value;
            }
        };

        JsonVisitor fieldsVisitor = new JsonAllErrorVisitor() {
            @Override
            public JsonVisitor array(JsonErrorReporter reporter) {
                fields = descriptor.fields;
                return fieldVisitor;
            }
        };

        JsonVisitor staticFieldsVisitor = new JsonAllErrorVisitor() {
            @Override
            public JsonVisitor array(JsonErrorReporter reporter) {
                fields = descriptor.staticFields;
                return fieldVisitor;
            }
        };

        JsonVisitor fieldNameVisitor = new JsonAllErrorVisitor() {
            @Override
            public void stringValue(JsonErrorReporter reporter, String value) {
                fieldDescriptor.name = value;
                symbolTable.lookup(value);
            }
        };

        JsonVisitor fieldTypeVisitor = new JsonAllErrorVisitor() {
            @Override
            public void stringValue(JsonErrorReporter reporter, String value) {
                fieldDescriptor.type = parseType(reporter, value);
            }
        };

        JsonVisitor fieldVisitor = new JsonAllErrorVisitor() {
            private JsonPropertyVisitor propertyVisitor = new JsonPropertyVisitor(true);

            {
                propertyVisitor.addProperty("name", fieldNameVisitor);
                propertyVisitor.addProperty("type", fieldTypeVisitor);
            }

            @Override
            public JsonVisitor object(JsonErrorReporter reporter) {
                fieldDescriptor = new FieldDescriptor();
                return propertyVisitor;
            }

            @Override
            public void end(JsonErrorReporter reporter) {
                if (fieldDescriptor.type == null) {
                    reporter.error("Type for this field not specified");
                }
                fields.add(fieldDescriptor);
                fieldDescriptor = null;
            }
        };

        JsonVisitor dataVisitor = new JsonAllErrorVisitor() {
            @Override
            public void stringValue(JsonErrorReporter reporter, String value) {
                descriptor.data = parseData(reporter, value);
            }
        };
    }

    private static Type parseType(JsonErrorReporter errorReporter, String type) {
        switch (type) {
            case "object":
                return Type.OBJECT;
            case "array":
                return Type.ARRAY;
            case "boolean":
                return Type.BOOLEAN;
            case "byte":
                return Type.BYTE;
            case "char":
                return Type.CHAR;
            case "short":
                return Type.SHORT;
            case "int":
                return Type.INT;
            case "long":
                return Type.LONG;
            case "float":
                return Type.FLOAT;
            case "double":
                return Type.DOUBLE;
            default:
                errorReporter.error("Unknown type: " + type);
                return Type.OBJECT;
        }
    }

    static class SymbolTableStackVisitor extends JsonAllErrorVisitor {
        SymbolTable symbolTable;
        private JsonPropertyVisitor propertyVisitor = new JsonPropertyVisitor(true);
        private Frame frame;
        private LongArrayList roots = new LongArrayList();

        SymbolTableStackVisitor(SymbolTable symbolTable) {
            this.symbolTable = symbolTable;
            propertyVisitor.addProperty("file", fileVisitor);
            propertyVisitor.addProperty("class", classVisitor);
            propertyVisitor.addProperty("method", methodVisitor);
            propertyVisitor.addProperty("line", lineVisitor);
            propertyVisitor.addProperty("roots", new JsonArrayVisitor(rootsVisitor));
        }

        @Override
        public JsonVisitor object(JsonErrorReporter reporter) {
            frame = new Frame();
            return propertyVisitor;
        }

        @Override
        public void end(JsonErrorReporter reporter) {
            if (!roots.isEmpty()) {
                frame.roots = roots.toArray();
                roots.clear();
            }
            symbolTable.stack.add(frame);
        }

        JsonVisitor fileVisitor = new JsonAllErrorVisitor() {
            @Override
            public void stringValue(JsonErrorReporter reporter, String value) {
                symbolTable.lookup(value);
                frame.fileName = value;
            }
        };

        JsonVisitor classVisitor = new JsonAllErrorVisitor() {
            @Override
            public void stringValue(JsonErrorReporter reporter, String value) {
                symbolTable.lookup(value);
                frame.className = value;
            }
        };

        JsonVisitor methodVisitor = new JsonAllErrorVisitor() {
            @Override
            public void stringValue(JsonErrorReporter reporter, String value) {
                symbolTable.lookup(value);
                frame.methodName = value;
            }
        };

        JsonVisitor lineVisitor = new JsonAllErrorVisitor() {
            @Override
            public void intValue(JsonErrorReporter reporter, long value) {
                frame.lineNumber = (int) value;
            }
        };

        JsonVisitor rootsVisitor = new JsonAllErrorVisitor() {
            @Override
            public void intValue(JsonErrorReporter reporter, long value) {
                roots.add(value);
            }
        };
    }

    static class Frame {
        String className;
        String methodName;
        String fileName;
        int lineNumber = -1;
        long[] roots;
    }

    static class ClassDescriptor {
        long id;
        String name;
        int serialId;
        Type primitiveType;
        long itemClassId;
        long superClassId;
        int size;
        List<FieldDescriptor> fields = new ArrayList<>();
        List<FieldDescriptor> staticFields = new ArrayList<>();
        byte[] data;
    }

    static class FieldDescriptor {
        String name;
        Type type;
    }

    enum Type {
        OBJECT,
        ARRAY,
        BOOLEAN,
        BYTE,
        CHAR,
        SHORT,
        INT,
        LONG,
        FLOAT,
        DOUBLE
    }

    private static int typeToInt(Type type) {
        switch (type) {
            case OBJECT:
            case ARRAY:
                return 2;
            case BOOLEAN:
                return 4;
            case CHAR:
                return 5;
            case FLOAT:
                return 6;
            case DOUBLE:
                return 7;
            case BYTE:
                return 8;
            case SHORT:
                return 9;
            case INT:
                return 10;
            case LONG:
                return 11;
            default:
                return 0;
        }
    }

    private static int typeSize(Type type) {
        switch (type) {
            case OBJECT:
            case ARRAY:
                return idSize;
            case BYTE:
            case BOOLEAN:
                return 1;
            case CHAR:
            case SHORT:
                return 2;
            case INT:
            case FLOAT:
                return 4;
            case LONG:
            case DOUBLE:
                return 8;
            default:
                return 0;
        }
    }

    static class SymbolTable {
        private List<String> strings = new ArrayList<>();
        private ObjectIntMap<String> stringIndexes = new ObjectIntHashMap<>();
        List<ClassDescriptor> classList = new ArrayList<>();
        Map<String, ClassDescriptor> classes = new LinkedHashMap<>();
        LongObjectMap<ClassDescriptor> classesById = new LongObjectHashMap<>();
        List<Frame> stack = new ArrayList<>();
        long objectClassId;
        long classLoaderClassId;
        long referenceClassId;
        long weakReferenceClassId;
        long softReferenceClassId;
        long finalReferenceClassId;
        long phantomReferenceClassId;

        List<String> getStrings() {
            return strings;
        }

        ClassDescriptor getClass(String name) {
            return classes.get(name);
        }

        ClassDescriptor getClassById(long id) {
            return classesById.get(id);
        }

        Collection<ClassDescriptor> getClasses() {
            return classList;
        }

        int lookup(String str) {
            int value = stringIndexes.getOrDefault(str, -1);
            if (value < 0) {
                value = strings.size() + 1;
                strings.add(str);
                stringIndexes.put(str, value);
            }
            return value;
        }
    }

    private static void writeId(BufferedFile out, long id) throws IOException {
        writeLongBytes(out, id, idSize);
    }

    private static void writeLongBytes(BufferedFile out, long v, int size) throws IOException {
        for (int i = size - 1; i >= 0; --i) {
            buffer[i] = (byte) (v & 255);
            v >>>= 8;
        }

        out.write(buffer, 0, size);
    }

    static byte[] parseData(JsonErrorReporter errorReporter, String data) {
        if (data.length() % 2 != 0) {
            errorReporter.error("Invalid hex sequence");
            return new byte[0];
        }
        byte[] bytes = new byte[data.length() / 2];
        int j = 0;
        for (int i = 0; i < data.length(); i += 2) {
            int b = (hexDigit(errorReporter, data.charAt(i)) << 4) | hexDigit(errorReporter, data.charAt(i + 1));
            bytes[j++] = (byte) b;
        }
        return bytes;
    }

    private static int hexDigit(JsonErrorReporter errorReporter, char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        } else if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        } else if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        } else {
            errorReporter.error("Invalid hex sequence");
            return -1;
        }
    }
}

