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

import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.IntDeque;
import com.carrotsearch.hppc.IntObjectMap;
import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.IntSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.common.IntegerArray;
import org.teavm.model.BasicBlock;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryBranchingInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BranchingInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.CastIntegerInstruction;
import org.teavm.model.instructions.CastNumberInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.EmptyInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InstructionVisitor;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.MonitorEnterInstruction;
import org.teavm.model.instructions.MonitorExitInstruction;
import org.teavm.model.instructions.NegateInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.instructions.SwitchInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;

public class PhiUpdater {
    private Program program;
    private Graph cfg;
    private DominatorTree domTree;
    private Graph domGraph;
    private int[][] domFrontiers;
    private Variable[] variableMap;
    private boolean[] variableDefined;
    private List<List<Variable>> definedVersions = new ArrayList<>();
    private BasicBlock currentBlock;
    private Phi[][] phiMap;
    private int[][] phiIndexMap;
    private List<List<Phi>> synthesizedPhisByBlock = new ArrayList<>();
    private IntObjectMap<Phi> phisByReceiver = new IntObjectOpenHashMap<>();
    private BitSet usedPhis = new BitSet();
    private Variable[] originalExceptionVariables;
    private boolean[] usedDefinitions;
    private IntegerArray variableToSourceMap = new IntegerArray(10);
    private List<Phi> synthesizedPhis = new ArrayList<>();

    public int getSourceVariable(int var) {
        if (var >= variableToSourceMap.size()) {
            return -1;
        }
        return variableToSourceMap.get(var);
    }

    public List<Phi> getSynthesizedPhis() {
        return synthesizedPhis;
    }

    public void updatePhis(Program program, Variable[] arguments) {
        if (program.basicBlockCount() == 0) {
            return;
        }
        this.program = program;
        phisByReceiver.clear();
        cfg = ProgramUtils.buildControlFlowGraph(program);
        domTree = GraphUtils.buildDominatorTree(cfg);
        domFrontiers = new int[cfg.size()][];
        domGraph = GraphUtils.buildDominatorGraph(domTree, program.basicBlockCount());

        variableMap = new Variable[program.variableCount()];
        usedDefinitions = new boolean[program.variableCount()];
        for (int i = 0; i < arguments.length; ++i) {
            variableMap[i] = arguments[i];
            usedDefinitions[i] = true;
        }

        for (int i = 0; i < program.variableCount(); ++i) {
            variableToSourceMap.add(-1);
        }
        definedVersions.addAll(Collections.nCopies(program.variableCount(), null));

        phiMap = new Phi[program.basicBlockCount()][];
        phiIndexMap = new int[program.basicBlockCount()][];
        for (int i = 0; i < phiMap.length; ++i) {
            phiMap[i] = new Phi[program.variableCount()];
            phiIndexMap[i] = new int[program.variableCount()];
        }
        domFrontiers = GraphUtils.findDominanceFrontiers(cfg, domTree);

        synthesizedPhisByBlock.clear();

        for (int i = 0; i < program.basicBlockCount(); ++i) {
            synthesizedPhisByBlock.add(new ArrayList<>());
        }

        originalExceptionVariables = new Variable[program.basicBlockCount()];
        Arrays.setAll(originalExceptionVariables, i -> program.basicBlockAt(i).getExceptionVariable());

        estimatePhis();
        renameVariables();
        propagatePhiUsageInformation();
        addSynthesizedPhis();
    }

    private void estimatePhis() {
        DefinitionExtractor definitionExtractor = new DefinitionExtractor();
        variableDefined = new boolean[program.variableCount()];

        IntDeque stack = new IntArrayDeque();
        stack.addLast(0);
        while (!stack.isEmpty()) {
            int i = stack.removeLast();
            currentBlock = program.basicBlockAt(i);

            if (currentBlock.getExceptionVariable() != null) {
                markAssignment(currentBlock.getExceptionVariable());
            }

            for (Phi phi : currentBlock.getPhis()) {
                markAssignment(phi.getReceiver());
            }

            for (Instruction insn : currentBlock) {
                currentBlock = program.basicBlockAt(i);
                insn.acceptVisitor(definitionExtractor);
                for (Variable var : definitionExtractor.getDefinedVariables()) {
                    markAssignment(var);
                }
            }

            for (int successor : domGraph.outgoingEdges(i)) {
                stack.addLast(successor);
            }
        }
    }

