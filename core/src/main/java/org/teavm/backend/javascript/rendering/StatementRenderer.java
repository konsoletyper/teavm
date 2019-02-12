/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.backend.javascript.rendering;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntIndexedContainer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.teavm.ast.AssignmentStatement;
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
import org.teavm.ast.InitClassStatement;
import org.teavm.ast.InstanceOfExpr;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.InvocationType;
import org.teavm.ast.MethodNode;
import org.teavm.ast.MonitorEnterStatement;
import org.teavm.ast.MonitorExitStatement;
import org.teavm.ast.NewArrayExpr;
import org.teavm.ast.NewExpr;
import org.teavm.ast.NewMultiArrayExpr;
import org.teavm.ast.OperationType;
import org.teavm.ast.PrimitiveCastExpr;
import org.teavm.ast.QualificationExpr;
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
import org.teavm.ast.UnwrapArrayExpr;
import org.teavm.ast.VariableExpr;
import org.teavm.ast.VariableNode;
import org.teavm.ast.WhileStatement;
import org.teavm.backend.javascript.codegen.NamingStrategy;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.spi.Injector;
import org.teavm.backend.javascript.spi.InjectorContext;
import org.teavm.debugging.information.DebugInformationEmitter;
import org.teavm.debugging.information.DeferredCallSite;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.ListableClassReaderSource;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.vm.RenderingException;

public class StatementRenderer implements ExprVisitor, StatementVisitor {
    private RenderingContext context;
    private SourceWriter writer;
    private ClassReaderSource classSource;
    private boolean async;
    private boolean minifying;
    private Precedence precedence;
    private DebugInformationEmitter debugEmitter;
    private NamingStrategy naming;
    private DeferredCallSite lastCallSite;
    private DeferredCallSite prevCallSite;
    private boolean end;
    private final Map<String, String> blockIdMap = new HashMap<>();
    private final List<String> cachedVariableNames = new ArrayList<>();
    private final Set<String> usedVariableNames = new HashSet<>();
    private MethodNode currentMethod;
    private int currentPart;
    private List<String> blockIds = new ArrayList<>();
    private IntIndexedContainer blockIndexMap = new IntArrayList();
    private boolean longLibraryUsed;

    public StatementRenderer(RenderingContext context, SourceWriter writer) {
        this.context = context;
        this.writer = writer;
        this.classSource = context.getClassSource();
        this.minifying = context.isMinifying();
        this.naming = context.getNaming();
        this.debugEmitter = context.getDebugEmitter();
    }

