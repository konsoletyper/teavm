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
public class ClassNativeGenerator implements Generator, Injector, DependencyPlugin {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef)
            throws IOException {
        switch (methodRef.getName()) {
            case "forNameImpl":
                generateForName(context, writer);
                break;
            case "getDeclaringClass":
                generateGetDeclaringClass(context, writer);
                break;
        }
    }

    @Override
    public void generate(InjectorContext context, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "getEnumConstantsImpl":
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(".$data.values()");
                break;
        }
    }

    private void generateForName(GeneratorContext context, SourceWriter writer) throws IOException {
        String param = context.getParameterName(1);
        writer.append("switch ($rt_ustr(" + param + ")) {").softNewLine().indent();
        for (String name : context.getClassSource().getClassNames()) {
            writer.append("case \"" + name + "\": ").appendClass(name).append(".$clinit(); ")
                    .append("return $rt_cls(").appendClass(name).append(");").softNewLine();
        }
        writer.append("default: return null;").softNewLine();
        writer.outdent().append("}").softNewLine();
    }

    private void generateGetDeclaringClass(GeneratorContext context, SourceWriter writer) throws IOException {
        String self = context.getParameterName(0);
        writer.append("if (!").appendClass("java.lang.Class").append(".$$owners$$) {").indent().softNewLine();
        writer.appendClass("java.lang.Class").append(".$$owners$$ = true;").softNewLine();
        for (String clsName : context.getClassSource().getClassNames()) {
            ClassReader cls = context.getClassSource().get(clsName);
            writer.appendClass(clsName).append(".$$owner$$ = ");
            if (cls.getOwnerName() != null) {
                writer.appendClass(cls.getOwnerName());
            } else {
                writer.append("null");
            }
            writer.append(";").softNewLine();
        }
        writer.outdent().append("}").softNewLine();
        writer.append("var cls = " + self + ".$data;").softNewLine();
        writer.append("return cls.$$owner$$ != null ? $rt_cls(cls.$$owner$$) : null;").softNewLine();
    }

    @Override
    public void methodAchieved(DependencyAgent agent, MethodDependency graph, CallLocation location) {
        switch (graph.getReference().getName()) {
            case "forNameImpl":
            case "getDeclaringClass":
                graph.getResult().propagate(agent.getType("java.lang.Class"));
                break;
            case "newInstance":
                agent.linkMethod(new MethodReference(InstantiationException.class, "<init>", void.class),
                        location).use();
                break;
        }
    }
}
