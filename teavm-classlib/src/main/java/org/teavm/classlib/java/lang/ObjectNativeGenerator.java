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
import org.teavm.dependency.*;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.javascript.ni.Injector;
import org.teavm.javascript.ni.InjectorContext;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ObjectNativeGenerator implements Generator, Injector, DependencyPlugin {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getDescriptor().getName()) {
            case "<init>":
                generateInit(context, writer);
                break;
            case "hashCode":
            case "identity":
                generateHashCode(context, writer);
                break;
            case "clone":
                generateClone(context, writer);
                break;
        }
    }

    @Override
    public void generate(InjectorContext context, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "getClass":
                generateGetClass(context);
                break;
            case "wrap":
                generateWrap(context);
                break;
        }
    }

    @Override
    public void methodAchieved(DependencyAgent agent, MethodDependency method) {
        switch (method.getReference().getName()) {
            case "clone":
                method.getVariable(0).connect(method.getResult());
                break;
            case "getClass":
                achieveGetClass(agent, method);
                break;
            case "wrap":
                method.getVariable(1).connect(method.getResult());
                break;
        }
    }

    private void generateInit(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append(context.getParameterName(0)).append(".$id = $rt_nextId();").softNewLine();
    }

    private void generateGetClass(InjectorContext context) throws IOException {
        SourceWriter writer = context.getWriter();
        writer.append("$rt_cls(");
        context.writeExpr(context.getArgument(0));
        writer.append(".constructor)");
    }

    private void achieveGetClass(DependencyAgent agent, MethodDependency method) {
        MethodReference initMethod = new MethodReference(Class.class, "createNew", Class.class);
        agent.linkMethod(initMethod, method.getStack()).use();
        method.getResult().propagate(agent.getType("java.lang.Class"));
    }

    private void generateHashCode(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append("return ").append(context.getParameterName(0)).append(".$id;").softNewLine();
    }

    private void generateClone(GeneratorContext context, SourceWriter writer) throws IOException {
        String obj = context.getParameterName(0);
        writer.append("var copy = new ").append(obj).append(".constructor();").softNewLine();
        writer.append("for (var field in " + obj + ") {").softNewLine().indent();
        writer.append("if (!" + obj + ".hasOwnProperty(field)) {").softNewLine().indent();
        writer.append("continue;").softNewLine().outdent().append("}").softNewLine();
        writer.append("copy[field] = " + obj + "[field];").softNewLine().outdent().append("}").softNewLine();
        writer.append("return copy;").softNewLine();
    }

    private void generateWrap(InjectorContext context) throws IOException {
        context.writeExpr(context.getArgument(0));
    }
}
