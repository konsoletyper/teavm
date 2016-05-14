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
package org.teavm.model.util;

import java.util.*;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

public class PhiUpdater {
    private Program program;
    private Graph cfg;
    private int[][] domFrontiers;
    private Variable[] variableMap;
    private BasicBlock currentBlock;
    private Phi[][] phiMap;
    private int[][] phiIndexMap;

    public void updatePhis(Program program, Variable[] arguments) {
        if (program.basicBlockCount() == 0) {
            return;
        }
        this.program = program;
        cfg = ProgramUtils.buildControlFlowGraphWithTryCatch(program);
        DominatorTree domTree = GraphUtils.buildDominatorTree(cfg);
        domFrontiers = new int[cfg.size()][];
        variableMap = new Variable[program.variableCount()];
        for (int i = 0; i < arguments.length; ++i) {
            variableMap[i] = arguments[i];
        }
        phiMap = new Phi[program.basicBlockCount()][];
        phiIndexMap = new int[program.basicBlockCount()][];
        for (int i = 0; i < phiMap.length; ++i) {
            phiMap[i] = new Phi[program.variableCount()];
            phiIndexMap[i] = new int[program.variableCount()];
        }
        domFrontiers = GraphUtils.findDominanceFrontiers(cfg, domTree);
        estimatePhis();
        renameVariables();
    }

