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
package org.teavm.backend.javascript.intrinsics.reflection;

import org.teavm.backend.javascript.rendering.Precedence;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.model.MethodReference;

public class MethodInfoGenerator implements Injector {
    @Override
    public void generate(InjectorContext context, MethodReference methodRef) {
        switch (methodRef.getName()) {
            case "name":
            case "modifiers":
            case "returnType":
            case "reflection":
                context.writeExpr(context.getArgument(0), Precedence.MEMBER_ACCESS);
                context.getWriter().append(".").append(methodRef.getName());
                break;
            case "call":
                context.getWriter().appendFunction("$rt_callMethod").append("(");
                context.writeExpr(context.getArgument(0), Precedence.min());
                context.getWriter().append(",").ws();
                context.writeExpr(context.getArgument(1), Precedence.min());
                context.getWriter().append(",").ws();
                context.writeExpr(context.getArgument(2), Precedence.min());
                context.getWriter().append(")");
                break;
            case "parameterCount":
                context.writeExpr(context.getArgument(0), Precedence.MEMBER_ACCESS);
                context.getWriter().append(".parameterTypes.length");
                break;
            case "parameterType":
                context.writeExpr(context.getArgument(0), Precedence.MEMBER_ACCESS);
                context.getWriter().append(".parameterTypes[");
                context.writeExpr(context.getArgument(1), Precedence.min());
                context.getWriter().append("]");
                break;
        }
    }
}
