/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.cache;

import java.util.LinkedHashSet;
import java.util.Set;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.MethodHandle;
import org.teavm.model.Program;
import org.teavm.model.RuntimeConstant;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.PutFieldInstruction;

public class ProgramDependencyExtractor extends AbstractInstructionVisitor {
    private final AnalyzingVisitor visitor = new AnalyzingVisitor();

    public String[] extractDependencies(Program program) {
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                instruction.acceptVisitor(visitor);
            }
        }
        String[] result = visitor.dependencies.toArray(new String[0]);
        visitor.dependencies.clear();
        return result;
    }

    static class AnalyzingVisitor extends AbstractInstructionVisitor {
        Set<String> dependencies = new LinkedHashSet<>();
        @Override public void visit(GetFieldInstruction insn) {
            dependencies.add(insn.getField().getClassName());
        }
        @Override public void visit(PutFieldInstruction insn) {
            dependencies.add(insn.getField().getClassName());
        }
        @Override public void visit(InvokeInstruction insn) {
            dependencies.add(insn.getMethod().getClassName());
        }
        @Override
        public void visit(InvokeDynamicInstruction insn) {
            for (RuntimeConstant cst : insn.getBootstrapArguments()) {
                if (cst.getKind() == RuntimeConstant.METHOD_HANDLE) {
                    MethodHandle handle = cst.getMethodHandle();
                    dependencies.add(handle.getClassName());
                }
            }
        }
    }
}
