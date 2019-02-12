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
    private final boolean onceUsedOnly;

    public DefaultInliningStrategy(int complexityThreshold, int depthThreshold, boolean onceUsedOnly) {
        this.complexityThreshold = complexityThreshold;
        this.depthThreshold = depthThreshold;
        this.onceUsedOnly = onceUsedOnly;
    }

    @Override
    public InliningStep start(MethodReference method, ProgramReader program) {
        int complexity = getComplexity(program);
        if (complexity > complexityThreshold) {
            return null;
        }

        ComplexityHolder complexityHolder = new ComplexityHolder();
        complexityHolder.complexity = complexity;
        return new InliningStepImpl(complexityHolder);
    }

    static int getComplexity(ProgramReader program) {
        int complexity = 0;
        ComplexityCounter counter = new ComplexityCounter();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlockReader block = program.basicBlockAt(i);
            counter.complexity = 0;
            block.readAllInstructions(counter);
            complexity += block.instructionCount() + counter.complexity;
        }
        return complexity;
    }

    class InliningStepImpl implements InliningStep {
        ComplexityHolder complexityHolder;

        InliningStepImpl(ComplexityHolder complexityHolder) {
            this.complexityHolder = complexityHolder;
        }

        @Override
        public InliningStep tryInline(MethodReference method, ProgramReader program, InliningContext context) {
            if (context.getDepth() > depthThreshold || (onceUsedOnly && !context.isUsedOnce(method))) {
                return null;
            }

            int complexity = getComplexity(program);
            if (complexityHolder.complexity + complexity > complexityThreshold) {
                return null;
            }

            complexityHolder.complexity += complexity;
            return new InliningStepImpl(complexityHolder);
        }
    }

    static class ComplexityHolder {
        int complexity;
    }

    static class ComplexityCounter extends AbstractInstructionReader {
        int complexity;

        @Override
        public void nop() {
            complexity--;
        }

        @Override
        public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments, InvocationType type) {
            complexity++;
            if (instance != null) {
                complexity++;
            }
        }

        @Override
        public void choose(VariableReader condition, List<? extends SwitchTableEntryReader> table,
                BasicBlockReader defaultTarget) {
            complexity += 3;
        }

        @Override
        public void jumpIf(BranchingCondition cond, VariableReader operand, BasicBlockReader consequent,
                BasicBlockReader alternative) {
            complexity += 2;
        }

        @Override
        public void jumpIf(BinaryBranchingCondition cond, VariableReader first, VariableReader second,
                BasicBlockReader consequent, BasicBlockReader alternative) {
            complexity += 2;
        }

        @Override
        public void jump(BasicBlockReader target) {
            complexity--;
        }

        @Override
        public void exit(VariableReader valueToReturn) {
            complexity--;
        }
    }
}
