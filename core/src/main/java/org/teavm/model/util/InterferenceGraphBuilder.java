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
        Set<MutableGraphNode> live = new HashSet<>(128);
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
                    live.add(nodes.get(j));
                }
            }

            for (Incoming outgoing : outgoings.get(i)) {
                live.add(nodes.get(outgoing.getValue().getIndex()));
            }

            for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
                for (TryCatchJoint joint : tryCatch.getJoints()) {
                    for (Variable sourceVar : joint.getSourceVariables()) {
                        live.add(nodes.get(sourceVar.getIndex()));
                    }
                    live.remove(nodes.get(joint.getReceiver().getIndex()));
                }
            }

            for (int j = block.getInstructions().size() - 1; j >= 0; --j) {
                Instruction insn = block.getInstructions().get(j);
                insn.acceptVisitor(useExtractor);
                insn.acceptVisitor(defExtractor);
                for (Variable var : defExtractor.getDefinedVariables()) {
                    nodes.get(var.getIndex()).connectAll(live);
                }
                for (Variable var : defExtractor.getDefinedVariables()) {
                    live.remove(nodes.get(var.getIndex()));
                }
                for (Variable var : useExtractor.getUsedVariables()) {
                    live.add(nodes.get(var.getIndex()));
                }
            }
            if (block.getExceptionVariable() != null) {
                nodes.get(block.getExceptionVariable().getIndex()).connectAll(live);
                live.remove(nodes.get(block.getExceptionVariable().getIndex()));
            }
            if (block.getIndex() == 0) {
                for (int j = 0; j <= paramCount; ++j) {
                    nodes.get(j).connectAll(live);
                }
            }

            BitSet liveIn = liveness.liveIn(i);
            live.clear();
            for (int j = 0; j < liveOut.length(); ++j) {
                if (liveIn.get(j)) {
                    live.add(nodes.get(j));
                }
            }

            for (Phi phi : block.getPhis()) {
                live.add(nodes.get(phi.getReceiver().getIndex()));
            }

            for (Phi phi : block.getPhis()) {
                nodes.get(phi.getReceiver().getIndex()).connectAll(live);
            }
        }
        return nodes;
    }
}
