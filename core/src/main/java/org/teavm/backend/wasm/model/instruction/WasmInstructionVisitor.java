/*
 *  Copyright 2024 Alexey Andreev.
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

public interface WasmInstructionVisitor {
    void visit(WasmUnreachableInstruction instruction);

    void visit(WasmBlockInstruction instruction);

    void visit(WasmConditionalInstruction instruction);

    void visit(WasmBranchInstruction instruction);

    void visit(WasmNullBranchInstruction instruction);

    void visit(WasmCastBranchInstruction instruction);

    void visit(WasmBreakInstruction instruction);

    void visit(WasmSwitchInstruction instruction);

    void visit(WasmReturnInstruction instruction);

    void visit(WasmInt32ConstantInstruction instruction);

    void visit(WasmInt64ConstantInstruction instruction);

    void visit(WasmFloat32ConstantInstruction instruction);

    void visit(WasmFloat64ConstantInstruction instruction);

    void visit(WasmNullConstantInstruction instruction);

    void visit(WasmIsNullInstruction instruction);

    void visit(WasmGetLocalInstruction instruction);

    void visit(WasmSetLocalInstruction instruction);

    void visit(WasmTeeLocalInstruction instruction);

    void visit(WasmGetGlobalInstruction instruction);

    void visit(WasmSetGlobalInstruction instruction);

    void visit(WasmIntBinaryInstruction instruction);

    void visit(WasmFloatBinaryInstruction instruction);

    void visit(WasmIntUnaryInstruction instruction);

    void visit(WasmFloatUnaryInstruction instruction);

    void visit(WasmConversionInstruction instruction);

    void visit(WasmCallInstruction instruction);

    void visit(WasmIndirectCallInstruction instruction);

    void visit(WasmCallReferenceInstruction instruction);

    void visit(WasmDropInstruction instruction);

    void visit(WasmLoadInt32Instruction instruction);

    void visit(WasmLoadInt64Instruction instruction);

    void visit(WasmLoadFloat32Instruction instruction);

    void visit(WasmLoadFloat64Instruction instruction);

    void visit(WasmStoreInt32Instruction instruction);

    void visit(WasmStoreInt64Instruction instruction);

    void visit(WasmStoreFloat32Instruction instruction);

    void visit(WasmStoreFloat64Instruction instruction);

    void visit(WasmMemoryGrowInstruction instruction);

    void visit(WasmFillInstruction instruction);

    void visit(WasmCopyInstruction instruction);

    void visit(WasmTryInstruction instruction);

    void visit(WasmThrowInstruction instruction);

    void visit(WasmReferencesEqualInstruction instruction);

    void visit(WasmCastInstruction instruction);

    void visit(WasmTestInstruction instruction);

    void visit(WasmExternConversionInstruction instruction);

    void visit(WasmStructNewInstruction instruction);

    void visit(WasmStructNewDefaultInstruction instruction);

    void visit(WasmStructGetInstruction instruction);

    void visit(WasmStructSetInstruction instruction);

    void visit(WasmArrayNewDefaultInstruction instruction);

    void visit(WasmArrayNewFixedInstruction instruction);

    void visit(WasmArrayGetInstruction instruction);

    void visit(WasmArraySetInstruction instruction);

    void visit(WasmArrayLengthInstruction instruction);

    void visit(WasmArrayCopyInstruction instruction);

    void visit(WasmFunctionReferenceInstruction instruction);

    void visit(WasmInt31ReferenceInstruction instruction);

    void visit(WasmInt31GetInstruction instruction);
}
