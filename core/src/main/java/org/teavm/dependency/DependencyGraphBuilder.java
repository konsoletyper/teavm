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
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.teavm.cache.NoCache;
import org.teavm.callgraph.DefaultCallGraphNode;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.BasicBlock;
import org.teavm.model.BasicBlockReader;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReference;
import org.teavm.model.Incoming;
import org.teavm.model.IncomingReader;
import org.teavm.model.Instruction;
import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodHandle;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Phi;
import org.teavm.model.PhiReader;
import org.teavm.model.Program;
import org.teavm.model.RuntimeConstant;
import org.teavm.model.TextLocation;
import org.teavm.model.TryCatchBlockReader;
import org.teavm.model.ValueType;
import org.teavm.model.VariableReader;
import org.teavm.model.emit.ProgramEmitter;
import org.teavm.model.emit.ValueEmitter;
import org.teavm.model.instructions.AbstractInstructionReader;
import org.teavm.model.instructions.ArrayElementType;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.InstructionReader;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.text.ListingBuilder;

class DependencyGraphBuilder {
    private DependencyAnalyzer dependencyAnalyzer;
    private DependencyNode[] nodes;
    private DependencyNode resultNode;
    private Program program;
    private DefaultCallGraphNode caller;
    private TextLocation currentLocation;
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

