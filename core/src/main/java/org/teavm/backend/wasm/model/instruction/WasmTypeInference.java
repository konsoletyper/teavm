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

import java.util.ArrayList;
import java.util.List;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmCastCondition;
import org.teavm.backend.wasm.model.expression.WasmFloatType;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.expression.WasmNullCondition;

public class WasmTypeInference implements WasmInstructionVisitor {
    public final List<WasmType> typeStack = new ArrayList<>();
    private int depthBeforeLastInstructionOut;

    public int getDepthBeforeLastInstructionOut() {
        return depthBeforeLastInstructionOut;
    }

    @Override
    public void visit(WasmUnreachableInstruction instruction) {
        typeStack.clear();
    }

    @Override
    public void visit(WasmBlockInstruction instruction) {
        var type = instruction.getType();
        if (type != null) {
            popN(type.getInputTypes().size());
            depthBeforeLastInstructionOut = typeStack.size();
            typeStack.addAll(type.getOutputTypes());
        }
    }

    @Override
    public void visit(WasmConditionalInstruction instruction) {
        pop();
        var type = instruction.getType();
        if (type != null) {
            popN(type.getInputTypes().size());
            depthBeforeLastInstructionOut = typeStack.size();
            typeStack.addAll(type.getOutputTypes());
        }
    }

    @Override
    public void visit(WasmBranchInstruction instruction) {
        pop();
    }

    @Override
    public void visit(WasmNullBranchInstruction instruction) {
        if (instruction.getCondition() == WasmNullCondition.NOT_NULL) {
            pop();
            depthBeforeLastInstructionOut = typeStack.size();
        } else if (!typeStack.isEmpty()) {
            depthBeforeLastInstructionOut = typeStack.size() - 1;
            var top = typeStack.get(typeStack.size() - 1);
            var ref = (WasmType.Reference) top;
            if (ref.isNullable()) {
                typeStack.set(typeStack.size() - 1, ref.asNonNull());
            }
        }
    }

    @Override
    public void visit(WasmCastBranchInstruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        if (instruction.getCondition() == WasmCastCondition.SUCCESS) {
            typeStack.add(instruction.getSourceType());
        } else {
            typeStack.add(instruction.getTargetType());
        }
    }

    @Override
    public void visit(WasmBreakInstruction instruction) {
        typeStack.clear();
    }

    @Override
    public void visit(WasmSwitchInstruction instruction) {
        typeStack.clear();
    }

    @Override
    public void visit(WasmReturnInstruction instruction) {
        typeStack.clear();
    }