    private void estimatePhis() {
        DefinitionExtractor definitionExtractor = new DefinitionExtractor();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            currentBlock = program.basicBlockAt(i);
            for (Instruction insn : currentBlock.getInstructions()) {
                insn.acceptVisitor(definitionExtractor);
                for (Variable var : definitionExtractor.getDefinedVariables()) {
                    markAssignment(var);
                }
            }
            for (Phi phi : currentBlock.getPhis()) {
                markAssignment(phi.getReceiver());
            }
        }
    }

    private static class Task {
        Variable[] variables;
        BasicBlock block;
    }

    private void renameVariables() {
        DominatorTree domTree = GraphUtils.buildDominatorTree(ProgramUtils.buildControlFlowGraph(program));
        Graph domGraph = GraphUtils.buildDominatorGraph(domTree, program.basicBlockCount());
        Task[] stack = new Task[cfg.size() * 2];
        int head = 0;
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            if (domGraph.incomingEdgesCount(i) == 0) {
                Task task = new Task();
                task.block = program.basicBlockAt(i);
                task.variables = Arrays.copyOf(variableMap, variableMap.length);
                stack[head++] = task;
            }
        }

        List<List<TryCatchBlock>> caughtBlocks = new ArrayList<>();
        List<List<Phi>> specialPhis = new ArrayList<>();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            caughtBlocks.add(new ArrayList<>());
            specialPhis.add(new ArrayList<>());
        }
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            for (TryCatchBlock tryCatch : program.basicBlockAt(i).getTryCatchBlocks()) {
                caughtBlocks.get(tryCatch.getHandler().getIndex()).add(tryCatch);
            }
        }
        boolean[] processed = new boolean[program.basicBlockCount()];
        while (head > 0) {
            Task task = stack[--head];
            currentBlock = task.block;
            if (processed[currentBlock.getIndex()]) {
                continue;
            }
            processed[currentBlock.getIndex()] = true;
            variableMap = Arrays.copyOf(task.variables, task.variables.length);
            for (Phi phi : currentBlock.getPhis()) {
                Variable var = program.createVariable();
                var.getDebugNames().addAll(phi.getReceiver().getDebugNames());
                variableMap[phi.getReceiver().getIndex()] = var;
                phi.setReceiver(var);
            }
            if (!caughtBlocks.get(currentBlock.getIndex()).isEmpty()) {
                Phi phi = new Phi();
                phi.setReceiver(program.createVariable());
                for (TryCatchBlock tryCatch : caughtBlocks.get(currentBlock.getIndex())) {
                    variableMap[tryCatch.getExceptionVariable().getIndex()] = phi.getReceiver();
                    Set<String> debugNames = tryCatch.getExceptionVariable().getDebugNames();
                    tryCatch.setExceptionVariable(program.createVariable());
                    tryCatch.getExceptionVariable().getDebugNames().addAll(debugNames);
                    Incoming incoming = new Incoming();
                    incoming.setSource(tryCatch.getProtectedBlock());
                    incoming.setValue(tryCatch.getExceptionVariable());
                    phi.getIncomings().add(incoming);
                }
                specialPhis.get(currentBlock.getIndex()).add(phi);
            }
            for (Instruction insn : currentBlock.getInstructions()) {
                insn.acceptVisitor(consumer);
            }
            int[] successors = domGraph.outgoingEdges(currentBlock.getIndex());
            for (int successor : successors) {
                Task next = new Task();
                next.variables = Arrays.copyOf(variableMap, variableMap.length);
                next.block = program.basicBlockAt(successor);
                stack[head++] = next;
            }
            successors = cfg.outgoingEdges(currentBlock.getIndex());
            for (int successor : successors) {
                int[] phiIndexes = phiIndexMap[successor];
                List<Phi> phis = program.basicBlockAt(successor).getPhis();
                for (int j = 0; j < phis.size(); ++j) {
                    Phi phi = phis.get(j);
                    Variable var = variableMap[phiIndexes[j]];
                    if (var != null) {
                        Incoming incoming = new Incoming();
                        incoming.setSource(currentBlock);
                        incoming.setValue(var);
                        phi.getIncomings().add(incoming);
                        phi.getReceiver().getDebugNames().addAll(var.getDebugNames());
                    }
                }
            }
        }
        for (int i = 0; i < specialPhis.size(); ++i) {
            program.basicBlockAt(i).getPhis().addAll(specialPhis.get(i));
        }
    }

    private void markAssignment(Variable var) {
        BasicBlock[] worklist = new BasicBlock[program.basicBlockCount() * 4];
        int head = 0;
        worklist[head++] = currentBlock;
        while (head > 0) {
            BasicBlock block = worklist[--head];
            int[] frontiers = domFrontiers[block.getIndex()];
            if (frontiers == null) {
                continue;
            }
            for (int frontier : frontiers) {
                BasicBlock frontierBlock = program.basicBlockAt(frontier);
                frontierBlock.getPhis();
                Phi phi = phiMap[frontier][var.getIndex()];
                if (phi == null) {
                    phi = new Phi();
                    phi.setReceiver(var);
                    phiIndexMap[frontier][frontierBlock.getPhis().size()] = var.getIndex();
                    frontierBlock.getPhis().add(phi);
                    phiMap[frontier][var.getIndex()] = phi;
                    worklist[head++] = frontierBlock;
                }
            }
        }
    }

    private Variable define(Variable var) {
        Variable result = program.createVariable();
        variableMap[var.getIndex()] = result;
        return result;
    }

    private Variable use(Variable var) {
        Variable mappedVar = variableMap[var.getIndex()];
        if (mappedVar == null) {
            throw new AssertionError();
        }
        return mappedVar;
    }

    private InstructionVisitor consumer = new InstructionVisitor() {
        @Override
        public void visit(EmptyInstruction insn) {
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(NullConstantInstruction insn) {
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(IntegerConstantInstruction insn) {
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(LongConstantInstruction insn) {
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(FloatConstantInstruction insn) {
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(DoubleConstantInstruction insn) {
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(BinaryInstruction insn) {
            insn.setFirstOperand(use(insn.getFirstOperand()));
            insn.setSecondOperand(use(insn.getSecondOperand()));
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(NegateInstruction insn) {
            insn.setOperand(use(insn.getOperand()));
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(AssignInstruction insn) {
            insn.setAssignee(use(insn.getAssignee()));
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(BranchingInstruction insn) {
            insn.setOperand(use(insn.getOperand()));
        }

        @Override
        public void visit(BinaryBranchingInstruction insn) {
            insn.setFirstOperand(use(insn.getFirstOperand()));
            insn.setSecondOperand(use(insn.getSecondOperand()));
        }

        @Override
        public void visit(JumpInstruction insn) {
        }

        @Override
        public void visit(SwitchInstruction insn) {
            insn.setCondition(use(insn.getCondition()));
        }

        @Override
        public void visit(ExitInstruction insn) {
            if (insn.getValueToReturn() != null) {
                insn.setValueToReturn(use(insn.getValueToReturn()));
            }
        }

        @Override
        public void visit(RaiseInstruction insn) {
            insn.setException(use(insn.getException()));
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            insn.setSize(use(insn.getSize()));
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(ConstructInstruction insn) {
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
            List<Variable> dimensions = insn.getDimensions();
            for (int i = 0; i < dimensions.size(); ++i) {
                dimensions.set(i, use(dimensions.get(i)));
            }
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            if (insn.getInstance() != null) {
                insn.setInstance(use(insn.getInstance()));
            }
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(PutFieldInstruction insn) {
            if (insn.getInstance() != null) {
                insn.setInstance(use(insn.getInstance()));
            }
            insn.setValue(use(insn.getValue()));
        }

        @Override
        public void visit(GetElementInstruction insn) {
            insn.setArray(use(insn.getArray()));
            insn.setIndex(use(insn.getIndex()));
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(PutElementInstruction insn) {
            insn.setArray(use(insn.getArray()));
            insn.setIndex(use(insn.getIndex()));
            insn.setValue(use(insn.getValue()));
        }

        @Override
        public void visit(InvokeInstruction insn) {
            List<Variable> args = insn.getArguments();
            for (int i = 0; i < args.size(); ++i) {
                args.set(i, use(args.get(i)));
            }
            if (insn.getInstance() != null) {
                insn.setInstance(use(insn.getInstance()));
            }
            if (insn.getReceiver() != null) {
                insn.setReceiver(define(insn.getReceiver()));
            }
        }

        @Override
        public void visit(InvokeDynamicInstruction insn) {
            List<Variable> args = insn.getArguments();
            for (int i = 0; i < args.size(); ++i) {
                args.set(i, use(args.get(i)));
            }
            if (insn.getInstance() != null) {
                insn.setInstance(use(insn.getInstance()));
            }
            if (insn.getReceiver() != null) {
                insn.setReceiver(define(insn.getReceiver()));
            }
        }

        @Override
        public void visit(IsInstanceInstruction insn) {
            insn.setValue(use(insn.getValue()));
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(CastInstruction insn) {
            insn.setValue(use(insn.getValue()));
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(CastNumberInstruction insn) {
            insn.setValue(use(insn.getValue()));
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(CastIntegerInstruction insn) {
            insn.setValue(use(insn.getValue()));
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(ArrayLengthInstruction insn) {
            insn.setArray(use(insn.getArray()));
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
            insn.setArray(use(insn.getArray()));
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
            insn.setArray(use(insn.getArray()));
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(InitClassInstruction insn) {
        }

        @Override
        public void visit(NullCheckInstruction insn) {
            insn.setValue(use(insn.getValue()));
            insn.setReceiver(define(insn.getReceiver()));
        }

        @Override
        public void visit(MonitorEnterInstruction insn) {
            insn.setObjectRef(use(insn.getObjectRef()));
        }

        @Override
        public void visit(MonitorExitInstruction insn) {
            insn.setObjectRef(use(insn.getObjectRef()));
        }
    };
}
