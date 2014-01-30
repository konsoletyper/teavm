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
package org.teavm.javascript;

import java.util.*;
import org.teavm.javascript.ast.*;

/**
 *
 * @author Alexey Andreev
 */
class OptimizingVisitor implements StatementVisitor, ExprVisitor {
    public Expr resultExpr;
    public Statement resultStmt;
    private ReadWriteStatsBuilder stats;
    Map<IdentifiedStatement, Integer> referencedStatements = new HashMap<>();
    private List<Statement> resultSequence;

    public OptimizingVisitor(ReadWriteStatsBuilder stats) {
        this.stats = stats;
    }

    private static boolean isZero(Expr expr) {
        return expr instanceof ConstantExpr &&
                Integer.valueOf(0).equals(((ConstantExpr)expr).getValue());
    }

    private static boolean isComparison(Expr expr) {
        return expr instanceof BinaryExpr &&
                ((BinaryExpr)expr).getOperation() == BinaryOperation.COMPARE;
    }

    @Override
    public void visit(BinaryExpr expr) {
        expr.getSecondOperand().acceptVisitor(this);
        Expr b = resultExpr;
        expr.getFirstOperand().acceptVisitor(this);
        Expr a = resultExpr;
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
                    BinaryExpr comparison = (BinaryExpr)p;
                    Expr result = BinaryExpr.binary(expr.getOperation(),
                            comparison.getFirstOperand(), comparison.getSecondOperand());
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
    }

    @Override
    public void visit(UnaryExpr expr) {
        expr.getOperand().acceptVisitor(this);
        Expr operand = resultExpr;
        expr.setOperand(operand);
        resultExpr = expr;
    }

    @Override
    public void visit(ConditionalExpr expr) {
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
    }

    @Override
    public void visit(ConstantExpr expr) {
        resultExpr = expr;
    }

    @Override
    public void visit(VariableExpr expr) {
        int index = expr.getIndex();
        resultExpr = expr;
        if (stats.reads[index] != 1 || stats.writes[index] != 1) {
            return;
        }
        if (resultSequence.isEmpty()) {
            return;
        }
        Statement last = resultSequence.get(resultSequence.size() - 1);
        if (!(last instanceof AssignmentStatement)) {
            return;
        }
        AssignmentStatement assignment = (AssignmentStatement)last;
        if (!(assignment.getLeftValue() instanceof VariableExpr)) {
            return;
        }
        VariableExpr var = (VariableExpr)assignment.getLeftValue();
        if (var.getIndex() == index) {
            resultSequence.remove(resultSequence.size() - 1);
            assignment.getRightValue().acceptVisitor(this);
        }
    }

    @Override
    public void visit(SubscriptExpr expr) {
        expr.getIndex().acceptVisitor(this);
        Expr index = resultExpr;
        expr.getArray().acceptVisitor(this);
        Expr array = resultExpr;
        expr.setArray(array);
        expr.setIndex(index);
        resultExpr = expr;
    }

    @Override
    public void visit(UnwrapArrayExpr expr) {
        expr.getArray().acceptVisitor(this);
        Expr arrayExpr = resultExpr;
        expr.setArray(arrayExpr);
        resultExpr = expr;
    }

    @Override
    public void visit(InvocationExpr expr) {
        Expr[] args = new Expr[expr.getArguments().size()];
        for (int i = expr.getArguments().size() - 1; i >= 0; --i) {
            expr.getArguments().get(i).acceptVisitor(this);
            args[i] = resultExpr;
        }
        for (int i = 0; i < args.length; ++i) {
            expr.getArguments().set(i, args[i]);
        }
        resultExpr = expr;
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
        AssignmentStatement assignment = (AssignmentStatement)last;
        if (!(assignment.getLeftValue() instanceof VariableExpr)) {
            return false;
        }
        VariableExpr var = (VariableExpr)assignment.getLeftValue();
        if (!(expr.getArguments().get(0) instanceof VariableExpr)) {
            return false;
        }
        VariableExpr target = (VariableExpr)expr.getArguments().get(0);
        if (target.getIndex() != var.getIndex()) {
            return false;
        }
        if (!(assignment.getRightValue() instanceof NewExpr)) {
            return false;
        }
        NewExpr constructed = (NewExpr)assignment.getRightValue();
        if (!constructed.getConstructedClass().equals(expr.getMethod().getClassName())) {
            return false;
        }
        Expr[] args = expr.getArguments().toArray(new Expr[0]);
        args = Arrays.copyOfRange(args, 1, args.length);
        assignment.setRightValue(Expr.constructObject(expr.getMethod(), args));
        stats.reads[var.getIndex()]--;
        return true;
    }

