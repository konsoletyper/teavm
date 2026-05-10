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
package org.teavm.backend.wasm.intrinsics;

import org.teavm.ast.InvocationExpr;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.runtime.StringInternPool;
import org.teavm.model.MethodReference;

public class StringIntrinsic implements WasmGCInlineIntrinsic {
    private WasmGCCodeGenContext codeGenContext;

    public StringIntrinsic(WasmGCCodeGenContext codeGenContext) {
        this.codeGenContext = codeGenContext;
    }

    @Override
    public void apply(InvocationExpr invocation, WasmGCInlineIntrinsicContext context, WasmInstructionBuilder builder) {
        var worker = codeGenContext.functions().forStaticMethod(new MethodReference(StringInternPool.class,
                "query", String.class, String.class));
        context.generate(builder, invocation.getArguments().get(0));
        builder.call(worker);
    }
}
