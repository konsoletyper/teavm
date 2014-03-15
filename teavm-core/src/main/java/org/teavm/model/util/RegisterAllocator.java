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
import org.teavm.common.*;
import org.teavm.model.*;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.JumpInstruction;

/**
 *
 * @author Alexey Andreev
 */
public class RegisterAllocator {
    public void allocateRegisters(MethodReader method, Program program) {
        insertPhiArgumentsCopies(program);
        InterferenceGraphBuilder interferenceBuilder = new InterferenceGraphBuilder();
        LivenessAnalyzer liveness = new LivenessAnalyzer();
        liveness.analyze(program);
        List<MutableGraphNode> interferenceGraph = interferenceBuilder.build(
                program, method.parameterCount(), liveness);
        DisjointSet congruenceClasses = buildPhiCongruenceClasses(program);
        joinClassNodes(interferenceGraph, congruenceClasses);
        removeRedundantCopies(program, interferenceGraph, congruenceClasses);
        int[] classArray = congruenceClasses.pack(program.variableCount());
        renameVariables(program, classArray);
        int[] colors = new int[program.variableCount()];
        Arrays.fill(colors, -1);
        for (int i = 0; i <= method.parameterCount(); ++i) {
            colors[i] = i;
        }
        renameInterferenceGraph(interferenceGraph, congruenceClasses, classArray);
        GraphColorer colorer = new GraphColorer();
        colorer.colorize(interferenceGraph, colors);
        for (int i = 0; i < colors.length; ++i) {
            program.variableAt(i).setRegister(colors[i]);
        }
    }

    private static void joinClassNodes(List<MutableGraphNode> graph, DisjointSet classes) {
        int sz = graph.size();
        for (int i = 0; i < sz; ++i) {
            int cls = classes.find(i);
            while (cls >= graph.size()) {
                graph.add(new MutableGraphNode(graph.size()));
            }
            if (cls != i) {
                for (MutableGraphEdge edge : graph.get(i).getEdges().toArray(new MutableGraphEdge[0])) {
                    if (edge.getFirst() == graph.get(i)) {
                        edge.setFirst(graph.get(cls));
                    }
                    if (edge.getSecond() == graph.get(i)) {
                        edge.setSecond(graph.get(cls));
                    }
                }
                graph.set(i, graph.get(cls));
            }
        }
    }

