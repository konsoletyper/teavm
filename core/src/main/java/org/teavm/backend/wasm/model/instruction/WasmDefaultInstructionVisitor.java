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
package org.teavm.backend.wasm.model.instruction;

public class WasmDefaultInstructionVisitor implements WasmInstructionVisitor {
    @Override
    public void visit(WasmUnreachable instruction) {
    }

    @Override
    public void visit(WasmBlock instruction) {
        visitMany(instruction.getBody());
    }

    @Override
    public void visit(WasmConditional instruction) {
        visitMany(instruction.getThenBlock());
        visitMany(instruction.getElseBlock());
    }

    @Override
    public void visit(WasmBranch instruction) {
    }

    @Override
    public void visit(WasmNullBranch instruction) {
    }

    @Override
    public void visit(WasmCastBranch instruction) {
    }

    @Override
    public void visit(WasmBreak instruction) {
    }

    @Override
    public void visit(WasmSwitch instruction) {
    }

    @Override
    public void visit(WasmReturn instruction) {
    }

    @Override
    public void visit(WasmInt32Constant instruction) {
    }

    @Override
    public void visit(WasmInt64Constant instruction) {
    }

    @Override
    public void visit(WasmFloat32Constant instruction) {
    }

    @Override
    public void visit(WasmFloat64Constant instruction) {
    }

    @Override
    public void visit(WasmNullConstant instruction) {
    }

    @Override
    public void visit(WasmIsNull instruction) {
    }

    @Override
    public void visit(WasmGetLocal instruction) {
    }

    @Override
    public void visit(WasmSetLocal instruction) {
    }

    @Override
    public void visit(WasmTeeLocal instruction) {
    }

    @Override
    public void visit(WasmGetGlobal instruction) {
    }

    @Override
    public void visit(WasmSetGlobal instruction) {
    }

    @Override
    public void visit(WasmIntBinary instruction) {
    }

    @Override
    public void visit(WasmFloatBinary instruction) {
    }

    @Override
    public void visit(WasmIntUnary instruction) {
    }

    @Override
    public void visit(WasmFloatUnary instruction) {
    }

    @Override
    public void visit(WasmConversion instruction) {
    }

    @Override
    public void visit(WasmCall instruction) {
    }

    @Override
    public void visit(WasmIndirectCall instruction) {
    }

    @Override
    public void visit(WasmCallReference instruction) {
    }

    @Override
    public void visit(WasmDrop instruction) {
    }

    @Override
    public void visit(WasmLoadInt32 instruction) {
    }

    @Override
    public void visit(WasmLoadInt64 instruction) {
    }

    @Override
    public void visit(WasmLoadFloat32 instruction) {
    }

    @Override
    public void visit(WasmLoadFloat64 instruction) {
    }

    @Override
    public void visit(WasmStoreInt32 instruction) {
    }

    @Override
    public void visit(WasmStoreInt64 instruction) {
    }

    @Override
    public void visit(WasmStoreFloat32 instruction) {
    }

    @Override
    public void visit(WasmStoreFloat64 instruction) {
    }

    @Override
    public void visit(WasmMemoryGrow instruction) {
    }

    @Override
    public void visit(WasmFill instruction) {
    }

    @Override
    public void visit(WasmCopy instruction) {
    }

    @Override
    public void visit(WasmTry instruction) {
        visitMany(instruction.getBody());
    }

    @Override
    public void visit(WasmThrow instruction) {
    }

    @Override
    public void visit(WasmReferencesEqual instruction) {
    }

    @Override
    public void visit(WasmCast instruction) {
    }

    @Override
    public void visit(WasmTest instruction) {
    }

    @Override
    public void visit(WasmExternConversion instruction) {
    }

    @Override
    public void visit(WasmStructNew instruction) {
    }

    @Override
    public void visit(WasmStructNewDefault instruction) {
    }

    @Override
    public void visit(WasmStructGet instruction) {
    }

    @Override
    public void visit(WasmStructSet instruction) {
    }

    @Override
    public void visit(WasmArrayNewDefault instruction) {
    }

    @Override
    public void visit(WasmArrayNewFixed instruction) {
    }

    @Override
    public void visit(WasmArrayGet instruction) {
    }

    @Override
    public void visit(WasmArraySet instruction) {
    }

    @Override
    public void visit(WasmArrayLength instruction) {
    }

    @Override
    public void visit(WasmArrayCopy instruction) {
    }

    @Override
    public void visit(WasmFunctionReference instruction) {
    }

    @Override
    public void visit(WasmInt31Reference instruction) {
    }

    @Override
    public void visit(WasmInt31Get instruction) {
    }

    public void visitMany(Iterable<WasmInstruction> instructions) {
        for (var instruction : instructions) {
            visitDefault(instruction);
        }
    }

    public void visitDefault(WasmInstruction instruction) {
        instruction.acceptVisitor(this);
    }
}
