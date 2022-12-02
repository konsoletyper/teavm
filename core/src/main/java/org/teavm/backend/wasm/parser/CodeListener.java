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

import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmFloatBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmFloatType;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;

public interface CodeListener {
    void error(int depth);

    int startBlock(boolean loop, WasmType type);

    int startConditionalBlock(WasmType type);

    void startElseSection(int token);

    void endBlock(int token);

    void branch(BranchOpcode opcode, int depth, int target);

    void tableBranch(int[] depths, int[] targets, int defaultDepth, int defaultTarget);

    void opcode(Opcode opcode);

    void local(LocalOpcode opcode, int index);

    void binary(WasmIntBinaryOperation opcode, WasmIntType type);

    void binary(WasmFloatBinaryOperation opcode, WasmFloatType type);

    void int32Constant(int value);

    void int64Constant(long value);

    void float32Constant(float value);

    void float64Constant(double value);
}
