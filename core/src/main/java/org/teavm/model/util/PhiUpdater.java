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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
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
    private List<Map<BasicBlock, Map<Variable, TryCatchJoint>>> jointMap = new ArrayList<>();
    private List<List<Phi>> synthesizedPhis = new ArrayList<>();
    private List<List<TryCatchJoint>> synthesizedJoints = new ArrayList<>();
    private boolean[] usedDefinitions;

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
        phiMap = new Phi[program.basicBlockCount()][];
        phiIndexMap = new int[program.basicBlockCount()][];
        jointMap = new ArrayList<>();
        for (int i = 0; i < phiMap.length; ++i) {
            phiMap[i] = new Phi[program.variableCount()];
            phiIndexMap[i] = new int[program.variableCount()];
            jointMap.add(new HashMap<>());
        }
        domFrontiers = GraphUtils.findDominanceFrontiers(cfg, domTree);

        synthesizedPhis.clear();
        synthesizedJoints.clear();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            synthesizedPhis.add(new ArrayList<>());
            synthesizedJoints.add(new ArrayList<>());
        }
        estimatePhis();
        renameVariables();
    }

    private void estimatePhis() {
        DefinitionExtractor definitionExtractor = new DefinitionExtractor();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            currentBlock = program.basicBlockAt(i);
            for (Phi phi : currentBlock.getPhis()) {
                markAssignment(phi.getReceiver());
            }
            for (TryCatchJoint joint : currentBlock.getTryCatchJoints()) {
                markAssignment(joint.getReceiver());
            }
            for (Instruction insn : currentBlock.getInstructions()) {
                insn.acceptVisitor(definitionExtractor);
                for (Variable var : definitionExtractor.getDefinedVariables()) {
                    markAssignment(var);
                }
            }
            for (TryCatchBlock tryCatch : currentBlock.getTryCatchBlocks()) {
                markAssignment(tryCatch.getExceptionVariable());
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

        List<List<Incoming>> phiOutputs = ProgramUtils.getPhiOutputs(program);
        List<List<TryCatchJoint>> existingOutputJoints = ProgramUtils.getOutputJoints(program);

        boolean[] processed = new boolean[program.basicBlockCount()];
        while (head > 0) {
            Task task = stack[--head];
            currentBlock = task.block;
            int index = currentBlock.getIndex();
            if (processed[index]) {
                continue;
            }
            processed[index] = true;
            variableMap = Arrays.copyOf(task.variables, task.variables.length);

            for (Phi phi : synthesizedPhis.get(index)) {
                Variable var = program.createVariable();
                var.getDebugNames().addAll(phi.getReceiver().getDebugNames());
                propagateToTryCatch(phi.getReceiver(), var, null);
                variableMap[phi.getReceiver().getIndex()] = var;
                phi.setReceiver(var);
            }
            for (TryCatchJoint joint : synthesizedJoints.get(index)) {
                Variable var = program.createVariable();
                var.getDebugNames().addAll(joint.getReceiver().getDebugNames());
                variableMap[joint.getReceiver().getIndex()] = var;
                joint.setReceiver(var);
            }
            for (Phi phi : currentBlock.getPhis()) {
                phi.setReceiver(define(phi.getReceiver()));
            }
            for (TryCatchJoint joint : currentBlock.getTryCatchJoints()) {
                joint.setReceiver(define(joint.getReceiver()));
            }

            for (Instruction insn : currentBlock.getInstructions()) {
                insn.acceptVisitor(consumer);
            }

            Map<BasicBlock, Map<Variable, Variable>> tryCatchVariableMap = new HashMap<>();
            for (TryCatchBlock tryCatch : currentBlock.getTryCatchBlocks()) {
                Map<Variable, Variable> catchVariableMap = new HashMap<>();
                tryCatchVariableMap.put(tryCatch.getHandler(), catchVariableMap);
                Variable var = tryCatch.getExceptionVariable();
                if (var != null) {
                    Variable newVar = introduce(var);
                    tryCatch.setExceptionVariable(newVar);
                    catchVariableMap.put(var, newVar);
                }
            }

            for (Incoming output : phiOutputs.get(index)) {
                Variable var = output.getValue();
                Variable exceptionVar = tryCatchVariableMap
                        .getOrDefault(output.getPhi().getBasicBlock(), Collections.emptyMap())
                        .get(var);
                output.setValue(exceptionVar != null ? exceptionVar : use(var));
            }
            for (TryCatchJoint joint : existingOutputJoints.get(index)) {
                for (int i = 0; i < joint.getSourceVariables().size(); ++i) {
                    Variable var = joint.getSourceVariables().get(i);
                    Variable exceptionVar = tryCatchVariableMap
                            .getOrDefault(joint.getBlock(), Collections.emptyMap())
                            .get(var);
                    joint.getSourceVariables().set(i, exceptionVar != null ? exceptionVar : use(var));
                }
            }

            int[] successors = domGraph.outgoingEdges(index);
            for (int successor : successors) {
                Task next = new Task();
                next.variables = Arrays.copyOf(variableMap, variableMap.length);
                next.block = program.basicBlockAt(successor);
                Map<Variable, Variable> catchVariableMap = tryCatchVariableMap.get(next.block);
                if (catchVariableMap != null) {
                    for (Map.Entry<Variable, Variable> entry : catchVariableMap.entrySet()) {
                        next.variables[entry.getKey().getIndex()] = entry.getValue();
                    }
                }
                stack[head++] = next;
            }
            successors = cfg.outgoingEdges(index);
            for (int successor : successors) {
                int[] phiIndexes = phiIndexMap[successor];
                List<Phi> phis = synthesizedPhis.get(successor);
                Map<Variable, Variable> catchVariableMap = tryCatchVariableMap.get(program.basicBlockAt(successor));
                for (int j = 0; j < phis.size(); ++j) {
                    Phi phi = phis.get(j);
                    Variable var = null;
                    if (catchVariableMap != null) {
                        var = catchVariableMap.get(program.variableAt(phiIndexes[j]));
                    }
                    if (var == null) {
                        var = variableMap[phiIndexes[j]];
                    }
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

        for (int i = 0; i < program.basicBlockCount(); ++i) {
            for (Phi phi : synthesizedPhis.get(i)) {
                if (!phi.getIncomings().isEmpty()) {
                    program.basicBlockAt(i).getPhis().add(phi);
                }
            }
            for (TryCatchJoint joint : synthesizedJoints.get(i)) {
                if (!joint.getSourceVariables().isEmpty()) {
                    program.basicBlockAt(i).getTryCatchJoints().add(joint);
                }
            }
        }
    }

    private void markAssignment(Variable var) {
        boolean fromHandler = currentBlock.getTryCatchBlocks().stream().anyMatch(
                tryCatch -> tryCatch.getExceptionVariable() == var);

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
                if (!fromHandler && isExceptionHandler(block, frontierBlock)) {
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

            if (!fromHandler) {
                for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
                    if (tryCatch.getExceptionVariable() == var || !hasReassignmentTo(block, var)) {
                        continue;
                    }
                    BasicBlock frontierBlock = tryCatch.getHandler();
                    int frontier = frontierBlock.getIndex();
                    boolean exists = frontierBlock.getTryCatchJoints().stream()
                            .anyMatch(joint -> joint.getSourceVariables().contains(var) && joint.getSource() == block);
                    if (exists) {
                        continue;
                    }

                    Map<Variable, TryCatchJoint> jointSubmap = jointMap.get(frontier).get(block);
                    if (jointSubmap == null) {
                        jointSubmap = new HashMap<>();
                        jointMap.get(frontier).put(block, jointSubmap);
                    }
                    TryCatchJoint joint = jointSubmap.get(var);
                    if (joint == null) {
                        joint = new TryCatchJoint();
                        joint.setSource(block);
                        joint.setReceiver(var);
                        synthesizedJoints.get(frontier).add(joint);
                        jointSubmap.put(var, joint);
                        worklist[head++] = frontierBlock;
                    }
                }
            }
        }
    }

    private boolean hasReassignmentTo(BasicBlock block, Variable var) {
        DefinitionExtractor defExtractor = new DefinitionExtractor();
        for (int i = 0; i < block.instructionCount(); ++i) {
            block.getInstructions().get(i).acceptVisitor(defExtractor);
            for (Variable definedVar : defExtractor.getDefinedVariables()) {
                if (definedVar == var) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isExceptionHandler(BasicBlock source, BasicBlock target) {
        return source.getTryCatchBlocks().stream().anyMatch(tryCatch -> tryCatch.getHandler() == target);
    }

    private Variable define(Variable var) {
        Variable old = variableMap[var.getIndex()];
        Variable original = var;
        var = introduce(var);
        propagateToTryCatch(original, var, old);
        variableMap[original.getIndex()] = var;
        return var;
    }

    private void propagateToTryCatch(Variable original, Variable var, Variable old) {
        for (TryCatchBlock tryCatch : currentBlock.getTryCatchBlocks()) {
            if (tryCatch.getExceptionVariable() == original) {
                continue;
            }
            Map<Variable, TryCatchJoint> joints = jointMap.get(tryCatch.getHandler().getIndex()).get(currentBlock);
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
