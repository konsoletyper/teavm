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

public class WasmTypeInference implements WasmInstructionVisitor {
    public final List<WasmType> typeStack = new ArrayList<>();
    private int depthBeforeLastInstructionOut;

    public int getDepthBeforeLastInstructionOut() {
        return depthBeforeLastInstructionOut;
    }

    @Override
    public void visit(WasmUnreachable instruction) {
        typeStack.clear();
    }

    @Override
    public void visit(WasmBlock instruction) {
        var type = instruction.getType();
        if (type != null) {
            popN(type.getInputTypes().size());
            depthBeforeLastInstructionOut = typeStack.size();
            typeStack.addAll(type.getOutputTypes());
        }
    }

    @Override
    public void visit(WasmConditional instruction) {
        pop();
        var type = instruction.getType();
        if (type != null) {
            popN(type.getInputTypes().size());
            depthBeforeLastInstructionOut = typeStack.size();
            typeStack.addAll(type.getOutputTypes());
        }
    }

    @Override
    public void visit(WasmBranch instruction) {
        pop();
    }

    @Override
    public void visit(WasmNullBranch instruction) {
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
    public void visit(WasmCastBranch instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        if (instruction.getCondition() == WasmCastCondition.SUCCESS) {
            typeStack.add(instruction.getSourceType());
        } else {
            typeStack.add(instruction.getTargetType());
        }
    }

    @Override
    public void visit(WasmBreak instruction) {
        typeStack.clear();
    }

    @Override
    public void visit(WasmSwitch instruction) {
        typeStack.clear();
    }

    @Override
    public void visit(WasmReturn instruction) {
        typeStack.clear();
    }

    @Override
    public void visit(WasmInt32Constant instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.INT32);
    }

    @Override
    public void visit(WasmInt64Constant instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.INT64);
    }

    @Override
    public void visit(WasmFloat32Constant instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.FLOAT32);
    }

    @Override
    public void visit(WasmFloat64Constant instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.FLOAT64);
    }

    @Override
    public void visit(WasmNullConstant instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getType());
    }

    @Override
    public void visit(WasmIsNull instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.INT32);
    }

    @Override
    public void visit(WasmGetLocal instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getLocal().getType());
    }

    @Override
    public void visit(WasmSetLocal instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmTeeLocal instruction) {
        depthBeforeLastInstructionOut = typeStack.size() - 1;
    }

    @Override
    public void visit(WasmGetGlobal instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getGlobal().getType());
    }

    @Override
    public void visit(WasmSetGlobal instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmIntBinary instruction) {
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
    public void visit(WasmFloatBinary instruction) {
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
    public void visit(WasmIntUnary instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        if (instruction.getOperation() == WasmIntUnaryOperation.EQZ) {
            typeStack.add(WasmType.INT32);
        } else {
            typeStack.add(instruction.getType() == WasmIntType.INT32 ? WasmType.INT32 : WasmType.INT64);
        }
    }

    @Override
    public void visit(WasmFloatUnary instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getType() == WasmFloatType.FLOAT32 ? WasmType.FLOAT32 : WasmType.FLOAT64);
    }

    @Override
    public void visit(WasmConversion instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.num(instruction.getTargetType()));
    }

    @Override
    public void visit(WasmCall instruction) {
        var function = instruction.getFunction();
        popN(function.getType().getParameterTypes().size());
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.addAll(function.getType().getReturnTypes());
    }

    @Override
    public void visit(WasmIndirectCall instruction) {
        popN(instruction.getType().getParameterTypes().size() + 1);
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.addAll(instruction.getType().getReturnTypes());
    }

    @Override
    public void visit(WasmCallReference instruction) {
        popN(instruction.getType().getParameterTypes().size() + 1);
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.addAll(instruction.getType().getReturnTypes());
    }

    @Override
    public void visit(WasmDrop instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmLoadInt32 instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.INT32);
    }

    @Override
    public void visit(WasmLoadInt64 instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.INT64);
    }

    @Override
    public void visit(WasmLoadFloat32 instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.FLOAT32);
    }

    @Override
    public void visit(WasmLoadFloat64 instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.FLOAT64);
    }

    @Override
    public void visit(WasmStoreInt32 instruction) {
        popN(2);
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmStoreInt64 instruction) {
        popN(2);
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmStoreFloat32 instruction) {
        popN(2);
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmStoreFloat64 instruction) {
        popN(2);
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmMemoryGrow instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.INT32);
    }

    @Override
    public void visit(WasmFill instruction) {
        popN(3);
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmCopy instruction) {
        popN(3);
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmTry instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        if (instruction.getType() != null) {
            typeStack.add(instruction.getType());
        }
    }

    @Override
    public void visit(WasmThrow instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.clear();
    }

    @Override
    public void visit(WasmReferencesEqual instruction) {
        popN(2);
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.INT32);
    }

    @Override
    public void visit(WasmCast instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getTargetType());
    }

    @Override
    public void visit(WasmTest instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.INT32);
    }

    @Override
    public void visit(WasmExternConversion instruction) {
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
    public void visit(WasmStructNew instruction) {
        popN(instruction.getType().getFields().size());
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getType().getReference());
    }

    @Override
    public void visit(WasmStructNewDefault instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getType().getReference());
    }

    @Override
    public void visit(WasmStructGet instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getType().getFields().get(instruction.getFieldIndex()).getUnpackedType());
    }

    @Override
    public void visit(WasmStructSet instruction) {
        popN(2);
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmArrayNewDefault instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getType().getReference());
    }

    @Override
    public void visit(WasmArrayNewFixed instruction) {
        popN(instruction.getSize());
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getType().getReference());
    }

    @Override
    public void visit(WasmArrayGet instruction) {
        popN(2);
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getType().getElementType().asUnpackedType());
    }

    @Override
    public void visit(WasmArraySet instruction) {
        popN(3);
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmArrayLength instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.INT32);
    }

    @Override
    public void visit(WasmArrayCopy instruction) {
        popN(5);
        depthBeforeLastInstructionOut = typeStack.size();
    }

    @Override
    public void visit(WasmFunctionReference instruction) {
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(instruction.getFunction().getType().getReference());
    }

    @Override
    public void visit(WasmInt31Reference instruction) {
        pop();
        depthBeforeLastInstructionOut = typeStack.size();
        typeStack.add(WasmType.I31);
    }

    @Override
    public void visit(WasmInt31Get instruction) {
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
