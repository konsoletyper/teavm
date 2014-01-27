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
    private Map<IdentifiedStatement, IdentifiedStatement> copies = new HashMap<>();
    private Map<IdentifiedStatement, Integer> referencedStatements = new HashMap<>();
    private ConditionalOptimizer conditionalOptimizer = new ConditionalOptimizer();
    private List<Statement> resultSequence;

    public OptimizingVisitor(ReadWriteStatsBuilder stats) {
        this.stats = stats;
        conditionalOptimizer.referencedStatements = referencedStatements;
        conditionalOptimizer.stats = stats;
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
        resultExpr = Expr.binary(expr.getOperation(), a, b);
    }

    @Override
    public void visit(UnaryExpr expr) {
        expr.getOperand().acceptVisitor(this);
        Expr operand = resultExpr;
        resultExpr = Expr.unary(expr.getOperation(), operand);
    }

    @Override
    public void visit(ConditionalExpr expr) {
        expr.getCondition().acceptVisitor(this);
        Expr cond = resultExpr;
        expr.getConsequent().acceptVisitor(this);
        Expr consequent = resultExpr;
        expr.getAlternative().acceptVisitor(this);
        Expr alternative = resultExpr;
        ConditionalExpr result = new ConditionalExpr();
        result.setCondition(cond);
        result.setConsequent(consequent);
        result.setAlternative(alternative);
        resultExpr = result;
    }

    @Override
    public void visit(ConstantExpr expr) {
        resultExpr = Expr.constant(expr.getValue());
    }

    @Override
    public void visit(VariableExpr expr) {
        int index = expr.getIndex();
        resultExpr = Expr.var(index);
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
        resultExpr = Expr.subscript(array, index);
    }

    @Override
    public void visit(UnwrapArrayExpr expr) {
        expr.getArray().acceptVisitor(this);
        Expr arrayExpr = resultExpr;
        UnwrapArrayExpr result = new UnwrapArrayExpr(expr.getElementType());
        result.setArray(arrayExpr);
        resultExpr = result;
    }

    @Override
    public void visit(InvocationExpr expr) {
        Expr[] args = new Expr[expr.getArguments().size()];
        for (int i = expr.getArguments().size() - 1; i >= 0; --i) {
            expr.getArguments().get(i).acceptVisitor(this);
            args[i] = resultExpr;
        }
        switch (expr.getType()) {
            case STATIC:
                resultExpr = Expr.invokeStatic(expr.getMethod(), args);
                break;
            case DYNAMIC:
                resultExpr = Expr.invoke(expr.getMethod(), args[0], Arrays.copyOfRange(args, 1, args.length));
                break;
            case SPECIAL:
                resultExpr = Expr.invokeSpecial(expr.getMethod(), args[0], Arrays.copyOfRange(args, 1, args.length));
                break;
            case CONSTRUCTOR:
                resultExpr = Expr.constructObject(expr.getMethod(), args);
                break;
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
        resultExpr = Expr.qualify(qualified, expr.getField());
    }

    @Override
    public void visit(NewExpr expr) {
        resultExpr = Expr.createObject(expr.getConstructedClass());
    }

    @Override
    public void visit(NewArrayExpr expr) {
        expr.getLength().acceptVisitor(this);
        Expr length = resultExpr;
        resultExpr = Expr.createArray(expr.getType(), length);
    }

    @Override
    public void visit(NewMultiArrayExpr expr) {
        NewMultiArrayExpr result = new NewMultiArrayExpr();
        result.setType(expr.getType());
        for (Expr dimension : expr.getDimensions()) {
            dimension.acceptVisitor(this);
            result.getDimensions().add(resultExpr);
        }
        resultExpr = result;
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        expr.getExpr().acceptVisitor(this);
        Expr value = resultExpr;
        resultExpr = Expr.instanceOf(value, expr.getType());
    }

    @Override
    public void visit(StaticClassExpr expr) {
        resultExpr = Expr.staticClass(expr.getType());
    }

    @Override
    public void visit(AssignmentStatement statement) {
        if (statement.getLeftValue() == null) {
            statement.getRightValue().acceptVisitor(this);
            if (resultExpr instanceof InvocationExpr &&
                    tryApplyConstructor((InvocationExpr)resultExpr)) {
                resultStmt = new SequentialStatement();
            } else {
                resultStmt = Statement.assign(null, resultExpr);
            }
        } else {
            statement.getRightValue().acceptVisitor(this);
            Expr right = resultExpr;
            Expr left = statement.getLeftValue();
            if (!(statement.getLeftValue() instanceof VariableExpr)) {
                statement.getLeftValue().acceptVisitor(this);
                left = resultExpr;
            }
            resultStmt = Statement.assign(left, right);
        }
    }

    private List<Statement> processSequence(List<Statement> statements, boolean strict) {
        List<Statement> backup = resultSequence;
        List<Statement> result = new ArrayList<>();
        if (strict) {
            resultSequence = result;
        }
        for (Statement part : statements) {
            part.acceptVisitor(this);
            if (resultStmt != null) {
                resultSequence = result;
                if (resultStmt instanceof SequentialStatement) {
                    result.addAll(((SequentialStatement)resultStmt).getSequence());
                } else {
                    result.add(resultStmt);
                }
            }
        }
        resultSequence = backup;
        return result;
    }

    @Override
    public void visit(SequentialStatement statement) {
        List<Statement> statements = processSequence(statement.getSequence(), false);
        if (statements.isEmpty()) {
            resultStmt = null;
            return;
        }
        if (statements.size() == 1) {
            resultStmt = statements.get(0);
        } else {
            SequentialStatement result = new SequentialStatement();
            result.getSequence().addAll(statements);
            resultStmt = result;
        }
    }

    @Override
    public void visit(ConditionalStatement statement) {
        statement.getCondition().acceptVisitor(this);
        Expr predicate = resultExpr;
        List<Statement> sequenceBackup = resultSequence;
        resultSequence = new ArrayList<>();
        statement.getConsequent().acceptVisitor(this);
        Statement consequent = resultStmt;
        Statement alternative = null;
        if (statement.getAlternative() != null) {
            statement.getAlternative().acceptVisitor(this);
            alternative = resultStmt;
        }
        if (consequent == null) {
            if (alternative != null) {
                Statement tmp = alternative;
                alternative = consequent;
                consequent = tmp;
                predicate = ExprOptimizer.invert(predicate);
            } else {
                consequent = Statement.empty();
            }
        }
        resultStmt = conditionalOptimizer.tryMakeInline(
                (ConditionalStatement)Statement.cond(predicate, consequent, alternative));
        resultSequence = sequenceBackup;
    }

    private void visitIdentified(IdentifiedStatement stmt, IdentifiedStatement copy) {
        copies.put(stmt, copy);
    }

    @Override
    public void visit(SwitchStatement statement) {
        SwitchStatement result = new SwitchStatement();
        result.setId(statement.getId());
        visitIdentified(statement, result);
        statement.getValue().acceptVisitor(this);
        result.setValue(resultExpr);
        for (SwitchClause clause : statement.getClauses()) {
            clause.getStatement().acceptVisitor(this);
            SwitchClause resultClause = new SwitchClause();
            resultClause.setConditions(clause.getConditions());
            resultClause.setStatement(resultStmt != null ? resultStmt : Statement.empty());
            result.getClauses().add(resultClause);
        }
        if (statement.getDefaultClause() != null) {
            statement.getDefaultClause().acceptVisitor(this);
        } else {
            resultStmt = null;
        }
        result.setDefaultClause(resultStmt != null ? resultStmt : Statement.empty());
        resultStmt = result;
    }

    @Override
    public void visit(WhileStatement statement) {
        WhileStatement result = new WhileStatement();
        result.setId(statement.getId());
        visitIdentified(statement, result);
        List<Statement> statements = processSequence(statement.getBody(), true);
        if (statement.getCondition() != null) {
            statement.getCondition().acceptVisitor(this);
            result.setCondition(resultExpr);
        } else {
            result.setCondition(null);
        }
        result.getBody().addAll(statements);
        conditionalOptimizer.tryOptimize(result);
        resultStmt = result;
    }

    @Override
    public void visit(BlockStatement statement) {
        BlockStatement result = new BlockStatement();
        result.setId(statement.getId());
        visitIdentified(statement, result);
        List<Statement> statements = processSequence(statement.getBody(), false);
        result.getBody().addAll(statements);
        if (referencedStatements.containsKey(result)) {
            resultStmt = conditionalOptimizer.tryOptimize(result);
        } else {
            SequentialStatement altResult = new SequentialStatement();
            altResult.getSequence().addAll(result.getBody());
            resultStmt = altResult;
        }
        if (resultStmt instanceof BlockStatement) {
            resultStmt = conditionalOptimizer.tryOptimizeElse((BlockStatement)resultStmt);
        }
        if (resultStmt instanceof BlockStatement) {
            resultStmt = conditionalOptimizer.tryOptimizeSwitch((BlockStatement)resultStmt);
        }
        if (resultStmt instanceof ConditionalStatement) {
            ConditionalStatement conditional = (ConditionalStatement)resultStmt;
            conditional.getCondition().acceptVisitor(this);
            conditional.setCondition(resultExpr);
        }
    }

    @Override
    public void visit(ForStatement statement) {
    }

    @Override
    public void visit(BreakStatement statement) {
        BreakStatement result = new BreakStatement();
        if (statement.getTarget() != null) {
            IdentifiedStatement targetCopy = copies.get(statement.getTarget());
            result.setTarget(targetCopy);
            Integer refCount = referencedStatements.get(targetCopy);
            if (refCount == null) {
                refCount = 0;
            }
            referencedStatements.put(targetCopy, refCount + 1);
        }
        resultStmt = result;
    }

    @Override
    public void visit(ContinueStatement statement) {
        ContinueStatement result = new ContinueStatement();
        if (statement.getTarget() != null) {
            IdentifiedStatement targetCopy = copies.get(statement.getTarget());
            result.setTarget(targetCopy);
            Integer refCount = referencedStatements.get(targetCopy);
            if (refCount == null) {
                refCount = 0;
            }
            referencedStatements.put(targetCopy, refCount + 1);
        }
        resultStmt = result;
    }

    @Override
    public void visit(ReturnStatement statement) {
        if (statement.getResult() == null) {
            resultStmt = Statement.exitFunction(null);
        } else {
            statement.getResult().acceptVisitor(this);
            resultStmt = Statement.exitFunction(resultExpr);
        }
    }

    @Override
    public void visit(ThrowStatement statement) {
        statement.getException().acceptVisitor(this);
        resultStmt = Statement.raiseException(resultExpr);
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
