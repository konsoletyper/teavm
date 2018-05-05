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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private BasicBlock[] variableDefinedAt;

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

        variableDefinedAt = ProgramUtils.getVariableDefinitionPlaces(program);
    }

    public BasicBlock split(BasicBlock block, Instruction afterInstruction) {
        initIfNecessary();

        if (afterInstruction.getBasicBlock() != block) {
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
        while (afterInstruction.getNext() != null) {
            Instruction nextInstruction = afterInstruction.getNext();
            nextInstruction.delete();
            splitBlock.add(nextInstruction);
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
                    if (mappings[incoming.getSource().getIndex()] == incoming.getSource().getIndex()) {
                        continue;
                    }
                    incomingsBySource.computeIfAbsent(incoming.getSource(), b -> new ArrayList<>()).add(incoming);
                }
            }

            for (BasicBlock source : incomingsBySource.keySet()) {
                boolean isExceptionHandler = source.getTryCatchBlocks().stream()
                        .anyMatch(tryCatch -> tryCatch.getHandler() == block);
                if (isExceptionHandler) {
                    fixIncomingsInExceptionHandler(source, incomingsBySource.get(source));
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

    private void fixIncomingsInExceptionHandler(BasicBlock source, List<Incoming> incomings) {
        List<BasicBlock> sourceParts = buildBasicBlocksSequence(source);
        assert sourceParts.get(0) == source;
        Map<Variable, List<Incoming>> incomingsByValue = groupIncomingsByValue(incomings);
        Map<Phi, Variable> lastDefinedValues = new HashMap<>();

        for (Incoming incoming : incomings) {
            if (variableDefinedAt[incoming.getValue().getIndex()] != source) {
                lastDefinedValues.put(incoming.getPhi(), incoming.getValue());
            }
        }

        DefinitionExtractor defExtractor = new DefinitionExtractor();
        for (BasicBlock block : sourceParts) {
            if (block != source) {
                for (Map.Entry<Phi, Variable> lastDefinedEntry : lastDefinedValues.entrySet()) {
                    Incoming incomingCopy = new Incoming();
                    incomingCopy.setSource(block);
                    incomingCopy.setValue(lastDefinedEntry.getValue());
                    lastDefinedEntry.getKey().getIncomings().add(incomingCopy);
                }
            }

            List<Variable> definedVars = ProgramUtils.getVariablesDefinedInBlock(block, defExtractor);
            for (Variable definedVar : definedVars) {
                List<Incoming> incomingsOfDefinedVar = incomingsByValue.get(definedVar);
                if (incomingsOfDefinedVar != null) {
                    for (Incoming incoming : incomingsOfDefinedVar) {
                        incoming.setSource(block);
                        lastDefinedValues.put(incoming.getPhi(), definedVar);
                    }
                }
            }
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

    private Map<Variable, List<Incoming>> groupIncomingsByValue(List<Incoming> incomings) {
        Map<Variable, List<Incoming>> incoingsByValue = new HashMap<>();
        for (Incoming incoming : incomings) {
            incoingsByValue.computeIfAbsent(incoming.getValue(), i -> new ArrayList<>()).add(incoming);
        }
        return incoingsByValue;
    }
}
