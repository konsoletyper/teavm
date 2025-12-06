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

import java.util.List;
import java.util.function.Function;
import org.teavm.backend.wasm.generators.gc.WasmGCCustomGenerator;
import org.teavm.backend.wasm.generators.gc.WasmGCCustomGeneratorContext;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmDrop;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmPop;
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
            function.getBody().add(generator.generate());
        }
    }

    private static class Generator {
        final WasmGCCustomGeneratorContext context;
        final WasmLocal param;

        Generator(WasmGCCustomGeneratorContext context, WasmLocal param) {
            this.context = context;
            this.param = param;
        }

        WasmExpression generate() {
            return sum(
                    cst(1),
                    block(b1 -> List.of(
                            callSum(
                                    cst(10),
                                    block(b2 -> List.of(
                                            callSum(
                                                    cst(100),
                                                    block(b3 -> List.of(
                                                            cst(1000),
                                                            breakIf(b1, isEqual(cst(1), param())),
                                                            drop(),
                                                            cst(2000),
                                                            breakIf(b2, isEqual(cst(2), param())),
                                                            drop(),
                                                            cst(3000),
                                                            breakIf(b3, isEqual(cst(3), param())),
                                                            drop(),
                                                            cst(4000)
                                                    ))
                                            )
                                    ))
                            )
                    ))
            );
        }

        private WasmExpression callSum(WasmExpression a, WasmExpression b) {
            var fn = context.functions().forStaticMethod(new MethodReference(WasmAsyncTest.class, "sum", int.class,
                    int.class, int.class));
            var result = new WasmCall(fn, a, b);
            result.setSuspensionPoint(true);
            return result;
        }

        private WasmExpression sum(WasmExpression a, WasmExpression b) {
            return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.ADD, a, b);
        }

        private WasmExpression block(Function<WasmBlock, List<WasmExpression>> body) {
            var block = new WasmBlock(false);
            block.setType(WasmType.INT32.asBlock());
            block.getBody().addAll(body.apply(block));
            return block;
        }

        private WasmExpression breakIf(WasmBlock target, WasmExpression cond) {
            return new WasmBranch(cond, target);
        }

        private WasmExpression drop() {
            return new WasmDrop(new WasmPop(WasmType.INT32));
        }

        private WasmExpression param() {
            return new WasmGetLocal(param);
        }

        private WasmExpression isEqual(WasmExpression a, WasmExpression b) {
            return new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ, a, b);
        }

        private WasmExpression cst(int value) {
            return new WasmInt32Constant(value);
        }
    }
}
