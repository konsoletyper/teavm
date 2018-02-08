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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.ast.ArrayType;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.AsyncMethodNode;
import org.teavm.ast.AsyncMethodPart;
import org.teavm.ast.BinaryExpr;
import org.teavm.ast.BinaryOperation;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.BreakStatement;
import org.teavm.ast.CastExpr;
import org.teavm.ast.ConditionalExpr;
import org.teavm.ast.ConditionalStatement;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.ContinueStatement;
import org.teavm.ast.Expr;
import org.teavm.ast.ExprVisitor;
import org.teavm.ast.GotoPartStatement;
import org.teavm.ast.IdentifiedStatement;
import org.teavm.ast.InitClassStatement;
import org.teavm.ast.InstanceOfExpr;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.InvocationType;
import org.teavm.ast.MonitorEnterStatement;
import org.teavm.ast.MonitorExitStatement;
import org.teavm.ast.NewArrayExpr;
import org.teavm.ast.NewExpr;
import org.teavm.ast.NewMultiArrayExpr;
import org.teavm.ast.OperationType;
import org.teavm.ast.PrimitiveCastExpr;
import org.teavm.ast.QualificationExpr;
import org.teavm.ast.RegularMethodNode;
import org.teavm.ast.ReturnStatement;
import org.teavm.ast.SequentialStatement;
import org.teavm.ast.Statement;
import org.teavm.ast.StatementVisitor;
import org.teavm.ast.SubscriptExpr;
import org.teavm.ast.SwitchClause;
import org.teavm.ast.SwitchStatement;
import org.teavm.ast.ThrowStatement;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.UnaryExpr;
import org.teavm.ast.UnaryOperation;
import org.teavm.ast.UnwrapArrayExpr;
import org.teavm.ast.VariableExpr;
import org.teavm.ast.VariableNode;
import org.teavm.ast.WhileStatement;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.util.VariableType;

public class AstIO {
    private static final ElementModifier[] nodeModifiers = ElementModifier.values();
    private static final BinaryOperation[] binaryOperations = BinaryOperation.values();
    private static final UnaryOperation[] unaryOperations = UnaryOperation.values();
    private final SymbolTable symbolTable;
    private final SymbolTable fileTable;
    private final Map<String, IdentifiedStatement> statementMap = new HashMap<>();

    public AstIO(SymbolTable symbolTable, SymbolTable fileTable) {
        this.symbolTable = symbolTable;
        this.fileTable = fileTable;
    }

    public void write(DataOutput output, RegularMethodNode method) throws IOException {
        output.writeInt(ElementModifier.pack(method.getModifiers()));
        output.writeShort(method.getVariables().size());
        for (VariableNode var : method.getVariables()) {
            write(output, var);
        }
        try {
            method.getBody().acceptVisitor(new NodeWriter(output));
        } catch (IOExceptionWrapper e) {
            throw new IOException("Error writing method body", e.getCause());
        }
    }

    private void write(DataOutput output, VariableNode variable) throws IOException {
        output.writeShort(variable.getIndex());
        output.writeByte(variable.getType().ordinal());
        output.writeUTF(variable.getName() != null ? variable.getName() : "");
    }

    public RegularMethodNode read(DataInput input, MethodReference method) throws IOException {
        RegularMethodNode node = new RegularMethodNode(method);
        node.getModifiers().addAll(unpackModifiers(input.readInt()));
        int varCount = input.readShort();
        for (int i = 0; i < varCount; ++i) {
            node.getVariables().add(readVariable(input));
        }
        node.setBody(readStatement(input));
        return node;
    }

    private VariableNode readVariable(DataInput input) throws IOException {
        int index = input.readShort();
        VariableType type = VariableType.values()[input.readByte()];
        VariableNode variable = new VariableNode(index, type);
        int nameCount = input.readByte();
        for (int i = 0; i < nameCount; ++i) {
            variable.setName(input.readUTF());
            if (variable.getName().isEmpty()) {
                variable.setName(null);
            }
        }
        return variable;
    }

