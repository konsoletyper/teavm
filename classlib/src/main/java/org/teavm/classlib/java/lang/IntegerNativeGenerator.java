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

import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.model.MethodReference;

public class IntegerNativeGenerator implements Injector {
    @Override
    public void generate(InjectorContext context, MethodReference methodRef) {
        switch (methodRef.getName()) {
            case "divideUnsigned":
                generateRuntimeCall(context, "$rt_udiv");
                break;
            case "remainderUnsigned":
                generateRuntimeCall(context, "$rt_umod");
                break;
            case "compareUnsigned":
                generateRuntimeCall(context, "$rt_ucmp");
                break;
        }
    }


    private void generateRuntimeCall(InjectorContext context, String name) {
        context.getWriter().appendFunction(name).append("(");
        context.writeExpr(context.getArgument(0));
        context.getWriter().append(",").ws();
        context.writeExpr(context.getArgument(1));
        context.getWriter().append(")");
    }
}
