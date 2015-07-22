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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.teavm.callgraph.DefaultCallGraphNode;
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
    private DefaultCallGraphNode caller;
    private InstructionLocation currentLocation;
    private ExceptionConsumer currentExceptionConsumer;

    public DependencyGraphBuilder(DependencyChecker dependencyChecker) {
        this.dependencyChecker = dependencyChecker;
    }

    public void buildGraph(MethodDependency dep) {
        caller = dependencyChecker.callGraph.getNode(dep.getReference());
        MethodReader method = dep.getMethod();
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

        if (DependencyChecker.shouldLog) {
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
        for (int i = dep.getVariableCount(); i < nodeClasses.length; ++i) {
            nodeClasses[i] = dependencyChecker.createNode();
            if (DependencyChecker.shouldLog) {
                nodeClasses[i].setTag(dep.getMethod().getReference() + ":" + i);
            }
        }
        nodes = new DependencyNode[dep.getMethod().getProgram().variableCount()];
        for (int i = 0; i < nodes.length; ++i) {
            int mappedNode = nodeMapping[i];
            nodes[i] = mappedNode >= 0 ? nodeClasses[mappedNode] : null;
        }
        dep.setVariables(nodes);

        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlockReader block = program.basicBlockAt(i);
            currentExceptionConsumer = createExceptionConsumer(dep, block);
            block.readAllInstructions(reader);
            for (PhiReader phi : block.readPhis()) {
                for (IncomingReader incoming : phi.readIncomings()) {
                    DependencyNode incomingNode = nodes[incoming.getValue().getIndex()];
                    DependencyNode receiverNode = nodes[phi.getReceiver().getIndex()];
                    if (incomingNode != null && receiverNode != null) {
                        incomingNode.connect(receiverNode);
                    }
                }
            }
            for (TryCatchBlockReader tryCatch : block.readTryCatchBlocks()) {
                if (tryCatch.getExceptionType() != null) {
                    dependencyChecker.linkClass(tryCatch.getExceptionType(), new CallLocation(caller.getMethod()));
                }
            }
        }

        if (method.hasModifier(ElementModifier.SYNCHRONIZED)) {
            List<DependencyNode> syncNodes = new ArrayList<>();

            MethodDependency methodDep = dependencyChecker.linkMethod(
                        new MethodReference(Object.class, "monitorEnter", Object.class, void.class), null);
            syncNodes.add(methodDep.getVariable(1));
            methodDep.use();

            methodDep = dependencyChecker.linkMethod(
                    new MethodReference(Object.class, "monitorEnterSync", Object.class, void.class), null);
            syncNodes.add(methodDep.getVariable(1));
            methodDep.use();

            methodDep = dependencyChecker.linkMethod(
                    new MethodReference(Object.class, "monitorExit", Object.class, void.class), null);
            syncNodes.add(methodDep.getVariable(1));
            methodDep.use();

            methodDep = dependencyChecker.linkMethod(
                    new MethodReference(Object.class, "monitorExitSync", Object.class, void.class), null);
            syncNodes.add(methodDep.getVariable(1));
            methodDep.use();

            if (method.hasModifier(ElementModifier.STATIC)) {
                for (DependencyNode node : syncNodes) {
                    node.propagate(dependencyChecker.getType("java.lang.Class"));
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
                exceptions[i] = dependencyChecker.getClassSource().get(tryCatch.getExceptionType());
            }
            vars[i] = methodDep.getVariable(tryCatch.getExceptionVariable().getIndex());
        }
        return new ExceptionConsumer(dependencyChecker, exceptions, vars, methodDep);
    }

    private static class ExceptionConsumer implements DependencyConsumer {
        private DependencyChecker checker;
        private ClassReader[] exceptions;
        private DependencyNode[] vars;
        private MethodDependency method;

        public ExceptionConsumer(DependencyChecker checker, ClassReader[] exceptions, DependencyNode[] vars,
                MethodDependency method) {
            this.checker = checker;
            this.exceptions = exceptions;
            this.vars = vars;
            this.method = method;
        }

        @Override
        public void consume(DependencyType type) {
            for (int i = 0; i < exceptions.length; ++i) {
                if (exceptions[i] == null || isAssignableFrom(checker.getClassSource(), exceptions[i].getName(),
                        type.getName())) {
                    if (vars[i] != null) {
                        vars[i].propagate(type);
                    }
                    return;
                }
            }
            method.getThrown().propagate(type);
        }
    }

    private static class VirtualCallConsumer implements DependencyConsumer {
        private final DependencyNode node;
        private final ClassReader filterClass;
        private final MethodDescriptor methodDesc;
        private final DependencyChecker checker;
        private final DependencyNode[] parameters;
        private final DependencyNode result;
        private final DefaultCallGraphNode caller;
        private final InstructionLocation location;
        private final Set<MethodReference> knownMethods = new HashSet<>();
        private ExceptionConsumer exceptionConsumer;

        public VirtualCallConsumer(DependencyNode node, ClassReader filterClass,
                MethodDescriptor methodDesc, DependencyChecker checker, DependencyNode[] parameters,
                DependencyNode result, DefaultCallGraphNode caller, InstructionLocation location,
                ExceptionConsumer exceptionConsumer) {
            this.node = node;
            this.filterClass = filterClass;
            this.methodDesc = methodDesc;
            this.checker = checker;
            this.parameters = parameters;
            this.result = result;
            this.caller = caller;
            this.location = location;
            this.exceptionConsumer = exceptionConsumer;
        }

        @Override
        public void consume(DependencyType type) {
            String className = type.getName();
            if (DependencyChecker.shouldLog) {
                System.out.println("Virtual call of " + methodDesc + " detected on " + node.getTag() + ". " +
                        "Target class is " + className);
            }
            if (className.startsWith("[")) {
                className = "java.lang.Object";
            }
            if (!isAssignableFrom(checker.getClassSource(), filterClass.getName(), className)) {
                return;
            }
            MethodReference methodRef = new MethodReference(className, methodDesc);
            final MethodDependency methodDep = checker.linkMethod(methodRef,
                    new CallLocation(caller.getMethod(), location));
            if (!methodDep.isMissing() && knownMethods.add(methodRef)) {
                methodDep.use();
                DependencyNode[] targetParams = methodDep.getVariables();
                if (parameters[0] != null && targetParams[0] != null) {
                    parameters[0].connect(targetParams[0], thisType -> isAssignableFrom(checker.getClassSource(),
                            methodDep.getMethod().getOwnerName(), thisType.getName()));
                }
                for (int i = 1; i < parameters.length; ++i) {
                    if (parameters[i] != null && targetParams[i] != null) {
                        parameters[i].connect(targetParams[i]);
                    }
                }
                if (result != null && methodDep.getResult() != null) {
                    methodDep.getResult().connect(result);
                }
                methodDep.getThrown().addConsumer(exceptionConsumer);
            }
        }
    }

    private static boolean isAssignableFrom(ClassReaderSource classSource, String supertype, String subtypeName) {
        if (supertype.equals(subtypeName)) {
            return true;
        }
        ClassReader subtype = classSource.get(subtypeName);
        if (subtype == null) {
            return false;
        }
        if (subtype.getParent() != null && isAssignableFrom(classSource, supertype, subtype.getParent())) {
            return true;
        }
        if (subtype.getInterfaces().stream().anyMatch(iface -> isAssignableFrom(classSource, supertype, iface))) {
            return true;
        }
        return false;
    }

    private InstructionReader reader = new InstructionReader() {
        @Override
        public void location(InstructionLocation location) {
            currentLocation = location;
        }

        @Override
        public void nop() {
        }

        @Override
        public void classConstant(VariableReader receiver, ValueType cst) {
            DependencyNode node = nodes[receiver.getIndex()];
            if (node != null) {
                node.propagate(dependencyChecker.getType("java.lang.Class"));
            }
            while (cst instanceof ValueType.Array) {
                cst = ((ValueType.Array)cst).getItemType();
            }
            if (cst instanceof ValueType.Object) {
                final String className = ((ValueType.Object)cst).getClassName();
                dependencyChecker.linkClass(className, new CallLocation(caller.getMethod(), currentLocation));
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
            DependencyNode node = nodes[receiver.getIndex()];
            if (node != null) {
                node.propagate(dependencyChecker.getType("java.lang.String"));
            }
            MethodDependency method = dependencyChecker.linkMethod(new MethodReference(String.class,
                    "<init>", char[].class, void.class), new CallLocation(caller.getMethod(), currentLocation));
            method.use();
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
            if (valueNode != null && receiverNode != null) {
                valueNode.connect(receiverNode);
            }
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
            DependencyNode valueNode = nodes[value.getIndex()];
            DependencyNode receiverNode = nodes[receiver.getIndex()];
            if (targetType instanceof ValueType.Object) {
                String targetClsName = ((ValueType.Object)targetType).getClassName();
                final ClassReader targetClass = dependencyChecker.getClassSource().get(targetClsName);
                if (targetClass != null) {
                    if (valueNode != null && receiverNode != null) {
                        valueNode.connect(receiverNode, type -> {
                            if (targetClass.getName().equals("java.lang.Object")) {
                                return true;
                            }
                            return isAssignableFrom(dependencyChecker.getClassSource(), targetClass.getName(),
                                    type.getName());
                        });
                    }
                    return;
                }
            }
            if (valueNode != null && receiverNode != null) {
                valueNode.connect(receiverNode);
            }
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
        public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
            DependencyNode node = nodes[receiver.getIndex()];
            if (node != null) {
                node.propagate(dependencyChecker.getType("[" + itemType));
            }
            String className = extractClassName(itemType);
            if (className != null) {
                dependencyChecker.linkClass(className, new CallLocation(caller.getMethod(), currentLocation));
            }
        }

        private String extractClassName(ValueType itemType) {
            while (itemType instanceof ValueType.Array) {
                itemType = ((ValueType.Array)itemType).getItemType();
            }
            return itemType instanceof ValueType.Object ? ((ValueType.Object)itemType).getClassName() : null;
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType,
                List<? extends VariableReader> dimensions) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < dimensions.size(); ++i) {
                sb.append('[');
                itemType = ((ValueType.Array)itemType).getItemType();
            }
            String itemTypeStr;
            if (itemType instanceof ValueType.Object) {
                itemTypeStr = ((ValueType.Object)itemType).getClassName();
            } else {
                itemTypeStr = itemType.toString();
            }
            sb.append(itemTypeStr);
            DependencyNode node = nodes[receiver.getIndex()];
            for (int i = 0; i < dimensions.size(); ++i) {
                if (node == null) {
                    break;
                }
                node.propagate(dependencyChecker.getType(sb.substring(i, sb.length())));
                node = node.getArrayItem();
            }
            String className = extractClassName(itemType);
            if (className != null) {
                dependencyChecker.linkClass(className, new CallLocation(caller.getMethod(), currentLocation));
            }
        }

        @Override
        public void create(VariableReader receiver, String type) {
            dependencyChecker.linkClass(type, new CallLocation(caller.getMethod(), currentLocation));
            DependencyNode node = nodes[receiver.getIndex()];
            if (node != null) {
                node.propagate(dependencyChecker.getType(type));
            }
        }

        @Override
        public void getField(VariableReader receiver, VariableReader instance, FieldReference field,
                ValueType fieldType) {
            FieldDependency fieldDep = dependencyChecker.linkField(field,
                    new CallLocation(caller.getMethod(), currentLocation));
            if (!(fieldType instanceof ValueType.Primitive)) {
                DependencyNode receiverNode = nodes[receiver.getIndex()];
                if (receiverNode != null) {
                    fieldDep.getValue().connect(receiverNode);
                }
            }
            initClass(field.getClassName());
        }

        @Override
        public void putField(VariableReader instance, FieldReference field, VariableReader value,
                ValueType fieldType) {
            FieldDependency fieldDep = dependencyChecker.linkField(field,
                    new CallLocation(caller.getMethod(), currentLocation));
            if (!(fieldType instanceof ValueType.Primitive)) {
                DependencyNode valueNode = nodes[value.getIndex()];
                if (valueNode != null) {
                    valueNode.connect(fieldDep.getValue());
                }
            }
            initClass(field.getClassName());
        }

        @Override
        public void arrayLength(VariableReader receiver, VariableReader array) {
        }

        @Override
        public void cloneArray(VariableReader receiver, VariableReader array) {
            DependencyNode arrayNode = nodes[array.getIndex()];
            final DependencyNode receiverNode = nodes[receiver.getIndex()];
            if (arrayNode != null && receiverNode != null) {
                arrayNode.addConsumer(type -> receiverNode.propagate(type));
                arrayNode.getArrayItem().connect(receiverNode.getArrayItem());
            }
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
        public void getElement(VariableReader receiver, VariableReader array, VariableReader index) {
            DependencyNode arrayNode = nodes[array.getIndex()];
            DependencyNode receiverNode = nodes[receiver.getIndex()];
            if (arrayNode != null && receiverNode != null && receiverNode != arrayNode.getArrayItem()) {
                arrayNode.getArrayItem().connect(receiverNode);
            }
        }

        @Override
        public void putElement(VariableReader array, VariableReader index, VariableReader value) {
            DependencyNode valueNode = nodes[value.getIndex()];
            DependencyNode arrayNode = nodes[array.getIndex()];
            if (valueNode != null && arrayNode != null && valueNode != arrayNode.getArrayItem()) {
                valueNode.connect(arrayNode.getArrayItem());
            }
        }

        @Override
        public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments, InvocationType type) {
            if (instance == null) {
                invokeSpecial(receiver, null, method, arguments);
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
            CallLocation callLocation = new CallLocation(caller.getMethod(), currentLocation);
            dependencyChecker.linkClass(method.getClassName(), callLocation).initClass(callLocation);
            MethodDependency methodDep = dependencyChecker.linkMethod(method, callLocation);
            if (methodDep.isMissing()) {
                return;
            }
            methodDep.use();
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

        private void invokeVirtual(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments) {
            MethodDependency methodDep = dependencyChecker.linkMethod(method,
                    new CallLocation(caller.getMethod(), currentLocation));
            if (methodDep.isMissing()) {
                return;
            }
            DependencyNode[] actualArgs = new DependencyNode[arguments.size() + 1];
            for (int i = 0; i < arguments.size(); ++i) {
                actualArgs[i + 1] = nodes[arguments.get(i).getIndex()];
            }
            actualArgs[0] = nodes[instance.getIndex()];
            DependencyConsumer listener = new VirtualCallConsumer(nodes[instance.getIndex()],
                    dependencyChecker.getClassSource().get(methodDep.getMethod().getOwnerName()),
                    method.getDescriptor(), dependencyChecker, actualArgs,
                    receiver != null ? nodes[receiver.getIndex()] : null, caller, currentLocation,
                    currentExceptionConsumer);
            nodes[instance.getIndex()].addConsumer(listener);
        }

        @Override
        public void isInstance(VariableReader receiver, VariableReader value, final ValueType type) {
            String className = extractClassName(type);
            if (className != null) {
                dependencyChecker.linkClass(className, new CallLocation(caller.getMethod(), currentLocation));
            }
        }

        @Override
        public void initClass(final String className) {
            CallLocation callLocation = new CallLocation(caller.getMethod(), currentLocation);
            dependencyChecker.linkClass(className, callLocation).initClass(callLocation);
        }

        @Override
        public void nullCheck(VariableReader receiver, VariableReader value) {
            DependencyNode valueNode = nodes[value.getIndex()];
            DependencyNode receiverNode = nodes[receiver.getIndex()];
            valueNode.connect(receiverNode);
            dependencyChecker.linkMethod(new MethodReference(NullPointerException.class, "<init>", void.class),
                    new CallLocation(caller.getMethod(), currentLocation)).use();
            currentExceptionConsumer.consume(dependencyChecker.getType("java.lang.NullPointerException"));
        }

        @Override
        public void monitorEnter(VariableReader objectRef) {
             MethodDependency methodDep = dependencyChecker.linkMethod(
                        new MethodReference(Object.class, "monitorEnter", Object.class, void.class), null);
             nodes[objectRef.getIndex()].connect(methodDep.getVariable(1));
             methodDep.use();

             methodDep = dependencyChecker.linkMethod(
                     new MethodReference(Object.class, "monitorEnterSync", Object.class, void.class), null);
             nodes[objectRef.getIndex()].connect(methodDep.getVariable(1));
             methodDep.use();
        }

        @Override
        public void monitorExit(VariableReader objectRef) {
            MethodDependency methodDep = dependencyChecker.linkMethod(
                    new MethodReference(Object.class, "monitorExit", Object.class, void.class), null);
            nodes[objectRef.getIndex()].connect(methodDep.getVariable(1));
            methodDep.use();

            methodDep = dependencyChecker.linkMethod(
                    new MethodReference(Object.class, "monitorExitSync", Object.class, void.class), null);
            nodes[objectRef.getIndex()].connect(methodDep.getVariable(1));
            methodDep.use();
        }
    };
}
