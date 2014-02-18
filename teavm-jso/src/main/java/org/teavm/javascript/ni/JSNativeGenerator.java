/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.javascript.ni;

import java.io.IOException;
import org.teavm.codegen.SourceWriter;
import org.teavm.dependency.DependencyChecker;
import org.teavm.dependency.DependencyConsumer;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.javascript.ast.ConstantExpr;
import org.teavm.javascript.ast.Expr;
import org.teavm.javascript.ast.InvocationExpr;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class JSNativeGenerator implements Generator, Injector, DependencyPlugin {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef)
            throws IOException {
        if (methodRef.getName().equals("wrap")) {
            generateWrapString(context, writer);
        } else if (methodRef.getName().equals("unwrapString")) {
            writer.append("return $rt_str(").append(context.getParameterName(1)).append(");")
                    .softNewLine();
        }
    }

    @Override
    public void generate(InjectorContext context, MethodReference methodRef) throws IOException {
        SourceWriter writer = context.getWriter();
        switch (methodRef.getName()) {
            case "getGlobal":
                writer.append("window");
                break;
            case "isUndefined":
                writer.append("(");
                context.writeExpr(context.getArgument(0));
                writer.ws().append("===").ws().append("undefined)");
                break;
            case "getTypeName":
                writer.append("(typeof ");
                context.writeExpr(context.getArgument(0));
                writer.append(")");
                break;
            case "get":
                context.writeExpr(context.getArgument(0));
                renderProperty(context.getArgument(1), context);
                break;
            case "set":
                writer.append('(');
                context.writeExpr(context.getArgument(0));
                renderProperty(context.getArgument(1), context);
                writer.ws().append('=').ws();
                context.writeExpr(context.getArgument(2));
                writer.append(')');
                break;
            case "invoke":
                context.writeExpr(context.getArgument(0));
                renderProperty(context.getArgument(1), context);
                writer.append('(');
                for (int i = 2; i < context.argumentCount(); ++i) {
                    if (i > 2) {
                        writer.append(',').ws();
                    }
                    context.writeExpr(context.getArgument(i));
                }
                writer.append(')');
                break;
            case "wrap":
            case "unwrap":
                context.writeExpr(context.getArgument(0));
                break;
            case "function":
                generateFunction(context);
                break;
        }
    }

    @Override
    public void methodAchieved(final DependencyChecker checker, final MethodDependency graph) {
        for (int i = 0; i < graph.getReference().parameterCount(); ++i) {
            graph.getVariable(i).addConsumer(new DependencyConsumer() {
                @Override public void consume(String type) {
                    achieveFunctorMethods(checker, type, graph);
                }
            });
        }
    }

    private void achieveFunctorMethods(DependencyChecker checker, String type, MethodDependency caller) {
        if (caller.isMissing()) {
            return;
        }
        ClassReader cls = checker.getClassSource().get(type);
        if (cls != null) {
            for (MethodReader method : cls.getMethods()) {
                checker.linkMethod(method.getReference(), caller.getStack());
            }
        }
    }

    private void generateWrapString(GeneratorContext context, SourceWriter writer) throws IOException {
        FieldReference charsField = new FieldReference("java.lang.String", "characters");
        writer.append("var result = \"\";").softNewLine();
        writer.append("var data = ").append(context.getParameterName(1)).append('.')
                .appendField(charsField).append(".data;").softNewLine();
        writer.append("for (var i = 0; i < data.length; i = (i + 1) | 0) {").indent().softNewLine();
        writer.append("result += String.fromCharCode(data[i]);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return result;").softNewLine();
    }

    private void generateFunction(InjectorContext context) throws IOException {
        SourceWriter writer = context.getWriter();
        writer.append("(function()").ws().append("{").indent().softNewLine();
        writer.append("return ");
        context.writeExpr(context.getArgument(0));
        renderProperty(context.getArgument(1), context);
        writer.append(".apply(");
        context.writeExpr(context.getArgument(0));
        writer.append(",").ws().append("arguments);").softNewLine();
        writer.outdent().append("})");
    }

    private void renderProperty(Expr property, InjectorContext context) throws IOException {
        SourceWriter writer = context.getWriter();
        String name = extractPropertyName(property);
        if (name == null) {
            writer.append('[');
            context.writeExpr(property);
            writer.append(']');
        } else if (!isIdentifier(name)) {
            writer.append("[\"");
            context.writeEscaped(name);
            writer.append("\"]");
        } else {
            writer.append(".").append(name);
        }
    }

    private String extractPropertyName(Expr propertyName) {
        if (!(propertyName instanceof InvocationExpr)) {
            return null;
        }
        InvocationExpr invoke = (InvocationExpr)propertyName;
        if (!invoke.getMethod().getClassName().equals(JS.class.getName())) {
            return null;
        }
        if (!invoke.getMethod().getName().equals("wrap") ||
                !invoke.getMethod().getDescriptor().parameterType(0).isObject("java.lang.String")) {
            return null;
        }
        Expr arg = invoke.getArguments().get(0);
        if (!(arg instanceof ConstantExpr)) {
            return null;
        }
        ConstantExpr constant = (ConstantExpr)arg;
        return constant.getValue() instanceof String ? (String)constant.getValue() : null;
    }

    private boolean isIdentifier(String name) {
        if (name.isEmpty() || !Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        for (int i = 1; i < name.length(); ++i) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
