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

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.teavm.ast.ArrayFromDataExpr;
import org.teavm.ast.ArrayType;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.AsyncMethodNode;
import org.teavm.ast.AsyncMethodPart;
import org.teavm.ast.BinaryExpr;
import org.teavm.ast.BinaryOperation;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.BoundCheckExpr;
import org.teavm.ast.BreakStatement;
import org.teavm.ast.CastExpr;
import org.teavm.ast.ConditionalExpr;
import org.teavm.ast.ConditionalStatement;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.ContinueStatement;
import org.teavm.ast.ControlFlowEntry;
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
import org.teavm.model.InliningInfo;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReference;
import org.teavm.model.ReferenceCache;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.util.VariableType;

public class AstIO {
    private static final ElementModifier[] nodeModifiers = ElementModifier.values();
    private static final BinaryOperation[] binaryOperations = BinaryOperation.values();
    private static final UnaryOperation[] unaryOperations = UnaryOperation.values();
    private final SymbolTable symbolTable;
    private final SymbolTable fileTable;
    private final SymbolTable variableTable;
    private final Map<String, IdentifiedStatement> statementMap = new HashMap<>();
    private ReferenceCache referenceCache;
    private TextLocation lastWrittenLocation;
    private TextLocation lastReadLocation;
    private InliningInfo lastReadInlining;

    public AstIO(ReferenceCache referenceCache, SymbolTable symbolTable, SymbolTable fileTable,
            SymbolTable variableTable) {
        this.referenceCache = referenceCache;
        this.symbolTable = symbolTable;
        this.fileTable = fileTable;
        this.variableTable = variableTable;
    }

    public void write(VarDataOutput output, ControlFlowEntry[] cfg) throws IOException {
        lastWrittenLocation = TextLocation.EMPTY;
        output.writeUnsigned(cfg.length);
        for (ControlFlowEntry entry : cfg) {
            writeLocation(output, entry.from);
            output.writeUnsigned(entry.to.length);
            for (TextLocation loc : entry.to) {
                writeLocation(output, loc);
            }
        }
    }

    public void write(VarDataOutput output, RegularMethodNode method) throws IOException {
        output.writeUnsigned(ElementModifier.pack(method.getModifiers()));
        output.writeUnsigned(method.getVariables().size());
        for (VariableNode var : method.getVariables()) {
            write(output, var);
        }
        try {
            method.getBody().acceptVisitor(new NodeWriter(output));
        } catch (IOExceptionWrapper e) {
            throw new IOException("Error writing method body", e.getCause());
        }
    }

    private void write(VarDataOutput output, VariableNode variable) throws IOException {
        output.writeUnsigned(variable.getIndex());
        output.writeUnsigned(variable.getType().ordinal());
        output.writeUnsigned(variable.getName() != null ? variableTable.lookup(variable.getName()) + 1 : 0);
    }

    public ControlFlowEntry[] readControlFlow(VarDataInput input) throws IOException {
        lastReadLocation = TextLocation.EMPTY;
        int size = input.readUnsigned();
        ControlFlowEntry[] result = new ControlFlowEntry[size];
        for (int i = 0; i < size; ++i) {
            TextLocation from = readLocation(input);
            int toSize = input.readUnsigned();
            TextLocation[] to = new TextLocation[toSize];
            for (int j = 0; j < toSize; ++j) {
                to[j] = readLocation(input);
            }
            result[i] = new ControlFlowEntry(from, to);
        }
        return result;
    }

    public RegularMethodNode read(VarDataInput input, MethodReference method) throws IOException {
        RegularMethodNode node = new RegularMethodNode(method);
        node.getModifiers().addAll(unpackModifiers(input.readUnsigned()));
        int varCount = input.readUnsigned();
        for (int i = 0; i < varCount; ++i) {
            node.getVariables().add(readVariable(input));
        }
        lastReadLocation = TextLocation.EMPTY;
        node.setBody(readStatement(input));
        return node;
    }

    private VariableNode readVariable(VarDataInput input) throws IOException {
        int index = input.readUnsigned();
        VariableType type = VariableType.values()[input.readUnsigned()];
        VariableNode variable = new VariableNode(index, type);
        int nameIndex = input.readUnsigned();
        variable.setName(nameIndex != 0 ? variableTable.at(nameIndex - 1) : null);
        return variable;
    }

