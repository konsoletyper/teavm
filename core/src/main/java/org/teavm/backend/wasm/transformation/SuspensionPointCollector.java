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
package org.teavm.backend.wasm.transformation;

import java.util.HashSet;
import java.util.Set;
import org.teavm.backend.wasm.model.instruction.WasmDefaultInstructionVisitor;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;

public class SuspensionPointCollector extends WasmDefaultInstructionVisitor {
    private Set<WasmInstruction> suspending = new HashSet<>();

    public boolean isSuspending(WasmInstruction insn) {
        return suspending.contains(insn);
    }

    @Override
    public void visitDefault(WasmInstruction instruction) {
        var sizeBefore = suspending.size();
        super.visitDefault(instruction);
        if (instruction.isSuspend() || suspending.size() > sizeBefore) {
            suspending.add(instruction);
        }
    }
}
