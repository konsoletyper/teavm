/*
 *  Copyright 2024 konsoletyper.
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

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsic;
import org.teavm.backend.wasm.intrinsics.gc.WasmGCIntrinsicContext;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetGlobal;

class WasmGCJSRuntimeIntrinsic implements WasmGCIntrinsic {
    private WasmGCJsoCommonGenerator commonGen;

    WasmGCJSRuntimeIntrinsic(WasmGCJsoCommonGenerator commonGen) {
        this.commonGen = commonGen;
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmGCIntrinsicContext context) {
        var jsoContext = WasmGCJsoContext.wrap(context);
        var wrapperClass = commonGen.getDefaultWrapperClass(jsoContext);
        var wrapperFunction = commonGen.javaObjectToJSFunction(jsoContext);
        return new WasmCall(wrapperFunction, context.generate(invocation.getArguments().get(0)),
                new WasmGetGlobal(wrapperClass));
    }
}
