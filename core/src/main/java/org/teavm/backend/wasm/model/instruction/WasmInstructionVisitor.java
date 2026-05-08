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
    void visit(WasmUnreachable instruction);

    void visit(WasmBlock instruction);

    void visit(WasmConditional instruction);

    void visit(WasmBranch instruction);

    void visit(WasmNullBranch instruction);

    void visit(WasmCastBranch instruction);

    void visit(WasmBreak instruction);

    void visit(WasmSwitch instruction);

    void visit(WasmReturn instruction);

    void visit(WasmInt32Constant instruction);

    void visit(WasmInt64Constant instruction);

    void visit(WasmFloat32Constant instruction);

    void visit(WasmFloat64Constant instruction);

    void visit(WasmNullConstant instruction);

    void visit(WasmIsNull instruction);

    void visit(WasmGetLocal instruction);

    void visit(WasmSetLocal instruction);

    void visit(WasmTeeLocal instruction);

    void visit(WasmGetGlobal instruction);

    void visit(WasmSetGlobal instruction);

    void visit(WasmIntBinary instruction);

    void visit(WasmFloatBinary instruction);

    void visit(WasmIntUnary instruction);

    void visit(WasmFloatUnary instruction);

    void visit(WasmConversion instruction);

    void visit(WasmCall instruction);

    void visit(WasmIndirectCall instruction);

    void visit(WasmCallReference instruction);

    void visit(WasmDrop instruction);

    void visit(WasmLoadInt32 instruction);

    void visit(WasmLoadInt64 instruction);

    void visit(WasmLoadFloat32 instruction);

    void visit(WasmLoadFloat64 instruction);

    void visit(WasmStoreInt32 instruction);

    void visit(WasmStoreInt64 instruction);

    void visit(WasmStoreFloat32 instruction);

    void visit(WasmStoreFloat64 instruction);

    void visit(WasmMemoryGrow instruction);

    void visit(WasmFill instruction);

    void visit(WasmCopy instruction);

    void visit(WasmTry instruction);

    void visit(WasmThrow instruction);

    void visit(WasmReferencesEqual instruction);

    void visit(WasmCast instruction);

    void visit(WasmTest instruction);

    void visit(WasmExternConversion instruction);

    void visit(WasmStructNew instruction);

    void visit(WasmStructNewDefault instruction);

    void visit(WasmStructGet instruction);

    void visit(WasmStructSet instruction);

    void visit(WasmArrayNewDefault instruction);

    void visit(WasmArrayNewFixed instruction);

    void visit(WasmArrayGet instruction);

    void visit(WasmArraySet instruction);

    void visit(WasmArrayLength instruction);

    void visit(WasmArrayCopy instruction);

    void visit(WasmFunctionReference instruction);

    void visit(WasmInt31Reference instruction);

    void visit(WasmInt31Get instruction);
}
