/*
 *  Copyright 2017 Alexey Andreev.
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
package org.teavm.model.analysis;

import com.carrotsearch.hppc.IntObjectMap;
import com.carrotsearch.hppc.IntObjectOpenHashMap;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.IntSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.dependency.DependencyInfo;
import org.teavm.dependency.FieldDependencyInfo;
import org.teavm.dependency.MethodDependencyInfo;
import org.teavm.dependency.ValueDependencyInfo;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.Incoming;
import org.teavm.model.Instruction;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlock;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.ExitInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.PutElementInstruction;
import org.teavm.model.instructions.PutFieldInstruction;
import org.teavm.model.instructions.RaiseInstruction;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;

public class ClassInference {
    private DependencyInfo dependencyInfo;
    private Graph assignmentGraph;
    private Graph cloneGraph;
    private Graph arrayGraph;
    private Graph itemGraph;
    private List<IntObjectMap<ValueType>> casts;
    private IntObjectMap<IntSet> exceptionMap;
    private VirtualCallSite[][] virtualCallSites;
    private List<Task> initialTasks;
    private List<List<Set<String>>> types;
    private List<Set<String>> finalTypes;

    public ClassInference(DependencyInfo dependencyInfo) {
        this.dependencyInfo = dependencyInfo;
    }

    public void infer(Program program, MethodReference methodReference) {
        MethodDependencyInfo thisMethodDep = dependencyInfo.getMethod(methodReference);
        buildGraphs(program, thisMethodDep);

        for (int i = 0; i <= methodReference.parameterCount(); ++i) {
            ValueDependencyInfo paramDep = thisMethodDep.getVariable(i);
            if (paramDep != null) {
                int degree = 0;
                while (true) {
                    for (String paramType : paramDep.getTypes()) {
                        initialTasks.add(new Task(i, degree, paramType));
                    }
                    if (!paramDep.hasArrayType()) {
                        break;
                    }
                    paramDep = paramDep.getArrayItem();
                    degree++;
                }
            }
        }

        types = new ArrayList<>(program.variableCount());
        for (int i = 0; i < program.variableCount(); ++i) {
            List<Set<String>> variableTypes = new ArrayList<>();
            types.add(variableTypes);
            for (int j = 0; j < 3; ++j) {
                variableTypes.add(new LinkedHashSet<>());
            }
        }

        propagate(program);
        assignmentGraph = null;
        cloneGraph = null;
        arrayGraph = null;
        itemGraph = null;
        casts = null;
        exceptionMap = null;
        virtualCallSites = null;

        finalTypes = new ArrayList<>(program.variableCount());
        for (int i = 0; i < program.variableCount(); ++i) {
            finalTypes.add(types.get(i).get(0));
        }

        types = null;
    }

    public String[] classesOf(int variableIndex) {
        return finalTypes.get(variableIndex).toArray(new String[0]);
    }

    private void buildGraphs(Program program, MethodDependencyInfo thisMethodDep) {
        GraphBuildingVisitor visitor = new GraphBuildingVisitor(program.variableCount(), dependencyInfo);
        visitor.thisMethodDep = thisMethodDep;
        for (BasicBlock block : program.getBasicBlocks()) {
            visitor.currentBlock = block;
            for (Phi phi : block.getPhis()) {
                visitor.visit(phi);
            }
            for (Instruction insn : block) {
                insn.acceptVisitor(visitor);
            }
        }

        assignmentGraph = visitor.assignmentGraphBuilder.build();
        cloneGraph = visitor.cloneGraphBuilder.build();
        arrayGraph = visitor.arrayGraphBuilder.build();
        itemGraph = visitor.itemGraphBuilder.build();
        casts = visitor.casts;
        exceptionMap = visitor.exceptionMap;
        initialTasks = visitor.tasks;

        virtualCallSites = new VirtualCallSite[program.variableCount()][];
        for (int i = 0; i < virtualCallSites.length; ++i) {
            List<VirtualCallSite> buildCallSites = visitor.virtualCallSites.get(i);
            if (buildCallSites != null) {
                virtualCallSites[i] = buildCallSites.toArray(new VirtualCallSite[0]);
            }
        }
    }

    private void propagate(Program program) {
        ClassReaderSource classSource = dependencyInfo.getClassSource();

        Queue<Task> queue = new ArrayDeque<>(initialTasks);
        initialTasks = null;

        while (!queue.isEmpty()) {
            Task task = queue.remove();

            if (task.degree < 0) {
                BasicBlock block = program.basicBlockAt(task.variable);
                for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
                    if (tryCatch.getExceptionType() == null
                            || classSource.isSuperType(tryCatch.getExceptionType(), task.className).orElse(false)) {
                        Variable exception = tryCatch.getHandler().getExceptionVariable();
                        if (exception != null) {
                            queue.add(new Task(exception.getIndex(), 0, task.className));
                        }
                        break;
                    }
                }

                continue;
            }

            List<Set<String>> variableTypes = types.get(task.variable);
            if (task.degree >= variableTypes.size()) {
                continue;
            }

            Set<String> typeSet = variableTypes.get(task.degree);
            if (!typeSet.add(task.className)) {
                continue;
            }

            for (int successor : assignmentGraph.outgoingEdges(task.variable)) {
                queue.add(new Task(successor, task.degree, task.className));
                int itemDegree = task.degree + 1;
                if (itemDegree < variableTypes.size()) {
                    for (String type : variableTypes.get(itemDegree)) {
                        queue.add(new Task(successor, itemDegree, type));
                    }
                }
                List<Set<String>> successorVariableTypes = types.get(successor);
                if (itemDegree < successorVariableTypes.size()) {
                    for (String type : successorVariableTypes.get(itemDegree)) {
                        queue.add(new Task(task.variable, itemDegree, type));
                    }
                }
            }

            if (task.degree > 0) {
                for (int predecessor : assignmentGraph.incomingEdges(task.variable)) {
                    queue.add(new Task(predecessor, task.degree, task.className));
                }

                for (int successor : itemGraph.outgoingEdges(task.variable)) {
                    queue.add(new Task(successor, task.degree - 1, task.className));
                }
            } else {
                for (int successor : cloneGraph.outgoingEdges(task.variable)) {
                    queue.add(new Task(successor, 0, task.className));
                }

                IntSet blocks = exceptionMap.get(task.variable);
                if (blocks != null) {
                    for (int block : blocks.toArray()) {
                        queue.add(new Task(block, -1, task.className));
                    }
                }

                VirtualCallSite[] callSites = virtualCallSites[task.variable];
                if (callSites != null) {
                    for (VirtualCallSite callSite : callSites) {
                        MethodReference rawMethod = new MethodReference(task.className,
                                callSite.method.getDescriptor());
                        MethodReader resolvedMethod = classSource.resolveImplementation(rawMethod);
                        if (resolvedMethod == null) {
                            continue;
                        }
                        MethodReference resolvedMethodRef = resolvedMethod.getReference();
                        if (callSite.resolvedMethods.add(resolvedMethodRef)) {
                            MethodDependencyInfo methodDep = dependencyInfo.getMethod(resolvedMethodRef);
                            if (methodDep != null) {
                                if (callSite.receiver >= 0) {
                                    readValue(methodDep.getResult(), program.variableAt(callSite.receiver), queue);
                                }
                                for (int i = 0; i < callSite.arguments.length; ++i) {
                                    writeValue(methodDep.getVariable(i + 1), program.variableAt(callSite.arguments[i]),
                                            queue);
                                }
                                for (String type : methodDep.getThrown().getTypes()) {
                                    queue.add(new Task(callSite.block, -1, type));
                                }
                            }
                        }
                    }
                }
            }

            for (int successor : arrayGraph.outgoingEdges(task.variable)) {
                queue.add(new Task(successor, task.degree + 1, task.className));
            }

            IntObjectMap<ValueType> variableCasts = casts.get(task.variable);
            if (variableCasts != null) {
                ValueType type;
                if (task.className.startsWith("[")) {
                    type = ValueType.parseIfPossible(task.className);
                    if (type == null) {
                        type = ValueType.arrayOf(ValueType.object("java.lang.Object"));
                    }
                } else {
                    type = ValueType.object(task.className);
                }
                for (int target : variableCasts.keys().toArray()) {
                    ValueType targetType = variableCasts.get(target);
                    if (classSource.isSuperType(targetType, type).orElse(false)) {
                        queue.add(new Task(target, 0, task.className));
                    }
                }
            }
        }
    }

    static class GraphBuildingVisitor extends AbstractInstructionVisitor {
        DependencyInfo dependencyInfo;
        GraphBuilder assignmentGraphBuilder;
        GraphBuilder cloneGraphBuilder;
        GraphBuilder arrayGraphBuilder;
        GraphBuilder itemGraphBuilder;
        MethodDependencyInfo thisMethodDep;
        List<IntObjectMap<ValueType>> casts;
        IntObjectMap<IntSet> exceptionMap = new IntObjectOpenHashMap<>();
        List<Task> tasks = new ArrayList<>();
        List<List<VirtualCallSite>> virtualCallSites;
        BasicBlock currentBlock;

        GraphBuildingVisitor(int variableCount, DependencyInfo dependencyInfo) {
            this.dependencyInfo = dependencyInfo;
            assignmentGraphBuilder = new GraphBuilder(variableCount);
            cloneGraphBuilder = new GraphBuilder(variableCount);
            arrayGraphBuilder = new GraphBuilder(variableCount);
            itemGraphBuilder = new GraphBuilder(variableCount);
            casts = new ArrayList<>(variableCount);
            for (int i = 0; i < variableCount; ++i) {
                casts.add(new IntObjectOpenHashMap<>());
            }

            virtualCallSites = new ArrayList<>(Collections.nCopies(variableCount, null));
        }

        public void visit(Phi phi) {
            for (Incoming incoming : phi.getIncomings()) {
                assignmentGraphBuilder.addEdge(incoming.getValue().getIndex(), phi.getReceiver().getIndex());
            }
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            tasks.add(new Task(insn.getReceiver().getIndex(), 0, "java.lang.Class"));
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            tasks.add(new Task(insn.getReceiver().getIndex(), 0, "java.lang.String"));
        }

        @Override
        public void visit(AssignInstruction insn) {
            assignmentGraphBuilder.addEdge(insn.getAssignee().getIndex(), insn.getReceiver().getIndex());
        }

        @Override
        public void visit(CastInstruction insn) {
            casts.get(insn.getValue().getIndex()).put(insn.getReceiver().getIndex(), insn.getTargetType());
        }

        @Override
        public void visit(RaiseInstruction insn) {
            IntSet blockIndexes = exceptionMap.get(insn.getException().getIndex());
            if (blockIndexes == null) {
                blockIndexes = new IntOpenHashSet();
                exceptionMap.put(insn.getException().getIndex(), blockIndexes);
            }
            blockIndexes.add(currentBlock.getIndex());
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            tasks.add(new Task(insn.getReceiver().getIndex(), 0, ValueType.arrayOf(insn.getItemType()).toString()));
        }

        @Override
        public void visit(ConstructInstruction insn) {
            tasks.add(new Task(insn.getReceiver().getIndex(), 0, insn.getType()));
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
            ValueType type = insn.getItemType();
            for (int i = 0; i < insn.getDimensions().size(); ++i) {
                type = ValueType.arrayOf(type);
            }
            tasks.add(new Task(insn.getReceiver().getIndex(), 0, type.toString()));
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            FieldDependencyInfo fieldDep = dependencyInfo.getField(insn.getField());
            ValueDependencyInfo valueDep = fieldDep.getValue();
            readValue(valueDep, insn.getReceiver(), tasks);
        }

        @Override
        public void visit(PutFieldInstruction insn) {
            FieldDependencyInfo fieldDep = dependencyInfo.getField(insn.getField());
            ValueDependencyInfo valueDep = fieldDep.getValue();
            writeValue(valueDep, insn.getValue(), tasks);
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
            cloneGraphBuilder.addEdge(insn.getArray().getIndex(), insn.getReceiver().getIndex());
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
            assignmentGraphBuilder.addEdge(insn.getArray().getIndex(), insn.getReceiver().getIndex());
        }

        @Override
        public void visit(GetElementInstruction insn) {
            itemGraphBuilder.addEdge(insn.getArray().getIndex(), insn.getReceiver().getIndex());
        }

        @Override
        public void visit(PutElementInstruction insn) {
            arrayGraphBuilder.addEdge(insn.getValue().getIndex(), insn.getArray().getIndex());
        }

        @Override
        public void visit(ExitInstruction insn) {
            if (insn.getValueToReturn() != null) {
                ValueDependencyInfo resultDependency = thisMethodDep.getResult();
                int resultDegree = 0;
                while (resultDependency.hasArrayType()) {
                    resultDependency = resultDependency.getArrayItem();
                    for (String paramType : resultDependency.getTypes()) {
                        tasks.add(new Task(insn.getValueToReturn().getIndex(), ++resultDegree, paramType));
                    }
                }
            }
        }

        @Override
        public void visit(InvokeInstruction insn) {
            if (insn.getType() == InvocationType.VIRTUAL) {
                int instance = insn.getInstance().getIndex();
                List<VirtualCallSite> callSites = virtualCallSites.get(instance);
                if (callSites == null) {
                    callSites = new ArrayList<>();
                    virtualCallSites.set(instance, callSites);
                }

                VirtualCallSite callSite = new VirtualCallSite();
                callSite.method = insn.getMethod();
                callSite.arguments = new int[insn.getArguments().size()];
                for (int i = 0; i < insn.getArguments().size(); ++i) {
                    callSite.arguments[i] = insn.getArguments().get(i).getIndex();
                }
                callSite.receiver = insn.getReceiver() != null ? insn.getReceiver().getIndex() : -1;
                callSite.block = currentBlock.getIndex();
                callSites.add(callSite);

                return;
            }

            MethodDependencyInfo methodDep = dependencyInfo.getMethod(insn.getMethod());
            if (methodDep != null) {
                if (insn.getReceiver() != null) {
                    readValue(methodDep.getResult(), insn.getReceiver(), tasks);
                }

                for (int i = 0; i < insn.getArguments().size(); ++i) {
                    writeValue(methodDep.getVariable(i + 1), insn.getArguments().get(i), tasks);
                }

                for (String type : methodDep.getThrown().getTypes()) {
                    tasks.add(new Task(currentBlock.getIndex(), -1, type));
                }
            }
        }
    }

    private static void readValue(ValueDependencyInfo valueDep, Variable receiver, Collection<Task> tasks) {
        int depth = 0;
        boolean hasArrayType;
        do {
            for (String type : valueDep.getTypes()) {
                tasks.add(new Task(receiver.getIndex(), depth, type));
            }
            depth++;
            hasArrayType = valueDep.hasArrayType();
            valueDep = valueDep.getArrayItem();
        } while (hasArrayType);
    }

    private static void writeValue(ValueDependencyInfo valueDep, Variable source, Collection<Task> tasks) {
        int depth = 0;
        while (valueDep.hasArrayType()) {
            depth++;
            valueDep = valueDep.getArrayItem();
            for (String type : valueDep.getTypes()) {
                tasks.add(new Task(source.getIndex(), depth, type));
            }
        }
    }

    static class Task {
        int variable;
        int degree;
        String className;

        Task(int variable, int degree, String className) {
            this.variable = variable;
            this.degree = degree;
            this.className = className;
        }
    }

    static class VirtualCallSite {
        Set<MethodReference> resolvedMethods = new HashSet<>();
        MethodReference method;
        int[] arguments;
        int receiver;
        int block;
    }
}
