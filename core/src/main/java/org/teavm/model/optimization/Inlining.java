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

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.teavm.dependency.DependencyInfo;
import org.teavm.model.BasicBlock;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.ProgramReader;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.VariableReader;
import org.teavm.model.analysis.ClassInference;
import org.teavm.model.instructions.AbstractInstructionReader;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.InitClassInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.JumpInstruction;
import org.teavm.model.util.BasicBlockMapper;
import org.teavm.model.util.InstructionVariableMapper;
import org.teavm.model.util.ProgramUtils;
import org.teavm.model.util.TransitionExtractor;

public class Inlining {
    private IntArrayList depthsByBlock;
    private Set<Instruction> instructionsToSkip;
    private ClassHierarchy hierarchy;
    private ListableClassReaderSource classes;
    private DependencyInfo dependencyInfo;
    private InliningStrategy strategy;
    private MethodUsageCounter usageCounter;
    private Set<MethodReference> methodsUsedOnce = new HashSet<>();

    public Inlining(ClassHierarchy hierarchy, DependencyInfo dependencyInfo, InliningStrategy strategy,
            ListableClassReaderSource classes, Predicate<MethodReference> externalMethods) {
        this.hierarchy = hierarchy;
        this.classes = classes;
        this.dependencyInfo = dependencyInfo;
        this.strategy = strategy;
        usageCounter = new MethodUsageCounter(externalMethods);

        for (String className : classes.getClassNames()) {
            ClassReader cls = classes.get(className);
            for (MethodReader method : cls.getMethods()) {
                ProgramReader program = method.getProgram();
                if (program != null) {
                    usageCounter.currentMethod = method.getReference();
                    for (BasicBlockReader block : program.getBasicBlocks()) {
                        block.readAllInstructions(usageCounter);
                    }
                }
            }
        }

        for (ObjectCursor<MethodReference> cursor : usageCounter.methodUsageCount.keys()) {
            if (usageCounter.methodUsageCount.get(cursor.value) == 1) {
                methodsUsedOnce.add(cursor.value);
            }
        }
    }

    public List<MethodReference> getOrder() {
        List<MethodReference> order = new ArrayList<>();
        Set<MethodReference> visited = new HashSet<>();
        for (String className : classes.getClassNames()) {
            ClassReader cls = classes.get(className);
            for (MethodReader method : cls.getMethods()) {
                if (method.getProgram() != null) {
                    computeOrder(method.getReference(), order, visited);
                }
            }
        }
        Collections.reverse(order);
        return order;
    }

    private void computeOrder(MethodReference method, List<MethodReference> order, Set<MethodReference> visited) {
        if (!visited.add(method)) {
            return;
        }
        Set<MethodReference> invokedMethods = usageCounter.methodDependencies.get(method);
        if (invokedMethods != null) {
            for (MethodReference invokedMethod : invokedMethods) {
                computeOrder(invokedMethod, order, visited);
            }
        }
        order.add(method);
    }

    public boolean hasUsages(MethodReference method) {
        return usageCounter.methodUsageCount.getOrDefault(method, -1) != 0;
    }

    public void apply(Program program, MethodReference method) {
        depthsByBlock = new IntArrayList(program.basicBlockCount());
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            depthsByBlock.add(0);
        }
        instructionsToSkip = new HashSet<>();

        while (applyOnce(program, method)) {
            devirtualize(program, method, dependencyInfo);
        }
        depthsByBlock = null;
        instructionsToSkip = null;

