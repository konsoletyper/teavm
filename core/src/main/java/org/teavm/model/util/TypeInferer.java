/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.model.util;

import java.util.List;
import org.teavm.common.Graph;
import org.teavm.common.GraphBuilder;
import org.teavm.common.IntegerStack;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

public class TypeInferer {
    private static InferenceKind[] typesByOrdinal = InferenceKind.values();
    InferenceType[] types;
    GraphBuilder builder;
    GraphBuilder arrayElemBuilder;

    public void inferTypes(ProgramReader program, MethodReference method) {
        int sz = program.variableCount();
        types = new InferenceType[sz];

        types[0] = new InferenceType(InferenceKind.OBJECT, 0);
        for (int i = 0; i < method.parameterCount(); ++i) {
            ValueType param = method.parameterType(i);
            types[i + 1] = convert(param);
        }

        builder = new GraphBuilder(sz);
        arrayElemBuilder = new GraphBuilder(sz);
        for (int i = 0; i < program.basicBlockCount(); ++i) {
            BasicBlockReader block = program.basicBlockAt(i);

            if (block.getExceptionVariable() != null) {
                types[block.getExceptionVariable().getIndex()] = new InferenceType(InferenceKind.OBJECT, 0);
            }

            block.readAllInstructions(reader);
            for (PhiReader phi : block.readPhis()) {
                for (IncomingReader incoming : phi.readIncomings()) {
                    builder.addEdge(incoming.getValue().getIndex(), phi.getReceiver().getIndex());
                }
            }
        }

        IntegerStack stack = new IntegerStack(sz * 2);
        Graph graph = builder.build();
        Graph arrayElemGraph = arrayElemBuilder.build();
        for (int i = 0; i < graph.size(); ++i) {
            if (types[i] != null && graph.outgoingEdgesCount(i) > 0) {
                stack.push(types[i].kind.ordinal());
                stack.push(types[i].degree);
                stack.push(i);
                types[i] = null;
            }
        }

        while (!stack.isEmpty()) {
            int node = stack.pop();
            int degree = stack.pop();
            InferenceKind kind = typesByOrdinal[stack.pop()];
            if (types[node] != null) {
                continue;
            }
            types[node] = new InferenceType(kind, degree);

            for (int successor : graph.outgoingEdges(node)) {
                if (types[successor] == null) {
                    stack.push(kind.ordinal());
                    stack.push(degree);
                    stack.push(successor);
                }
            }
            for (int successor : arrayElemGraph.outgoingEdges(node)) {
                if (types[successor] == null) {
                    stack.push(kind.ordinal());
                    stack.push(degree - 1);
                    stack.push(successor);
                }
            }
        }
    }

    public VariableType typeOf(int variableIndex) {
        InferenceType result = types[variableIndex];
        if (result == null) {
            return VariableType.OBJECT;
        }
        if (result.degree == 0) {
            switch (result.kind) {
                case BYTE:
                case SHORT:
                case CHAR:
                case INT:
                    return VariableType.INT;
                case LONG:
                    return VariableType.LONG;
                case FLOAT:
                    return VariableType.FLOAT;
                case DOUBLE:
                    return VariableType.DOUBLE;
                case OBJECT:
                    break;
            }
            return VariableType.OBJECT;
        } else if (result.degree == 1) {
            switch (result.kind) {
                case BYTE:
                    return VariableType.BYTE_ARRAY;
                case SHORT:
                    return VariableType.SHORT_ARRAY;
                case CHAR:
                    return VariableType.CHAR_ARRAY;
                case INT:
                    return VariableType.INT_ARRAY;
                case LONG:
                    return VariableType.LONG_ARRAY;
                case FLOAT:
                    return VariableType.FLOAT_ARRAY;
                case DOUBLE:
                    return VariableType.DOUBLE_ARRAY;
                case OBJECT:
                    break;
            }
        }
        return VariableType.OBJECT_ARRAY;
    }

