/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.cache;

import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.teavm.javascript.ast.*;
import org.teavm.model.ValueType;

/**
 *
 * @author Alexey Andreev
 */
public class AstIO {
    private static BinaryOperation[] binaryOperations = BinaryOperation.values();
    private static UnaryOperation[] unaryOperations = UnaryOperation.values();
    private SymbolTable symbolTable;
    private SymbolTable fileTable;

    public void write(DataOutput output, RegularMethodNode method) throws IOException {
        output.writeInt(packModifiers(method.getModifiers()));
        output.writeShort(method.getVariables().size());
        for (int var : method.getVariables()) {
            output.writeShort(var);
        }
        output.writeShort(method.getParameterDebugNames().size());
        for (Set<String> debugNames : method.getParameterDebugNames()) {
            output.writeShort(debugNames.size());
            for (String debugName : debugNames) {
                output.writeUTF(debugName);
            }
        }
        try {
            method.getBody().acceptVisitor(new NodeWriter(output));
        } catch (IOExceptionWrapper e) {
            throw new IOException("Error writing method body", e.getCause());
        }
    }

    private int packModifiers(Set<NodeModifier> modifiers) {
        int packed = 0;
        for (NodeModifier modifier : modifiers) {
            packed |= 1 << modifier.ordinal();
        }
        return packed;
    }

    class NodeWriter implements ExprVisitor, StatementVisitor {
        private DataOutput output;

        public NodeWriter(DataOutput output) {
            super();
            this.output = output;
        }

        public void writeExpr(Expr expr) throws IOException {
            writeLocation(expr.getLocation());
            expr.acceptVisitor(this);
        }

        private void writeLocation(NodeLocation location) throws IOException {
            if (location == null || location.getFileName() == null) {
                output.writeShort(-1);
            } else {
                output.writeShort(fileTable.lookup(location.getFileName()));
                output.writeShort(location.getLine());
            }
        }

        private void writeSequence(List<Statement> sequence) throws IOException {
            output.writeShort(sequence.size());
            for (Statement part : sequence) {
                part.acceptVisitor(this);
            }
        }

        private void writeNullableString(String str) throws IOException {
            if (str == null) {
                output.writeBoolean(false);
            } else {
                output.writeBoolean(true);
                output.writeUTF(str);
            }
        }

