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
import org.teavm.common.DisjointSet;
import org.teavm.common.Graph;
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
        List<PhiArgumentCopy> phiArgsCopies = insertPhiArgumentsCopies(program);
        InterferenceGraphBuilder interferenceBuilder = new InterferenceGraphBuilder();
        LivenessAnalyzer liveness = new LivenessAnalyzer();
        liveness.analyze(program);
        Graph interferenceGraph = interferenceBuilder.build(program, method.parameterCount(), liveness);
        DisjointSet congruenceClasses = buildPhiCongruenceClasses(program);
        removeRedundantCopies(program, phiArgsCopies, interferenceGraph, congruenceClasses);
        int[] classArray = congruenceClasses.pack(program.variableCount());
        int[] colors = new int[program.variableCount()];
        Arrays.fill(colors, -1);
        for (int i = 0; i <= method.parameterCount(); ++i) {
            colors[i] = i;
        }
        GraphColorer colorer = new GraphColorer();
        colorer.colorize(interferenceGraph, classArray, colors);
        for (int i = 0; i < colors.length; ++i) {
            program.variableAt(i).setRegister(colors[i]);
        }
    }

    private static class PhiArgumentCopy {
        Incoming incoming;
        int original;
        int index;
        BasicBlock block;
        int var;
    }

    private List<PhiArgumentCopy> insertPhiArgumentsCopies(Program program) {
        List<PhiArgumentCopy> copies = new ArrayList<>();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            Map<BasicBlock, BasicBlock> blockMap = new HashMap<>();
            for (final Phi phi : program.basicBlockAt(i).getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    PhiArgumentCopy copy = new PhiArgumentCopy();
                    copy.incoming = incoming;
                    copy.original = incoming.getValue().getIndex();
                    AssignInstruction copyInstruction = new AssignInstruction();
                    copyInstruction.setReceiver(program.createVariable());
                    copyInstruction.setAssignee(incoming.getValue());
                    copy.var = copyInstruction.getReceiver().getIndex();
                    BasicBlock source = blockMap.get(incoming.getSource());
                    if (source == null) {
                        source = incoming.getSource();
                    } else {
                        incoming.setSource(source);
                    }
                    if (incoming.getSource().getLastInstruction() instanceof JumpInstruction) {
                        copy.index = incoming.getSource().getInstructions().size() - 1;
                        copy.block = incoming.getSource();
                        copy.block.getInstructions().add(copy.index, copyInstruction);
                    } else {
                        final BasicBlock copyBlock = program.createBasicBlock();
                        copyBlock.getInstructions().add(copyInstruction);
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
                        copy.index = 0;
                        copy.block = incoming.getSource();
                    }
                    incoming.setValue(copyInstruction.getReceiver());
                    copies.add(copy);
                }
            }
        }
        return copies;
    }

    private void removeRedundantCopies(Program program, List<PhiArgumentCopy> copies, Graph inteferenceGraph,
            DisjointSet congruenceClasses) {
        for (PhiArgumentCopy copy : copies) {
            boolean interfere = false;
            for (int neighbour : inteferenceGraph.outgoingEdges(copy.original)) {
                if (neighbour == copy.var || neighbour == copy.original) {
                    continue;
                }
                if (congruenceClasses.find(neighbour) == congruenceClasses.find(copy.var) ||
                        congruenceClasses.find(neighbour) == congruenceClasses.find(copy.original)) {
                    interfere = true;
                    break;
                }
            }
            if (!interfere) {
                congruenceClasses.union(copy.var, copy.original);
                copy.block.getInstructions().set(copy.index, new EmptyInstruction());
                copy.incoming.setValue(program.variableAt(copy.original));
            }
        }
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
