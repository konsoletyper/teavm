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
package org.teavm.backend.wasm.intrinsics.reflection;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.teavm.backend.wasm.intrinsics.WasmGCCodeGenContext;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.instruction.WasmNullConstant;
import org.teavm.model.MethodReference;

public class ProxyIntrinsicContext {
    private WasmGCCodeGenContext genContext;
    private Map<MethodReference, WasmGlobal> globals = new HashMap<>();

    public ProxyIntrinsicContext(WasmGCCodeGenContext genContext) {
        this.genContext = genContext;
    }

    public WasmGlobal getMethodGlobal(MethodReference methodRef) {
        return globals.computeIfAbsent(methodRef, ref -> {
            var name = genContext.names().topLevel("teavm.proxy:" + genContext.names().suggestForMethod(ref));
            var methodInfo = genContext.classInfoProvider().getClassInfo(Method.class.getName());
            var global = new WasmGlobal(name, methodInfo.getType());
            global.getInitialValue().add(new WasmNullConstant(methodInfo.getStructure().getReference()));
            genContext.module().globals.add(global);
            return global;
        });
    }
}
