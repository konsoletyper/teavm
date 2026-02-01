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
package org.teavm.backend.c.generate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.teavm.model.AccessLevel;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.reflection.AnnotationGenerationHelper;
import org.teavm.reflection.ReflectionDependencyListener;

class ClassReflectionGenerator {
    private GenerationContext context;
    private CodeWriter writer;
    private IncludeManager includes;
    private ReflectionDependencyListener reflection;
    private Collection<ValueType> types;

    ClassReflectionGenerator(GenerationContext context, ReflectionDependencyListener reflection,
            Collection<ValueType> types) {
        this.context = context;
        this.reflection = reflection;
        this.types = types;
    }

    void generate(CodeWriter writer, IncludeManager includes, ClassReader cls) {
        var annotations = extractAnnotations(cls);
        var fields = extractFields(cls);
        if (annotations.isEmpty() && fields.isEmpty()) {
            writer.print("NULL");
            return;
        }

        this.writer = writer;
        this.includes = includes;

        includes.includePath("reflection.h");
        writer.println("&(TeaVM_ClassReflection) {").indent();

        var needComma = false;
        if (!annotations.isEmpty()) {
            writer.print(".annotations = ");
            generateAnnotations(annotations);
            needComma = true;
        }
        if (!fields.isEmpty()) {
            if (needComma) {
                writer.println(",");
            }
            writer.print(".fields = ");
            generateFields(cls, fields);
        }

        writer.println();
        writer.outdent().print("}");

        this.writer = null;
        this.includes = null;
    }

    private void generateFields(ClassReader cls, List<FieldReader> fields) {
        writer.print("(TeaVM_FieldInfoList*) &(struct { int32_t count; TeaVM_FieldInfo data[")
                .print(String.valueOf(fields.size())).println("];}) {").indent();
        writer.print(".count = ").print(String.valueOf(fields.size())).println(",");
        writer.print(".data = ").println("{").indent();
        generateField(cls, fields.get(0));
        for (var i = 1; i < fields.size(); ++i) {
            writer.println(",");
            generateField(cls, fields.get(i));
        }
        writer.println().outdent().println("}");
        writer.outdent().print("}");
    }

    private void generateField(ClassReader cls, FieldReader field) {
        includes.addInclude("<stddef.h>");

        var names = context.getNames();
        writer.println("{").indent();

        var nameIndex = context.getStringPool().getStringIndex(field.getName());
        writer.print(".name = TEAVM_GET_STRING_ADDRESS(").print(String.valueOf(nameIndex)).println("),");

        var modifiers = ElementModifier.asModifiersInfo(field.readModifiers(), field.getLevel());
        writer.print(".modifiers = ").print(String.valueOf(modifiers)).println(",");

        writer.print(".type = ");
        writeType(field.getType());
        writer.println(",");

        writer.print(".location = { ");
        if (field.hasModifier(ElementModifier.STATIC)) {
            writer.print(".memory = &")
                    .print(names.forStaticField(field.getReference()));
        } else {
            writer.print(".instance = (int16_t) offsetof(")
                    .print(names.forClass(cls.getName())).print(", ")
                    .print(names.forMemberField(field.getReference()))
                    .print(")");
        }
        writer.println(" }");
        writer.outdent().print("}");
    }

    private void generateAnnotations(List<AnnotationReader> annotations) {
        writer.println("(TeaVM_AnnotationInfoList*) &(struct { int32_t count; "
                + "TeaVM_AnnotationInfo annotations[" + annotations.size() + "]; }) {").indent();
        writer.print(".count = ").print(String.valueOf(annotations.size())).println(",");
        writer.println(".annotations = {").indent();
        generateAnnotation(annotations.get(0));
        for (var i = 1; i < annotations.size(); ++i) {
            writer.println(",");
            generateAnnotation(annotations.get(i));
        }
        writer.println();
        writer.outdent().println("}");
        writer.outdent().print("}");
    }

    private void generateAnnotation(AnnotationReader annotation) {
        var implClass = annotation.getType() + AnnotationGenerationHelper.ANNOTATION_IMPLEMENTOR_SUFFIX;
        var dataClass = annotation.getType() + AnnotationGenerationHelper.ANNOTATION_DATA_SUFFIX;
        var ctor = new MethodReference(implClass, "create", ValueType.object(dataClass),
                ValueType.object(implClass));
        includes.includeClass(dataClass);
        includes.includeClass(ctor.getClassName());
        writer.println("{").indent();
        writer.print(".constructor = &").print(context.getNames().forMethod(ctor)).println(",");
        writer.print(".data = ");
        generateAnnotationData(annotation);
        writer.println();
        writer.outdent().print("}");
    }

    private void generateAnnotationData(AnnotationReader annotation) {
        var cls = context.getClassSource().get(annotation.getType());
        if (cls == null) {
            writer.print("NULL");
            return;
        }

        var dataClass = annotation.getType() + AnnotationGenerationHelper.ANNOTATION_DATA_SUFFIX;
        includes.includeClass(dataClass);
        writer.print("&(").print(context.getNames().forClass(dataClass)).println(") {").indent();

        var first = true;
        for (var method : cls.getMethods()) {
            if (!first) {
                writer.println(",");
            }
            first = false;
            var fieldName = context.getNames().forMemberField(new FieldReference(dataClass, method.getName()));
            writer.print(".").print(fieldName).print(" = ");
            var value = annotation.getValue(method.getName());
            if (value == null) {
                value = method.getAnnotationDefault();
            }
            generateAnnotationValue(method.getResultType(), value);
        }
        writer.outdent().print("}");
    }

