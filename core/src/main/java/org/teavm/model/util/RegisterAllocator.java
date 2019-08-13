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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.teavm.common.DisjointSet;
import org.teavm.common.MutableGraphEdge;
import org.teavm.common.MutableGraphNode;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.ProgramReader;
import org.teavm.model.TextLocation;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.JumpInstruction;

public class RegisterAllocator {
    public void allocateRegisters(MethodReference method, Program program, boolean debuggerFriendly) {
        insertPhiArgumentsCopies(program);
        InterferenceGraphBuilder interferenceBuilder = new InterferenceGraphBuilder();
        LivenessAnalyzer liveness = new LivenessAnalyzer();
        liveness.analyze(program, method.getDescriptor());
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

        int maxClass = 0;
        for (int cls : classArray) {
            maxClass = Math.max(maxClass, cls + 1);
        }
        int[] categories = getVariableCategories(program, method);
        String[] names = getVariableNames(program, debuggerFriendly);
        colorer.colorize(MutableGraphNode.toGraph(interferenceGraph), colors, categories, names);

        int maxColor = 0;
        for (int i = 0; i < colors.length; ++i) {
            program.variableAt(i).setRegister(colors[i]);
            maxColor = Math.max(maxColor, colors[i] + 1);
        }

        String[] namesByRegister = new String[maxColor];
        for (int i = 0; i < program.variableCount(); ++i) {
            Variable var = program.variableAt(i);
            if (var.getDebugName() != null && var.getRegister() >= 0) {
                namesByRegister[var.getRegister()] = names[i];
            }
        }
        for (int i = 0; i < program.variableCount(); ++i) {
            Variable var = program.variableAt(i);
            if (var.getRegister() >= 0) {
                var.setDebugName(namesByRegister[var.getRegister()]);
            }
        }

        for (int i = 0; i < program.basicBlockCount(); ++i) {
            program.basicBlockAt(i).getPhis().clear();
        }
    }

    private int[] getVariableCategories(ProgramReader program, MethodReference method) {
        TypeInferer inferer = new TypeInferer();
        inferer.inferTypes(program, method);
        int[] categories = new int[program.variableCount()];
        for (int i = 0; i < program.variableCount(); ++i) {
            categories[i] = getCategory(inferer.typeOf(i));
        }
        return categories;
    }

    private int getCategory(VariableType type) {
        if (type == null) {
            return 255;
        }
        switch (type) {
            case INT:
                return 0;
            case LONG:
                return 1;
            case FLOAT:
                return 2;
            case DOUBLE:
                return 3;
            case OBJECT:
                return 4;
            default:
                return 5;
        }
    }

    private String[] getVariableNames(ProgramReader program, boolean debuggerFriendly) {
        String[] names = new String[program.variableCount()];
        for (int i = 0; i < names.length; ++i) {
            names[i] = program.variableAt(i).getDebugName();
            if (debuggerFriendly && names[i] == null) {
                names[i] = "";
            }
        }
        return names;
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
        List<List<Incoming>> catchIncomingsByVariable = new ArrayList<>(
                Collections.nCopies(program.variableCount(), null));

        int sz = program.basicBlockCount();
        for (int i = 0; i < sz; ++i) {
            BasicBlock block = program.basicBlockAt(i);
            Map<BasicBlock, BasicBlock> blockMap = new HashMap<>();
            List<Incoming> incomingsToRepeat = new ArrayList<>();
            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    boolean fromTry = incoming.getSource().getTryCatchBlocks().stream()
                            .anyMatch(tryCatch -> tryCatch.getHandler() == incoming.getPhi().getBasicBlock());
                    if (fromTry) {
                        int valueIndex = incoming.getValue().getIndex();
                        List<Incoming> catchIncomings = catchIncomingsByVariable.get(valueIndex);
                        if (catchIncomings == null) {
                            catchIncomings = new ArrayList<>(1);
                            catchIncomingsByVariable.set(valueIndex, catchIncomings);
                        }
                        catchIncomings.add(incoming);
                    } else {
                        insertCopy(incoming, blockMap);
                        incomingsToRepeat.add(incoming);
                    }
                }
            }

