/*
 *  Copyright 2025 Alexey Andreev.
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

import java.util.function.Consumer;
import org.teavm.backend.wasm.intrinsics.WasmGCBodyIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmGCCodeGenContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.model.MethodReference;

public class WasmAsyncTestGenerator implements WasmGCBodyIntrinsic {
    private WasmGCCodeGenContext context;

    public WasmAsyncTestGenerator(WasmGCCodeGenContext context) {
        this.context = context;
    }

    @Override
    public void apply(MethodReference method, WasmFunction function) {
        switch (method.getName()) {
            case "generatedMethod":
                new Generator(addParam(function)).generate(function.getBody().builder());
                break;
            case "loopAfterBranch":
                generateLoopAfterBranch(function, addParam(function));
                break;
            case "loopAfterThrow":
                generateLoopAfterThrow(function, addParam(function));
                break;
            case "teeThenSuspend":
                generateTeeThenSuspend(function);
                break;
            case "useAfterSuspend":
                addParam(function);
                generateUseAfterSuspend(function);
                break;
            default:
                break;
        }
    }

    private static WasmLocal addParam(WasmFunction function) {
        var param = new WasmLocal(WasmType.INT32, "n");
        function.add(param);
        return param;
    }

    // A suspending loop that follows a block whose body ends with a branch. The branch empties the
    // type stack, so a stale depth recorded by an earlier instruction indexes an empty snapshot.
    private void generateLoopAfterBranch(WasmFunction function, WasmLocal param) {
        var outer = function.getBody().builder().block(WasmType.INT32);
        escapeIfNonZero(outer, param);
        outer.block()
                .i32Const(1)
                .i32Const(2)
                .call(sumFn(), true)
                .i32Const(7)
                .breakTo(outer.list);
        suspendingLoop(outer);
        outer.i32Const(0);
    }

    // Same shape, with the type stack emptied by throw instead of by a branch. This is what Kotlin
    // coroutine state machines produce.
    private void generateLoopAfterThrow(WasmFunction function, WasmLocal param) {
        var outer = function.getBody().builder().block(WasmType.INT32);
        escapeIfNonZero(outer, param);
        outer.block()
                .i32Const(1)
                .i32Const(2)
                .call(sumFn(), true)
                .drop()
                .call(newExceptionFn(), false)
                .throw_(context.exceptionTag());
        suspendingLoop(outer);
        outer.i32Const(0);
    }
    
    private void generateTeeThenSuspend(WasmFunction function) {
        var throwableType = context.classInfoProvider().getClassInfo("java.lang.Throwable").getType();
        var wider = new WasmLocal(throwableType, "wider");
        var sum = new WasmLocal(WasmType.INT32, "sum");
        function.add(wider);
        function.add(sum);
        
        function.getBody().builder()
                .call(newExceptionFn(), false)
                .teeLocal(wider)
                .i32Const(20)
                .i32Const(25)
                .call(sumFn(), true)
                .setLocal(sum)
                .call(lengthOfFn(), false)
                .getLocal(sum)
                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD);
    }

    private void escapeIfNonZero(WasmInstructionBuilder outer, WasmLocal param) {
        outer.getLocal(param);
        outer.conditional().getThenBlock().builder()
                .i32Const(100)
                .breakTo(outer.list);
    }

    private void suspendingLoop(WasmInstructionBuilder builder) {
        builder.loop()
                .i32Const(1)
                .i32Const(2)
                .call(sumFn(), true)
                .drop();
    }

    private WasmFunction sumFn() {
        return context.functions().forStaticMethod(new MethodReference(WasmAsyncTest.class, "sum", int.class,
                int.class, int.class));
    }

    private void generateUseAfterSuspend(WasmFunction function) {
        var stringType = (WasmType.Reference) context.classInfoProvider()
                .getClassInfo("java.lang.String").getType();
        var value = new WasmLocal(stringType.asNonNull(), "value");
        function.add(value);
        function.getBody().builder()
                .call(newStringFn())
                .cast(stringType.asNonNull())
                .setLocal(value)
                .i32Const(1)
                .i32Const(2)
                .call(sumFn(), true)
                .drop()
                .getLocal(value)
                .call(tagFn());
    }

    private WasmFunction newExceptionFn() {
        return context.functions().forStaticMethod(new MethodReference(WasmAsyncTest.class, "newException",
                RuntimeException.class));
    }

    private WasmFunction lengthOfFn() {
        return context.functions().forStaticMethod(new MethodReference(WasmAsyncTest.class, "lengthOf",
                Throwable.class, int.class));
    }

    private WasmFunction newStringFn() {
        return context.functions().forStaticMethod(new MethodReference(WasmAsyncTest.class, "newString",
                String.class));
    }

    private WasmFunction tagFn() {
        return context.functions().forStaticMethod(new MethodReference(WasmAsyncTest.class, "tag", Object.class,
                int.class));
    }

    private class Generator {
        final WasmLocal param;

        Generator(WasmLocal param) {
            this.param = param;
        }

        void generate(WasmInstructionBuilder builder) {
            builder.i32Const(1);
            block(builder, b1 -> {
                b1.i32Const(10);
                block(b1, b2 -> {
                    b2.i32Const(100);
                    block(b2, b3 -> {
                        b3
                                .i32Const(1000)
                                .i32Const(1)
                                .getLocal(param)
                                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ)
                                .branch(b1)
                                .drop()
                                .i32Const(2000)
                                .i32Const(2)
                                .getLocal(param)
                                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ)
                                .branch(b2)
                                .drop()
                                .i32Const(3000)
                                .i32Const(3)
                                .getLocal(param)
                                .intBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ)
                                .branch(b3)
                                .drop()
                                .i32Const(4000);
                    });
                    b2.call(sumFn(), true);
                });
                b1.call(sumFn(), true);
            });
            builder.intBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD);
        }

        private void block(WasmInstructionBuilder builder, Consumer<WasmInstructionBuilder> body) {
            var block = builder.block(WasmType.INT32);
            body.accept(block);
        }
    }
}
