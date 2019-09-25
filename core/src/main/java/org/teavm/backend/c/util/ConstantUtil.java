/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.backend.c.util;

import org.teavm.ast.ConstantExpr;
import org.teavm.ast.Expr;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.c.intrinsic.IntrinsicContext;
import org.teavm.model.CallLocation;
import org.teavm.model.ValueType;

public final class ConstantUtil {
    private ConstantUtil() {
    }

    public static String getClassLiteral(IntrinsicContext context, InvocationExpr invocation, Expr expr) {
        if (expr instanceof ConstantExpr) {
            Object cst = ((ConstantExpr) expr).getValue();
            if (cst instanceof ValueType.Object) {
                return ((ValueType.Object) cst).getClassName();
            }
        }
        context.diagnotics().error(
                new CallLocation(context.callingMethod(), invocation.getLocation()),
                "This method should take class literal");
        return "java.lang.Object";
    }

    public static String getStringLiteral(IntrinsicContext context, InvocationExpr invocation, Expr expr) {
        if (expr instanceof ConstantExpr) {
            Object cst = ((ConstantExpr) expr).getValue();
            if (cst instanceof String) {
                return (String) cst;
            }
        }
        context.diagnotics().error(
                new CallLocation(context.callingMethod(), invocation.getLocation()),
                "This method should take string literal");
        return "";
    }
}
