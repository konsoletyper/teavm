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
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class InterferenceGraphBuilder {
    public Graph build(Program program, LivenessAnalyzer liveness) {
        List<Set<Integer>> edges = new ArrayList<>();
        for (int i = 0; i < program.variableCount(); ++i) {
            edges.add(new HashSet<Integer>());
        }
        UsageExtractor useExtractor = new UsageExtractor();
        DefinitionExtractor defExtractor = new DefinitionExtractor();
        InstructionTransitionExtractor succExtractor = new InstructionTransitionExtractor();
        List<List<Incoming>> outgoings = getOutgoings(program);
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            block.getLastInstruction().acceptVisitor(succExtractor);
            BitSet liveOut = new BitSet(program.variableCount());
            for (BasicBlock succ : succExtractor.getTargets()) {
                liveOut.or(liveness.liveIn(succ.getIndex()));
            }
            Set<Integer> live = new HashSet<>();
            for (int j = 0; j < liveOut.length(); ++j) {
                if (liveOut.get(j)) {
                    live.add(j);
                }
            }
            for (Incoming outgoing : outgoings.get(i)) {
                live.add(outgoing.getValue().getIndex());
            }
            for (int j = block.getInstructions().size() - 1; j >= 0; --j) {
                Instruction insn = block.getInstructions().get(j);
                insn.acceptVisitor(useExtractor);
                insn.acceptVisitor(defExtractor);
                for (Variable var : defExtractor.getDefinedVariables()) {
                    edges.get(var.getIndex()).addAll(live);
                }
                for (Variable var : defExtractor.getDefinedVariables()) {
                    live.remove(var.getIndex());
                }
                for (Variable var : useExtractor.getUsedVariables()) {
                    live.add(var.getIndex());
                }
            }
            BitSet liveIn = liveness.liveIn(i);
            live = new HashSet<>();
            for (int j = 0; j < liveOut.length(); ++j) {
                if (liveIn.get(j)) {
                    live.add(j);
                }
            }
            for (Phi phi : block.getPhis()) {
                live.add(phi.getReceiver().getIndex());
            }
            for (Phi phi : block.getPhis()) {
                edges.get(phi.getReceiver().getIndex()).addAll(live);
            }
        }
        GraphBuilder builder = new GraphBuilder();
        for (int i = 0; i < edges.size(); ++i) {
            for (Integer j : edges.get(i)) {
                builder.addEdge(i, j);
                builder.addEdge(j, i);
            }
        }
        return builder.build();
    }

    private List<List<Incoming>> getOutgoings(Program program) {
        List<List<Incoming>> outgoings = new ArrayList<>();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            outgoings.add(new ArrayList<Incoming>());
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Phi phi : block.getPhis()) {
                for(Incoming incoming : phi.getIncomings()) {
                    outgoings.get(incoming.getSource().getIndex()).add(incoming);
                }
            }
        }
        return outgoings;
    }
}
