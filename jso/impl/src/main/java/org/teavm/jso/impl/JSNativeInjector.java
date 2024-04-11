/*
 *  Copyright 2023 Alexey Andreev.
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.teavm.ast.ArrayFromDataExpr;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.Expr;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.InvocationType;
import org.teavm.ast.NewArrayExpr;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.rendering.Precedence;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

public class JSNativeInjector implements Injector, DependencyPlugin {
    private Set<MethodReference> reachedFunctorMethods = new HashSet<>();
    private Set<DependencyNode> functorParamNodes = new HashSet<>();
    private static final ValueType STRING_ARRAY = ValueType.arrayOf(ValueType.object("java.lang.String"));

    @Override
    public void generate(InjectorContext context, MethodReference methodRef) {
        SourceWriter writer = context.getWriter();
        switch (methodRef.getName()) {
            case "arrayData":
                context.writeExpr(context.getArgument(0));
                writer.append(".data");
                break;
            case "concatArray":
                writer.appendFunction("$rt_concatArrays").append("(");
                context.writeExpr(context.getArgument(0), Precedence.ASSIGNMENT);
                writer.append(",").ws();
                context.writeExpr(context.getArgument(1), Precedence.ASSIGNMENT);
                writer.append(")");
                break;
            case "get":
            case "getPure":
                if (isNull(context.getArgument(0))) {
                    writer.append(extractPropertyName(context.getArgument(1)));
                } else {
                    context.writeExpr(context.getArgument(0), Precedence.MEMBER_ACCESS);
                    renderProperty(context.getArgument(1), context);
                }
                break;
            case "set":
            case "setPure":
                if (isNull(context.getArgument(0))) {
                    writer.append(extractPropertyName(context.getArgument(1)));
                } else {
                    context.writeExpr(context.getArgument(0), Precedence.MEMBER_ACCESS.next());
                    renderProperty(context.getArgument(1), context);
                }
                writer.ws().append('=').ws();
                context.writeExpr(context.getArgument(2), Precedence.ASSIGNMENT.next());
                break;
            case "invoke":
                if (isNull(context.getArgument(0))) {
                    writer.append(extractPropertyName(context.getArgument(1)));
                } else {
                    context.writeExpr(context.getArgument(0), Precedence.GROUPING);
                    renderProperty(context.getArgument(1), context);
                }
                writer.append('(');
                for (int i = 2; i < context.argumentCount(); ++i) {
                    if (i > 2) {
                        writer.append(',').ws();
                    }
                    context.writeExpr(context.getArgument(i), Precedence.min());
                }
                writer.append(')');
                break;
            case "apply":
                applyFunction(context);
                break;
            case "arrayOf":
                writer.append('[');
                for (int i = 0; i < context.argumentCount(); ++i) {
                    if (i > 0) {
                        writer.append(',').ws();
                    }
                    context.writeExpr(context.getArgument(i), Precedence.min());
                }
                writer.append(']');
                break;
            case "construct":
                if (context.getPrecedence().ordinal() >= Precedence.FUNCTION_CALL.ordinal()) {
                    writer.append("(");
                }
                writer.append("new ");
                context.writeExpr(context.getArgument(0), Precedence.GROUPING);
                writer.append('(');
                for (int i = 1; i < context.argumentCount(); ++i) {
                    if (i > 1) {
                        writer.append(',').ws();
                    }
                    context.writeExpr(context.getArgument(i), Precedence.min());
                }
                writer.append(')');
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
                    writer.appendFunction("$rt_ustr").append("(");
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
                writer.appendFunction("$rt_str").append("(");
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

            case "dataToByteArray":
                dataToArray(context, "$rt_bytecls");
                break;
            case "dataToShortArray":
                dataToArray(context, "$rt_shortcls");
                break;
            case "dataToCharArray":
                dataToArray(context, "$rt_charcls");
                break;
            case "dataToIntArray":
                dataToArray(context, "$rt_intcls");
                break;
            case "dataToFloatArray":
                dataToArray(context, "$rt_floatcls");
                break;
            case "dataToDoubleArray":
                dataToArray(context, "$rt_doublecls");
                break;
            case "dataToArray":
                dataToArray(context, "$rt_objcls");
                break;
            case "global": {
                var cst = (ConstantExpr) context.getArgument(0);
                var name = (String) cst.getValue();
                writer.appendGlobal(name);
                break;
            }
            case "importModule": {
                var cst = (ConstantExpr) context.getArgument(0);
                var name = (String) cst.getValue();
                writer.appendFunction(context.importModule(name));
                break;
            }
            case "instanceOf": {
                if (context.getPrecedence().ordinal() >= Precedence.CONDITIONAL.ordinal()) {
                    writer.append("(");
                }
                context.writeExpr(context.getArgument(0), Precedence.COMPARISON.next());
                writer.append(" instanceof ");
                context.writeExpr(context.getArgument(1), Precedence.COMPARISON.next());
                writer.ws().append("?").ws().append("1").ws().append(":").ws().append("0");
                if (context.getPrecedence().ordinal() >= Precedence.CONDITIONAL.ordinal()) {
                    writer.append(")");
                }
                break;
            }
            case "isPrimitive": {
                if (context.getPrecedence().ordinal() >= Precedence.CONDITIONAL.ordinal()) {
                    writer.append("(");
                }
                writer.append("typeof ");
                context.writeExpr(context.getArgument(0), Precedence.UNARY.next());
                writer.ws().append("===").ws();
                context.writeExpr(context.getArgument(1), Precedence.COMPARISON.next());
                writer.ws().append("?").ws().append("1").ws().append(":").ws().append("0");
                if (context.getPrecedence().ordinal() >= Precedence.CONDITIONAL.ordinal()) {
                    writer.append(")");
                }
                break;
            }
            case "throwCCEIfFalse": {
                writer.appendFunction("$rt_throwCCEIfFalse").append("(");
                context.writeExpr(context.getArgument(0), Precedence.min());
                writer.append(",").ws();
                context.writeExpr(context.getArgument(1), Precedence.min());
                writer.append(")");
                break;
            }

            default:
                if (methodRef.getName().startsWith("unwrap")) {
                    context.writeExpr(context.getArgument(0), context.getPrecedence());
                }
                break;
        }
    }

    private static boolean isNull(Expr expr) {
        if (expr instanceof ConstantExpr) {
            var constantExpr = (ConstantExpr) expr;
            if (constantExpr.getValue() == null) {
                return true;
            }
        }
        return false;
    }

    private void applyFunction(InjectorContext context) {
        if (tryApplyFunctionOptimized(context)) {
            return;
        }
        var writer = context.getWriter();
        if (isNull(context.getArgument(0))) {
            writer.appendFunction("$rt_apply_topLevel").append("(");
            writer.append(extractPropertyName(context.getArgument(1)));
        } else {
            writer.appendFunction("$rt_apply").append("(");
            context.writeExpr(context.getArgument(0), Precedence.ASSIGNMENT);
            writer.append(",").ws();
            context.writeExpr(context.getArgument(1), Precedence.ASSIGNMENT);
        }
        writer.append(",").ws();
        context.writeExpr(context.getArgument(2), Precedence.ASSIGNMENT);
        writer.append(")");
    }

    private boolean tryApplyFunctionOptimized(InjectorContext context) {
        var paramList = new ArrayList<Expr>();
        if (!extractConstantArgList(context.getArgument(2), paramList) || paramList.size() >= 13) {
            return false;
        }

        applyFunctionOptimized(context, paramList);
        return true;
    }

    private boolean extractConstantArgList(Expr expr, List<Expr> target) {
        if (!(expr instanceof InvocationExpr)) {
            return false;
        }
        var invocation = (InvocationExpr) expr;
        if (!invocation.getMethod().getClassName().equals(JS.class.getName())) {
            return false;
        }

        switch (invocation.getMethod().getName()) {
            case "arrayOf":
                target.addAll(invocation.getArguments());
                return true;
            case "concatArray":
                return extractConstantArgList(invocation.getArguments().get(0), target)
                        && extractConstantArgList(invocation.getArguments().get(1), target);
            case "arrayData": {
                var arg = invocation.getArguments().get(0);
                if (arg instanceof ArrayFromDataExpr) {
                    target.addAll(((ArrayFromDataExpr) arg).getData());
                    return true;
                }
                if (arg instanceof NewArrayExpr && isEmptyArrayConstructor((NewArrayExpr) arg)) {
                    return true;
                }
                break;
            }
            case "wrap": {
                if (invocation.getMethod().parameterType(0).equals(STRING_ARRAY)) {
                    var arg = invocation.getArguments().get(0);
                    if (arg instanceof ArrayFromDataExpr) {
                        extractConstantStringArgList(((ArrayFromDataExpr) arg).getData(), target);
                        return true;
                    }
                    if (arg instanceof NewArrayExpr && isEmptyArrayConstructor((NewArrayExpr) arg)) {
                        return true;
                    }
                }
                break;
            }
        }
        return false;
    }

    private boolean isEmptyArrayConstructor(NewArrayExpr expr) {
        var length = expr.getLength();
        if (!(length instanceof ConstantExpr)) {
            return false;
        }
        return Objects.equals(((ConstantExpr) length).getValue(), 0);
    }

    private void extractConstantStringArgList(List<Expr> source, List<Expr> target) {
        for (var element : source) {
            var invocation = new InvocationExpr();
            invocation.setType(InvocationType.STATIC);
            invocation.setMethod(JSMethods.WRAP_STRING);
            invocation.setLocation(element.getLocation());
            invocation.getArguments().add(element);
            target.add(invocation);
        }
    }

    private void applyFunctionOptimized(InjectorContext context, List<Expr> paramList) {
        var writer = context.getWriter();
        if (isNull(context.getArgument(0))) {
            writer.append(extractPropertyName(context.getArgument(1)));
        } else {
            context.writeExpr(context.getArgument(0), Precedence.GROUPING);
            renderProperty(context.getArgument(1), context);
        }
        writer.append('(');
        for (int i = 0; i < paramList.size(); ++i) {
            if (i > 0) {
                writer.append(',').ws();
            }
            context.writeExpr(paramList.get(i), Precedence.min());
        }
        writer.append(')');
    }

    private void dataToArray(InjectorContext context, String className) {
        var writer = context.getWriter();
        writer.appendFunction("$rt_wrapArray").append("(").appendFunction(className).append(",").ws();
        context.writeExpr(context.getArgument(0), Precedence.min());
        writer.append(")");
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        switch (method.getReference().getName()) {
            case "invoke":
            case "construct":
            case "function":
                if (reachedFunctorMethods.add(method.getReference()) && !method.isMissing()) {
                    for (int i = 0; i < method.getReference().parameterCount(); ++i) {
                        DependencyNode node = method.getVariable(i);
                        if (functorParamNodes.add(node)) {
                            node.addConsumer(type -> {
                                if (agent.getClassHierarchy().isSuperType(method.getMethod().getOwnerName(),
                                        type.getName(), false)) {
                                    reachFunctorMethods(agent, type.getName());
                                }
                            });
                        }
                    }
                }
                break;
            case "unwrapString":
                method.getResult().propagate(agent.getType("java.lang.String"));
                break;

            case "dataToByteArray":
                method.getResult().propagate(agent.getType("[B"));
                break;
            case "dataToShortArray":
                method.getResult().propagate(agent.getType("[S"));
                break;
            case "dataToCharArray":
                method.getResult().propagate(agent.getType("[C"));
                break;
            case "dataToIntArray":
                method.getResult().propagate(agent.getType("[I"));
                break;
            case "dataToFloatArray":
                method.getResult().propagate(agent.getType("[F"));
                break;
            case "dataToDoubleArray":
                method.getResult().propagate(agent.getType("[D"));
                break;
            case "dataToArray":
                method.getResult().propagate(agent.getType("[Ljava/lang/Object;"));
                break;
        }
    }

    private void reachFunctorMethods(DependencyAgent agent, String type) {
        ClassReader cls = agent.getClassSource().get(type);
        if (cls != null) {
            for (MethodReader method : cls.getMethods()) {
                if (!method.hasModifier(ElementModifier.STATIC)) {
                    agent.linkMethod(method.getReference()).use();
                }
            }
        }
    }


    private void renderProperty(Expr property, InjectorContext context) {
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
