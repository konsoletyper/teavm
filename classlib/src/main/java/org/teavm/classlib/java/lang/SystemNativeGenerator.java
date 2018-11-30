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
package org.teavm.classlib.java.lang;

import java.io.IOException;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.MethodReference;

public class SystemNativeGenerator implements Generator, DependencyPlugin {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "doArrayCopy":
                generateArrayCopy(context, writer);
                break;
            case "currentTimeMillis":
                generateCurrentTimeMillis(writer);
                break;
        }
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        switch (method.getReference().getName()) {
            case "doArrayCopy":
                reachArrayCopy(method);
                break;
        }
    }

    private void generateArrayCopy(GeneratorContext context, SourceWriter writer) throws IOException {
        String src = context.getParameterName(1);
        String srcPos = context.getParameterName(2);
        String dest = context.getParameterName(3);
        String destPos = context.getParameterName(4);
        String length = context.getParameterName(5);
        writer.append("if (" + src + " !== " +  dest + " || " + destPos + " < " + srcPos + ") {").indent().newLine();
        writer.append("for (var i = 0; i < " + length + "; i = (i + 1) | 0) {").indent().softNewLine();
        writer.append(dest + ".data[" + destPos + "++] = " + src + ".data[" + srcPos + "++];").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.outdent().append("} else {").indent().softNewLine();
        writer.append(srcPos + " = (" + srcPos + " + " + length + ") | 0;").softNewLine();
        writer.append(destPos + " = (" + destPos + " + " + length + ") | 0;").softNewLine();
        writer.append("for (var i = 0; i < " + length + "; i = (i + 1) | 0) {").indent().softNewLine();
        writer.append(dest + ".data[--" + destPos + "] = " + src + ".data[--" + srcPos + "];").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.outdent().append("}").softNewLine();
    }

    private void generateCurrentTimeMillis(SourceWriter writer) throws IOException {
        writer.append("return Long_fromNumber(new Date().getTime());").softNewLine();
    }

    private void reachArrayCopy(MethodDependency method) {
        DependencyNode src = method.getVariable(1);
        DependencyNode dest = method.getVariable(3);
        src.getArrayItem().connect(dest.getArrayItem());
    }
}
