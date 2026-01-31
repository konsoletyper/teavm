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
package org.teavm.backend.c.generators;

import org.teavm.backend.c.generate.CodeGeneratorUtil;
import org.teavm.interop.Address;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.RuntimeClass;

public class ClassMethodsGenerator implements Generator {
    private static final MethodReference ALLOC_ARRAY_METHOD = new MethodReference(Allocator.class,
            "allocateArray", RuntimeClass.class, int.class, Address.class);

    @Override
    public boolean canHandle(MethodReference method) {
        if (!method.getClassName().equals("java.lang.Class")) {
            return false;
        }
        switch (method.getName()) {
            case "getDeclaredAnnotationsImpl":
            case "getReflectionStateC":
            case "setReflectionStateC":
                return true;
            default:
                return false;
        }
    }

    @Override
    public void generate(GeneratorContext context, MethodReference method) {
        switch (method.getName()) {
            case "getDeclaredAnnotationsImpl":
                generateGetDeclaredAnnot(context);
                break;
            case "getReflectionStateC":
                generateGetReflectionState(context);
                break;
            case "setReflectionStateC":
                generateSetReflectionState(context);
                break;
        }
    }

    private void generateGetDeclaredAnnot(GeneratorContext context) {
        var annotType = ValueType.object("java.lang.annotation.Annotation");
        context.importMethod(ALLOC_ARRAY_METHOD, true);

        var writer = context.writer();
        context.includes().includePath("reflection-ext.h");
        writer.println("TeaVM_ClassReflection* reflect = teavm_class_reflect(" + context.parameterName(0) + ");");

        writer.println("if (reflect == NULL || reflect->annotationsRef == NULL) {").indent();
        writer.print("return ").print(context.names().forMethod(ALLOC_ARRAY_METHOD)).print("(");
        CodeGeneratorUtil.writeTypeReference(writer, context.mainContext(), context.includes(),
                ValueType.arrayOf(annotType));
        writer.print(", 0);").println();
        writer.outdent().println("}");

        writer.print("TeaVM_Array* array = ").print(context.names().forMethod(ALLOC_ARRAY_METHOD)).print("(");
        CodeGeneratorUtil.writeTypeReference(writer, context.mainContext(), context.includes(),
                        ValueType.arrayOf(annotType));
        writer.print(", reflect->annotationCount);").println();
        writer.println("void** arrayData = TEAVM_ARRAY_DATA(array, void*);");
        writer.println("for (int32_t i = 0; i < reflect->annotationCount; ++i) {").indent();
        writer.println("arrayData[i] = reflect->annotationsRef[i];");
        writer.outdent().println("}");
        writer.println("return array;");
    }

    private void generateGetReflectionState(GeneratorContext context) {
        var writer = context.writer();
        writer.println("return ((TeaVM_Class*) " + context.parameterName(0) + ")->reflectionState;");
    }

    private void generateSetReflectionState(GeneratorContext context) {
        var writer = context.writer();
        writer.println("((TeaVM_Class*) " + context.parameterName(0) + ")->reflectionState = "
                + context.parameterName(1) + ";");
    }
}
