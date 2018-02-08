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
import org.teavm.runtime.ShadowStack;

public class ShadowStackIntrinsic implements Intrinsic {
    @Override
    public boolean canHandle(MethodReference method) {
        if (!method.getClassName().equals(ShadowStack.class.getName())) {
            return false;
        }

        switch (method.getName()) {
            case "allocStack":
            case "releaseStack":
            case "registerGCRoot":
            case "removeGCRoot":
            case "registerCallSite":
            case "getExceptionHandlerId":
            case "setExceptionHandlerId":
            case "getStackTop":
            case "getNextStackFrame":
            case "getStackRootCount":
            case "getStackRootPointer":
            case "getCallSiteId":
                return true;
            default:
                return false;
        }
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        switch (invocation.getMethod().getName()) {
            case "allocStack":
                context.writer().print("ALLOC_STACK");
                break;
            case "releaseStack":
                context.writer().print("RELEASE_STACK");
                return;
            case "registerGCRoot":
                context.writer().print("GC_ROOT");
                break;
            case "removeGCRoot":
                context.writer().print("GC_ROOT_RELEASE");
                break;
            case "registerCallSite":
                context.writer().print("CALL_SITE");
                break;
            case "getExceptionHandlerId":
                context.writer().print("EXCEPTION_HANDLER");
                return;
            case "setExceptionHandlerId":
                context.writer().print("SET_EXCEPTION_HANDLER");
                break;
            case "getStackTop":
                context.writer().print("stackTop");
                return;
            case "getNextStackFrame":
                context.writer().print("((void**) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")[0]");
                return;
            case "getStackRootCount":
                context.writer().print("((int32_t) (intptr_t) ((void**) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")[2])");
                return;
            case "getStackRootPointer":
                context.writer().print("&((void**) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")[3]");
                return;
            case "getCallSiteId":
                context.writer().print("((int32_t) (intptr_t) ((void**) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")[1])");
                return;
        }

        context.writer().print("(");
        for (int i = 0; i < invocation.getArguments().size(); ++i) {
            if (i > 0) {
                context.writer().print(", ");
            }
            context.emit(invocation.getArguments().get(i));
        }
        context.writer().print(")");
    }
}
