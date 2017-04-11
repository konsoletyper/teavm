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
package org.teavm.platform.plugin;

import java.io.IOException;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.Expr;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class ResourceAccessorInjector implements Injector {
    @Override
    public void generate(InjectorContext context, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "get":
            case "getProperty":
                if (methodRef.getDescriptor().parameterType(1) == ValueType.INTEGER) {
                    context.writeExpr(context.getArgument(0));
                    context.getWriter().append('[');
                    context.writeExpr(context.getArgument(1));
                    context.getWriter().append(']');
                } else {
                    context.writeExpr(context.getArgument(0));
                    writePropertyAccessor(context, context.getArgument(1));
                }
                break;
            case "put":
                context.getWriter().append('(');
                if (methodRef.getDescriptor().parameterType(1) == ValueType.INTEGER) {
                    context.writeExpr(context.getArgument(0));
                    context.getWriter().append('[');
                    context.writeExpr(context.getArgument(1));
                } else {
                    context.writeExpr(context.getArgument(0));
                    writePropertyAccessor(context, context.getArgument(1));
                }
                context.getWriter().ws().append('=').ws();
                context.writeExpr(context.getArgument(2));
                context.getWriter().append(')');
                break;
            case "add":
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(".push(");
                context.writeExpr(context.getArgument(1));
                context.getWriter().append(')');
                break;
            case "has":
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(".hasOwnProperty(");
                writeStringExpr(context, context.getArgument(1));
                context.getWriter().append(')');
                break;
            case "size":
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(".length");
                break;
            case "castToInt":
            case "castToShort":
            case "castToByte":
            case "castToBoolean":
            case "castToFloat":
            case "castToDouble":
            case "castFromInt":
            case "castFromShort":
            case "castFromByte":
            case "castFromBoolean":
            case "castFromFloat":
            case "castFromDouble":
                context.writeExpr(context.getArgument(0));
                break;
            case "castToString":
                context.getWriter().append('(');
                context.writeExpr(context.getArgument(0));
                context.getWriter().ws().append("!==").ws().append("null").ws().append("?").ws();
                context.getWriter().append("$rt_str(");
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(")").ws().append(':').ws().append("null)");
                break;
            case "castFromString":
                context.getWriter().append('(');
                context.writeExpr(context.getArgument(0));
                context.getWriter().ws().append("!==").ws().append("null").ws().append("?").ws();
                context.getWriter().append("$rt_ustr(");
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(")").ws().append(':').ws().append("null)");
                break;
        }
    }

    private void writePropertyAccessor(InjectorContext context, Expr property) throws IOException {
        if (property instanceof ConstantExpr) {
            String str = (String) ((ConstantExpr) property).getValue();
            if (str.isEmpty()) {
                context.getWriter().append("[\"\"]");
                return;
            }
            if (isValidIndentifier(str)) {
                context.getWriter().append(".").append(str);
                return;
            }
        }
        context.getWriter().append("[$rt_ustr(");
        context.writeExpr(property);
        context.getWriter().append(")]");
    }

    private void writeStringExpr(InjectorContext context, Expr expr) throws IOException {
        if (expr instanceof ConstantExpr) {
            String str = (String) ((ConstantExpr) expr).getValue();
            context.getWriter().append('"');
            context.writeEscaped(str);
            context.getWriter().append('"');
            return;
        }
        context.getWriter().append("$rt_ustr(");
        context.writeExpr(expr);
        context.getWriter().append(")");
    }

    private boolean isValidIndentifier(String str) {
        if (!Character.isJavaIdentifierStart(str.charAt(0))) {
            return false;
        }
        for (int i = 1; i < str.length(); ++i) {
            if (!Character.isJavaIdentifierPart(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
