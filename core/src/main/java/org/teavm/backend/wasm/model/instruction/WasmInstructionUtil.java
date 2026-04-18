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

import org.teavm.backend.wasm.model.expression.WasmFloatBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnaryOperation;

public final class WasmInstructionUtil {
    private WasmInstructionUtil() {
    }

    public static void negate(WasmInstruction instruction) {
        if (instruction instanceof WasmIntBinaryInstruction) {
            var binary = (WasmIntBinaryInstruction) instruction;

            var negatedOp = negate(binary.getOperation());
            if (negatedOp != null) {
                binary.setOperation(negatedOp);
                return;
            }
        } else if (instruction instanceof WasmFloatBinaryInstruction) {
            var binary = (WasmFloatBinaryInstruction) instruction;
            var negatedOp = negate(binary.getOperation());
            if (negatedOp != null) {
                binary.setOperation(negatedOp);
                return;
            }
        } else if (instruction instanceof WasmIntUnaryInstruction) {
            var unary = (WasmIntUnaryInstruction) instruction;
            if (unary.getOperation() == WasmIntUnaryOperation.EQZ) {
                unary.delete();
                return;
            }
        } else if (instruction instanceof WasmInt32ConstantInstruction) {
            var cst = (WasmInt32ConstantInstruction) instruction;
            if (cst.getValue() == 0) {
                cst.setValue(1);
            } else {
                cst.setValue(0);
            }
            return;
        }

        instruction.insertNext(new WasmIntUnaryInstruction(WasmIntType.INT32, WasmIntUnaryOperation.EQZ));
    }

    private static WasmIntBinaryOperation negate(WasmIntBinaryOperation op) {
        switch (op) {
            case EQ:
                return WasmIntBinaryOperation.NE;
            case NE:
                return WasmIntBinaryOperation.EQ;
            case LT_SIGNED:
                return WasmIntBinaryOperation.GE_SIGNED;
            case LT_UNSIGNED:
                return WasmIntBinaryOperation.GE_UNSIGNED;
            case LE_SIGNED:
                return WasmIntBinaryOperation.GT_SIGNED;
            case LE_UNSIGNED:
                return WasmIntBinaryOperation.GT_UNSIGNED;
            case GT_SIGNED:
                return WasmIntBinaryOperation.LE_SIGNED;
            case GT_UNSIGNED:
                return WasmIntBinaryOperation.LE_UNSIGNED;
            case GE_SIGNED:
                return WasmIntBinaryOperation.LT_SIGNED;
            case GE_UNSIGNED:
                return WasmIntBinaryOperation.LT_UNSIGNED;
            default:
                return null;
        }
    }

    private static WasmFloatBinaryOperation negate(WasmFloatBinaryOperation op) {
        switch (op) {
            case EQ:
                return WasmFloatBinaryOperation.NE;
            case NE:
                return WasmFloatBinaryOperation.EQ;
            default:
                return null;
        }
    }
}
