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
package org.teavm.ast.optimization;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
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
import org.teavm.ast.IdentifiedStatement;
import org.teavm.ast.InitClassStatement;
import org.teavm.ast.InstanceOfExpr;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.MonitorEnterStatement;
import org.teavm.ast.MonitorExitStatement;
import org.teavm.ast.NewArrayExpr;
import org.teavm.ast.NewExpr;
import org.teavm.ast.NewMultiArrayExpr;
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
import org.teavm.ast.UnaryOperation;
import org.teavm.ast.UnwrapArrayExpr;
import org.teavm.ast.VariableExpr;
import org.teavm.ast.WhileStatement;
import org.teavm.model.TextLocation;

class OptimizingVisitor implements StatementVisitor, ExprVisitor {
    private Expr resultExpr;
    Statement resultStmt;
    private final boolean[] preservedVars;
    private final int[] writeFrequencies;
    private final int[] readFrequencies;
    private List<Statement> resultSequence;
    private boolean friendlyToDebugger;
    private TextLocation currentLocation;
    private Deque<TextLocation> locationStack = new LinkedList<>();
    private Deque<TextLocation> notNullLocationStack = new ArrayDeque<>();

    OptimizingVisitor(boolean[] preservedVars, int[] writeFrequencies, int[] readFrequencies,
            boolean friendlyToDebugger) {
        this.preservedVars = preservedVars;
        this.writeFrequencies = writeFrequencies;
        this.readFrequencies = readFrequencies;
        this.friendlyToDebugger = friendlyToDebugger;
    }

    private static boolean isZero(Expr expr) {
        return expr instanceof ConstantExpr && Integer.valueOf(0).equals(((ConstantExpr) expr).getValue());
    }

    private static boolean isComparison(Expr expr) {
        return expr instanceof BinaryExpr && ((BinaryExpr) expr).getOperation() == BinaryOperation.COMPARE;
    }

    private void pushLocation(TextLocation location) {
        locationStack.push(location);
        if (location != null) {
            if (currentLocation != null) {
                notNullLocationStack.push(currentLocation);
            }
            currentLocation = location;
        }
    }

    private void popLocation() {
        if (locationStack.pop() != null) {
            currentLocation = notNullLocationStack.pollFirst();
        }
    }

