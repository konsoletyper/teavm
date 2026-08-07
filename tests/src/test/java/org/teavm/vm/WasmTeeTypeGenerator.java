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

public class WasmTeeTypeGenerator implements WasmGCBodyIntrinsic {
    private WasmGCCodeGenContext context;

    public WasmTeeTypeGenerator(WasmGCCodeGenContext context) {
        this.context = context;
    }

    @Override
    public void apply(MethodReference method, WasmFunction function) {
        if (!method.getName().equals("teeThenSuspend")) {
            return;
        }

        var throwableType = context.classInfoProvider().getClassInfo("java.lang.Throwable").getType();
        var wider = new WasmLocal(throwableType, "wider");
        var sum = new WasmLocal(WasmType.INT32, "sum");
        function.add(wider);
        function.add(sum);

        generate(function.getBody().builder(), wider, sum);
    }

    /*
     * An IllegalStateException is tee'd into a java.lang.Throwable local, so what local.tee leaves
     * on the stack is a Throwable. It is still there across the suspending call, which makes the
     * coroutine transformation record its type and rebuild it into a block signature on resume.
     */
    private void generate(WasmInstructionBuilder builder, WasmLocal wider, WasmLocal sum) {
        builder
                .call(newErrorFn(), false)
                .teeLocal(wider)
                .i32Const(20)
                .i32Const(25)
                .call(sumFn(), true)
                .setLocal(sum)
                .call(lengthOfFn(), false)
                .getLocal(sum)
                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD);
    }

    private WasmFunction sumFn() {
        return context.functions().forStaticMethod(new MethodReference(WasmTeeTypeTest.class, "sum", int.class,
                int.class, int.class));
    }

    private WasmFunction newErrorFn() {
        return context.functions().forStaticMethod(new MethodReference(WasmTeeTypeTest.class, "newError",
                IllegalStateException.class));
    }

    private WasmFunction lengthOfFn() {
        return context.functions().forStaticMethod(new MethodReference(WasmTeeTypeTest.class, "lengthOf",
                Throwable.class, int.class));
    }
}
