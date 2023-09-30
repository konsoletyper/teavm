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
package org.teavm.classlib.java.lang;

import java.io.IOException;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Generator;
import org.teavm.backend.javascript.spi.GeneratorContext;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.MethodReference;
import org.teavm.model.PrimitiveType;
import org.teavm.model.ValueType;

public class IntegerNativeGenerator implements Injector, Generator, DependencyPlugin {
    @Override
    public void generate(InjectorContext context, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "divideUnsigned":
                context.getWriter().appendFunction("$rt_udiv").append("(");
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(",").ws();
                context.writeExpr(context.getArgument(1));
                context.getWriter().append(")");
                break;
            case "remainderUnsigned":
                context.getWriter().appendFunction("$rt_umod").append("(");
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(",").ws();
                context.writeExpr(context.getArgument(1));
                context.getWriter().append(")");
                break;
            case "compareUnsigned":
                context.getWriter().appendFunction("$rt_ucmp").append("(");
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(",").ws();
                context.writeExpr(context.getArgument(1));
                context.getWriter().append(")");
                break;
        }
    }

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        if (methodRef.getName().equals("toStringNative")) {
            if (methodRef.getSignature().length == 2) {
                writer.append("return").ws().appendFunction("$rt_str").append("(")
                        .append(context.getParameterName(1)).append(".toString());").softNewLine();
            } else {
                writer.append("return").ws().appendFunction("$rt_str")
                        .append("(").append(context.getParameterName(1))
                        .append(".toString(").append(context.getParameterName(2)).append("));").softNewLine();
            }
        }
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        if (method.getReference().getName().equals("toStringNative")
                && method.getReference().getSignature()[0].equals(ValueType.primitive(PrimitiveType.INTEGER))) {
            method.getResult().propagate(agent.getType("java.lang.String"));
        }
    }
}
