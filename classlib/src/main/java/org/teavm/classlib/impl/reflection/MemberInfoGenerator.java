/*
 *  Copyright 2025 Alexey Andreev.
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
package org.teavm.classlib.impl.reflection;

import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.model.MethodReference;

public class MemberInfoGenerator implements Injector {
    @Override
    public void generate(InjectorContext context, MethodReference methodRef) {
        switch (methodRef.getName()) {
            case "name":
                context.getWriter().appendFunction("$rt_str").append("(");
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(".name)");
                break;
            case "modifiers":
            case "accessLevel":
            case "parameterTypes":
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(".").append(methodRef.getName());
                break;
            case "annotations":
            case "typeParameters":
                context.getWriter().appendFunction("$rt_undefinedAsNull").append("(");
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(".").append(methodRef.getName()).append(")");
                break;
            case "returnType":
            case "type":
                context.getWriter().appendFunction("$rt_cls").append("(");
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(".").append(methodRef.getName()).append(")");
                break;
            case "reader":
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(".getter");
                break;
            case "writer":
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(".setter");
                break;
            case "caller":
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(".callable");
                break;
        }
    }
}
