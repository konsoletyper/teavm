/*
 *  Copyright 2012 Alexey Andreev.
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
package org.teavm.dependency;

import static org.teavm.dependency.AbstractInstructionAnalyzer.MONITOR_ENTER_METHOD;
import static org.teavm.dependency.AbstractInstructionAnalyzer.MONITOR_ENTER_SYNC_METHOD;
import static org.teavm.dependency.AbstractInstructionAnalyzer.MONITOR_EXIT_METHOD;
import static org.teavm.dependency.AbstractInstructionAnalyzer.MONITOR_EXIT_SYNC_METHOD;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.teavm.callgraph.DefaultCallGraphNode;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHierarchy;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.IncomingReader;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.PhiReader;
import org.teavm.model.Program;
import org.teavm.model.TryCatchBlockReader;
import org.teavm.model.ValueType;
import org.teavm.model.VariableReader;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.text.ListingBuilder;

class DependencyGraphBuilder {
    private static final MethodDescriptor GET_CLASS = new MethodDescriptor("getClass", Class.class);
    private DependencyAnalyzer dependencyAnalyzer;
    private DependencyNode[] nodes;
    private DependencyNode resultNode;
    private Program program;
    private DefaultCallGraphNode caller;
    private ExceptionConsumer currentExceptionConsumer;

    DependencyGraphBuilder(DependencyAnalyzer dependencyAnalyzer) {
        this.dependencyAnalyzer = dependencyAnalyzer;
    }

    public void buildGraph(MethodDependency dep) {
        caller = dependencyAnalyzer.callGraph.getNode(dep.getReference());
        MethodHolder method = dep.method;
        if (method.getProgram() == null || method.getProgram().basicBlockCount() == 0) {
            return;
        }
        program = method.getProgram();
        resultNode = dep.getResult();

        DataFlowGraphBuilder dfgBuilder = new DataFlowGraphBuilder();
        boolean[] significantParams = new boolean[dep.getParameterCount()];
        significantParams[0] = true;
        for (int i = 1; i < dep.getParameterCount(); ++i) {
            ValueType arg = method.parameterType(i - 1);
            if (!(arg instanceof ValueType.Primitive)) {
                significantParams[i] = true;
            }
        }
        int[] nodeMapping = dfgBuilder.buildMapping(program, significantParams,
                !(method.getResultType() instanceof ValueType.Primitive) && method.getResultType() != ValueType.VOID);

        if (DependencyAnalyzer.shouldLog) {
            System.out.println("Method reached: " + method.getReference());
            System.out.print(new ListingBuilder().buildListing(program, "    "));
            for (int i = 0; i < nodeMapping.length; ++i) {
                System.out.print(i + ":" + nodeMapping[i] + " ");
            }
            System.out.println();
            System.out.println();
        }

        int nodeClassCount = 0;
        for (int i = 0; i < nodeMapping.length; ++i) {
            nodeClassCount = Math.max(nodeClassCount, nodeMapping[i] + 1);
        }
        DependencyNode[] nodeClasses = Arrays.copyOf(dep.getVariables(), nodeClassCount);
        MethodReference ref = method.getReference();
        for (int i = dep.getVariableCount(); i < nodeClasses.length; ++i) {
            nodeClasses[i] = dependencyAnalyzer.createNode();
            nodeClasses[i].method = ref;
            if (DependencyAnalyzer.shouldTag) {
                nodeClasses[i].setTag(dep.getMethod().getReference() + ":" + i);
            }
        }
        nodes = new DependencyNode[dep.getMethod().getProgram().variableCount()];
        for (int i = 0; i < nodes.length; ++i) {
            int mappedNode = nodeMapping[i];
            nodes[i] = mappedNode >= 0 ? nodeClasses[mappedNode] : null;
        }
        dep.setVariables(nodes);

        reader.setCaller(caller.getMethod());
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlockReader block = program.basicBlockAt(i);
            currentExceptionConsumer = createExceptionConsumer(dep, block);
            block.readAllInstructions(reader);

            for (PhiReader phi : block.readPhis()) {
                DependencyNode receiverNode = nodes[phi.getReceiver().getIndex()];
                for (IncomingReader incoming : phi.readIncomings()) {
                    DependencyNode incomingNode = nodes[incoming.getValue().getIndex()];
                    if (incomingNode != null && receiverNode != null) {
                        incomingNode.connect(receiverNode);
                    }
                }
            }

            for (TryCatchBlockReader tryCatch : block.readTryCatchBlocks()) {
                if (tryCatch.getExceptionType() != null) {
                    dependencyAnalyzer.linkClass(tryCatch.getExceptionType());
                }
            }
        }

        if (method.hasModifier(ElementModifier.SYNCHRONIZED)) {
            List<DependencyNode> syncNodes = new ArrayList<>();

            MethodDependency methodDep;
            if (dependencyAnalyzer.asyncSupported) {
                methodDep = dependencyAnalyzer.linkMethod(MONITOR_ENTER_METHOD);
                syncNodes.add(methodDep.getVariable(1));
                methodDep.use();
            }

            methodDep = dependencyAnalyzer.linkMethod(MONITOR_ENTER_SYNC_METHOD);
            syncNodes.add(methodDep.getVariable(1));
            methodDep.use();

            if (dependencyAnalyzer.asyncSupported) {
                methodDep = dependencyAnalyzer.linkMethod(MONITOR_EXIT_METHOD);
                syncNodes.add(methodDep.getVariable(1));
                methodDep.use();
            }

            methodDep = dependencyAnalyzer.linkMethod(MONITOR_EXIT_SYNC_METHOD);
            syncNodes.add(methodDep.getVariable(1));
            methodDep.use();

            if (method.hasModifier(ElementModifier.STATIC)) {
                for (DependencyNode node : syncNodes) {
                    node.propagate(dependencyAnalyzer.getType("java.lang.Class"));
                }
            } else {
                for (DependencyNode node : syncNodes) {
                    nodes[0].connect(node);
                }
            }
        }
    }

    private ExceptionConsumer createExceptionConsumer(MethodDependency methodDep, BasicBlockReader block) {
        List<? extends TryCatchBlockReader> tryCatchBlocks = block.readTryCatchBlocks();
        ClassReader[] exceptions = new ClassReader[tryCatchBlocks.size()];
        DependencyNode[] vars = new DependencyNode[tryCatchBlocks.size()];
        for (int i = 0; i < tryCatchBlocks.size(); ++i) {
            TryCatchBlockReader tryCatch = tryCatchBlocks.get(i);
            if (tryCatch.getExceptionType() != null) {
                exceptions[i] = dependencyAnalyzer.getClassSource().get(tryCatch.getExceptionType());
            }
            if (tryCatch.getHandler().getExceptionVariable() != null) {
                vars[i] = methodDep.getVariable(tryCatch.getHandler().getExceptionVariable().getIndex());
            }
        }
        return new ExceptionConsumer(dependencyAnalyzer, exceptions, vars, methodDep);
    }

    static class ExceptionConsumer implements DependencyConsumer {
        private DependencyAnalyzer analyzer;
        private ClassReader[] exceptions;
        private DependencyNode[] vars;
        private MethodDependency method;

        ExceptionConsumer(DependencyAnalyzer analyzer, ClassReader[] exceptions, DependencyNode[] vars,
                MethodDependency method) {
            this.analyzer = analyzer;
            this.exceptions = exceptions;
            this.vars = vars;
            this.method = method;
        }

        @Override
        public void consume(DependencyType type) {
            ClassHierarchy hierarchy = analyzer.getClassHierarchy();
            for (int i = 0; i < exceptions.length; ++i) {
                if (exceptions[i] == null || hierarchy.isSuperType(exceptions[i].getName(), type.getName(), false)) {
                    if (vars[i] != null) {
                        vars[i].propagate(type);
                    }
                    return;
                }
            }
            method.getThrown().propagate(type);
        }
    }

    private AbstractInstructionAnalyzer reader = new AbstractInstructionAnalyzer() {
        @Override
        public void assign(VariableReader receiver, VariableReader assignee) {
            DependencyNode valueNode = nodes[assignee.getIndex()];
            DependencyNode receiverNode = nodes[receiver.getIndex()];
            if (valueNode != null && receiverNode != null) {
                valueNode.connect(receiverNode);
            }
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
            DependencyNode valueNode = nodes[value.getIndex()];
            DependencyNode receiverNode = nodes[receiver.getIndex()];
            ClassReaderSource classSource = dependencyAnalyzer.getClassSource();
            if (targetType instanceof ValueType.Object) {
                String targetClsName = ((ValueType.Object) targetType).getClassName();
                ClassReader targetClass = classSource.get(targetClsName);
                if (targetClass != null && !(targetClass.getName().equals("java.lang.Object"))) {
                    if (valueNode != null && receiverNode != null) {
                        valueNode.connect(receiverNode, dependencyAnalyzer.getSuperClassFilter(targetClass.getName()));
                    }
                    return;
                }
            } else if (targetType instanceof ValueType.Array) {
                ValueType itemType = targetType;
                while (itemType instanceof ValueType.Array) {
                    itemType = ((ValueType.Array) itemType).getItemType();
                }
                if (itemType instanceof ValueType.Object) {
                    ClassReader targetClass = classSource.get(((ValueType.Object) itemType).getClassName());
                    if (targetClass == null) {
                        valueNode.connect(receiverNode);
                        return;
                    }
                }
                if (valueNode != null && receiverNode != null) {
                    valueNode.connect(receiverNode, dependencyAnalyzer.getSuperClassFilter(targetType.toString()));
                }
                return;
            }
            if (valueNode != null && receiverNode != null) {
                valueNode.connect(receiverNode);
            }
        }
        @Override
        public void exit(VariableReader valueToReturn) {
            if (valueToReturn != null) {
                DependencyNode node = nodes[valueToReturn.getIndex()];
                if (node != null) {
                    node.connect(resultNode);
                }
            }
        }

        @Override
        public void raise(VariableReader exception) {
            nodes[exception.getIndex()].addConsumer(currentExceptionConsumer);
        }

        @Override
        public void unwrapArray(VariableReader receiver, VariableReader array, ArrayElementType elementType) {
            DependencyNode arrayNode = nodes[array.getIndex()];
            DependencyNode receiverNode = nodes[receiver.getIndex()];
            if (arrayNode != null && receiverNode != null) {
                arrayNode.connect(receiverNode);
            }
        }

        @Override
        public void cloneArray(VariableReader receiver, VariableReader array) {
            DependencyNode arrayNode = getNode(array);
            DependencyNode receiverNode = getNode(receiver);
            if (arrayNode != null && receiverNode != null) {
                arrayNode.addConsumer(receiverNode::propagate);
                arrayNode.getArrayItem().connect(receiverNode.getArrayItem());
            }
            MethodDependency cloneDep = getAnalyzer().linkMethod(CLONE_METHOD);
            cloneDep.addLocation(getCallLocation());
            arrayNode.connect(cloneDep.getVariable(0));
            cloneDep.use();
        }

        @Override
        public void getElement(VariableReader receiver, VariableReader array, VariableReader index,
                ArrayElementType type) {
            if (isPrimitive(type)) {
                return;
            }
            DependencyNode arrayNode = nodes[array.getIndex()];
            DependencyNode receiverNode = nodes[receiver.getIndex()];
            if (arrayNode != null && receiverNode != null && receiverNode != arrayNode.getArrayItem()) {
                arrayNode.getArrayItem().connect(receiverNode);
            }
        }

        @Override
        public void putElement(VariableReader array, VariableReader index, VariableReader value,
                ArrayElementType type) {
            if (isPrimitive(type)) {
                return;
            }
            DependencyNode valueNode = nodes[value.getIndex()];
            DependencyNode arrayNode = nodes[array.getIndex()];
            if (valueNode != null && arrayNode != null && valueNode != arrayNode.getArrayItem()) {
                valueNode.connect(arrayNode.getArrayItem());
            }
        }

        private boolean isPrimitive(ArrayElementType type) {
            return type != ArrayElementType.OBJECT;
        }

        @Override
        protected void invokeSpecial(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments) {
            if (method.getDescriptor().equals(GET_CLASS)) {
                invokeGetClass(receiver, instance);
                return;
            }
            CallLocation callLocation = getCallLocation();
            if (instance == null) {
                dependencyAnalyzer.linkClass(method.getClassName()).initClass(callLocation);
            } else {
                dependencyAnalyzer.linkClass(method.getClassName());
            }
            MethodDependency methodDep = dependencyAnalyzer.linkMethod(method);
            methodDep.addLocation(callLocation);
            methodDep.use(false);
            if (methodDep.isMissing()) {
                return;
            }
            DependencyNode[] targetParams = methodDep.getVariables();
            for (int i = 0; i < arguments.size(); ++i) {
                DependencyNode value = nodes[arguments.get(i).getIndex()];
                DependencyNode param = targetParams[i + 1];
                if (value != null && param != null) {
                    value.connect(param);
                }
            }
            if (instance != null) {
                nodes[instance.getIndex()].connect(targetParams[0]);
            }
            if (methodDep.getResult() != null && receiver != null) {
                DependencyNode receiverNode = nodes[receiver.getIndex()];
                if (methodDep.getResult() != null && receiverNode != null) {
                    methodDep.getResult().connect(receiverNode);
                }
            }
            methodDep.getThrown().addConsumer(currentExceptionConsumer);
            initClass(method.getClassName());
        }

        @Override
        protected void invokeVirtual(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments) {
            if (method.getDescriptor().equals(GET_CLASS)) {
                invokeGetClass(receiver, instance);
                return;
            }

            DependencyNode[] actualArgs = new DependencyNode[arguments.size() + 1];
            for (int i = 0; i < arguments.size(); ++i) {
                actualArgs[i + 1] = nodes[arguments.get(i).getIndex()];
            }
            actualArgs[0] = getNode(instance);
            DependencyConsumer listener = new VirtualCallConsumer(getNode(instance),
                    method.getClassName(), method.getDescriptor(), dependencyAnalyzer, actualArgs,
                    receiver != null ? getNode(receiver) : null, getCallLocation(),
                    currentExceptionConsumer);
            getNode(instance).addConsumer(listener);

            dependencyAnalyzer.getClassSource().overriddenMethods(method).forEach(methodImpl -> {
                dependencyAnalyzer.linkMethod(methodImpl.getReference()).addLocation(getCallLocation());
            });
        }

        private void invokeGetClass(VariableReader receiver, VariableReader instance) {
            MethodDependency getClassDep = dependencyAnalyzer.linkMethod("java.lang.Object", GET_CLASS);
            getClassDep.addLocation(getCallLocation());
            getNode(instance).addConsumer(t -> {
                getClassDep.getVariable(0).propagate(t);
                if (receiver != null) {
                    getNode(receiver).getClassValueNode().propagate(t);
                }
            });
            if (receiver != null) {
                getNode(receiver).propagate(dependencyAnalyzer.getType("java.lang.Class"));
            }
            getClassDep.use();
        }

        @Override
        public void nullCheck(VariableReader receiver, VariableReader value) {
            super.nullCheck(receiver, value);
            currentExceptionConsumer.consume(dependencyAnalyzer.getType("java.lang.NullPointerException"));
        }

        @Override
        protected DependencyNode getNode(VariableReader variable) {
            return nodes[variable.getIndex()];
        }

        @Override
        protected DependencyAnalyzer getAnalyzer() {
            return dependencyAnalyzer;
        }
    };
}
