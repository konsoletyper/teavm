/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.wasm.decompile;

import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.util.VariableType;
import org.teavm.wasm.model.WasmLocal;
import org.teavm.wasm.model.WasmType;
import org.teavm.wasm.model.expression.WasmExpression;
import org.teavm.wasm.model.expression.WasmFloat32Constant;
import org.teavm.wasm.model.expression.WasmFloat64Constant;
import org.teavm.wasm.model.expression.WasmFloatBinary;
import org.teavm.wasm.model.expression.WasmFloatBinaryOperation;
import org.teavm.wasm.model.expression.WasmFloatType;
import org.teavm.wasm.model.expression.WasmInt32Constant;
import org.teavm.wasm.model.expression.WasmInt64Constant;
import org.teavm.wasm.model.expression.WasmIntBinary;
import org.teavm.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.wasm.model.expression.WasmIntType;
import org.teavm.wasm.model.expression.WasmLocalReference;

final class DecompileSupport {
    private DecompileSupport() {
    }

    public static WasmExpression getCondition(BranchingInstruction instruction, WasmLocal local, VariableType type) {
        WasmExpression operand = new WasmLocalReference(local);

        WasmExpression condition;
        switch (type) {
            case FLOAT:
                condition = new WasmFloatBinary(WasmFloatType.FLOAT32, getFloatCondition(instruction.getCondition()),
                        operand, new WasmFloat32Constant(0));
                break;
            case DOUBLE:
                condition = new WasmFloatBinary(WasmFloatType.FLOAT64, getFloatCondition(instruction.getCondition()),
                        operand, new WasmFloat64Constant(0));
                break;
            case INT:
                condition = new WasmIntBinary(WasmIntType.INT32, getIntCondition(instruction.getCondition()),
                        operand, new WasmInt32Constant(0));
                break;
            case LONG:
                condition = new WasmIntBinary(WasmIntType.INT64, getIntCondition(instruction.getCondition()),
                        operand, new WasmInt64Constant(0));
                break;
            default:
                condition = new WasmIntBinary(WasmIntType.INT32, getReferenceCondition(instruction.getCondition()),
                        operand, new WasmInt32Constant(0));
                break;
        }

        return operand;
    }

    public static WasmExpression getCondition(BinaryBranchingInstruction instruction, WasmLocal first,
            WasmLocal second) {
        WasmExpression a = new WasmLocalReference(first);
        WasmExpression b = new WasmLocalReference(second);

        WasmExpression condition;
        switch (instruction.getCondition()) {
            case REFERENCE_EQUAL:
            case EQUAL:
                condition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.EQ, a, b);
                break;
            case REFERENCE_NOT_EQUAL:
            case NOT_EQUAL:
                condition = new WasmIntBinary(WasmIntType.INT32, WasmIntBinaryOperation.NE, a, b);
                break;
            default:
                throw new IllegalArgumentException(instruction.getCondition().toString());
        }

        return condition;
    }

    private static WasmFloatBinaryOperation getFloatCondition(BranchingCondition condition) {
        switch (condition) {
            case EQUAL:
                return WasmFloatBinaryOperation.EQ;
            case NOT_EQUAL:
                return WasmFloatBinaryOperation.NE;
            case GREATER:
                return WasmFloatBinaryOperation.GT;
            case GREATER_OR_EQUAL:
                return WasmFloatBinaryOperation.GE;
            case LESS:
                return WasmFloatBinaryOperation.LT;
            case LESS_OR_EQUAL:
                return WasmFloatBinaryOperation.LE;
            case NULL:
            case NOT_NULL:
                break;
        }
        throw new IllegalArgumentException(condition.toString());
    }

    private static WasmIntBinaryOperation getIntCondition(BranchingCondition condition) {
        switch (condition) {
            case EQUAL:
                return WasmIntBinaryOperation.EQ;
            case NOT_EQUAL:
                return WasmIntBinaryOperation.NE;
            case GREATER:
                return WasmIntBinaryOperation.GT_SIGNED;
            case GREATER_OR_EQUAL:
                return WasmIntBinaryOperation.GE_SIGNED;
            case LESS:
                return WasmIntBinaryOperation.LT_SIGNED;
            case LESS_OR_EQUAL:
                return WasmIntBinaryOperation.LE_SIGNED;
            case NULL:
            case NOT_NULL:
                break;
        }
        throw new IllegalArgumentException(condition.toString());
    }

    private static WasmIntBinaryOperation getReferenceCondition(BranchingCondition condition) {
        switch (condition) {
            case NULL:
                return WasmIntBinaryOperation.EQ;
            case NOT_NULL:
                return WasmIntBinaryOperation.NE;
            default:
                break;
        }
        throw new IllegalArgumentException(condition.toString());
    }

    public static WasmType mapType(VariableType type) {
        switch (type) {
            case INT:
                return WasmType.INT32;
            case LONG:
                return WasmType.INT64;
            case FLOAT:
                return WasmType.FLOAT32;
            case DOUBLE:
                return WasmType.FLOAT64;
            default:
                return WasmType.INT32;
        }
    }
}