    private void generateAnnotationValue(ValueType type, AnnotationValue value) {
        switch (value.getType()) {
            case AnnotationValue.BOOLEAN:
                writer.print(value.getBoolean() ? "1" : "0");
                break;
            case AnnotationValue.BYTE:
                writer.print("(int8_t) ");
                writer.print(String.valueOf(value.getByte()));
                break;
            case AnnotationValue.SHORT:
                writer.print("(int16_t) ");
                writer.print(String.valueOf(value.getShort()));
                break;
            case AnnotationValue.CHAR:
                writer.print("(uint16_t) ");
                CodeGeneratorUtil.writeIntValue(writer, value.getInt());
                break;
            case AnnotationValue.INT:
                CodeGeneratorUtil.writeIntValue(writer, value.getInt());
                break;
            case AnnotationValue.LONG:
                CodeGeneratorUtil.writeValue(writer, value.getLong());
                break;
            case AnnotationValue.FLOAT:
                CodeGeneratorUtil.writeValue(writer, value.getFloat());
                break;
            case AnnotationValue.DOUBLE:
                CodeGeneratorUtil.writeValue(writer, value.getDouble());
                break;
            case AnnotationValue.STRING:
                writer.print("(TeaVM_Object**) TEAVM_GET_STRING_ADDRESS("
                        + context.getStringPool().getStringIndex(value.getString()) + ")");
                break;
            case AnnotationValue.CLASS:
                writeType(value.getJavaClass());
                break;
            case AnnotationValue.ENUM: {
                var enumCls = context.getClassSource().get(value.getEnumValue().getClassName());
                if (enumCls == null) {
                    writer.print("NULL");
                    break;
                }
                var index = 0;
                for (var field : enumCls.getFields()) {
                    if (field.hasModifier(ElementModifier.ENUM)) {
                        if (field.getName().equals(value.getEnumValue().getFieldName())) {
                            break;
                        }
                        ++index;
                    }
                }
                writer.print(String.valueOf(index));
                break;
            }
            case AnnotationValue.ANNOTATION:
                generateAnnotationData(value.getAnnotation());
                break;
            case AnnotationValue.LIST: {
                var itemType = ((ValueType.Array) type).getItemType();
                var list = value.getList();
                writer.print("(");
                ClassGenerator.generateAnnotationFieldType(context, includes, writer, type);
                writer.print(") &(struct { int32_t count; ");
                ClassGenerator.generateAnnotationFieldType(context, includes, writer, itemType);
                writer.println(" data[" + list.size() + "]; }) {").indent();
                writer.print(".count = ").print(String.valueOf(list.size())).println(",");
                writer.print(".data = {").indent();
                if (!list.isEmpty()) {
                    writer.println();
                    generateAnnotationValue(itemType, list.get(0));
                    for (var i = 1; i < list.size(); ++i) {
                        writer.println(",");
                        generateAnnotationValue(itemType, list.get(i));
                    }
                    writer.println();
                }
                writer.outdent().println("}");
                writer.outdent().print("}");
                break;
            }
        }
    }

    private void writeType(ValueType cls) {
        var degree = 0;
        while (cls instanceof ValueType.Array) {
            cls = ((ValueType.Array) cls).getItemType();
            degree++;
        }
        includes.includeType(cls);
        types.add(cls);
        writer.print("{ .baseClass = (TeaVM_Class*) &").print(context.getNames().forClassInstance(cls))
                .print(", .arrayDegree = ").print(String.valueOf(degree)).print(" }");
    }

    private List<AnnotationReader> extractAnnotations(ClassReader cls) {
        if (!context.getMetadataRequirements().hasGetAnnotations()) {
            return List.of();
        }
        var info = context.getMetadataRequirements().getInfo(cls.getName());
        if (info == null || !info.annotations()) {
            return List.of();
        }
        return AnnotationGenerationHelper.collectRuntimeAnnotations(context.getClassSource(),
                cls.getAnnotations().all());
    }

    private List<FieldReader> extractFields(ClassReader cls) {
        var accessibleFields = reflection.getAccessibleFields(cls.getName());
        if (accessibleFields == null || accessibleFields.isEmpty()) {
            return List.of();
        }
        var skipPrivates = ReflectionDependencyListener.shouldSkipPrivates(cls);
        var fields = new ArrayList<FieldReader>();
        for (var field : cls.getFields()) {
            if (!accessibleFields.contains(field.getName())) {
                continue;
            }
            if (skipPrivates) {
                if (field.getLevel() == AccessLevel.PRIVATE || field.getLevel() == AccessLevel.PACKAGE_PRIVATE) {
                    continue;
                }
            }
            fields.add(field);
        }
        return fields;
    }
}
