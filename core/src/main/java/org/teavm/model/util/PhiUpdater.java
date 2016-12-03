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

import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.IntSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.teavm.model.TryCatchJoint;
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
    private int[][] domFrontiers;
    private Variable[] variableMap;
    private BasicBlock currentBlock;
    private Phi[][] phiMap;
    private int[][] phiIndexMap;
    private Map<TryCatchBlock, Map<Variable, TryCatchJoint>> jointMap = new HashMap<>();
    private List<List<Phi>> synthesizedPhis = new ArrayList<>();
    private List<List<List<TryCatchJoint>>> synthesizedJoints = new ArrayList<>();
    private Variable[] originalExceptionVariables;
    private boolean[] usedDefinitions;
    private IntegerArray variableToSourceMap = new IntegerArray(10);

    public int getSourceVariable(int var) {
        return variableToSourceMap.get(var);
    }

    public void updatePhis(Program program, Variable[] arguments) {
        if (program.basicBlockCount() == 0) {
            return;
        }
        this.program = program;
        cfg = ProgramUtils.buildControlFlowGraph(program);
        DominatorTree domTree = GraphUtils.buildDominatorTree(cfg);
        domFrontiers = new int[cfg.size()][];
        variableMap = new Variable[program.variableCount()];
        usedDefinitions = new boolean[program.variableCount()];
        for (int i = 0; i < arguments.length; ++i) {
            variableMap[i] = arguments[i];
            usedDefinitions[i] = true;
        }
        for (int i = 0; i < program.variableCount(); ++i) {
            variableToSourceMap.add(-1);
        }
        phiMap = new Phi[program.basicBlockCount()][];
        phiIndexMap = new int[program.basicBlockCount()][];
        jointMap.clear();
        for (int i = 0; i < phiMap.length; ++i) {
            phiMap[i] = new Phi[program.variableCount()];
            phiIndexMap[i] = new int[program.variableCount()];
        }
        domFrontiers = GraphUtils.findDominanceFrontiers(cfg, domTree);

        synthesizedPhis.clear();
        synthesizedJoints.clear();

        for (int i = 0; i < program.basicBlockCount(); ++i) {
            synthesizedPhis.add(new ArrayList<>());
            synthesizedJoints.add(new ArrayList<>());
            int catchCount = program.basicBlockAt(i).getTryCatchBlocks().size();
            for (int j = 0; j < catchCount; ++j) {
                synthesizedJoints.get(i).add(new ArrayList<>());
            }
        }

        originalExceptionVariables = new Variable[program.basicBlockCount()];
        Arrays.setAll(originalExceptionVariables, i -> program.basicBlockAt(i).getExceptionVariable());

        estimatePhis();
        renameVariables();
    }

    private void estimatePhis() {
        DefinitionExtractor definitionExtractor = new DefinitionExtractor();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            currentBlock = program.basicBlockAt(i);

            if (currentBlock.getExceptionVariable() != null) {
                markAssignment(currentBlock.getExceptionVariable());
            }

            for (Phi phi : currentBlock.getPhis()) {
                markAssignment(phi.getReceiver());
            }

            for (Instruction insn : currentBlock.getInstructions()) {
                currentBlock = program.basicBlockAt(i);
                insn.acceptVisitor(definitionExtractor);
                Set<Variable> definedVariables = new HashSet<>();
                for (Variable var : definitionExtractor.getDefinedVariables()) {
                    markAssignment(var);
                    definedVariables.add(var);
                }

                Set<BasicBlock> handlers = currentBlock.getTryCatchBlocks().stream()
                        .map(tryCatch -> tryCatch.getHandler())
                        .collect(Collectors.toSet());
                for (BasicBlock handler : handlers) {
                    currentBlock = handler;
                    for (Variable var : definedVariables) {
                        markAssignment(var);
                    }
                }
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
                task.variables = variableMap.clone();
                stack[head++] = task;
            }
        }

        List<List<Incoming>> phiOutputs = ProgramUtils.getPhiOutputs(program);

        while (head > 0) {
            Task task = stack[--head];

            currentBlock = task.block;
            int index = currentBlock.getIndex();
            variableMap = task.variables.clone();

            if (currentBlock.getExceptionVariable() != null) {
                currentBlock.setExceptionVariable(define(currentBlock.getExceptionVariable()));
            }

            for (Phi phi : synthesizedPhis.get(index)) {
                Variable var = program.createVariable();
                var.setDebugName(phi.getReceiver().getDebugName());
                mapVariable(phi.getReceiver().getIndex(), var);
                phi.setReceiver(var);
            }
            for (Phi phi : currentBlock.getPhis()) {
                phi.setReceiver(define(phi.getReceiver()));
            }

            for (Instruction insn : currentBlock.getInstructions()) {
                insn.acceptVisitor(consumer);
            }

            for (Incoming output : phiOutputs.get(index)) {
                Variable var = output.getValue();
                output.setValue(use(var));
            }

            for (TryCatchBlock tryCatch : currentBlock.getTryCatchBlocks()) {
                for (TryCatchJoint joint : tryCatch.getJoints()) {
                    for (int i = 0; i < joint.getSourceVariables().size(); ++i) {
                        joint.getSourceVariables().set(i, use(joint.getSourceVariables().get(i)));
                    }
                    joint.setReceiver(define(joint.getReceiver()));
                }
            }

            IntSet catchSuccessors = new IntOpenHashSet();
            Variable[] regularVariableMap = variableMap;
            Variable[] catchVariableMap = variableMap.clone();

            variableMap = catchVariableMap;
            for (int i = 0; i < currentBlock.getTryCatchBlocks().size(); ++i) {
                TryCatchBlock tryCatch = currentBlock.getTryCatchBlocks().get(i);
                catchSuccessors.add(tryCatch.getHandler().getIndex());
                for (TryCatchJoint joint : synthesizedJoints.get(index).get(i)) {
                    joint.setReceiver(defineForExceptionPhi(joint.getReceiver()));
                }
            }
            variableMap = regularVariableMap;

            int[] successors = domGraph.outgoingEdges(index);
            for (int successor : successors) {
                Task next = new Task();
                next.variables = (catchSuccessors.contains(successor) ? catchVariableMap : variableMap).clone();
                next.block = program.basicBlockAt(successor);
                stack[head++] = next;
            }

            successors = cfg.outgoingEdges(index);
            for (int successor : successors) {
                variableMap = catchSuccessors.contains(successor) ? catchVariableMap : variableMap;
                renameOutgoingPhis(successor);
            }
        }

        for (int i = 0; i < program.basicBlockCount(); ++i) {
            for (Phi phi : synthesizedPhis.get(i)) {
                if (!phi.getIncomings().isEmpty()) {
                    program.basicBlockAt(i).getPhis().add(phi);
                }
            }

            List<List<TryCatchJoint>> joints = synthesizedJoints.get(i);
            for (int j = 0; j < joints.size(); ++j) {
                List<TryCatchJoint> jointList = joints.get(j);
                TryCatchBlock targetTryCatch = program.basicBlockAt(i).getTryCatchBlocks().get(j);
                for (TryCatchJoint joint : jointList) {
                    if (!joint.getSourceVariables().isEmpty()) {
                        targetTryCatch.getJoints().add(joint);
                    }
                }
            }
        }
    }

    private void renameOutgoingPhis(int successor) {
        int[] phiIndexes = phiIndexMap[successor];
        List<Phi> phis = synthesizedPhis.get(successor);

        for (int j = 0; j < phis.size(); ++j) {
            Phi phi = phis.get(j);
            Variable var = variableMap[phiIndexes[j]];
            if (var != null) {
                Incoming incoming = new Incoming();
                incoming.setSource(currentBlock);
                incoming.setValue(var);
                phi.getIncomings().add(incoming);
                phi.getReceiver().setDebugName(var.getDebugName());
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

            if (frontiers != null) {
                for (int frontier : frontiers) {
                    BasicBlock frontierBlock = program.basicBlockAt(frontier);
                    if (frontierBlock.getExceptionVariable() == var) {
                        continue;
                    }

                    boolean exists = frontierBlock.getPhis().stream()
                            .flatMap(phi -> phi.getIncomings().stream())
                            .anyMatch(incoming -> incoming.getSource() == block && incoming.getValue() == var);
                    if (exists) {
                        continue;
                    }

                    Phi phi = phiMap[frontier][var.getIndex()];
                    if (phi == null) {
                        phi = new Phi();
                        phi.setReceiver(var);
                        phiIndexMap[frontier][synthesizedPhis.get(frontier).size()] = var.getIndex();
                        synthesizedPhis.get(frontier).add(phi);
                        phiMap[frontier][var.getIndex()] = phi;
                        worklist[head++] = frontierBlock;
                    }
                }
            }

            List<TryCatchBlock> tryCatchBlocks = block.getTryCatchBlocks();
            for (int i = 0; i < tryCatchBlocks.size(); i++) {
                TryCatchBlock tryCatch = tryCatchBlocks.get(i);
                TryCatchJoint joint = jointMap.computeIfAbsent(tryCatch, k -> new HashMap<>()).get(var);
                if (joint == null) {
                    joint = new TryCatchJoint();
                    joint.setReceiver(var);
                    synthesizedJoints.get(block.getIndex()).get(i).add(joint);
                    jointMap.get(tryCatch).put(var, joint);
                    worklist[head++] = tryCatch.getHandler();
                }
            }
        }
    }

    private Variable defineForExceptionPhi(Variable var) {
        Variable original = var;
        var = introduce(var);
        mapVariable(original.getIndex(), var);
        return var;
    }

    private Variable define(Variable var) {
        Variable old = variableMap[var.getIndex()];
        Variable original = var;
        var = introduce(var);
        propagateToTryCatch(original, var, old);
        mapVariable(original.getIndex(), var);
        return var;
    }

    private void propagateToTryCatch(Variable original, Variable var, Variable old) {
        for (int i = 0; i < currentBlock.getTryCatchBlocks().size(); ++i) {
            TryCatchBlock tryCatch = currentBlock.getTryCatchBlocks().get(i);
            if (originalExceptionVariables[tryCatch.getHandler().getIndex()] == original) {
                continue;
            }

            Map<Variable, TryCatchJoint> joints = jointMap.get(tryCatch);
            if (joints == null) {
                continue;
            }

            TryCatchJoint joint = joints.get(original);
            if (joint == null) {
                continue;
            }

            if (joint.getSourceVariables().isEmpty() && old != null) {
                joint.getSourceVariables().add(old);
            }
            joint.getSourceVariables().add(var);
        }
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
            var = program.createVariable();
        }

        return var;
    }

    private Variable use(Variable var) {
        Variable mappedVar = variableMap[var.getIndex()];
        if (mappedVar == null) {
            throw new AssertionError("Variable used before definition: " + var.getIndex());
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
