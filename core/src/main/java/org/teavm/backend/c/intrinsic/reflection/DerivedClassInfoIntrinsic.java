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
import org.teavm.runtime.reflect.DerivedClassInfo;

public class DerivedClassInfoIntrinsic implements Intrinsic {
    @Override
    public boolean canHandle(MethodReference method) {
        return method.getClassName().equals(DerivedClassInfo.class.getName());
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        switch (invocation.getMethod().getName()) {
            case "classInfo":
                context.includes().includePath("reflection.h");
                context.writer().print("((TeaVM_ClassPtr*) (");
                context.emit(invocation.getArguments().get(0));
                context.writer().print("))->baseClass");
                break;
            case "arrayDegree":
                context.includes().includePath("reflection.h");
                context.writer().print("((TeaVM_ClassPtr*) (");
                context.emit(invocation.getArguments().get(0));
                context.writer().print("))->arrayDegree");
                break;
        }
    }
}
