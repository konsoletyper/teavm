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
package org.teavm.jso.impl;

import static org.teavm.backend.javascript.rendering.RenderingUtil.escapeString;
import java.io.IOException;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.Expr;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.rendering.Precedence;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class JSNativeGenerator implements Injector, DependencyPlugin, Generator {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef)
            throws IOException {
        switch (methodRef.getName()) {
            case "function":
                writeFunction(context, writer);
                break;
            case "functionAsObject":
                writeFunctionAsObject(context, writer);
                break;
        }
    }

    private void writeFunction(GeneratorContext context, SourceWriter writer) throws IOException {
        String thisName = context.getParameterName(1);
        String methodName = context.getParameterName(2);
        writer.append("var name").ws().append('=').ws().append("'jso$functor$'").ws().append('+').ws()
                .append(methodName).append(';').softNewLine();
        writer.append("if").ws().append("(!").append(thisName).append("[name])").ws().append('{')
                .indent().softNewLine();

        writer.append("var fn").ws().append('=').ws().append("function()").ws().append('{')
                .indent().softNewLine();
        writer.append("return ").append(thisName).append('[').append(methodName).append(']')
                .append(".apply(").append(thisName).append(',').ws().append("arguments);").softNewLine();
        writer.outdent().append("};").softNewLine();
        writer.append(thisName).append("[name]").ws().append('=').ws().append("function()").ws().append('{')
                .indent().softNewLine();
        writer.append("return fn;").softNewLine();
        writer.outdent().append("};").softNewLine();

        writer.outdent().append('}').softNewLine();
        writer.append("return ").append(thisName).append("[name]();").softNewLine();
    }

    private void writeFunctionAsObject(GeneratorContext context, SourceWriter writer) throws IOException {
        String thisName = context.getParameterName(1);
        String methodName = context.getParameterName(2);

        writer.append("if").ws().append("(").append(thisName).ws().append("===").ws().append("null)").ws()
                .append("return null;").softNewLine();
        writer.append("var result").ws().append("=").ws().append("{};").softNewLine();
        writer.append("result[").append(methodName).append("]").ws().append("=").ws().append(thisName)
                .append(";").softNewLine();
        writer.append("return result;").softNewLine();
    }

    @Override
    public void generate(InjectorContext context, MethodReference methodRef) throws IOException {
        SourceWriter writer = context.getWriter();
        switch (methodRef.getName()) {
            case "arrayData":
                context.writeExpr(context.getArgument(0));
                writer.append(".data");
                break;
            case "get":
                context.writeExpr(context.getArgument(0), Precedence.MEMBER_ACCESS);
                renderProperty(context.getArgument(1), context);
                break;
            case "set":
                context.writeExpr(context.getArgument(0), Precedence.ASSIGNMENT.next());
                renderProperty(context.getArgument(1), context);
                writer.ws().append('=').ws();
                context.writeExpr(context.getArgument(2), Precedence.ASSIGNMENT.next());
                break;
            case "invoke":
                context.writeExpr(context.getArgument(0), Precedence.GROUPING);
                renderProperty(context.getArgument(1), context);
                writer.append('(');
                for (int i = 2; i < context.argumentCount(); ++i) {
                    if (i > 2) {
                        writer.append(',').ws();
                    }
                    context.writeExpr(context.getArgument(i), Precedence.min());
                }
                writer.append(')');
                break;
            case "instantiate":
                if (context.getPrecedence().ordinal() >= Precedence.FUNCTION_CALL.ordinal()) {
                    writer.append("(");
                }
                writer.append("new ");
                context.writeExpr(context.getArgument(0), Precedence.GROUPING);
                renderProperty(context.getArgument(1), context);
                writer.append("(");
                for (int i = 2; i < context.argumentCount(); ++i) {
                    if (i > 2) {
                        writer.append(',').ws();
                    }
                    context.writeExpr(context.getArgument(i), Precedence.min());
                }
                writer.append(")");
                if (context.getPrecedence().ordinal() >= Precedence.FUNCTION_CALL.ordinal()) {
                    writer.append(")");
                }
                break;
            case "wrap":
                if (methodRef.getDescriptor().parameterType(0).isObject("java.lang.String")) {
                    if (context.getArgument(0) instanceof ConstantExpr) {
                        ConstantExpr constant = (ConstantExpr) context.getArgument(0);
                        if (constant.getValue() instanceof String) {
                            writer.append('"').append(escapeString((String) constant.getValue())).append('"');
                            break;
                        }
                    }
                    writer.append("$rt_ustr(");
                    context.writeExpr(context.getArgument(0), Precedence.min());
                    writer.append(")");
                } else if (methodRef.getDescriptor().parameterType(0) == ValueType.BOOLEAN) {
                    if (context.getPrecedence().ordinal() >= Precedence.UNARY.ordinal()) {
                        writer.append("(");
                    }
                    writer.append("!!");
                    context.writeExpr(context.getArgument(0), Precedence.UNARY);
                    if (context.getPrecedence().ordinal() >= Precedence.UNARY.ordinal()) {
                        writer.append(")");
                    }
                } else {
                    context.writeExpr(context.getArgument(0), context.getPrecedence());
                }
                break;
            case "unwrapString":
                writer.append("$rt_str(");
                context.writeExpr(context.getArgument(0), Precedence.min());
                writer.append(")");
                break;
            case "unwrapBoolean":
                if (context.getPrecedence().ordinal() >= Precedence.CONDITIONAL.ordinal()) {
                    writer.append("(");
                }
                context.writeExpr(context.getArgument(0), Precedence.CONDITIONAL.next());
                writer.ws().append("?").ws().append("1").ws().append(":").ws().append("0");
                if (context.getPrecedence().ordinal() >= Precedence.CONDITIONAL.ordinal()) {
                    writer.append(")");
                }
                break;
            default:
                if (methodRef.getName().startsWith("unwrap")) {
                    context.writeExpr(context.getArgument(0), context.getPrecedence());
                }
                break;
        }
    }

    @Override
    public void methodReached(final DependencyAgent agent, final MethodDependency method,
            final CallLocation location) {
        switch (method.getReference().getName()) {
            case "invoke":
            case "instantiate":
            case "function":
                for (int i = 0; i < method.getReference().parameterCount(); ++i) {
                    method.getVariable(i).addConsumer(type -> reachFunctorMethods(agent, type.getName(), method));
                }
                break;
            case "unwrapString":
                method.getResult().propagate(agent.getType("java.lang.String"));
                break;
        }
    }

    private void reachFunctorMethods(DependencyAgent agent, String type, MethodDependency caller) {
        if (caller.isMissing()) {
            return;
        }
        ClassReader cls = agent.getClassSource().get(type);
        if (cls != null) {
            for (MethodReader method : cls.getMethods()) {
                if (!method.hasModifier(ElementModifier.STATIC)) {
                    agent.linkMethod(method.getReference(), null).use();
                }
            }
        }
    }


    private void renderProperty(Expr property, InjectorContext context) throws IOException {
        SourceWriter writer = context.getWriter();
        String name = extractPropertyName(property);
        if (name == null) {
            writer.append('[');
            context.writeExpr(property, Precedence.min());
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
        InvocationExpr invoke = (InvocationExpr) propertyName;
        if (!invoke.getMethod().getClassName().equals(JS.class.getName())) {
            return null;
        }
        if (!invoke.getMethod().getName().equals("wrap")
                || !invoke.getMethod().getDescriptor().parameterType(0).isObject("java.lang.String")) {
            return null;
        }
        Expr arg = invoke.getArguments().get(0);
        if (!(arg instanceof ConstantExpr)) {
            return null;
        }
        ConstantExpr constant = (ConstantExpr) arg;
        return constant.getValue() instanceof String ? (String) constant.getValue() : null;
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
