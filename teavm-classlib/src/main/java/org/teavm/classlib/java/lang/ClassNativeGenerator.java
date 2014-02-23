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
            case "getComponentType0":
                generateGetComponentType(context, writer);
                break;
            case "getSuperclass":
                generateGetSuperclass(context, writer);
                break;
            case "forNameImpl":
                generateForName(context, writer);
                break;
            case "newInstance":
                generateNewInstance(context, writer);
                break;
            case "getDeclaringClass":
                generateGetDeclaringClass(context, writer);
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
                context.getWriter().append("$rt_cls($rt_booleancls())");
                break;
            case "intClass":
                context.getWriter().append("$rt_cls($rt_intcls())");
                break;
            case "voidClass":
                context.getWriter().append("$rt_cls($rt_voidcls())");
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

    private void generateNewInstance(GeneratorContext context, SourceWriter writer) throws IOException {
        String self = context.getParameterName(0);
        writer.append("if (!").appendClass("java.lang.Class").append(".$$constructors$$) {").indent().softNewLine();
        writer.appendClass("java.lang.Class").append(".$$constructors$$ = true;").softNewLine();
        for (String clsName : context.getClassSource().getClassNames()) {
            ClassReader cls = context.getClassSource().get(clsName);
            MethodReader method = cls.getMethod(new MethodDescriptor("<init>", ValueType.VOID));
            if (method != null) {
                writer.appendClass(clsName).append(".$$constructor$$ = ").appendMethodBody(method.getReference())
                        .append(";").softNewLine();
            }
        }
        writer.outdent().append("}").softNewLine();
        writer.append("var cls = " + self + ".$data;").softNewLine();
        writer.append("var ctor = cls.$$constructor$$;").softNewLine();
        writer.append("if (ctor === null) {").indent().softNewLine();
        writer.append("var ex = new ").appendClass(InstantiationException.class.getName()).append("();");
        writer.appendMethodBody(new MethodReference(InstantiationException.class.getName(), new MethodDescriptor(
                "<init>", ValueType.VOID))).append("(ex);").softNewLine();
        writer.append("$rt_throw(ex);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("var instance = new cls();").softNewLine();
        writer.append("ctor(instance);").softNewLine();
        writer.append("return instance;").softNewLine();
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
    public void methodAchieved(DependencyChecker checker, MethodDependency graph) {
        switch (graph.getReference().getName()) {
            case "booleanClass":
            case "intClass":
            case "wrap":
            case "getSuperclass":
            case "getComponentType0":
            case "forNameImpl":
            case "getDeclaringClass":
                graph.getResult().propagate("java.lang.Class");
                break;
        }
    }
}
