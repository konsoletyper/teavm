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
package org.teavm.backend.wasm.optimization;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
import org.teavm.backend.wasm.model.WasmNumType;
import org.teavm.backend.wasm.model.instruction.WasmArrayNewDefault;
import org.teavm.backend.wasm.model.instruction.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.instruction.WasmBlock;
import org.teavm.backend.wasm.model.instruction.WasmBranch;
import org.teavm.backend.wasm.model.instruction.WasmBreak;
import org.teavm.backend.wasm.model.instruction.WasmCall;
import org.teavm.backend.wasm.model.instruction.WasmCallReference;
import org.teavm.backend.wasm.model.instruction.WasmCastBranch;
import org.teavm.backend.wasm.model.instruction.WasmConditional;
import org.teavm.backend.wasm.model.instruction.WasmConversion;
import org.teavm.backend.wasm.model.instruction.WasmDefaultInstructionVisitor;
import org.teavm.backend.wasm.model.instruction.WasmDrop;
import org.teavm.backend.wasm.model.instruction.WasmFloat32Constant;
import org.teavm.backend.wasm.model.instruction.WasmFloat64Constant;
import org.teavm.backend.wasm.model.instruction.WasmFloatBinary;
import org.teavm.backend.wasm.model.instruction.WasmFloatUnary;
import org.teavm.backend.wasm.model.instruction.WasmFunctionReference;
import org.teavm.backend.wasm.model.instruction.WasmGetGlobal;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmIndirectCall;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmInt31Reference;
import org.teavm.backend.wasm.model.instruction.WasmInt32Constant;
import org.teavm.backend.wasm.model.instruction.WasmInt64Constant;
import org.teavm.backend.wasm.model.instruction.WasmIntBinary;
import org.teavm.backend.wasm.model.instruction.WasmIntUnary;
import org.teavm.backend.wasm.model.instruction.WasmIsNull;
import org.teavm.backend.wasm.model.instruction.WasmNullBranch;
import org.teavm.backend.wasm.model.instruction.WasmNullConstant;
import org.teavm.backend.wasm.model.instruction.WasmSetLocal;
import org.teavm.backend.wasm.model.instruction.WasmStructNew;
import org.teavm.backend.wasm.model.instruction.WasmStructNewDefault;
import org.teavm.backend.wasm.model.instruction.WasmSwitch;
import org.teavm.backend.wasm.model.instruction.WasmTeeLocal;
import org.teavm.backend.wasm.model.instruction.WasmTest;
import org.teavm.backend.wasm.model.instruction.WasmThrow;
import org.teavm.backend.wasm.model.instruction.WasmTry;

public class WasmReschedulingAnalysis extends WasmDefaultInstructionVisitor {
    public boolean hasBarriers;
    public boolean hasExits;
    public boolean mayThrow;
    public BitSet setLocals = new BitSet();
    public BitSet getLocals = new BitSet();

    private boolean instructionCanHaveBarriers;
    private Set<WasmInstructionList> safeLabels = new HashSet<>();
    private boolean hasUnsafeLabels;

    public void reset() {
        hasBarriers = false;
        hasExits = false;
        mayThrow = false;
        setLocals.clear();
        getLocals.clear();
    }

    @Override
    public void visit(WasmBlock instruction) {
        // Loop may never terminate
        if (!instruction.isLoop()) {
            safe();
        }
        super.visit(instruction);
    }

    @Override
    public void visit(WasmConditional instruction) {
        safe();
        super.visit(instruction);
    }

    @Override
    public void visit(WasmTeeLocal instruction) {
        safe();
        setLocals.set(instruction.getLocal().getIndex());
    }

    @Override
    public void visit(WasmGetGlobal instruction) {
        if (instruction.getGlobal().isImmutable()) {
            safe();
        }
    }

    @Override
    public void visit(WasmGetLocal instruction) {
        safe();
        getLocals.set(instruction.getLocal().getIndex());
    }

    @Override
    public void visit(WasmSetLocal instruction) {
        safe();
        setLocals.set(instruction.getLocal().getIndex());
    }

