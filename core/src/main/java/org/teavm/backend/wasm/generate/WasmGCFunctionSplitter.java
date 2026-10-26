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
package org.teavm.backend.wasm.generate;

import org.teavm.backend.wasm.WasmFunctionTypes;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;

/**
 * Distributes the statements of a generated function over several part functions, which the function calls in
 * order. V8 rejects a function body larger than 7,654,321 bytes. On arm64, V8 also stops the process when the
 * machine code of one function needs more than half of its code space. The start function and
 * {@code teavm@initReflection} grow with the number of classes, so a large program can exceed these limits.
 */
public class WasmGCFunctionSplitter {
    private static final int MAX_INSTRUCTIONS_PER_PART = 10000;
    private final WasmFunction function;
    private final WasmModule module;
    private final WasmFunctionTypes functionTypes;
    private final WasmGCNameProvider names;
    private WasmFunction part;
    private WasmInstruction lastCounted;
    private int instructionCount;

    public WasmGCFunctionSplitter(WasmFunction function, WasmModule module, WasmFunctionTypes functionTypes,
            WasmGCNameProvider names) {
        this.function = function;
        this.module = module;
        this.functionTypes = functionTypes;
        this.names = names;
    }

    /**
     * Returns the builder for the next statement. The statement must leave the operand stack empty.
     */
    public WasmInstructionBuilder next() {
        if (part != null) {
            var insn = lastCounted != null ? lastCounted.getNext() : part.getBody().getFirst();
            for (; insn != null; insn = insn.getNext()) {
                ++instructionCount;
            }
            lastCounted = part.getBody().getLast();
        }
        if (part == null || instructionCount >= MAX_INSTRUCTIONS_PER_PART) {
            part = new WasmFunction(functionTypes.of(null));
            part.setName(names.topLevel(function.getName() + "$part"));
            module.functions.add(part);
            function.getBody().builder().call(part);
            lastCounted = null;
            instructionCount = 0;
        }
        return part.getBody().builder();
    }
}