    public void writeAsync(DataOutput output, AsyncMethodNode method) throws IOException {
        output.writeInt(ElementModifier.pack(method.getModifiers()));
        output.writeShort(method.getVariables().size());
        for (VariableNode var : method.getVariables()) {
            write(output, var);
        }
        try {
             output.writeShort(method.getBody().size());
             for (int i = 0; i < method.getBody().size(); ++i) {
                 method.getBody().get(i).getStatement().acceptVisitor(new NodeWriter(output));
             }
        } catch (IOExceptionWrapper e) {
            throw new IOException("Error writing method body", e.getCause());
        }
    }

    public AsyncMethodNode readAsync(DataInput input, MethodReference method) throws IOException {
        AsyncMethodNode node = new AsyncMethodNode(method);
        node.getModifiers().addAll(unpackModifiers(input.readInt()));
        int varCount = input.readShort();
        for (int i = 0; i < varCount; ++i) {
            node.getVariables().add(readVariable(input));
        }
        int partCount = input.readShort();
        for (int i = 0; i < partCount; ++i) {
            AsyncMethodPart part = new AsyncMethodPart();
            part.setStatement(readStatement(input));
            node.getBody().add(part);
        }
        return node;
    }

    private Set<ElementModifier> unpackModifiers(int packed) {
        EnumSet<ElementModifier> modifiers = EnumSet.noneOf(ElementModifier.class);
        while (packed != 0) {
            int shift = Integer.numberOfTrailingZeros(packed);
            modifiers.add(nodeModifiers[shift]);
            packed ^= 1 << shift;
        }
        return modifiers;
    }

    private class NodeWriter implements ExprVisitor, StatementVisitor {
        private final DataOutput output;

        NodeWriter(DataOutput output) {
            super();
            this.output = output;
        }

        void writeExpr(Expr expr) throws IOException {
            writeLocation(expr.getLocation());
            expr.acceptVisitor(this);
        }

