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
package org.teavm.backend.wasm.intrinsics;

import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.types.PreciseTypeInference;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.StringConstantInstruction;

public interface WasmGCInlineIntrinsicContext {
    WasmLocal mapToLocal(Variable variable);

    WasmLocal newWasmLocal(WasmType type);

    boolean isAsync();

    boolean isAsyncMethod(MethodReference method);

    MethodReference currentMethod();

    Program currentProgram();

    PreciseTypeInference types();

    /**
     * Returns instruction that defines the given variable, following chains of assignments.
     * Returns {@code null} if variable is not defined by an instruction (e.g. it's a parameter or a phi).
     */
    Instruction definition(Variable variable);

    /**
     * Returns type of the class literal assigned to the given variable, or {@code null} if variable is not
     * a class literal.
     */
    default ValueType classConstant(Variable variable) {
        return definition(variable) instanceof ClassConstantInstruction cst ? cst.getConstant() : null;
    }

    /**
     * Returns value of the string literal assigned to the given variable, or {@code null} if variable is not
     * a string literal.
     */
    default String stringConstant(Variable variable) {
        return definition(variable) instanceof StringConstantInstruction cst ? cst.getConstant() : null;
    }
}
