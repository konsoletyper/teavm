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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationContainerReader;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.ReferenceCache;
import org.teavm.model.ValueType;

public class ClassIO {
    private static AccessLevel[] accessLevels = AccessLevel.values();
    private static ElementModifier[] elementModifiers = ElementModifier.values();
    private ReferenceCache referenceCache;
    private SymbolTable symbolTable;
    private ProgramIO programIO;
    private AnnotationIO annotationIO;

    public ClassIO(ReferenceCache referenceCache, SymbolTable symbolTable, SymbolTable fileTable,
            SymbolTable varTable) {
        this.referenceCache = referenceCache;
        this.symbolTable = symbolTable;
        programIO = new ProgramIO(referenceCache, symbolTable, fileTable, varTable);
        annotationIO = new AnnotationIO(referenceCache, symbolTable);
    }

    public void writeClass(OutputStream stream, ClassReader cls) throws IOException {
        VarDataOutput output = new VarDataOutput(stream);
        output.writeUnsigned(cls.getLevel().ordinal());
        output.writeUnsigned(packModifiers(cls.readModifiers()));
        output.writeUnsigned(cls.getParent() != null ? symbolTable.lookup(cls.getParent()) + 1 : 0);
        output.writeUnsigned(cls.getOwnerName() != null ? symbolTable.lookup(cls.getOwnerName()) + 1 : 0);
        output.writeUnsigned(cls.getDeclaringClassName() != null
                ? symbolTable.lookup(cls.getDeclaringClassName()) + 1 : 0);
        output.writeUnsigned(cls.getSimpleName() != null ? symbolTable.lookup(cls.getSimpleName()) + 1 : 0);
        output.writeUnsigned(cls.getInterfaces().size());
        for (String iface : cls.getInterfaces()) {
            output.writeUnsigned(symbolTable.lookup(iface));
        }
        annotationIO.writeAnnotations(output, cls.getAnnotations());
        output.writeUnsigned(cls.getFields().size());
        for (FieldReader field : cls.getFields()) {
            writeField(output, field);
        }
        output.writeUnsigned(cls.getMethods().size());
        for (MethodReader method : cls.getMethods()) {
            writeMethod(output, method);
        }
    }

    public ClassReader readClass(InputStream stream, String name) throws IOException {
        VarDataInput input = new VarDataInput(stream);
        CachedClassReader cls = new CachedClassReader();
        cls.name = name;
        cls.level = accessLevels[input.readUnsigned()];
        cls.modifiers = unpackModifiers(input.readUnsigned());
        int parentIndex = input.readUnsigned();
        cls.parent = parentIndex > 0 ? referenceCache.getCached(symbolTable.at(parentIndex - 1)) : null;
        int ownerIndex = input.readUnsigned();
        cls.owner = ownerIndex > 0 ? referenceCache.getCached(symbolTable.at(ownerIndex - 1)) : null;
        int declaringClassIndex = input.readUnsigned();
        cls.declaringClass = declaringClassIndex > 0
                ? referenceCache.getCached(symbolTable.at(declaringClassIndex - 1)) : null;
        int simpleNameIndex = input.readUnsigned();
        cls.simpleName = simpleNameIndex > 0 ? referenceCache.getCached(symbolTable.at(simpleNameIndex - 1)) : null;
        int ifaceCount = input.readUnsigned();
        Set<String> interfaces = new LinkedHashSet<>();
        for (int i = 0; i < ifaceCount; ++i) {
            interfaces.add(referenceCache.getCached(symbolTable.at(input.readUnsigned())));
        }
        cls.interfaces = Collections.unmodifiableSet(interfaces);
        cls.annotations = annotationIO.readAnnotations(input);

        Map<String, CachedField> fields = new LinkedHashMap<>();
        int fieldCount = input.readUnsigned();
        for (int i = 0; i < fieldCount; ++i) {
            CachedField field = readField(name, input);
            fields.put(field.name, field);
        }
        cls.fields = fields;

        Map<MethodDescriptor, CachedMethod> methods = new LinkedHashMap<>();
        int methodCount = input.readUnsigned();
        for (int i = 0; i < methodCount; ++i) {
            CachedMethod method = readMethod(cls.name, input);
            methods.put(method.reference.getDescriptor(), method);
        }
        cls.methods = methods;

        return cls;
    }

