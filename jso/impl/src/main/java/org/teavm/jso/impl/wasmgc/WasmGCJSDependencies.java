/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.jso.impl.wasmgc;

import static org.teavm.jso.impl.JSMethods.JS_OBJECT;
import static org.teavm.jso.impl.JSMethods.JS_WRAPPER_CLASS;
import static org.teavm.jso.impl.JSMethods.OBJECT;
import static org.teavm.jso.impl.JSMethods.WASM_GC_JS_RUNTIME_CLASS;
import static org.teavm.jso.impl.wasmgc.WasmGCJSConstants.JS_TO_STRING;
import static org.teavm.jso.impl.wasmgc.WasmGCJSConstants.STRING_TO_JS;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.MethodDependency;
import org.teavm.jso.impl.JS;
import org.teavm.jso.impl.JSBodyDelegate;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class WasmGCJSDependencies extends AbstractDependencyListener {
    @Override
    public void started(DependencyAgent agent) {
        reachUtilities(agent);
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        var methodReader = method.getMethod();
        if (methodReader.getOwnerName().equals(JS.class.getName())) {
            switch (methodReader.getName()) {
                case "jsArrayItem":
                    method.getVariable(1).getArrayItem().connect(method.getResult());
                    break;
                case "invoke":
                case "apply":
                case "construct":
                case "get":
                case "getPure":
                case "set":
                case "setPure":
                case "global":
                    method.getThrown().propagate(agent.getType(WasmGCExceptionWrapper.class.getName()));
                    break;
            }
        } else if (methodReader.getOwnerName().equals(JS_WRAPPER_CLASS)) {
            if (methodReader.getName().equals("wrap")) {
                agent.linkMethod(new MethodReference(JS_WRAPPER_CLASS, "createWrapper", JS_OBJECT, OBJECT))
                        .use();
            }
        } else if (methodReader.getAnnotations().get(JSBodyDelegate.class.getName()) != null) {
            method.getThrown().propagate(agent.getType(WasmGCExceptionWrapper.class.getName()));
        } else if (methodReader.getOwnerName().equals(WasmGCJSRuntime.CharArrayData.class.getName())) {
            if (method.getMethod().getName().equals("asString")) {
                method.getResult().propagate(agent.getType(String.class.getName()));
                agent.linkMethod(new MethodReference(String.class, "fromArray", char[].class, String.class)).use();
            }
        }
    }

    private void reachUtilities(DependencyAgent agent) {
        agent.linkMethod(STRING_TO_JS)
                .propagate(1, agent.getType("java.lang.String"))
                .use();

        var jsToString = agent.linkMethod(JS_TO_STRING);
        jsToString.getResult().propagate(agent.getType("java.lang.String"));
        jsToString.use();

        agent.linkMethod(new MethodReference(WASM_GC_JS_RUNTIME_CLASS, "wrapException", JS_OBJECT,
                ValueType.object("java.lang.Throwable"))).use();
        agent.linkMethod(new MethodReference(WASM_GC_JS_RUNTIME_CLASS, "extractException",
                ValueType.object("java.lang.Throwable"), JS_OBJECT)).use();
    }
}
