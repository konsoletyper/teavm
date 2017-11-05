/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.model.util;

import java.util.*;
import org.teavm.common.MutableGraphNode;
import org.teavm.model.*;

class InterferenceGraphBuilder {
    public List<MutableGraphNode> build(Program program, int paramCount, LivenessAnalyzer liveness) {
        List<MutableGraphNode> nodes = new ArrayList<>();
        for (int i = 0; i < program.variableCount(); ++i) {
            nodes.add(new MutableGraphNode(i));
        }
        UsageExtractor useExtractor = new UsageExtractor();
        DefinitionExtractor defExtractor = new DefinitionExtractor();
        InstructionTransitionExtractor succExtractor = new InstructionTransitionExtractor();
        List<List<Incoming>> outgoings = ProgramUtils.getPhiOutputs(program);
        BitSet live = new BitSet();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            block.getLastInstruction().acceptVisitor(succExtractor);

            BitSet liveOut = new BitSet(program.variableCount());
            for (BasicBlock succ : succExtractor.getTargets()) {
                liveOut.or(liveness.liveIn(succ.getIndex()));
            }
            for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
                liveOut.or(liveness.liveIn(tryCatch.getHandler().getIndex()));
            }
            live.clear();
            for (int j = 0; j < liveOut.length(); ++j) {
                if (liveOut.get(j)) {
                    live.set(j);
                }
            }

            for (Incoming outgoing : outgoings.get(i)) {
                live.set(outgoing.getValue().getIndex());
            }

            for (Instruction insn = block.getLastInstruction(); insn != null; insn = insn.getPrevious()) {
                insn.acceptVisitor(useExtractor);
                insn.acceptVisitor(defExtractor);
                for (Variable var : defExtractor.getDefinedVariables()) {
                    connect(nodes, var.getIndex(), live);
                }
                for (Variable var : defExtractor.getDefinedVariables()) {
                    live.clear(var.getIndex());
                }
                for (Variable var : useExtractor.getUsedVariables()) {
                    live.set(var.getIndex());
                }
            }
            if (block.getExceptionVariable() != null) {
                connect(nodes, block.getExceptionVariable().getIndex(), live);
                live.clear(block.getExceptionVariable().getIndex());
            }
            if (block.getIndex() == 0) {
                for (int j = 0; j <= paramCount; ++j) {
                    connect(nodes, j, live);
                }
            }

            BitSet liveIn = liveness.liveIn(i);
            live.clear();
            for (int j = 0; j < liveOut.length(); ++j) {
                if (liveIn.get(j)) {
                    live.set(j);
                }
            }

            for (Phi phi : block.getPhis()) {
                live.set(phi.getReceiver().getIndex());
            }

            for (Phi phi : block.getPhis()) {
                connect(nodes, phi.getReceiver().getIndex(), live);
            }
        }
        return nodes;
    }

    private void connect(List<MutableGraphNode> nodes, int fromIndex, BitSet to) {
        MutableGraphNode from = nodes.get(fromIndex);
        List<MutableGraphNode> toList = new ArrayList<>(to.cardinality());
        for (int i = to.nextSetBit(0); i >= 0; i = to.nextSetBit(i + 1)) {
            toList.add(nodes.get(i));
        }
        from.connectAll(toList);
    }
}
