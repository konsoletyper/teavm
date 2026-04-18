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

import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmGetLocalInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionBuilder;
import org.teavm.backend.wasm.model.instruction.WasmInt32ConstantInstruction;

public class ValueCache {
    private TemporaryVariablePool tmpVars;

    public ValueCache(TemporaryVariablePool tmpVars) {
        this.tmpVars = tmpVars;
    }

    public CachedValue create(WasmType type, WasmInstructionBuilder builder) {
        var insn = builder.list.getLast();
        if (insn instanceof WasmGetLocalInstruction) {
            var getLocal = (WasmGetLocalInstruction) insn;
            return new LocalVarCachedValue(getLocal.getLocal());
        } else if (insn instanceof WasmInt32ConstantInstruction) {
            var constExpr = (WasmInt32ConstantInstruction) insn;
            return new Int32CachedValue(constExpr.getValue());
        } else {
            var tmpVar = tmpVars.acquire(type);
            builder.teeLocal(tmpVar);
            return new TmpVarCachedExpression(tmpVar);
        }
    }

    private static class LocalVarCachedValue extends CachedValue {
        private final WasmLocal localVar;

        LocalVarCachedValue(WasmLocal localVar) {
            this.localVar = localVar;
        }

        @Override
        public void emit(WasmInstructionBuilder builder) {
            builder.getLocal(localVar);
        }
    }

    private class TmpVarCachedExpression extends CachedValue {
        private final WasmLocal tmpVar;

        TmpVarCachedExpression(WasmLocal tmpVar) {
            this.tmpVar = tmpVar;
        }

        @Override
        public void emit(WasmInstructionBuilder builder) {
            builder.getLocal(tmpVar);
        }

        @Override
        public void release() {
            tmpVars.release(tmpVar);
        }
    }

    private static class Int32CachedValue extends CachedValue {
        private final int value;

        Int32CachedValue(int value) {
            this.value = value;
        }

        @Override
        public void emit(WasmInstructionBuilder builder) {
            builder.i32Const(value);
        }
    }
}
