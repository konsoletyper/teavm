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
package org.teavm.optimization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.teavm.common.*;
import org.teavm.model.*;
import org.teavm.model.instructions.*;
import org.teavm.model.util.*;

/**
 *
 * @author Alexey Andreev
 */
public class LoopInvariantMotion implements MethodOptimization {
    private int[] preheaders;
    private Instruction[] constantInstructions;
    private LoopGraph graph;
    private DominatorTree dom;
    private Program program;

    @Override
    public void optimize(MethodReader method, Program program) {
        this.program = program;
        graph = new LoopGraph(ProgramUtils.buildControlFlowGraph(program));
        dom = GraphUtils.buildDominatorTree(graph);
        Graph domGraph = GraphUtils.buildDominatorGraph(dom, graph.size());
        preheaders = new int[graph.size()];
        Arrays.fill(preheaders, -1);
        IntegerStack stack = new IntegerStack(graph.size());
        int[] defLocation = new int[program.variableCount()];
        Arrays.fill(defLocation, -1);
        constantInstructions = new Instruction[program.variableCount()];
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
        InstructionAnalyzer analyzer = new InstructionAnalyzer();
        CopyConstantVisitor constantCopier = new CopyConstantVisitor();
        while (!stack.isEmpty()) {
            int v = stack.pop();
            BasicBlock block = program.basicBlockAt(v);
            insnLoop: for (int i = 0; i < block.getInstructions().size(); ++i) {
                Instruction insn = block.getInstructions().get(i);
                insn.acceptVisitor(defExtractor);
                Variable[] defs = defExtractor.getDefinedVariables();
                for (Variable def : defs) {
                    defLocation[def.getIndex()] = v;
                }
                analyzer.canMove = false;
                analyzer.constant = false;
                insn.acceptVisitor(analyzer);
                if (analyzer.constant) {
                    constantInstructions[defs[0].getIndex()] = insn;
                }
                if (!analyzer.canMove) {
                    continue;
                }
                Loop defLoop = graph.loopAt(v);
                if (defLoop == null) {
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
                block.getInstructions().set(i, new EmptyInstruction());
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
                    insn.acceptVisitor(new VariableMapperImpl(variableMap));
                }
                newInstructions.add(insn);
                preheaderInstructions.addAll(preheaderInstructions.size() - 1, newInstructions);
                defLocation[defs[0].getIndex()] = commonUseLoop != null ? commonUseLoop.getHead() : 0;
            }
            for (int succ : domGraph.outgoingEdges(v)) {
                stack.push(succ);
            }
        }
    }