            for (Incoming incoming : incomingsToRepeat) {
                insertCopy(incoming, blockMap);
            }
        }

        DefinitionExtractor definitionExtractor = new DefinitionExtractor();
        List<Instruction> nextInstructions = new ArrayList<>();
        for (BasicBlock block : program.getBasicBlocks()) {
            for (Phi phi : block.getPhis()) {
                addExceptionHandlingCopies(catchIncomingsByVariable, phi.getReceiver(), block,
                        program, block.getFirstInstruction().getLocation(), nextInstructions);
            }

            if (!nextInstructions.isEmpty()) {
                block.addFirstAll(nextInstructions);
                nextInstructions.clear();
            }

            for (Instruction instruction : block) {
                instruction.acceptVisitor(definitionExtractor);
                Variable[] definedVariables = definitionExtractor.getDefinedVariables();
                for (Variable definedVariable : definedVariables) {
                    addExceptionHandlingCopies(catchIncomingsByVariable, definedVariable, block,
                            program, instruction.getLocation(), nextInstructions);
                }

                if (!nextInstructions.isEmpty()) {
                    instruction.insertNextAll(nextInstructions);
                    nextInstructions.clear();
                }
            }
        }

        for (List<Incoming> remainingIncomings : catchIncomingsByVariable) {
            if (remainingIncomings == null) {
                continue;
            }
            for (Incoming incoming : remainingIncomings) {
                BasicBlock block = incoming.getSource();

                Variable copy = program.createVariable();
                copy.setLabel(incoming.getPhi().getReceiver().getLabel());
                copy.setDebugName(incoming.getPhi().getReceiver().getDebugName());

                AssignInstruction copyInstruction = new AssignInstruction();
                copyInstruction.setReceiver(copy);
                copyInstruction.setAssignee(incoming.getValue());
                copyInstruction.setLocation(block.getFirstInstruction().getLocation());
                incoming.setValue(copy);

                block.addFirst(copyInstruction);
            }
        }
    }

    private void addExceptionHandlingCopies(List<List<Incoming>> catchIncomingsByVariable, Variable definedVariable,
            BasicBlock block, Program program, TextLocation location, List<Instruction> nextInstructions) {
        if (definedVariable.getIndex() >= catchIncomingsByVariable.size()) {
            return;
        }
        List<Incoming> catchIncomings = catchIncomingsByVariable.get(definedVariable.getIndex());
        if (catchIncomings == null) {
            return;
        }

        for (Iterator<Incoming> iter = catchIncomings.iterator(); iter.hasNext();) {
            Incoming incoming = iter.next();
            if (incoming.getSource() == block) {
                Variable copy = program.createVariable();
                copy.setLabel(incoming.getPhi().getReceiver().getLabel());
                copy.setDebugName(incoming.getPhi().getReceiver().getDebugName());

                AssignInstruction copyInstruction = new AssignInstruction();
                copyInstruction.setReceiver(copy);
                copyInstruction.setAssignee(incoming.getValue());
                copyInstruction.setLocation(location);

                incoming.setValue(copy);
                nextInstructions.add(copyInstruction);

                iter.remove();
            }
        }
    }

    private void insertCopy(Incoming incoming, Map<BasicBlock, BasicBlock> blockMap) {
        Phi phi = incoming.getPhi();
        Program program = phi.getBasicBlock().getProgram();
        AssignInstruction copyInstruction = new AssignInstruction();
        Variable firstCopy = program.createVariable();
        firstCopy.setLabel(phi.getReceiver().getLabel());
        firstCopy.setDebugName(phi.getReceiver().getDebugName());
        copyInstruction.setReceiver(firstCopy);
        copyInstruction.setAssignee(incoming.getValue());
        BasicBlock source = blockMap.get(incoming.getSource());
        if (source == null) {
            source = incoming.getSource();
        } else {
            incoming.setSource(source);
        }
        if (!(incoming.getSource().getLastInstruction() instanceof JumpInstruction)) {
            BasicBlock copyBlock = program.createBasicBlock();
            JumpInstruction jumpInstruction = new JumpInstruction();
            jumpInstruction.setLocation(incoming.getSource().getLastInstruction().getLocation());
            jumpInstruction.setTarget(phi.getBasicBlock());
            copyBlock.add(jumpInstruction);
            incoming.getSource().getLastInstruction().acceptVisitor(new BasicBlockMapper((int block) ->
                    block == phi.getBasicBlock().getIndex() ? copyBlock.getIndex() : block));
            blockMap.put(source, copyBlock);
            incoming.setSource(copyBlock);
            source = copyBlock;
        }
        source.getLastInstruction().insertPrevious(copyInstruction);
        incoming.setValue(copyInstruction.getReceiver());
    }

    private void removeRedundantCopies(Program program, List<MutableGraphNode> interferenceGraph,
            DisjointSet congruenceClasses) {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            Instruction nextInsn;
            for (Instruction insn = block.getFirstInstruction(); insn != null; insn = nextInsn) {
                nextInsn = insn.getNext();
                if (!(insn instanceof AssignInstruction)) {
                    continue;
                }
                AssignInstruction assignment = (AssignInstruction) insn;
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
                    insn.delete();
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

    private void renameVariables(Program program, int[] varMap) {
        InstructionVariableMapper mapper = new InstructionVariableMapper(var ->
                program.variableAt(varMap[var.getIndex()]));
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            mapper.apply(block);
        }
        String[] originalNames = getVariableNames(program, false);
        for (int i = 0; i < program.variableCount(); ++i) {
            program.variableAt(i).setDebugName(null);
        }
        for (int i = 0; i < program.variableCount(); ++i) {
            Variable var = program.variableAt(varMap[i]);
            if (originalNames[i] != null) {
                var.setDebugName(originalNames[i]);
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