    private static class Task {
        Variable[] variables;
        BasicBlock block;
    }

    private void renameVariables() {
        Deque<Task> stack = new ArrayDeque<>();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            if (domGraph.incomingEdgesCount(i) == 0) {
                Task task = new Task();
                task.block = program.basicBlockAt(i);
                task.variables = variableMap.clone();
                stack.push(task);
            }
        }

        List<List<Incoming>> phiOutputs = ProgramUtils.getPhiOutputs(program);

        while (!stack.isEmpty()) {
            Collections.fill(definedVersions, null);
            Task task = stack.pop();

            currentBlock = task.block;
            int index = currentBlock.getIndex();
            variableMap = task.variables.clone();

            if (currentBlock.getExceptionVariable() != null) {
                currentBlock.setExceptionVariable(define(currentBlock.getExceptionVariable()));
            }

            for (Phi phi : synthesizedPhisByBlock.get(index)) {
                Variable var = program.createVariable();
                var.setDebugName(phi.getReceiver().getDebugName());
                var.setLabel(phi.getReceiver().getLabel());
                mapVariable(phi.getReceiver().getIndex(), var);
                phisByReceiver.put(var.getIndex(), phi);
                phi.setReceiver(var);
            }
            for (Phi phi : currentBlock.getPhis()) {
                phi.setReceiver(define(phi.getReceiver()));
            }

            for (Instruction insn : currentBlock) {
                insn.acceptVisitor(consumer);
            }

            int[] successors = domGraph.outgoingEdges(index);

            for (Incoming output : phiOutputs.get(index)) {
                Variable var = output.getValue();
                output.setValue(use(var));
            }

            for (int j = successors.length - 1; j >= 0; --j) {
                int successor = successors[j];
                Task next = new Task();
                next.variables = variableMap.clone();
                next.block = program.basicBlockAt(successor);
                stack.push(next);
            }

            IntSet exceptionHandlingSuccessors = new IntOpenHashSet();
            for (TryCatchBlock tryCatch : currentBlock.getTryCatchBlocks()) {
                exceptionHandlingSuccessors.add(tryCatch.getHandler().getIndex());
            }

            for (int successor : cfg.outgoingEdges(index)) {
                renameOutgoingPhis(successor, exceptionHandlingSuccessors.contains(successor));
            }
        }
    }

    private void addSynthesizedPhis() {
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            for (Phi phi : synthesizedPhisByBlock.get(i)) {
                if (!usedPhis.get(phi.getReceiver().getIndex())) {
                    continue;
                }
                if (!phi.getIncomings().isEmpty()) {
                    program.basicBlockAt(i).getPhis().add(phi);
                    synthesizedPhis.add(phi);
                }
            }
        }
    }

    private void propagatePhiUsageInformation() {
        IntDeque worklist = new IntArrayDeque();
        for (int receiverIndex : phisByReceiver.keys().toArray()) {
            if (usedPhis.get(receiverIndex)) {
                worklist.addLast(receiverIndex);
            }
        }

        IntSet visited = new IntOpenHashSet();
        while (!worklist.isEmpty()) {
            int varIndex = worklist.removeFirst();
            if (!visited.add(varIndex)) {
                continue;
            }
            usedPhis.set(varIndex);

            Phi phi = phisByReceiver.get(varIndex);
            if (phi != null) {
                for (Incoming incoming : phi.getIncomings()) {
                    if (!visited.contains(incoming.getValue().getIndex())) {
                        worklist.addLast(incoming.getValue().getIndex());
                    }
                }
            }
        }
    }

    private void renameOutgoingPhis(int successor, boolean allVersions) {
        int[] phiIndexes = phiIndexMap[successor];
        List<Phi> phis = synthesizedPhisByBlock.get(successor);

        for (int j = 0; j < phis.size(); ++j) {
            Phi phi = phis.get(j);
            Variable var = variableMap[phiIndexes[j]];
            if (var != null) {
                List<Variable> versions = definedVersions.get(phiIndexes[j]);
                if (versions != null && allVersions) {
                    for (Variable version : versions) {
                        Incoming incoming = new Incoming();
                        incoming.setSource(currentBlock);
                        incoming.setValue(version);
                        phi.getIncomings().add(incoming);
                    }
                }

                Incoming incoming = new Incoming();
                incoming.setSource(currentBlock);
                incoming.setValue(var);
                phi.getIncomings().add(incoming);
                phi.getReceiver().setDebugName(var.getDebugName());
            }
        }
    }

    private void markAssignment(Variable var) {
        Deque<BasicBlock> worklist = new ArrayDeque<>();
        worklist.push(currentBlock);

        if (variableDefined[var.getIndex()]) {
            for (TryCatchBlock tryCatch : currentBlock.getTryCatchBlocks()) {
                placePhi(tryCatch.getHandler().getIndex(), var, currentBlock, worklist);
            }
        } else {
            variableDefined[var.getIndex()] = true;
        }

        while (!worklist.isEmpty()) {
            BasicBlock block = worklist.pop();
            int[] frontiers = domFrontiers[block.getIndex()];

            if (frontiers != null) {
                for (int frontier : frontiers) {
                    placePhi(frontier, var, block, worklist);
                }
            }
        }
    }

    private void placePhi(int frontier, Variable var, BasicBlock block, Deque<BasicBlock> worklist) {
        BasicBlock frontierBlock = program.basicBlockAt(frontier);
        if (frontierBlock.getExceptionVariable() == var) {
            return;
        }

        boolean exists = frontierBlock.getPhis().stream()
                .flatMap(phi -> phi.getIncomings().stream())
                .anyMatch(incoming -> incoming.getSource() == block && incoming.getValue() == var);
        if (exists) {
            return;
        }

        Phi phi = phiMap[frontier][var.getIndex()];
        if (phi == null) {
            phi = new Phi();
            phi.setReceiver(var);
            phiIndexMap[frontier][synthesizedPhisByBlock.get(frontier).size()] = var.getIndex();
            synthesizedPhisByBlock.get(frontier).add(phi);
            phiMap[frontier][var.getIndex()] = phi;
            worklist.push(frontierBlock);
        }
    }

    private Variable define(Variable var) {
        Variable old = variableMap[var.getIndex()];
        if (old != null) {
            if (definedVersions.get(var.getIndex()) == null) {
                definedVersions.set(var.getIndex(), new ArrayList<>());
            }
            definedVersions.get(var.getIndex()).add(old);
        }

        Variable original = var;
        var = introduce(var);
        mapVariable(original.getIndex(), var);
        return var;
    }

    private void mapVariable(int index, Variable var) {
        variableMap[index] = var;
        while (variableToSourceMap.size() <= var.getIndex()) {
            variableToSourceMap.add(-1);
        }
        variableToSourceMap.set(var.getIndex(), index);
    }

    private Variable introduce(Variable var) {
        if (!usedDefinitions[var.getIndex()]) {
            usedDefinitions[var.getIndex()] = true;
        } else {
            Variable old = var;
            var = program.createVariable();
            var.setDebugName(old.getDebugName());
            var.setLabel(old.getLabel());
        }

        return var;
    }

    private Variable use(Variable var) {
        Variable mappedVar = variableMap[var.getIndex()];
        if (mappedVar == null) {
            throw new AssertionError("Variable used before definition: @" + var.getDisplayLabel()
                    + " at $" + currentBlock.getIndex());
        }
        usedPhis.set(mappedVar.getIndex());
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