    public void writeAsync(VarDataOutput output, AsyncMethodNode method) throws IOException {
        output.writeUnsigned(ElementModifier.pack(method.getModifiers()));
        output.writeUnsigned(method.getVariables().size());
        for (VariableNode var : method.getVariables()) {
            write(output, var);
        }
        try {
            output.writeUnsigned(method.getBody().size());
            NodeWriter writer = new NodeWriter(output);
            for (int i = 0; i < method.getBody().size(); ++i) {
                method.getBody().get(i).getStatement().acceptVisitor(writer);
            }
        } catch (IOExceptionWrapper e) {
            throw new IOException("Error writing method body", e.getCause());
        }
    }

    public AsyncMethodNode readAsync(VarDataInput input, MethodReference method) throws IOException {
        AsyncMethodNode node = new AsyncMethodNode(method);
        node.getModifiers().addAll(unpackModifiers(input.readUnsigned()));
        int varCount = input.readUnsigned();
        for (int i = 0; i < varCount; ++i) {
            node.getVariables().add(readVariable(input));
        }
        int partCount = input.readUnsigned();
        lastReadLocation = null;
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

    private void writeLocation(VarDataOutput output, TextLocation location) throws IOException {
        if (location == null) {
            location = TextLocation.EMPTY;
        }
        if (location.isEmpty()) {
            output.writeUnsigned(0);
        } else if (!lastWrittenLocation.isEmpty() && lastWrittenLocation.getFileName().equals(location.getFileName())) {
            output.writeUnsigned(1);
            output.writeSigned(location.getLine() - lastWrittenLocation.getLine());
        } else {
            output.writeUnsigned(fileTable.lookup(location.getFileName()) + 2);
            output.writeUnsigned(location.getLine());
        }
        lastWrittenLocation = location;
    }

    private class NodeWriter implements ExprVisitor, StatementVisitor {
        private final VarDataOutput output;
        private TextLocation lastLocation = TextLocation.EMPTY;

        NodeWriter(VarDataOutput output) {
            super();
            this.output = output;
        }

        void writeExpr(Expr expr) throws IOException {
            writeLocation(expr.getLocation());
            expr.acceptVisitor(this);
        }

        private void writeLocation(TextLocation location) throws IOException {
            if (location == null) {
                location = TextLocation.EMPTY;
            }
            if (Objects.equals(location, lastLocation)) {
                return;
            }

            String fileName = lastLocation.getFileName();
            int lineNumber = lastLocation.getLine();

            if (location.getInlining() != lastLocation.getInlining()) {
                InliningInfo lastCommonInlining = null;
                InliningInfo[] prevPath = lastLocation.getInliningPath();
                InliningInfo[] newPath = location.getInliningPath();
                int pathIndex = 0;
                while (pathIndex < prevPath.length && pathIndex < newPath.length
                        && prevPath[pathIndex].equals(newPath[pathIndex])) {
                    lastCommonInlining = prevPath[pathIndex++];
                }

                InliningInfo prevInlining = location.getInlining();
                while (prevInlining != lastCommonInlining) {
                    output.writeUnsigned(123);
                    fileName = prevInlining.getFileName();
                    lineNumber = prevInlining.getLine();
                    prevInlining = prevInlining.getParent();
                }

                while (pathIndex < newPath.length) {
                    InliningInfo inlining = newPath[pathIndex++];
                    writeSimpleLocation(fileName, lineNumber, inlining.getFileName(), inlining.getLine());
                    fileName = null;
                    lineNumber = -1;

                    output.writeUnsigned(124);
                    MethodReference method = inlining.getMethod();
                    output.writeUnsigned(symbolTable.lookup(method.getClassName()));
                    output.writeUnsigned(symbolTable.lookup(method.getDescriptor().toString()));
                }
            }

            writeSimpleLocation(fileName, lineNumber, location.getFileName(), location.getLine());

            lastLocation = location;
        }

        private void writeSimpleLocation(String fileName, int lineNumber, String newFileName, int newLineNumber)
                throws IOException {
            if (Objects.equals(fileName, newFileName) && lineNumber == newLineNumber) {
                return;
            }

            if (newFileName == null) {
                output.writeUnsigned(127);
            } else if (fileName != null && fileName.equals(newFileName)) {
                output.writeUnsigned(126);
                output.writeSigned(newLineNumber - lineNumber);
            } else {
                output.writeUnsigned(125);
                output.writeUnsigned(fileTable.lookup(newFileName));
                output.writeUnsigned(newLineNumber);
            }
        }

        private void writeSequence(List<Statement> sequence) throws IOException {
            output.writeUnsigned(sequence.size());
            for (Statement part : sequence) {
                part.acceptVisitor(this);
            }
        }

        @Override
        public void visit(AssignmentStatement statement) {
            try {
                writeLocation(statement.getLocation());
                output.writeUnsigned(statement.getLeftValue() != null ? 0 : 1);
                if (statement.getLeftValue() != null) {
                    writeExpr(statement.getLeftValue());
                }
                writeExpr(statement.getRightValue());
                output.writeUnsigned(statement.isAsync() ? 1 : 0);
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(SequentialStatement statement) {
            try {
                output.writeUnsigned(2);
                writeSequence(statement.getSequence());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ConditionalStatement statement) {
            try {
                output.writeUnsigned(3);
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
                output.writeUnsigned(4);
                output.write(statement.getId());
                writeExpr(statement.getValue());
                output.writeUnsigned(statement.getClauses().size());
                for (SwitchClause clause : statement.getClauses()) {
                    int[] conditions = clause.getConditions();
                    output.writeUnsigned(conditions.length);
                    for (int condition : conditions) {
                        output.writeSigned(condition);
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
                output.writeUnsigned(statement.getCondition() != null ? 5 : 6);
                output.write(statement.getId());
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
                output.writeUnsigned(7);
                output.write(statement.getId());
                writeSequence(statement.getBody());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(BreakStatement statement) {
            try {
                writeLocation(statement.getLocation());
                output.writeUnsigned(statement.getTarget() != null && statement.getTarget().getId() != null ? 8 : 9);
                if (statement.getTarget() != null && statement.getTarget().getId() != null) {
                    output.write(statement.getTarget().getId());
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ContinueStatement statement) {
            try {
                writeLocation(statement.getLocation());
                output.writeUnsigned(statement.getTarget() != null && statement.getTarget().getId() != null ? 10 : 11);
                if (statement.getTarget() != null && statement.getTarget().getId() != null) {
                    output.write(statement.getTarget().getId());
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ReturnStatement statement) {
            try {
                writeLocation(statement.getLocation());
                output.writeUnsigned(statement.getResult() != null ? 12 : 13);
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
                writeLocation(statement.getLocation());
                output.writeUnsigned(14);
                writeExpr(statement.getException());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(InitClassStatement statement) {
            try {
                writeLocation(statement.getLocation());
                output.writeUnsigned(15);
                output.writeUnsigned(symbolTable.lookup(statement.getClassName()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(TryCatchStatement statement) {
            try {
                output.writeUnsigned(16);
                writeSequence(statement.getProtectedBody());
                output.writeUnsigned(statement.getExceptionType() != null
                        ? symbolTable.lookup(statement.getExceptionType()) + 1 : 0);
                output.writeUnsigned(statement.getExceptionVariable() != null
                        ? statement.getExceptionVariable() + 1 : 0);
                writeSequence(statement.getHandler());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(GotoPartStatement statement) {
            try {
                output.writeUnsigned(17);
                output.writeUnsigned(statement.getPart());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(MonitorEnterStatement statement) {
            try {
                writeLocation(statement.getLocation());
                output.writeUnsigned(18);
                writeExpr(statement.getObjectRef());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(MonitorExitStatement statement) {
            try {
                writeLocation(statement.getLocation());
                output.writeUnsigned(19);
                writeExpr(statement.getObjectRef());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(BinaryExpr expr) {
            try {
                output.writeUnsigned(0);
                output.writeUnsigned(expr.getOperation().ordinal());
                output.writeUnsigned(expr.getType() != null ? expr.getType().ordinal() + 1 : 0);
                writeExpr(expr.getFirstOperand());
                writeExpr(expr.getSecondOperand());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(UnaryExpr expr) {
            try {
                output.writeUnsigned(1);
                output.writeUnsigned(expr.getOperation().ordinal());
                output.writeUnsigned(expr.getType() != null ? expr.getType().ordinal() + 1 : 0);
                writeExpr(expr.getOperand());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ConditionalExpr expr) {
            try {
                output.writeUnsigned(2);
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
                    output.writeUnsigned(3);
                } else if (value instanceof Integer) {
                    output.writeUnsigned(4);
                    output.writeSigned((Integer) value);
                } else if (value instanceof Long) {
                    output.writeUnsigned(5);
                    output.writeSigned((Long) value);
                } else if (value instanceof Float) {
                    output.writeUnsigned(6);
                    output.writeFloat((Float) value);
                } else if (value instanceof Double) {
                    output.writeUnsigned(7);
                    output.writeDouble((Double) value);
                } else if (value instanceof String) {
                    output.writeUnsigned(8);
                    output.write((String) value);
                } else if (value instanceof ValueType) {
                    output.writeUnsigned(9);
                    output.writeUnsigned(symbolTable.lookup(value.toString()));
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(VariableExpr expr) {
            try {
                output.writeUnsigned(10);
                output.writeUnsigned(expr.getIndex());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(SubscriptExpr expr) {
            try {
                output.writeUnsigned(11);
                writeExpr(expr.getArray());
                writeExpr(expr.getIndex());
                output.writeUnsigned(expr.getType().ordinal());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(UnwrapArrayExpr expr) {
            try {
                output.writeUnsigned(12);
                output.writeUnsigned(expr.getElementType().ordinal());
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
                        output.writeUnsigned(13);
                        break;
                    case STATIC:
                        output.writeUnsigned(14);
                        break;
                    case SPECIAL:
                        output.writeUnsigned(15);
                        break;
                    case DYNAMIC:
                        output.writeUnsigned(16);
                        break;
                }
                output.writeUnsigned(symbolTable.lookup(expr.getMethod().getClassName()));
                output.writeUnsigned(symbolTable.lookup(expr.getMethod().getDescriptor().toString()));
                output.writeUnsigned(expr.getArguments().size());
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
                output.writeUnsigned(expr.getQualified() == null ? 17 : 18);
                if (expr.getQualified() != null) {
                    writeExpr(expr.getQualified());
                }
                output.writeUnsigned(symbolTable.lookup(expr.getField().getClassName()));
                output.writeUnsigned(symbolTable.lookup(expr.getField().getFieldName()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(NewExpr expr) {
            try {
                output.writeUnsigned(19);
                output.writeUnsigned(symbolTable.lookup(expr.getConstructedClass()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(NewArrayExpr expr) {
            try {
                output.writeUnsigned(20);
                writeExpr(expr.getLength());
                output.writeUnsigned(symbolTable.lookup(expr.getType().toString()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(NewMultiArrayExpr expr) {
            try {
                output.writeUnsigned(21);
                output.writeUnsigned(expr.getDimensions().size());
                for (Expr dimension : expr.getDimensions()) {
                    writeExpr(dimension);
                }
                output.writeUnsigned(symbolTable.lookup(expr.getType().toString()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(ArrayFromDataExpr expr) {
            try {
                output.writeUnsigned(28);
                output.writeUnsigned(symbolTable.lookup(expr.getType().toString()));
                output.writeUnsigned(expr.getData().size());
                for (Expr element : expr.getData()) {
                    writeExpr(element);
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(InstanceOfExpr expr) {
            try {
                output.writeUnsigned(22);
                writeExpr(expr.getExpr());
                output.writeUnsigned(symbolTable.lookup(expr.getType().toString()));
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(CastExpr expr) {
            try {
                output.writeUnsigned(23);
                output.writeUnsigned(symbolTable.lookup(expr.getTarget().toString()));
                writeExpr(expr.getValue());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(PrimitiveCastExpr expr) {
            try {
                output.writeUnsigned(24);
                output.writeUnsigned(expr.getSource().ordinal());
                output.writeUnsigned(expr.getTarget().ordinal());
                writeExpr(expr.getValue());
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }

        @Override
        public void visit(BoundCheckExpr expr) {
            try {
                output.writeUnsigned(expr.getArray() == null ? 27 : !expr.isLower() ? 26 : 25);
                writeExpr(expr.getIndex());
                if (expr.getArray() != null) {
                    writeExpr(expr.getArray());
                }
            } catch (IOException e) {
                throw new IOExceptionWrapper(e);
            }
        }
    }

    private TextLocation readLocation(VarDataInput input) throws IOException {
        int fileIndex = input.readUnsigned();
        if (fileIndex == 0) {
            lastReadLocation = null;
        } else if (fileIndex == 1) {
            lastReadLocation = new TextLocation(lastReadLocation.getFileName(),
                    lastReadLocation.getLine() + input.readSigned());
        } else {
            lastReadLocation = new TextLocation(fileTable.at(fileIndex - 2), input.readUnsigned());
            return lastReadLocation;
        }
        return lastReadLocation;
    }

    private int readNodeLocation(int type, VarDataInput input) throws IOException {
        switch (type) {
            case 127:
                lastReadLocation = null;
                break;
            case 126:
                lastReadLocation = new TextLocation(lastReadLocation.getFileName(),
                        lastReadLocation.getLine() + input.readSigned(), lastReadInlining);
                break;
            case 125:
                lastReadLocation = new TextLocation(fileTable.at(input.readUnsigned()), input.readUnsigned(),
                        lastReadInlining);
                break;
            case 124: {
                String className = symbolTable.at(input.readUnsigned());
                MethodDescriptor methodDescriptor = MethodDescriptor.parse(symbolTable.at(input.readUnsigned()));
                methodDescriptor = referenceCache.getCached(methodDescriptor);
                lastReadInlining = new InliningInfo(referenceCache.getCached(className, methodDescriptor),
                        lastReadLocation.getFileName(), lastReadLocation.getLine(), lastReadInlining);
                lastReadLocation = new TextLocation(null, -1, lastReadInlining);
                break;
            }
            case 123:
                lastReadLocation = new TextLocation(lastReadInlining.getFileName(), lastReadInlining.getLine());
                lastReadInlining = lastReadInlining.getParent();
                break;
            default:
                return type;
        }
        return input.readUnsigned();
    }

    private Statement readStatement(VarDataInput input) throws IOException {
        int type = readNodeLocation(input.readUnsigned(), input);
        switch (type) {
            case 0: {
                AssignmentStatement stmt = new AssignmentStatement();
                stmt.setLocation(lastReadLocation);
                stmt.setLeftValue(readExpr(input));
                stmt.setRightValue(readExpr(input));
                stmt.setAsync(input.readUnsigned() != 0);
                return stmt;
            }
            case 1: {
                AssignmentStatement stmt = new AssignmentStatement();
                stmt.setLocation(lastReadLocation);
                stmt.setRightValue(readExpr(input));
                stmt.setAsync(input.readUnsigned() != 0);
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
                stmt.setId(input.read());
                stmt.setValue(readExpr(input));
                int clauseCount = input.readUnsigned();
                for (int i = 0; i < clauseCount; ++i) {
                    SwitchClause clause = new SwitchClause();
                    int conditionCount = input.readUnsigned();
                    int[] conditions = new int[conditionCount];
                    for (int j = 0; j < conditionCount; ++j) {
                        conditions[j] = input.readSigned();
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
                stmt.setId(input.read());
                stmt.setCondition(readExpr(input));
                if (stmt.getId() != null) {
                    statementMap.put(stmt.getId(), stmt);
                }
                readSequence(input, stmt.getBody());
                return stmt;
            }
            case 6: {
                WhileStatement stmt = new WhileStatement();
                stmt.setId(input.read());
                if (stmt.getId() != null) {
                    statementMap.put(stmt.getId(), stmt);
                }
                readSequence(input, stmt.getBody());
                return stmt;
            }
            case 7: {
                BlockStatement stmt = new BlockStatement();
                stmt.setId(input.read());
                if (stmt.getId() != null) {
                    statementMap.put(stmt.getId(), stmt);
                }
                readSequence(input, stmt.getBody());
                return stmt;
            }
            case 8: {
                BreakStatement stmt = new BreakStatement();
                stmt.setLocation(lastReadLocation);
                stmt.setTarget(statementMap.get(input.read()));
                return stmt;
            }
            case 9: {
                BreakStatement stmt = new BreakStatement();
                stmt.setLocation(lastReadLocation);
                return stmt;
            }
            case 10: {
                ContinueStatement stmt = new ContinueStatement();
                stmt.setLocation(lastReadLocation);
                stmt.setTarget(statementMap.get(input.read()));
                return stmt;
            }
            case 11: {
                ContinueStatement stmt = new ContinueStatement();
                stmt.setLocation(lastReadLocation);
                return stmt;
            }
            case 12: {
                ReturnStatement stmt = new ReturnStatement();
                stmt.setLocation(lastReadLocation);
                stmt.setResult(readExpr(input));
                return stmt;
            }
            case 13: {
                ReturnStatement stmt = new ReturnStatement();
                stmt.setLocation(lastReadLocation);
                return stmt;
            }
            case 14: {
                ThrowStatement stmt = new ThrowStatement();
                stmt.setLocation(lastReadLocation);
                stmt.setException(readExpr(input));
                return stmt;
            }
            case 15: {
                InitClassStatement stmt = new InitClassStatement();
                stmt.setLocation(lastReadLocation);
                stmt.setClassName(symbolTable.at(input.readUnsigned()));
                return stmt;
            }
            case 16: {
                TryCatchStatement stmt = new TryCatchStatement();
                readSequence(input, stmt.getProtectedBody());
                int exceptionTypeIndex = input.readUnsigned();
                if (exceptionTypeIndex > 0) {
                    stmt.setExceptionType(symbolTable.at(exceptionTypeIndex - 1));
                }
                int exceptionVarIndex = input.readUnsigned();
                if (exceptionVarIndex > 0) {
                    stmt.setExceptionVariable(exceptionVarIndex - 1);
                }
                readSequence(input, stmt.getHandler());
                return stmt;
            }
            case 17: {
                GotoPartStatement stmt = new GotoPartStatement();
                stmt.setPart(input.readUnsigned());
                return stmt;
            }
            case 18: {
                MonitorEnterStatement stmt = new MonitorEnterStatement();
                stmt.setLocation(lastReadLocation);
                stmt.setObjectRef(readExpr(input));
                return stmt;
            }
            case 19: {
                MonitorExitStatement stmt = new MonitorExitStatement();
                stmt.setLocation(lastReadLocation);
                stmt.setObjectRef(readExpr(input));
                return stmt;
            }
            default:
                throw new RuntimeException("Unexpected statement type: " + type);
        }
    }

    private void readSequence(VarDataInput input, List<Statement> statements) throws IOException {
        int count = input.readUnsigned();
        for (int i = 0; i < count; ++i) {
            statements.add(readStatement(input));
        }
    }

    private Expr readExpr(VarDataInput input) throws IOException {
        int type = readNodeLocation(input.readUnsigned(), input);
        switch (type) {
            case 0: {
                BinaryExpr expr = new BinaryExpr();
                expr.setLocation(lastReadLocation);
                expr.setOperation(binaryOperations[input.readUnsigned()]);
                int valueType = input.readUnsigned();
                expr.setType(valueType > 0 ? OperationType.values()[valueType - 1] : null);
                expr.setFirstOperand(readExpr(input));
                expr.setSecondOperand(readExpr(input));
                return expr;
            }
            case 1: {
                UnaryExpr expr = new UnaryExpr();
                expr.setLocation(lastReadLocation);
                expr.setOperation(unaryOperations[input.readUnsigned()]);
                int valueType = input.readUnsigned();
                expr.setType(valueType > 0 ? OperationType.values()[valueType - 1] : null);
                expr.setOperand(readExpr(input));
                return expr;
            }
            case 2: {
                ConditionalExpr expr = new ConditionalExpr();
                expr.setLocation(lastReadLocation);
                expr.setCondition(readExpr(input));
                expr.setConsequent(readExpr(input));
                expr.setAlternative(readExpr(input));
                return expr;
            }
            case 3: {
                ConstantExpr expr = new ConstantExpr();
                expr.setLocation(lastReadLocation);
                return expr;
            }
            case 4: {
                ConstantExpr expr = new ConstantExpr();
                expr.setLocation(lastReadLocation);
                expr.setValue(input.readSigned());
                return expr;
            }
            case 5: {
                ConstantExpr expr = new ConstantExpr();
                expr.setValue(input.readSignedLong());
                return expr;
            }
            case 6: {
                ConstantExpr expr = new ConstantExpr();
                expr.setLocation(lastReadLocation);
                expr.setValue(input.readFloat());
                return expr;
            }
            case 7: {
                ConstantExpr expr = new ConstantExpr();
                expr.setLocation(lastReadLocation);
                expr.setValue(input.readDouble());
                return expr;
            }
            case 8: {
                ConstantExpr expr = new ConstantExpr();
                expr.setLocation(lastReadLocation);
                expr.setValue(input.read());
                return expr;
            }
            case 9: {
                ConstantExpr expr = new ConstantExpr();
                expr.setLocation(lastReadLocation);
                expr.setValue(ValueType.parse(symbolTable.at(input.readUnsigned())));
                return expr;
            }
            case 10: {
                VariableExpr expr = new VariableExpr();
                expr.setLocation(lastReadLocation);
                expr.setIndex(input.readUnsigned());
                return expr;
            }
            case 11: {
                SubscriptExpr expr = new SubscriptExpr();
                expr.setLocation(lastReadLocation);
                expr.setArray(readExpr(input));
                expr.setIndex(readExpr(input));
                expr.setType(ArrayType.values()[input.readUnsigned()]);
                return expr;
            }
            case 12: {
                UnwrapArrayExpr expr = new UnwrapArrayExpr(ArrayType.values()[input.readUnsigned()]);
                expr.setLocation(lastReadLocation);
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
                expr.setLocation(lastReadLocation);
                String className = symbolTable.at(input.readUnsigned());
                String fieldName = symbolTable.at(input.readUnsigned());
                expr.setField(new FieldReference(className, fieldName));
                return expr;
            }
            case 18: {
                QualificationExpr expr = new QualificationExpr();
                expr.setLocation(lastReadLocation);
                expr.setQualified(readExpr(input));
                String className = symbolTable.at(input.readUnsigned());
                String fieldName = symbolTable.at(input.readUnsigned());
                expr.setField(new FieldReference(className, fieldName));
                return expr;
            }
            case 19: {
                NewExpr expr = new NewExpr();
                expr.setLocation(lastReadLocation);
                expr.setConstructedClass(symbolTable.at(input.readUnsigned()));
                return expr;
            }
            case 20: {
                NewArrayExpr expr = new NewArrayExpr();
                expr.setLocation(lastReadLocation);
                expr.setLength(readExpr(input));
                expr.setType(ValueType.parse(symbolTable.at(input.readUnsigned())));
                return expr;
            }
            case 21: {
                NewMultiArrayExpr expr = new NewMultiArrayExpr();
                expr.setLocation(lastReadLocation);
                int dimensionCount = input.readUnsigned();
                for (int i = 0; i < dimensionCount; ++i) {
                    expr.getDimensions().add(readExpr(input));
                }
                expr.setType(ValueType.parse(symbolTable.at(input.readUnsigned())));
                return expr;
            }
            case 22: {
                InstanceOfExpr expr = new InstanceOfExpr();
                expr.setLocation(lastReadLocation);
                expr.setExpr(readExpr(input));
                expr.setType(ValueType.parse(symbolTable.at(input.readUnsigned())));
                return expr;
            }
            case 23: {
                CastExpr expr = new CastExpr();
                expr.setLocation(lastReadLocation);
                expr.setTarget(ValueType.parse(symbolTable.at(input.readUnsigned())));
                expr.setValue(readExpr(input));
                return expr;
            }
            case 24: {
                PrimitiveCastExpr expr = new PrimitiveCastExpr();
                expr.setLocation(lastReadLocation);
                expr.setSource(OperationType.values()[input.readUnsigned()]);
                expr.setTarget(OperationType.values()[input.readUnsigned()]);
                expr.setValue(readExpr(input));
                return expr;
            }
            case 25:
            case 26: {
                BoundCheckExpr expr = new BoundCheckExpr();
                expr.setLocation(lastReadLocation);
                expr.setIndex(readExpr(input));
                expr.setArray(readExpr(input));
                expr.setLower(type == 25);
                return expr;
            }
            case 27: {
                BoundCheckExpr expr = new BoundCheckExpr();
                expr.setLocation(lastReadLocation);
                expr.setIndex(readExpr(input));
                expr.setLower(true);
                return expr;
            }
            case 28: {
                ArrayFromDataExpr expr = new ArrayFromDataExpr();
                expr.setType(ValueType.parse(symbolTable.at(input.readUnsigned())));
                int count = input.readUnsigned();
                for (int i = 0; i < count; ++i) {
                    expr.getData().add(readExpr(input));
                }
                return expr;
            }
            default:
                throw new RuntimeException("Unknown expression type: " + type);
        }
    }

    private InvocationExpr parseInvocationExpr(InvocationType invocationType, VarDataInput input) throws IOException {
        InvocationExpr expr = new InvocationExpr();
        expr.setLocation(lastReadLocation);
        expr.setType(invocationType);
        String className = symbolTable.at(input.readUnsigned());
        String signature = symbolTable.at(input.readUnsigned());
        MethodReference methodRef = referenceCache.getCached(className, MethodDescriptor.parse(signature));
        expr.setMethod(methodRef);
        int argCount = input.readUnsigned();
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