    @Override
    public void visit(WasmInt32ConstantInstruction instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.INT32);
    }

    @Override
    public void visit(WasmInt64ConstantInstruction instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.INT64);
    }

    @Override
    public void visit(WasmFloat32ConstantInstruction instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.FLOAT32);
    }

    @Override
    public void visit(WasmFloat64ConstantInstruction instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.FLOAT64);
    }

    @Override
    public void visit(WasmNullConstantInstruction instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getType());
    }

    @Override
    public void visit(WasmIsNullInstruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.INT32);
    }

    @Override
    public void visit(WasmGetLocalInstruction instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getLocal().getType());
    }

    @Override
    public void visit(WasmSetLocalInstruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmTeeLocalInstruction instruction) {
        depthBeforeLastInstructionOut = typeStack.size() - 1;
    }

    @Override
    public void visit(WasmGetGlobalInstruction instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getGlobal().getType());
    }

    @Override
    public void visit(WasmSetGlobalInstruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmIntBinaryInstruction instruction) {
        popN(2);
        depthBeforeLastInstructionOut = typeStack.size();
        switch (instruction.getOperation()) {
            case EQ:
            case NE:
            case LT_SIGNED:
            case LT_UNSIGNED:
            case LE_SIGNED:
            case LE_UNSIGNED:
            case GT_SIGNED:
            case GT_UNSIGNED:
            case GE_SIGNED:
            case GE_UNSIGNED:
                typeStack.add(WasmType.INT32);
                break;
            default:
                typeStack.add(instruction.getType() == WasmIntType.INT32 ? WasmType.INT32 : WasmType.INT64);
                break;
        }
    }

    @Override
    public void visit(WasmFloatBinaryInstruction instruction) {
        popN(2);
        depthBeforeLastInstructionOut = typeStack.size();
        switch (instruction.getOperation()) {
            case EQ:
            case NE:
            case LT:
            case LE:
            case GT:
            case GE:
                typeStack.add(WasmType.INT32);
                break;
            default:
                typeStack.add(instruction.getType() == WasmFloatType.FLOAT32 ? WasmType.FLOAT32 : WasmType.FLOAT64);
                break;
        }
    }

    @Override
    public void visit(WasmIntUnaryInstruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        if (instruction.getOperation() == WasmIntUnaryOperation.EQZ) {
            typeStack.add(WasmType.INT32);
        } else {
            typeStack.add(instruction.getType() == WasmIntType.INT32 ? WasmType.INT32 : WasmType.INT64);
        }
    }

    @Override
    public void visit(WasmFloatUnaryInstruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getType() == WasmFloatType.FLOAT32 ? WasmType.FLOAT32 : WasmType.FLOAT64);
    }

    @Override
    public void visit(WasmConversionInstruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.num(instruction.getTargetType()));
    }

    @Override
    public void visit(WasmCallInstruction instruction) {
        var function = instruction.getFunction();
        popN(function.getType().getParameterTypes().size());
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.addAll(function.getType().getReturnTypes());
    }

    @Override
    public void visit(WasmIndirectCallInstruction instruction) {
        popN(instruction.getType().getParameterTypes().size() + 1);
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.addAll(instruction.getType().getReturnTypes());
    }

    @Override
    public void visit(WasmCallReferenceInstruction instruction) {
        popN(instruction.getType().getParameterTypes().size() + 1);
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.addAll(instruction.getType().getReturnTypes());
    }

    @Override
    public void visit(WasmDropInstruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmLoadInt32Instruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.INT32);
    }

    @Override
    public void visit(WasmLoadInt64Instruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.INT64);
    }

    @Override
    public void visit(WasmLoadFloat32Instruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.FLOAT32);
    }

    @Override
    public void visit(WasmLoadFloat64Instruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.FLOAT64);
    }

    @Override
    public void visit(WasmStoreInt32Instruction instruction) {
        popN(2);
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmStoreInt64Instruction instruction) {
        popN(2);
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmStoreFloat32Instruction instruction) {
        popN(2);
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmStoreFloat64Instruction instruction) {
        popN(2);
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmMemoryGrowInstruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.INT32);
    }

    @Override
    public void visit(WasmFillInstruction instruction) {
        popN(3);
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmCopyInstruction instruction) {
        popN(3);
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmTryInstruction instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        if (instruction.getType() != null) {
            typeStack.add(instruction.getType());
        }
    }

    @Override
    public void visit(WasmThrowInstruction instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.clear();
    }

    @Override
    public void visit(WasmReferencesEqualInstruction instruction) {
        popN(2);
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.INT32);
    }

    @Override
    public void visit(WasmCastInstruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getTargetType());
    }

    @Override
    public void visit(WasmTestInstruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.INT32);
    }

    @Override
    public void visit(WasmExternConversionInstruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        switch (instruction.getType()) {
            case EXTERN_TO_ANY:
                typeStack.add(WasmType.ANY);
                break;
            case ANY_TO_EXTERN:
                typeStack.add(WasmType.EXTERN);
                break;
        }
    }

    @Override
    public void visit(WasmStructNewInstruction instruction) {
        popN(instruction.getType().getFields().size());
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getType().getReference());
    }

    @Override
    public void visit(WasmStructNewDefaultInstruction instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getType().getReference());
    }

    @Override
    public void visit(WasmStructGetInstruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getType().getFields().get(instruction.getFieldIndex()).getUnpackedType());
    }

    @Override
    public void visit(WasmStructSetInstruction instruction) {
        popN(2);
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmArrayNewDefaultInstruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getType().getReference());
    }

    @Override
    public void visit(WasmArrayNewFixedInstruction instruction) {
        popN(instruction.getSize());
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getType().getReference());
    }

    @Override
    public void visit(WasmArrayGetInstruction instruction) {
        popN(2);
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getType().getElementType().asUnpackedType());
    }

    @Override
    public void visit(WasmArraySetInstruction instruction) {
        popN(3);
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmArrayLengthInstruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.INT32);
    }

    @Override
    public void visit(WasmArrayCopyInstruction instruction) {
        popN(5);
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmFunctionReferenceInstruction instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getFunction().getType().getReference());
    }

    @Override
    public void visit(WasmInt31ReferenceInstruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.I31);
    }

    @Override
    public void visit(WasmInt31GetInstruction instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.INT32);
    }

    private void pop() {
        typeStack.remove(typeStack.size() - 1);
    }

    private void popN(int count) {
        var removeFrom = typeStack.size() - count;
        typeStack.subList(removeFrom, typeStack.size()).clear();
    }
}
