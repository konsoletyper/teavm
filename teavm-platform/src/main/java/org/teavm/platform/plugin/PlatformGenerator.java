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
import org.teavm.platform.Platform;

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
        }
    }

    @Override
    public void generate(InjectorContext context, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "asJavaClass":
                context.writeExpr(context.getArgument(0));
                return;
        }
    }

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "newInstance":
                generateNewInstance(context, writer);
        }
    }

    private void generateNewInstance(GeneratorContext context, SourceWriter writer) throws IOException {
        String self = context.getParameterName(0);
        writer.append("if").ws().append("(!").appendClass(Platform.class).append(".$$constructors$$)").ws()
                .append("{").indent().softNewLine();
        writer.appendClass(Platform.class).append(".$$constructors$$").ws().append("=").append("true;").softNewLine();
        for (String clsName : context.getClassSource().getClassNames()) {
            ClassReader cls = context.getClassSource().get(clsName);
            MethodReader method = cls.getMethod(new MethodDescriptor("<init>", void.class));
            if (method != null) {
                writer.appendClass(clsName).append(".$$constructor$$").ws().append("=").ws()
                        .appendMethodBody(method.getReference()).append(";").softNewLine();
            }
        }
        writer.outdent().append("}").softNewLine();
        writer.append("var cls = " + self + ".$data;").softNewLine();
        writer.append("var ctor = cls.$$constructor$$;").softNewLine();
        writer.append("if (!ctor) {").indent().softNewLine();
        writer.append("var ex = new ").appendClass(InstantiationException.class.getName()).append("();").softNewLine();
        writer.appendMethodBody(new MethodReference(InstantiationException.class, "<init>", void.class))
                .append("(ex);").softNewLine();
        writer.append("$rt_throw(ex);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("var instance = new cls();").softNewLine();
        writer.append("ctor(instance);").softNewLine();
        writer.append("return instance;").softNewLine();
    }
}
