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
package org.teavm.model.optimization;

import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.instructions.UnwrapArrayInstruction;
import org.teavm.model.util.DefinitionExtractor;

public class ArrayUnwrapMotion implements MethodOptimization {
    @Override
    public boolean optimize(MethodOptimizationContext context, Program program) {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            optimize(program.basicBlockAt(i));
        }
        return false;
    }

    private void optimize(BasicBlock block) {
        for (Instruction insn : block) {
            if (insn instanceof UnwrapArrayInstruction) {
                UnwrapArrayInstruction unwrap = (UnwrapArrayInstruction) insn;
                Instruction def = whereDefined(insn, unwrap.getArray());
                insn.delete();
                if (def == null) {
                    block.addFirst(unwrap);
                } else {
                    def.insertNext(unwrap);
                    unwrap.setLocation(def.getLocation());
                }
            }
        }
    }

    private Instruction whereDefined(Instruction instruction, Variable var) {
        DefinitionExtractor def = new DefinitionExtractor();
        while (instruction != null) {
            instruction.acceptVisitor(def);
            for (Variable defVar : def.getDefinedVariables()) {
                if (defVar == var) {
                    return instruction;
                }
            }
            instruction = instruction.getPrevious();
        }
        return instruction;
    }
}
