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
package org.teavm.backend.c.intrinsic;

import org.teavm.ast.InvocationExpr;
import org.teavm.model.MethodReference;

public class StringIntrinsic implements Intrinsic {
    @Override
    public boolean canHandle(MethodReference method) {
        return method.getClassName().equals(String.class.getName()) && method.getName().equals("intern");
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        var arg = invocation.getArguments().get(0);
        context.includes().includePath("stringhash.h");
        context.writer().print("((void*) teavm_internString((TeaVM_String*) (");
        context.emit(arg);
        context.writer().print(")))");
    }
}
