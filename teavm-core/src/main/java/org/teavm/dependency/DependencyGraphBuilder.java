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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private List<Runnable> useRunners = new ArrayList<>();
    private ExceptionConsumer currentExceptionConsumer;

    public DependencyGraphBuilder(DependencyChecker dependencyChecker) {
        this.dependencyChecker = dependencyChecker;
    }

    public void buildGraph(MethodDependency dep) {
        MethodReader method = dep.getMethod();
        if (method.getProgram() == null || method.getProgram().basicBlockCount() == 0) {
            return;
        }
        callerStack = dep.getStack();
        program = method.getProgram();
        if (DependencyChecker.shouldLog) {
            System.out.println("Method achieved: " + method.getReference());
            System.out.println(new ListingBuilder().buildListing(program, "    "));
        }
        resultNode = dep.getResult();
        nodes = dep.getVariables();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlockReader block = program.basicBlockAt(i);
            currentExceptionConsumer = createExceptionConsumer(dep, block);
            block.readAllInstructions(reader);
            for (PhiReader phi : block.readPhis()) {
                for (IncomingReader incoming : phi.readIncomings()) {
                    nodes[incoming.getValue().getIndex()].connect(nodes[phi.getReceiver().getIndex()]);
                }
            }
            for (final TryCatchBlockReader tryCatch : block.readTryCatchBlocks()) {
                useRunners.add(new Runnable() {
                    @Override public void run() {
                        if (tryCatch.getExceptionType() != null) {
                            dependencyChecker.initClass(tryCatch.getExceptionType(), callerStack);
                        }
                    }
                });
            }
        }
        dep.setUseRunner(new MultipleRunner(useRunners));
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
        public void consume(DependencyAgentType type) {
            for (int i = 0; i < exceptions.length; ++i) {
                if (exceptions[i] == null || isAssignableFrom(checker.getClassSource(), exceptions[i],
                        type.getName())) {
                    vars[i].propagate(type);
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
        private final DependencyStack stack;
        private final Set<MethodReference> knownMethods = new HashSet<>();
        private ExceptionConsumer exceptionConsumer;

        public VirtualCallConsumer(DependencyNode node, ClassReader filterClass,
                MethodDescriptor methodDesc, DependencyChecker checker, DependencyNode[] parameters,
                DependencyNode result, DependencyStack stack, ExceptionConsumer exceptionConsumer) {
            this.node = node;
            this.filterClass = filterClass;
            this.methodDesc = methodDesc;
            this.checker = checker;
            this.parameters = parameters;
            this.result = result;
            this.stack = stack;
            this.exceptionConsumer = exceptionConsumer;
        }

        @Override
        public void consume(DependencyAgentType type) {
            String className = type.getName();
            if (DependencyChecker.shouldLog) {
                System.out.println("Virtual call of " + methodDesc + " detected on " + node.getTag() + ". " +
                        "Target class is " + className);
            }
            if (className.startsWith("[")) {
                className = "java.lang.Object";
            }
            if (!isAssignableFrom(checker.getClassSource(), filterClass, className)) {
                return;
            }
            MethodReference methodRef = new MethodReference(className, methodDesc);
            MethodDependency methodDep = checker.linkMethod(methodRef, stack);
            if (!methodDep.isMissing() && knownMethods.add(methodRef)) {
                methodDep.use();
                DependencyNode[] targetParams = methodDep.getVariables();
                for (int i = 0; i < parameters.length; ++i) {
                    parameters[i].connect(targetParams[i]);
                }
                if (result != null && methodDep.getResult() != null) {
                    methodDep.getResult().connect(result);
                }
                methodDep.getThrown().addConsumer(exceptionConsumer);
            }
        }
    }

    private static class MultipleRunner implements Runnable {
        private List<Runnable> parts;
        public MultipleRunner(List<Runnable> parts) {
            this.parts = parts;
        }
        @Override public void run() {
            for (Runnable part : parts) {
                part.run();
            }
        }
    }

    private static boolean isAssignableFrom(ClassReaderSource classSource, ClassReader supertype,
            String subtypeName) {
        if (supertype.getName().equals(subtypeName)) {
            return true;
        }
        ClassReader subtype = classSource.get(subtypeName);
        if (subtype == null) {
            return false;
        }
        if (subtype.getParent() != null && isAssignableFrom(classSource, supertype, subtype.getParent())) {
            return true;
        }
        for (String iface : subtype.getInterfaces()) {
            if (isAssignableFrom(classSource, supertype, iface)) {
                return true;
            }
        }
        return false;
    }

    private static class TypePropagationRunner implements Runnable {
        private DependencyNode node;
        private DependencyType type;
        public TypePropagationRunner(DependencyNode node, DependencyType type) {
            this.node = node;
            this.type = type;
        }
        @Override public void run() {
            node.propagate(type);
        }
    }

    private InstructionReader reader = new InstructionReader() {
        @Override
        public void location(InstructionLocation location) {
        }

        @Override
        public void nop() {
        }

        @Override
        public void classConstant(VariableReader receiver, ValueType cst) {
            useRunners.add(new TypePropagationRunner(nodes[receiver.getIndex()],
                    dependencyChecker.getType("java.lang.Class")));
            while (cst instanceof ValueType.Array) {
                cst = ((ValueType.Array)cst).getItemType();
            }
            if (cst instanceof ValueType.Object) {
                final String className = ((ValueType.Object)cst).getClassName();
                useRunners.add(new Runnable() {
                    @Override public void run() {
                        dependencyChecker.initClass(className, callerStack);
                    }
                });
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
            useRunners.add(new TypePropagationRunner(nodes[receiver.getIndex()],
                    dependencyChecker.getType("java.lang.String")));
            MethodDependency method = dependencyChecker.linkMethod(new MethodReference(String.class,
                    "<init>", char[].class, void.class), callerStack);
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
            valueNode.connect(receiverNode);
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
            DependencyNode valueNode = nodes[value.getIndex()];
            DependencyNode receiverNode = nodes[receiver.getIndex()];
            if (targetType instanceof ValueType.Object) {
                String targetClsName = ((ValueType.Object)targetType).getClassName();
                final ClassReader targetClass = dependencyChecker.getClassSource().get(targetClsName);
                if (targetClass != null) {
                    valueNode.connect(receiverNode, new DependencyTypeFilter() {
                        @Override public boolean match(DependencyAgentType type) {
                            if (targetClass.getName().equals("java.lang.Object")) {
                                return true;
                            }
                            return isAssignableFrom(dependencyChecker.getClassSource(), targetClass, type.getName());
                        }
                    });
                    return;
                }
            }
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
            nodes[exception.getIndex()].addConsumer(currentExceptionConsumer);
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
            useRunners.add(new TypePropagationRunner(nodes[receiver.getIndex()],
                    dependencyChecker.getType("[" + itemType)));
            final String className = extractClassName(itemType);
            if (className != null) {
                useRunners.add(new Runnable() {
                    @Override public void run() {
                        dependencyChecker.initClass(className, callerStack);
                    }
                });
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
            }
            sb.append(itemType);
            useRunners.add(new TypePropagationRunner(nodes[receiver.getIndex()],
                    dependencyChecker.getType(sb.toString())));
        }

        @Override
        public void create(VariableReader receiver, String type) {
            useRunners.add(new TypePropagationRunner(nodes[receiver.getIndex()], dependencyChecker.getType(type)));
        }

        @Override
        public void getField(VariableReader receiver, VariableReader instance, FieldReference field,
                ValueType fieldType) {
            FieldDependency fieldDep = dependencyChecker.linkField(field, callerStack);
            DependencyNode receiverNode = nodes[receiver.getIndex()];
            fieldDep.getValue().connect(receiverNode);
            initClass(field.getClassName());
        }

        @Override
        public void putField(VariableReader instance, FieldReference field, VariableReader value) {
            FieldDependency fieldDep = dependencyChecker.linkField(field, callerStack);
            DependencyNode valueNode = nodes[value.getIndex()];
            valueNode.connect(fieldDep.getValue());
            initClass(field.getClassName());
        }

        @Override
        public void arrayLength(VariableReader receiver, VariableReader array) {
        }

        @Override
        public void cloneArray(VariableReader receiver, VariableReader array) {
            DependencyNode arrayNode = nodes[array.getIndex()];
            final DependencyNode receiverNode = nodes[receiver.getIndex()];
            arrayNode.addConsumer(new DependencyConsumer() {
                @Override public void consume(DependencyAgentType type) {
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
            MethodDependency methodDep = dependencyChecker.linkMethod(method, callerStack);
            if (methodDep.isMissing()) {
                return;
            }
            methodDep.use();
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
            methodDep.getThrown().addConsumer(currentExceptionConsumer);
            initClass(method.getClassName());
        }

        private void invokeVirtual(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments) {
            MethodDependency methodDep = dependencyChecker.linkMethod(method, callerStack);
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
                    receiver != null ? nodes[receiver.getIndex()] : null, callerStack,
                    currentExceptionConsumer);
            nodes[instance.getIndex()].addConsumer(listener);
        }

        @Override
        public void isInstance(VariableReader receiver, VariableReader value, final ValueType type) {
            if (type instanceof ValueType.Object) {
                final String className = ((ValueType.Object)type).getClassName();
                useRunners.add(new Runnable() {
                    @Override public void run() {
                        dependencyChecker.initClass(className, callerStack);
                    }
                });
            }
        }

        @Override
        public void initClass(final String className) {
            useRunners.add(new Runnable() {
                @Override public void run() {
                    dependencyChecker.initClass(className, callerStack);
                }
            });
        }

        @Override
        public void nullCheck(VariableReader receiver, VariableReader value) {
            DependencyNode valueNode = nodes[value.getIndex()];
            DependencyNode receiverNode = nodes[receiver.getIndex()];
            valueNode.connect(receiverNode);
            useRunners.add(new Runnable() {
                @Override public void run() {
                    dependencyChecker.linkMethod(new MethodReference("java.lang.NullPointerException",
                            "<init>", ValueType.VOID), callerStack);
                }
            });
            currentExceptionConsumer.consume(dependencyChecker.getType("java.lang.NullPointerException"));
        }
    };
}
