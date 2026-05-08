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
import org.teavm.backend.wasm.generators.WasmGCCustomGenerator;
import org.teavm.backend.wasm.generators.WasmGCCustomGeneratorContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.MethodDependency;
import org.teavm.model.MethodReference;

public class WasmAsyncTestGenerator extends AbstractDependencyListener implements WasmGCCustomGenerator {
    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        if (method.getMethod().getName().equals("generatedMethod")) {
            agent.linkMethod(new MethodReference(WasmAsyncTest.class, "sum", int.class, int.class, int.class)).use();
        }
    }

    @Override
    public void apply(MethodReference method, WasmFunction function, WasmGCCustomGeneratorContext context) {
        if (method.getName().equals("generatedMethod")) {
            var param = new WasmLocal(WasmType.INT32, "n");
            function.add(param);
            var generator = new Generator(context, param);
            generator.generate(function.getBody().builder());
        }
    }

    private static class Generator {
        final WasmGCCustomGeneratorContext context;
        final WasmLocal param;

        Generator(WasmGCCustomGeneratorContext context, WasmLocal param) {
            this.context = context;
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
        
        private WasmFunction sumFn() {
            return context.functions().forStaticMethod(new MethodReference(WasmAsyncTest.class, "sum", int.class,
                    int.class, int.class));
        }
    }
}
