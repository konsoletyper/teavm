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
package org.teavm.model.lowlevel;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.common.GraphUtils;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.util.DefinitionExtractor;
import org.teavm.model.util.GraphColorer;
import org.teavm.model.util.LivenessAnalyzer;
import org.teavm.model.util.ProgramUtils;
import org.teavm.model.util.TypeInferer;
import org.teavm.model.util.UsageExtractor;
import org.teavm.model.util.VariableType;
import org.teavm.runtime.ShadowStack;

public class GCShadowStackContributor {
    private Characteristics characteristics;

    public GCShadowStackContributor(Characteristics characteristics) {
        this.characteristics = characteristics;
    }

    public int contribute(Program program, MethodReader method) {
        List<Map<Instruction, BitSet>> liveInInformation = findCallSiteLiveIns(program, method);

        Graph interferenceGraph = buildInterferenceGraph(liveInInformation, program);
        boolean[] spilled = getAffectedVariables(liveInInformation, program);
        int[] colors = new int[interferenceGraph.size()];
        Arrays.fill(colors, -1);
        new GraphColorer().colorize(interferenceGraph, colors);

        int usedColors = 0;
        for (int var = 0; var < colors.length; ++var) {
            if (spilled[var]) {
                usedColors = Math.max(usedColors, colors[var]);
                colors[var]--;
            }
        }
        if (usedColors == 0) {
            return 0;
        }

        // If a variable is spilled to stack, then phi which input this variable also spilled to stack
        // If all of phi inputs are spilled to stack, then we don't need to insert spilling instruction
        // for this phi.
        List<Set<Phi>> destinationPhis = getDestinationPhis(program);
        int[] inputCount = getInputCount(program);
        boolean[] autoSpilled = new boolean[spilled.length];
        for (int i = 0; i < spilled.length; ++i) {
            findAutoSpilledPhis(spilled, destinationPhis, inputCount, autoSpilled, i);
        }

        List<Map<Instruction, int[]>> liveInStores = reduceGCRootStores(program, usedColors, liveInInformation,
                colors, autoSpilled);
        putLiveInGCRoots(program, liveInStores);

        return usedColors;
    }

    private void findAutoSpilledPhis(boolean[] spilled, List<Set<Phi>> destinationPhis, int[] inputCount,
            boolean[] autoSpilled, int i) {
        if (spilled[i]) {
            Set<Phi> phis = destinationPhis.get(i);
            if (phis != null) {
                for (Phi phi : destinationPhis.get(i)) {
                    int destination = phi.getReceiver().getIndex();
                    autoSpilled[destination] = --inputCount[destination] == 0;
                    if (!spilled[destination]) {
                        spilled[destination] = true;
                        if (i > destination) {
                            findAutoSpilledPhis(spilled, destinationPhis, inputCount, autoSpilled, destination);
                        }
                    }
                }
            }
        }
    }

    private List<Map<Instruction, BitSet>> findCallSiteLiveIns(Program program, MethodReader method) {
        Graph cfg = ProgramUtils.buildControlFlowGraph(program);
        TypeInferer typeInferer = new TypeInferer();
        typeInferer.inferTypes(program, method.getReference());
        List<Map<Instruction, BitSet>> liveInInformation = new ArrayList<>();

        LivenessAnalyzer livenessAnalyzer = new LivenessAnalyzer();
        livenessAnalyzer.analyze(program);
        DefinitionExtractor defExtractor = new DefinitionExtractor();
        UsageExtractor useExtractor = new UsageExtractor();

        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            Map<Instruction, BitSet> blockLiveIn = new HashMap<>();
            liveInInformation.add(blockLiveIn);
            BitSet currentLiveOut = new BitSet();
            for (int successor : cfg.outgoingEdges(i)) {
                currentLiveOut.or(livenessAnalyzer.liveIn(successor));
            }

            for (Instruction insn = block.getLastInstruction(); insn != null; insn = insn.getPrevious()) {
                insn.acceptVisitor(defExtractor);
                insn.acceptVisitor(useExtractor);
                for (Variable usedVar : useExtractor.getUsedVariables()) {
                    currentLiveOut.set(usedVar.getIndex());
                }
                for (Variable definedVar : defExtractor.getDefinedVariables()) {
                    currentLiveOut.clear(definedVar.getIndex());
                }
                if (insn instanceof InvokeInstruction || insn instanceof InitClassInstruction
                        || insn instanceof ConstructInstruction || insn instanceof ConstructArrayInstruction
                        || insn instanceof ConstructMultiArrayInstruction
                        || insn instanceof CloneArrayInstruction || insn instanceof RaiseInstruction) {
                    if (insn instanceof InvokeInstruction
                            && !characteristics.isManaged(((InvokeInstruction) insn).getMethod())) {
                        continue;
                    }

                    BitSet csLiveIn = (BitSet) currentLiveOut.clone();
                    for (int v = csLiveIn.nextSetBit(0); v >= 0; v = csLiveIn.nextSetBit(v + 1)) {
                        if (!isReference(typeInferer, v)) {
                            csLiveIn.clear(v);
                        }
                    }
                    csLiveIn.clear(0, method.parameterCount() + 1);
                    blockLiveIn.put(insn, csLiveIn);
                }
            }
            if (block.getExceptionVariable() != null) {
                currentLiveOut.clear(block.getExceptionVariable().getIndex());
            }
        }

