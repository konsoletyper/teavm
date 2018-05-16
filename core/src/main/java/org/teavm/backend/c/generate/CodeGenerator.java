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
package org.teavm.backend.c.generate;

import java.util.Set;
import org.teavm.ast.RegularMethodNode;
import org.teavm.ast.VariableNode;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;

public class CodeGenerator {
    private GenerationContext context;
    private CodeWriter writer;
    private CodeWriter localsWriter;
    private NameProvider names;
    private Set<? super String> includes;

    public CodeGenerator(GenerationContext context, CodeWriter writer, Set<? super String> includes) {
        this.context = context;
        this.writer = writer;
        this.names = context.getNames();
        this.includes = includes;
    }

    public void generateMethod(RegularMethodNode methodNode) {
        generateMethodSignature(writer, methodNode.getReference(),
                methodNode.getModifiers().contains(ElementModifier.STATIC), true);

        writer.print(" {").indent().println();

        localsWriter = writer.fragment();

        CodeGenerationVisitor visitor = new CodeGenerationVisitor(context, writer, includes);
        visitor.setCallingMethod(methodNode.getReference());
        methodNode.getBody().acceptVisitor(visitor);

        generateLocals(methodNode, visitor.getTemporaries());

        writer.outdent().println("}");
    }

    public void generateMethodSignature(CodeWriter writer, MethodReference methodRef, boolean isStatic,
            boolean withNames) {
        writer.print("static ");
        writer.printType(methodRef.getReturnType()).print(" ").print(names.forMethod(methodRef)).print("(");

        generateMethodParameters(writer, methodRef.getDescriptor(), isStatic, withNames);

        writer.print(")");
    }

    public void generateMethodParameters(CodeWriter writer, MethodDescriptor methodRef, boolean isStatic,
            boolean withNames) {
        if (methodRef.parameterCount() == 0 && isStatic) {
            return;
        }

        int start = 0;
        if (!isStatic) {
            writer.print("void*");
            if (withNames) {
                writer.print(" _this_");
            }
        } else {
            writer.printType(methodRef.parameterType(0));
            if (withNames) {
                writer.print(" local_1");
            }
            start++;
        }

        for (int i = start; i < methodRef.parameterCount(); ++i) {
            writer.print(", ").printType(methodRef.parameterType(i));
            if (withNames) {
                writer.print(" ").print("local_").print(String.valueOf(i + 1));
            }
        }
    }

    private void generateLocals(RegularMethodNode methodNode, int[] temporaryCount) {
        int start = methodNode.getReference().parameterCount() + 1;
        for (int i = start; i < methodNode.getVariables().size(); ++i) {
            VariableNode variableNode = methodNode.getVariables().get(i);
            if (variableNode.getType() == null) {
                continue;
            }
            localsWriter.printType(variableNode.getType()).print(" local_").print(String.valueOf(i)).println(";");
        }

        for (CVariableType type : CVariableType.values()) {
            for (int i = 0; i < temporaryCount[type.ordinal()]; ++i) {
                localsWriter.print(type.text + " tmp_" + type.name().toLowerCase() + "_" + i).println(";");
            }
        }
    }
}
