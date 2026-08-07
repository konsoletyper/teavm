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

public class WasmFlattenedConditionalGenerator implements WasmGCBodyIntrinsic {
    private WasmGCCodeGenContext context;

    public WasmFlattenedConditionalGenerator(WasmGCCodeGenContext context) {
        this.context = context;
    }

    @Override
    public void apply(MethodReference method, WasmFunction function) {
        if (!method.getName().equals("conditionalThenSuspend")) {
            return;
        }

        var param = new WasmLocal(WasmType.INT32, "n");
        var sum = new WasmLocal(WasmType.INT32, "sum");
        function.add(param);
        function.add(sum);

        generate(function.getBody().builder(), param, sum);
    }

    private void generate(WasmInstructionBuilder builder, WasmLocal param, WasmLocal sum) {
        var objectType = context.classInfoProvider().getClassInfo("java.lang.Object").getType();

        builder.getLocal(param);
        var conditional = builder.conditional(objectType);

        // the suspension point in here is what makes the transformation flatten the conditional
        conditional.getThenBlock().builder()
                .i32Const(1)
                .i32Const(2)
                .call(sumFn(), true)
                .drop()
                .call(newObjectFn(), false);

        // the arm walked last, and the one that pushes a subclass of the declared Object
        conditional.getElseBlock().builder()
                .call(newStringFn(), false);

        // a second suspension point with the conditional's value still beneath it on the stack
        builder
                .i32Const(3)
                .i32Const(4)
                .call(sumFn(), true)
                .setLocal(sum)
                .call(tagFn(), false)
                .getLocal(sum)
                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD);
    }

    private WasmFunction sumFn() {
        return context.functions().forStaticMethod(new MethodReference(WasmFlattenedConditionalTest.class, "sum",
                int.class, int.class, int.class));
    }

    private WasmFunction newObjectFn() {
        return context.functions().forStaticMethod(new MethodReference(WasmFlattenedConditionalTest.class,
                "newObject", Object.class));
    }

    private WasmFunction newStringFn() {
        return context.functions().forStaticMethod(new MethodReference(WasmFlattenedConditionalTest.class,
                "newString", String.class));
    }

    private WasmFunction tagFn() {
        return context.functions().forStaticMethod(new MethodReference(WasmFlattenedConditionalTest.class, "tag",
                Object.class, int.class));
    }
}
