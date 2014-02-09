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
import org.teavm.codegen.SourceWriter;
import org.teavm.dependency.DependencyChecker;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodGraph;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
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
    public void methodAchieved(DependencyChecker checker, MethodReference method) {
        switch (method.getName()) {
            case "doArrayCopy":
                achieveArrayCopy(checker, method);
                break;
        }
    }

    private void generateArrayCopy(GeneratorContext context, SourceWriter writer) throws IOException {
        String src = context.getParameterName(1);
        String srcPos = context.getParameterName(2);
        String dest = context.getParameterName(3);
        String destPos = context.getParameterName(4);
        String length = context.getParameterName(5);
        writer.append("for (var i = 0; i < " + length + "; i = (i + 1) | 0) {").indent().softNewLine();
        writer.append(dest + ".data[" + srcPos + "++] = " + src + ".data[" + destPos + "++];").softNewLine();
        writer.outdent().append("}").softNewLine();
    }

    private void generateCurrentTimeMillis(SourceWriter writer) throws IOException {
        writer.append("return Long_fromNumber(new Date().getTime());").softNewLine();
    }

    private void achieveArrayCopy(DependencyChecker checker, MethodReference method) {
        MethodGraph graph = checker.attachMethodGraph(method);
        DependencyNode src = graph.getVariableNode(1);
        DependencyNode dest = graph.getVariableNode(3);
        src.getArrayItemNode().connect(dest.getArrayItemNode());
    }
}