        private void writeLocation(TextLocation location) throws IOException {
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
                if (statement.getLeftValue() != null) {
                    writeExpr(statement.getLeftValue());
                }
                writeExpr(statement.getRightValue());
                output.writeBoolean(statement.isAsync());
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
                output.writeInt(symbolTable.lookup(statement.getClassName()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(TryCatchStatement statement) {
            try {
                output.writeByte(16);
                writeSequence(statement.getProtectedBody());
                output.writeInt(statement.getExceptionType() != null
                        ? symbolTable.lookup(statement.getExceptionType()) : -1);
                output.writeShort(statement.getExceptionVariable() != null
                        ? statement.getExceptionVariable() : -1);
                writeSequence(statement.getHandler());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(GotoPartStatement statement) {
            try {
                output.writeByte(17);
                output.writeShort(statement.getPart());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(MonitorEnterStatement statement) {
            try {
                output.writeByte(18);
                writeLocation(statement.getLocation());
                writeExpr(statement.getObjectRef());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(MonitorExitStatement statement) {
            try {
                output.writeByte(19);
                writeLocation(statement.getLocation());
                writeExpr(statement.getObjectRef());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(BinaryExpr expr) {
            try {
                output.writeByte(0);
                output.writeByte(expr.getOperation().ordinal());
                output.writeByte(expr.getType() != null ? expr.getType().ordinal() + 1 : 0);
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
                output.writeByte(expr.getType() != null ? expr.getType().ordinal() + 1 : 0);
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
                    output.writeInt((Integer) value);
                } else if (value instanceof Long) {
                    output.writeByte(5);
                    output.writeLong((Long) value);
                } else if (value instanceof Float) {
                    output.writeByte(6);
                    output.writeFloat((Float) value);
                } else if (value instanceof Double) {
                    output.writeByte(7);
                    output.writeDouble((Double) value);
                } else if (value instanceof String) {
                    output.writeByte(8);
                    output.writeUTF((String) value);
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
                output.writeByte(expr.getType().ordinal());
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
                output.writeByte(expr.getQualified() != null ? 17 : 18);
                if (expr.getQualified() != null) {
                    writeExpr(expr.getQualified());
                }
                output.writeInt(symbolTable.lookup(expr.getField().getClassName()));
                output.writeInt(symbolTable.lookup(expr.getField().getFieldName()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(NewExpr expr) {
            try {
                output.writeByte(19);
                output.writeInt(symbolTable.lookup(expr.getConstructedClass()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(NewArrayExpr expr) {
            try {
                output.writeByte(20);
                writeExpr(expr.getLength());
                output.writeInt(symbolTable.lookup(expr.getType().toString()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(NewMultiArrayExpr expr) {
            try {
                output.writeByte(21);
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
                output.writeByte(22);
                writeExpr(expr.getExpr());
                output.writeInt(symbolTable.lookup(expr.getType().toString()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(CastExpr expr) {
            try {
                output.writeByte(23);
                output.writeInt(symbolTable.lookup(expr.getTarget().toString()));
                writeExpr(expr.getValue());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(PrimitiveCastExpr expr) {
            try {
                output.writeByte(24);
                output.writeByte(expr.getSource().ordinal());
                output.writeByte(expr.getTarget().ordinal());
                writeExpr(expr.getValue());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }
    }

    private TextLocation readLocation(DataInput input) throws IOException {
        int fileIndex = input.readShort();
        if (fileIndex == -1) {
            return null;
        } else {
            return new TextLocation(fileTable.at(fileIndex), input.readShort());
        }
    }

    private Statement readStatement(DataInput input) throws IOException {
        byte type = input.readByte();
        switch (type) {
            case 0: {
                AssignmentStatement stmt = new AssignmentStatement();
                stmt.setLocation(readLocation(input));
                stmt.setLeftValue(readExpr(input));
                stmt.setRightValue(readExpr(input));
                stmt.setAsync(input.readBoolean());
                return stmt;
            }
            case 1: {
                AssignmentStatement stmt = new AssignmentStatement();
                stmt.setLocation(readLocation(input));
                stmt.setRightValue(readExpr(input));
                stmt.setAsync(input.readBoolean());
                return stmt;
            }
            case 2: {
                SequentialStatement stmt = new SequentialStatement();
                readSequence(input, stmt.getSequence());
                return stmt;
            }
            case 3: {
                ConditionalStatement stmt = new ConditionalStatement();
                stmt.setCondition(readExpr(input));
                readSequence(input, stmt.getConsequent());
                readSequence(input, stmt.getAlternative());
                return stmt;
            }
            case 4: {
                SwitchStatement stmt = new SwitchStatement();
                stmt.setId(readNullableString(input));
                stmt.setValue(readExpr(input));
                int clauseCount = input.readShort();
                for (int i = 0; i < clauseCount; ++i) {
                    SwitchClause clause = new SwitchClause();
                    int conditionCount = input.readShort();
                    int[] conditions = new int[conditionCount];
                    for (int j = 0; j < conditionCount; ++j) {
                        conditions[j] = input.readInt();
                    }
                    clause.setConditions(conditions);
                    readSequence(input, clause.getBody());
                    stmt.getClauses().add(clause);
                }
                readSequence(input, stmt.getDefaultClause());
                return stmt;
            }
            case 5: {
                WhileStatement stmt = new WhileStatement();
                stmt.setId(readNullableString(input));
                stmt.setCondition(readExpr(input));
                if (stmt.getId() != null) {
                    statementMap.put(stmt.getId(), stmt);
                }
                readSequence(input, stmt.getBody());
                return stmt;
            }
            case 6: {
                WhileStatement stmt = new WhileStatement();
                stmt.setId(readNullableString(input));
                if (stmt.getId() != null) {
                    statementMap.put(stmt.getId(), stmt);
                }
                readSequence(input, stmt.getBody());
                return stmt;
            }
            case 7: {
                BlockStatement stmt = new BlockStatement();
                stmt.setId(readNullableString(input));
                if (stmt.getId() != null) {
                    statementMap.put(stmt.getId(), stmt);
                }
                readSequence(input, stmt.getBody());
                return stmt;
            }
            case 8: {
                BreakStatement stmt = new BreakStatement();
                stmt.setLocation(readLocation(input));
                stmt.setTarget(statementMap.get(input.readUTF()));
                return stmt;
            }
            case 9: {
                BreakStatement stmt = new BreakStatement();
                stmt.setLocation(readLocation(input));
                return stmt;
            }
            case 10: {
                ContinueStatement stmt = new ContinueStatement();
                stmt.setLocation(readLocation(input));
                stmt.setTarget(statementMap.get(input.readUTF()));
                return stmt;
            }
            case 11: {
                ContinueStatement stmt = new ContinueStatement();
                stmt.setLocation(readLocation(input));
                return stmt;
            }
            case 12: {
                ReturnStatement stmt = new ReturnStatement();
                stmt.setLocation(readLocation(input));
                stmt.setResult(readExpr(input));
                return stmt;
            }
            case 13: {
                ReturnStatement stmt = new ReturnStatement();
                stmt.setLocation(readLocation(input));
                return stmt;
            }
            case 14: {
                ThrowStatement stmt = new ThrowStatement();
                stmt.setLocation(readLocation(input));
                stmt.setException(readExpr(input));
                return stmt;
            }
            case 15: {
                InitClassStatement stmt = new InitClassStatement();
                stmt.setLocation(readLocation(input));
                stmt.setClassName(symbolTable.at(input.readInt()));
                return stmt;
            }
            case 16: {
                TryCatchStatement stmt = new TryCatchStatement();
                readSequence(input, stmt.getProtectedBody());
                int exceptionTypeIndex = input.readInt();
                if (exceptionTypeIndex >= 0) {
                    stmt.setExceptionType(symbolTable.at(exceptionTypeIndex));
                }
                int exceptionVarIndex = input.readShort();
                if (exceptionVarIndex >= 0) {
                    stmt.setExceptionVariable(exceptionVarIndex);
                }
                readSequence(input, stmt.getHandler());
                return stmt;
            }
            case 17: {
                GotoPartStatement stmt = new GotoPartStatement();
                stmt.setPart(input.readShort());
                return stmt;
            }
            case 18: {
                MonitorEnterStatement stmt = new MonitorEnterStatement();
                stmt.setLocation(readLocation(input));
                stmt.setObjectRef(readExpr(input));
                return stmt;
            }
            case 19: {
                MonitorExitStatement stmt = new MonitorExitStatement();
                stmt.setLocation(readLocation(input));
                stmt.setObjectRef(readExpr(input));
                return stmt;
            }
            default:
                throw new RuntimeException("Unexpected statement type: " + type);
        }
    }

    private void readSequence(DataInput input, List<Statement> statements) throws IOException {
        int count = input.readShort();
        for (int i = 0; i < count; ++i) {
            statements.add(readStatement(input));
        }
    }

    private String readNullableString(DataInput input) throws IOException {
        return input.readBoolean() ? input.readUTF() : null;
    }

    private Expr readExpr(DataInput input) throws IOException {
        TextLocation location = readLocation(input);
        Expr expr = readExprWithoutLocation(input);
        expr.setLocation(location);
        return expr;
    }

    private Expr readExprWithoutLocation(DataInput input) throws IOException {
        byte type = input.readByte();
        switch (type) {
            case 0: {
                BinaryExpr expr = new BinaryExpr();
                expr.setOperation(binaryOperations[input.readByte()]);
                byte valueType = input.readByte();
                expr.setType(valueType > 0 ? OperationType.values()[valueType] : null);
                expr.setFirstOperand(readExpr(input));
                expr.setSecondOperand(readExpr(input));
                return expr;
            }
            case 1: {
                UnaryExpr expr = new UnaryExpr();
                expr.setOperation(unaryOperations[input.readByte()]);
                byte valueType = input.readByte();
                expr.setType(valueType > 0 ? OperationType.values()[valueType] : null);
                expr.setOperand(readExpr(input));
                return expr;
            }
            case 2: {
                ConditionalExpr expr = new ConditionalExpr();
                expr.setCondition(readExpr(input));
                expr.setConsequent(readExpr(input));
                expr.setAlternative(readExpr(input));
                return expr;
            }
            case 3: {
                return new ConstantExpr();
            }
            case 4: {
                ConstantExpr expr = new ConstantExpr();
                expr.setValue(input.readInt());
                return expr;
            }
            case 5: {
                ConstantExpr expr = new ConstantExpr();
                expr.setValue(input.readLong());
                return expr;
            }
            case 6: {
                ConstantExpr expr = new ConstantExpr();
                expr.setValue(input.readFloat());
                return expr;
            }
            case 7: {
                ConstantExpr expr = new ConstantExpr();
                expr.setValue(input.readDouble());
                return expr;
            }
            case 8: {
                ConstantExpr expr = new ConstantExpr();
                expr.setValue(input.readUTF());
                return expr;
            }
            case 9: {
                ConstantExpr expr = new ConstantExpr();
                expr.setValue(ValueType.parse(symbolTable.at(input.readInt())));
                return expr;
            }
            case 10: {
                VariableExpr expr = new VariableExpr();
                expr.setIndex(input.readShort());
                return expr;
            }
            case 11: {
                SubscriptExpr expr = new SubscriptExpr();
                expr.setArray(readExpr(input));
                expr.setIndex(readExpr(input));
                expr.setType(ArrayType.values()[input.readByte()]);
                return expr;
            }
            case 12: {
                UnwrapArrayExpr expr = new UnwrapArrayExpr(ArrayType.values()[input.readByte()]);
                expr.setArray(readExpr(input));
                return expr;
            }
            case 13:
                return parseInvocationExpr(InvocationType.CONSTRUCTOR, input);
            case 14:
                return parseInvocationExpr(InvocationType.STATIC, input);
            case 15:
                return parseInvocationExpr(InvocationType.SPECIAL, input);
            case 16:
                return parseInvocationExpr(InvocationType.DYNAMIC, input);
            case 17: {
                QualificationExpr expr = new QualificationExpr();
                String className = symbolTable.at(input.readInt());
                String fieldName = symbolTable.at(input.readInt());
                expr.setField(new FieldReference(className, fieldName));
                return expr;
            }
            case 18: {
                QualificationExpr expr = new QualificationExpr();
                expr.setQualified(readExpr(input));
                String className = symbolTable.at(input.readInt());
                String fieldName = symbolTable.at(input.readInt());
                expr.setField(new FieldReference(className, fieldName));
                return expr;
            }
            case 19: {
                NewExpr expr = new NewExpr();
                expr.setConstructedClass(symbolTable.at(input.readInt()));
                return expr;
            }
            case 20: {
                NewArrayExpr expr = new NewArrayExpr();
                expr.setLength(readExpr(input));
                expr.setType(ValueType.parse(symbolTable.at(input.readInt())));
                return expr;
            }
            case 21: {
                NewMultiArrayExpr expr = new NewMultiArrayExpr();
                int dimensionCount = input.readByte();
                for (int i = 0; i < dimensionCount; ++i) {
                    expr.getDimensions().add(readExpr(input));
                }
                expr.setType(ValueType.parse(symbolTable.at(input.readInt())));
                return expr;
            }
            case 22: {
                InstanceOfExpr expr = new InstanceOfExpr();
                expr.setExpr(readExpr(input));
                expr.setType(ValueType.parse(symbolTable.at(input.readInt())));
                return expr;
            }
            case 23: {
                CastExpr expr = new CastExpr();
                expr.setTarget(ValueType.parse(symbolTable.at(input.readInt())));
                expr.setValue(readExpr(input));
                return expr;
            }
            case 24: {
                PrimitiveCastExpr expr = new PrimitiveCastExpr();
                expr.setSource(OperationType.values()[input.readByte()]);
                expr.setTarget(OperationType.values()[input.readByte()]);
                expr.setValue(readExpr(input));
                return expr;
            }
            default:
                throw new RuntimeException("Unknown expression type: " + type);
        }
    }

    private InvocationExpr parseInvocationExpr(InvocationType invocationType, DataInput input) throws IOException {
        InvocationExpr expr = new InvocationExpr();
        expr.setType(invocationType);
        String className = symbolTable.at(input.readInt());
        MethodDescriptor method = MethodDescriptor.parse(symbolTable.at(input.readInt()));
        expr.setMethod(new MethodReference(className, method));
        int argCount = input.readShort();
        for (int i = 0; i < argCount; ++i) {
            expr.getArguments().add(readExpr(input));
        }
        return expr;
    }

    private static class IOExceptionWrapper extends RuntimeException {
        private static final long serialVersionUID = -7566355431593608333L;

        IOExceptionWrapper(Throwable cause) {
            super(cause);
        }
    }
}
