/*
 *  Copyright 2023 Alexey Andreev.
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

import com.carrotsearch.hppc.IntStack;
import java.util.ArrayDeque;
import java.util.Objects;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.model.InvokeDynamicInstruction;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.AbstractInstructionVisitor;
import org.teavm.model.instructions.ArrayLengthInstruction;
import org.teavm.model.instructions.AssignInstruction;
import org.teavm.model.instructions.BinaryInstruction;
import org.teavm.model.instructions.BoundCheckInstruction;
import org.teavm.model.instructions.CastInstruction;
import org.teavm.model.instructions.CastIntegerInstruction;
import org.teavm.model.instructions.CastNumberInstruction;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.CloneArrayInstruction;
import org.teavm.model.instructions.ConstructArrayInstruction;
import org.teavm.model.instructions.ConstructInstruction;
import org.teavm.model.instructions.ConstructMultiArrayInstruction;
import org.teavm.model.instructions.DoubleConstantInstruction;
import org.teavm.model.instructions.FloatConstantInstruction;
import org.teavm.model.instructions.GetElementInstruction;
import org.teavm.model.instructions.GetFieldInstruction;
import org.teavm.model.instructions.IntegerConstantInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.model.instructions.IsInstanceInstruction;
import org.teavm.model.instructions.LongConstantInstruction;
import org.teavm.model.instructions.NegateInstruction;
import org.teavm.model.instructions.NullCheckInstruction;
import org.teavm.model.instructions.NullConstantInstruction;
import org.teavm.model.instructions.NumericOperandType;
import org.teavm.model.instructions.StringConstantInstruction;
import org.teavm.model.instructions.UnwrapArrayInstruction;

public abstract class BaseTypeInference<T> {
    private Program program;
    private MethodReference reference;
    private Object[] types;
    private Graph graph;
    private Graph arrayGraph;

    public BaseTypeInference(Program program, MethodReference reference) {
        this.program = program;
        this.reference = reference;
    }

    private void prepare() {
        types = new Object[program.variableCount()];
        var visitor = new InitialTypeVisitor(program.variableCount());
        var params = Math.min(reference.parameterCount(), program.variableCount() - 1);
        for (var i = 0; i < params; ++i) {
            visitor.type(program.variableAt(i + 1), reference.parameterType(i));
        }
        visitor.type(program.variableAt(0), ValueType.object(reference.getClassName()));
        for (var block : program.getBasicBlocks()) {
            for (var insn : block) {
                insn.acceptVisitor(visitor);
            }
            for (var phi : block.getPhis()) {
                for (var incoming : phi.getIncomings()) {
                    visitor.graphBuilder.addEdge(incoming.getValue().getIndex(), phi.getReceiver().getIndex());
                }
            }
        }
        graph = visitor.graphBuilder.build();
        arrayGraph = visitor.arrayGraphBuilder.build();
    }

    @SuppressWarnings("unchecked")
    private void propagate() {
        var stack = new IntStack();
        var typeStack = new ArrayDeque<T>();
        for (var i = 0; i < types.length; ++i) {
            if (types[i] != null) {
                stack.push(i);
                typeStack.push((T) types[i]);
                types[i] = null;
            }
        }
        while (!stack.isEmpty()) {
            var variable = stack.pop();
            var type = typeStack.pop();
            var formerType = (T) types[variable];
            if (Objects.equals(formerType, type)) {
                continue;
            }
            type = doMerge(type, formerType);
            if (Objects.equals(type, formerType) || type == null) {
                continue;
            }
            types[variable] = type;
            for (var succ : graph.outgoingEdges(variable)) {
                if (!Objects.equals(types[succ], type)) {
                    stack.push(succ);
                    typeStack.push(type);
                }
            }
            if (arrayGraph.outgoingEdgesCount(variable) > 0) {
                var elementType = elementType(type);
                for (var succ : arrayGraph.outgoingEdges(variable)) {
                    if (!Objects.equals(types[succ], elementType)) {
                        stack.push(succ);
                        typeStack.push(elementType);
                    }
                }
            }
        }
    }

    public void ensure() {
        if (types == null) {
            prepare();
            propagate();
        }
    }

    @SuppressWarnings("unchecked")
    public T typeOf(Variable variable) {
        ensure();
        return (T) types[variable.getIndex()];
    }


    protected abstract T mapType(ValueType type);

    protected abstract T nullType();

    private T doMerge(T a, T b) {
        if (a == null) {
            return b;
        } else if (b == null) {
            return a;
        } else {
            return merge(a, b);
        }
    }

    protected abstract T merge(T a, T b);

    protected abstract T elementType(T t);

    protected T methodReturnType(MethodReference methodRef) {
        return mapType(methodRef.getReturnType());
    }

    private class InitialTypeVisitor extends AbstractInstructionVisitor {
        private GraphBuilder graphBuilder;
        private GraphBuilder arrayGraphBuilder;

        InitialTypeVisitor(int size) {
            graphBuilder = new GraphBuilder(size);
            arrayGraphBuilder = new GraphBuilder(size);
        }

        @Override
        public void visit(NullConstantInstruction insn) {
            types[insn.getReceiver().getIndex()] = nullType();
        }

        @Override
        public void visit(IntegerConstantInstruction insn) {
            type(insn.getReceiver(), ValueType.INTEGER);
        }

        @Override
        public void visit(LongConstantInstruction insn) {
            type(insn.getReceiver(), ValueType.LONG);
        }

        @Override
        public void visit(FloatConstantInstruction insn) {
            type(insn.getReceiver(), ValueType.FLOAT);
        }

        @Override
        public void visit(DoubleConstantInstruction insn) {
            type(insn.getReceiver(), ValueType.DOUBLE);
        }

        @Override
        public void visit(ClassConstantInstruction insn) {
            type(insn.getReceiver(), ValueType.object("java/lang/Class"));
        }

        @Override
        public void visit(StringConstantInstruction insn) {
            type(insn.getReceiver(), ValueType.object("java/lang/String"));
        }

        @Override
        public void visit(ConstructInstruction insn) {
            type(insn.getReceiver(), ValueType.object(insn.getType()));
        }

        @Override
        public void visit(ConstructArrayInstruction insn) {
            type(insn.getReceiver(), ValueType.arrayOf(insn.getItemType()));
        }

        @Override
        public void visit(ConstructMultiArrayInstruction insn) {
            var type = insn.getItemType();
            for (var i = 0; i < insn.getDimensions().size(); ++i) {
                type = ValueType.arrayOf(type);
            }
            type(insn.getReceiver(), type);
        }

        @Override
        public void visit(IsInstanceInstruction insn) {
            type(insn.getReceiver(), ValueType.BOOLEAN);
        }

        @Override
        public void visit(CastInstruction insn) {
            type(insn.getReceiver(), insn.getTargetType());
        }

        @Override
        public void visit(NegateInstruction insn) {
            type(insn.getReceiver(), insn.getOperandType());
        }

        @Override
        public void visit(CastNumberInstruction insn) {
            type(insn.getReceiver(), insn.getTargetType());
        }

        @Override
        public void visit(BinaryInstruction insn) {
            type(insn.getReceiver(), insn.getOperandType());
        }

        @Override
        public void visit(CastIntegerInstruction insn) {
            switch (insn.getTargetType()) {
                case BYTE:
                    type(insn.getReceiver(), ValueType.BYTE);
                    break;
                case CHAR:
                    type(insn.getReceiver(), ValueType.CHARACTER);
                    break;
                case SHORT:
                    type(insn.getReceiver(), ValueType.SHORT);
                    break;
            }
        }

        @Override
        public void visit(ArrayLengthInstruction insn) {
            type(insn.getReceiver(), ValueType.INTEGER);
        }

        @Override
        public void visit(CloneArrayInstruction insn) {
            graphBuilder.addEdge(insn.getArray().getIndex(), insn.getReceiver().getIndex());
        }

        @Override
        public void visit(BoundCheckInstruction insn) {
            type(insn.getReceiver(), ValueType.INTEGER);
        }

        @Override
        public void visit(InvokeInstruction insn) {
            type(insn.getReceiver(), methodReturnType(insn.getMethod()));
        }

        @Override
        public void visit(InvokeDynamicInstruction insn) {
            type(insn.getReceiver(), insn.getMethod().getResultType());
        }

        @Override
        public void visit(GetFieldInstruction insn) {
            type(insn.getReceiver(), insn.getFieldType());
        }

        @Override
        public void visit(UnwrapArrayInstruction insn) {
            graphBuilder.addEdge(insn.getArray().getIndex(), insn.getReceiver().getIndex());
        }

        @Override
        public void visit(GetElementInstruction insn) {
            arrayGraphBuilder.addEdge(insn.getArray().getIndex(), insn.getReceiver().getIndex());
        }

        @Override
        public void visit(AssignInstruction insn) {
            graphBuilder.addEdge(insn.getAssignee().getIndex(), insn.getReceiver().getIndex());
        }

        @Override
        public void visit(NullCheckInstruction insn) {
            graphBuilder.addEdge(insn.getValue().getIndex(), insn.getReceiver().getIndex());
        }

        void type(Variable target, NumericOperandType type) {
            switch (type) {
                case INT:
                    type(target, ValueType.INTEGER);
                    break;
                case LONG:
                    type(target, ValueType.LONG);
                    break;
                case FLOAT:
                    type(target, ValueType.FLOAT);
                    break;
                case DOUBLE:
                    type(target, ValueType.DOUBLE);
                    break;
            }
        }

        void type(Variable target, ValueType type) {
            if (target != null) {
                var t = mapType(type);
                if (t != null) {
                    types[target.getIndex()] = t;
                }
            }
        }

        void type(Variable target, T type) {
            if (target != null && type != null) {
                types[target.getIndex()] = type;
            }
        }
    }
}