    private int getPreheader(int header) {
        int preheader = preheaders[header];
        if (preheader < 0) {
            int[] entries = getLoopEntries(header);
            if (entries.length == 1) {
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
        final BasicBlock preheader = program.createBasicBlock();
        JumpInstruction escapeInsn = new JumpInstruction();
        final BasicBlock header = program.basicBlockAt(headerIndex);
        escapeInsn.setTarget(header);
        preheader.getInstructions().add(escapeInsn);
        for (int i = 0; i < header.getPhis().size(); ++i) {
            Phi phi = header.getPhis().get(i);
            Phi preheaderPhi = null;
            for (int j = 0; j < phi.getIncomings().size(); ++j) {
                Incoming incoming = phi.getIncomings().get(j);
                if (!dom.dominates(headerIndex, incoming.getSource().getIndex())) {
                    phi.getIncomings().remove(j--);
                    if (preheaderPhi == null) {
                        preheaderPhi = new Phi();
                        preheaderPhi.setReceiver(program.createVariable(null));
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
                pred.getLastInstruction().acceptVisitor(new BasicBlockMapper() {
                    @Override protected BasicBlock map(BasicBlock block) {
                        if (block == header) {
                            block = preheader;
                        }
                        return block;
                    }
                });
            }
        }
        return preheader.getIndex();
    }

    private static class VariableMapperImpl extends InstructionVariableMapper {
        private Variable[] map;

        public VariableMapperImpl(Variable[] map) {
            this.map = map;
        }

        @Override
        protected Variable map(Variable var) {
            return map[var.getIndex()];
        }
    }

    private static class InstructionAnalyzer implements InstructionVisitor {
        public boolean canMove;
        public boolean constant;

        @Override
        public void visit(EmptyInstruction insn) {
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            constant = true;
        }

        @Override
        public void visit(NullConstantInstruction insn) {
            constant = true;
        }

        @Override
        public void visit(IntegerConstantInstruction insn) {
            constant = true;
        }

        @Override
        public void visit(LongConstantInstruction insn) {
            constant = true;
        }

        @Override
        public void visit(FloatConstantInstruction insn) {
            constant = true;
        }

        @Override
        public void visit(DoubleConstantInstruction insn) {
            constant = true;
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            constant = true;
        }

        @Override
        public void visit(BinaryInstruction insn) {
            canMove = true;
        }

        @Override
        public void visit(NegateInstruction insn) {
            canMove = true;
        }

        @Override
        public void visit(AssignInstruction insn) {
            canMove = true;
        }

        @Override
        public void visit(CastInstruction insn) {
            canMove = true;
        }

        @Override
        public void visit(CastNumberInstruction insn) {
            canMove = true;
        }

        @Override
        public void visit(CastIntegerInstruction insn) {
            canMove = true;
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
            // TODO: Sometimes we can cast NPE when array is null and its length is read only in certain cases
            //canMove = true;
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
            // TODO: Sometimes we can cast NPE when array is null and is is unwrapped only in certain cases
            //canMove = true;
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
        public void visit(IsInstanceInstruction insn) {
            canMove = true;
        }

        @Override
        public void visit(InitClassInstruction insn) {
        }

        @Override
        public void visit(NullCheckInstruction insn) {
            canMove = true;
        }
    }

    private class CopyConstantVisitor implements InstructionVisitor {
        Instruction copy;
        Variable var;

        @Override
        public void visit(EmptyInstruction insn) {
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            var = program.createVariable(insn.getReceiver().getDebugName());
            ClassConstantInstruction copy = new ClassConstantInstruction();
            copy.setConstant(insn.getConstant());
            copy.setReceiver(var);
            this.copy = copy;
        }

        @Override
        public void visit(NullConstantInstruction insn) {
            var = program.createVariable(insn.getReceiver().getDebugName());
            NullConstantInstruction copy = new NullConstantInstruction();
            copy.setReceiver(var);
            this.copy = copy;
        }

        @Override
        public void visit(IntegerConstantInstruction insn) {
            var = program.createVariable(insn.getReceiver().getDebugName());
            IntegerConstantInstruction copy = new IntegerConstantInstruction();
            copy.setConstant(insn.getConstant());
            copy.setReceiver(var);
            this.copy = copy;
        }

        @Override
        public void visit(LongConstantInstruction insn) {
            var = program.createVariable(insn.getReceiver().getDebugName());
            LongConstantInstruction copy = new LongConstantInstruction();
            copy.setConstant(insn.getConstant());
            copy.setReceiver(var);
            this.copy = copy;
        }

        @Override
        public void visit(FloatConstantInstruction insn) {
            var = program.createVariable(insn.getReceiver().getDebugName());
            FloatConstantInstruction copy = new FloatConstantInstruction();
            copy.setConstant(insn.getConstant());
            copy.setReceiver(var);
            this.copy = copy;
        }

        @Override
        public void visit(DoubleConstantInstruction insn) {
            var = program.createVariable(insn.getReceiver().getDebugName());
            DoubleConstantInstruction copy = new DoubleConstantInstruction();
            copy.setConstant(insn.getConstant());
            copy.setReceiver(var);
            this.copy = copy;
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            var = program.createVariable(insn.getReceiver().getDebugName());
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
        public void visit(IsInstanceInstruction insn) {
        }

        @Override
        public void visit(InitClassInstruction insn) {
        }

        @Override
        public void visit(NullCheckInstruction insn) {
        }
    }
}
