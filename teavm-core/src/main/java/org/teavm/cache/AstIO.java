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
import java.util.*;
import org.teavm.javascript.ast.*;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.ArrayElementType;

/**
 *
 * @author Alexey Andreev
 */
public class AstIO {
    private static NodeModifier[] nodeModifiers = NodeModifier.values();
    private static BinaryOperation[] binaryOperations = BinaryOperation.values();
    private static UnaryOperation[] unaryOperations = UnaryOperation.values();
    private static ArrayElementType[] arrayElementTypes = ArrayElementType.values();
    private SymbolTable symbolTable;
    private SymbolTable fileTable;
    private Map<String, IdentifiedStatement> statementMap = new HashMap<>();

    public AstIO(SymbolTable symbolTable, SymbolTable fileTable) {
        this.symbolTable = symbolTable;
        this.fileTable = fileTable;
    }

    public void write(DataOutput output, RegularMethodNode method) throws IOException {
        output.writeInt(packModifiers(method.getModifiers()));
        output.writeShort(method.getVariables().size());
        for (int var : method.getVariables()) {
            output.writeShort(var);
        }
        output.writeShort(method.getParameterDebugNames().size());
        for (Set<String> debugNames : method.getParameterDebugNames()) {
            output.writeShort(debugNames != null ? debugNames.size() : 0);
            if (debugNames != null) {
                for (String debugName : debugNames) {
                    output.writeUTF(debugName);
                }
            }
        }
        try {
            method.getBody().acceptVisitor(new NodeWriter(output));
        } catch (IOExceptionWrapper e) {
            throw new IOException("Error writing method body", e.getCause());
        }
    }

    public RegularMethodNode read(DataInput input, MethodReference method) throws IOException {
        RegularMethodNode node = new RegularMethodNode(method);
        node.getModifiers().addAll(unpackModifiers(input.readInt()));
        int varCount = input.readShort();
        for (int i = 0; i < varCount; ++i) {
            node.getVariables().add((int)input.readShort());
        }
        int paramDebugNameCount = input.readShort();
        for (int i = 0; i < paramDebugNameCount; ++i) {
            int debugNameCount = input.readShort();
            Set<String> debugNames = new HashSet<>();
            for (int j = 0; j < debugNameCount; ++j) {
                debugNames.add(input.readUTF());
            }
            node.getParameterDebugNames().add(debugNames);
        }
        node.setBody(readStatement(input));
        return node;
    }

    public void writeAsync(DataOutput output, AsyncMethodNode method) throws IOException {
        output.writeInt(packModifiers(method.getModifiers()));
        output.writeShort(method.getVariables().size());
        for (int var : method.getVariables()) {
            output.writeShort(var);
        }
        output.writeShort(method.getParameterDebugNames().size());
        for (Set<String> debugNames : method.getParameterDebugNames()) {
            output.writeShort(debugNames != null ? debugNames.size() : 0);
            if (debugNames != null) {
                for (String debugName : debugNames) {
                    output.writeUTF(debugName);
                }
            }
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
            node.getVariables().add((int)input.readShort());
        }
        int paramDebugNameCount = input.readShort();
        for (int i = 0; i < paramDebugNameCount; ++i) {
            int debugNameCount = input.readShort();
            Set<String> debugNames = new HashSet<>();
            for (int j = 0; j < debugNameCount; ++j) {
                debugNames.add(input.readUTF());
            }
            node.getParameterDebugNames().add(debugNames);
        }
        int partCount = input.readShort();
        for (int i = 0; i < partCount; ++i) {
            AsyncMethodPart part = new AsyncMethodPart();
            part.setStatement(readStatement(input));
            node.getBody().add(part);
        }
        return node;
    }

    private int packModifiers(Set<NodeModifier> modifiers) {
        int packed = 0;
        for (NodeModifier modifier : modifiers) {
            packed |= 1 << modifier.ordinal();
        }
        return packed;
    }

    private Set<NodeModifier> unpackModifiers(int packed) {
        EnumSet<NodeModifier> modifiers = EnumSet.noneOf(NodeModifier.class);
        while (packed != 0) {
            int shift = Integer.numberOfTrailingZeros(packed);
            modifiers.add(nodeModifiers[shift]);
            packed ^= 1 << shift;
        }
        return modifiers;
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

    private NodeLocation readLocation(DataInput input) throws IOException {
        int fileIndex = input.readShort();
        if (fileIndex == -1) {
            return null;
        } else {
            return new NodeLocation(fileTable.at(fileIndex), input.readShort());
        }
    }

    private Statement readStatement(DataInput input) throws IOException {
        byte type = input.readByte();
        switch (type) {
            case 0: {
                AssignmentStatement stmt = new AssignmentStatement();
                stmt.setLocation(readLocation(input));
                int debugNameCount = input.readShort();
                for (int i = 0; i < debugNameCount; ++i) {
                    stmt.getDebugNames().add(input.readUTF());
                }
                stmt.setLeftValue(readExpr(input));
                stmt.setRightValue(readExpr(input));
                stmt.setAsync(input.readBoolean());
                return stmt;
            }
            case 1: {
                AssignmentStatement stmt = new AssignmentStatement();
                stmt.setLocation(readLocation(input));
                int debugNameCount = input.readShort();
                for (int i = 0; i < debugNameCount; ++i) {
                    stmt.getDebugNames().add(input.readUTF());
                }
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
            // TODO: MonitorEnter/MonitorExit
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
        NodeLocation location = readLocation(input);
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
                expr.setFirstOperand(readExpr(input));
                expr.setSecondOperand(readExpr(input));
                return expr;
            }
            case 1: {
                UnaryExpr expr = new UnaryExpr();
                expr.setOperation(unaryOperations[input.readByte()]);
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
                ConstantExpr expr = new ConstantExpr();
                return expr;
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
                return expr;
            }
            case 12: {
                UnwrapArrayExpr expr = new UnwrapArrayExpr(arrayElementTypes[input.readByte()]);
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
                expr.setQualified(readExpr(input));
                String className = symbolTable.at(input.readInt());
                String fieldName = symbolTable.at(input.readInt());
                expr.setField(new FieldReference(className, fieldName));
                return expr;
            }
            case 18: {
                NewExpr expr = new NewExpr();
                expr.setConstructedClass(symbolTable.at(input.readInt()));
                return expr;
            }
            case 19: {
                NewArrayExpr expr = new NewArrayExpr();
                expr.setLength(readExpr(input));
                expr.setType(ValueType.parse(symbolTable.at(input.readInt())));
                return expr;
            }
            case 20: {
                NewMultiArrayExpr expr = new NewMultiArrayExpr();
                int dimensionCount = input.readByte();
                for (int i = 0; i < dimensionCount; ++i) {
                    expr.getDimensions().add(readExpr(input));
                }
                expr.setType(ValueType.parse(symbolTable.at(input.readInt())));
                return expr;
            }
            case 21: {
                InstanceOfExpr expr = new InstanceOfExpr();
                expr.setExpr(readExpr(input));
                expr.setType(ValueType.parse(symbolTable.at(input.readInt())));
                return expr;
            }
            case 22: {
                StaticClassExpr expr = new StaticClassExpr();
                expr.setType(ValueType.parse(symbolTable.at(input.readInt())));
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

    static class IOExceptionWrapper extends RuntimeException {
        private static final long serialVersionUID = -7566355431593608333L;

        public IOExceptionWrapper(Throwable cause) {
            super(cause);
        }
    }
}
