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

import java.util.ArrayList;
import java.util.List;
import org.teavm.model.*;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;
import org.teavm.model.util.DefinitionExtractor;

public class ArrayUnwrapMotion implements MethodOptimization {
    @Override
    public boolean optimize(MethodReader method, Program program) {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            optimize(program.basicBlockAt(i));
        }
        return false;
    }

    private void optimize(BasicBlock block) {
        List<Instruction> newInstructions = new ArrayList<>();
        List<Instruction> instructions = block.getInstructions();
        for (int i = 0; i < instructions.size(); ++i) {
            Instruction insn = instructions.get(i);
            if (insn instanceof UnwrapArrayInstruction) {
                UnwrapArrayInstruction unwrap = (UnwrapArrayInstruction) insn;
                EmptyInstruction empty = new EmptyInstruction();
                empty.setLocation(unwrap.getLocation());
                instructions.set(i, empty);
                int def = whereDefined(instructions, i, unwrap.getArray());
                if (def < 0) {
                    newInstructions.add(unwrap);
                } else {
                    instructions.add(def + 1, unwrap);
                    unwrap.setLocation(instructions.get(def).getLocation());
                    ++i;
                }
            }
        }
        if (!newInstructions.isEmpty()) {
            instructions.addAll(0, newInstructions);
        }
    }

    private int whereDefined(List<Instruction> instructions, int index, Variable var) {
        DefinitionExtractor def = new DefinitionExtractor();
        while (index >= 0) {
            Instruction insn = instructions.get(index);
            insn.acceptVisitor(def);
            for (Variable defVar : def.getDefinedVariables()) {
                if (defVar == var) {
                    return index;
                }
            }
            --index;
        }
        return index;
    }
}
