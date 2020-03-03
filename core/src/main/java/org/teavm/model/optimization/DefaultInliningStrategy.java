/*
 *  Copyright 2019 Alexey Andreev.
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

import java.util.List;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ProgramReader;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.AbstractInstructionReader;
import org.teavm.model.instructions.BinaryBranchingCondition;
import org.teavm.model.instructions.BranchingCondition;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.SwitchTableEntryReader;

public class DefaultInliningStrategy implements InliningStrategy {
    private final int complexityThreshold;
    private final int depthThreshold;
    private final int totalComplexityThreshold;
    private final boolean onceUsedOnly;

    public DefaultInliningStrategy(int complexityThreshold, int depthThreshold, int totalComplexityThreshold,
            boolean onceUsedOnly) {
        this.complexityThreshold = complexityThreshold;
        this.depthThreshold = depthThreshold;
        this.totalComplexityThreshold = totalComplexityThreshold;
        this.onceUsedOnly = onceUsedOnly;
    }

    @Override
    public InliningStep start(MethodReference method, ProgramReader program) {
        Complexity complexity = getComplexity(program, null);
        if (complexity.score > totalComplexityThreshold) {
            return null;
        }

        ComplexityHolder complexityHolder = new ComplexityHolder();
        complexityHolder.complexity = complexity.score;
        return new InliningStepImpl(complexityHolder);
    }

    private static Complexity getComplexity(ProgramReader program, InliningContext context) {
        int complexity = 0;
        ComplexityCounter counter = new ComplexityCounter(context);
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlockReader block = program.basicBlockAt(i);
            counter.complexity = 0;
            block.readAllInstructions(counter);
            complexity += block.instructionCount() + counter.complexity;
        }
        Complexity result = new Complexity();
        result.score = complexity;
        result.callsToUsedOnceMethods = counter.callsToUsedOnceMethods;
        return result;
    }

    class InliningStepImpl implements InliningStep {
        ComplexityHolder complexityHolder;

        InliningStepImpl(ComplexityHolder complexityHolder) {
            this.complexityHolder = complexityHolder;
        }

        @Override
        public InliningStep tryInline(MethodReference method, ProgramReader program, InliningContext context) {
            if (context.getDepth() > depthThreshold) {
                return null;
            }

            Complexity complexity = getComplexity(program, context);
            if (onceUsedOnly && !context.isUsedOnce(method)) {
                if (complexity.callsToUsedOnceMethods || complexity.score > 1) {
                    return null;
                }
            }

            if (complexity.score > complexityThreshold
                    || complexityHolder.complexity + complexity.score > totalComplexityThreshold) {
                return null;
            }

            complexityHolder.complexity += complexity.score;
            return new InliningStepImpl(complexityHolder);
        }
    }

    static class ComplexityHolder {
        int complexity;
    }

    static class ComplexityCounter extends AbstractInstructionReader {
        InliningContext context;
        int complexity;
        boolean callsToUsedOnceMethods;

        ComplexityCounter(InliningContext context) {
            this.context = context;
        }

        @Override
        public void nop() {
            complexity--;
        }

        @Override
        public void assign(VariableReader receiver, VariableReader assignee) {
            complexity--;
        }

        @Override
        public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments, InvocationType type) {
            if (type == InvocationType.SPECIAL && context != null && context.isUsedOnce(method)) {
                ProgramReader program = context.getProgram(method);
                if (!isTrivialCall(program)) {
                    callsToUsedOnceMethods = true;
                }
            }
        }

        private boolean isTrivialCall(ProgramReader program) {
            if (program == null) {
               return false;
            }
            Complexity complexity = getComplexity(program, context);
            return complexity.score <= 1 && !complexity.callsToUsedOnceMethods;
        }

        @Override
        public void choose(VariableReader condition, List<? extends SwitchTableEntryReader> table,
                BasicBlockReader defaultTarget) {
            complexity += 2;
        }

        @Override
        public void jumpIf(BranchingCondition cond, VariableReader operand, BasicBlockReader consequent,
                BasicBlockReader alternative) {
            complexity += 1;
        }

        @Override
        public void jumpIf(BinaryBranchingCondition cond, VariableReader first, VariableReader second,
                BasicBlockReader consequent, BasicBlockReader alternative) {
            complexity += 1;
        }

        @Override
        public void jump(BasicBlockReader target) {
            complexity--;
        }

        @Override
        public void exit(VariableReader valueToReturn) {
            complexity--;
        }

        @Override
        public void raise(VariableReader exception) {
            complexity--;
        }
    }

    static class Complexity {
        int score;
        boolean callsToUsedOnceMethods;
    }
}
