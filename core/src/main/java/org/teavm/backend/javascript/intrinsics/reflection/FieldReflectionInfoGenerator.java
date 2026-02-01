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

public class FieldReflectionInfoGenerator implements Injector {
    @Override
    public void generate(InjectorContext context, MethodReference methodRef) {
        switch (methodRef.getName()) {
            case "annotationCount":
                context.writeExpr(context.getArgument(0), Precedence.MEMBER_ACCESS);
                context.getWriter().append(".annotations.length");
                break;
            case "annotation":
                context.writeExpr(context.getArgument(0), Precedence.MEMBER_ACCESS);
                context.getWriter().append(".annotations[");
                context.writeExpr(context.getArgument(1), Precedence.min());
                context.getWriter().append("]");
                break;
            case "genericType":
                context.writeExpr(context.getArgument(0), Precedence.MEMBER_ACCESS);
                context.getWriter().append(".genericType");
                break;
        }
    }
}