        processInvokeDynamic(dep);

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
                    dependencyAnalyzer.linkClass(tryCatch.getExceptionType(), new CallLocation(caller.getMethod()));
                }
            }
        }

        if (method.hasModifier(ElementModifier.SYNCHRONIZED)) {
            List<DependencyNode> syncNodes = new ArrayList<>();

            MethodDependency methodDep = dependencyAnalyzer.linkMethod(
                        new MethodReference(Object.class, "monitorEnter", Object.class, void.class), null);
            syncNodes.add(methodDep.getVariable(1));
            methodDep.use();

            methodDep = dependencyAnalyzer.linkMethod(
                    new MethodReference(Object.class, "monitorEnterSync", Object.class, void.class), null);
            syncNodes.add(methodDep.getVariable(1));
            methodDep.use();

            methodDep = dependencyAnalyzer.linkMethod(
                    new MethodReference(Object.class, "monitorExit", Object.class, void.class), null);
            syncNodes.add(methodDep.getVariable(1));
            methodDep.use();

            methodDep = dependencyAnalyzer.linkMethod(
                    new MethodReference(Object.class, "monitorExitSync", Object.class, void.class), null);
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

    private void processInvokeDynamic(MethodDependency methodDep) {
        if (program == null) {
            return;
        }
        ProgramEmitter pe = ProgramEmitter.create(program, dependencyAnalyzer.getClassSource());
        boolean hasIndy = false;
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block) {
                if (!(insn instanceof InvokeDynamicInstruction)) {
                    continue;
                }
                block = insn.getBasicBlock();

                InvokeDynamicInstruction indy = (InvokeDynamicInstruction) insn;
                MethodReference bootstrapMethod = new MethodReference(indy.getBootstrapMethod().getClassName(),
                        indy.getBootstrapMethod().getName(), indy.getBootstrapMethod().signature());
                BootstrapMethodSubstitutor substitutor = dependencyAnalyzer.bootstrapMethodSubstitutors
                        .get(bootstrapMethod);
                if (substitutor == null) {
                    NullConstantInstruction nullInsn = new NullConstantInstruction();
                    nullInsn.setReceiver(indy.getReceiver());
                    nullInsn.setLocation(indy.getLocation());
                    insn.replace(nullInsn);
                    CallLocation location = new CallLocation(caller.getMethod(), currentLocation);
                    dependencyAnalyzer.getDiagnostics().error(location, "Substitutor for bootstrap "
                            + "method {{m0}} was not found", bootstrapMethod);
                    continue;
                }

                hasIndy = true;
                BasicBlock splitBlock = program.createBasicBlock();
                while (insn.getNext() != null) {
                    Instruction nextInsn = insn.getNext();
                    nextInsn.delete();
                    splitBlock.add(nextInsn);
                }

                for (int k = 0; k < program.basicBlockCount() - 1; ++k) {
                    BasicBlock replaceBlock = program.basicBlockAt(k);
                    for (Phi phi : replaceBlock.getPhis()) {
                        for (Incoming incoming : phi.getIncomings()) {
                            if (incoming.getSource() == block) {
                                incoming.setSource(splitBlock);
                            }
                        }
                    }
                }

                pe.enter(block);
                pe.setCurrentLocation(indy.getLocation());
                insn.delete();

                List<ValueEmitter> arguments = new ArrayList<>();
                for (int k = 0; k < indy.getArguments().size(); ++k) {
                    arguments.add(pe.var(indy.getArguments().get(k), indy.getMethod().parameterType(k)));
                }
                DynamicCallSite callSite = new DynamicCallSite(indy.getMethod(),
                        indy.getInstance() != null ? pe.var(indy.getInstance(),
                                ValueType.object(methodDep.getMethod().getOwnerName())) : null,
                        arguments, indy.getBootstrapMethod(), indy.getBootstrapArguments(),
                        dependencyAnalyzer.getAgent());
                ValueEmitter result = substitutor.substitute(callSite, pe);
                if (result.getVariable() != null && result.getVariable() != indy.getReceiver()) {
                    AssignInstruction assign = new AssignInstruction();
                    assign.setAssignee(result.getVariable());
                    assign.setReceiver(indy.getReceiver());
                    pe.addInstruction(assign);
                }
                pe.jump(splitBlock);
            }
        }

        if (hasIndy && methodDep.method.getAnnotations().get(NoCache.class.getName()) == null) {
            methodDep.method.getAnnotations().add(new AnnotationHolder(NoCache.class.getName()));
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
            ClassReaderSource classSource = analyzer.getClassSource();
            for (int i = 0; i < exceptions.length; ++i) {
                if (exceptions[i] == null || classSource.isSuperType(exceptions[i].getName(), type.getName())
                        .orElse(false)) {
                    if (vars[i] != null) {
                        vars[i].propagate(type);
                    }
                    return;
                }
            }
            method.getThrown().propagate(type);
        }
    }

    static class VirtualCallConsumer implements DependencyConsumer {
        private final DependencyNode node;
        private final MethodDescriptor methodDesc;
        private final DependencyAnalyzer analyzer;
        private final DependencyNode[] parameters;
        private final DependencyNode result;
        private final DefaultCallGraphNode caller;
        private final TextLocation location;
        private final Set<MethodReference> knownMethods = new HashSet<>();
        private final BitSet knownTypes = new BitSet();
        private ExceptionConsumer exceptionConsumer;
        private SuperClassFilter filter;

        VirtualCallConsumer(DependencyNode node, String filterClass,
                MethodDescriptor methodDesc, DependencyAnalyzer analyzer, DependencyNode[] parameters,
                DependencyNode result, DefaultCallGraphNode caller, TextLocation location,
                ExceptionConsumer exceptionConsumer) {
            this.node = node;
            this.filter = analyzer.getSuperClassFilter(filterClass);
            this.methodDesc = methodDesc;
            this.analyzer = analyzer;
            this.parameters = parameters;
            this.result = result;
            this.caller = caller;
            this.location = location;
            this.exceptionConsumer = exceptionConsumer;
        }

        @Override
        public void consume(DependencyType type) {
            if (knownTypes.get(type.index)) {
                return;
            }
            knownTypes.set(type.index);

            String className = type.getName();
            if (DependencyAnalyzer.shouldLog) {
                System.out.println("Virtual call of " + methodDesc + " detected on " + node.getTag() + ". "
                        + "Target class is " + className);
            }
            if (className.startsWith("[")) {
                className = "java.lang.Object";
                type = analyzer.getType(className);
            }

            if (!filter.match(type)) {
                return;
            }
            MethodReference methodRef = new MethodReference(className, methodDesc);
            MethodDependency methodDep = analyzer.linkMethod(methodRef, new CallLocation(caller.getMethod(), location));
            if (!methodDep.isMissing() && knownMethods.add(methodRef)) {
                methodDep.use();
                DependencyNode[] targetParams = methodDep.getVariables();
                if (parameters[0] != null && targetParams[0] != null) {
                    parameters[0].connect(targetParams[0],
                            analyzer.getSuperClassFilter(methodDep.getMethod().getOwnerName()));
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

    private InstructionReader reader = new AbstractInstructionReader() {
        @Override
        public void location(TextLocation location) {
            currentLocation = location;
        }

        @Override
        public void classConstant(VariableReader receiver, ValueType cst) {
            DependencyNode node = nodes[receiver.getIndex()];
            if (node != null) {
                node.propagate(dependencyAnalyzer.getType("java.lang.Class"));
                if (!(cst instanceof ValueType.Primitive)) {
                    StringBuilder sb = new StringBuilder();
                    while (cst instanceof ValueType.Array) {
                        cst = ((ValueType.Array) cst).getItemType();
                        sb.append('[');
                    }
                    if (cst instanceof ValueType.Object) {
                        sb.append(((ValueType.Object) cst).getClassName());
                    } else {
                        sb.append(cst.toString());
                    }
                    node.getClassValueNode().propagate(dependencyAnalyzer.getType(sb.toString()));
                }
            }
            while (cst instanceof ValueType.Array) {
                cst = ((ValueType.Array) cst).getItemType();
            }
            if (cst instanceof ValueType.Object) {
                final String className = ((ValueType.Object) cst).getClassName();
                dependencyAnalyzer.linkClass(className, new CallLocation(caller.getMethod(), currentLocation));
            }
        }

        @Override
        public void stringConstant(VariableReader receiver, String cst) {
            DependencyNode node = nodes[receiver.getIndex()];
            if (node != null) {
                node.propagate(dependencyAnalyzer.getType("java.lang.String"));
            }
            MethodDependency method = dependencyAnalyzer.linkMethod(new MethodReference(String.class,
                    "<init>", char[].class, void.class), new CallLocation(caller.getMethod(), currentLocation));
            method.use();
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
            ClassReaderSource classSource = dependencyAnalyzer.getClassSource();
            if (targetType instanceof ValueType.Object) {
                String targetClsName = ((ValueType.Object) targetType).getClassName();
                final ClassReader targetClass = classSource.get(targetClsName);
                if (targetClass != null && !(targetClass.getName().equals("java.lang.Object"))) {
                    if (valueNode != null && receiverNode != null) {
                        valueNode.connect(receiverNode, dependencyAnalyzer.getSuperClassFilter(targetClass.getName()));
                    }
                    return;
                }
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
        public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
            DependencyNode node = nodes[receiver.getIndex()];
            if (node != null) {
                node.propagate(dependencyAnalyzer.getType("[" + itemType));
            }
            String className = extractClassName(itemType);
            if (className != null) {
                dependencyAnalyzer.linkClass(className, new CallLocation(caller.getMethod(), currentLocation));
            }
        }

        private String extractClassName(ValueType itemType) {
            while (itemType instanceof ValueType.Array) {
                itemType = ((ValueType.Array) itemType).getItemType();
            }
            return itemType instanceof ValueType.Object ? ((ValueType.Object) itemType).getClassName() : null;
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType,
                List<? extends VariableReader> dimensions) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < dimensions.size(); ++i) {
                sb.append('[');
                itemType = ((ValueType.Array) itemType).getItemType();
            }
            String itemTypeStr;
            if (itemType instanceof ValueType.Object) {
                itemTypeStr = ((ValueType.Object) itemType).getClassName();
            } else {
                itemTypeStr = itemType.toString();
            }
            sb.append(itemTypeStr);
            DependencyNode node = nodes[receiver.getIndex()];
            for (int i = 0; i < dimensions.size(); ++i) {
                if (node == null) {
                    break;
                }
                node.propagate(dependencyAnalyzer.getType(sb.substring(i, sb.length())));
                node = node.getArrayItem();
            }
            String className = extractClassName(itemType);
            if (className != null) {
                dependencyAnalyzer.linkClass(className, new CallLocation(caller.getMethod(), currentLocation));
            }
        }

        @Override
        public void create(VariableReader receiver, String type) {
            dependencyAnalyzer.linkClass(type, new CallLocation(caller.getMethod(), currentLocation));
            DependencyNode node = nodes[receiver.getIndex()];
            if (node != null) {
                node.propagate(dependencyAnalyzer.getType(type));
            }
        }

        @Override
        public void getField(VariableReader receiver, VariableReader instance, FieldReference field,
                ValueType fieldType) {
            FieldDependency fieldDep = dependencyAnalyzer.linkField(field,
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
            FieldDependency fieldDep = dependencyAnalyzer.linkField(field,
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
        public void cloneArray(VariableReader receiver, VariableReader array) {
            DependencyNode arrayNode = nodes[array.getIndex()];
            final DependencyNode receiverNode = nodes[receiver.getIndex()];
            if (arrayNode != null && receiverNode != null) {
                arrayNode.addConsumer(receiverNode::propagate);
                arrayNode.getArrayItem().connect(receiverNode.getArrayItem());
            }
            MethodDependency cloneDep = dependencyAnalyzer.linkMethod(
                    new MethodReference(Object.class, "clone", Object.class),
                    new CallLocation(caller.getMethod(), currentLocation));
            arrayNode.connect(cloneDep.getVariable(0));
            cloneDep.use();
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
        public void getElement(VariableReader receiver, VariableReader array, VariableReader index,
                ArrayElementType type) {
            DependencyNode arrayNode = nodes[array.getIndex()];
            DependencyNode receiverNode = nodes[receiver.getIndex()];
            if (arrayNode != null && receiverNode != null && receiverNode != arrayNode.getArrayItem()) {
                arrayNode.getArrayItem().connect(receiverNode);
            }
        }

        @Override
        public void putElement(VariableReader array, VariableReader index, VariableReader value,
                ArrayElementType type) {
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
                if (method.getName().equals("getClass") && method.parameterCount() == 0
                        && method.getReturnType().isObject(Class.class) && receiver != null) {
                    nodes[instance.getIndex()].connect(nodes[receiver.getIndex()].getClassValueNode());
                }
            }
        }

        private void invokeSpecial(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments) {
            CallLocation callLocation = new CallLocation(caller.getMethod(), currentLocation);
            dependencyAnalyzer.linkClass(method.getClassName(), callLocation).initClass(callLocation);
            MethodDependency methodDep = dependencyAnalyzer.linkMethod(method, callLocation);
            methodDep.use();
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

        private void invokeVirtual(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments) {
            DependencyNode[] actualArgs = new DependencyNode[arguments.size() + 1];
            for (int i = 0; i < arguments.size(); ++i) {
                actualArgs[i + 1] = nodes[arguments.get(i).getIndex()];
            }
            actualArgs[0] = nodes[instance.getIndex()];
            DependencyConsumer listener = new VirtualCallConsumer(nodes[instance.getIndex()],
                    method.getClassName(), method.getDescriptor(), dependencyAnalyzer, actualArgs,
                    receiver != null ? nodes[receiver.getIndex()] : null, caller, currentLocation,
                    currentExceptionConsumer);
            nodes[instance.getIndex()].addConsumer(listener);

            dependencyAnalyzer.getClassSource().overriddenMethods(method).forEach(methodImpl -> {
                CallLocation callLocation = new CallLocation(caller.getMethod(), currentLocation);
                dependencyAnalyzer.linkMethod(methodImpl.getReference(), callLocation);
            });
        }

        @Override
        public void isInstance(VariableReader receiver, VariableReader value, final ValueType type) {
            String className = extractClassName(type);
            if (className != null) {
                dependencyAnalyzer.linkClass(className, new CallLocation(caller.getMethod(), currentLocation));
            }
        }

        @Override
        public void invokeDynamic(VariableReader receiver, VariableReader instance, MethodDescriptor method,
                List<? extends VariableReader> arguments, MethodHandle bootstrapMethod,
                List<RuntimeConstant> bootstrapArguments) {
            // Should be eliminated by processInvokeDynamic method
        }

        @Override
        public void initClass(final String className) {
            CallLocation callLocation = new CallLocation(caller.getMethod(), currentLocation);
            dependencyAnalyzer.linkClass(className, callLocation).initClass(callLocation);
        }

        @Override
        public void nullCheck(VariableReader receiver, VariableReader value) {
            DependencyNode valueNode = nodes[value.getIndex()];
            DependencyNode receiverNode = nodes[receiver.getIndex()];
            valueNode.connect(receiverNode);
            dependencyAnalyzer.linkMethod(new MethodReference(NullPointerException.class, "<init>", void.class),
                    new CallLocation(caller.getMethod(), currentLocation)).use();
            currentExceptionConsumer.consume(dependencyAnalyzer.getType("java.lang.NullPointerException"));
        }

        @Override
        public void monitorEnter(VariableReader objectRef) {
             MethodDependency methodDep = dependencyAnalyzer.linkMethod(
                        new MethodReference(Object.class, "monitorEnter", Object.class, void.class), null);
             nodes[objectRef.getIndex()].connect(methodDep.getVariable(1));
             methodDep.use();

             methodDep = dependencyAnalyzer.linkMethod(
                     new MethodReference(Object.class, "monitorEnterSync", Object.class, void.class), null);
             nodes[objectRef.getIndex()].connect(methodDep.getVariable(1));
             methodDep.use();
        }

        @Override
        public void monitorExit(VariableReader objectRef) {
            MethodDependency methodDep = dependencyAnalyzer.linkMethod(
                    new MethodReference(Object.class, "monitorExit", Object.class, void.class), null);
            nodes[objectRef.getIndex()].connect(methodDep.getVariable(1));
            methodDep.use();

            methodDep = dependencyAnalyzer.linkMethod(
                    new MethodReference(Object.class, "monitorExitSync", Object.class, void.class), null);
            nodes[objectRef.getIndex()].connect(methodDep.getVariable(1));
            methodDep.use();
        }
    };
}