        @Override
        public void visit(AssignmentStatement statement) {
            try {
                output.writeByte(statement.getLeftValue() != null ? 0 : 1);
                writeLocation(statement.getLocation());
                output.writeShort(statement.getDebugNames().size());
                for (String name : statement.getDebugNames()) {
                    output.writeUTF(name);
                }
                if (statement.getLeftValue() != null) {
                    writeExpr(statement.getLeftValue());
                }
                writeExpr(statement.getRightValue());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(SequentialStatement statement) {
            try {
                output.writeByte(2);
                writeSequence(statement.getSequence());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ConditionalStatement statement) {
            try {
                output.writeByte(3);
                writeExpr(statement.getCondition());
                writeSequence(statement.getConsequent());
                writeSequence(statement.getAlternative());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(SwitchStatement statement) {
            try {
                output.writeByte(4);
                writeNullableString(statement.getId());
                writeExpr(statement.getValue());
                output.writeShort(statement.getClauses().size());
                for (SwitchClause clause : statement.getClauses()) {
                    int[] conditions = clause.getConditions();
                    output.writeShort(conditions.length);
                    for (int condition : conditions) {
                        output.writeInt(condition);
                    }
                    writeSequence(clause.getBody());
                }
                writeSequence(statement.getDefaultClause());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(WhileStatement statement) {
            try {
                output.writeByte(statement.getCondition() != null ? 5 : 6);
                writeNullableString(statement.getId());
                if (statement.getCondition() != null) {
                    writeExpr(statement.getCondition());
                }
                writeSequence(statement.getBody());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(BlockStatement statement) {
            try {
                output.writeByte(7);
                writeNullableString(statement.getId());
                writeSequence(statement.getBody());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(BreakStatement statement) {
            try {
                output.writeByte(statement.getTarget() != null && statement.getTarget().getId() != null ? 8 : 9);
                writeLocation(statement.getLocation());
                if (statement.getTarget() != null && statement.getTarget().getId() != null) {
                    output.writeUTF(statement.getTarget().getId());
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ContinueStatement statement) {
            try {
                output.writeByte(statement.getTarget() != null && statement.getTarget().getId() != null ? 10 : 11);
                writeLocation(statement.getLocation());
                if (statement.getTarget() != null && statement.getTarget().getId() != null) {
                    output.writeUTF(statement.getTarget().getId());
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ReturnStatement statement) {
            try {
                output.writeByte(statement.getResult() != null ? 12 : 13);
                writeLocation(statement.getLocation());
                if (statement.getResult() != null) {
                    writeExpr(statement.getResult());
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ThrowStatement statement) {
            try {
                output.writeByte(14);
                writeLocation(statement.getLocation());
                writeExpr(statement.getException());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(InitClassStatement statement) {
            try {
                output.writeByte(15);
                writeLocation(statement.getLocation());
                output.writeUTF(statement.getClassName());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(TryCatchStatement statement) {
            try {
                output.writeByte(16);
                writeSequence(statement.getProtectedBody());
                output.writeInt(statement.getExceptionType() != null ?
                        symbolTable.lookup(statement.getExceptionType()) : -1);
                output.writeShort(statement.getExceptionVariable() != null ?
                        statement.getExceptionVariable().intValue() : -1);
                writeSequence(statement.getHandler());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(BinaryExpr expr) {
            try {
                output.writeByte(0);
                output.writeByte(expr.getOperation().ordinal());
                writeExpr(expr.getFirstOperand());
                writeExpr(expr.getSecondOperand());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(UnaryExpr expr) {
            try {
                output.writeByte(1);
                output.writeByte(expr.getOperation().ordinal());
                writeExpr(expr.getOperand());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ConditionalExpr expr) {
            try {
                output.writeByte(2);
                writeExpr(expr.getCondition());
                writeExpr(expr.getConsequent());
                writeExpr(expr.getAlternative());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ConstantExpr expr) {
            try {
                Object value = expr.getValue();
                if (value == null) {
                    output.writeByte(3);
                } else if (value instanceof Integer) {
                    output.writeByte(4);
                    output.writeInt((Integer)value);
                } else if (value instanceof Long) {
                    output.writeByte(5);
                    output.writeLong((Long)value);
                } else if (value instanceof Float) {
                    output.writeByte(6);
                    output.writeFloat((Float)value);
                } else if (value instanceof Double) {
                    output.writeByte(7);
                    output.writeDouble((Double)value);
                } else if (value instanceof String) {
                    output.writeByte(8);
                    output.writeUTF((String)value);
                } else if (value instanceof ValueType) {
                    output.writeByte(9);
                    output.writeInt(symbolTable.lookup(value.toString()));
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(VariableExpr expr) {
            try {
                output.writeByte(10);
                output.writeShort(expr.getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(SubscriptExpr expr) {
            try {
                output.writeByte(11);
                writeExpr(expr.getArray());
                writeExpr(expr.getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(UnwrapArrayExpr expr) {
            try {
                output.writeByte(12);
                output.writeByte(expr.getElementType().ordinal());
                writeExpr(expr.getArray());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(InvocationExpr expr) {
            try {
                switch (expr.getType()) {
                    case CONSTRUCTOR:
                        output.writeByte(13);
                        break;
                    case STATIC:
                        output.writeByte(14);
                        break;
                    case SPECIAL:
                        output.writeByte(15);
                        break;
                    case DYNAMIC:
                        output.writeByte(16);
                        break;
                }
                output.writeInt(symbolTable.lookup(expr.getMethod().getClassName()));
                output.writeInt(symbolTable.lookup(expr.getMethod().getDescriptor().toString()));
                output.writeShort(expr.getArguments().size());
                for (int i = 0; i < expr.getArguments().size(); ++i) {
                    writeExpr(expr.getArguments().get(i));
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(QualificationExpr expr) {
            try {
                output.writeByte(17);
                writeExpr(expr.getQualified());
                output.writeInt(symbolTable.lookup(expr.getField().getClassName()));
                output.writeInt(symbolTable.lookup(expr.getField().getFieldName()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(NewExpr expr) {
            try {
                output.writeByte(18);
                output.writeInt(symbolTable.lookup(expr.getConstructedClass()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(NewArrayExpr expr) {
            try {
                output.writeByte(19);
                writeExpr(expr.getLength());
                output.writeInt(symbolTable.lookup(expr.getType().toString()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(NewMultiArrayExpr expr) {
            try {
                output.writeByte(20);
                output.writeByte(expr.getDimensions().size());
                for (Expr dimension : expr.getDimensions()) {
                    writeExpr(dimension);
                }
                output.writeInt(symbolTable.lookup(expr.getType().toString()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(InstanceOfExpr expr) {
            try {
                output.writeByte(21);
                writeExpr(expr.getExpr());
                output.writeInt(symbolTable.lookup(expr.getType().toString()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(StaticClassExpr expr) {
            try {
                output.writeByte(22);
                output.writeInt(symbolTable.lookup(expr.getType().toString()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }
    }

    static class IOExceptionWrapper extends RuntimeException {
        private static final long serialVersionUID = -7566355431593608333L;

        public IOExceptionWrapper(Throwable cause) {
            super(cause);
        }
    }
}
