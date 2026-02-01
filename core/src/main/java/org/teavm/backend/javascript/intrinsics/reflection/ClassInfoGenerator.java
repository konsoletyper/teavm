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

import java.lang.reflect.Array;
import java.util.Set;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.rendering.Precedence;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.model.AccessLevel;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.runtime.reflect.ClassInfo;

public class ClassInfoGenerator implements Injector, Generator {
    private boolean enumMetadataGenerated;

    @Override
    public void generate(InjectorContext context, MethodReference methodRef) {
        switch (methodRef.getName()) {
            case "classObject":
                context.getWriter().appendFunction("$rt_cls").append("(");
                context.writeExpr(context.getArgument(0), Precedence.min());
                context.getWriter().append(")");
                break;
            case "primitiveKind":
            case "modifiers":
            case "name":
            case "simpleName":
            case "itemType":
            case "parent":
            case "declaringClass":
            case "enclosingClass":
            case "reflection":
                context.writeExpr(context.getArgument(0));
                context.getWriter().append("[").appendFunction("$rt_meta").append("].").append(methodRef.getName());
                break;
            case "isSuperTypeOf":
                context.getWriter().appendFunction("$rt_isAssignable").append("(");
                context.writeExpr(context.getArgument(1), Precedence.min());
                context.getWriter().append(",").ws();
                context.writeExpr(context.getArgument(0), Precedence.min());
                context.getWriter().append(")");
                break;
            case "superinterfaceCount":
                context.writeExpr(context.getArgument(0));
                context.getWriter().append("[").appendFunction("$rt_meta").append("].superinterfaces.length");
                break;
            case "superinterface":
                context.writeExpr(context.getArgument(0));
                context.getWriter().append("[").appendFunction("$rt_meta").append("].superinterfaces[");
                context.writeExpr(context.getArgument(1));
                context.getWriter().append("]");
                break;
            case "initialize":
                context.writeExpr(context.getArgument(0));
                context.getWriter().append("[").appendFunction("$rt_meta").append("].clinit()");
                break;
            case "newInstance":
                context.getWriter().append("new ");
                context.writeExpr(context.getArgument(0), Precedence.MEMBER_ACCESS);
                break;
            case "initializeNewInstance":
                context.getWriter().appendFunction("$rt_callDefaultConstructor").append("(");
                context.writeExpr(context.getArgument(0), Precedence.min());
                context.getWriter().append(",").ws();
                context.writeExpr(context.getArgument(1), Precedence.min());
                context.getWriter().append(")");
                writeSimpleConstructors(context);
                break;
            case "arrayType":
                context.getWriter().appendFunction("$rt_arraycls").append("(");
                context.writeExpr(context.getArgument(0), Precedence.min());
                context.getWriter().append(")");
                break;
            case "enumConstant":
                writeEnumMetadata(context);
                context.getWriter().appendFunction("$rt_enumConstants").append("(");
                context.writeExpr(context.getArgument(0), Precedence.min());
                context.getWriter().append(")[");
                context.writeExpr(context.getArgument(1), Precedence.min());
                context.getWriter().append("]");
                break;
            case "enumConstantCount":
                writeEnumMetadata(context);
                context.getWriter().appendFunction("$rt_enumConstants").append("(");
                context.writeExpr(context.getArgument(0), Precedence.min());
                context.getWriter().append(").length");
                break;
            case "arrayLength":
                context.getWriter().appendFunction("$rt_arrayLength").append("(");
                context.writeExpr(context.getArgument(1), Precedence.min());
                context.getWriter().append(")");
                break;
            case "getItem":
                context.getWriter().appendFunction("$rt_arrayGet").append("(");
                context.writeExpr(context.getArgument(0), Precedence.min());
                context.getWriter().append(",").ws();
                context.writeExpr(context.getArgument(1), Precedence.min());
                context.getWriter().append(",").ws();
                context.writeExpr(context.getArgument(2), Precedence.min());
                context.getWriter().append(")");
                break;
            case "putItem":
                context.getWriter().appendFunction("$rt_arrayPut").append("(");
                context.writeExpr(context.getArgument(0), Precedence.min());
                context.getWriter().append(",").ws();
                context.writeExpr(context.getArgument(1), Precedence.min());
                context.getWriter().append(",").ws();
                context.writeExpr(context.getArgument(2), Precedence.min());
                context.getWriter().append(",").ws();
                context.writeExpr(context.getArgument(3), Precedence.min());
                context.getWriter().append(")");
                break;
            case "rewind":
                context.getWriter().appendFunction("$rt_allClassesRewind");
                context.getWriter().append("()");
                break;
            case "hasNext":
                context.getWriter().appendFunction("$rt_allClassesHasNext");
                context.getWriter().append("()");
                break;
            case "next":
                context.getWriter().appendFunction("$rt_allClassesNext");
                context.getWriter().append("()");
                break;
        }
    }

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) {
        switch (methodRef.getName()) {
            case "newArrayInstance":
                generateNewArrayInstance(context, writer);
                break;
        }
    }

    private void generateNewArrayInstance(GeneratorContext context, SourceWriter writer) {
        var dependency = context.getDependency().getMethod(new MethodReference(Array.class, "newInstance", Class.class,
                int.class, Object.class));
        writer.append("switch").ws().append("(").append(context.getParameterName(0)).append(".primitiveKind)")
                .appendBlockStart();
        var length = context.getParameterName(1);
        var types = Set.of(dependency.getResult().getTypes());
        writeArrayClause(types, ValueType.BOOLEAN, writer, ClassInfo.PrimitiveKind.BOOLEAN,
                "$rt_createBooleanArray", length);
        writeArrayClause(types, ValueType.BYTE, writer, ClassInfo.PrimitiveKind.BYTE,
                "$rt_createByteArray", length);
        writeArrayClause(types, ValueType.SHORT, writer, ClassInfo.PrimitiveKind.SHORT,
                "$rt_createShortArray", length);
        writeArrayClause(types, ValueType.CHARACTER, writer, ClassInfo.PrimitiveKind.CHAR,
                "$rt_createCharArray", length);
        writeArrayClause(types, ValueType.INTEGER, writer, ClassInfo.PrimitiveKind.INT,
                "$rt_createIntArray", length);
        writeArrayClause(types, ValueType.LONG, writer, ClassInfo.PrimitiveKind.LONG,
                "$rt_createLongArray", length);
        writeArrayClause(types, ValueType.FLOAT, writer, ClassInfo.PrimitiveKind.FLOAT,
                "$rt_createFloatArray", length);
        writeArrayClause(types, ValueType.DOUBLE, writer, ClassInfo.PrimitiveKind.DOUBLE,
                "$rt_createDoubleArray", length);
        writer.append("default:").ws().append("return ").appendFunction("$rt_createArray").append("(")
                .append(context.getParameterName(0)).append(",").ws().append(length).append(");").softNewLine();
        writer.appendBlockEnd();
    }

    private void writeArrayClause(Set<ValueType> types, ValueType type, SourceWriter writer, int kind,
            String functionName, String length) {
        if (!types.contains(ValueType.arrayOf(type))) {
            return;
        }
        writer.append("case ").append(kind).append(":").ws();
        writer.append("return ").append(functionName).append("(").append(length).append(");").softNewLine();
    }

    private void writeEnumMetadata(InjectorContext context) {
        if (enumMetadataGenerated) {
            return;
        }
        var writer = context.getMetadataWriter();
        enumMetadataGenerated = true;
        writer.appendFunction("$rt_enumConstantsMetadata").append("([").softNewLine().indent();
        var first = true;
        for (var clsName : context.getClassSource().getClassNames()) {
            var cls = context.getClassSource().get(clsName);
            if (!cls.hasModifier(ElementModifier.ENUM)) {
                continue;
            }
            if (!first) {
                writer.append(",").ws();
            }
            first = false;
            writer.appendClass(clsName).append(",").ws().append("()").sameLineWs().append("=>")
                    .sameLineWs().append("[");
            var firstField = true;
            for (var field : cls.getFields()) {
                if (field.hasModifier(ElementModifier.STATIC) && field.hasModifier(ElementModifier.ENUM)) {
                    if (!firstField) {
                        writer.append(",").ws();
                    }
                    firstField = false;
                    writer.appendStaticField(field.getReference());
                }
            }
            writer.append("]");
        }
        writer.outdent().append("]);").newLine();
    }

    private void writeSimpleConstructors(InjectorContext context) {
        var methodDep = context.getDependencies()
                .getMethod(new MethodReference(Class.class, "newInstance", Object.class));
        if (methodDep == null) {
            return;
        }
        var writer = context.getMetadataWriter();
        writer.appendFunction("$rt_simpleConstructors").append("([");
        var first = true;
        for (var type : methodDep.getVariable(0).getClassValueNode().getTypes()) {
            if (type instanceof ValueType.Object) {
                var className = ((ValueType.Object) type).getClassName();
                var cls = context.getClassSource().get(className);
                var ctor = cls.getMethod(new MethodDescriptor("<init>", void.class));
                if (ctor != null && !ctor.hasModifier(ElementModifier.ABSTRACT)
                        && ctor.getLevel() == AccessLevel.PUBLIC) {
                    if (!first) {
                        writer.append(",").ws();
                    }
                    first = false;
                    writer.appendClass(cls.getName()).append(",").ws().appendMethod(ctor.getReference());
                }
            }
        }
        writer.append("]);").newLine();
    }
}
