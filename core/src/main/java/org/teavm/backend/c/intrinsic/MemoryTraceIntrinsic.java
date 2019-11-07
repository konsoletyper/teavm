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

import org.teavm.ast.InvocationExpr;
import org.teavm.model.MethodReference;
import org.teavm.runtime.MemoryTrace;

public class MemoryTraceIntrinsic implements Intrinsic {
    @Override
    public boolean canHandle(MethodReference method) {
        return method.getClassName().equals(MemoryTrace.class.getName());
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        context.includes().includePath("heaptrace.h");
        context.writer().print("teavm_gc_").print(invocation.getMethod().getName()).print("(");
        if (!invocation.getArguments().isEmpty()) {
            context.emit(invocation.getArguments().get(0));
            for (int i = 1; i < invocation.getArguments().size(); ++i) {
                context.writer().print(", ");
                context.emit(invocation.getArguments().get(i));
            }
        }
        context.writer().print(")");
    }
}
