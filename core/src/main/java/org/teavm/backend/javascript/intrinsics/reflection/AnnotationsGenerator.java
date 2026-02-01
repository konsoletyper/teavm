/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.backend.javascript.intrinsics.reflection;

import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.rendering.RenderingUtil;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.reflection.AnnotationGenerationHelper;

public class AnnotationsGenerator {
    private AnnotationsGenerator() {
    }

    public static void generate(SourceWriter writer, ClassReaderSource classes, AnnotationReader annot) {
        var implName = annot.getType() + AnnotationGenerationHelper.ANNOTATION_IMPLEMENTOR_SUFFIX;
        var dataName = annot.getType() + AnnotationGenerationHelper.ANNOTATION_DATA_SUFFIX;
        var ctor = new MethodReference(implName, "create", ValueType.object(dataName), ValueType.object(implName));
        writer.append('[').appendMethod(ctor);
        generateValues(writer, classes, annot, true);
        writer.append(']');
    }

    private static void generateValues(SourceWriter writer, ClassReaderSource classes, AnnotationReader annot,
            boolean needsComma) {
        var cls = classes.get(annot.getType());
        if (cls == null) {
            return;
        }
        for (var method : cls.getMethods()) {
            if (needsComma) {
                writer.append(',').ws();
            }
            needsComma = true;
            var value = annot.getValue(method.getName());
            if (value == null) {
                value = method.getAnnotationDefault();
            }
            generateValue(writer, classes, method.getResultType(), value);
        }
    }

    private static void generateValue(SourceWriter writer, ClassReaderSource classes, ValueType type,
            AnnotationValue value) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    writer.append(value.getBoolean() ? "1" : "0");
                    break;
                case BYTE:
                    writer.append(String.valueOf(value.getByte()));
                    break;
                case SHORT:
                    writer.append(String.valueOf(value.getShort()));
                    break;
                case CHARACTER:
                    writer.append(String.valueOf(value.getChar()));
                    break;
                case INTEGER:
                    writer.append(String.valueOf(value.getInt()));
                    break;
                case LONG:
                    RenderingUtil.appendLongConstant(writer, value.getLong());
                    break;
                case FLOAT:
                    writer.append(String.valueOf(value.getFloat()));
                    break;
                case DOUBLE:
                    writer.append(String.valueOf(value.getDouble()));
                    break;
            }
        } else if (type instanceof ValueType.Object) {
            var className = ((ValueType.Object) type).getClassName();
            switch (className) {
                case "java.lang.String":
                    writer.append("\"").append(RenderingUtil.escapeString(value.getString())).append("\"");
                    break;
                case "java.lang.Class":
                    RenderingUtil.typeToClsString(writer, value.getJavaClass());
                    break;
                default:
                    var cls = classes.get(className);
                    if (cls == null) {
                        writer.append("null");
                    } else if (cls.hasModifier(ElementModifier.ENUM)) {
                        var index = 0;
                        for (var field : cls.getFields()) {
                            if (field.hasModifier(ElementModifier.STATIC) && field.hasModifier(ElementModifier.ENUM)) {
                                if (field.getName().equals(value.getEnumValue().getFieldName())) {
                                    break;
                                }
                                ++index;
                            }
                        }
                        writer.append(String.valueOf(index));
                    } else {
                        writer.append('[');
                        generateValues(writer, classes, value.getAnnotation(), false);
                        writer.append(']');
                    }
                    break;
            }
        } else if (type instanceof ValueType.Array) {
            writer.append('[');
            var itemType = ((ValueType.Array) type).getItemType();
            var arrayValue = value.getList();
            var first = true;
            for (var item : arrayValue) {
                if (!first) {
                    writer.append(",").ws();
                }
                first = false;
                generateValue(writer, classes, itemType, item);
            }
            writer.append(']');
        }
    }
}
