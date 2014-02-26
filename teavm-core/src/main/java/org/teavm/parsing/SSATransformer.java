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
package org.teavm.parsing;

import java.util.Arrays;
import java.util.List;
import org.teavm.common.*;
import org.teavm.model.*;
import org.teavm.model.instructions.*;
import org.teavm.model.util.DefinitionExtractor;
import org.teavm.model.util.ProgramUtils;

/**
 *
 * @author Alexey Andreev
 */
public class SSATransformer {
    private Program program;
    private Graph cfg;
    private DominatorTree domTree;
    private Graph domGraph;
    private int[][] domFrontiers;
    private Variable[] variableMap;
    private BasicBlock currentBlock;
    private Phi[][] phiMap;
    private int[][] phiIndexMap;
    private ValueType[] arguments;

    public void transformToSSA(Program program, ValueType[] arguments) {
        if (program.basicBlockCount() == 0) {
            return;
        }
        this.program = program;
        this.arguments = arguments;
        cfg = ProgramUtils.buildControlFlowGraph(program);
        domTree = GraphUtils.buildDominatorTree(cfg);
        domFrontiers = new int[cfg.size()][];
        variableMap = new Variable[program.variableCount()];
        phiMap = new Phi[program.basicBlockCount()][];
        phiIndexMap = new int[program.basicBlockCount()][];
        for (int i = 0; i < phiMap.length; ++i) {
            phiMap[i] = new Phi[program.variableCount()];
            phiIndexMap[i] = new int[program.variableCount()];
        }
        applySignature();
        domFrontiers = GraphUtils.findDominanceFrontiers(cfg, domTree);
        estimatePhis();
        renameVariables();
    }

    private void applySignature() {
        if (program.variableCount() == 0) {
            return;
        }
        int index = 0;
        variableMap[index] = program.variableAt(index);
        ++index;
        for (int i = 0; i < arguments.length; ++i) {
            variableMap[index] = program.variableAt(index);
            ++index;
            ValueType arg = arguments[i];
            if (arg instanceof ValueType.Primitive) {
                PrimitiveType kind = ((ValueType.Primitive)arg).getKind();
                if (kind == PrimitiveType.LONG || kind == PrimitiveType.DOUBLE) {
                    variableMap[index] = variableMap[index - 1];
                    ++index;
                }
            }
        }
        arguments = null;
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
        }
    }

    static class Task {
        Variable[] variables;
        BasicBlock block;
    }

    private void renameVariables() {
        domGraph = GraphUtils.buildDominatorGraph(domTree, program.basicBlockCount());
        Task[] stack = new Task[cfg.size() * 2];
        int head = 0;
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            if (domTree.immediateDominatorOf(i) < 0) {
                Task task = new Task();
                task.block = program.basicBlockAt(i);
                task.variables = Arrays.copyOf(variableMap, variableMap.length);
                stack[head++] = task;
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
                variableMap[phi.getReceiver().getIndex()] = var;
                phi.setReceiver(var);
            }
            for (Instruction insn : currentBlock.getInstructions()) {
                insn.acceptVisitor(consumer);
            }
            for (TryCatchBlock tryCatch : currentBlock.getTryCatchBlocks()) {
                define(tryCatch.getExceptionVariable());
            }
            int[] successors = domGraph.outgoingEdges(currentBlock.getIndex());
            for (int i = 0; i < successors.length; ++i) {
                Task next = new Task();
                next.variables = Arrays.copyOf(variableMap, variableMap.length);
                next.block = program.basicBlockAt(successors[i]);
                stack[head++] = next;
            }
            successors = cfg.outgoingEdges(currentBlock.getIndex());
            for (int i = 0; i < successors.length; ++i) {
                int successor = successors[i];
                int[] phiIndexes = phiIndexMap[successor];
                List<Phi> phis = program.basicBlockAt(successor).getPhis();
                for (int j = 0; j < phis.size(); ++j) {
                    Variable var = variableMap[phiIndexes[j]];
                    if (var != null) {
                        Incoming incoming = new Incoming();
                        incoming.setSource(currentBlock);
                        incoming.setValue(var);
                        phis.get(j).getIncomings().add(incoming);
                    }
                }
            }
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
    };
}