    InferenceType convert(ValueType type) {
        int degree = 0;
        while (type instanceof ValueType.Array) {
            ++degree;
            type = ((ValueType.Array) type).getItemType();
        }

        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case BYTE:
                    return new InferenceType(InferenceKind.BYTE, degree);
                case SHORT:
                    return new InferenceType(InferenceKind.SHORT, degree);
                case CHARACTER:
                    return new InferenceType(InferenceKind.CHAR, degree);
                case INTEGER:
                    return new InferenceType(InferenceKind.INT, degree);
                case FLOAT:
                    return new InferenceType(InferenceKind.FLOAT, degree);
                case DOUBLE:
                    return new InferenceType(InferenceKind.DOUBLE, degree);
                case LONG:
                    return new InferenceType(InferenceKind.LONG, degree);
            }
        }
        return new InferenceType(InferenceKind.OBJECT, degree);
    }

    InferenceKind convert(NumericOperandType type) {
        switch (type) {
            case INT:
                return InferenceKind.INT;
            case LONG:
                return InferenceKind.LONG;
            case FLOAT:
                return InferenceKind.FLOAT;
            case DOUBLE:
                return InferenceKind.DOUBLE;
            default:
                throw new AssertionError();
        }
    }

    InstructionReader reader = new AbstractInstructionReader() {
        @Override
        public void unwrapArray(VariableReader receiver, VariableReader array, ArrayElementType elementType) {
            builder.addEdge(array.getIndex(), receiver.getIndex());
        }

        @Override
        public void stringConstant(VariableReader receiver, String cst) {
            types[receiver.getIndex()] = new InferenceType(InferenceKind.OBJECT, 0);
        }

        @Override
        public void nullCheck(VariableReader receiver, VariableReader value) {
            builder.addEdge(value.getIndex(), receiver.getIndex());
        }

        @Override
        public void negate(VariableReader receiver, VariableReader operand, NumericOperandType type) {
            types[receiver.getIndex()] = new InferenceType(convert(type), 0);
        }

        @Override
        public void longConstant(VariableReader receiver, long cst) {
            types[receiver.getIndex()] = new InferenceType(InferenceKind.LONG, 0);
        }

        @Override
        public void isInstance(VariableReader receiver, VariableReader value, ValueType type) {
            types[receiver.getIndex()] = new InferenceType(InferenceKind.BYTE, 0);
        }

        @Override
        public void invoke(VariableReader receiver, VariableReader instance, MethodReference method,
                List<? extends VariableReader> arguments, InvocationType type) {
            if (receiver != null) {
                types[receiver.getIndex()] = convert(method.getReturnType());
            }
        }

        @Override
        public void invokeDynamic(VariableReader receiver, VariableReader instance, MethodDescriptor method,
                List<? extends VariableReader> arguments, MethodHandle bootstrapMethod,
                List<RuntimeConstant> bootstrapArguments) {
            if (receiver != null) {
                types[receiver.getIndex()] = convert(method.getResultType());
            }
        }

        @Override
        public void integerConstant(VariableReader receiver, int cst) {
            types[receiver.getIndex()] = new InferenceType(InferenceKind.INT, 0);
        }

        @Override
        public void getField(VariableReader receiver, VariableReader instance, FieldReference field,
                ValueType fieldType) {
            types[receiver.getIndex()] = convert(fieldType);
        }

        @Override
        public void getElement(VariableReader receiver, VariableReader array, VariableReader index,
                ArrayElementType type) {
            arrayElemBuilder.addEdge(array.getIndex(), receiver.getIndex());
        }

        @Override
        public void floatConstant(VariableReader receiver, float cst) {
            types[receiver.getIndex()] = new InferenceType(InferenceKind.FLOAT, 0);
        }

        @Override
        public void doubleConstant(VariableReader receiver, double cst) {
            types[receiver.getIndex()] = new InferenceType(InferenceKind.DOUBLE, 0);
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType,
                List<? extends VariableReader> dimensions) {
            types[receiver.getIndex()] = convert(ValueType.arrayOf(itemType));
        }

        @Override
        public void createArray(VariableReader receiver, ValueType itemType, VariableReader size) {
            types[receiver.getIndex()] = convert(ValueType.arrayOf(itemType));
        }

        @Override
        public void create(VariableReader receiver, String type) {
            types[receiver.getIndex()] =  new InferenceType(InferenceKind.OBJECT, 0);
        }

        @Override
        public void cloneArray(VariableReader receiver, VariableReader array) {
            builder.addEdge(array.getIndex(), receiver.getIndex());
        }

        @Override
        public void classConstant(VariableReader receiver, ValueType cst) {
            types[receiver.getIndex()] =  new InferenceType(InferenceKind.OBJECT, 0);
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, IntegerSubtype type,
                CastIntegerDirection targetType) {
            types[receiver.getIndex()] =  new InferenceType(InferenceKind.BYTE, 0);
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, NumericOperandType sourceType,
                NumericOperandType targetType) {
            types[receiver.getIndex()] = new InferenceType(convert(targetType), 0);
        }

        @Override
        public void cast(VariableReader receiver, VariableReader value, ValueType targetType) {
            types[receiver.getIndex()] = convert(targetType);
        }

        @Override
        public void binary(BinaryOperation op, VariableReader receiver, VariableReader first, VariableReader second,
                NumericOperandType type) {
            switch (op) {
                case COMPARE:
                    types[receiver.getIndex()] = new InferenceType(InferenceKind.INT, 0);
                    break;
                default:
                    types[receiver.getIndex()] = new InferenceType(convert(type), 0);
                    break;
            }
        }

        @Override
        public void assign(VariableReader receiver, VariableReader assignee) {
            builder.addEdge(assignee.getIndex(), receiver.getIndex());
        }

        @Override
        public void arrayLength(VariableReader receiver, VariableReader array) {
            types[receiver.getIndex()] = new InferenceType(InferenceKind.INT, 0);
        }
    };

    enum InferenceKind {
        BYTE,
        SHORT,
        CHAR,
        INT,
        LONG,
        FLOAT,
        DOUBLE,
        OBJECT,
    }

    static class InferenceType {
        final InferenceKind kind;
        final int degree;

        InferenceType(InferenceKind kind, int degree) {
            this.kind = kind;
            this.degree = degree;
        }
    }
}