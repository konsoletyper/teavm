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
import org.teavm.model.FieldReference;
import org.teavm.model.MethodReference;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.RuntimeClass;

public class AllocatorIntrinsic implements Intrinsic {
    private static final FieldReference FLAGS_FIELD = new FieldReference(RuntimeClass.class.getName(), "flags");

    @Override
    public boolean canHandle(MethodReference method) {
        if (!method.getClassName().equals(Allocator.class.getName())) {
            return false;
        }

        switch (method.getName()) {
            case "fillZero":
            case "fill":
            case "moveMemoryBlock":
            case "isInitialized":
                return true;
            default:
                return false;
        }
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        switch (invocation.getMethod().getName()) {
            case "fillZero":
                context.includes().addInclude("<string.h>");
                context.writer().print("memset(");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(", 0, ");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(")");
                break;
            case "fill":
                context.includes().addInclude("<string.h>");
                context.writer().print("memset(");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(", ");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(", ");
                context.emit(invocation.getArguments().get(2));
                context.writer().print(")");
                break;
            case "moveMemoryBlock":
                context.includes().addInclude("<string.h>");
                context.writer().print("memmove(");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(", ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(", ");
                context.emit(invocation.getArguments().get(2));
                context.writer().print(")");
                break;
            case "isInitialized":
                context.writer().print("(((TeaVM_Class *) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")->").print(context.names().forMemberField(FLAGS_FIELD))
                        .print(" & INT32_C(" + RuntimeClass.INITIALIZED + "))");
                break;
        }
    }
}
