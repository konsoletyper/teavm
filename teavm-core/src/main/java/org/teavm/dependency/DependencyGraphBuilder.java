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

import java.util.Set;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev
 */
class DependencyGraphBuilder {
    private DependencyChecker dependencyChecker;
    private ClassHolderSource classSource;
    private DependencyNode[] nodes;
    private DependencyNode resultNode;
    private Program program;
    private ValueType resultType;

    public DependencyGraphBuilder(DependencyChecker dependencyChecker) {
        this.dependencyChecker = dependencyChecker;
        this.classSource = dependencyChecker.getClassSource();
    }

    public void buildGraph(MethodHolder method, MethodGraph graph) {
        if (method.getProgram().basicBlockCount() == 0) {
            return;
        }
        program = method.getProgram();
        resultType = method.getResultType();
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

    private static boolean hasBody(MethodHolder method) {
        Set<ElementModifier> modifiers = method.getModifiers();
        return !modifiers.contains(ElementModifier.ABSTRACT) &&
                !modifiers.contains(ElementModifier.NATIVE);
    }

    private static class VirtualCallPropagationListener implements DependencyConsumer {
        private final DependencyNode node;
        private final MethodDescriptor methodDesc;
        private final DependencyChecker checker;
        private final ValueType[] paramTypes;
        private final DependencyNode[] parameters;
        private final ValueType resultType;
        private final DependencyNode result;

        public VirtualCallPropagationListener(DependencyNode node, MethodDescriptor methodDesc,
                DependencyChecker checker, ValueType[] paramTypes, DependencyNode[] parameters,
                ValueType resultType, DependencyNode result) {
            this.node = node;
            this.methodDesc = methodDesc;
            this.checker = checker;
            this.paramTypes = paramTypes;
            this.parameters = parameters;
            this.resultType = resultType;
            this.result = result;
        }

        @Override
        public void consume(String className) {
            if (DependencyChecker.shouldLog) {
                System.out.println("Virtual call of " + methodDesc + " detected on " +
                        node.getTag() + ". Target class is " + className);
            }
            MethodReference methodRef = new MethodReference(className, methodDesc);
            MethodHolder method = findMethod(methodRef, checker.getClassSource());
            if (method == null) {
                return;
            }
            MethodGraph targetGraph = checker.attachMethodGraph(methodRef);
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

    private static MethodHolder requireMethod(MethodReference methodRef, ClassHolderSource classSource) {
        MethodHolder method = findMethod(methodRef, classSource);
        if (method == null) {
            throw new IllegalStateException("Method not found: " + methodRef);
        }
        return method;
    }

    private static FieldHolder findField(FieldReference fieldRef, ClassHolderSource classSource) {
        String className = fieldRef.getClassName();
        while (className != null) {
            ClassHolder cls = classSource.getClassHolder(className);
            if (cls == null) {
                break;
            }
            FieldHolder field = cls.getField(fieldRef.getFieldName());
            if (field != null) {
                return field;
            }
            className = cls.getParent();
        }
        return null;
    }

    private static boolean isPossibleArrayPair(ValueType a, ValueType b) {
        if (a instanceof ValueType.Array || b instanceof ValueType.Array) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.toString().equals("Ljava/lang/Object;") && b.toString().equals("Ljava/lang/Object;")) {
            return true;
        }
        return false;
    }

    private InstructionVisitor visitor = new InstructionVisitor() {
        @Override
        public void visit(IsInstanceInstruction insn) {
        }

        @Override
        public void visit(InvokeInstruction insn) {
        }

        @Override
        public void visit(PutElementInstruction insn) {
        }

        @Override
        public void visit(GetElementInstruction insn) {
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
        }

        @Override
        public void visit(ArrayLengthInstruction insn) {
        }

        @Override
        public void visit(PutFieldInstruction insn) {
        }

        @Override
        public void visit(GetFieldInstruction insn) {
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
        }

        @Override
        public void visit(ConstructInstruction insn) {
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
        }

        @Override
        public void visit(RaiseInstruction insn) {
        }

        @Override
        public void visit(ExitInstruction insn) {
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
        }

        @Override
        public void visit(AssignInstruction insn) {
        }

        @Override
        public void visit(NegateInstruction insn) {
        }

        @Override
        public void visit(BinaryInstruction insn) {
        }

        @Override
        public void visit(StringConstantInstruction insn) {
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
        }

        @Override
        public void visit(EmptyInstruction insn) {
        }
    };
}
