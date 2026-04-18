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
    public void visit(WasmUnreachableInstruction instruction) {
    }

    @Override
    public void visit(WasmBlockInstruction instruction) {
        visitMany(instruction.getBody());
    }

    @Override
    public void visit(WasmConditionalInstruction instruction) {
        visitMany(instruction.getThenBlock());
        visitMany(instruction.getElseBlock());
    }

    @Override
    public void visit(WasmBranchInstruction instruction) {
    }

    @Override
    public void visit(WasmNullBranchInstruction instruction) {
    }

    @Override
    public void visit(WasmCastBranchInstruction instruction) {
    }

    @Override
    public void visit(WasmBreakInstruction instruction) {
    }

    @Override
    public void visit(WasmSwitchInstruction instruction) {
    }

    @Override
    public void visit(WasmReturnInstruction instruction) {
    }

    @Override
    public void visit(WasmInt32ConstantInstruction instruction) {
    }

    @Override
    public void visit(WasmInt64ConstantInstruction instruction) {
    }

    @Override
    public void visit(WasmFloat32ConstantInstruction instruction) {
    }

    @Override
    public void visit(WasmFloat64ConstantInstruction instruction) {
    }

    @Override
    public void visit(WasmNullConstantInstruction instruction) {
    }

    @Override
    public void visit(WasmIsNullInstruction instruction) {
    }

    @Override
    public void visit(WasmGetLocalInstruction instruction) {
    }

    @Override
    public void visit(WasmSetLocalInstruction instruction) {
    }

    @Override
    public void visit(WasmTeeLocalInstruction instruction) {
    }

    @Override
    public void visit(WasmGetGlobalInstruction instruction) {
    }

    @Override
    public void visit(WasmSetGlobalInstruction instruction) {
    }

    @Override
    public void visit(WasmIntBinaryInstruction instruction) {
    }

    @Override
    public void visit(WasmFloatBinaryInstruction instruction) {
    }

    @Override
    public void visit(WasmIntUnaryInstruction instruction) {
    }

    @Override
    public void visit(WasmFloatUnaryInstruction instruction) {
    }

    @Override
    public void visit(WasmConversionInstruction instruction) {
    }

    @Override
    public void visit(WasmCallInstruction instruction) {
    }

    @Override
    public void visit(WasmIndirectCallInstruction instruction) {
    }

    @Override
    public void visit(WasmCallReferenceInstruction instruction) {
    }

    @Override
    public void visit(WasmDropInstruction instruction) {
    }

    @Override
    public void visit(WasmLoadInt32Instruction instruction) {
    }

    @Override
    public void visit(WasmLoadInt64Instruction instruction) {
    }

    @Override
    public void visit(WasmLoadFloat32Instruction instruction) {
    }

    @Override
    public void visit(WasmLoadFloat64Instruction instruction) {
    }

    @Override
    public void visit(WasmStoreInt32Instruction instruction) {
    }

    @Override
    public void visit(WasmStoreInt64Instruction instruction) {
    }

    @Override
    public void visit(WasmStoreFloat32Instruction instruction) {
    }

    @Override
    public void visit(WasmStoreFloat64Instruction instruction) {
    }

    @Override
    public void visit(WasmMemoryGrowInstruction instruction) {
    }

    @Override
    public void visit(WasmFillInstruction instruction) {
    }

    @Override
    public void visit(WasmCopyInstruction instruction) {
    }

    @Override
    public void visit(WasmTryInstruction instruction) {
        visitMany(instruction.getBody());
        for (var catchClause : instruction.getCatches()) {
            visitMany(catchClause);
        }
    }

    @Override
    public void visit(WasmThrowInstruction instruction) {
    }

    @Override
    public void visit(WasmReferencesEqualInstruction instruction) {
    }

    @Override
    public void visit(WasmCastInstruction instruction) {
    }

    @Override
    public void visit(WasmTestInstruction instruction) {
    }

    @Override
    public void visit(WasmExternConversionInstruction instruction) {
    }

    @Override
    public void visit(WasmStructNewInstruction instruction) {
    }

    @Override
    public void visit(WasmStructNewDefaultInstruction instruction) {
    }

    @Override
    public void visit(WasmStructGetInstruction instruction) {
    }

    @Override
    public void visit(WasmStructSetInstruction instruction) {
    }

    @Override
    public void visit(WasmArrayNewDefaultInstruction instruction) {
    }

    @Override
    public void visit(WasmArrayNewFixedInstruction instruction) {
    }

    @Override
    public void visit(WasmArrayGetInstruction instruction) {
    }

    @Override
    public void visit(WasmArraySetInstruction instruction) {
    }

    @Override
    public void visit(WasmArrayLengthInstruction instruction) {
    }

    @Override
    public void visit(WasmArrayCopyInstruction instruction) {
    }

    @Override
    public void visit(WasmFunctionReferenceInstruction instruction) {
    }

    @Override
    public void visit(WasmInt31ReferenceInstruction instruction) {
    }

    @Override
    public void visit(WasmInt31GetInstruction instruction) {
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
