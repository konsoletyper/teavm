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

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.IntStack;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import com.carrotsearch.hppc.cursors.IntCursor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.common.GraphUtils;
import org.teavm.common.IntegerArray;
import org.teavm.dependency.DependencyInfo;
import org.teavm.dependency.FieldDependencyInfo;
import org.teavm.dependency.MethodDependencyInfo;
import org.teavm.dependency.ValueDependencyInfo;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHierarchy;
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
    private ClassHierarchy hierarchy;
    private Graph assignmentGraph;
    private Graph cloneGraph;
    private Graph arrayGraph;
    private Graph itemGraph;
    private Graph graph;
    private ValueCast[] casts;
    private int[] exceptions;
    private VirtualCallSite[] virtualCallSites;

    private int[] propagationPath;
    private int[] nodeMapping;

    private IntHashSet[] types;
    private ObjectIntMap<String> typeMap = new ObjectIntHashMap<>();
    private List<String> typeList = new ArrayList<>();

    private boolean changed = true;
    private boolean[] nodeChanged;
    private boolean[] formerNodeChanged;

    private static final int MAX_DEGREE = 3;

    public ClassInference(DependencyInfo dependencyInfo, ClassHierarchy hierarchy) {
        this.dependencyInfo = dependencyInfo;
        this.hierarchy = hierarchy;
    }

    public void infer(Program program, MethodReference methodReference) {
        /*
          The idea behind this algorithm
            1. Build preliminary graphs that represent different connection types between variables.
               See `assignmentGraph`, `cloneGraph`, `arrayGraph`, `itemGraph`.
            2. Build initial type sets where possible. See `types`.
            3. Build additional info: casts, virtual invocations, exceptions.
               See `casts`, `exceptions`, `virtualCallSites`.
            4. Build graph from set of preliminary paths
            5. Find strongly connected components, collapse then into one nodes.
            6. Calculate topological order of the DAG (let it be propagation path).
               Let resulting order be `propagationPath`.
            7. Propagate types along calculated path; then propagate types using additional info.
            8. Repeat 7 until it changes anything (i.e. calculate fixed point).
        */

        types = new IntHashSet[program.variableCount() << 3];
        nodeChanged = new boolean[types.length];
        formerNodeChanged = new boolean[nodeChanged.length];
        nodeMapping = new int[types.length];
        for (int i = 0; i < types.length; ++i) {
            nodeMapping[i] = i;
        }

        // See 1, 2, 3
        MethodDependencyInfo thisMethodDep = dependencyInfo.getMethod(methodReference);
        buildPreliminaryGraphs(program, thisMethodDep);

        // Augment (2) with input types of method
        for (int i = 0; i <= methodReference.parameterCount(); ++i) {
            ValueDependencyInfo paramDep = thisMethodDep.getVariable(i);
            if (paramDep != null) {
                int degree = 0;
                while (degree <= MAX_DEGREE) {
                    for (String paramType : paramDep.getTypes()) {
                        addType(i, degree, paramType);
                    }
                    if (!paramDep.hasArrayType()) {
                        break;
                    }
                    paramDep = paramDep.getArrayItem();
                    degree++;
                }
            }
        }

        // See 4
        buildPropagationGraph();

        // See 5
        collapseSCCs();

        // See 6
        buildPropagationPath();

        // See 7, 8
        propagate(program);

        // Cleanup
        assignmentGraph = null;
        graph = null;
        cloneGraph = null;
        arrayGraph = null;
        itemGraph = null;
        casts = null;
        exceptions = null;
        virtualCallSites = null;
        propagationPath = null;
        nodeChanged = null;
    }

    public String[] classesOf(int variableIndex) {
        IntHashSet typeSet = types[nodeMapping[packNodeAndDegree(variableIndex, 0)]];
        if (typeSet == null) {
            return new String[0];
        }

        int[] typeIndexes = typeSet.toArray();
        String[] types = new String[typeIndexes.length];
        for (int i = 0; i < typeIndexes.length; ++i) {
            types[i] = typeList.get(typeIndexes[i]);
        }
        return types;
    }

    private void buildPreliminaryGraphs(Program program, MethodDependencyInfo thisMethodDep) {
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

            if (block.getExceptionVariable() != null) {
                getNodeTypes(packNodeAndDegree(block.getExceptionVariable().getIndex(), 0));
            }
        }

        assignmentGraph = visitor.assignmentGraphBuilder.build();
        cloneGraph = visitor.cloneGraphBuilder.build();
        arrayGraph = visitor.arrayGraphBuilder.build();
        itemGraph = visitor.itemGraphBuilder.build();
        casts = visitor.casts.toArray(new ValueCast[0]);
        exceptions = visitor.exceptions.getAll();
        virtualCallSites = visitor.virtualCallSites.toArray(new VirtualCallSite[0]);
    }

    private void buildPropagationGraph() {
        IntStack stack = new IntStack();

        for (int i = 0; i < types.length; ++i) {
            if (types[i] != null) {
                stack.push(i);
            }
        }

        boolean[] visited = new boolean[types.length];
        GraphBuilder graphBuilder = new GraphBuilder(types.length);

        while (!stack.isEmpty()) {
            int entry = stack.pop();

            if (visited[entry]) {
                continue;
            }

            visited[entry] = true;

            int degree = extractDegree(entry);
            int variable = extractNode(entry);

            // Actually, successor nodes in resulting graph
            IntSet nextEntries = new IntHashSet();

            // Start: calculating successor nodes in resulting DAG along different paths
            //

            for (int successor : assignmentGraph.outgoingEdges(variable)) {
                nextEntries.add(packNodeAndDegree(successor, degree));
            }

            for (int successor : cloneGraph.outgoingEdges(variable)) {
                nextEntries.add(packNodeAndDegree(successor, degree));
            }

            if (degree > 0) {
                for (int predecessor : assignmentGraph.incomingEdges(variable)) {
                    nextEntries.add(packNodeAndDegree(predecessor, degree));
                }

                for (int successor : itemGraph.outgoingEdges(variable)) {
                    nextEntries.add(packNodeAndDegree(successor, degree - 1));
                }
            }

            for (int successor : arrayGraph.outgoingEdges(variable)) {
                nextEntries.add(packNodeAndDegree(successor, degree + 1));
            }

            //
            // End: calculating successor nodes in resulting graph

            for (IntCursor next : nextEntries) {
                graphBuilder.addEdge(entry, next.value);
                if (!visited[next.value]) {
                    stack.push(next.value);
                }
            }
        }

        graph = graphBuilder.build();
    }

    private void collapseSCCs() {
        int[][] sccs = GraphUtils.findStronglyConnectedComponents(graph);
        if (sccs.length == 0) {
            return;
        }

        for (int[] scc : sccs) {
            for (int i = 1; i < scc.length; ++i) {
                nodeMapping[scc[i]] = scc[0];
            }
        }

        boolean[] nodeChangedBackup = nodeChanged.clone();
        IntHashSet[] typesBackup = types.clone();
        Arrays.fill(nodeChanged, false);
        Arrays.fill(types, null);

        GraphBuilder graphBuilder = new GraphBuilder(graph.size());
        for (int i = 0; i < graph.size(); ++i) {
            for (int j : graph.outgoingEdges(i)) {
                int from = nodeMapping[i];
                int to = nodeMapping[j];
                if (from != to) {
                    graphBuilder.addEdge(from, to);
                }
            }

            int node = nodeMapping[i];
            if (typesBackup[i] != null) {
                getNodeTypes(node).addAll(typesBackup[i]);
            }

            if (nodeChangedBackup[i]) {
                nodeChanged[node] = true;
            }
        }

        graph = graphBuilder.build();
    }

    private static final byte FRESH = 0;
    private static final byte VISITING = 1;
    private static final byte VISITED = 2;

    private void buildPropagationPath() {
        byte[] state = new byte[types.length];
        int[] path = new int[types.length];
        int pathSize = 0;
        IntStack stack = new IntStack();

        for (int i = 0; i < graph.size(); ++i) {
            if (graph.incomingEdgesCount(i) == 0 && types[i] != null) {
                stack.push(i);
            }
        }

        while (!stack.isEmpty()) {
            int node = stack.pop();
            if (state[node] == FRESH) {
                state[node] = VISITING;

                stack.push(node);

                for (int successor : graph.outgoingEdges(node)) {
                    if (state[successor] == FRESH) {
                        stack.push(successor);
                    }
                }

            } else if (state[node] == VISITING) {
                path[pathSize++] = node;
                state[node] = VISITED;
            }
        }

        propagationPath = Arrays.copyOf(path, pathSize);
    }

    private void propagate(Program program) {
        changed = false;

        while (true) {
            System.arraycopy(nodeChanged, 0, formerNodeChanged, 0, nodeChanged.length);
            Arrays.fill(nodeChanged, false);

            propagateAlongDAG();
            boolean outerChanged = changed;

            do {
                changed = false;
                propagateAlongCasts();
                propagateAlongVirtualCalls(program);
                propagateAlongExceptions(program);
                if (changed) {
                    outerChanged = true;
                }
            } while (changed);

            if (!outerChanged) {
                break;
            }

            changed = false;
        }
    }

    private void propagateAlongDAG() {
        for (int i = propagationPath.length - 1; i >= 0; --i) {
            int node = propagationPath[i];
            boolean predecessorsChanged = false;
            for (int predecessor : graph.incomingEdges(node)) {
                if (formerNodeChanged[predecessor] || nodeChanged[predecessor]) {
                    predecessorsChanged = true;
                    break;
                }
            }
            if (!predecessorsChanged) {
                continue;
            }

            IntHashSet nodeTypes = getNodeTypes(node);
            for (int predecessor : graph.incomingEdges(node)) {
                if (formerNodeChanged[predecessor] || nodeChanged[predecessor]) {
                    if (nodeTypes.addAll(types[predecessor]) > 0) {
                        nodeChanged[node] = true;
                        changed = true;
                    }
                }
            }
        }
    }

    private void propagateAlongCasts() {
        for (ValueCast cast : casts) {
            int fromNode = nodeMapping[packNodeAndDegree(cast.fromVariable, 0)];
            if (!formerNodeChanged[fromNode] && !nodeChanged[fromNode]) {
                continue;
            }

            int toNode = nodeMapping[packNodeAndDegree(cast.toVariable, 0)];
            IntHashSet targetTypes = getNodeTypes(toNode);

            for (IntCursor cursor : types[fromNode]) {
                if (targetTypes.contains(cursor.value)) {
                    continue;
                }
                String className = typeList.get(cursor.value);

                ValueType type;
                if (className.startsWith("[")) {
                    type = ValueType.parseIfPossible(className);
                    if (type == null) {
                        type = ValueType.arrayOf(ValueType.object("java.lang.Object"));
                    }
                } else {
                    type = ValueType.object(className);
                }

                if (hierarchy.isSuperType(cast.targetType, type, false)) {
                    changed = true;
                    nodeChanged[toNode] = true;
                    targetTypes.add(cursor.value);
                }
            }
        }
    }

    private void propagateAlongVirtualCalls(Program program) {
        ClassReaderSource classSource = dependencyInfo.getClassSource();

        for (VirtualCallSite callSite : virtualCallSites) {
            int instanceNode = nodeMapping[packNodeAndDegree(callSite.instance, 0)];
            if (!formerNodeChanged[instanceNode] && !nodeChanged[instanceNode]) {
                continue;
            }

            for (IntCursor type : types[instanceNode]) {
                if (!callSite.knownClasses.add(type.value)) {
                    continue;
                }

                String className = typeList.get(type.value);
                MethodReference rawMethod = new MethodReference(className, callSite.method.getDescriptor());
                MethodReader resolvedMethod = classSource.resolveImplementation(rawMethod);

                if (resolvedMethod == null) {
                    continue;
                }

                MethodReference resolvedMethodRef = resolvedMethod.getReference();
                if (!callSite.resolvedMethods.add(resolvedMethodRef)) {
                    continue;
                }

                MethodDependencyInfo methodDep = dependencyInfo.getMethod(resolvedMethodRef);
                if (methodDep == null) {
                    continue;
                }
                if (callSite.receiver >= 0) {
                    readValue(methodDep.getResult(), program.variableAt(callSite.receiver));
                }
                for (int i = 0; i < callSite.arguments.length; ++i) {
                    writeValue(methodDep.getVariable(i + 1), program.variableAt(callSite.arguments[i]));
                }

                for (String thrownTypeName : methodDep.getThrown().getTypes()) {
                    propagateException(thrownTypeName, program.basicBlockAt(callSite.block));
                }
            }
        }
    }

    private void propagateAlongExceptions(Program program) {
        for (int i = 0; i < exceptions.length; i += 2) {
            int variable = nodeMapping[packNodeAndDegree(exceptions[i], 0)];
            if (!formerNodeChanged[variable] && !nodeChanged[variable]) {
                continue;
            }

            BasicBlock block = program.basicBlockAt(exceptions[i + 1]);
            for (IntCursor type : types[variable]) {
                String typeName = typeList.get(type.value);
                propagateException(typeName, block);
            }
        }
    }

    private void propagateException(String thrownTypeName, BasicBlock block) {
        for (TryCatchBlock tryCatch : block.getTryCatchBlocks()) {
            String expectedType = tryCatch.getExceptionType();
            if (expectedType == null || hierarchy.isSuperType(expectedType, thrownTypeName, false)) {
                if (tryCatch.getHandler().getExceptionVariable() == null) {
                    break;
                }
                int exceptionNode = packNodeAndDegree(tryCatch.getHandler().getExceptionVariable().getIndex(), 0);
                exceptionNode = nodeMapping[exceptionNode];
                int thrownType = getTypeByName(thrownTypeName);
                if (getNodeTypes(exceptionNode).add(thrownType)) {
                    nodeChanged[exceptionNode] = true;
                    changed = true;
                }

                break;
            }
        }
    }

    IntHashSet getNodeTypes(int node) {
        IntHashSet result = types[node];
        if (result == null) {
            result = new IntHashSet();
            types[node] = result;
        }
        return result;
    }

    int getTypeByName(String typeName) {
        int type = typeMap.getOrDefault(typeName, -1);
        if (type < 0) {
            type = typeList.size();
            typeMap.put(typeName, type);
            typeList.add(typeName);
        }
        return type;
    }

    class GraphBuildingVisitor extends AbstractInstructionVisitor {
        DependencyInfo dependencyInfo;
        GraphBuilder assignmentGraphBuilder;
        GraphBuilder cloneGraphBuilder;
        GraphBuilder arrayGraphBuilder;
        GraphBuilder itemGraphBuilder;
        MethodDependencyInfo thisMethodDep;
        List<ValueCast> casts = new ArrayList<>();
        IntegerArray exceptions = new IntegerArray(2);
        List<VirtualCallSite> virtualCallSites = new ArrayList<>();
        BasicBlock currentBlock;

        GraphBuildingVisitor(int variableCount, DependencyInfo dependencyInfo) {
            this.dependencyInfo = dependencyInfo;
            assignmentGraphBuilder = new GraphBuilder(variableCount);
            cloneGraphBuilder = new GraphBuilder(variableCount);
            arrayGraphBuilder = new GraphBuilder(variableCount);
            itemGraphBuilder = new GraphBuilder(variableCount);
        }

        public void visit(Phi phi) {
            for (Incoming incoming : phi.getIncomings()) {
                assignmentGraphBuilder.addEdge(incoming.getValue().getIndex(), phi.getReceiver().getIndex());
            }
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            addType(insn.getReceiver().getIndex(), 0, "java.lang.Class");
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            addType(insn.getReceiver().getIndex(), 0, "java.lang.String");
        }

        @Override
        public void visit(AssignInstruction insn) {
            assignmentGraphBuilder.addEdge(insn.getAssignee().getIndex(), insn.getReceiver().getIndex());
        }

        @Override
        public void visit(CastInstruction insn) {
            casts.add(new ValueCast(insn.getValue().getIndex(), insn.getReceiver().getIndex(), insn.getTargetType()));
            getNodeTypes(packNodeAndDegree(insn.getReceiver().getIndex(), 0));
        }

        @Override
        public void visit(RaiseInstruction insn) {
            exceptions.add(insn.getException().getIndex());
            exceptions.add(currentBlock.getIndex());
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            addType(insn.getReceiver().getIndex(), 0, ValueType.arrayOf(insn.getItemType()).toString());
        }

        @Override
        public void visit(ConstructInstruction insn) {
            addType(insn.getReceiver().getIndex(), 0, insn.getType());
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
            ValueType type = insn.getItemType();
            for (int i = 0; i < insn.getDimensions().size(); ++i) {
                type = ValueType.arrayOf(type);
            }
            addType(insn.getReceiver().getIndex(), 0, type.toString());
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            FieldDependencyInfo fieldDep = dependencyInfo.getField(insn.getField());
            ValueDependencyInfo valueDep = fieldDep.getValue();
            readValue(valueDep, insn.getReceiver());
        }

        @Override
        public void visit(PutFieldInstruction insn) {
            FieldDependencyInfo fieldDep = dependencyInfo.getField(insn.getField());
            ValueDependencyInfo valueDep = fieldDep.getValue();
            writeValue(valueDep, insn.getValue());
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
                while (resultDependency.hasArrayType() && resultDegree <= MAX_DEGREE) {
                    resultDependency = resultDependency.getArrayItem();
                    for (String paramType : resultDependency.getTypes()) {
                        addType(insn.getValueToReturn().getIndex(), resultDegree, paramType);
                    }
                    ++resultDegree;
                }
            }
        }

        @Override
        public void visit(InvokeInstruction insn) {
            if (insn.getType() == InvocationType.VIRTUAL) {
                int instance = insn.getInstance().getIndex();

                VirtualCallSite callSite = new VirtualCallSite();
                callSite.instance = instance;
                callSite.method = insn.getMethod();
                callSite.arguments = new int[insn.getArguments().size()];
                for (int i = 0; i < insn.getArguments().size(); ++i) {
                    callSite.arguments[i] = insn.getArguments().get(i).getIndex();
                    for (int j = 0; j <= MAX_DEGREE; ++j) {
                        getNodeTypes(packNodeAndDegree(callSite.arguments[i], j));
                    }
                }
                callSite.receiver = insn.getReceiver() != null ? insn.getReceiver().getIndex() : -1;
                callSite.block = currentBlock.getIndex();
                virtualCallSites.add(callSite);

                if (insn.getReceiver() != null) {
                    for (int j = 0; j <= MAX_DEGREE; ++j) {
                        getNodeTypes(packNodeAndDegree(callSite.receiver, j));
                    }
                }

                return;
            }

            MethodDependencyInfo methodDep = dependencyInfo.getMethod(insn.getMethod());
            if (methodDep != null) {
                if (insn.getReceiver() != null) {
                    readValue(methodDep.getResult(), insn.getReceiver());
                }

                for (int i = 0; i < insn.getArguments().size(); ++i) {
                    writeValue(methodDep.getVariable(i + 1), insn.getArguments().get(i));
                }

                for (String type : methodDep.getThrown().getTypes()) {
                    propagateException(type, currentBlock);
                }
            }
        }
    }

    static class VirtualCallSite {
        int instance;
        IntSet knownClasses = new IntHashSet();
        Set<MethodReference> resolvedMethods = new HashSet<>();
        MethodReference method;
        int[] arguments;
        int receiver;
        int block;
    }

    void readValue(ValueDependencyInfo valueDep, Variable receiver) {
        int depth = 0;
        boolean hasArrayType;
        do {
            for (String type : valueDep.getTypes()) {
                addType(receiver.getIndex(), depth, type);
            }
            depth++;
            hasArrayType = valueDep.hasArrayType();
            valueDep = valueDep.getArrayItem();
        } while (hasArrayType && depth <= MAX_DEGREE);
    }

    void writeValue(ValueDependencyInfo valueDep, Variable source) {
        int depth = 0;
        while (valueDep.hasArrayType() && depth < MAX_DEGREE) {
            depth++;
            valueDep = valueDep.getArrayItem();
            for (String type : valueDep.getTypes()) {
                addType(source.getIndex(), depth, type);
            }
        }
    }

    void addType(int variable, int degree, String typeName) {
        int entry = nodeMapping[packNodeAndDegree(variable, degree)];
        if (getNodeTypes(entry).add(getTypeByName(typeName))) {
            nodeChanged[entry] = true;
            changed = true;
        }
    }

    static int extractNode(int nodeAndDegree) {
        return nodeAndDegree >>> 3;
    }

    static int extractDegree(int nodeAndDegree) {
        return nodeAndDegree & 7;
    }

    static int packNodeAndDegree(int node, int degree) {
        return (node << 3) | degree;
    }

    static long packTwoIntegers(int a, int b) {
        return ((long) a << 32) | b;
    }

    static int unpackFirst(long pair) {
        return (int) (pair >>> 32);
    }

    static int unpackSecond(long pair) {
        return (int) pair;
    }

    static final class ValueCast {
        final int fromVariable;
        final int toVariable;
        final ValueType targetType;

        ValueCast(int fromVariable, int toVariable, ValueType targetType) {
            this.fromVariable = fromVariable;
            this.toVariable = toVariable;
            this.targetType = targetType;
        }
    }
}