    @Override
    public void visit(QualificationExpr expr) {
        expr.getQualified().acceptVisitor(this);
        Expr qualified = resultExpr;
        expr.setQualified(qualified);
        resultExpr = expr;
    }

    @Override
    public void visit(NewExpr expr) {
        resultExpr = expr;
    }

    @Override
    public void visit(NewArrayExpr expr) {
        expr.getLength().acceptVisitor(this);
        Expr length = resultExpr;
        expr.setLength(length);
        resultExpr = expr;
    }

    @Override
    public void visit(NewMultiArrayExpr expr) {
        for (int i = 0; i < expr.getDimensions().size(); ++i) {
            Expr dimension = expr.getDimensions().get(i);
            dimension.acceptVisitor(this);
            expr.getDimensions().set(i, resultExpr);
        }
        resultExpr = expr;
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        expr.getExpr().acceptVisitor(this);
        expr.setExpr(resultExpr);
        resultExpr = expr;
    }

    @Override
    public void visit(StaticClassExpr expr) {
        resultExpr = expr;
    }

    @Override
    public void visit(AssignmentStatement statement) {
        if (statement.getLeftValue() == null) {
            statement.getRightValue().acceptVisitor(this);
            if (resultExpr instanceof InvocationExpr &&
                    tryApplyConstructor((InvocationExpr)resultExpr)) {
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
    }

    private List<Statement> processSequence(List<Statement> statements, boolean strict) {
        List<Statement> backup = resultSequence;
        List<Statement> result = new ArrayList<>();
        if (strict) {
            resultSequence = result;
        }
        outer: for (int i = 0; i < statements.size(); ++i) {
            Statement part = statements.get(i);
            part.acceptVisitor(this);
            List<Statement> newStatements = new ArrayList<>();
            if (resultStmt instanceof SequentialStatement) {
                newStatements.addAll(((SequentialStatement)resultStmt).getSequence());
            } else {
                newStatements.add(resultStmt);
            }
            for (int j = 0; j < newStatements.size(); ++j) {
                Statement newStatement = newStatements.get(j);
                resultSequence = result;
                result.add(newStatement);
                if (newStatement instanceof BreakStatement) {
                    break outer;
                }
            }
        }
        resultSequence = backup;
        return result;
    }

    private void eliminateRedundantBreaks(List<Statement> statements, IdentifiedStatement exit) {
        if (statements.isEmpty()) {
            return;
        }
        Statement last = statements.get(statements.size() - 1);
        if (last instanceof BreakStatement && exit != null) {
            IdentifiedStatement target = ((BreakStatement)last).getTarget();
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
                ConditionalStatement cond = (ConditionalStatement)stmt;
                check_conditional: {
                    last = cond.getConsequent().isEmpty() ? null :
                            cond.getConsequent().get(cond.getConsequent().size() - 1);
                    if (last instanceof BreakStatement) {
                        BreakStatement breakStmt = (BreakStatement)last;
                        if (exit != null && exit == breakStmt.getTarget()) {
                            int refs = referencedStatements.get(breakStmt.getTarget());
                            referencedStatements.put(breakStmt.getTarget(), refs - 1);
                            cond.getConsequent().remove(cond.getConsequent().size() - 1);
                            List<Statement> remaining = statements.subList(i + 1, statements.size());
                            cond.getAlternative().addAll(remaining);
                            remaining.clear();
                            break check_conditional;
                        }
                    }
                    last = cond.getAlternative().isEmpty() ? null :
                            cond.getAlternative().get(cond.getAlternative().size() - 1);
                    if (last instanceof BreakStatement) {
                        BreakStatement breakStmt = (BreakStatement)last;
                        if (exit != null && exit == breakStmt.getTarget()) {
                            int refs = referencedStatements.get(breakStmt.getTarget());
                            referencedStatements.put(breakStmt.getTarget(), refs - 1);
                            cond.getAlternative().remove(cond.getConsequent().size() - 1);
                            List<Statement> remaining = statements.subList(i + 1, statements.size());
                            cond.getConsequent().addAll(remaining);
                            remaining.clear();
                            break check_conditional;
                        }
                    }
                }
                if (i == statements.size() - 1) {
                    eliminateRedundantBreaks(cond.getConsequent(), exit);
                    eliminateRedundantBreaks(cond.getAlternative(), exit);
                }
                normalizeConditional(cond);
                if (cond.getConsequent().size() == 1 && cond.getConsequent().get(0) instanceof ConditionalStatement) {
                    ConditionalStatement innerCond = (ConditionalStatement)cond.getConsequent().get(0);
                    if (innerCond.getAlternative().isEmpty()) {
                        if (cond.getAlternative().isEmpty()) {
                            cond.getConsequent().clear();
                            cond.getConsequent().addAll(innerCond.getConsequent());
                            cond.setCondition(Expr.binary(BinaryOperation.AND, cond.getCondition(),
                                    innerCond.getCondition()));
                        } else {
                            cond.setCondition(ExprOptimizer.invert(cond.getCondition()));
                            cond.getConsequent().clear();
                            cond.getConsequent().addAll(cond.getAlternative());
                            cond.getAlternative().clear();
                            cond.getAlternative().add(innerCond);
                        }
                        --i;
                    }
                }
            } else if (stmt instanceof BlockStatement) {
                BlockStatement nestedBlock = (BlockStatement)stmt;
                eliminateRedundantBreaks(nestedBlock.getBody(), nestedBlock);
            } else if (stmt instanceof WhileStatement) {
                WhileStatement whileStmt = (WhileStatement)stmt;
                eliminateRedundantBreaks(whileStmt.getBody(), null);
            } else if (stmt instanceof SwitchStatement) {
                SwitchStatement switchStmt = (SwitchStatement)stmt;
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
        List<Statement> statements = processSequence(statement.getSequence(), false);
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
        List<Statement> consequent = processSequence(statement.getConsequent(), true);
        List<Statement> alternative = processSequence(statement.getAlternative(), true);
        if (consequent.isEmpty()) {
            consequent.addAll(alternative);
            alternative.clear();
            statement.setCondition(ExprOptimizer.invert(statement.getCondition()));
        }
        if (consequent.isEmpty()) {
            resultStmt = Statement.empty();
            return;
        }
        statement.getConsequent().clear();
        statement.getConsequent().addAll(consequent);
        statement.getAlternative().clear();
        statement.getAlternative().addAll(alternative);
        resultStmt = statement;
    }

    @Override
    public void visit(SwitchStatement statement) {
        statement.getValue().acceptVisitor(this);
        statement.setValue(resultExpr);
        for (SwitchClause clause : statement.getClauses()) {
            List<Statement> newBody = processSequence(clause.getBody(), true);
            clause.getBody().clear();
            clause.getBody().addAll(newBody);
        }
        List<Statement> newDefault = processSequence(statement.getDefaultClause(), true);
        statement.getDefaultClause().clear();
        statement.getDefaultClause().addAll(newDefault);
        resultStmt = statement;
    }

    @Override
    public void visit(WhileStatement statement) {
        List<Statement> statements = processSequence(statement.getBody(), true);
        statement.getBody().clear();
        statement.getBody().addAll(statements);
        if (statement.getCondition() != null) {
            statement.getCondition().acceptVisitor(this);
            statement.setCondition(resultExpr);
        }
        resultStmt = statement;
    }

    @Override
    public void visit(BlockStatement statement) {
        List<Statement> statements = processSequence(statement.getBody(), false);
        eliminateRedundantBreaks(statements, statement);
        if (referencedStatements.get(statement).equals(0)) {
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
    public void visit(ForStatement statement) {
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
        if (statement.getResult() != null) {
            statement.getResult().acceptVisitor(this);
            statement.setResult(resultExpr);
        }
        resultStmt = statement;
    }

    @Override
    public void visit(ThrowStatement statement) {
        statement.getException().acceptVisitor(this);
        statement.setException(resultExpr);
        resultStmt = statement;
    }

    @Override
    public void visit(IncrementStatement statement) {
        resultStmt = Statement.increment(statement.getVar(), statement.getAmount());
    }

    @Override
    public void visit(InitClassStatement statement) {
        resultStmt = statement;
    }
}
