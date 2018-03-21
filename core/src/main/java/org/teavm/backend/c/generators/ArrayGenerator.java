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
import org.teavm.backend.c.generate.CodeWriter;
import org.teavm.model.ClassReader;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.runtime.RuntimeClass;

public class ArrayGenerator implements Generator {
    private static final int[] primitives = {
            RuntimeClass.BYTE_PRIMITIVE,
            RuntimeClass.SHORT_PRIMITIVE,
            RuntimeClass.CHAR_PRIMITIVE,
            RuntimeClass.INT_PRIMITIVE,
            RuntimeClass.LONG_PRIMITIVE,
            RuntimeClass.FLOAT_PRIMITIVE,
            RuntimeClass.DOUBLE_PRIMITIVE,
            RuntimeClass.BOOLEAN_PRIMITIVE
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
    public void generate(GeneratorContext context, CodeWriter writer, MethodReference method) {
        String array = context.getParameterName(1);
        String index = context.getParameterName(2);
        String componentTypeField = context.names().forMemberField(
                new FieldReference(RuntimeClass.class.getName(), "itemType"));
        String flagsField = context.names().forMemberField(
                new FieldReference(RuntimeClass.class.getName(), "flags"));

        writer.println("JavaClass* componentType = (JavaClass*) CLASS_OF(" + array + ")"
                + "->" + componentTypeField + ";");
        writer.println("int32_t flags = componentType->" + flagsField + ";");
        writer.println("if (flags & " + RuntimeClass.PRIMITIVE + ") {").indent();

        writer.println("switch ((flags >> " + RuntimeClass.PRIMITIVE_SHIFT + ") & "
                + RuntimeClass.PRIMITIVE_MASK + ") {").indent();
        for (int i = 0; i < primitiveWrappers.length; ++i) {
            String wrapper = "java.lang." + primitiveWrappers[i];
            MethodReference methodRef = new MethodReference(wrapper, "valueOf",
                    primitiveTypes[i], ValueType.object(wrapper));
            ClassReader cls = context.getClassSource().get(methodRef.getClassName());
            if (cls == null || cls.getMethod(methodRef.getDescriptor()) == null) {
                continue;
            }

            String type = CodeWriter.strictTypeAsString(primitiveTypes[i]);
            writer.println("case " + primitives[i] + ":").indent();
            writer.println("return " + context.names().forMethod(methodRef) + "(ARRAY_AT(" + array + ", "
                    + type + ", " + index + "));");
            writer.outdent();
        }
        writer.outdent().println("}").outdent().println("}");

        writer.println("return ARRAY_AT(" + array + ", void*, " + index + ");");
    }
}
