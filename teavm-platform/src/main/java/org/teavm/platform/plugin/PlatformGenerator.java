/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.platform.plugin;

import java.io.IOException;
import org.teavm.codegen.SourceWriter;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.javascript.spi.Generator;
import org.teavm.javascript.spi.GeneratorContext;
import org.teavm.javascript.spi.Injector;
import org.teavm.javascript.spi.InjectorContext;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class PlatformGenerator implements Generator, Injector, DependencyPlugin {
    @Override
    public void methodAchieved(DependencyAgent agent, MethodDependency method, CallLocation location) {
        switch (method.getReference().getName()) {
            case "asJavaClass":
                method.getResult().propagate(agent.getType("java.lang.Class"));
                return;
            case "clone":
                method.getVariable(0).connect(method.getResult());
                break;
        }
    }

    @Override
    public void generate(InjectorContext context, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "asJavaClass":
            case "classFromResource":
                context.writeExpr(context.getArgument(0));
                return;
            case "getEnumConstants":
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(".values()");
                break;
        }
    }

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "newInstance":
                generateNewInstance(context, writer, methodRef);
                break;
            case "lookupClass":
                generateLookup(context, writer);
                break;
            case "clone":
                generateClone(context, writer);
                break;
        }
    }

    private void generateNewInstance(GeneratorContext context, SourceWriter writer, MethodReference methodRef)
            throws IOException {
        writer.append("var c").ws().append("=").ws().append("'$$constructor$$';").softNewLine();
        for (String clsName : context.getClassSource().getClassNames()) {
            ClassReader cls = context.getClassSource().get(clsName);
            MethodReader method = cls.getMethod(new MethodDescriptor("<init>", void.class));
            if (method != null) {
                writer.appendClass(clsName).append("[c]").ws().append("=").ws()
                        .append(writer.getNaming().getNameForInit(method.getReference()))
                        .append(";").softNewLine();
            }
        }
        writer.appendMethodBody(methodRef).ws().append("=").ws().append("function(cls)").ws().append("{")
                .softNewLine().indent();
        writer.append("if").ws().append("(!cls.hasOwnProperty(c))").ws().append("{").indent().softNewLine();
        writer.append("return null;").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return cls[c]();").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return ").appendMethodBody(methodRef).append("(")
                .append(context.getParameterName(1)).append(");").softNewLine();
    }

    private void generateLookup(GeneratorContext context, SourceWriter writer) throws IOException {
        String param = context.getParameterName(1);
        writer.append("switch ($rt_ustr(" + param + ")) {").softNewLine().indent();
        for (String name : context.getClassSource().getClassNames()) {
            writer.append("case \"" + name + "\": ").appendClass(name).append(".$clinit(); ")
                    .append("return ").appendClass(name).append(";").softNewLine();
        }
        writer.append("default: return null;").softNewLine();
        writer.outdent().append("}").softNewLine();
    }

    private void generateClone(GeneratorContext context, SourceWriter writer) throws IOException {
        String obj = context.getParameterName(1);
        writer.append("var copy").ws().append("=").ws().append("new ").append(obj).append(".constructor();")
                .softNewLine();
        writer.append("for").ws().append("(var field in " + obj + ")").ws().append("{").softNewLine().indent();
        writer.append("if").ws().append("(!" + obj + ".hasOwnProperty(field))").ws().append("{").softNewLine().indent();
        writer.append("continue;").softNewLine().outdent().append("}").softNewLine();
        writer.append("copy[field]").ws().append("=").ws().append(obj).append("[field];")
                .softNewLine().outdent().append("}").softNewLine();
        writer.append("return copy;").softNewLine();
    }
}
