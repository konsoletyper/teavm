/*
 *  Copyright 2013 Alexey Andreev.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.model.BasicBlock;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.Incoming;
import org.teavm.model.IncomingReader;
import org.teavm.model.Instruction;
import org.teavm.model.InstructionIterator;
import org.teavm.model.InstructionReadVisitor;
import org.teavm.model.Phi;
import org.teavm.model.PhiReader;
import org.teavm.model.Program;
import org.teavm.model.ProgramReader;
import org.teavm.model.TextLocation;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.TryCatchBlockReader;
import org.teavm.model.Variable;

public final class ProgramUtils {
    private ProgramUtils() {
    }

    public static Graph buildControlFlowGraph(Program program) {
        GraphBuilder graphBuilder = new GraphBuilder(program.basicBlockCount());
        InstructionTransitionExtractor transitionExtractor = new InstructionTransitionExtractor();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            Instruction insn = block.getLastInstruction();
            if (insn != null) {
                insn.acceptVisitor(transitionExtractor);
                if (transitionExtractor.getTargets() != null) {
                    for (BasicBlock successor : transitionExtractor.getTargets()) {
                        graphBuilder.addEdge(i, successor.getIndex());
                    }
                }
            }
            for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
                graphBuilder.addEdge(i, tryCatch.getHandler().getIndex());
            }
        }
        return graphBuilder.build();
    }

    public static Map<TextLocation, TextLocation[]> getLocationCFG(Program program) {
        return new LocationGraphBuilder().build(program);
    }

    public static Program copy(ProgramReader program) {
        Program copy = new Program();
        for (int i = 0; i < program.variableCount(); ++i) {
            Variable var = copy.createVariable();
            var.setDebugName(program.variableAt(i).getDebugName());
            var.setLabel(program.variableAt(i).getLabel());
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            copy.createBasicBlock();
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlockReader block = program.basicBlockAt(i);
            BasicBlock blockCopy = copy.basicBlockAt(i);
            copyBasicBlock(block, blockCopy);
        }
        return copy;
    }

    public static void copyBasicBlock(BasicBlockReader block, BasicBlock target) {
        Program targetProgram = target.getProgram();

        if (block.getExceptionVariable() != null) {
            target.setExceptionVariable(targetProgram.variableAt(block.getExceptionVariable().getIndex()));
        }
        InstructionCopyReader copyReader = new InstructionCopyReader(targetProgram);
        for (InstructionIterator iterator = block.iterateInstructions(); iterator.hasNext();) {
            iterator.next();
            iterator.read(copyReader);
            target.add(copyReader.getCopy());
        }
        target.getPhis().addAll(copyPhis(block, targetProgram));
        target.getTryCatchBlocks().addAll(copyTryCatches(block, targetProgram));
    }

    public static List<Instruction> copyInstructions(Instruction from, Instruction to, Program target) {
        List<Instruction> result = new ArrayList<>();
        InstructionCopyReader copyReader = new InstructionCopyReader(target);
        InstructionReadVisitor visitor = new InstructionReadVisitor(copyReader);
        while (from != to) {
            from.acceptVisitor(visitor);
            copyReader.getCopy().setLocation(from.getLocation());
            result.add(copyReader.getCopy());
            from = from.getNext();
        }
        return result;
    }

    public static List<Phi> copyPhis(BasicBlockReader block, Program target) {
        List<Phi> result = new ArrayList<>();
        for (PhiReader phi : block.readPhis()) {
            Phi phiCopy = new Phi();
            phiCopy.setReceiver(target.variableAt(phi.getReceiver().getIndex()));
            for (IncomingReader incoming : phi.readIncomings()) {
                Incoming incomingCopy = new Incoming();
                incomingCopy.setSource(target.basicBlockAt(incoming.getSource().getIndex()));
                incomingCopy.setValue(target.variableAt(incoming.getValue().getIndex()));
                phiCopy.getIncomings().add(incomingCopy);
            }
            result.add(phiCopy);
        }
        return result;
    }

    public static List<TryCatchBlock> copyTryCatches(BasicBlockReader block, Program target) {
        List<TryCatchBlock> result = new ArrayList<>();
        for (TryCatchBlockReader tryCatch : block.readTryCatchBlocks()) {
            TryCatchBlock tryCatchCopy = new TryCatchBlock();
            tryCatchCopy.setExceptionType(tryCatch.getExceptionType());
            tryCatchCopy.setHandler(target.basicBlockAt(tryCatch.getHandler().getIndex()));
            result.add(tryCatchCopy);
        }
        return result;
    }

    public static List<List<Incoming>> getPhiOutputs(Program program) {
        List<List<Incoming>> outputs = new ArrayList<>(program.basicBlockCount());
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            outputs.add(new ArrayList<>());
        }

        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
                    outputs.get(incoming.getSource().getIndex()).add(incoming);
                }
            }
        }

        return outputs;
    }

    public static BasicBlock[] getVariableDefinitionPlaces(Program program) {
        BasicBlock[] places = new BasicBlock[program.variableCount()];
        DefinitionExtractor defExtractor = new DefinitionExtractor();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);

            Variable exceptionVar = block.getExceptionVariable();
            if (exceptionVar != null) {
                places[exceptionVar.getIndex()] = block;
            }

            for (Phi phi : block.getPhis()) {
                places[phi.getReceiver().getIndex()] = block;
            }

            for (Instruction insn : block) {
                insn.acceptVisitor(defExtractor);
                for (Variable var : defExtractor.getDefinedVariables()) {
                    places[var.getIndex()] = block;
                }
            }
        }
        return places;
    }

    public static void makeUniqueLabels(Program program) {
        Set<String> occupiedLabels = new HashSet<>();

        for (int i = 0; i < program.variableCount(); ++i) {
            Variable var = program.variableAt(i);
            if (var.getLabel() == null) {
                continue;
            }
            String suggestedName = var.getLabel();
            if (!occupiedLabels.add(suggestedName)) {
                int suffix = 1;
                String base = suggestedName + "_";
                do {
                    suggestedName = base + suffix++;
                } while (!occupiedLabels.add(suggestedName));
            }
            var.setLabel(suggestedName);
        }
    }
}
