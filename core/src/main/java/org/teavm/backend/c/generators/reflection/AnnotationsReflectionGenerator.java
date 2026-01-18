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
package org.teavm.backend.c.generators.reflection;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.teavm.backend.c.generate.CodeGeneratorUtil;
import org.teavm.backend.c.generate.GenerationContext;
import org.teavm.backend.c.generators.ReflectionGenerator;
import org.teavm.backend.c.generators.ReflectionGeneratorContext;
import org.teavm.backend.c.generators.ReflectionPartGenerator;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReference;
import org.teavm.model.ValueType;

public class AnnotationsReflectionGenerator implements ReflectionGenerator {
    @Override
    public boolean isApplicable(GenerationContext context, String className) {
        if (!context.getMetadataRequirements().hasGetAnnotations()) {
            return false;
        }
        if (!context.getMetadataRequirements().getInfo(className).annotations()) {
            return false;
        }
        var cls = context.getClassSource().get(className);
        return cls != null && !filterAnnotations(context, cls.getAnnotations().all()).isEmpty();
    }

    @Override
    public void generate(ReflectionGeneratorContext context, String className) {
        var cls = context.globalContext().getClassSource().get(className);
        var targetName = context.globalContext().getNames().forClassInstance(ValueType.object(className));
        generateAnnotations(targetName, context, cls.getAnnotations().all());
    }

    private void generateAnnotations(String targetName, ReflectionGeneratorContext context,
            Iterable<? extends AnnotationReader> annotations) {
        var annotationsToExpose = filterAnnotations(context.globalContext(), annotations);
        if (annotationsToExpose.isEmpty()) {
            return;
        }

        var writer = context.writer();
        writer.println(".annotationCount = " + annotationsToExpose.size() + ",");
        writer.print(".annotationsRef = (void*[]){").indent();
        var first = true;
        for (var annot : annotationsToExpose) {
            if (!first) {
                writer.print(",");
            }
            first = false;
            writer.println();
            var varName = generateAnnotation(targetName, context, annot);
            writer.print("&").print(varName);
        }
        if (!first) {
            writer.println();
        }
        writer.outdent().print("}");
    }

    private String generateAnnotation(String targetName, ReflectionGeneratorContext context,
            AnnotationReader annotation) {
        var names = context.globalContext().getNames();
        var includes = context.includes();
        var annotImpl = annotation.getType() + "$$_impl";
        includes.includeClass(annotImpl);
        includes.includePath("core.h");

        var annotVarName = names.createTopLevelName(targetName + "_annotation");
        var fieldGenerators = new ArrayList<ReflectionPartGenerator>();
        var annotCls = context.globalContext().getClassSource().get(annotation.getType());
        for (var method : annotCls.getMethods()) {
            var field = new FieldReference(annotImpl, "$" + method.getName());
            var fieldName = names.forMemberField(field);
            var value = annotation.getValue(method.getName());
            if (value == null) {
                value = method.getAnnotationDefault();
            }
            if (value.getType() == AnnotationValue.STRING) {
                var str = value.getString();
                context.addToInitializer(w -> {
                    w.print(annotVarName).print(".").print(fieldName).print(" = ");
                    CodeGeneratorUtil.writeValue(w, context.globalContext(), includes, str);
                    w.println(";");
                });
            } else {
                var valueGen = generateAnnotationValue(targetName, context, value, method.getResultType());
                fieldGenerators.add(w -> {
                    w.print(".").print(fieldName).print(" = ");
                    valueGen.generate(w);
                });
            }
        }
        context.addTopLevel(w -> {
            w.print("static ").print(names.forClass(annotImpl)).print(" ").print(annotVarName);
            if (!fieldGenerators.isEmpty()) {
                w.println(" = {").indent();
                fieldGenerators.get(0).generate(w);
                for (var i = 1; i < fieldGenerators.size(); i++) {
                    w.println(",");
                    fieldGenerators.get(i).generate(w);
                }
                w.println();
                w.outdent().print("}");
            }
            w.println(";");
        });
        context.addToInitializer(w -> {
            var instanceName = names.forClassInstance(ValueType.object(annotImpl));
            w.print(annotVarName).print(".parent.header = TEAVM_PACK_CLASS(&").print(instanceName).println(");");
        });

        return annotVarName;
    }