    public boolean isLongLibraryUsed() {
        return longLibraryUsed;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public void setCurrentMethod(MethodNode currentMethod) {
        this.currentMethod = currentMethod;
    }

    public void setCurrentPart(int currentPart) {
        this.currentPart = currentPart;
    }

    public void setEnd(boolean end) {
        this.end = end;
    }

    private void pushLocation(TextLocation location) {
        context.pushLocation(location);
    }

    private void popLocation() {
        context.popLocation();
    }

    @Override
    public void visit(AssignmentStatement statement) throws RenderingException {
        try {
            debugEmitter.emitStatementStart();
            if (statement.getLocation() != null) {
                pushLocation(statement.getLocation());
            }
            prevCallSite = debugEmitter.emitCallSite();
            if (statement.getLeftValue() != null) {
                if (statement.isAsync()) {
                    writer.append(context.tempVarName());
                } else {
                    precedence = Precedence.COMMA;
                    statement.getLeftValue().acceptVisitor(this);
                }
                writer.ws().append("=").ws();
            }
            precedence = Precedence.COMMA;
            statement.getRightValue().acceptVisitor(this);
            debugEmitter.emitCallSite();
            writer.append(";").softNewLine();
            if (statement.isAsync()) {
                emitSuspendChecker();
                if (statement.getLeftValue() != null) {
                    precedence = Precedence.COMMA;
                    statement.getLeftValue().acceptVisitor(this);
                    writer.ws().append("=").ws().append(context.tempVarName()).append(";").softNewLine();
                }
            }
            if (statement.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(SequentialStatement statement) {
        visitStatements(statement.getSequence());
    }

    @Override
    public void visit(ConditionalStatement statement) {
        try {
            while (true) {
                debugEmitter.emitStatementStart();
                if (statement.getCondition().getLocation() != null) {
                    pushLocation(statement.getCondition().getLocation());
                }
                prevCallSite = debugEmitter.emitCallSite();
                writer.append("if").ws().append("(");
                precedence = Precedence.COMMA;
                statement.getCondition().acceptVisitor(this);
                if (statement.getCondition().getLocation() != null) {
                    popLocation();
                }
                debugEmitter.emitCallSite();
                writer.append(")").ws().append("{").softNewLine().indent();
                visitStatements(statement.getConsequent());
                if (!statement.getAlternative().isEmpty()) {
                    writer.outdent().append("}").ws();
                    if (statement.getAlternative().size() == 1
                            && statement.getAlternative().get(0) instanceof ConditionalStatement) {
                        statement = (ConditionalStatement) statement.getAlternative().get(0);
                        writer.append("else ");
                        continue;
                    }
                    writer.append("else").ws().append("{").indent().softNewLine();
                    visitStatements(statement.getAlternative());
                }
                break;
            }
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(SwitchStatement statement) {
        try {
            debugEmitter.emitStatementStart();
            if (statement.getValue().getLocation() != null) {
                pushLocation(statement.getValue().getLocation());
            }
            if (statement.getId() != null) {
                writer.append(mapBlockId(statement.getId())).append(":").ws();
            }
            prevCallSite = debugEmitter.emitCallSite();
            writer.append("switch").ws().append("(");
            precedence = Precedence.min();
            statement.getValue().acceptVisitor(this);
            if (statement.getValue().getLocation() != null) {
                popLocation();
            }
            debugEmitter.emitCallSite();
            writer.append(")").ws().append("{").softNewLine().indent();
            for (SwitchClause clause : statement.getClauses()) {
                for (int condition : clause.getConditions()) {
                    writer.append("case ").append(condition).append(":").softNewLine();
                }
                writer.indent();
                boolean oldEnd = end;
                for (Statement part : clause.getBody()) {
                    end = false;
                    part.acceptVisitor(this);
                }
                end = oldEnd;
                writer.outdent();
            }
            if (statement.getDefaultClause() != null) {
                writer.append("default:").softNewLine().indent();
                boolean oldEnd = end;
                for (Statement part : statement.getDefaultClause()) {
                    end = false;
                    part.acceptVisitor(this);
                }
                end = oldEnd;
                writer.outdent();
            }
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(WhileStatement statement) {
        try {
            debugEmitter.emitStatementStart();
            if (statement.getId() != null) {
                writer.append(mapBlockId(statement.getId())).append(":").ws();
            }
            writer.append("while");
            writer.ws().append("(");
            if (statement.getCondition() != null) {
                prevCallSite = debugEmitter.emitCallSite();
                precedence = Precedence.min();
                statement.getCondition().acceptVisitor(this);
                debugEmitter.emitCallSite();
            } else {
                writer.append("true");
            }
            writer.append(")").ws().append("{").softNewLine().indent();
            boolean oldEnd = end;
            for (Statement part : statement.getBody()) {
                end = false;
                part.acceptVisitor(this);
            }
            end = oldEnd;
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    private String mapBlockId(String id) {
        String name = blockIdMap.get(id);
        if (name == null) {
            int index = blockIdMap.size();
            name = generateBlockId(index);
            blockIdMap.put(id, name);
        }
        return name;
    }

    private String generateBlockId(int index) {
        int mappedIndex;
        while (blockIds.size() <= index) {
            mappedIndex = blockIndexMap.isEmpty() ? -1 : blockIndexMap.get(blockIds.size() - 1);
            mappedIndex++;
            while (RenderingUtil.KEYWORDS.contains(RenderingUtil.indexToId(mappedIndex))) {
                mappedIndex++;
            }
            blockIndexMap.add(mappedIndex);
            blockIds.add(RenderingUtil.indexToId(mappedIndex));
        }
        return blockIds.get(index);
    }

    @Override
    public void visit(BlockStatement statement) {
        try {
            writer.append(mapBlockId(statement.getId())).append(":").ws().append("{").softNewLine().indent();
            visitStatements(statement.getBody());
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(BreakStatement statement) {
        try {
            debugEmitter.emitStatementStart();
            if (statement.getLocation() != null) {
                pushLocation(statement.getLocation());
            }
            writer.append("break");
            if (statement.getTarget() != null) {
                writer.append(' ').append(mapBlockId(statement.getTarget().getId()));
            }
            writer.append(";").softNewLine();
            if (statement.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(ContinueStatement statement) {
        try {
            debugEmitter.emitStatementStart();
            if (statement.getLocation() != null) {
                pushLocation(statement.getLocation());
            }
            writer.append("continue");
            if (statement.getTarget() != null) {
                writer.append(' ').append(mapBlockId(statement.getTarget().getId()));
            }
            writer.append(";").softNewLine();
            if (statement.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(ReturnStatement statement) {
        try {
            debugEmitter.emitStatementStart();
            if (statement.getLocation() != null) {
                pushLocation(statement.getLocation());
            }
            writer.append("return");
            if (statement.getResult() != null) {
                writer.append(' ');
                prevCallSite = debugEmitter.emitCallSite();
                precedence = Precedence.min();
                statement.getResult().acceptVisitor(this);
                debugEmitter.emitCallSite();
            }
            writer.append(";").softNewLine();
            if (statement.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(ThrowStatement statement) {
        try {
            debugEmitter.emitStatementStart();
            if (statement.getLocation() != null) {
                pushLocation(statement.getLocation());
            }
            writer.appendFunction("$rt_throw").append("(");
            prevCallSite = debugEmitter.emitCallSite();
            precedence = Precedence.min();
            statement.getException().acceptVisitor(this);
            writer.append(");").softNewLine();
            debugEmitter.emitCallSite();
            if (statement.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(InitClassStatement statement) {
        ClassReader cls = classSource.get(statement.getClassName());
        if (cls == null) {
            return;
        }
        MethodReader method = cls.getMethod(new MethodDescriptor("<clinit>", void.class));
        if (method == null) {
            return;
        }
        try {
            debugEmitter.emitStatementStart();
            if (statement.getLocation() != null) {
                pushLocation(statement.getLocation());
            }
            writer.appendClassInit(statement.getClassName()).append("();").softNewLine();
            if (statement.isAsync()) {
                emitSuspendChecker();
            }
            if (statement.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    public String variableName(int index) {
        while (index >= cachedVariableNames.size()) {
            cachedVariableNames.add(null);
        }
        String name = cachedVariableNames.get(index);
        if (name == null) {
            name = generateVariableName(index);
            cachedVariableNames.set(index, name);
        }
        return name;
    }

    private String generateVariableName(int index) {
        if (!minifying) {
            VariableNode variable = index < currentMethod.getVariables().size()
                    ? currentMethod.getVariables().get(index)
                    : null;
            if (variable != null && variable.getName() != null) {
                String result = "$" + RenderingUtil.escapeName(variable.getName());
                if (RenderingUtil.KEYWORDS.contains(result) || !usedVariableNames.add(result)) {
                    String base = result;
                    int suffix = 0;
                    do {
                        result = base + "_" + suffix++;
                    } while (!usedVariableNames.add(result));
                }
                return result;
            } else {
                return "var$" + index;
            }
        } else {
            return RenderingUtil.indexToId(index);
        }
    }

    private void visitBinary(BinaryExpr expr, String op, boolean guarded) {
        if (expr.getLocation() != null) {
            pushLocation(expr.getLocation());
        }
        if (guarded) {
            visitBinary(BinaryOperation.OR, "|", () -> visitBinary(expr, op, false),
                    () -> {
                        try {
                            writer.append("0");
                        } catch (IOException e) {
                            throw new RenderingException("IO error occurred", e);
                        }
                    });
        } else {
            visitBinary(expr.getOperation(), op, () -> expr.getFirstOperand().acceptVisitor(this),
                    () -> expr.getSecondOperand().acceptVisitor(this));
        }
        if (expr.getLocation() != null) {
            popLocation();
        }
    }

    private void visitBinary(BinaryOperation operation, String infixText, Runnable a, Runnable b) {
        try {
            Precedence outerPrecedence = precedence;
            Precedence innerPrecedence = getPrecedence(operation);
            if (innerPrecedence.ordinal() < outerPrecedence.ordinal()) {
                writer.append('(');
            }

            switch (operation) {
                case ADD:
                case SUBTRACT:
                case MULTIPLY:
                case DIVIDE:
                case MODULO:
                case AND:
                case OR:
                case BITWISE_AND:
                case BITWISE_OR:
                case BITWISE_XOR:
                case LEFT_SHIFT:
                case RIGHT_SHIFT:
                case UNSIGNED_RIGHT_SHIFT:
                    precedence = innerPrecedence;
                    break;
                default:
                    precedence = innerPrecedence.next();
            }
            a.run();

            writer.ws().append(infixText).ws();

            switch (operation) {
                case ADD:
                case MULTIPLY:
                case AND:
                case OR:
                case BITWISE_AND:
                case BITWISE_OR:
                case BITWISE_XOR:
                    precedence = innerPrecedence;
                    break;
                default:
                    precedence = innerPrecedence.next();
                    break;
            }
            b.run();

            if (innerPrecedence.ordinal() < outerPrecedence.ordinal()) {
                writer.append(')');
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    private static Precedence getPrecedence(BinaryOperation op) {
        switch (op) {
            case ADD:
            case SUBTRACT:
                return Precedence.ADDITION;
            case MULTIPLY:
            case DIVIDE:
            case MODULO:
                return Precedence.MULTIPLICATION;
            case AND:
                return Precedence.LOGICAL_AND;
            case OR:
                return Precedence.LOGICAL_OR;
            case EQUALS:
            case NOT_EQUALS:
                return Precedence.EQUALITY;
            case GREATER:
            case GREATER_OR_EQUALS:
            case LESS:
            case LESS_OR_EQUALS:
                return Precedence.COMPARISON;
            case BITWISE_AND:
                return Precedence.BITWISE_AND;
            case BITWISE_OR:
                return Precedence.BITWISE_OR;
            case BITWISE_XOR:
                return Precedence.BITWISE_XOR;
            case LEFT_SHIFT:
            case RIGHT_SHIFT:
            case UNSIGNED_RIGHT_SHIFT:
                return Precedence.BITWISE_SHIFT;
            default:
                return Precedence.GROUPING;
        }
    }

    private void visitBinaryFunction(BinaryExpr expr, String function) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            writer.append(function);
            writer.append('(');
            precedence = Precedence.min();
            expr.getFirstOperand().acceptVisitor(this);
            writer.append(",").ws();
            precedence = Precedence.min();
            expr.getSecondOperand().acceptVisitor(this);
            writer.append(')');
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(BinaryExpr expr) {
        if (expr.getType() == OperationType.LONG) {
            longLibraryUsed = true;
            switch (expr.getOperation()) {
                case ADD:
                    visitBinaryFunction(expr, "Long_add");
                    break;
                case SUBTRACT:
                    visitBinaryFunction(expr, "Long_sub");
                    break;
                case MULTIPLY:
                    visitBinaryFunction(expr, "Long_mul");
                    break;
                case DIVIDE:
                    visitBinaryFunction(expr, "Long_div");
                    break;
                case MODULO:
                    visitBinaryFunction(expr, "Long_rem");
                    break;
                case BITWISE_OR:
                    visitBinaryFunction(expr, "Long_or");
                    break;
                case BITWISE_AND:
                    visitBinaryFunction(expr, "Long_and");
                    break;
                case BITWISE_XOR:
                    visitBinaryFunction(expr, "Long_xor");
                    break;
                case LEFT_SHIFT:
                    visitBinaryFunction(expr, "Long_shl");
                    break;
                case RIGHT_SHIFT:
                    visitBinaryFunction(expr, "Long_shr");
                    break;
                case UNSIGNED_RIGHT_SHIFT:
                    visitBinaryFunction(expr, "Long_shru");
                    break;
                case COMPARE:
                    visitBinaryFunction(expr, "Long_compare");
                    break;
                case EQUALS:
                    visitBinaryFunction(expr, "Long_eq");
                    break;
                case NOT_EQUALS:
                    visitBinaryFunction(expr, "Long_ne");
                    break;
                case LESS:
                    visitBinaryFunction(expr, "Long_lt");
                    break;
                case LESS_OR_EQUALS:
                    visitBinaryFunction(expr, "Long_le");
                    break;
                case GREATER:
                    visitBinaryFunction(expr, "Long_gt");
                    break;
                case GREATER_OR_EQUALS:
                    visitBinaryFunction(expr, "Long_ge");
                    break;
                default:
                    break;
            }
        } else {
            switch (expr.getOperation()) {
                case ADD:
                    visitBinary(expr, "+", expr.getType() == OperationType.INT);
                    break;
                case SUBTRACT:
                    visitBinary(expr, "-", expr.getType() == OperationType.INT);
                    break;
                case MULTIPLY:
                    if (expr.getType() != OperationType.INT || RenderingUtil.isSmallInteger(expr.getFirstOperand())
                            || RenderingUtil.isSmallInteger(expr.getSecondOperand())) {
                        visitBinary(expr, "*", expr.getType() == OperationType.INT);
                    } else {
                        visitBinaryFunction(expr, naming.getNameForFunction("$rt_imul"));
                    }
                    break;
                case DIVIDE:
                    visitBinary(expr, "/", expr.getType() == OperationType.INT);
                    break;
                case MODULO:
                    visitBinary(expr, "%", expr.getType() == OperationType.INT);
                    break;
                case EQUALS:
                    if (expr.getType() == OperationType.INT) {
                        visitBinary(expr, "==", false);
                    } else {
                        visitBinary(expr, "===", false);
                    }
                    break;
                case NOT_EQUALS:
                    if (expr.getType() == OperationType.INT) {
                        visitBinary(expr, "!=", false);
                    } else {
                        visitBinary(expr, "!==", false);
                    }
                    break;
                case GREATER:
                    visitBinary(expr, ">", false);
                    break;
                case GREATER_OR_EQUALS:
                    visitBinary(expr, ">=", false);
                    break;
                case LESS:
                    visitBinary(expr, "<", false);
                    break;
                case LESS_OR_EQUALS:
                    visitBinary(expr, "<=", false);
                    break;
                case COMPARE:
                    visitBinaryFunction(expr, naming.getNameForFunction("$rt_compare"));
                    break;
                case OR:
                    visitBinary(expr, "||", false);
                    break;
                case AND:
                    visitBinary(expr, "&&", false);
                    break;
                case BITWISE_OR:
                    visitBinary(expr, "|", false);
                    break;
                case BITWISE_AND:
                    visitBinary(expr, "&", false);
                    break;
                case BITWISE_XOR:
                    visitBinary(expr, "^", false);
                    break;
                case LEFT_SHIFT:
                    visitBinary(expr, "<<", false);
                    break;
                case RIGHT_SHIFT:
                    visitBinary(expr, ">>", false);
                    break;
                case UNSIGNED_RIGHT_SHIFT:
                    visitBinary(expr, ">>>", false);
                    break;
            }
        }
    }

    @Override
    public void visit(UnaryExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            Precedence outerPrecedence = precedence;
            switch (expr.getOperation()) {
                case NOT: {
                    if (expr.getType() == OperationType.LONG) {
                        longLibraryUsed = true;
                        writer.append("Long_not(");
                        precedence = Precedence.min();
                        expr.getOperand().acceptVisitor(this);
                        writer.append(')');
                    } else {
                        if (outerPrecedence.ordinal() > Precedence.UNARY.ordinal()) {
                            writer.append('(');
                        }
                        writer.append(expr.getType() == null ? "!" : "~");
                        precedence = Precedence.UNARY;
                        expr.getOperand().acceptVisitor(this);
                        if (outerPrecedence.ordinal() > Precedence.UNARY.ordinal()) {
                            writer.append(')');
                        }
                    }
                    break;
                }
                case NEGATE:
                    if (expr.getType() == OperationType.LONG) {
                        longLibraryUsed = true;
                        writer.append("Long_neg(");
                        precedence = Precedence.min();
                        expr.getOperand().acceptVisitor(this);
                        writer.append(')');
                    } else {
                        if (outerPrecedence.ordinal() > Precedence.UNARY.ordinal()) {
                            writer.append('(');
                        }
                        writer.append(" -");
                        precedence = Precedence.UNARY;
                        expr.getOperand().acceptVisitor(this);
                        if (outerPrecedence.ordinal() > Precedence.UNARY.ordinal()) {
                            writer.append(')');
                        }
                    }
                    break;
                case LENGTH:
                    precedence = Precedence.MEMBER_ACCESS;
                    expr.getOperand().acceptVisitor(this);
                    writer.append(".length");
                    break;
                case INT_TO_BYTE:
                    if (outerPrecedence.ordinal() > Precedence.BITWISE_SHIFT.ordinal()) {
                        writer.append('(');
                    }
                    precedence = Precedence.BITWISE_SHIFT;
                    expr.getOperand().acceptVisitor(this);
                    writer.ws().append("<<").ws().append("24").ws().append(">>").ws().append("24");
                    if (outerPrecedence.ordinal() > Precedence.BITWISE_SHIFT.ordinal()) {
                        writer.append(')');
                    }
                    break;
                case INT_TO_SHORT:
                    if (outerPrecedence.ordinal() > Precedence.BITWISE_SHIFT.ordinal()) {
                        writer.append('(');
                    }
                    precedence = Precedence.BITWISE_SHIFT;
                    expr.getOperand().acceptVisitor(this);
                    writer.ws().append("<<").ws().append("16").ws().append(">>").ws().append("16");
                    if (outerPrecedence.ordinal() > Precedence.BITWISE_SHIFT.ordinal()) {
                        writer.append(')');
                    }
                    break;
                case INT_TO_CHAR:
                    if (outerPrecedence.ordinal() > Precedence.BITWISE_AND.ordinal()) {
                        writer.append('(');
                    }
                    precedence = Precedence.BITWISE_AND;
                    expr.getOperand().acceptVisitor(this);
                    writer.ws().append("&").ws().append("65535");
                    if (outerPrecedence.ordinal() > Precedence.BITWISE_AND.ordinal()) {
                        writer.append(')');
                    }
                    break;
                case NULL_CHECK:
                    writer.appendFunction("$rt_nullCheck").append("(");
                    precedence = Precedence.min();
                    expr.getOperand().acceptVisitor(this);
                    writer.append(')');
                    break;
            }
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(CastExpr expr) {
        expr.getValue().acceptVisitor(this);
    }

    @Override
    public void visit(PrimitiveCastExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            switch (expr.getSource()) {
                case INT:
                    if (expr.getTarget() == OperationType.LONG) {
                        writer.append("Long_fromInt(");
                        precedence = Precedence.min();
                        expr.getValue().acceptVisitor(this);
                        writer.append(')');
                    } else {
                        expr.getValue().acceptVisitor(this);
                    }
                    break;
                case LONG:
                    switch (expr.getTarget()) {
                        case INT:
                            precedence = Precedence.MEMBER_ACCESS;
                            Expr longShifted = extractLongRightShiftedBy32(expr.getValue());
                            if (longShifted != null) {
                                longShifted.acceptVisitor(this);
                                writer.append(".hi");
                            } else {
                                expr.getValue().acceptVisitor(this);
                                writer.append(".lo");
                            }
                            break;
                        case FLOAT:
                        case DOUBLE:
                            writer.append("Long_toNumber(");
                            precedence = Precedence.min();
                            expr.getValue().acceptVisitor(this);
                            writer.append(')');
                            break;
                        default:
                            expr.getValue().acceptVisitor(this);
                    }
                    break;
                case FLOAT:
                case DOUBLE:
                    switch (expr.getTarget()) {
                        case LONG:
                            writer.append("Long_fromNumber(");
                            precedence = Precedence.min();
                            expr.getValue().acceptVisitor(this);
                            writer.append(')');
                            break;
                        case INT:
                            visitBinary(BinaryOperation.BITWISE_OR, "|", () -> expr.getValue().acceptVisitor(this),
                                    () -> {
                                        try {
                                            writer.append("0");
                                        } catch (IOException e) {
                                            throw new RenderingException("IO error occurred", e);
                                        }
                                    });
                            break;
                        default:
                            expr.getValue().acceptVisitor(this);
                    }
                    break;
            }
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    private Expr extractLongRightShiftedBy32(Expr expr) {
        if (!(expr instanceof BinaryExpr)) {
            return null;
        }

        BinaryExpr binary = (BinaryExpr) expr;
        if (binary.getOperation() != BinaryOperation.RIGHT_SHIFT
                && binary.getOperation() != BinaryOperation.UNSIGNED_RIGHT_SHIFT) {
            return null;
        }
        if (binary.getType() != OperationType.LONG) {
            return null;
        }
        if (!(binary.getSecondOperand() instanceof ConstantExpr)) {
            return null;
        }

        Object rightConstant = ((ConstantExpr) binary.getSecondOperand()).getValue();
        if (rightConstant.equals(32) || rightConstant.equals(32L)) {
            return binary.getFirstOperand();
        }

        return null;
    }

    @Override
    public void visit(ConditionalExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }

            Precedence outerPrecedence = precedence;
            if (outerPrecedence.ordinal() > Precedence.CONDITIONAL.ordinal()) {
                writer.append('(');
            }

            precedence = Precedence.CONDITIONAL.next();
            expr.getCondition().acceptVisitor(this);
            writer.ws().append("?").ws();
            precedence = Precedence.CONDITIONAL.next();
            expr.getConsequent().acceptVisitor(this);
            writer.ws().append(":").ws();
            precedence = Precedence.CONDITIONAL;
            expr.getAlternative().acceptVisitor(this);

            if (outerPrecedence.ordinal() > Precedence.CONDITIONAL.ordinal()) {
                writer.append(')');
            }

            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(ConstantExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            context.constantToString(writer, expr.getValue());
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(VariableExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            writer.append(variableName(expr.getIndex()));
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occured", e);
        }
    }

    @Override
    public void visit(SubscriptExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            precedence = Precedence.MEMBER_ACCESS;
            expr.getArray().acceptVisitor(this);
            writer.append('[');
            precedence = Precedence.min();
            expr.getIndex().acceptVisitor(this);
            writer.append(']');
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(UnwrapArrayExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            precedence = Precedence.MEMBER_ACCESS;
            expr.getArray().acceptVisitor(this);
            writer.append(".data");
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(InvocationExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            Injector injector = context.getInjector(expr.getMethod());
            if (injector != null) {
                injector.generate(new InjectorContextImpl(expr.getArguments()), expr.getMethod());
            } else {
                if (expr.getType() == InvocationType.DYNAMIC) {
                    precedence = Precedence.MEMBER_ACCESS;
                    expr.getArguments().get(0).acceptVisitor(this);
                }
                MethodReference method = expr.getMethod();
                String name = naming.getNameFor(method.getDescriptor());
                DeferredCallSite callSite = prevCallSite;
                boolean shouldEraseCallSite = lastCallSite == null;
                if (lastCallSite == null) {
                    lastCallSite = callSite;
                }
                boolean virtual = false;
                switch (expr.getType()) {
                    case STATIC:
                        writer.appendMethodBody(method).append("(");
                        prevCallSite = debugEmitter.emitCallSite();
                        for (int i = 0; i < expr.getArguments().size(); ++i) {
                            if (i > 0) {
                                writer.append(",").ws();
                            }
                            precedence = Precedence.min();
                            expr.getArguments().get(i).acceptVisitor(this);
                        }
                        break;
                    case SPECIAL:
                        writer.appendMethodBody(method).append("(");
                        prevCallSite = debugEmitter.emitCallSite();
                        precedence = Precedence.min();
                        expr.getArguments().get(0).acceptVisitor(this);
                        for (int i = 1; i < expr.getArguments().size(); ++i) {
                            writer.append(",").ws();
                            precedence = Precedence.min();
                            expr.getArguments().get(i).acceptVisitor(this);
                        }
                        break;
                    case DYNAMIC:
                        writer.append(".").append(name).append("(");
                        prevCallSite = debugEmitter.emitCallSite();
                        for (int i = 1; i < expr.getArguments().size(); ++i) {
                            if (i > 1) {
                                writer.append(",").ws();
                            }
                            precedence = Precedence.min();
                            expr.getArguments().get(i).acceptVisitor(this);
                        }
                        virtual = true;
                        break;
                    case CONSTRUCTOR:
                        writer.appendInit(expr.getMethod()).append("(");
                        prevCallSite = debugEmitter.emitCallSite();
                        for (int i = 0; i < expr.getArguments().size(); ++i) {
                            if (i > 0) {
                                writer.append(",").ws();
                            }
                            precedence = Precedence.min();
                            expr.getArguments().get(i).acceptVisitor(this);
                        }
                        break;
                }
                writer.append(')');
                if (lastCallSite != null) {
                    if (virtual) {
                        lastCallSite.setVirtualMethod(expr.getMethod());
                    } else {
                        lastCallSite.setStaticMethod(expr.getMethod());
                    }
                    lastCallSite = callSite;
                }
                if (shouldEraseCallSite) {
                    lastCallSite = null;
                }
            }
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(QualificationExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            precedence = Precedence.MEMBER_ACCESS;

            if (expr.getQualified() != null) {
                expr.getQualified().acceptVisitor(this);
                writer.append('.').appendField(expr.getField());
            } else {
                writer.appendStaticField(expr.getField());
            }

            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(NewExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }

            Precedence outerPrecedence = precedence;
            if (outerPrecedence.ordinal() > Precedence.FUNCTION_CALL.ordinal()) {
                writer.append('(');
            }

            precedence = Precedence.FUNCTION_CALL;

            writer.append("new ").appendClass(expr.getConstructedClass());
            if (outerPrecedence.ordinal() > Precedence.FUNCTION_CALL.ordinal()) {
                writer.append(')');
            }

            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(NewArrayExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            ValueType type = expr.getType();
            if (type instanceof ValueType.Primitive) {
                switch (((ValueType.Primitive) type).getKind()) {
                    case BOOLEAN:
                        writer.append("$rt_createBooleanArray(");
                        precedence = Precedence.min();
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case BYTE:
                        writer.append("$rt_createByteArray(");
                        precedence = Precedence.min();
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case SHORT:
                        writer.append("$rt_createShortArray(");
                        precedence = Precedence.min();
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case INTEGER:
                        writer.append("$rt_createIntArray(");
                        precedence = Precedence.min();
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case LONG:
                        writer.append("$rt_createLongArray(");
                        precedence = Precedence.min();
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case FLOAT:
                        writer.append("$rt_createFloatArray(");
                        precedence = Precedence.min();
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case DOUBLE:
                        writer.append("$rt_createDoubleArray(");
                        precedence = Precedence.min();
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                    case CHARACTER:
                        writer.append("$rt_createCharArray(");
                        precedence = Precedence.min();
                        expr.getLength().acceptVisitor(this);
                        writer.append(")");
                        break;
                }
            } else {
                writer.appendFunction("$rt_createArray").append("(");
                context.typeToClsString(writer, expr.getType());
                writer.append(",").ws();
                precedence = Precedence.min();
                expr.getLength().acceptVisitor(this);
                writer.append(")");
            }
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(NewMultiArrayExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            ValueType type = expr.getType();
            for (int i = 0; i < expr.getDimensions().size(); ++i) {
                type = ((ValueType.Array) type).getItemType();
            }
            if (type instanceof ValueType.Primitive) {
                switch (((ValueType.Primitive) type).getKind()) {
                    case BOOLEAN:
                        writer.append("$rt_createBooleanMultiArray(");
                        break;
                    case BYTE:
                        writer.append("$rt_createByteMultiArray(");
                        break;
                    case SHORT:
                        writer.append("$rt_createShortMultiArray(");
                        break;
                    case INTEGER:
                        writer.append("$rt_createIntMultiArray(");
                        break;
                    case LONG:
                        writer.append("$rt_createLongMultiArray(");
                        break;
                    case FLOAT:
                        writer.append("$rt_createFloatMultiArray(");
                        break;
                    case DOUBLE:
                        writer.append("$rt_createDoubleMultiArray(");
                        break;
                    case CHARACTER:
                        writer.append("$rt_createCharMultiArray(");
                        break;
                }
            } else {
                writer.append("$rt_createMultiArray(");
                context.typeToClsString(writer, expr.getType());
                writer.append(",").ws();
            }
            writer.append("[");
            boolean first = true;
            List<Expr> dimensions = new ArrayList<>(expr.getDimensions());
            Collections.reverse(dimensions);
            for (Expr dimension : dimensions) {
                if (!first) {
                    writer.append(",").ws();
                }
                first = false;
                precedence = Precedence.min();
                dimension.acceptVisitor(this);
            }
            writer.append("])");
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        try {
            if (expr.getLocation() != null) {
                pushLocation(expr.getLocation());
            }
            if (expr.getType() instanceof ValueType.Object) {
                String clsName = ((ValueType.Object) expr.getType()).getClassName();
                ClassReader cls = classSource.get(clsName);
                if (cls != null && !cls.hasModifier(ElementModifier.INTERFACE)) {
                    boolean needsParentheses = Precedence.COMPARISON.ordinal() < precedence.ordinal();
                    if (needsParentheses) {
                        writer.append('(');
                    }
                    precedence = Precedence.CONDITIONAL.next();
                    expr.getExpr().acceptVisitor(this);
                    writer.append(" instanceof ").appendClass(clsName);
                    if (needsParentheses) {
                        writer.append(')');
                    }
                    if (expr.getLocation() != null) {
                        popLocation();
                    }
                    return;
                }
            }
            writer.appendFunction("$rt_isInstance").append("(");
            precedence = Precedence.min();
            expr.getExpr().acceptVisitor(this);
            writer.append(",").ws();
            context.typeToClsString(writer, expr.getType());
            writer.append(")");
            if (expr.getLocation() != null) {
                popLocation();
            }
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    private void visitStatements(List<Statement> statements) {
        if (statements.isEmpty()) {
            return;
        }
        boolean oldEnd = end;
        for (int i = 0; i < statements.size() - 1; ++i) {
            end = false;
            statements.get(i).acceptVisitor(this);
        }
        end = oldEnd;
        statements.get(statements.size() - 1).acceptVisitor(this);
        end = oldEnd;
    }

    @Override
    public void visit(TryCatchStatement statement) {
        try {
            writer.append("try").ws().append("{").softNewLine().indent();
            List<TryCatchStatement> sequence = new ArrayList<>();
            sequence.add(statement);
            List<Statement> protectedBody = statement.getProtectedBody();
            while (protectedBody.size() == 1 && protectedBody.get(0) instanceof TryCatchStatement) {
                TryCatchStatement nextStatement = (TryCatchStatement) protectedBody.get(0);
                sequence.add(nextStatement);
                protectedBody = nextStatement.getProtectedBody();
            }
            visitStatements(protectedBody);
            writer.outdent().append("}").ws().append("catch").ws().append("($$e)")
                    .ws().append("{").indent().softNewLine();
            writer.append("$$je").ws().append("=").ws().append("$rt_wrapException($$e);").softNewLine();
            boolean first = true;
            boolean defaultHandlerOccurred = false;
            for (TryCatchStatement catchClause : sequence) {
                if (!first) {
                    writer.ws().append("else");
                }
                if (catchClause.getExceptionType() != null) {
                    if (!first) {
                        writer.append(" ");
                    }
                    writer.append("if").ws().append("($$je instanceof ").appendClass(catchClause.getExceptionType());
                    writer.append(")").ws();
                } else {
                    defaultHandlerOccurred = true;
                }

                if (catchClause.getExceptionType() != null || !first) {
                    writer.append("{").indent().softNewLine();
                }

                if (catchClause.getExceptionVariable() != null) {
                    writer.append(variableName(catchClause.getExceptionVariable())).ws().append("=").ws()
                            .append("$$je;").softNewLine();
                }
                visitStatements(catchClause.getHandler());

                if (catchClause.getExceptionType() != null || !first) {
                    writer.outdent().append("}");
                }

                first = false;

                if (defaultHandlerOccurred) {
                    break;
                }
            }
            if (!defaultHandlerOccurred) {
                writer.ws().append("else").ws().append("{").indent().softNewLine();
                writer.append("throw $$je;").softNewLine();
                writer.outdent().append("}").softNewLine();
            } else {
                writer.softNewLine();
            }
            writer.outdent().append("}").softNewLine();
        } catch (IOException e) {
            throw new RenderingException("IO error occurred", e);
        }
    }

    @Override
    public void visit(GotoPartStatement statement) {
        try {
            if (statement.getPart() != currentPart) {
                writer.append(context.pointerName()).ws().append("=").ws().append(statement.getPart()).append(";")
                        .softNewLine();
            }
            if (!end || statement.getPart() != currentPart + 1) {
                writer.append("continue ").append(context.mainLoopName()).append(";").softNewLine();
            }
        } catch (IOException ex) {
            throw new RenderingException("IO error occurred", ex);
        }
    }

    @Override
    public void visit(MonitorEnterStatement statement) {
        try {
            if (async) {
                writer.appendMethodBody(NameFrequencyEstimator.MONITOR_ENTER_METHOD).append("(");
                precedence = Precedence.min();
                statement.getObjectRef().acceptVisitor(this);
                writer.append(");").softNewLine();
                emitSuspendChecker();
            } else {
                writer.appendMethodBody(NameFrequencyEstimator.MONITOR_ENTER_SYNC_METHOD).append('(');
                precedence = Precedence.min();
                statement.getObjectRef().acceptVisitor(this);
                writer.append(");").softNewLine();
            }
        } catch (IOException ex) {
            throw new RenderingException("IO error occurred", ex);
        }
    }

    public void emitSuspendChecker() throws IOException {
        writer.append("if").ws().append("(").appendFunction("$rt_suspending").append("())").ws()
                .append("{").indent().softNewLine();
        writer.append("break ").append(context.mainLoopName()).append(";").softNewLine();
        writer.outdent().append("}").softNewLine();
    }

    @Override
    public void visit(MonitorExitStatement statement) {
        try {
            if (async) {
                writer.appendMethodBody(NameFrequencyEstimator.MONITOR_EXIT_METHOD).append("(");
                precedence = Precedence.min();
                statement.getObjectRef().acceptVisitor(this);
                writer.append(");").softNewLine();
            } else {
                writer.appendMethodBody(NameFrequencyEstimator.MONITOR_EXIT_SYNC_METHOD).append('(');
                precedence = Precedence.min();
                statement.getObjectRef().acceptVisitor(this);
                writer.append(");").softNewLine();
            }
        } catch (IOException ex) {
            throw new RenderingException("IO error occurred", ex);
        }
    }

    private class InjectorContextImpl implements InjectorContext {
        private final List<Expr> arguments;
        private final Precedence precedence = StatementRenderer.this.precedence;

        InjectorContextImpl(List<Expr> arguments) {
            this.arguments = arguments;
        }

        @Override
        public Expr getArgument(int index) {
            return arguments.get(index);
        }

        @Override
        public boolean isMinifying() {
            return minifying;
        }

        @Override
        public SourceWriter getWriter() {
            return writer;
        }

        @Override
        public void writeEscaped(String str) throws IOException {
            writer.append(RenderingUtil.escapeString(str));
        }

        @Override
        public void writeType(ValueType type) throws IOException {
            context.typeToClsString(writer, type);
        }

        @Override
        public void writeExpr(Expr expr) {
            writeExpr(expr, Precedence.GROUPING);
        }

        @Override
        public void writeExpr(Expr expr, Precedence precedence) {
            StatementRenderer.this.precedence = precedence;
            expr.acceptVisitor(StatementRenderer.this);
        }

        @Override
        public int argumentCount() {
            return arguments.size();
        }

        @Override
        public <T> T getService(Class<T> type) {
            return context.getServices().getService(type);
        }

        @Override
        public Properties getProperties() {
            return new Properties(context.getProperties());
        }

        @Override
        public Precedence getPrecedence() {
            return precedence;
        }

        @Override
        public ClassLoader getClassLoader() {
            return context.getClassLoader();
        }

        @Override
        public ListableClassReaderSource getClassSource() {
            return context.getClassSource();
        }
    }
}