    @Override
    public void visit(WasmSwitch instruction) {
        safe();
        for (var label : instruction.getTargets()) {
            checkLabel(label);
        }
        checkLabel(instruction.getDefaultTarget());
    }

    @Override
    public void visit(WasmDrop instruction) {
        safe();
    }

    @Override
    public void visit(WasmBranch instruction) {
        safe();
        checkLabel(instruction.getTarget());
    }

    @Override
    public void visit(WasmBreak instruction) {
        safe();
        checkLabel(instruction.getTarget());
    }

    @Override
    public void visit(WasmCastBranch instruction) {
        safe();
        checkLabel(instruction.getTarget());
    }

    @Override
    public void visit(WasmNullBranch instruction) {
        safe();
        checkLabel(instruction.getTarget());
    }

    @Override
    public void visit(WasmTest instruction) {
        safe();
    }

    @Override
    public void visit(WasmIsNull instruction) {
        safe();
    }

    @Override
    public void visit(WasmIntBinary instruction) {
        switch (instruction.getOperation()) {
            case DIV_SIGNED, DIV_UNSIGNED, REM_SIGNED, REM_UNSIGNED -> {
                // keep unsafe
            }
            default -> safe();
        }
    }

    @Override
    public void visit(WasmIntUnary instruction) {
        safe();
    }

    @Override
    public void visit(WasmFloatBinary instruction) {
        safe();
    }

    @Override
    public void visit(WasmConversion instruction) {
        if (instruction.isNonTrapping() || instruction.isReinterpret() || !isFloat(instruction.getSourceType())
                || isFloat(instruction.getTargetType())) {
            safe();
        }
    }

    private static boolean isFloat(WasmNumType type) {
        return type == WasmNumType.FLOAT32 || type == WasmNumType.FLOAT64;
    }

    @Override
    public void visit(WasmFloatUnary instruction) {
        safe();
    }

    @Override
    public void visit(WasmInt32Constant instruction) {
        safe();
    }

    @Override
    public void visit(WasmInt64Constant instruction) {
        safe();
    }

    @Override
    public void visit(WasmFloat32Constant instruction) {
        safe();
    }

    @Override
    public void visit(WasmFloat64Constant instruction) {
        safe();
    }

    @Override
    public void visit(WasmNullConstant instruction) {
        safe();
    }

    @Override
    public void visit(WasmStructNew instruction) {
        safe();
    }

    @Override
    public void visit(WasmStructNewDefault instruction) {
        safe();
    }

    @Override
    public void visit(WasmArrayNewDefault instruction) {
        safe();
    }

    @Override
    public void visit(WasmArrayNewFixed instruction) {
        safe();
    }

    @Override
    public void visit(WasmCall instruction) {
        mayThrow = true;
    }

    @Override
    public void visit(WasmIndirectCall instruction) {
        mayThrow = true;
    }

    @Override
    public void visit(WasmCallReference instruction) {
        mayThrow = true;
    }

    @Override
    public void visit(WasmThrow instruction) {
        mayThrow = true;
    }

    @Override
    public void visit(WasmTry instruction) {
        for (var clause : instruction.getCatches()) {
            checkLabel(clause.getTarget());
        }
        super.visit(instruction);
    }

    @Override
    public void visit(WasmFunctionReference instruction) {
        safe();
    }

    @Override
    public void visit(WasmInt31Reference instruction) {
        safe();
    }

    @Override
    public void visitDefault(WasmInstruction instruction) {
        instructionCanHaveBarriers = true;
        hasUnsafeLabels = false;
        instruction.acceptVisitor(this);
        hasBarriers |= instructionCanHaveBarriers | hasUnsafeLabels;
        hasExits |= hasUnsafeLabels;
    }

    @Override
    public void visitMany(WasmInstructionList instructions) {
        var savedHasUnsafeLabels = hasUnsafeLabels;
        safeLabels.add(instructions);
        for (var instruction : instructions) {
            visitDefault(instruction);
        }
        safeLabels.remove(instructions);
        hasUnsafeLabels = savedHasUnsafeLabels;
    }

    private void safe() {
        instructionCanHaveBarriers = false;
    }

    private void checkLabel(WasmInstructionList label) {
        hasUnsafeLabels |= !safeLabels.contains(label);
    }
}
