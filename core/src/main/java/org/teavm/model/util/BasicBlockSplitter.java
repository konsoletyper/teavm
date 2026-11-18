/*
 *  Copyright 2018 Alexey Andreev.
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

import com.carrotsearch.hppc.ByteArrayList;
import com.carrotsearch.hppc.ByteIndexedContainer;
import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntIndexedContainer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.optimization.RedundantJumpElimination;

public class BasicBlockSplitter {
    private Program program;
    private int[] mappings;
    private IntIndexedContainer previousPtr;
    private IntIndexedContainer firstPtr;
    private ByteIndexedContainer isLastInSequence;

    public BasicBlockSplitter(Program program) {
        this.program = program;
    }

    private void initIfNecessary() {
        if (mappings != null) {
            return;
        }

        mappings = new int[program.basicBlockCount()];
        previousPtr = new IntArrayList(program.basicBlockCount() * 2);
        firstPtr = new IntArrayList(program.basicBlockCount() * 2);
        isLastInSequence = new ByteArrayList(program.basicBlockCount() * 2);
        for (int i = 0; i < mappings.length; ++i) {
            mappings[i] = i;
            previousPtr.add(i);
            firstPtr.add(i);
            isLastInSequence.add((byte) 1);
        }
    }

    public BasicBlock split(BasicBlock block, Instruction afterInstruction) {
        initIfNecessary();

        if (afterInstruction != null && afterInstruction.getBasicBlock() != block) {
            throw new IllegalArgumentException();
        }

        if (isLastInSequence.get(block.getIndex()) == 0) {
            throw new IllegalArgumentException();
        }

        BasicBlock splitBlock = program.createBasicBlock();
        while (previousPtr.size() < splitBlock.getIndex()) {
            previousPtr.add(previousPtr.size());
            firstPtr.add(firstPtr.size());
            isLastInSequence.add((byte) 1);
        }
        isLastInSequence.set(block.getIndex(), (byte) 0);
        previousPtr.add(block.getIndex());
        firstPtr.add(firstPtr.get(block.getIndex()));
        mappings[firstPtr.get(block.getIndex())] = splitBlock.getIndex();
        isLastInSequence.add((byte) 1);

        splitBlock.getTryCatchBlocks().addAll(ProgramUtils.copyTryCatches(block, program));
        if (afterInstruction != null) {
            while (afterInstruction.getNext() != null) {
                Instruction nextInstruction = afterInstruction.getNext();
                nextInstruction.delete();
                splitBlock.add(nextInstruction);
            }
        } else {
            while (block.getFirstInstruction() != null) {
                Instruction instruction = block.getFirstInstruction();
                instruction.delete();
                splitBlock.add(instruction);
            }
        }

        return splitBlock;
    }

    public void fixProgram() {
        if (mappings == null) {
            return;
        }

        for (BasicBlock block : program.getBasicBlocks()) {
            Map<BasicBlock, List<Incoming>> incomingsBySource = new LinkedHashMap<>();
            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    if (incoming.getSource().getIndex() >= mappings.length
                            || mappings[incoming.getSource().getIndex()] == incoming.getSource().getIndex()) {
                        continue;
                    }
                    incomingsBySource.computeIfAbsent(incoming.getSource(), b -> new ArrayList<>()).add(incoming);
                }
            }

            for (BasicBlock source : incomingsBySource.keySet()) {
                boolean isExceptionHandler = source.getTryCatchBlocks().stream()
                        .anyMatch(tryCatch -> tryCatch.getHandler() == block);
                if (isExceptionHandler) {
                    fixIncomingsInExceptionHandler(source, block);
                } else {
                    BasicBlock newSource = program.basicBlockAt(mappings[source.getIndex()]);
                    for (Incoming incoming : incomingsBySource.get(source)) {
                        incoming.setSource(newSource);
                    }
                }
            }
        }

        RedundantJumpElimination.optimize(program);
    }

    /*
     * Incomings of a handler phi that come from one protected block are ordered: first goes the value
     * that is visible at the beginning of the block, then go values defined in the block, in order
     * of their definitions. After split, each part of the block gets the same structure: the value visible
     * at the beginning of the part (i.e. the last value of the previous part), followed by values
     * defined in the part.
     */
    private void fixIncomingsInExceptionHandler(BasicBlock source, BasicBlock handler) {
        List<BasicBlock> sourceParts = buildBasicBlocksSequence(source);
        assert sourceParts.get(0) == source;

        DefinitionExtractor defExtractor = new DefinitionExtractor();
        List<List<Variable>> definedVarsByPart = new ArrayList<>(sourceParts.size());
        Set<Variable> definedVars = new HashSet<>();
        for (BasicBlock part : sourceParts) {
            List<Variable> definedVarsInPart = ProgramUtils.getVariablesDefinedInBlock(part, defExtractor);
            definedVarsByPart.add(definedVarsInPart);
            definedVars.addAll(definedVarsInPart);
        }

        for (Phi phi : handler.getPhis()) {
            int insertionIndex = -1;
            List<Incoming> group = new ArrayList<>();
            for (int i = 0; i < phi.getIncomings().size(); ++i) {
                Incoming incoming = phi.getIncomings().get(i);
                if (incoming.getSource() == source) {
                    if (insertionIndex < 0) {
                        insertionIndex = i;
                    }
                    group.add(phi.getIncomings().remove(i--));
                }
            }
            if (insertionIndex < 0) {
                continue;
            }

            List<Incoming> newGroup = new ArrayList<>();
            Map<Variable, List<Incoming>> incomingsByValue = new HashMap<>();
            Variable lastValue = null;
            for (Incoming incoming : group) {
                if (definedVars.contains(incoming.getValue())) {
                    incomingsByValue.computeIfAbsent(incoming.getValue(), v -> new ArrayList<>()).add(incoming);
                } else {
                    newGroup.add(incoming);
                    lastValue = incoming.getValue();
                }
            }

            for (int i = 0; i < sourceParts.size(); ++i) {
                BasicBlock part = sourceParts.get(i);
                if (i > 0 && lastValue != null) {
                    Incoming entryIncoming = new Incoming();
                    entryIncoming.setSource(part);
                    entryIncoming.setValue(lastValue);
                    newGroup.add(entryIncoming);
                }
                for (Variable definedVar : definedVarsByPart.get(i)) {
                    List<Incoming> incomingsOfDefinedVar = incomingsByValue.get(definedVar);
                    if (incomingsOfDefinedVar != null) {
                        for (Incoming incoming : incomingsOfDefinedVar) {
                            incoming.setSource(part);
                            newGroup.add(incoming);
                        }
                        lastValue = definedVar;
                    }
                }
            }

            phi.getIncomings().addAll(insertionIndex, newGroup);
        }
    }

    private List<BasicBlock> buildBasicBlocksSequence(BasicBlock first) {
        List<BasicBlock> result = new ArrayList<>(2);
        BasicBlock block = program.basicBlockAt(mappings[first.getIndex()]);
        while (previousPtr.get(block.getIndex()) != block.getIndex()) {
            result.add(block);
            block = program.basicBlockAt(previousPtr.get(block.getIndex()));
        }
        result.add(block);
        Collections.reverse(result);
        return result;
    }
}
