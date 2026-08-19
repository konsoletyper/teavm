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
package org.teavm.vm;

import org.teavm.backend.wasm.intrinsics.WasmGCBodyIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCCodeGenContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.model.MethodReference;

public class WasmTeeLocalGenerator implements WasmGCBodyIntrinsic {
    private WasmGCCodeGenContext context;

    public WasmTeeLocalGenerator(WasmGCCodeGenContext context) {
        this.context = context;
    }

    @Override
    public void apply(MethodReference method, WasmFunction function) {
        if (!method.getName().equals("teeThenSuspend")) {
            return;
        }

        var param = new WasmLocal(WasmType.INT32, "n");
        var obj = new WasmLocal(context.classInfoProvider().getClassInfo("java.lang.Object").getType(), "obj");
        function.add(param);
        function.add(obj);

        generate(function.getBody().builder(), obj);
    }

    private void generate(WasmInstructionBuilder builder, WasmLocal obj) {
        // a String teed into an Object local stays on the stack at the local's type across the
        // suspension point, so the snapshot taken there must record the local's type
        builder
                .call(newStringFn(), false)
                .teeLocal(obj)
                .i32Const(1)
                .i32Const(2)
                .call(sumFn(), true)
                .drop()
                .call(tagFn(), false)
                .getLocal(obj)
                .call(tagFn(), false)
                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD);
    }

    private WasmFunction sumFn() {
        return context.functions().forStaticMethod(new MethodReference(WasmTeeLocalTest.class, "sum",
                int.class, int.class, int.class));
    }

    private WasmFunction newStringFn() {
        return context.functions().forStaticMethod(new MethodReference(WasmTeeLocalTest.class,
                "newString", String.class));
    }

    private WasmFunction tagFn() {
        return context.functions().forStaticMethod(new MethodReference(WasmTeeLocalTest.class, "tag",
                Object.class, int.class));
    }
}
