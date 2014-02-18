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

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.teavm.model.*;
import org.teavm.model.instructions.*;
import org.teavm.model.util.ListingBuilder;

/**
 *
 * @author Alexey Andreev
 */
class DependencyGraphBuilder {
    private DependencyChecker dependencyChecker;
    private DependencyNode[] nodes;
    private DependencyNode resultNode;
    private ProgramReader program;
    private DependencyStack callerStack;

    public DependencyGraphBuilder(DependencyChecker dependencyChecker) {
        this.dependencyChecker = dependencyChecker;
    }

    public void buildGraph(MethodReader method, MethodDependency graph) {
        if (method.getProgram() == null || method.getProgram().basicBlockCount() == 0) {
            return;
        }
        callerStack = graph.getStack();
        program = method.getProgram();
        if (DependencyChecker.shouldLog) {
            System.out.println("Method achieved: " + method.getReference());
            System.out.println(new ListingBuilder().buildListing(program, "    "));
        }
        resultNode = graph.getResult();
        nodes = graph.getVariables();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlockReader block = program.basicBlockAt(i);
            block.readAllInstructions(reader);
            for (PhiReader phi : block.readPhis()) {
                for (IncomingReader incoming : phi.readIncomings()) {
                    nodes[incoming.getValue().getIndex()].connect(nodes[phi.getReceiver().getIndex()]);
                }
            }
        }
    }

    private static class VirtualCallPropagationListener implements DependencyConsumer {
        private final DependencyNode node;
        private final MethodDescriptor methodDesc;
        private final DependencyChecker checker;
        private final DependencyNode[] parameters;
        private final DependencyNode result;
        private final DependencyStack stack;
        private final ConcurrentMap<MethodReference, MethodReference> knownMethods = new ConcurrentHashMap<>();

        public VirtualCallPropagationListener(DependencyNode node, MethodDescriptor methodDesc,
                DependencyChecker checker, DependencyNode[] parameters, DependencyNode result,
                DependencyStack stack) {
            this.node = node;
            this.methodDesc = methodDesc;
            this.checker = checker;
            this.parameters = parameters;
            this.result = result;
            this.stack = stack;
        }

        @Override
        public void consume(String className) {
            if (DependencyChecker.shouldLog) {
                System.out.println("Virtual call of " + methodDesc + " detected on " + node.getTag() + ". " +
                        "Target class is " + className);
            }
            if (className.startsWith("[")) {
                className = "java.lang.Object";
            }
            MethodReference methodRef = new MethodReference(className, methodDesc);
            MethodDependency methodDep = checker.linkMethod(methodRef, stack);
            if (!methodDep.isMissing() && knownMethods.putIfAbsent(methodRef, methodRef) == null) {
                DependencyNode[] targetParams = methodDep.getVariables();
                for (int i = 0; i < parameters.length; ++i) {
                    parameters[i].connect(targetParams[i]);
                }
                if (methodDep.getResult() != null) {
                    methodDep.getResult().connect(result);
                }
            }
        }
    }

    private InstructionReader reader = new InstructionReader() {
        @Override
        public void nop() {
        }

        @Override
        public void classConstant(VariableReader receiver, ValueType cst) {
            nodes[receiver.getIndex()].propagate("java.lang.Class");
            while (cst instanceof ValueType.Array) {
                cst = ((ValueType.Array)cst).getItemType();
            }
            if (cst instanceof ValueType.Object) {
                dependencyChecker.achieveClass(((ValueType.Object)cst).getClassName(), callerStack);
            }
        }

        @Override
        public void nullConstant(VariableReader receiver) {
        }

        @Override
        public void integerConstant(VariableReader receiver, int cst) {
        }

        @Override
        public void longConstant(VariableReader receiver, long cst) {
        }

        @Override
        public void floatConstant(VariableReader receiver, float cst) {
        }

        @Override
        public void doubleConstant(VariableReader receiver, double cst) {
        }

        @Override
        public void stringConstant(VariableReader receiver, String cst) {
            nodes[receiver.getIndex()].propagate("java.lang.String");
            dependencyChecker.linkMethod(new MethodReference("java.lang.String", new MethodDescriptor(
                    "<init>", ValueType.arrayOf(ValueType.CHARACTER), ValueType.VOID)), callerStack);
        }

        @Override
        public void binary(BinaryOperation op, VariableReader receiver, VariableReader first, VariableReader second,
                NumericOperandType type) {
        }

        @Override
        public void negate(VariableReader receiver, VariableReader operand, NumericOperandType type) {
        }

        @Override
        public void assign(VariableReader receiver, VariableReader assignee) {
            DependencyNode valueNode = nodes[assignee.getIndex()];
            DependencyNode receiverNode = nodes[receiver.getIndex()];
            valueNode.connect(receiverNode);
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
            DependencyNode valueNode = nodes[value.getIndex()];
            DependencyNode receiverNode = nodes[receiver.getIndex()];
            valueNode.connect(receiverNode);
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, NumericOperandType sourceType,
                NumericOperandType targetType) {
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, IntegerSubtype type,
                CastIntegerDirection targetType) {
        }

        @Override
        public void jumpIf(BranchingCondition cond, VariableReader operand, BasicBlockReader consequent,
                BasicBlockReader alternative) {
        }

        @Override
        public void jumpIf(BinaryBranchingCondition cond, VariableReader first, VariableReader second,
                BasicBlockReader consequent, BasicBlockReader alternative) {
        }

        @Override
        public void jump(BasicBlockReader target) {
        }

        @Override
        public void choose(VariableReader condition, List<? extends SwitchTableEntryReader> table,
                BasicBlockReader defaultTarget) {
        }

        @Override
        public void exit(VariableReader valueToReturn) {
            if (valueToReturn != null) {
                nodes[valueToReturn.getIndex()].connect(resultNode);
            }
        }

        @Override
        public void raise(VariableReader exception) {
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
            nodes[receiver.getIndex()].propagate("[" + itemType);
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType,
                List<? extends VariableReader> dimensions) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < dimensions.size(); ++i) {
                sb.append('[');
            }
            sb.append(itemType);
            nodes[receiver.getIndex()].propagate(sb.toString());
        }

        @Override
        public void create(VariableReader receiver, String type) {
            nodes[receiver.getIndex()].propagate(type);
        }

        @Override
        public void getField(VariableReader receiver, VariableReader instance, FieldReference field,
                ValueType fieldType) {
            DependencyNode fieldNode = dependencyChecker.linkField(field, callerStack);
            DependencyNode receiverNode = nodes[receiver.getIndex()];
            fieldNode.connect(receiverNode);
        }

        @Override
        public void putField(VariableReader instance, FieldReference field, VariableReader value) {
            DependencyNode fieldNode = dependencyChecker.linkField(field, callerStack);
            DependencyNode valueNode = nodes[value.getIndex()];
            valueNode.connect(fieldNode);
        }

        @Override
        public void arrayLength(VariableReader receiver, VariableReader array) {
        }

        @Override
        public void cloneArray(VariableReader receiver, VariableReader array) {
            DependencyNode arrayNode = nodes[array.getIndex()];
            final DependencyNode receiverNode = nodes[receiver.getIndex()];
            arrayNode.addConsumer(new DependencyConsumer() {
                @Override public void consume(String type) {
                    receiverNode.propagate(type);
                }
            });
            arrayNode.getArrayItem().connect(receiverNode.getArrayItem());
        }

        @Override
        public void unwrapArray(VariableReader receiver, VariableReader array, ArrayElementType elementType) {
            DependencyNode arrayNode = nodes[array.getIndex()];
            DependencyNode receiverNode = nodes[receiver.getIndex()];
            arrayNode.connect(receiverNode);
        }

        @Override
        public void getElement(VariableReader receiver, VariableReader array, VariableReader index) {
            DependencyNode arrayNode = nodes[array.getIndex()];
            DependencyNode receiverNode = nodes[receiver.getIndex()];
            arrayNode.getArrayItem().connect(receiverNode);
        }

        @Override
        public void putElement(VariableReader array, VariableReader index, VariableReader value) {
            DependencyNode valueNode = nodes[value.getIndex()];
            DependencyNode arrayNode = nodes[array.getIndex()];
            valueNode.connect(arrayNode.getArrayItem());
        }

        @Override
        public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments, InvocationType type) {
            if (instance == null) {
                invokeSpecial(receiver, instance, method, arguments);
            } else {
                switch (type) {
                    case SPECIAL:
                        invokeSpecial(receiver, instance, method, arguments);
                        break;
                    case VIRTUAL:
                        invokeVirtual(receiver, instance, method, arguments);
                        break;
                }
            }
        }

        private void invokeSpecial(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments) {
            MethodDependency methodDep = dependencyChecker.linkMethod(method, callerStack);
            if (methodDep.isMissing()) {
                return;
            }
            DependencyNode[] targetParams = methodDep.getVariables();
            for (int i = 0; i < arguments.size(); ++i) {
                nodes[arguments.get(i).getIndex()].connect(targetParams[i + 1]);
            }
            if (instance != null) {
                nodes[instance.getIndex()].connect(targetParams[0]);
            }
            if (methodDep.getResult() != null && receiver != null) {
                methodDep.getResult().connect(nodes[receiver.getIndex()]);
            }
        }

        private void invokeVirtual(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments) {
            if (dependencyChecker.linkMethod(method, callerStack).isMissing()) {
                return;
            }
            DependencyNode[] actualArgs = new DependencyNode[arguments.size() + 1];
            for (int i = 0; i < arguments.size(); ++i) {
                actualArgs[i + 1] = nodes[arguments.get(i).getIndex()];
            }
            actualArgs[0] = nodes[instance.getIndex()];
            DependencyConsumer listener = new VirtualCallPropagationListener(nodes[instance.getIndex()],
                    method.getDescriptor(), dependencyChecker, actualArgs,
                    receiver != null ? nodes[receiver.getIndex()] : null, callerStack);
            nodes[instance.getIndex()].addConsumer(listener);
        }

        @Override
        public void isInstance(VariableReader receiver, VariableReader value, ValueType type) {
        }

        @Override
        public void initClass(String className) {
            dependencyChecker.initClass(className, callerStack);
        }
    };
}
