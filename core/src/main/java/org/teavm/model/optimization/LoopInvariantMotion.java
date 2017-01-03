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
package org.teavm.model.optimization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.common.IntegerStack;
import org.teavm.common.Loop;
import org.teavm.common.LoopGraph;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.Variable;
import org.teavm.model.analysis.NullnessInformation;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.util.BasicBlockMapper;
import org.teavm.model.util.ControlFlowUtils;
import org.teavm.model.util.DefinitionExtractor;
import org.teavm.model.util.InstructionVariableMapper;
import org.teavm.model.util.ProgramUtils;
import org.teavm.model.util.UsageExtractor;

public class LoopInvariantMotion implements MethodOptimization {
    private int[] preheaders;
    private LoopGraph graph;
    private DominatorTree dom;
    private Program program;

    @Override
    public boolean optimize(MethodOptimizationContext context, Program program) {
        NullnessInformation nullness = NullnessInformation.build(program, context.getMethod().getDescriptor());

        boolean affected = false;
        this.program = program;
        graph = new LoopGraph(ProgramUtils.buildControlFlowGraph(program));
        dom = GraphUtils.buildDominatorTree(graph);
        Graph domGraph = GraphUtils.buildDominatorGraph(dom, graph.size());
        preheaders = new int[graph.size()];
        Arrays.fill(preheaders, -1);
        IntegerStack stack = new IntegerStack(graph.size());
        int[] defLocation = new int[program.variableCount()];
        Arrays.fill(defLocation, -1);
        Instruction[] constantInstructions = new Instruction[program.variableCount()];
        for (int i = 0; i <= context.getMethod().parameterCount(); ++i) {
            defLocation[i] = 0;
        }
        for (int i = 0; i < domGraph.size(); ++i) {
            if (dom.immediateDominatorOf(i) < 0) {
                stack.push(i);
            }
        }

        DefinitionExtractor defExtractor = new DefinitionExtractor();
        UsageExtractor useExtractor = new UsageExtractor();
        LoopInvariantAnalyzer analyzer = new LoopInvariantAnalyzer(nullness);
        CopyConstantVisitor constantCopier = new CopyConstantVisitor();
        int[][] loopExits = ControlFlowUtils.findLoopExits(graph);

        while (!stack.isEmpty()) {
            int v = stack.pop();
            Loop defLoop = graph.loopAt(v);
            int[] exits = loopExits[v];
            boolean dominatesExits = exits != null && Arrays.stream(exits)
                    .allMatch(exit -> dom.dominates(v, exit));
            BasicBlock block = program.basicBlockAt(v);
            Instruction nextInsn;
            insnLoop: for (Instruction insn = block.getFirstInstruction(); insn != null; insn = nextInsn) {
                nextInsn = insn.getNext();
                insn.acceptVisitor(defExtractor);
                Variable[] defs = defExtractor.getDefinedVariables();
                for (Variable def : defs) {
                    defLocation[def.getIndex()] = v;
                }
                analyzer.reset();
                insn.acceptVisitor(analyzer);
                if (analyzer.constant) {
                    constantInstructions[defs[0].getIndex()] = insn;
                }
                if (!analyzer.canMove) {
                    continue;
                }
                if (defLoop == null) {
                    continue;
                }
                if (analyzer.sideEffect && !dominatesExits) {
                    continue;
                }
                insn.acceptVisitor(useExtractor);
                Loop commonUseLoop = null;
                for (Variable use : useExtractor.getUsedVariables()) {
                    if (constantInstructions[use.getIndex()] != null) {
                        continue;
                    }
                    int useLoc = defLocation[use.getIndex()];
                    if (useLoc == -1) {
                        continue insnLoop;
                    }
                    Loop useLoop = graph.loopAt(useLoc);
                    if (useLoop == defLoop) {
                        continue insnLoop;
                    }
                    if (useLoop != null && useLoop.isChildOf(commonUseLoop)) {
                        commonUseLoop = useLoop;
                    }
                }
                while (defLoop.getParent() != commonUseLoop) {
                    defLoop = defLoop.getParent();
                    if (defLoop == null) {
                        continue insnLoop;
                    }
                }

                insn.delete();
                BasicBlock preheader = program.basicBlockAt(getPreheader(defLoop.getHead()));
                List<Instruction> newInstructions = new ArrayList<>();
                Variable[] variableMap = null;
                for (Variable use : useExtractor.getUsedVariables()) {
                    Instruction constInsn = constantInstructions[use.getIndex()];
                    if (constInsn != null) {
                        constInsn.acceptVisitor(constantCopier);
                        newInstructions.add(constantCopier.copy);
                        if (variableMap == null) {
                            variableMap = new Variable[program.variableCount()];
                            for (int j = 0; j < variableMap.length; ++j) {
                                variableMap[j] = program.variableAt(j);
                            }
                        }
                        variableMap[use.getIndex()] = constantCopier.var;
                    }
                }
                if (variableMap != null) {
                    Variable[] currentVariableMap = variableMap;
                    insn.acceptVisitor(new InstructionVariableMapper(var -> currentVariableMap[var.getIndex()]));
                }
                newInstructions.add(insn);
                preheader.getLastInstruction().insertPreviousAll(newInstructions);
                defLocation[defs[0].getIndex()] = commonUseLoop != null ? commonUseLoop.getHead() : 0;
                affected = true;
            }
            for (int succ : domGraph.outgoingEdges(v)) {
                stack.push(succ);
            }
        }

        nullness.dispose();
        return affected;
    }

