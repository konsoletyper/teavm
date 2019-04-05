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
package org.teavm.model.optimization;

import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.Program;
import org.teavm.model.analysis.NullnessInformation;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.util.ProgramUtils;

public class RedundantNullCheckElimination implements MethodOptimization {
    @Override
    public boolean optimize(MethodOptimizationContext context, Program program) {
        NullnessInformation nullness = NullnessInformation.build(program, context.getMethod().getDescriptor());

        boolean hasChanges = false;
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                if (!(instruction instanceof NullCheckInstruction)) {
                    continue;
                }

                NullCheckInstruction nullCheck = (NullCheckInstruction) instruction;
                if (nullness.isSynthesized(nullCheck.getReceiver())) {
                    continue;
                }

                if (nullness.isNotNull(nullCheck.getValue())) {
                    AssignInstruction assign = new AssignInstruction();
                    assign.setAssignee(nullCheck.getValue());
                    assign.setReceiver(nullCheck.getReceiver());
                    assign.setLocation(nullCheck.getLocation());
                    nullCheck.replace(assign);
                    hasChanges = true;
                } else if (nullness.isNull(nullCheck.getValue())) {
                    block.detachSuccessors();
                    while (nullCheck.getNext() != null) {
                        nullCheck.getNext().delete();
                    }

                    nullCheck.insertPreviousAll(ProgramUtils.createThrowNPEInstructions(
                            program, nullCheck.getLocation()));
                    nullCheck.delete();
                    hasChanges = true;
                }
            }
        }

        if (hasChanges) {
            new UnreachableBasicBlockEliminator().optimize(program);
        }

        nullness.dispose();
        return hasChanges;
    }
}