    @Override
    public void visit(BinaryExpr expr) {
        pushLocation(expr.getLocation());
        try {
            switch (expr.getOperation()) {
                case AND:
                case OR:
                    resultExpr = expr;
                    return;
                default:
                    break;
            }
            Expr b = expr.getSecondOperand();
            Expr a = expr.getFirstOperand();

            if (a instanceof VariableExpr || a instanceof ConstantExpr) {
                b.acceptVisitor(this);
                b = resultExpr;
                if (b instanceof ConstantExpr && expr.getOperation() == BinaryOperation.SUBTRACT) {
                    if (tryMakePositive((ConstantExpr) b)) {
                        expr.setOperation(BinaryOperation.ADD);
                    }
                }
                a.acceptVisitor(this);
                a = resultExpr;
            }

            Expr p = a;
            Expr q = b;
            boolean invert = false;
            if (isZero(p)) {
                Expr tmp = p;
                p = q;
                q = tmp;
                invert = true;
            }
            if (isComparison(p) && isZero(q)) {
                switch (expr.getOperation()) {
                    case EQUALS:
                    case NOT_EQUALS:
                    case LESS:
                    case LESS_OR_EQUALS:
                    case GREATER:
                    case GREATER_OR_EQUALS: {
                        BinaryExpr comparison = (BinaryExpr) p;
                        Expr result = BinaryExpr.binary(expr.getOperation(), comparison.getType(),
                                comparison.getFirstOperand(), comparison.getSecondOperand());
                        result.setLocation(comparison.getLocation());
                        if (invert) {
                            result = ExprOptimizer.invert(result);
                        }
                        resultExpr = result;
                        return;
                    }
                    default:
                        break;
                }
            }
            expr.setFirstOperand(a);
            expr.setSecondOperand(b);
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(UnaryExpr expr) {
        pushLocation(expr.getLocation());
        try {
            expr.getOperand().acceptVisitor(this);
            Expr operand = resultExpr;
            if (expr.getOperation() == UnaryOperation.NEGATE && operand instanceof ConstantExpr) {
                ConstantExpr constantExpr = (ConstantExpr) operand;
                if (tryMakePositive(constantExpr)) {
                    resultExpr = expr;
                    return;
                }
            }
            expr.setOperand(operand);
            resultExpr = expr;
        } finally {
             popLocation();
        }
    }

    private boolean tryMakePositive(ConstantExpr constantExpr) {
        Object value = constantExpr.getValue();
        if (value instanceof Integer && (Integer) value < 0) {
            constantExpr.setValue(-(Integer) value);
            return true;
        } else if (value instanceof Float && (Float) value < 0) {
            constantExpr.setValue(-(Float) value);
            return true;
        } else if (value instanceof Byte && (Byte) value < 0) {
            constantExpr.setValue(-(Byte) value);
            return true;
        } else if (value instanceof Short && (Short) value < 0) {
            constantExpr.setValue(-(Short) value);
            return true;
        } else if (value instanceof Long && (Long) value < 0) {
            constantExpr.setValue(-(Long) value);
            return true;
        } else if (value instanceof Double && (Double) value < 0) {
            constantExpr.setValue(-(Double) value);
            return true;
        }
        return false;
    }

    @Override
    public void visit(ConditionalExpr expr) {
        pushLocation(expr.getLocation());
        try {
            expr.getCondition().acceptVisitor(this);
            Expr cond = resultExpr;
            expr.getConsequent().acceptVisitor(this);
            Expr consequent = resultExpr;
            expr.getAlternative().acceptVisitor(this);
            Expr alternative = resultExpr;
            expr.setCondition(cond);
            expr.setConsequent(consequent);
            expr.setAlternative(alternative);
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(ConstantExpr expr) {
        resultExpr = expr;
    }

    @Override
    public void visit(VariableExpr expr) {
        pushLocation(expr.getLocation());
        try {
            int index = expr.getIndex();
            resultExpr = expr;
            if (writeFrequencies[index] != 1) {
                return;
            }
            if (readFrequencies[index] != 1 || preservedVars[index]) {
                return;
            }
            if (resultSequence.isEmpty()) {
                return;
            }
            Statement last = resultSequence.get(resultSequence.size() - 1);
            if (!(last instanceof AssignmentStatement)) {
                return;
            }
            AssignmentStatement assignment = (AssignmentStatement) last;
            if (assignment.isAsync()) {
                return;
            }
            if (!(assignment.getLeftValue() instanceof VariableExpr)) {
                return;
            }
            VariableExpr var = (VariableExpr) assignment.getLeftValue();
            if (friendlyToDebugger) {
                if (currentLocation != null && assignment.getLocation() != null
                        && !assignment.getLocation().equals(currentLocation)) {
                    return;
                }
            }
            if (var.getIndex() == index) {
                resultSequence.remove(resultSequence.size() - 1);
                assignment.getRightValue().setLocation(assignment.getLocation());
                assignment.getRightValue().acceptVisitor(this);
            }
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(SubscriptExpr expr) {
        pushLocation(expr.getLocation());
        try {
            expr.getIndex().acceptVisitor(this);
            Expr index = resultExpr;
            expr.getArray().acceptVisitor(this);
            Expr array = resultExpr;
            expr.setArray(array);
            expr.setIndex(index);
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(UnwrapArrayExpr expr) {
        pushLocation(expr.getLocation());
        try {
            expr.getArray().acceptVisitor(this);
            Expr arrayExpr = resultExpr;
            expr.setArray(arrayExpr);
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(InvocationExpr expr) {
        pushLocation(expr.getLocation());
        try {
            Expr[] args = new Expr[expr.getArguments().size()];
            for (int i = expr.getArguments().size() - 1; i >= 0; --i) {
                expr.getArguments().get(i).acceptVisitor(this);
                args[i] = resultExpr;
            }
            for (int i = 0; i < args.length; ++i) {
                expr.getArguments().set(i, args[i]);
            }
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    private boolean tryApplyConstructor(InvocationExpr expr) {
        if (!expr.getMethod().getName().equals("<init>")) {
            return false;
        }
        if (resultSequence == null || resultSequence.isEmpty()) {
            return false;
        }
        Statement last = resultSequence.get(resultSequence.size() - 1);
        if (!(last instanceof AssignmentStatement)) {
            return false;
        }
        AssignmentStatement assignment = (AssignmentStatement) last;
        if (!(assignment.getLeftValue() instanceof VariableExpr)) {
            return false;
        }
        VariableExpr var = (VariableExpr) assignment.getLeftValue();
        if (!(expr.getArguments().get(0) instanceof VariableExpr)) {
            return false;
        }
        VariableExpr target = (VariableExpr) expr.getArguments().get(0);
        if (target.getIndex() != var.getIndex()) {
            return false;
        }
        if (!(assignment.getRightValue() instanceof NewExpr)) {
            return false;
        }
        NewExpr constructed = (NewExpr) assignment.getRightValue();
        if (!constructed.getConstructedClass().equals(expr.getMethod().getClassName())) {
            return false;
        }
        Expr[] args = expr.getArguments().toArray(new Expr[0]);
        args = Arrays.copyOfRange(args, 1, args.length);
        InvocationExpr constructrExpr = Expr.constructObject(expr.getMethod(), args);
        constructrExpr.setLocation(expr.getLocation());
        assignment.setRightValue(constructrExpr);
        readFrequencies[var.getIndex()]--;
        return true;
    }

    @Override
    public void visit(QualificationExpr expr) {
        pushLocation(expr.getLocation());
        try {
            if (expr.getQualified() != null) {
                expr.getQualified().acceptVisitor(this);
                Expr qualified = resultExpr;
                expr.setQualified(qualified);
            }
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(NewExpr expr) {
        resultExpr = expr;
    }

    @Override
    public void visit(NewArrayExpr expr) {
        pushLocation(expr.getLocation());
        try {
            expr.getLength().acceptVisitor(this);
            Expr length = resultExpr;
            expr.setLength(length);
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(NewMultiArrayExpr expr) {
        pushLocation(expr.getLocation());
        try {
            for (int i = 0; i < expr.getDimensions().size(); ++i) {
                Expr dimension = expr.getDimensions().get(i);
                dimension.acceptVisitor(this);
                expr.getDimensions().set(i, resultExpr);
            }
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        pushLocation(expr.getLocation());
        try {
            expr.getExpr().acceptVisitor(this);
            expr.setExpr(resultExpr);
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(CastExpr expr) {
        pushLocation(expr.getLocation());
        try {
            expr.getValue().acceptVisitor(this);
            expr.setValue(resultExpr);
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(PrimitiveCastExpr expr) {
        pushLocation(expr.getLocation());
        try {
            expr.getValue().acceptVisitor(this);
            expr.setValue(resultExpr);
            resultExpr = expr;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(AssignmentStatement statement) {
        pushLocation(statement.getLocation());
        try {
            if (statement.getLeftValue() == null) {
                statement.getRightValue().acceptVisitor(this);
                if (resultExpr instanceof InvocationExpr && tryApplyConstructor((InvocationExpr) resultExpr)) {
                    resultStmt = new SequentialStatement();
                } else {
                    statement.setRightValue(resultExpr);
                    resultStmt = statement;
                }
            } else {
                statement.getRightValue().acceptVisitor(this);
                Expr right = resultExpr;
                Expr left = statement.getLeftValue();
                if (!(statement.getLeftValue() instanceof VariableExpr)) {
                    statement.getLeftValue().acceptVisitor(this);
                    left = resultExpr;
                }
                statement.setLeftValue(left);
                statement.setRightValue(right);
                resultStmt = statement;
            }
        } finally {
            popLocation();
        }
    }

    private List<Statement> processSequence(List<Statement> statements) {
        List<Statement> backup = resultSequence;
        resultSequence = new ArrayList<>();
        processSequenceImpl(statements);
        wieldTryCatch(resultSequence);
        List<Statement> result = resultSequence.stream().filter(part -> part != null).collect(Collectors.toList());
        resultSequence = backup;
        return result;
    }

    private boolean processSequenceImpl(List<Statement> statements) {
        for (Statement part : statements) {
            if (part instanceof SequentialStatement) {
                if (!processSequenceImpl(((SequentialStatement) part).getSequence())) {
                    return false;
                }
                continue;
            }
            part.acceptVisitor(this);
            part = resultStmt;
            if (part instanceof SequentialStatement) {
                if (!processSequenceImpl(((SequentialStatement) part).getSequence())) {
                    return false;
                }
                continue;
            }
            resultSequence.add(part);
            if (part instanceof BreakStatement) {
                return false;
            }
        }
        return true;
    }

    private void wieldTryCatch(List<Statement> statements) {
        for (int i = 0; i < statements.size() - 1; ++i) {
            if (statements.get(i) instanceof TryCatchStatement && statements.get(i + 1) instanceof TryCatchStatement) {
                TryCatchStatement first = (TryCatchStatement) statements.get(i);
                TryCatchStatement second = (TryCatchStatement) statements.get(i + 1);
                if (Objects.equals(first.getExceptionType(), second.getExceptionType())
                        && Objects.equals(first.getExceptionVariable(), second.getExceptionVariable())
                        && briefStatementComparison(first.getHandler(), second.getHandler())) {
                    first.getProtectedBody().addAll(second.getProtectedBody());
                    statements.remove(i + 1);
                    wieldTryCatch(first.getProtectedBody());
                    --i;
                }
            }
        }
    }

    private boolean briefStatementComparison(List<Statement> firstSeq, List<Statement> secondSeq) {
        if (firstSeq.isEmpty() && secondSeq.isEmpty()) {
            return true;
        }
        if (firstSeq.size() != 1 || secondSeq.size() != 1) {
            return false;
        }
        Statement first = firstSeq.get(0);
        Statement second = secondSeq.get(0);
        if (first instanceof BreakStatement && second instanceof BreakStatement) {
            BreakStatement firstBreak = (BreakStatement) first;
            BreakStatement secondBreak = (BreakStatement) second;
            return firstBreak.getTarget() == secondBreak.getTarget();
        }
        return false;
    }

    private void eliminateRedundantBreaks(List<Statement> statements, IdentifiedStatement exit) {
        if (statements.isEmpty()) {
            return;
        }
        Statement last = statements.get(statements.size() - 1);
        if (last instanceof BreakStatement && exit != null) {
            IdentifiedStatement target = ((BreakStatement) last).getTarget();
            if (exit == target) {
                statements.remove(statements.size() - 1);
            }
        }
        if (statements.isEmpty()) {
            return;
        }
        for (int i = 0; i < statements.size(); ++i) {
            Statement stmt = statements.get(i);
            if (stmt instanceof ConditionalStatement) {
                ConditionalStatement cond = (ConditionalStatement) stmt;
                check_conditional: {
                    last = cond.getConsequent().isEmpty() ? null
                            : cond.getConsequent().get(cond.getConsequent().size() - 1);
                    if (last instanceof BreakStatement) {
                        BreakStatement breakStmt = (BreakStatement) last;
                        if (exit != null && exit == breakStmt.getTarget()) {
                            cond.getConsequent().remove(cond.getConsequent().size() - 1);
                            List<Statement> remaining = statements.subList(i + 1, statements.size());
                            cond.getAlternative().addAll(remaining);
                            remaining.clear();
                            break check_conditional;
                        }
                    }
                    last = cond.getAlternative().isEmpty() ? null
                            : cond.getAlternative().get(cond.getAlternative().size() - 1);
                    if (last instanceof BreakStatement) {
                        BreakStatement breakStmt = (BreakStatement) last;
                        if (exit != null && exit == breakStmt.getTarget()) {
                            cond.getAlternative().remove(cond.getAlternative().size() - 1);
                            List<Statement> remaining = statements.subList(i + 1, statements.size());
                            cond.getConsequent().addAll(remaining);
                            remaining.clear();
                        }
                    }
                }
                if (i == statements.size() - 1) {
                    eliminateRedundantBreaks(cond.getConsequent(), exit);
                    eliminateRedundantBreaks(cond.getAlternative(), exit);
                }
                normalizeConditional(cond);
                if (cond.getConsequent().size() == 1 && cond.getConsequent().get(0) instanceof ConditionalStatement) {
                    ConditionalStatement innerCond = (ConditionalStatement) cond.getConsequent().get(0);
                    if (innerCond.getAlternative().isEmpty()) {
                        if (cond.getAlternative().isEmpty()) {
                            cond.getConsequent().clear();
                            cond.getConsequent().addAll(innerCond.getConsequent());
                            cond.setCondition(Expr.binary(BinaryOperation.AND, null, cond.getCondition(),
                                    innerCond.getCondition(), cond.getCondition().getLocation()));
                            --i;
                        } else if (cond.getAlternative().size() != 1
                                || !(cond.getAlternative().get(0) instanceof ConditionalStatement)) {
                            cond.setCondition(ExprOptimizer.invert(cond.getCondition()));
                            cond.getConsequent().clear();
                            cond.getConsequent().addAll(cond.getAlternative());
                            cond.getAlternative().clear();
                            cond.getAlternative().add(innerCond);
                            --i;
                        }
                    }
                }
            } else if (stmt instanceof BlockStatement) {
                BlockStatement nestedBlock = (BlockStatement) stmt;
                eliminateRedundantBreaks(nestedBlock.getBody(), nestedBlock);
            } else if (stmt instanceof WhileStatement) {
                WhileStatement whileStmt = (WhileStatement) stmt;
                eliminateRedundantBreaks(whileStmt.getBody(), null);
            } else if (stmt instanceof SwitchStatement) {
                SwitchStatement switchStmt = (SwitchStatement) stmt;
                for (SwitchClause clause : switchStmt.getClauses()) {
                    eliminateRedundantBreaks(clause.getBody(), null);
                }
                eliminateRedundantBreaks(switchStmt.getDefaultClause(), null);
            }
        }
    }


    private void normalizeConditional(ConditionalStatement stmt) {
        if (stmt.getConsequent().isEmpty()) {
            stmt.getConsequent().addAll(stmt.getAlternative());
            stmt.getAlternative().clear();
            stmt.setCondition(ExprOptimizer.invert(stmt.getCondition()));
        }
    }

    @Override
    public void visit(SequentialStatement statement) {
        List<Statement> statements = processSequence(statement.getSequence());
        if (statements.size() == 1) {
            resultStmt = statements.get(0);
        } else {
            statement.getSequence().clear();
            statement.getSequence().addAll(statements);
            resultStmt = statement;
        }
    }

    @Override
    public void visit(ConditionalStatement statement) {
        statement.getCondition().acceptVisitor(this);
        statement.setCondition(resultExpr);
        List<Statement> consequent = processSequence(statement.getConsequent());
        List<Statement> alternative = processSequence(statement.getAlternative());
        if (consequent.isEmpty()) {
            consequent.addAll(alternative);
            alternative.clear();
            statement.setCondition(ExprOptimizer.invert(statement.getCondition()));
        }
        if (consequent.isEmpty()) {
            SequentialStatement sequentialStatement = new SequentialStatement();
            resultStmt = sequentialStatement;
            statement.getCondition().acceptVisitor(new ExpressionSideEffectDecomposer(
                    sequentialStatement.getSequence()));
            return;
        }
        statement.getConsequent().clear();
        statement.getConsequent().addAll(consequent);
        statement.getAlternative().clear();
        statement.getAlternative().addAll(alternative);

        Statement asConditional = tryConvertToConditionalExpression(statement);
        if (asConditional != null) {
            asConditional.acceptVisitor(this);
        } else {
            resultStmt = statement;
        }
    }

    private Statement tryConvertToConditionalExpression(ConditionalStatement statement) {
        if (statement.getConsequent().size() != 1 || statement.getAlternative().size() != 1) {
            return null;
        }

        Statement first = statement.getConsequent().get(0);
        Statement second = statement.getAlternative().get(0);
        if (!(first instanceof AssignmentStatement) || !(second instanceof AssignmentStatement)) {
            return null;
        }

        AssignmentStatement firstAssignment = (AssignmentStatement) first;
        AssignmentStatement secondAssignment = (AssignmentStatement) second;
        if (firstAssignment.getLeftValue() == null || secondAssignment.getRightValue() == null) {
            return null;
        }
        if (firstAssignment.isAsync() || secondAssignment.isAsync()) {
            return null;
        }

        if (!(firstAssignment.getLeftValue() instanceof VariableExpr)
                || !(secondAssignment.getLeftValue() instanceof VariableExpr)) {
            return null;
        }
        VariableExpr firstLhs = (VariableExpr) firstAssignment.getLeftValue();
        VariableExpr secondLhs = (VariableExpr) secondAssignment.getLeftValue();
        if (firstLhs.getIndex() == secondLhs.getIndex()) {
            ConditionalExpr conditionalExpr = new ConditionalExpr();
            conditionalExpr.setCondition(statement.getCondition());
            conditionalExpr.setConsequent(firstAssignment.getRightValue());
            conditionalExpr.setAlternative(secondAssignment.getRightValue());
            conditionalExpr.setLocation(statement.getCondition().getLocation());
            AssignmentStatement assignment = new AssignmentStatement();
            assignment.setLocation(conditionalExpr.getLocation());
            VariableExpr lhs = new VariableExpr();
            lhs.setIndex(firstLhs.getIndex());
            assignment.setLeftValue(lhs);
            assignment.setRightValue(conditionalExpr);
            writeFrequencies[lhs.getIndex()]--;
            return assignment;
        }

        return null;
    }

    @Override
    public void visit(SwitchStatement statement) {
        statement.getValue().acceptVisitor(this);
        statement.setValue(resultExpr);
        for (SwitchClause clause : statement.getClauses()) {
            List<Statement> newBody = processSequence(clause.getBody());
            clause.getBody().clear();
            clause.getBody().addAll(newBody);
        }
        List<Statement> newDefault = processSequence(statement.getDefaultClause());
        statement.getDefaultClause().clear();
        statement.getDefaultClause().addAll(newDefault);
        resultStmt = statement;
    }

    @Override
    public void visit(WhileStatement statement) {
        if (statement.getBody().size() == 1 && statement.getBody().get(0) instanceof WhileStatement) {
            WhileStatement innerLoop = (WhileStatement) statement.getBody().get(0);
            BreakToContinueReplacer replacer = new BreakToContinueReplacer(innerLoop, statement);
            replacer.visit(innerLoop.getBody());
            statement.getBody().clear();
            statement.getBody().addAll(innerLoop.getBody());
        }
        List<Statement> statements = processSequence(statement.getBody());
        for (int i = 0; i < statements.size(); ++i) {
            if (statements.get(i) instanceof ContinueStatement) {
                ContinueStatement continueStmt = (ContinueStatement) statements.get(i);
                if (continueStmt.getTarget() == statement) {
                    statements.subList(i, statements.size()).clear();
                    break;
                }
            }
        }
        statement.getBody().clear();
        statement.getBody().addAll(statements);
        if (statement.getCondition() != null) {
            List<Statement> sequenceBackup = resultSequence;
            resultSequence = new ArrayList<>();
            statement.getCondition().acceptVisitor(this);
            statement.setCondition(resultExpr);
            resultSequence = sequenceBackup;
        }
        while (true) {
            if (!statement.getBody().isEmpty() && statement.getBody().get(0) instanceof ConditionalStatement) {
                ConditionalStatement cond = (ConditionalStatement) statement.getBody().get(0);
                if (cond.getConsequent().size() == 1 && cond.getConsequent().get(0) instanceof BreakStatement) {
                    BreakStatement breakStmt = (BreakStatement) cond.getConsequent().get(0);
                    if (breakStmt.getTarget() == statement) {
                        statement.getBody().remove(0);
                        if (statement.getCondition() != null) {
                            Expr newCondition = Expr.binary(BinaryOperation.AND, null, statement.getCondition(),
                                    ExprOptimizer.invert(cond.getCondition()));
                            newCondition.setLocation(statement.getCondition().getLocation());
                            statement.setCondition(newCondition);
                        } else {
                            statement.setCondition(ExprOptimizer.invert(cond.getCondition()));
                        }
                        continue;
                    }
                }
            }
            break;
        }
        resultStmt = statement;
    }

    @Override
    public void visit(BlockStatement statement) {
        List<Statement> statements = processSequence(statement.getBody());
        eliminateRedundantBreaks(statements, statement);
        BlockCountVisitor usageCounter = new BlockCountVisitor(statement);
        usageCounter.visit(statements);
        if (usageCounter.getCount() == 0) {
            SequentialStatement result = new SequentialStatement();
            result.getSequence().addAll(statements);
            resultStmt = result;
        } else {
            statement.getBody().clear();
            statement.getBody().addAll(statements);
            resultStmt = statement;
        }
    }

    @Override
    public void visit(BreakStatement statement) {
        resultStmt = statement;
    }

    @Override
    public void visit(ContinueStatement statement) {
        resultStmt = statement;
    }

    @Override
    public void visit(ReturnStatement statement) {
        pushLocation(statement.getLocation());
        try {
            if (statement.getResult() != null) {
                statement.getResult().acceptVisitor(this);
                statement.setResult(resultExpr);
            }
            resultStmt = statement;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(ThrowStatement statement) {
        pushLocation(statement.getLocation());
        try {
            statement.getException().acceptVisitor(this);
            statement.setException(resultExpr);
            resultStmt = statement;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(InitClassStatement statement) {
        pushLocation(statement.getLocation());
        try {
            resultStmt = statement;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(TryCatchStatement statement) {
        List<Statement> statements = processSequence(statement.getProtectedBody());
        statement.getProtectedBody().clear();
        statement.getProtectedBody().addAll(statements);
        statements = processSequence(statement.getHandler());
        statement.getHandler().clear();
        statement.getHandler().addAll(statements);
        resultStmt = statement;
    }

    @Override
    public void visit(GotoPartStatement statement) {
        resultStmt = statement;
    }

    @Override
    public void visit(MonitorEnterStatement statement) {
        pushLocation(statement.getLocation());
        try {
            statement.getObjectRef().acceptVisitor(this);
            statement.setObjectRef(resultExpr);
            resultStmt = statement;
        } finally {
            popLocation();
        }
    }

    @Override
    public void visit(MonitorExitStatement statement) {
        pushLocation(statement.getLocation());
        try {
            statement.getObjectRef().acceptVisitor(this);
            statement.setObjectRef(resultExpr);
            resultStmt = statement;
        } finally {
            popLocation();
        }
    }
}
