/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.backend.c.generators;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import org.teavm.backend.c.generate.CodeWriter;
import org.teavm.dependency.MethodDependencyInfo;
import org.teavm.dependency.ValueDependencyInfo;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.reflect.ClassInfo;

public class ArrayGenerator implements Generator {
    private static final int[] primitives = {
            ClassInfo.PrimitiveKind.BYTE,
            ClassInfo.PrimitiveKind.SHORT,
            ClassInfo.PrimitiveKind.CHAR,
            ClassInfo.PrimitiveKind.INT,
            ClassInfo.PrimitiveKind.LONG,
            ClassInfo.PrimitiveKind.FLOAT,
            ClassInfo.PrimitiveKind.DOUBLE,
            ClassInfo.PrimitiveKind.BOOLEAN
    };
    private static final String[] primitiveWrappers = { "Byte", "Short", "Character", "Integer", "Long",
            "Float", "Double", "Boolean" };
    private static final ValueType[] primitiveTypes = { ValueType.BYTE, ValueType.SHORT, ValueType.CHARACTER,
            ValueType.INTEGER, ValueType.LONG, ValueType.FLOAT, ValueType.DOUBLE, ValueType.BOOLEAN };

    @Override
    public boolean canHandle(MethodReference method) {
        if (!method.getClassName().equals(Array.class.getName())) {
            return false;
        }

        switch (method.getName()) {
            case "getImpl":
                return true;
            default:
                return false;
        }
    }

    @Override
    public void generate(GeneratorContext context, MethodReference method) {
        String array = context.parameterName(1);
        String index = context.parameterName(2);

        context.writer().println("TeaVM_Class* componentType = (TeaVM_Class*) TEAVM_CLASS_OF(" + array + ")"
                + "->itemType;");
        context.writer().println("int32_t flags = componentType->flags;");
        context.writer().println("if (((flags & " + RuntimeClass.PRIMITIVE_TYPE_MASK + ") >> "
                + RuntimeClass.PRIMITIVE_TYPE_SHIFT + ") != 0) {").indent();

        context.writer().println("switch ((flags >> " + RuntimeClass.PRIMITIVE_TYPE_SHIFT + ") & "
                + RuntimeClass.PRIMITIVE_TYPE_MASK + ") {").indent();
        MethodDependencyInfo dependency = context.dependencies().getMethod(new MethodReference(Array.class,
                "getImpl", Object.class, int.class, Object.class));
        ValueDependencyInfo arrayDependency = dependency.getVariable(1);
        var types = new HashSet<>(Arrays.asList(arrayDependency.getTypes()));
        for (int i = 0; i < primitiveWrappers.length; ++i) {
            var typeName = ValueType.arrayOf(primitiveTypes[i]);
            if (!types.contains(typeName)) {
                continue;
            }

            String wrapper = "java.lang." + primitiveWrappers[i];
            MethodReference methodRef = new MethodReference(wrapper, "valueOf",
                    primitiveTypes[i], ValueType.object(wrapper));

            String type = CodeWriter.strictTypeAsString(primitiveTypes[i]);
            context.writer().println("case " + primitives[i] + ":").indent();
            context.importMethod(methodRef, true);
            context.writer().println("return " + context.names().forMethod(methodRef) + "(TEAVM_ARRAY_AT(" + array
                    + ", " + type + ", " + index + "));");
            context.writer().outdent();
        }
        context.writer().outdent().println("}").outdent().println("}");

        context.writer().println("return TEAVM_ARRAY_AT(" + array + ", void*, " + index + ");");
    }
}