    private void writeField(VarDataOutput output, FieldReader field) throws IOException {
        output.writeUnsigned(symbolTable.lookup(field.getName()));
        output.writeUnsigned(symbolTable.lookup(field.getType().toString()));
        output.writeUnsigned(field.getLevel().ordinal());
        output.writeUnsigned(packModifiers(field.readModifiers()));
        writeFieldValue(output, field.getInitialValue());
        annotationIO.writeAnnotations(output, field.getAnnotations());
    }

    private CachedField readField(String className, VarDataInput input) throws IOException {
        CachedField field = new CachedField();
        field.name = referenceCache.getCached(symbolTable.at(input.readUnsigned()));
        field.type = referenceCache.getCached(ValueType.parse(symbolTable.at(input.readUnsigned())));
        field.level = accessLevels[input.readUnsigned()];
        field.modifiers = unpackModifiers(input.readUnsigned());
        field.initialValue = readFieldValue(input);
        field.annotations = annotationIO.readAnnotations(input);
        field.ownerName = className;
        field.reference = referenceCache.getCached(new FieldReference(className, field.name));
        return field;
    }

    private void writeFieldValue(VarDataOutput output, Object value) throws IOException {
        if (value == null) {
            output.writeUnsigned(0);
        } else if (value instanceof Integer) {
            output.writeUnsigned(1);
            output.writeSigned((Integer) value);
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

    private void writeMethod(VarDataOutput output, MethodReader method) throws IOException {
        output.writeUnsigned(symbolTable.lookup(method.getDescriptor().toString()));
        output.writeUnsigned(method.getLevel().ordinal());
        output.writeUnsigned(packModifiers(method.readModifiers()));
        annotationIO.writeAnnotations(output, method.getAnnotations());

        for (AnnotationContainerReader parameterAnnotation : method.getParameterAnnotations()) {
            annotationIO.writeAnnotations(output, parameterAnnotation);
        }

        if (method.getAnnotationDefault() != null) {
            output.writeUnsigned(1);
            annotationIO.writeAnnotationValue(output, method.getAnnotationDefault());
        } else {
            output.writeUnsigned(0);
        }

        if (method.getProgram() != null) {
            output.writeUnsigned(1);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            VarDataOutput programOutput = new VarDataOutput(buffer);
            programIO.write(method.getProgram(), programOutput);
            output.writeBytes(buffer.toByteArray());
        } else {
            output.writeUnsigned(0);
        }
    }

    private CachedMethod readMethod(String className, VarDataInput input) throws IOException {
        CachedMethod method = new CachedMethod();
        MethodDescriptor descriptor = referenceCache.getCached(
                MethodDescriptor.parse(symbolTable.at(input.readUnsigned())));
        method.reference = referenceCache.getCached(className, descriptor);
        method.level = accessLevels[input.readUnsigned()];
        method.modifiers = unpackModifiers(input.readUnsigned());
        method.annotations = annotationIO.readAnnotations(input);
        method.ownerName = className;
        method.name = descriptor.getName();

        method.parameterAnnotations = new CachedAnnotations[descriptor.parameterCount()];
        for (int i = 0; i < method.parameterCount(); ++i) {
            method.parameterAnnotations[i] = annotationIO.readAnnotations(input);
        }

        if (input.readUnsigned() != 0) {
            method.annotationDefault = annotationIO.readAnnotationValue(input);
        }

        if (input.readUnsigned() != 0) {
            byte[] programData = input.readBytes();
            method.programSupplier = () -> {
                VarDataInput programInput = new VarDataInput(new ByteArrayInputStream(programData));
                try {
                    return programIO.read(programInput);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            };
        }
        return method;
    }


    private int packModifiers(Set<ElementModifier> modifiers) {
        int result = 0;
        for (ElementModifier modifier : modifiers) {
            result |= 1 << modifier.ordinal();
        }
        return result;
    }

    private EnumSet<ElementModifier> unpackModifiers(int packed) {
        EnumSet<ElementModifier> modifiers = EnumSet.noneOf(ElementModifier.class);
        while (packed != 0) {
            int n = Integer.numberOfTrailingZeros(packed);
            packed ^= 1 << n;
            modifiers.add(elementModifiers[n]);
        }
        return modifiers;
    }
}
