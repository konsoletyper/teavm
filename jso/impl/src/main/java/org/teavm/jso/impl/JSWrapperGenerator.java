/*
 *  Copyright 2023 konsoletyper.
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
package org.teavm.jso.impl;

import org.teavm.backend.javascript.rendering.Precedence;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.DependencyNode;
import org.teavm.dependency.DependencyPlugin;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.MethodReference;

public class JSWrapperGenerator implements Injector, DependencyPlugin {
    private DependencyNode externalClassesNode;

    @Override
    public void generate(InjectorContext context, MethodReference methodRef) {
        switch (methodRef.getName()) {
            case "directJavaToJs":
            case "directJsToJava":
            case "dependencyJavaToJs":
            case "dependencyJsToJava":
            case "wrapperToJs":
            case "jsToWrapper":
                context.writeExpr(context.getArgument(0), context.getPrecedence());
                break;
            case "isJava":
                if (context.getPrecedence().ordinal() >= Precedence.COMPARISON.ordinal()) {
                    context.getWriter().append("(");
                }
                context.writeExpr(context.getArgument(0));
                context.getWriter().append(" instanceof ").appendFunction("$rt_objcls").append("()");
                if (context.getPrecedence().ordinal() >= Precedence.COMPARISON.ordinal()) {
                    context.getWriter().append(")");
                }
                break;
            case "isJSImplementation":
                if (context.getPrecedence().ordinal() >= Precedence.EQUALITY.ordinal()) {
                    context.getWriter().append("(");
                }
                context.writeExpr(context.getArgument(0), Precedence.MEMBER_ACCESS);
                context.getWriter().append("[").appendFunction("$rt_jso_marker").append("]")
                        .ws().append("===").ws().append("true");
                if (context.getPrecedence().ordinal() >= Precedence.EQUALITY.ordinal()) {
                    context.getWriter().append(")");
                }
                break;
        }
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        switch (method.getMethod().getName()) {
            case "jsToWrapper":
                method.getResult().propagate(agent.getType(JSWrapper.class.getName()));
                break;
            case "dependencyJavaToJs":
                method.getVariable(1).connect(getExternalClassesNode(agent));
                break;
            case "dependencyJsToJava":
                getExternalClassesNode(agent).connect(method.getResult());
                break;
        }
    }

    private DependencyNode getExternalClassesNode(DependencyAgent agent) {
        if (externalClassesNode == null) {
            externalClassesNode = agent.createNode();
        }
        return externalClassesNode;
    }
}
