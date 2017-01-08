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
package org.teavm.model.analysis;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
import org.teavm.model.BasicBlock;
import org.teavm.model.Instruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.util.DefinitionExtractor;
import org.teavm.model.util.InstructionVariableMapper;
import org.teavm.model.util.PhiUpdater;

public class NullnessInformation {
    private Program program;
    private BitSet synthesizedVariables;
    private PhiUpdater phiUpdater;
    private BitSet notNullVariables;
    private BitSet nullVariables;

    NullnessInformation(Program program, BitSet synthesizedVariables, PhiUpdater phiUpdater, BitSet notNullVariables,
            BitSet nullVariables) {
        this.program = program;
        this.synthesizedVariables = synthesizedVariables;
        this.phiUpdater = phiUpdater;
        this.notNullVariables = notNullVariables;
        this.nullVariables = nullVariables;
    }

    public boolean isNotNull(Variable variable) {
        return notNullVariables.get(variable.getIndex());
    }

    public boolean isNull(Variable variable) {
        return nullVariables.get(variable.getIndex());
    }

    public void dispose() {
        Set<Phi> phisToRemove = new HashSet<>(phiUpdater.getSynthesizedPhis());
        DefinitionExtractor defExtractor = new DefinitionExtractor();
        InstructionVariableMapper variableMapper = new InstructionVariableMapper(var -> {
            int source = phiUpdater.getSourceVariable(var.getIndex());
            return source >= 0 ? program.variableAt(source) : var;
        });
        for (BasicBlock block : program.getBasicBlocks()) {
            block.getPhis().removeIf(phisToRemove::contains);
            for (Instruction insn : block) {
                insn.acceptVisitor(defExtractor);
                if (Arrays.stream(defExtractor.getDefinedVariables())
                        .anyMatch(var -> synthesizedVariables.get(var.getIndex()))) {
                    insn.delete();
                } else {
                    insn.acceptVisitor(variableMapper);
                }
            }
            variableMapper.applyToPhis(block);
            if (block.getExceptionVariable() != null) {
                block.setExceptionVariable(variableMapper.map(block.getExceptionVariable()));
            }
        }

        for (int i = 0; i < program.variableCount(); ++i) {
            int sourceVar = phiUpdater.getSourceVariable(i);
            if (sourceVar >= 0 && sourceVar != i) {
                program.deleteVariable(i);
            }
        }
        program.pack();
    }

    public static NullnessInformation build(Program program, MethodDescriptor methodDescriptor) {
        NullnessInformationBuilder builder = new NullnessInformationBuilder(program, methodDescriptor);
        builder.build();
        return new NullnessInformation(program, builder.synthesizedVariables, builder.phiUpdater,
                builder.notNullVariables, builder.nullVariables);
    }
}