    private int getPreheader(int header) {
        int preheader = preheaders[header];
        if (preheader < 0) {
            int[] entries = getLoopEntries(header);
            if (entries.length == 1 && graph.outgoingEdgesCount(entries[0]) == 1) {
                preheader = entries[0];
            } else {
                preheader = insertPreheader(header);
            }
            preheaders[header] = preheader;
        }
        return preheader;
    }

    private int[] getLoopEntries(int header) {
        int[] predecessors = graph.incomingEdges(header);
        int j = 0;
        for (int i = 0; i < predecessors.length; ++i) {
            int pred = predecessors[i];
            if (!dom.dominates(header, pred)) {
                predecessors[j++] = pred;
            }
        }
        return Arrays.copyOf(predecessors, j);
    }

    private int insertPreheader(int headerIndex) {
        BasicBlock preheader = program.createBasicBlock();
        JumpInstruction escapeInsn = new JumpInstruction();
        BasicBlock header = program.basicBlockAt(headerIndex);
        escapeInsn.setTarget(header);
        preheader.add(escapeInsn);

        for (Phi phi : header.getPhis()) {
            Phi preheaderPhi = null;
            for (int i = 0; i < phi.getIncomings().size(); ++i) {
                Incoming incoming = phi.getIncomings().get(i);
                if (!dom.dominates(headerIndex, incoming.getSource().getIndex())) {
                    phi.getIncomings().remove(i--);
                    if (preheaderPhi == null) {
                        preheaderPhi = new Phi();
                        preheaderPhi.setReceiver(program.createVariable());
                        preheaderPhi.getReceiver().setLabel(phi.getReceiver().getLabel());
                        preheaderPhi.getReceiver().setDebugName(phi.getReceiver().getDebugName());
                        preheader.getPhis().add(preheaderPhi);
                    }
                    preheaderPhi.getIncomings().add(incoming);
                }
            }
            if (preheaderPhi != null) {
                Incoming incoming = new Incoming();
                incoming.setSource(preheader);
                incoming.setValue(preheaderPhi.getReceiver());
                phi.getIncomings().add(incoming);
            }
        }

        for (int predIndex : graph.incomingEdges(headerIndex)) {
            if (!dom.dominates(headerIndex, predIndex)) {
                BasicBlock pred = program.basicBlockAt(predIndex);
                pred.getLastInstruction().acceptVisitor(new BasicBlockMapper(
                        (int block) -> block == header.getIndex() ? preheader.getIndex() : block));
            }
        }

        return preheader.getIndex();
    }

    private class CopyConstantVisitor extends AbstractInstructionVisitor {
        Instruction copy;
        Variable var;

        @Override
        public void visit(ClassConstantInstruction insn) {
            var = program.createVariable();
            var.setDebugName(insn.getReceiver().getDebugName());
            var.setLabel(insn.getReceiver().getLabel());
            ClassConstantInstruction copy = new ClassConstantInstruction();
            copy.setConstant(insn.getConstant());
            copy.setReceiver(var);
            this.copy = copy;
        }

        @Override
        public void visit(NullConstantInstruction insn) {
            var = program.createVariable();
            var.setDebugName(insn.getReceiver().getDebugName());
            var.setLabel(insn.getReceiver().getLabel());
            NullConstantInstruction copy = new NullConstantInstruction();
            copy.setReceiver(var);
            this.copy = copy;
        }

        @Override
        public void visit(IntegerConstantInstruction insn) {
            var = program.createVariable();
            var.setDebugName(insn.getReceiver().getDebugName());
            var.setLabel(insn.getReceiver().getLabel());
            IntegerConstantInstruction copy = new IntegerConstantInstruction();
            copy.setConstant(insn.getConstant());
            copy.setReceiver(var);
            this.copy = copy;
        }

        @Override
        public void visit(LongConstantInstruction insn) {
            var = program.createVariable();
            var.setDebugName(insn.getReceiver().getDebugName());
            var.setLabel(insn.getReceiver().getLabel());
            LongConstantInstruction copy = new LongConstantInstruction();
            copy.setConstant(insn.getConstant());
            copy.setReceiver(var);
            this.copy = copy;
        }

        @Override
        public void visit(FloatConstantInstruction insn) {
            var = program.createVariable();
            var.setDebugName(insn.getReceiver().getDebugName());
            var.setLabel(insn.getReceiver().getLabel());
            FloatConstantInstruction copy = new FloatConstantInstruction();
            copy.setConstant(insn.getConstant());
            copy.setReceiver(var);
            this.copy = copy;
        }

        @Override
        public void visit(DoubleConstantInstruction insn) {
            var = program.createVariable();
            var.setDebugName(insn.getReceiver().getDebugName());
            var.setLabel(insn.getReceiver().getLabel());
            DoubleConstantInstruction copy = new DoubleConstantInstruction();
            copy.setConstant(insn.getConstant());
            copy.setReceiver(var);
            this.copy = copy;
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            var = program.createVariable();
            var.setDebugName(insn.getReceiver().getDebugName());
            var.setLabel(insn.getReceiver().getLabel());
            StringConstantInstruction copy = new StringConstantInstruction();
            copy.setConstant(insn.getConstant());
            copy.setReceiver(var);
            this.copy = copy;
        }
    }
}
