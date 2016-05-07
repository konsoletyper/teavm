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
import java.util.List;
import java.util.Map;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.model.BasicBlock;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.Incoming;
import org.teavm.model.IncomingReader;
import org.teavm.model.Instruction;
import org.teavm.model.InstructionLocation;
import org.teavm.model.Phi;
import org.teavm.model.PhiReader;
import org.teavm.model.Program;
import org.teavm.model.ProgramReader;
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

    public static Graph buildControlFlowGraphWithTryCatch(Program program) {
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
                        for (TryCatchBlock succTryCatch : successor.getTryCatchBlocks()) {
                            graphBuilder.addEdge(i, succTryCatch.getHandler().getIndex());
                        }
                    }
                }
            }
            for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
                graphBuilder.addEdge(i, tryCatch.getHandler().getIndex());
            }
        }
        return graphBuilder.build();
    }

    public static Map<InstructionLocation, InstructionLocation[]> getLocationCFG(Program program) {
        return new LocationGraphBuilder().build(program);
    }

    public static Program copy(ProgramReader program) {
        Program copy = new Program();
        for (int i = 0; i < program.variableCount(); ++i) {
            Variable var = copy.createVariable();
            var.getDebugNames().addAll(program.variableAt(i).readDebugNames());
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            copy.createBasicBlock();
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlockReader block = program.basicBlockAt(i);
            BasicBlock blockCopy = copy.basicBlockAt(i);
            blockCopy.getInstructions().addAll(copyInstructions(block, 0, block.instructionCount(), copy));
            blockCopy.getPhis().addAll(copyPhis(block, copy));
            for (TryCatchBlockReader tryCatch : block.readTryCatchBlocks()) {
                TryCatchBlock tryCatchCopy = new TryCatchBlock();
                tryCatchCopy.setExceptionType(tryCatch.getExceptionType());
                tryCatchCopy.setExceptionVariable(copy.variableAt(tryCatch.getExceptionVariable().getIndex()));
                tryCatchCopy.setHandler(copy.basicBlockAt(tryCatch.getHandler().getIndex()));
                blockCopy.getTryCatchBlocks().add(tryCatchCopy);
            }
        }
        return copy;
    }

    public static List<Instruction> copyInstructions(BasicBlockReader block, int from, int to, Program target) {
        List<Instruction> result = new ArrayList<>();
        InstructionCopyReader copyReader = new InstructionCopyReader(target);
        for (int i = from; i < to; ++i) {
            block.readInstruction(i, copyReader);
            result.add(copyReader.getCopy());
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
            tryCatchCopy.setExceptionVariable(target.variableAt(tryCatch.getExceptionVariable().getIndex()));
            tryCatchCopy.setHandler(target.basicBlockAt(tryCatch.getHandler().getIndex()));
            result.add(tryCatchCopy);
        }
        return result;
    }
}
