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
import org.teavm.common.*;
import org.teavm.model.*;
import org.teavm.model.instructions.*;
import org.teavm.model.util.*;

public class LoopInvariantMotion implements MethodOptimization {
    private int[] preheaders;
    private LoopGraph graph;
    private DominatorTree dom;
    private Program program;

    @Override
    public boolean optimize(MethodReader method, Program program) {
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
        for (int i = 0; i <= method.parameterCount(); ++i) {
            defLocation[i] = 0;
        }
        for (int i = 0; i < domGraph.size(); ++i) {
            if (dom.immediateDominatorOf(i) < 0) {
                stack.push(i);
            }
        }

        DefinitionExtractor defExtractor = new DefinitionExtractor();
        UsageExtractor useExtractor = new UsageExtractor();
        LoopInvariantAnalyzer analyzer = new LoopInvariantAnalyzer();
        CopyConstantVisitor constantCopier = new CopyConstantVisitor();
        int[][] loopExits = ControlFlowUtils.findLoopExits(graph);

        while (!stack.isEmpty()) {
            int v = stack.pop();
            Loop defLoop = graph.loopAt(v);
            int[] exits = loopExits[v];
            boolean dominatesExits = exits != null && Arrays.stream(exits)
                    .allMatch(exit -> dom.dominates(v, exit));
            BasicBlock block = program.basicBlockAt(v);
            insnLoop: for (int i = 0; i < block.getInstructions().size(); ++i) {
                Instruction insn = block.getInstructions().get(i);
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

                EmptyInstruction empty = new EmptyInstruction();
                empty.setLocation(insn.getLocation());
                block.getInstructions().set(i, empty);
                int preheader = getPreheader(defLoop.getHead());
                List<Instruction> preheaderInstructions = program.basicBlockAt(preheader).getInstructions();
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
                preheaderInstructions.addAll(preheaderInstructions.size() - 1, newInstructions);
                defLocation[defs[0].getIndex()] = commonUseLoop != null ? commonUseLoop.getHead() : 0;
                affected = true;
            }
            for (int succ : domGraph.outgoingEdges(v)) {
                stack.push(succ);
            }
        }

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
        preheader.getInstructions().add(escapeInsn);

        for (Phi phi : header.getPhis()) {
            Phi preheaderPhi = null;
            for (int i = 0; i < phi.getIncomings().size(); ++i) {
                Incoming incoming = phi.getIncomings().get(i);
                if (!dom.dominates(headerIndex, incoming.getSource().getIndex())) {
                    phi.getIncomings().remove(i--);
                    if (preheaderPhi == null) {
                        preheaderPhi = new Phi();
                        preheaderPhi.setReceiver(program.createVariable());
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
                        block -> block == header.getIndex() ? preheader.getIndex() : block));
            }
        }

        return preheader.getIndex();
    }

    private class CopyConstantVisitor implements InstructionVisitor {
        Instruction copy;
        Variable var;

        @Override
        public void visit(EmptyInstruction insn) {
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            var = program.createVariable();
            var.setDebugName(insn.getReceiver().getDebugName());
            ClassConstantInstruction copy = new ClassConstantInstruction();
            copy.setConstant(insn.getConstant());
            copy.setReceiver(var);
            this.copy = copy;
        }

        @Override
        public void visit(NullConstantInstruction insn) {
            var = program.createVariable();
            var.setDebugName(insn.getReceiver().getDebugName());
            NullConstantInstruction copy = new NullConstantInstruction();
            copy.setReceiver(var);
            this.copy = copy;
        }

        @Override
        public void visit(IntegerConstantInstruction insn) {
            var = program.createVariable();
            var.setDebugName(insn.getReceiver().getDebugName());
            IntegerConstantInstruction copy = new IntegerConstantInstruction();
            copy.setConstant(insn.getConstant());
            copy.setReceiver(var);
            this.copy = copy;
        }

        @Override
        public void visit(LongConstantInstruction insn) {
            var = program.createVariable();
            var.setDebugName(insn.getReceiver().getDebugName());
            LongConstantInstruction copy = new LongConstantInstruction();
            copy.setConstant(insn.getConstant());
            copy.setReceiver(var);
            this.copy = copy;
        }

        @Override
        public void visit(FloatConstantInstruction insn) {
            var = program.createVariable();
            var.setDebugName(insn.getReceiver().getDebugName());
            FloatConstantInstruction copy = new FloatConstantInstruction();
            copy.setConstant(insn.getConstant());
            copy.setReceiver(var);
            this.copy = copy;
        }

        @Override
        public void visit(DoubleConstantInstruction insn) {
            var = program.createVariable();
            var.setDebugName(insn.getReceiver().getDebugName());
            DoubleConstantInstruction copy = new DoubleConstantInstruction();
            copy.setConstant(insn.getConstant());
            copy.setReceiver(var);
            this.copy = copy;
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            var = program.createVariable();
            var.setDebugName(insn.getReceiver().getDebugName());
            StringConstantInstruction copy = new StringConstantInstruction();
            copy.setConstant(insn.getConstant());
            copy.setReceiver(var);
            this.copy = copy;
        }

        @Override
        public void visit(BinaryInstruction insn) {
        }

        @Override
        public void visit(NegateInstruction insn) {
        }

        @Override
        public void visit(AssignInstruction insn) {
        }

        @Override
        public void visit(CastInstruction insn) {
        }

        @Override
        public void visit(CastNumberInstruction insn) {
        }

        @Override
        public void visit(CastIntegerInstruction insn) {
        }

        @Override
        public void visit(BranchingInstruction insn) {
        }

        @Override
        public void visit(BinaryBranchingInstruction insn) {
        }

        @Override
        public void visit(JumpInstruction insn) {
        }

        @Override
        public void visit(SwitchInstruction insn) {
        }

        @Override
        public void visit(ExitInstruction insn) {
        }

        @Override
        public void visit(RaiseInstruction insn) {
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
        }

        @Override
        public void visit(ConstructInstruction insn) {
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
        }

        @Override
        public void visit(GetFieldInstruction insn) {
        }

        @Override
        public void visit(PutFieldInstruction insn) {
        }

        @Override
        public void visit(ArrayLengthInstruction insn) {
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
        }

        @Override
        public void visit(GetElementInstruction insn) {
        }

        @Override
        public void visit(PutElementInstruction insn) {
        }

        @Override
        public void visit(InvokeInstruction insn) {
        }

        @Override
        public void visit(InvokeDynamicInstruction insn) {
        }

        @Override
        public void visit(IsInstanceInstruction insn) {
        }

        @Override
        public void visit(InitClassInstruction insn) {
        }

        @Override
        public void visit(NullCheckInstruction insn) {
        }

        @Override
        public void visit(MonitorEnterInstruction insn) {
        }

        @Override
        public void visit(MonitorExitInstruction insn) {
        }
    }
}