        return liveInInformation;
    }

    private Graph buildInterferenceGraph(List<Map<Instruction, BitSet>> liveInInformation, Program program) {
        GraphBuilder builder = new GraphBuilder(program.variableCount());
        for (Map<Instruction, BitSet> blockLiveIn : liveInInformation) {
            for (BitSet liveVarsSet : blockLiveIn.values()) {
                IntArrayList liveVars = new IntArrayList();
                for (int i = liveVarsSet.nextSetBit(0); i >= 0; i = liveVarsSet.nextSetBit(i + 1)) {
                    liveVars.add(i);
                }
                int[] liveVarArray = liveVars.toArray();
                for (int i = 0; i < liveVarArray.length - 1; ++i) {
                    for (int j = i + 1; j < liveVarArray.length; ++j) {
                        builder.addEdge(liveVarArray[i], liveVarArray[j]);
                        builder.addEdge(liveVarArray[j], liveVarArray[i]);
                    }
                }
            }
        }
        return builder.build();
    }

    private boolean[] getAffectedVariables(List<Map<Instruction, BitSet>> liveInInformation, Program program) {
        boolean[] affectedVariables = new boolean[program.variableCount()];
        for (Map<Instruction, BitSet> blockLiveIn : liveInInformation) {
            for (BitSet liveVarsSet : blockLiveIn.values()) {
                for (int i = liveVarsSet.nextSetBit(0); i >= 0; i = liveVarsSet.nextSetBit(i + 1)) {
                    affectedVariables[i] = true;
                }
            }
        }
        return affectedVariables;
    }

    private List<Set<Phi>> getDestinationPhis(Program program) {
        List<Set<Phi>> destinationPhis = new ArrayList<>();
        destinationPhis.addAll(Collections.nCopies(program.variableCount(), null));

        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    Set<Phi> phis = destinationPhis.get(incoming.getValue().getIndex());
                    if (phis == null) {
                        phis = new LinkedHashSet<>();
                        destinationPhis.set(incoming.getValue().getIndex(), phis);
                    }
                    phis.add(phi);
                }
            }
        }

        return destinationPhis;
    }

    private int[] getInputCount(Program program) {
        int[] inputCount = new int[program.variableCount()];

        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Phi phi : block.getPhis()) {
                inputCount[phi.getReceiver().getIndex()] = phi.getIncomings().size();
            }
        }

        return inputCount;
    }

    private List<Map<Instruction, int[]>> reduceGCRootStores(Program program, int usedColors,
            List<Map<Instruction, BitSet>> liveInInformation, int[] colors, boolean[] autoSpilled) {
        class Step {
            private final int node;
            private final int[] slotStates = new int[usedColors];
            private Step(int node) {
                this.node = node;
            }
        }

        List<Map<Instruction, int[]>> slotsToUpdate = new ArrayList<>();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            slotsToUpdate.add(new LinkedHashMap<>());
        }

        Graph cfg = ProgramUtils.buildControlFlowGraph(program);
        DominatorTree dom = GraphUtils.buildDominatorTree(cfg);
        Graph domGraph = GraphUtils.buildDominatorGraph(dom, cfg.size());

        Step[] stack = new Step[program.basicBlockCount() * 2];
        int head = 0;
        Step start = new Step(0);
        Arrays.fill(start.slotStates, usedColors);
        stack[head++] = start;

        while (head > 0) {
            Step step = stack[--head];

            int[] previousStates = step.slotStates;
            int[] states = previousStates.clone();

            Map<Instruction, BitSet> callSites = liveInInformation.get(step.node);
            Map<Instruction, int[]> updatesByCallSite = slotsToUpdate.get(step.node);
            for (Instruction callSiteLocation : sortInstructions(callSites.keySet(), program.basicBlockAt(step.node))) {
                BitSet liveIns = callSites.get(callSiteLocation);
                for (int liveVar = liveIns.nextSetBit(0); liveVar >= 0; liveVar = liveIns.nextSetBit(liveVar + 1)) {
                    int slot = colors[liveVar];
                    states[slot] = liveVar;
                }
                for (int slot = 0; slot < states.length; ++slot) {
                    if (states[slot] >= 0 && !liveIns.get(states[slot])) {
                        states[slot] = -1;
                    }
                }

                updatesByCallSite.put(callSiteLocation, compareStates(previousStates, states, autoSpilled));
                previousStates = states;
                states = states.clone();
            }

            for (int succ : domGraph.outgoingEdges(step.node)) {
                Step next = new Step(succ);
                System.arraycopy(states, 0, next.slotStates, 0, usedColors);
                stack[head++] = next;
            }
        }

        return slotsToUpdate;
    }

    private List<Instruction> sortInstructions(Collection<Instruction> instructions, BasicBlock block) {
        ObjectIntMap<Instruction> indexes = new ObjectIntHashMap<>();
        int index = 0;
        for (Instruction instruction : block) {
            indexes.put(instruction, index++);
        }
        List<Instruction> sortedInstructions = new ArrayList<>(instructions);
        sortedInstructions.sort(Comparator.comparing(insn -> indexes.getOrDefault(insn, -1)));
        return sortedInstructions;
    }

    private static int[] compareStates(int[] oldStates, int[] newStates, boolean[] autoSpilled) {
        int[] comparison = new int[oldStates.length];
        Arrays.fill(comparison, -2);

        for (int i = 0; i < oldStates.length; ++i) {
            if (oldStates[i] != newStates[i]) {
                comparison[i] = newStates[i];
            }
        }

        for (int i = 0; i < newStates.length; ++i) {
            if (newStates[i] >= 0 && autoSpilled[newStates[i]]) {
                comparison[i] = -2;
            }
        }

        return comparison;
    }

    private void putLiveInGCRoots(Program program, List<Map<Instruction, int[]>> updateInformation) {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            Map<Instruction, int[]> updatesByIndex = updateInformation.get(i);
            Instruction[] callSiteLocations = updatesByIndex.keySet().toArray(new Instruction[0]);
            ObjectIntMap<Instruction> instructionIndexes = getInstructionIndexes(block);
            Arrays.sort(callSiteLocations, Comparator.comparing(instructionIndexes::get));
            for (Instruction callSiteLocation : updatesByIndex.keySet()) {
                int[] updates = updatesByIndex.get(callSiteLocation);
                storeLiveIns(block, callSiteLocation, updates);
            }
        }
    }

    private ObjectIntMap<Instruction> getInstructionIndexes(BasicBlock block) {
        ObjectIntMap<Instruction> indexes = new ObjectIntHashMap<>();
        for (Instruction instruction : block) {
            indexes.put(instruction, indexes.size());
        }
        return indexes;
    }

    private void storeLiveIns(BasicBlock block, Instruction callInstruction, int[] updates) {
        Program program = block.getProgram();
        List<Instruction> instructionsToAdd = new ArrayList<>();

        for (int slot = 0; slot < updates.length; ++slot) {
            int var = updates[slot];
            if (var == -2) {
                continue;
            }

            Variable slotVar = program.createVariable();
            IntegerConstantInstruction slotConstant = new IntegerConstantInstruction();
            slotConstant.setReceiver(slotVar);
            slotConstant.setConstant(slot);
            slotConstant.setLocation(callInstruction.getLocation());
            instructionsToAdd.add(slotConstant);

            InvokeInstruction registerInvocation = new InvokeInstruction();
            registerInvocation.setLocation(callInstruction.getLocation());
            registerInvocation.setType(InvocationType.SPECIAL);
            registerInvocation.getArguments().add(slotVar);
            if (var >= 0) {
                registerInvocation.setMethod(new MethodReference(ShadowStack.class, "registerGCRoot", int.class,
                        Object.class, void.class));
                registerInvocation.getArguments().add(program.variableAt(var));
            } else {
                registerInvocation.setMethod(new MethodReference(ShadowStack.class, "removeGCRoot", int.class,
                        void.class));
            }
            instructionsToAdd.add(registerInvocation);
        }

        callInstruction.insertPreviousAll(instructionsToAdd);
    }

    private boolean isReference(TypeInferer typeInferer, int var) {
        VariableType liveType = typeInferer.typeOf(var);
        switch (liveType) {
            case BYTE_ARRAY:
            case CHAR_ARRAY:
            case SHORT_ARRAY:
            case INT_ARRAY:
            case FLOAT_ARRAY:
            case LONG_ARRAY:
            case DOUBLE_ARRAY:
            case OBJECT_ARRAY:
            case OBJECT:
                return true;
            default:
                return false;
        }
    }
}
