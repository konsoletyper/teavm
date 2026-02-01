/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.backend.c.intrinsic.reflection;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.c.intrinsic.Intrinsic;
import org.teavm.backend.c.intrinsic.IntrinsicContext;
import org.teavm.model.MethodReference;
import org.teavm.runtime.reflect.FieldInfo;

public class FieldInfoIntrinsic implements Intrinsic {
    @Override
    public boolean canHandle(MethodReference method) {
        return method.getClassName().equals(FieldInfo.class.getName());
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        switch (invocation.getMethod().getName()) {
            case "name":
            case "modifiers":
                context.includes().includePath("reflection.h");
                context.writer().print("((TeaVM_FieldInfo*) (");
                context.emit(invocation.getArguments().get(0));
                context.writer().print("))->").print(invocation.getMethod().getName());
                break;
            case "type":
                context.includes().includePath("reflection.h");
                context.writer().print("(&((TeaVM_FieldInfo*) (");
                context.emit(invocation.getArguments().get(0));
                context.writer().print("))->type)");
                break;
            case "read":
                context.includes().includePath("reflection.h");
                context.writer().print("teavm_reflection_readField(");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(", (TeaVM_FieldInfo*)");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(")");
                break;
            case "write":
                context.includes().includePath("reflection.h");
                context.writer().print("teavm_reflection_writeField(");
                context.emit(invocation.getArguments().get(1));
                context.writer().print(", (TeaVM_FieldInfo*) ");
                context.emit(invocation.getArguments().get(0));
                context.writer().print(", ");
                context.emit(invocation.getArguments().get(2));
                context.writer().print(")");
                break;
            default:
                throw new IllegalArgumentException(invocation.getMethod().getName());
        }
    }
}