    private ReflectionPartGenerator generateAnnotationValue(String targetName, ReflectionGeneratorContext context,
            AnnotationValue value, ValueType type) {
        switch (value.getType()) {
            case AnnotationValue.BOOLEAN: {
                return w -> CodeGeneratorUtil.writeValue(w, context.globalContext(), context.includes(),
                        value.getBoolean());
            }
            case AnnotationValue.CHAR: {
                return w -> CodeGeneratorUtil.writeValue(w, context.globalContext(), context.includes(),
                        value.getChar());
            }
            case AnnotationValue.BYTE:
                return w -> CodeGeneratorUtil.writeValue(w, context.globalContext(), context.includes(),
                        (int) value.getByte());
            case AnnotationValue.SHORT:
                return w -> CodeGeneratorUtil.writeValue(w, context.globalContext(), context.includes(),
                        (int) value.getShort());
            case AnnotationValue.INT:
                return w -> CodeGeneratorUtil.writeValue(w, context.globalContext(), context.includes(),
                        value.getInt());
            case AnnotationValue.LONG:
                return w -> CodeGeneratorUtil.writeValue(w, context.globalContext(), context.includes(),
                        value.getLong());
            case AnnotationValue.FLOAT:
                return w -> CodeGeneratorUtil.writeValue(w, context.globalContext(), context.includes(),
                        value.getFloat());
            case AnnotationValue.DOUBLE:
                return w -> CodeGeneratorUtil.writeValue(w, context.globalContext(), context.includes(),
                        value.getDouble());
            case AnnotationValue.STRING: {
                return w -> w.print("NULL");
            }
            case AnnotationValue.LIST: {
                var varName = generateArray(targetName, context, (ValueType.Array) type, value.getList());
                return w -> w.print("(TeaVM_Array*) &").print(varName);
            }
            case AnnotationValue.ANNOTATION: {
                var varName = generateAnnotation(targetName, context, value.getAnnotation());
                return w -> w.print("&").print(varName);
            }
            case AnnotationValue.ENUM: {
                var enumCls = context.globalContext().getClassSource().get(value.getEnumValue().getClassName());
                if (enumCls != null) {
                    var index = 0;
                    for (var field : enumCls.getFields()) {
                        if (field.hasModifier(ElementModifier.STATIC) && field.hasModifier(ElementModifier.ENUM)) {
                            if (field.getName().equals(value.getEnumValue().getFieldName())) {
                                break;
                            }
                            ++index;
                        }
                    }
                    var result = String.valueOf(index);
                    return w -> w.print(result);
                }

                return w -> w.print("-1");
            }
            case AnnotationValue.CLASS: {
                return w -> CodeGeneratorUtil.writeTypeReference(w, context.globalContext(), context.includes(),
                        value.getJavaClass());
            }
            default:
                throw new IllegalArgumentException();
        }
    }

    private String generateArray(String targetName, ReflectionGeneratorContext context, ValueType.Array type,
            List<AnnotationValue> list) {
        var names = context.globalContext().getNames();
        var includes = context.includes();
        includes.includePath("core.h");

        var arrayVarName = names.createTopLevelName(targetName + "_annotationValue");
        var elementGenerators = new ArrayList<ReflectionPartGenerator>();

        for (var elem : list) {
            elementGenerators.add(generateAnnotationValue(targetName, context, elem, type.getItemType()));
        }
        context.addTopLevel(w -> {
            w.println("static struct {").indent();
            w.println("TeaVM_Array parent;");
            if (isEnum(context.globalContext(), type.getItemType())) {
                w.print("int32_t");
            } else {
                w.printType(type.getItemType());
            }
            w.print(" data[").print(Integer.toString(elementGenerators.size())).println("]");
            w.outdent().print("} ").print(arrayVarName).println(" = {").indent();
            w.print(".data = { ").indent();
            if (!elementGenerators.isEmpty()) {
                w.println();
                elementGenerators.get(0).generate(w);
                for (var i = 1; i < elementGenerators.size(); i++) {
                    w.println(",");
                    elementGenerators.get(i).generate(w);
                }
                w.println();
            }
            w.outdent().println("},");
            w.println(".parent = {").indent();
            w.println(".size = ").print(String.valueOf(list.size())).println();
            w.outdent().println("}");
            w.outdent().println("};");
        });
        context.addToInitializer(w -> {
            w.print(arrayVarName).print(".parent.parent.header = TEAVM_PACK_CLASS(");
            CodeGeneratorUtil.writeTypeReference(w, context.globalContext(), includes, type);
            w.println(");");
        });
        for (var i = 0; i < list.size(); ++i) {
            var elem = list.get(i);
            if (elem.getType() == AnnotationValue.STRING) {
                var index = i;
                context.addToInitializer(w -> {
                    w.print(arrayVarName).print(".data[").print(String.valueOf(index)).print("] = ");
                    CodeGeneratorUtil.writeValue(w, context.globalContext(), includes, elem.getString());
                    w.println(";");
                });
            }
        }

        return arrayVarName;
    }

    private boolean isEnum(GenerationContext context, ValueType type) {
        if (!(type instanceof ValueType.Object)) {
            return false;
        }
        var cls = context.getClassSource().get(((ValueType.Object) type).getClassName());
        if (cls == null) {
            return false;
        }
        return cls.hasModifier(ElementModifier.ENUM);
    }

    private List<AnnotationReader> filterAnnotations(GenerationContext context,
            Iterable<? extends AnnotationReader> annotations) {
        var annotationsToExpose = new ArrayList<AnnotationReader>();
        for (var annotation : annotations) {
            var annotationCls = context.getClassSource().get(annotation.getType());
            if (annotationCls == null) {
                continue;
            }
            var retention = annotationCls.getAnnotations().get(Retention.class.getName());
            if (retention == null) {
                continue;
            }
            if (Objects.equals(retention.getValue("value").getEnumValue().getFieldName(), "RUNTIME")) {
                annotationsToExpose.add(annotation);
            }
        }
        return annotationsToExpose;
    }
}
