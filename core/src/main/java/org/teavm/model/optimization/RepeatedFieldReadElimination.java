/*
 *  Copyright 2019 Alexey Andreev.
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

import com.carrotsearch.hppc.IntArrayDeque;
import com.carrotsearch.hppc.IntDeque;
import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.IntObjectMap;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import com.carrotsearch.hppc.cursors.IntObjectCursor;
import com.carrotsearch.hppc.cursors.ObjectIntCursor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.teavm.common.DominatorTree;
import org.teavm.common.Graph;
import org.teavm.common.GraphUtils;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReader;
import org.teavm.model.FieldReference;
import org.teavm.model.Instruction;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.analysis.AliasAnalysis;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.util.ProgramUtils;

public class RepeatedFieldReadElimination implements MethodOptimization {
    private static final int ENTER = 0;
    private static final int EXIT = 1;
    private ClassReaderSource classSource;
    private boolean[] everythingInvalid;
    private List<IntObjectHashMap<Set<FieldReference>>> invalidFields = new ArrayList<>();
    private AliasAnalysis aliasAnalysis;
    private boolean changed;

    @Override
    public boolean optimize(MethodOptimizationContext context, Program program) {
        classSource = context.getClassSource();
        Graph cfg = ProgramUtils.buildControlFlowGraph(program);
        DominatorTree dom = GraphUtils.buildDominatorTree(cfg);
        aliasAnalysis = new AliasAnalysis();
        aliasAnalysis.analyze(program, context.getMethod().getDescriptor());

        insertInvalidationPoints(cfg, dom, program);
        new Traversal(program, dom, cfg).perform();

        return changed;
    }

    class Traversal {
        Program program;
        IntDeque worklist = new IntArrayDeque();
        IntObjectMap<ObjectIntMap<FieldReference>> cacheVars = new IntObjectHashMap<>();
        Deque<State> stateStack = new ArrayDeque<>();
        Graph domGraph;
        InstructionAnalyzer instructionAnalyzer = new InstructionAnalyzer();

        Traversal(Program program, DominatorTree dom, Graph cfg) {
            this.program = program;
            worklist.addLast(0);
            worklist.addLast(ENTER);
            domGraph = GraphUtils.buildDominatorGraph(dom, cfg.size());
        }

        void perform() {
            while (!worklist.isEmpty()) {
                int operation = worklist.removeLast();
                int blockIndex = worklist.removeLast();
                BasicBlock block = program.basicBlockAt(blockIndex);
                switch (operation) {
                    case ENTER:
                        enterBlock(block);
                        break;
                    case EXIT:
                        exitBlock();
                        break;
                }
            }
        }

        private void enterBlock(BasicBlock block) {
            stateStack.addLast(new State());
            invalidatePreparedFields(block);

            for (Instruction instruction : block) {
                if (instruction instanceof GetFieldInstruction) {
                    handleGetField((GetFieldInstruction) instruction);
                } else {
                    instructionAnalyzer.reset();
                    instruction.acceptVisitor(instructionAnalyzer);
                    if (instructionAnalyzer.invalidatesAll) {
                        invalidateAllFields();
                    } else if (instructionAnalyzer.invalidatedField != null) {
                        FieldReference field = instructionAnalyzer.invalidatedField;
                        int instance = instructionAnalyzer.instance;
                        if (!isVolatile(field)) {
                            invalidateField(instance, field);
                            storeIntoCache(instance, field, instructionAnalyzer.newValue);
                        }
                    }
                }
            }

            worklist.addLast(block.getIndex());
            worklist.addLast(EXIT);
            for (int successor : domGraph.outgoingEdges(block.getIndex())) {
                worklist.addLast(successor);
                worklist.addLast(ENTER);
            }
        }

        private void invalidatePreparedFields(BasicBlock block) {
            if (everythingInvalid[block.getIndex()]) {
                invalidateAllFields();
                return;
            }

            IntObjectHashMap<Set<FieldReference>> invalidFieldsByBlock = invalidFields.get(block.getIndex());
            if (invalidFieldsByBlock != null) {
                for (IntObjectCursor<Set<FieldReference>> cursor : invalidFieldsByBlock) {
                    int instance = cursor.key;
                    for (FieldReference field : cursor.value) {
                        invalidateField(instance, field);
                    }
                }
            }
        }

        private void handleGetField(GetFieldInstruction instruction) {
            FieldReference field = instruction.getField();
            if (isVolatile(field)) {
                return;
            }

            int instanceIndex = instruction.getInstance() != null ? instruction.getInstance().getIndex() : -1;
            ObjectIntMap<FieldReference> cacheVarsByInstance = cacheVars.get(instanceIndex);
            if (cacheVarsByInstance == null) {
                cacheVarsByInstance = new ObjectIntHashMap<>();
                cacheVars.put(instanceIndex, cacheVarsByInstance);
            }

            int cachedVar = cacheVarsByInstance.getOrDefault(field, -1);
            if (cachedVar >= 0) {
                AssignInstruction assign = new AssignInstruction();
                assign.setReceiver(instruction.getReceiver());
                assign.setAssignee(program.variableAt(cachedVar));
                assign.setLocation(instruction.getLocation());
                instruction.replace(assign);
                changed = true;
            } else {
                cachedVar = instruction.getReceiver().getIndex();
                cacheVarsByInstance.put(field, cachedVar);
                markFieldAsAdded(instanceIndex, field);
            }
        }

        private void storeIntoCache(int instance, FieldReference field, int value) {
            ObjectIntMap<FieldReference> cacheVarsByInstance = cacheVars.get(instance);
            if (cacheVarsByInstance == null) {
                cacheVarsByInstance = new ObjectIntHashMap<>();
                cacheVars.put(instance, cacheVarsByInstance);
            }
            cacheVarsByInstance.put(field, value);
            markFieldAsAdded(instance, field);
        }

        private void markFieldAsAdded(int instance, FieldReference field) {
            State state = currentState();
            ObjectIntMap<FieldReference> removedFieldsByInstance = state.removedCacheFields.get(instance);
            if (removedFieldsByInstance == null || !removedFieldsByInstance.containsKey(field)) {
                Set<FieldReference> fields = state.addedCacheFields.get(instance);
                if (fields == null) {
                    fields = new HashSet<>();
                    state.addedCacheFields.put(instance, fields);
                }
                fields.add(field);
            }
        }

        private void invalidateAllFields() {
            State state = currentState();

            for (IntObjectCursor<ObjectIntMap<FieldReference>> instanceCursor : cacheVars) {
                int instance = instanceCursor.key;
                ObjectIntMap<FieldReference> cacheVarsByInstance = instanceCursor.value;
                for (ObjectIntCursor<FieldReference> fieldCursor : cacheVarsByInstance) {
                    FieldReference field = fieldCursor.key;
                    int value = fieldCursor.value;
                    markFieldAsRemoved(state, instance, field, value);
                }
            }

            cacheVars.clear();
        }

        private void invalidateField(int instance, FieldReference field) {
            if (instance == -1) {
                invalidateSingleField(instance, field);
                return;
            }

            if (aliasAnalysis.affectsEverything(instance)) {
                invalidateFieldOnAllInstances(field);
            } else {
                for (int affectedVar : aliasAnalysis.affectedVariables(instance)) {
                    invalidateSingleField(affectedVar, field);
                }
                for (int affectedVar : aliasAnalysis.getExternalObjects()) {
                    invalidateSingleField(affectedVar, field);
                }
            }
        }

        private void invalidateSingleField(int instance, FieldReference field) {
            ObjectIntMap<FieldReference> cacheVarsByInstance = cacheVars.get(instance);
            if (cacheVarsByInstance == null || !cacheVarsByInstance.containsKey(field)) {
                return;
            }
            int value = cacheVarsByInstance.remove(field);
            State state = currentState();
            markFieldAsRemoved(state, instance, field, value);
        }

        private void invalidateFieldOnAllInstances(FieldReference field) {
            for (IntObjectCursor<ObjectIntMap<FieldReference>> instanceCursor : cacheVars) {
                int instance = instanceCursor.key;
                int value = instanceCursor.value.getOrDefault(field, -1);
                if (value >= 0) {
                    instanceCursor.value.remove(field);
                    State state = currentState();
                    markFieldAsRemoved(state, instance, field, value);
                }
            }
        }

        private void markFieldAsRemoved(State state, int instance, FieldReference field, int value) {
            Set<FieldReference> addedFieldsByInstance = state.addedCacheFields.get(instance);
            if (addedFieldsByInstance == null || !addedFieldsByInstance.contains(field)) {
                ObjectIntMap<FieldReference> removedFieldsByInstance = state.removedCacheFields.get(instance);
                if (removedFieldsByInstance == null) {
                    removedFieldsByInstance = new ObjectIntHashMap<>();
                    state.removedCacheFields.put(instance, removedFieldsByInstance);
                }
                if (!removedFieldsByInstance.containsKey(field)) {
                    removedFieldsByInstance.put(field, value);
                }
            }
        }

        private void exitBlock() {
            State state = stateStack.removeLast();

            for (IntObjectCursor<Set<FieldReference>> instanceCursor : state.addedCacheFields) {
                int instance = instanceCursor.key;
                ObjectIntMap<FieldReference> cachedFieldsByInstances = cacheVars.get(instance);
                if (cachedFieldsByInstances != null) {
                    for (FieldReference field : instanceCursor.value) {
                        cachedFieldsByInstances.remove(field);
                    }
                    if (cachedFieldsByInstances.isEmpty()) {
                        cacheVars.remove(instance);
                    }
                }
            }

            for (IntObjectCursor<ObjectIntMap<FieldReference>> instanceCursor : state.removedCacheFields) {
                int instance = instanceCursor.key;
                ObjectIntMap<FieldReference> cacheVarsByInstance = cacheVars.get(instance);
                if (cacheVarsByInstance == null) {
                    cacheVarsByInstance = new ObjectIntHashMap<>();
                    cacheVars.put(instance, cacheVarsByInstance);
                }
                for (ObjectIntCursor<FieldReference> fieldCursor : instanceCursor.value) {
                    FieldReference field = fieldCursor.key;
                    int value = fieldCursor.value;
                    cacheVarsByInstance.put(field, value);
                }
            }
        }

        private State currentState() {
            return stateStack.getLast();
        }
    }

    static class State {
        IntObjectMap<ObjectIntMap<FieldReference>> removedCacheFields = new IntObjectHashMap<>();
        IntObjectMap<Set<FieldReference>> addedCacheFields = new IntObjectHashMap<>();
    }

    private void insertInvalidationPoints(Graph cfg, DominatorTree dom, Program program) {
        int[][] domFrontiers = GraphUtils.findDominanceFrontiers(cfg, dom);
        everythingInvalid = new boolean[program.basicBlockCount()];
        invalidFields.addAll(Collections.nCopies(program.basicBlockCount(), null));
        boolean[] exceptionHandlers = new boolean[program.basicBlockCount()];
        for (BasicBlock block : program.getBasicBlocks()) {
            for (TryCatchBlock tryCatchBlock : block.getTryCatchBlocks()) {
                exceptionHandlers[tryCatchBlock.getHandler().getIndex()] = true;
            }
        }

        IntArrayDeque worklist = new IntArrayDeque();
        InstructionAnalyzer instructionAnalyzer = new InstructionAnalyzer();
        for (BasicBlock block : program.getBasicBlocks()) {
            int[] frontiers = domFrontiers[block.getIndex()];
            if (frontiers.length == 0) {
                continue;
            }

            Set<FieldAndInstance> fieldsToInvalidate = new LinkedHashSet<>();
            boolean allInvalid = false;
            if (exceptionHandlers[block.getIndex()]) {
                allInvalid = true;
            } else {
                for (Instruction instruction : block) {
                    instructionAnalyzer.reset();
                    instruction.acceptVisitor(instructionAnalyzer);

                    if (instructionAnalyzer.invalidatesAll) {
                        allInvalid = true;
                        fieldsToInvalidate.clear();
                        break;
                    } else if (instructionAnalyzer.invalidatedField != null
                            && !isVolatile(instructionAnalyzer.invalidatedField)) {
                        fieldsToInvalidate.add(new FieldAndInstance(instructionAnalyzer.invalidatedField,
                                instructionAnalyzer.instance));
                    }
                }
            }

            if (allInvalid) {
                worklist.addLast(frontiers);
                while (!worklist.isEmpty()) {
                    int target = worklist.removeFirst();
                    if (!everythingInvalid[target]) {
                        everythingInvalid[target] = true;
                        invalidFields.set(target, null);
                        worklist.addLast(domFrontiers[target]);
                    }
                }
            } else {
                for (FieldAndInstance fieldAndInstance : fieldsToInvalidate) {
                    worklist.addLast(frontiers);

                    int instance = fieldAndInstance.instance;
                    FieldReference field = fieldAndInstance.field;

                    while (!worklist.isEmpty()) {
                        int target = worklist.removeFirst();
                        if (everythingInvalid[target]) {
                            continue;
                        }

                        IntObjectHashMap<Set<FieldReference>> invalidFieldsByBlock = invalidFields.get(target);
                        if (invalidFieldsByBlock == null) {
                            invalidFieldsByBlock = new IntObjectHashMap<>();
                            invalidFields.set(target, invalidFieldsByBlock);
                        }

                        Set<FieldReference> invalidFieldsByVar = invalidFieldsByBlock.get(instance);
                        if (invalidFieldsByVar == null) {
                            invalidFieldsByVar = new HashSet<>();
                            invalidFieldsByBlock.put(instance, invalidFieldsByVar);
                        }
                        if (invalidFieldsByVar.add(field)) {
                            worklist.addLast(domFrontiers[target]);
                        }
                    }
                }
            }
        }
    }

    private boolean isVolatile(FieldReference fieldRef) {
        FieldReader field = classSource.resolve(fieldRef);
        return field == null || field.hasModifier(ElementModifier.VOLATILE);
    }

    static class InstructionAnalyzer extends AbstractInstructionVisitor {
        boolean invalidatesAll;
        FieldReference invalidatedField;
        int instance;
        int newValue;

        void reset() {
            invalidatesAll = false;
            invalidatedField = null;
            instance = -1;
        }

        @Override
        public void visit(PutFieldInstruction insn) {
            invalidatedField = insn.getField();
            instance = insn.getInstance() != null ? insn.getInstance().getIndex() : -1;
            newValue = insn.getValue().getIndex();
        }

        @Override
        public void visit(InvokeInstruction insn) {
            invalidatesAll = true;
        }
    }

    static class FieldAndInstance {
        final FieldReference field;
        final int instance;

        FieldAndInstance(FieldReference field, int instance) {
            this.field = field;
            this.instance = instance;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FieldAndInstance)) {
                return false;
            }
            FieldAndInstance that = (FieldAndInstance) o;
            return instance == that.instance && field.equals(that.field);
        }

        @Override
        public int hashCode() {
            return Objects.hash(field, instance);
        }
    }
}
