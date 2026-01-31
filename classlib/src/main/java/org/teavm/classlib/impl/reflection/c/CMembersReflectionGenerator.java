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
package org.teavm.classlib.impl.reflection.c;

import org.teavm.backend.c.generate.CodeWriter;
import org.teavm.backend.c.generate.GenerationContext;
import org.teavm.backend.c.generate.IncludeManager;
import org.teavm.backend.c.generators.ReflectionGenerator;
import org.teavm.backend.c.generators.ReflectionGeneratorContext;
import org.teavm.classlib.impl.ReflectionDependencyListener;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.ValueType;

public class CMembersReflectionGenerator implements ReflectionGenerator {
    private ReflectionDependencyListener reflection;

    public CMembersReflectionGenerator(ReflectionDependencyListener reflection) {
        this.reflection = reflection;
    }

    @Override
    public boolean isApplicable(GenerationContext context, String className) {
        if (!reflection.getClassesWithReflectableFields().contains(className)) {
            return false;
        }
        return !reflection.getAccessibleFields(className).isEmpty();
    }

    @Override
    public void generate(ReflectionGeneratorContext context, String className) {
        var writer = context.writer();
        context.includes().addInclude("<stdint.h>");
        context.includes().addInclude("<stdbool.h>");
        context.includes().includePath("reflection.h");
        var cls = context.globalContext().getClassSource().get(className);
        var fields = reflection.getAccessibleFields(className);
        writer.print(".fields = (TeaVM_FieldInfoList*) &(struct { int32_t count; TeaVM_FieldInfo data[")
                .print(String.valueOf(fields.size())).println("];}) {").indent();
        writer.print(".count = ").print(String.valueOf(fields.size())).println(",");
        writer.print(".data = ").println("{").indent();
        var iter = fields.iterator();
        generateField(context, cls, iter.next());
        while (iter.hasNext()) {
            writer.println(",");
            generateField(context, cls, iter.next());
        }
        writer.println().outdent().print("}");
        writer.println().outdent().print("}");
    }

    private void generateField(ReflectionGeneratorContext context, ClassReader cls, String fieldName) {
        context.includes().addInclude("<stddef.h>");

        var writer = context.writer();
        var names = context.globalContext().getNames();
        var field = cls.getField(fieldName);
        writer.println("{").indent();

        var nameIndex = context.globalContext().getStringPool().getStringIndex(fieldName);
        writer.print(".name = TEAVM_GET_STRING_ADDRESS(").print(String.valueOf(nameIndex)).println("),");

        var modifiers = ElementModifier.pack(field.readModifiers());
        writer.print(".modifiers = ").print(String.valueOf(modifiers)).println(",");

        writer.print(".accessLevel = ").print(String.valueOf(field.getLevel().ordinal())).println(",");

        writer.print(".type = ");
        writeType(writer, context.includes(), context.globalContext(), field.getType());
        writer.println(",");

        writer.println(".readerWriter = {").indent();
        writer.print(".location = { ");
        if (field.hasModifier(ElementModifier.STATIC)) {
            writer.print(".isStatic = true, .offset = { .memory = &")
                    .print(names.forStaticField(field.getReference()));
        } else {
            writer.print(".isStatic = false, .offset = { .instance = (int16_t) offsetof(")
                    .print(names.forClass(cls.getName())).print(", ")
                    .print(names.forMemberField(field.getReference()))
                    .print(")");
        }
        writer.print(" } },");
        if (field.hasModifier(ElementModifier.STATIC)) {
            if (cls.getMethod(new MethodDescriptor("<clinit>", ValueType.VOID)) != null
                    && context.globalContext().getClassInitializerInfo().isDynamicInitializer(cls.getName())) {
                writer.println();
                writer.print(".initializer = &").print(names.forClassInitializer(cls.getName())).print(",");
            }
        }
        writer.println();
        writer.print(".valueConv = ");
        writeValueConv(writer, field.getType());
        writer.println();
        writer.outdent().println("}");

        writer.outdent().print("}");
    }

    private void writeValueConv(CodeWriter writer, ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    writer.print("TEAVM_VALUE_CONV_BOOLEAN");
                    return;
                case BYTE:
                    writer.print("TEAVM_VALUE_CONV_BYTE");
                    return;
                case SHORT:
                    writer.print("TEAVM_VALUE_CONV_SHORT");
                    return;
                case CHARACTER:
                    writer.print("TEAVM_VALUE_CONV_CHAR");
                    return;
                case INTEGER:
                    writer.print("TEAVM_VALUE_CONV_INT");
                    return;
                case LONG:
                    writer.print("TEAVM_VALUE_CONV_LONG");
                    return;
                case FLOAT:
                    writer.print("TEAVM_VALUE_CONV_FLOAT");
                    return;
                case DOUBLE:
                    writer.print("TEAVM_VALUE_CONV_DOUBLE");
                    return;
            }
        }
        writer.print("TEAVM_VALUE_CONV_REF");
    }

    private void writeType(CodeWriter writer, IncludeManager includes, GenerationContext context, ValueType type) {
        var degree = 0;
        while (type instanceof ValueType.Array) {
            var item = ((ValueType.Array) type).getItemType();
            if (item instanceof ValueType.Primitive) {
                break;
            }
            degree++;
            type = item;
        }
        includes.includeType(type);
        context.addType(type);
        writer.print("{ .baseClass = (TeaVM_Class*) &").print(context.getNames().forClassInstance(type));
        if (degree > 0) {
            writer.print(", .arrayDegree = ").print(String.valueOf(degree));
        }
        writer.print(" }");
    }
}
