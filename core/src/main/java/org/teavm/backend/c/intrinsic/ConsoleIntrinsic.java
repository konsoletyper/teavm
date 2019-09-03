/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.backend.c.intrinsic;

import org.teavm.ast.ConstantExpr;
import org.teavm.ast.Expr;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.c.generate.StringPoolGenerator;
import org.teavm.model.MethodReference;
import org.teavm.runtime.Console;

public class ConsoleIntrinsic implements Intrinsic {
    @Override
    public boolean canHandle(MethodReference method) {
        if (!method.getClassName().equals(Console.class.getName())) {
            return false;
        }

        return method.getName().equals("printString");
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        switch (invocation.getMethod().getName()) {
            case "printString": {
                context.includes().includePath("log.h");
                context.writer().print("teavm_printString(");
                Expr arg = invocation.getArguments().get(0);
                String literal = extractStringConstant(arg);
                if (literal != null) {
                    context.writer().print("u");
                    StringPoolGenerator.generateSimpleStringLiteral(context.writer(), literal);
                } else {
                    context.includes().includePath("string.h");
                    context.writer().print("teavm_stringToC16(");
                    context.emit(arg);
                    context.writer().print(")");
                }
                context.writer().print(")");
                break;
            }
        }
    }

    private String extractStringConstant(Expr expr) {
        if (!(expr instanceof ConstantExpr)) {
            return null;
        }

        Object value = ((ConstantExpr) expr).getValue();
        return value instanceof String ? (String) value : null;
    }
}
