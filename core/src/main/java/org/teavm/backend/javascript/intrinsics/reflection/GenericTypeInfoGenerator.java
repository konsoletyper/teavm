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

public class GenericTypeInfoGenerator implements Injector {
    @Override
    public void generate(InjectorContext context, MethodReference methodRef) {
        switch (methodRef.getDescriptor().getName()) {
            case "kind":
            case "rawType":
            case "ownerType":
            case "level":
            case "index":
            case "itemType":
            case "bound":
                context.writeExpr(context.getArgument(0), Precedence.MEMBER_ACCESS);
                context.getWriter().append(".").append(methodRef.getDescriptor().getName());
                break;
            case "actualTypeArgumentCount":
                context.writeExpr(context.getArgument(0), Precedence.MEMBER_ACCESS);
                context.getWriter().append(".actualTypeArguments.length");
                break;
            case "actualTypeArgument":
                context.writeExpr(context.getArgument(0), Precedence.MEMBER_ACCESS);
                context.getWriter().append(".actualTypeArguments[");
                context.writeExpr(context.getArgument(1), Precedence.min());
                context.getWriter().append("]");
                break;
            case "asParameterizedType":
            case "asTypeVariable":
            case "asGenericArray":
            case "asWildcard":
            case "asRawType":
                context.writeExpr(context.getArgument(0), context.getPrecedence());
                break;
        }
    }
}