    private void insertPhiArgumentsCopies(Program program) {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            Map<BasicBlock, BasicBlock> blockMap = new HashMap<>();
            for (Phi phi : program.basicBlockAt(i).getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    if (!isExceptionHandler(incoming)) {
                        insertCopy(incoming, blockMap);
                    }
                }
            }
            for (Phi phi : program.basicBlockAt(i).getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    if (!isExceptionHandler(incoming)) {
                        insertCopy(incoming, blockMap);
                    }
                }
            }
        }
    }

    private void insertCopy(Incoming incoming, Map<BasicBlock, BasicBlock> blockMap) {
        final Phi phi = incoming.getPhi();
        Program program = phi.getBasicBlock().getProgram();
        AssignInstruction copyInstruction = new AssignInstruction();
        Variable firstCopy = program.createVariable();
        copyInstruction.setReceiver(firstCopy);
        copyInstruction.setAssignee(incoming.getValue());
        BasicBlock source = blockMap.get(incoming.getSource());
        if (source == null) {
            source = incoming.getSource();
        } else {
            incoming.setSource(source);
        }
        if (!(incoming.getSource().getLastInstruction() instanceof JumpInstruction)) {
            final BasicBlock copyBlock = program.createBasicBlock();
            JumpInstruction jumpInstruction = new JumpInstruction();
            jumpInstruction.setTarget(phi.getBasicBlock());
            copyBlock.getInstructions().add(jumpInstruction);
            incoming.getSource().getLastInstruction().acceptVisitor(new BasicBlockMapper() {
                @Override protected BasicBlock map(BasicBlock block) {
                    if (block == phi.getBasicBlock()) {
                        return copyBlock;
                    } else {
                        return block;
                    }
                }
            });
            blockMap.put(source, copyBlock);
            incoming.setSource(copyBlock);
            source = copyBlock;
        }
        source.getInstructions().add(source.getInstructions().size() - 1, copyInstruction);
        incoming.setValue(copyInstruction.getReceiver());
    }

    private boolean isExceptionHandler(Incoming incoming) {
        for (TryCatchBlock tryCatch : incoming.getSource().getTryCatchBlocks()) {
            if (tryCatch.getExceptionVariable() == incoming.getValue()) {
                return true;
            }
        }
        return false;
    }

    private void removeRedundantCopies(Program program, List<MutableGraphNode> interferenceGraph,
            DisjointSet congruenceClasses) {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (int j = 0; j < block.getInstructions().size(); ++j) {
                Instruction insn = block.getInstructions().get(j);
                if (!(insn instanceof AssignInstruction)) {
                    continue;
                }
                AssignInstruction assignment = (AssignInstruction)insn;
                boolean interfere = false;
                int copyClass = congruenceClasses.find(assignment.getReceiver().getIndex());
                int origClass = congruenceClasses.find(assignment.getAssignee().getIndex());
                for (MutableGraphEdge edge : interferenceGraph.get(origClass).getEdges()) {
                    if (edge.getFirst() == edge.getSecond()) {
                        continue;
                    }
                    int neighbour = congruenceClasses.find(edge.getSecond().getTag());
                    if (neighbour == copyClass || neighbour == origClass) {
                        interfere = true;
                        break;
                    }
                }
                if (!interfere) {
                    int newClass = congruenceClasses.union(copyClass, origClass);
                    block.getInstructions().set(j, new EmptyInstruction());
                    if (newClass == interferenceGraph.size()) {
                        MutableGraphNode newNode = new MutableGraphNode(interferenceGraph.size());
                        interferenceGraph.add(newNode);
                    }
                    for (MutableGraphEdge edge : interferenceGraph.get(origClass).getEdges()
                            .toArray(new MutableGraphEdge[0])) {
                        if (edge.getFirst() == interferenceGraph.get(origClass)) {
                            edge.setFirst(interferenceGraph.get(newClass));
                        }
                        if (edge.getSecond() == interferenceGraph.get(origClass)) {
                            edge.setSecond(interferenceGraph.get(newClass));
                        }
                    }
                    for (MutableGraphEdge edge : interferenceGraph.get(copyClass).getEdges()
                            .toArray(new MutableGraphEdge[0])) {
                        if (edge.getFirst() == interferenceGraph.get(copyClass)) {
                            edge.setFirst(interferenceGraph.get(newClass));
                        }
                        if (edge.getSecond() == interferenceGraph.get(copyClass)) {
                            edge.setSecond(interferenceGraph.get(newClass));
                        }
                    }
                    interferenceGraph.set(copyClass, interferenceGraph.get(newClass));
                    interferenceGraph.set(origClass, interferenceGraph.get(newClass));
                }
            }
        }
    }

    private void renameVariables(final Program program, final int[] varMap) {
        InstructionVariableMapper mapper = new InstructionVariableMapper() {
            @Override protected Variable map(Variable var) {
                return program.variableAt(varMap[var.getIndex()]);
            }
        };
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block.getInstructions()) {
                insn.acceptVisitor(mapper);
            }
            for (Phi phi : block.getPhis()) {
                phi.setReceiver(program.variableAt(varMap[phi.getReceiver().getIndex()]));
                for (Incoming incoming : phi.getIncomings()) {
                    incoming.setValue(program.variableAt(varMap[incoming.getValue().getIndex()]));
                }
            }
            for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
                tryCatch.setExceptionVariable(program.variableAt(
                        varMap[tryCatch.getExceptionVariable().getIndex()]));
            }
        }
    }

    private void renameInterferenceGraph(List<MutableGraphNode> graph, DisjointSet classes, final int[] varMap) {
        List<MutableGraphNode> newGraph = new ArrayList<>();
        for (int i = 0; i < graph.size(); ++i) {
            int mapped = varMap[i];
            while (newGraph.size() <= mapped) {
                newGraph.add(null);
            }
            if (newGraph.get(mapped) == null) {
                int cls = classes.find(i);
                newGraph.set(mapped, graph.get(cls));
                graph.get(cls).setTag(mapped);
            }
        }
        graph.clear();
        graph.addAll(newGraph);
    }

    private DisjointSet buildPhiCongruenceClasses(Program program) {
        DisjointSet classes = new DisjointSet();
        for (int i = 0; i < program.variableCount(); ++i) {
            classes.create();
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    classes.union(phi.getReceiver().getIndex(), incoming.getValue().getIndex());
                }
            }
        }
        return classes;
    }
}
