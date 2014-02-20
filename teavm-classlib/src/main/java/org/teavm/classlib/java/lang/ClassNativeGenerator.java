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
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.javascript.ni.Injector;
import org.teavm.javascript.ni.InjectorContext;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ClassNativeGenerator implements Generator, Injector, DependencyPlugin {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef)
            throws IOException {
        switch (methodRef.getName()) {
            case "getComponentType0":
                generateGetComponentType(context, writer);
                break;
            case "getSuperclass":
                generateGetSuperclass(context, writer);
                break;
        }
    }

    private void generateGetComponentType(GeneratorContext context, SourceWriter writer) throws IOException {
        String thisArg = context.getParameterName(0);
        writer.append("var item = " + thisArg + ".$data.$meta.item;").softNewLine();
        writer.append("return item != null ? $rt_cls(item) : null;").softNewLine();
    }

    private void generateGetSuperclass(GeneratorContext context, SourceWriter writer) throws IOException {
        String thisArg = context.getParameterName(0);
        writer.append("var superclass = " + thisArg + ".$data.$meta.superclass;").softNewLine();
        writer.append("return superclass ? $rt_cls(superclass) : null;").softNewLine();
    }

    @Override
    public void generate(InjectorContext context, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "isInstance":
                generateIsInstance(context);
                break;
            case "isAssignableFrom":
                generateIsAssignableFrom(context);
                break;
            case "booleanClass":
                generateBooleanClass(context);
                break;
            case "intClass":
                generateIntClass(context);
                break;
            case "wrap":
                context.writeExpr(context.getArgument(0));
                break;
            case "getEnumConstantsImpl":
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(".$data.values()");
                break;
        }
    }

    private void generateIsAssignableFrom(InjectorContext context) throws IOException {
        SourceWriter writer = context.getWriter();
        writer.append("$rt_isAssignable(");
        context.writeExpr(context.getArgument(1));
        writer.append(".$data,").ws();
        context.writeExpr(context.getArgument(0));
        writer.append(".$data)");
    }

    private void generateIsInstance(InjectorContext context) throws IOException {
        SourceWriter writer = context.getWriter();
        writer.append("$rt_isInstance(");
        context.writeExpr(context.getArgument(1));
        writer.append(",").ws();
        context.writeExpr(context.getArgument(0));
        writer.append(".$data)");
    }

    private void generateBooleanClass(InjectorContext context) throws IOException {
        context.getWriter().append("$rt_cls($rt_booleancls())");
    }

    private void generateIntClass(InjectorContext context) throws IOException {
        context.getWriter().append("$rt_cls($rt_intcls())");
    }

    @Override
    public void methodAchieved(DependencyChecker checker, MethodDependency graph) {
        switch (graph.getReference().getName()) {
            case "booleanClass":
            case "intClass":
            case "wrap":
            case "getSuperclass":
            case "getComponentType0":
                graph.getResult().propagate("java.lang.Class");
                break;
        }
    }
}
