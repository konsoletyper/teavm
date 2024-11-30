/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.wasm.parser;

import org.teavm.backend.wasm.model.WasmNumType;
import org.teavm.backend.wasm.model.expression.WasmFloatBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmFloatType;
import org.teavm.backend.wasm.model.expression.WasmFloatUnaryOperation;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmInt64Subtype;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.expression.WasmSignedType;

public interface CodeListener {
    default void error(int depth) {
    }

    default int startBlock(boolean loop, WasmHollowType type) {
        return 0;
    }

    default int startConditionalBlock(WasmHollowType type) {
        return 0;
    }

    default void startElseSection(int token) {
    }

    default int startTry(WasmHollowType type) {
        return 0;
    }

    default void startCatch(int tagIndex) {
    }

    default void endBlock(int token, boolean loop) {
    }

    default void branch(BranchOpcode opcode, int depth, int target) {
    }

    default void castBranch(boolean success, int depth, int target, WasmHollowType.Reference sourceType,
            WasmHollowType.Reference targetType) {
    }

    default void tableBranch(int[] depths, int[] targets, int defaultDepth, int defaultTarget) {
    }

    default void throwInstruction(int tagIndex) {
    }

    default void opcode(Opcode opcode) {
    }

    default void local(LocalOpcode opcode, int index) {
    }

    default void unary(WasmIntUnaryOperation opcode, WasmIntType type) {
    }

    default void unary(WasmFloatUnaryOperation opcode, WasmFloatType type) {
    }

    default void binary(WasmIntBinaryOperation opcode, WasmIntType type) {
    }

    default void binary(WasmFloatBinaryOperation opcode, WasmFloatType type) {
    }

    default void call(int functionIndex) {
    }

    default void indirectCall(int typeIndex, int tableIndex) {
    }

    default void loadInt32(WasmInt32Subtype convertFrom, int align, int offset) {
    }

    default void storeInt32(WasmInt32Subtype convertTo, int align, int offset) {
    }

    default void loadInt64(WasmInt64Subtype convertFrom, int align, int offset) {
    }

    default void storeInt64(WasmInt64Subtype convertTo, int align, int offset) {
    }

    default void loadFloat32(int align, int offset) {
    }

    default void storeFloat32(int align, int offset) {
    }

    default void loadFloat64(int align, int offset) {
    }

    default void storeFloat64(int align, int offset) {
    }

    default void convert(WasmNumType sourceType, WasmNumType targetType, boolean signed, boolean reinterpret,
            boolean nonTrapping) {
    }

    default void memoryGrow() {
    }

    default void memoryFill() {
    }

    default void memoryCopy() {
    }

    default void int32Constant(int value) {
    }

    default void int64Constant(long value) {
    }

    default void float32Constant(float value) {
    }

    default void float64Constant(double value) {
    }

    default void nullConstant(WasmHollowType.Reference type) {
    }

    default void cast(WasmHollowType.Reference type) {
    }

    default void test(WasmHollowType.Reference type) {
    }

    default void structNew(int typeIndex) {
    }

    default void structNewDefault(int typeIndex) {
    }

    default void structGet(WasmSignedType signedType, int typeIndex, int fieldIndex) {
    }

    default void structSet(int typeIndex, int fieldIndex) {
    }

    default void arrayNewDefault(int typeIndex) {
    }

    default void arrayNewFixed(int typeIndex, int size) {
    }

    default void arrayGet(WasmSignedType signedType, int typeIndex) {
    }

    default void arraySet(int typeIndex) {
    }

    default void arrayCopy(int targetTypeIndex, int sourceTypeIndex) {
    }

    default void getGlobal(int globalIndex) {
    }

    default void setGlobal(int globalIndex) {
    }

    default void callReference(int typeIndex) {
    }

    default void functionReference(int functionIndex) {
    }

    default void int31Reference() {
    }

    default void int31Get(WasmSignedType signedType) {
    }

    CodeListener EMPTY = new CodeListener() {
    };
}
