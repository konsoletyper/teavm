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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.model.AnnotationContainerReader;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.FieldReference;
import org.teavm.model.ReferenceCache;
import org.teavm.model.ValueType;

public class AnnotationIO {
    private ReferenceCache referenceCache;
    private SymbolTable symbolTable;

    public AnnotationIO(ReferenceCache referenceCache, SymbolTable symbolTable) {
        this.referenceCache = referenceCache;
        this.symbolTable = symbolTable;
    }

    public void writeAnnotations(VarDataOutput output, AnnotationContainerReader annotations) throws IOException {
        List<AnnotationReader> annotationList = new ArrayList<>();
        for (AnnotationReader annot : annotations.all()) {
            annotationList.add(annot);
        }
        output.writeUnsigned(annotationList.size());
        for (AnnotationReader annot : annotationList) {
            writeAnnotation(output, annot);
        }
    }

    public CachedAnnotations readAnnotations(VarDataInput input) throws IOException {
        Map<String, CachedAnnotation> annotations = new HashMap<>();
        int annotCount = input.readUnsigned();
        for (int i = 0; i < annotCount; ++i) {
            CachedAnnotation annot = readAnnotation(input);
            annotations.put(annot.type, annot);
        }
        return new CachedAnnotations(annotations);
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

    private CachedAnnotation readAnnotation(VarDataInput input) throws IOException {
        CachedAnnotation annotation = new CachedAnnotation();
        annotation.type = referenceCache.getCached(symbolTable.at(input.readUnsigned()));
        int valueCount = input.readUnsigned();
        Map<String, AnnotationValue> fields = new HashMap<>();
        for (int i = 0; i < valueCount; ++i) {
            String name = referenceCache.getCached(symbolTable.at(input.readUnsigned()));
            AnnotationValue value = readAnnotationValue(input);
            fields.put(name, value);
        }
        annotation.fields = fields;
        return annotation;
    }

    public void writeAnnotationValue(VarDataOutput output, AnnotationValue value) throws IOException {
        output.writeUnsigned(value.getType());
        switch (value.getType()) {
            case AnnotationValue.ANNOTATION:
                writeAnnotation(output, value.getAnnotation());
                break;
            case AnnotationValue.BOOLEAN:
                output.writeUnsigned(value.getBoolean() ? 1 : 0);
                break;
            case AnnotationValue.CHAR:
                output.writeSigned(value.getChar());
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

    public AnnotationValue readAnnotationValue(VarDataInput input) throws IOException {
        int type = input.readUnsigned();
        switch (type) {
            case AnnotationValue.ANNOTATION:
                return new AnnotationValue(readAnnotation(input));
            case AnnotationValue.BOOLEAN:
                return new AnnotationValue(input.readUnsigned() != 0);
            case AnnotationValue.CHAR:
                return new AnnotationValue((char) input.readUnsigned());
            case AnnotationValue.BYTE:
                return new AnnotationValue((byte) input.readSigned());
            case AnnotationValue.CLASS:
                return new AnnotationValue(referenceCache.getCached(ValueType.parse(
                        symbolTable.at(input.readUnsigned()))));
            case AnnotationValue.DOUBLE:
                return new AnnotationValue(input.readDouble());
            case AnnotationValue.ENUM: {
                String className = referenceCache.getCached(symbolTable.at(input.readUnsigned()));
                String fieldName = referenceCache.getCached(symbolTable.at(input.readUnsigned()));
                return new AnnotationValue(referenceCache.getCached(new FieldReference(className, fieldName)));
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

}
