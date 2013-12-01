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
import org.teavm.model.*;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev
 */
class DependencyGraphBuilder {
    private DependencyChecker dependencyChecker;
    private DependencyNode[] nodes;
    private DependencyNode resultNode;
    private Program program;

    public DependencyGraphBuilder(DependencyChecker dependencyChecker) {
        this.dependencyChecker = dependencyChecker;
    }

    public void buildGraph(MethodHolder method, MethodGraph graph) {
        if (method.getProgram().basicBlockCount() == 0) {
            return;
        }
        program = method.getProgram();
        resultNode = graph.getResultNode();
        nodes = graph.getVariableNodes();
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlock block = program.basicBlockAt(i);
            for (Instruction insn : block.getInstructions()) {
                insn.acceptVisitor(visitor);
            }
            for (Phi phi : block.getPhis()) {
                for (Incoming incoming : phi.getIncomings()) {
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

        public VirtualCallPropagationListener(DependencyNode node, MethodDescriptor methodDesc,
                DependencyChecker checker, DependencyNode[] parameters, DependencyNode result) {
            this.node = node;
            this.methodDesc = methodDesc;
            this.checker = checker;
            this.parameters = parameters;
            this.result = result;
        }

        @Override
        public void consume(String className) {
            if (DependencyChecker.shouldLog) {
                System.out.println("Virtual call of " + methodDesc + " detected on " + node.getTag() + ". " +
                        "Target class is " + className);
            }
            MethodReference methodRef = new MethodReference(className, methodDesc);
            MethodHolder method = findMethod(methodRef, checker.getClassSource());
            if (method == null) {
                return;
            }
            MethodGraph targetGraph = checker.attachMethodGraph(methodRef);
            if (targetGraph == null) {
                throw new RuntimeException("Method not found: " + methodRef);
            }
            DependencyNode[] targetParams = targetGraph.getVariableNodes();
            for (int i = 0; i < parameters.length; ++i) {
                parameters[i].connect(targetParams[i]);
            }
            if (targetGraph.getResultNode() != null) {
                targetGraph.getResultNode().connect(result);
            }
        }
    }

    private static MethodHolder findMethod(MethodReference methodRef, ClassHolderSource classSource) {
        String className = methodRef.getClassName();
        while (className != null) {
            ClassHolder cls = classSource.getClassHolder(className);
            if (cls == null) {
                break;
            }
            MethodHolder method = cls.getMethod(methodRef.getDescriptor());
            if (method != null) {
                return method;
            }
            className = cls.getParent();
        }
        return null;
    }

    private InstructionVisitor visitor = new InstructionVisitor() {
        @Override
        public void visit(IsInstanceInstruction insn) {
        }

        @Override
        public void visit(InvokeInstruction insn) {
            if (insn.getInstance() == null) {
                invokeSpecial(insn);
            } else {
                switch (insn.getType()) {
                    case SPECIAL:
                        invokeSpecial(insn);
                        break;
                    case VIRTUAL:
                        invokeVirtual(insn);
                        break;
                }
            }
        }

        private void invokeSpecial(InvokeInstruction insn) {
            MethodGraph targetGraph = dependencyChecker.attachMethodGraph(insn.getMethod());
            DependencyNode[] targetParams = targetGraph.getVariableNodes();
            List<Variable> arguments = insn.getArguments();
            for (int i = 0; i < arguments.size(); ++i) {
                nodes[arguments.get(i).getIndex()].connect(targetParams[i + 1]);
            }
            if (insn.getInstance() != null) {
                nodes[insn.getInstance().getIndex()].connect(targetParams[0]);
            }
            if (targetGraph.getResultNode() != null) {
                targetGraph.getResultNode().connect(nodes[insn.getReceiver().getIndex()]);
            }
        }

        private void invokeVirtual(InvokeInstruction insn) {
            List<Variable> arguments = insn.getArguments();
            DependencyNode[] actualArgs = new DependencyNode[arguments.size() + 1];
            for (int i = 0; i < arguments.size(); ++i) {
                actualArgs[i + 1] = nodes[arguments.get(i).getIndex()];
            }
            actualArgs[0] = nodes[insn.getInstance().getIndex()];
            DependencyConsumer listener = new VirtualCallPropagationListener(nodes[insn.getInstance().getIndex()],
                    insn.getMethod().getDescriptor(), dependencyChecker, actualArgs,
                    insn.getReceiver() != null ? nodes[insn.getReceiver().getIndex()] : null);
            dependencyChecker.addAbstractMethod(insn.getMethod());
            nodes[insn.getInstance().getIndex()].addConsumer(listener);
        }

        @Override
        public void visit(PutElementInstruction insn) {
            DependencyNode valueNode = nodes[insn.getValue().getIndex()];
            DependencyNode arrayNode = nodes[insn.getArray().getIndex()];
            valueNode.connect(arrayNode.getArrayItemNode());
        }

        @Override
        public void visit(GetElementInstruction insn) {
            DependencyNode arrayNode = nodes[insn.getArray().getIndex()];
            DependencyNode receiverNode = nodes[insn.getReceiver().getIndex()];
            arrayNode.getArrayItemNode().connect(receiverNode);
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
            DependencyNode arrayNode = nodes[insn.getArray().getIndex()];
            DependencyNode receiverNode = nodes[insn.getReceiver().getIndex()];
            arrayNode.connect(receiverNode);
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
            DependencyNode arrayNode = nodes[insn.getArray().getIndex()];
            final DependencyNode receiverNode = nodes[insn.getReceiver().getIndex()];
            arrayNode.addConsumer(new DependencyConsumer() {
                @Override public void consume(String type) {
                    receiverNode.propagate(type);

                }
            });
            arrayNode.getArrayItemNode().connect(receiverNode.getArrayItemNode());
        }

        @Override
        public void visit(ArrayLengthInstruction insn) {
        }

        @Override
        public void visit(PutFieldInstruction insn) {
            DependencyNode fieldNode = dependencyChecker.getFieldNode(insn.getField());
            DependencyNode valueNode = nodes[insn.getValue().getIndex()];
            valueNode.connect(fieldNode);
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            DependencyNode fieldNode = dependencyChecker.getFieldNode(insn.getField());
            DependencyNode receiverNode = nodes[insn.getReceiver().getIndex()];
            fieldNode.connect(receiverNode);
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < insn.getDimensions().size(); ++i) {
                sb.append('[');
            }
            sb.append(insn.getItemType());
            nodes[insn.getReceiver().getIndex()].propagate(sb.toString());
        }

        @Override
        public void visit(ConstructInstruction insn) {
            nodes[insn.getReceiver().getIndex()].propagate(insn.getType());
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            nodes[insn.getReceiver().getIndex()].propagate("[" + insn.getItemType());
        }

        @Override
        public void visit(RaiseInstruction insn) {
        }

        @Override
        public void visit(ExitInstruction insn) {
            if (insn.getValueToReturn() != null) {
                nodes[insn.getValueToReturn().getIndex()].connect(resultNode);
            }
        }

        @Override
        public void visit(SwitchInstruction insn) {
        }

        @Override
        public void visit(JumpInstruction insn) {
        }

        @Override
        public void visit(BinaryBranchingInstruction insn) {
        }

        @Override
        public void visit(BranchingInstruction insn) {
        }

        @Override
        public void visit(CastNumberInstruction insn) {
        }

        @Override
        public void visit(CastInstruction insn) {
            DependencyNode valueNode = nodes[insn.getValue().getIndex()];
            DependencyNode receiverNode = nodes[insn.getReceiver().getIndex()];
            valueNode.connect(receiverNode);
        }

        @Override
        public void visit(AssignInstruction insn) {
            DependencyNode valueNode = nodes[insn.getAssignee().getIndex()];
            DependencyNode receiverNode = nodes[insn.getReceiver().getIndex()];
            valueNode.connect(receiverNode);
        }

        @Override
        public void visit(NegateInstruction insn) {
        }

        @Override
        public void visit(BinaryInstruction insn) {
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            nodes[insn.getReceiver().getIndex()].propagate("java.lang.String");
            dependencyChecker.attachMethodGraph(new MethodReference("java.lang.String", new MethodDescriptor(
                    "<init>", ValueType.arrayOf(ValueType.CHARACTER), ValueType.VOID)));
        }

        @Override
        public void visit(DoubleConstantInstruction insn) {
        }

        @Override
        public void visit(FloatConstantInstruction insn) {
        }

        @Override
        public void visit(LongConstantInstruction insn) {
        }

        @Override
        public void visit(IntegerConstantInstruction insn) {
        }

        @Override
        public void visit(NullConstantInstruction insn) {
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            nodes[insn.getReceiver().getIndex()].propagate("java.lang.Class");
            ValueType type = insn.getConstant();
            while (type instanceof ValueType.Array) {
                type = ((ValueType.Array)type).getItemType();
            }
            if (type instanceof ValueType.Object) {
                dependencyChecker.achieveClass(((ValueType.Object)type).getClassName());
            }
        }

        @Override
        public void visit(EmptyInstruction insn) {
        }
    };
}
