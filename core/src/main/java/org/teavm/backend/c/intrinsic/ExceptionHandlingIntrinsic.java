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
package org.teavm.backend.c.intrinsic;

import org.teavm.ast.InvocationExpr;
import org.teavm.model.MethodReference;
import org.teavm.runtime.ExceptionHandling;

public class ExceptionHandlingIntrinsic implements Intrinsic {
    @Override
    public boolean canHandle(MethodReference method) {
        if (!method.getClassName().equals(ExceptionHandling.class.getName())) {
            return false;
        }

        switch (method.getName()) {
            case "findCallSiteById":
            case "isJumpSupported":
            case "jumpToFrame":
            case "abort":
            case "isObfuscated":
                return true;
            default:
                return false;
        }
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        switch (invocation.getMethod().getName()) {
            case "findCallSiteById":
                context.writer().print("TEAVM_FIND_CALLSITE(");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(", ");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(")");
                break;

            case "isJumpSupported":
                context.writer().print("TEAVM_JUMP_SUPPORTED");
                break;

            case "jumpToFrame":
                context.writer().print("TEAVM_JUMP_TO_FRAME(");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(", ");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(")");
                break;

            case "abort":
                context.includes().addInclude("<stdlib.h>");
                context.writer().print("abort();");
                break;

            case "isObfuscated":
                context.writer().print("TEAVM_OBFUSCATED");
                break;
        }
    }
}