        new UnreachableBasicBlockEliminator().optimize(program);
    }

    private boolean applyOnce(Program program, MethodReference method) {
        InliningStep step = strategy.start(method, program);
        if (step == null) {
            return false;
        }
        List<PlanEntry> plan = buildPlan(program, -1, step);
        if (plan.isEmpty()) {
            return false;
        }
        execPlan(program, plan, 0);
        return true;
    }

    private void execPlan(Program program, List<PlanEntry> plan, int offset) {
        for (PlanEntry entry : plan) {
            execPlanEntry(program, entry, offset);
        }
    }

    private void execPlanEntry(Program program, PlanEntry planEntry, int offset) {
        int usageCount = usageCounter.methodUsageCount.getOrDefault(planEntry.method, -1);
        if (usageCount > 0) {
            usageCounter.methodUsageCount.put(planEntry.method, usageCount - 1);
        }

        BasicBlock block = program.basicBlockAt(planEntry.targetBlock + offset);
        InvokeInstruction invoke = (InvokeInstruction) planEntry.targetInstruction;
        BasicBlock splitBlock = program.createBasicBlock();
        BasicBlock firstInlineBlock = program.createBasicBlock();
        Program inlineProgram = planEntry.program;
        for (int i = 1; i < inlineProgram.basicBlockCount(); ++i) {
            program.createBasicBlock();
        }
        while (depthsByBlock.size() < program.basicBlockCount()) {
            depthsByBlock.add(planEntry.depth + 1);
        }

        int variableOffset = program.variableCount();
        for (int i = 0; i < inlineProgram.variableCount(); ++i) {
            program.createVariable();
        }

        while (planEntry.targetInstruction.getNext() != null) {
            Instruction insn = planEntry.targetInstruction.getNext();
            insn.delete();
            splitBlock.add(insn);
        }
        splitBlock.getTryCatchBlocks().addAll(ProgramUtils.copyTryCatches(block, program));

        invoke.delete();
        if (invoke.getInstance() == null || invoke.getMethod().getName().equals("<init>")) {
            InitClassInstruction clinit = new InitClassInstruction();
            clinit.setClassName(invoke.getMethod().getClassName());
            block.add(clinit);
        }
        JumpInstruction jumpToInlinedProgram = new JumpInstruction();
        jumpToInlinedProgram.setTarget(firstInlineBlock);
        block.add(jumpToInlinedProgram);

        for (int i = 0; i < inlineProgram.basicBlockCount(); ++i) {
            BasicBlock blockToInline = inlineProgram.basicBlockAt(i);
            BasicBlock inlineBlock = program.basicBlockAt(firstInlineBlock.getIndex() + i);
            while (blockToInline.getFirstInstruction() != null) {
                Instruction insn = blockToInline.getFirstInstruction();
                insn.delete();
                inlineBlock.add(insn);
            }

            List<Phi> phis = new ArrayList<>(blockToInline.getPhis());
            blockToInline.getPhis().clear();
            inlineBlock.getPhis().addAll(phis);

            List<TryCatchBlock> tryCatches = new ArrayList<>(blockToInline.getTryCatchBlocks());
            blockToInline.getTryCatchBlocks().clear();
            inlineBlock.getTryCatchBlocks().addAll(tryCatches);

            inlineBlock.setExceptionVariable(blockToInline.getExceptionVariable());
        }

        BasicBlockMapper blockMapper = new BasicBlockMapper((BasicBlock b) ->
                program.basicBlockAt(b.getIndex() + firstInlineBlock.getIndex()));
        InstructionVariableMapper variableMapper = new InstructionVariableMapper(var -> {
            if (var.getIndex() == 0) {
                return invoke.getInstance();
            } else if (var.getIndex() <= invoke.getArguments().size()) {
                return invoke.getArguments().get(var.getIndex() - 1);
            } else {
                return program.variableAt(var.getIndex() + variableOffset);
            }
        });

        List<Incoming> resultVariables = new ArrayList<>();
        for (int i = 0; i < inlineProgram.basicBlockCount(); ++i) {
            BasicBlock mappedBlock = program.basicBlockAt(firstInlineBlock.getIndex() + i);
            blockMapper.transform(mappedBlock);
            variableMapper.apply(mappedBlock);
            mappedBlock.getTryCatchBlocks().addAll(ProgramUtils.copyTryCatches(block, program));
            Instruction lastInsn = mappedBlock.getLastInstruction();
            if (lastInsn instanceof ExitInstruction) {
                ExitInstruction exit = (ExitInstruction) lastInsn;
                JumpInstruction exitReplacement = new JumpInstruction();
                exitReplacement.setTarget(splitBlock);
                exitReplacement.setLocation(exit.getLocation());
                exit.replace(exitReplacement);
                if (exit.getValueToReturn() != null) {
                    Incoming resultIncoming = new Incoming();
                    resultIncoming.setSource(mappedBlock);
                    resultIncoming.setValue(exit.getValueToReturn());
                    resultVariables.add(resultIncoming);
                }
            }
        }

        if (!resultVariables.isEmpty() && invoke.getReceiver() != null) {
            if (resultVariables.size() == 1) {
                AssignInstruction resultAssignment = new AssignInstruction();
                resultAssignment.setReceiver(invoke.getReceiver());
                resultAssignment.setAssignee(resultVariables.get(0).getValue());
                splitBlock.addFirst(resultAssignment);
            } else {
                Phi resultPhi = new Phi();
                resultPhi.setReceiver(invoke.getReceiver());
                resultPhi.getIncomings().addAll(resultVariables);
                splitBlock.getPhis().add(resultPhi);
            }
        }

        TransitionExtractor transitionExtractor = new TransitionExtractor();
        Instruction splitLastInsn = splitBlock.getLastInstruction();
        if (splitLastInsn != null) {
            splitLastInsn.acceptVisitor(transitionExtractor);
            if (transitionExtractor.getTargets() != null) {
                List<Incoming> incomings = Arrays.stream(transitionExtractor.getTargets())
                        .flatMap(bb -> bb.getPhis().stream())
                        .flatMap(phi -> phi.getIncomings().stream())
                        .filter(incoming -> incoming.getSource() == block)
                        .collect(Collectors.toList());
                for (Incoming incoming : incomings) {
                    incoming.setSource(splitBlock);
                }
            }
        }

        execPlan(program, planEntry.innerPlan, firstInlineBlock.getIndex());
    }

    private List<PlanEntry> buildPlan(Program program, int depth, InliningStep step) {
        List<PlanEntry> plan = new ArrayList<>();
        int originalDepth = depth;

        ContextImpl context = new ContextImpl();
        for (BasicBlock block : program.getBasicBlocks()) {
            if (!block.getTryCatchBlocks().isEmpty()) {
                continue;
            }

            if (originalDepth < 0) {
                depth = depthsByBlock.get(block.getIndex());
            }

            for (Instruction insn : block) {
                if (instructionsToSkip.contains(insn)) {
                    continue;
                }

                if (!(insn instanceof InvokeInstruction)) {
                    continue;
                }
                InvokeInstruction invoke = (InvokeInstruction) insn;
                if (invoke.getType() == InvocationType.VIRTUAL) {
                    continue;
                }

                MethodReader invokedMethod = getMethod(invoke.getMethod());
                if (invokedMethod == null || invokedMethod.getProgram() == null
                        || invokedMethod.getProgram().basicBlockCount() == 0
                        || invokedMethod.hasModifier(ElementModifier.SYNCHRONIZED)) {
                    instructionsToSkip.add(insn);
                    continue;
                }

                context.depth = depth;
                InliningStep innerStep = step.tryInline(invokedMethod.getReference(), invokedMethod.getProgram(),
                        context);
                if (innerStep == null) {
                    instructionsToSkip.add(insn);
                    continue;
                }
                Program invokedProgram = ProgramUtils.copy(invokedMethod.getProgram());

                PlanEntry entry = new PlanEntry();
                entry.targetBlock = block.getIndex();
                entry.targetInstruction = insn;
                entry.program = invokedProgram;
                entry.innerPlan.addAll(buildPlan(invokedProgram, depth + 1, innerStep));
                entry.depth = depth;
                entry.method = invokedMethod.getReference();
                plan.add(entry);
            }
        }
        Collections.reverse(plan);

        return plan;
    }

    private MethodReader getMethod(MethodReference methodRef) {
        ClassReader cls = classes.get(methodRef.getClassName());
        return cls != null ? cls.getMethod(methodRef.getDescriptor()) : null;
    }

    private void devirtualize(Program program, MethodReference method, DependencyInfo dependencyInfo) {
        ClassInference inference = new ClassInference(dependencyInfo, hierarchy);
        inference.infer(program, method);

        for (BasicBlock block : program.getBasicBlocks()) {
            for (Instruction instruction : block) {
                if (!(instruction instanceof InvokeInstruction)) {
                    continue;
                }
                InvokeInstruction invoke = (InvokeInstruction) instruction;
                if (invoke.getType() != InvocationType.VIRTUAL) {
                    continue;
                }

                Set<MethodReference> implementations = new HashSet<>();
                for (String className : inference.classesOf(invoke.getInstance().getIndex())) {
                    MethodReference rawMethod = new MethodReference(className, invoke.getMethod().getDescriptor());
                    MethodReader resolvedMethod = dependencyInfo.getClassSource().resolveImplementation(rawMethod);
                    if (resolvedMethod != null) {
                        implementations.add(resolvedMethod.getReference());
                    }
                }

                if (implementations.size() == 1) {
                    invoke.setType(InvocationType.SPECIAL);
                    invoke.setMethod(implementations.iterator().next());
                }
            }
        }
    }

    private class PlanEntry {
        int targetBlock;
        Instruction targetInstruction;
        MethodReference method;
        Program program;
        int depth;
        final List<PlanEntry> innerPlan = new ArrayList<>();
    }

    static class MethodUsageCounter extends AbstractInstructionReader {
        ObjectIntMap<MethodReference> methodUsageCount = new ObjectIntHashMap<>();
        Map<MethodReference, Set<MethodReference>> methodDependencies = new LinkedHashMap<>();
        Predicate<MethodReference> externalMethods;
        MethodReference currentMethod;

        MethodUsageCounter(Predicate<MethodReference> externalMethods) {
            this.externalMethods = externalMethods;
        }

        @Override
        public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments, InvocationType type) {
            if (type == InvocationType.SPECIAL && !externalMethods.test(method)) {
                methodUsageCount.put(method, methodUsageCount.get(method) + 1);
                methodDependencies.computeIfAbsent(currentMethod, k -> new LinkedHashSet<>()).add(method);
            }
        }
    }

    class ContextImpl implements InliningContext {
        int depth;

        @Override
        public boolean isUsedOnce(MethodReference method) {
            return methodsUsedOnce.contains(method);
        }

        @Override
        public int getDepth() {
            return depth;
        }
    }
}
