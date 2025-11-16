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

public class ClassListGenerator implements Injector {
    @Override
    public void generate(InjectorContext context, MethodReference methodRef) {
        switch (methodRef.getDescriptor().getName()) {
            case "count":
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(".length");
                break;
            case "get":
                context.getWriter().appendFunction("$rt_cls").append("(");
                context.writeExpr(context.getArgument(0));
                context.getWriter().append("[");
                context.writeExpr(context.getArgument(1));
                context.getWriter().append("])");
                break;
        }
    }
}
