/*
 *  Copyright 2012 Alexey Andreev.
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
package org.teavm.javascript;

import org.teavm.model.*;
import org.teavm.model.util.DefinitionExtractor;
import org.teavm.model.util.UsageExtractor;

/**
 *
 * @author Alexey Andreev
 */
class ReadWriteStatsBuilder {
    public int[] reads;
    public int[] writes;

    public ReadWriteStatsBuilder(int variableCount) {
        reads = new int[variableCount];
        writes = new int[variableCount];
    }

    public void analyze(Program program) {
        DefinitionExtractor defExtractor = new DefinitionExtractor();
        UsageExtractor useExtractor = new UsageExtractor();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block.getInstructions()) {
                insn.acceptVisitor(defExtractor);
                insn.acceptVisitor(useExtractor);
                for (Variable var : defExtractor.getDefinedVariables()) {
                    writes[var.getIndex()]++;
                }
                for (Variable var : useExtractor.getUsedVariables()) {
                    reads[var.getIndex()]++;
                }
            }
            for (Phi phi : block.getPhis()) {
                writes[phi.getReceiver().getIndex()] += phi.getIncomings().size();
                for (Incoming incoming : phi.getIncomings()) {
                    reads[incoming.getValue().getIndex()]++;
                }
            }
            for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
                writes[tryCatch.getExceptionVariable().getIndex()]++;
                reads[tryCatch.getExceptionVariable().getIndex()]++;
            }
        }
    }
}
