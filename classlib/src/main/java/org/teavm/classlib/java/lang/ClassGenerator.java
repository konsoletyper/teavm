/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

import java.io.IOException;
import java.util.Set;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.rendering.RenderingUtil;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.classlib.impl.ReflectionDependencyListener;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.MemberReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class ClassGenerator implements Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "createMetadata":
                generateCreateMetadata(context, writer);
                break;
        }
    }

    private void generateCreateMetadata(GeneratorContext context, SourceWriter writer) throws IOException {
        ReflectionDependencyListener reflection = context.getService(ReflectionDependencyListener.class);
        for (String className : reflection.getClassesWithReflectableFields()) {
            generateCreateFieldsForClass(context, writer, className);
        }
        for (String className : reflection.getClassesWithReflectableMethods()) {
            generateCreateMethodsForClass(context, writer, className);
        }
    }

    private void generateCreateFieldsForClass(GeneratorContext context, SourceWriter writer, String className)
            throws IOException {
        ReflectionDependencyListener reflection = context.getService(ReflectionDependencyListener.class);
        Set<String> accessibleFields = reflection.getAccessibleFields(className);

        ClassReader cls = context.getClassSource().get(className);
        writer.appendClass(className).append(".$meta.fields").ws().append('=').ws().append('[').indent();

        generateCreateMembers(writer, cls.getFields(), field -> {
            appendProperty(writer, "type", false, () -> writer.append(context.typeToClassString(field.getType())));

            appendProperty(writer, "getter", false, () -> {
                if (accessibleFields != null && accessibleFields.contains(field.getName())) {
                    renderGetter(writer, field);
                } else {
                    writer.append("null");
                }
            });

            appendProperty(writer, "setter", false, () -> {
                if (accessibleFields != null && accessibleFields.contains(field.getName())) {
                    renderSetter(writer, field);
                } else {
                    writer.append("null");
                }
            });
        });

        writer.outdent().append("];").softNewLine();
    }

    private void generateCreateMethodsForClass(GeneratorContext context, SourceWriter writer, String className)
            throws IOException {
        ReflectionDependencyListener reflection = context.getService(ReflectionDependencyListener.class);

        ClassReader cls = context.getClassSource().get(className);
        writer.appendClass(className).append(".$meta.methods").ws().append('=').ws().append('[').indent();

        writer.outdent().append("];").softNewLine();
    }

    private <T extends MemberReader> void generateCreateMembers(SourceWriter writer, Iterable<T> members,
            MemberRenderer<T> renderer) throws IOException {
        boolean first = true;
        for (T member : members) {
            if (!first) {
                writer.append(",").ws();
            } else {
                writer.softNewLine();
            }
            first = false;
            writer.append("{").indent().softNewLine();

            appendProperty(writer, "name", true, () ->  writer.append('"')
                    .append(RenderingUtil.escapeString(member.getName())).append('"'));
            appendProperty(writer, "modifiers", false, () -> writer.append(packModifiers(member.readModifiers())));
            appendProperty(writer, "accessLevel", false, () -> writer.append(member.getLevel().ordinal()));
            renderer.render(member);
            writer.outdent().softNewLine().append("}");
        }

        writer.outdent().append("];").softNewLine();
    }

    private void appendProperty(SourceWriter writer, String name, boolean first, Fragment value) throws IOException {
        if (!first) {
            writer.append(",").softNewLine();
        }
        writer.append(name).ws().append(':').ws();
        value.render();
    }

    private void renderGetter(SourceWriter writer, FieldReader field) throws IOException {
        writer.append("function(obj)").ws().append("{").indent().softNewLine();
        initClass(writer, field);
        writer.append("return ");
        boxIfNecessary(writer, field.getType(), () -> fieldAccess(writer, field));
        writer.append(";").softNewLine();
        writer.outdent().append("}");
    }

    private void renderSetter(SourceWriter writer, FieldReader field) throws IOException {
        writer.append("function(obj,").ws().append("val)").ws().append("{").indent().softNewLine();
        initClass(writer, field);
        fieldAccess(writer, field);
        writer.ws().append('=').ws();
        unboxIfNecessary(writer, field.getType(), () -> writer.append("val"));
        writer.append(";").softNewLine();
        writer.outdent().append("}");
    }

    private void initClass(SourceWriter writer, FieldReader field) throws IOException {
        if (field.hasModifier(ElementModifier.STATIC)) {
            writer.appendClass(field.getOwnerName()).append("_$callClinit();").softNewLine();
        }
    }

    private void fieldAccess(SourceWriter writer, FieldReader field) throws IOException {
        if (field.hasModifier(ElementModifier.STATIC)) {
            writer.appendStaticField(field.getReference());
        } else {
            writer.append("obj.").appendField(field.getReference());
        }
    }

    private void boxIfNecessary(SourceWriter writer, ValueType type, Fragment fragment) throws IOException {
        boolean boxed = false;
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    writer.appendMethodBody(new MethodReference(Boolean.class, "valueOf", boolean.class,
                            Boolean.class));
                    break;
                case BYTE:
                    writer.appendMethodBody(new MethodReference(Byte.class, "valueOf", byte.class, Byte.class));
                    break;
                case SHORT:
                    writer.appendMethodBody(new MethodReference(Short.class, "valueOf", short.class, Short.class));
                    break;
                case CHARACTER:
                    writer.appendMethodBody(new MethodReference(Character.class, "valueOf", char.class,
                            Character.class));
                    break;
                case INTEGER:
                    writer.appendMethodBody(new MethodReference(Integer.class, "valueOf", int.class, Integer.class));
                    break;
                case LONG:
                    writer.appendMethodBody(new MethodReference(Long.class, "valueOf", long.class, Long.class));
                    break;
                case FLOAT:
                    writer.appendMethodBody(new MethodReference(Float.class, "valueOf", float.class, Float.class));
                    break;
                case DOUBLE:
                    writer.appendMethodBody(new MethodReference(Double.class, "valueOf", double.class, Double.class));
                    break;
            }
            writer.append('(');
            boxed = true;
        }
        fragment.render();
        if (boxed) {
            writer.append(')');
        }
    }

    private void unboxIfNecessary(SourceWriter writer, ValueType type, Fragment fragment) throws IOException {
        boolean boxed = false;
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    writer.appendMethodBody(new MethodReference(Boolean.class, "booleanValue", boolean.class));
                    break;
                case BYTE:
                    writer.appendMethodBody(new MethodReference(Byte.class, "byteValue", byte.class));
                    break;
                case SHORT:
                    writer.appendMethodBody(new MethodReference(Short.class, "shortValue", short.class));
                    break;
                case CHARACTER:
                    writer.appendMethodBody(new MethodReference(Character.class, "charValue", char.class));
                    break;
                case INTEGER:
                    writer.appendMethodBody(new MethodReference(Integer.class, "intValue", int.class));
                    break;
                case LONG:
                    writer.appendMethodBody(new MethodReference(Long.class, "longValue", long.class));
                    break;
                case FLOAT:
                    writer.appendMethodBody(new MethodReference(Float.class, "floatValue", float.class));
                    break;
                case DOUBLE:
                    writer.appendMethodBody(new MethodReference(Double.class, "doubleValue", double.class));
                    break;
            }
            writer.append('(');
            boxed = true;
        }
        fragment.render();
        if (boxed) {
            writer.append(')');
        }
    }

    private interface Fragment {
        void render() throws IOException;
    }

    private interface MemberRenderer<T extends MemberReader> {
        void render(T member) throws IOException;
    }

    private int packModifiers(Set<ElementModifier> elementModifiers) {
        ElementModifier[] knownModifiers = ElementModifier.values();
        int value = 0;
        int bit = 1;
        for (int i = 0; i < knownModifiers.length; ++i) {
            ElementModifier modifier = knownModifiers[i];
            if (elementModifiers.contains(modifier)) {
                value |= bit;
            }
            bit <<= 1;
        }
        return value;
    }
}
