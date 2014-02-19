/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.classlib.java.lang.reflect;

import java.io.IOException;
import org.teavm.codegen.SourceWriter;
import org.teavm.dependency.DependencyChecker;
import org.teavm.dependency.DependencyConsumer;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

/**
 *
 * @author Alexey Andreev
 */
public class ArrayNativeGenerator implements Generator, DependencyPlugin {
    @Override
    public void methodAchieved(DependencyChecker checker, MethodDependency method) {
        if (method.getReference().getName().equals("getLength")) {
            achieveGetLength(checker, method);
        }
    }

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "getLength":
                generateGetLength(context, writer);
                break;
        }
    }

    private void generateGetLength(GeneratorContext context, SourceWriter writer) throws IOException {
        String array = context.getParameterName(1);
        writer.append("if (" + array + " === null || " + array + ".constructor.$meta.item === undefined) {")
                .softNewLine().indent();
        String clsName = "java.lang.IllegalArgumentException";
        MethodReference cons = new MethodReference(clsName, new MethodDescriptor("<init>", ValueType.VOID));
        writer.append("$rt_throw(").appendClass(clsName).append(".").appendMethod(cons).append("());").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return " + array + ".data.length;").softNewLine();
    }

    private void achieveGetLength(final DependencyChecker checker, final MethodDependency method) {
        method.getVariable(1).addConsumer(new DependencyConsumer() {
            @Override public void consume(String type) {
                if (!type.startsWith("[")) {
                    MethodReference cons = new MethodReference("java.lang.IllegalArgumentException",
                            new MethodDescriptor("<init>", ValueType.VOID));
                    checker.addEntryPoint(cons);
                }
            }
        });
    }
}
