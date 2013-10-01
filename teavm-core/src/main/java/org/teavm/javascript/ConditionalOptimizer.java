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

import java.util.Map;
import org.teavm.javascript.ast.*;

/**
 *
 * @author Alexey Andreev
 */
class ConditionalOptimizer {
    public Map<IdentifiedStatement, Integer> referencedStatements;
    public ReadWriteStatsBuilder stats;

    public Statement tryOptimizeElse(BlockStatement stmt) {
        if (stmt.getBody().isEmpty()) {
            return stmt;
        }
        if (!(stmt.getBody().get(0) instanceof ConditionalStatement)) {
            return stmt;
        }
        ConditionalStatement condStmt = (ConditionalStatement)stmt.getBody().get(0);
        if (condStmt.getAlternative() != null) {
            return stmt;
        }
        if (!(condStmt.getConsequent() instanceof SequentialStatement)) {
            return stmt;
        }
        SequentialStatement condBody = (SequentialStatement)condStmt.getConsequent();
        if (condBody.getSequence().isEmpty()) {
            return stmt;
        }
        Statement lastStmt = condBody.getSequence().get(condBody.getSequence().size() - 1);
        if (!(lastStmt instanceof BreakStatement)) {
            return stmt;
        }
        BreakStatement breakStmt = (BreakStatement)lastStmt;
        if (breakStmt.getTarget() != stmt) {
            return stmt;
        }
        SequentialStatement altBody = new SequentialStatement();
        for (int i = 1; i < stmt.getBody().size(); ++i) {
            altBody.getSequence().add(stmt.getBody().get(i));
        }
        if (!altBody.getSequence().isEmpty()) {
            condStmt.setAlternative(altBody.getSequence().size() != 1 ?
                    altBody : altBody.getSequence().get(0));
        }
        condBody.getSequence().remove(condBody.getSequence().size() - 1);
        if (condBody.getSequence().size() == 1) {
            condStmt.setConsequent(condBody.getSequence().get(0));
        }
        stmt.getBody().clear();
        stmt.getBody().add(condStmt);
        referencedStatements.put(stmt, referencedStatements.get(stmt) - 1);
        if (referencedStatements.get(stmt) > 0) {
            return stmt;
        } else {
            return tryMakeInline(condStmt);
        }
    }

    public Statement tryOptimize(BlockStatement stmt) {
        Expr condition = null;
        while (true) {
            if (stmt.getBody().isEmpty()) {
                break;
            }
            if (!(stmt.getBody().get(0) instanceof ConditionalStatement)) {
                break;
            }
            ConditionalStatement condStmt = (ConditionalStatement)stmt.getBody().get(0);
            if (condStmt.getAlternative() != null) {
                break;
            }
            if (!(condStmt.getConsequent() instanceof BreakStatement)) {
                break;
            }
            BreakStatement breakStmt = (BreakStatement)condStmt.getConsequent();
            if (breakStmt.getTarget() != stmt) {
                break;
            }
            stmt.getBody().remove(0);
            if (condition == null) {
                condition = ExprOptimizer.invert(condStmt.getCondition());
            } else {
                condition = Expr.binary(BinaryOperation.AND, condition,
                        ExprOptimizer.invert(condStmt.getCondition()));
            }
            referencedStatements.put(stmt, referencedStatements.get(stmt) - 1);
        }
        if (condition == null) {
            return stmt;
        }
        ConditionalStatement newCond = new ConditionalStatement();
        newCond.setCondition(condition);
        if (referencedStatements.get(stmt) > 0) {
            newCond.setConsequent(stmt);
        } else {
            if (stmt.getBody().size() == 1) {
                newCond.setConsequent(stmt.getBody().get(0));
            } else {
                SequentialStatement consequent = new SequentialStatement();
                consequent.getSequence().addAll(stmt.getBody());
                newCond.setConsequent(consequent);
            }
        }
        return newCond;
    }

    public void tryOptimize(WhileStatement stmt) {
        if (stmt.getBody().isEmpty()) {
            return;
        }
        if (!(stmt.getBody().get(0) instanceof ConditionalStatement)) {
            return;
        }
        if (stmt.getCondition() != null) {
            return;
        }
        ConditionalStatement condStmt = (ConditionalStatement)stmt.getBody().get(0);
        if (condStmt.getAlternative() != null) {
            return;
        }
        if (!(condStmt.getConsequent() instanceof BreakStatement)) {
            return;
        }
        BreakStatement breakStmt = (BreakStatement)condStmt.getConsequent();
        if (breakStmt.getTarget() != stmt) {
            return;
        }
        stmt.getBody().remove(0);
        stmt.setCondition(ExprOptimizer.invert(condStmt.getCondition()));
    }

    public Statement tryMakeInline(ConditionalStatement stmt) {
        if (!(stmt.getConsequent() instanceof AssignmentStatement) ||
                !(stmt.getAlternative() instanceof AssignmentStatement)) {
            return stmt;
        }
        AssignmentStatement consequent = (AssignmentStatement)stmt.getConsequent();
        AssignmentStatement alternative = (AssignmentStatement)stmt.getAlternative();
        if (!(consequent.getLeftValue() instanceof VariableExpr) ||
                !(alternative.getLeftValue() instanceof VariableExpr)) {
            return stmt;
        }
        VariableExpr consequentLeft = (VariableExpr)consequent.getLeftValue();
        VariableExpr alternativeLeft = (VariableExpr)alternative.getLeftValue();
        if (consequentLeft.getIndex() != alternativeLeft.getIndex()) {
            return stmt;
        }
        AssignmentStatement result = new AssignmentStatement();
        result.setLeftValue(consequentLeft);
        ConditionalExpr rightValue = new ConditionalExpr();
        rightValue.setCondition(stmt.getCondition());
        rightValue.setConsequent(consequent.getRightValue());
        rightValue.setAlternative(alternative.getRightValue());
        result.setRightValue(rightValue);
        stats.writes[consequentLeft.getIndex()]--;
        return result;
    }

    public Statement tryOptimizeSwitch(BlockStatement stmt) {
        if (stmt.getBody().size() < 2) {
            return stmt;
        }
        if (!(stmt.getBody().get(0) instanceof SwitchStatement)) {
            return stmt;
        }
        SwitchStatement switchStmt = (SwitchStatement)stmt.getBody().get(0);
        Statement last = stmt.getBody().get(stmt.getBody().size() - 1);
        if (!(last instanceof BreakStatement) && !(last instanceof ContinueStatement) &&
                !(last instanceof ReturnStatement) && !(last instanceof ThrowStatement)) {
            return stmt;
        }
        SequentialStatement seqStmt = new SequentialStatement();
        for (int i = 1; i < stmt.getBody().size(); ++i) {
            seqStmt.getSequence().add(stmt.getBody().get(i));
        }
        int count = referencedStatements.get(stmt);
        ReferenceCountingVisitor refCounter = new ReferenceCountingVisitor(stmt);
        switchStmt.acceptVisitor(refCounter);
        if (count > refCounter.count) {
            return stmt;
        }
        referencedStatements.put(stmt, 0);
        for (SwitchClause clause : switchStmt.getClauses()) {
            if (!(clause.getStatement() instanceof BreakStatement)) {
                continue;
            }
            BreakStatement breakStmt = (BreakStatement)clause.getStatement();
            if (breakStmt.getTarget() == stmt) {
                referencedStatements.put(stmt, referencedStatements.get(stmt) - 1);
                Integer switchRefs = referencedStatements.get(switchStmt);
                referencedStatements.put(switchStmt, (switchRefs != null ? switchRefs : 0) + 1);
                breakStmt.setTarget(switchStmt);
            } else if (breakStmt.getTarget() == switchStmt) {
                clause.setStatement(seqStmt);
            }
        }
        if (switchStmt.getDefaultClause() instanceof BreakStatement) {
            BreakStatement breakStmt = (BreakStatement)switchStmt.getDefaultClause();
            if (breakStmt.getTarget() == stmt) {
                referencedStatements.put(stmt, referencedStatements.get(stmt) - 1);
                Integer switchRefs = referencedStatements.get(switchStmt);
                referencedStatements.put(switchStmt, (switchRefs != null ? switchRefs : 0) + 1);
                breakStmt.setTarget(switchStmt);
            } else if (breakStmt.getTarget() == switchStmt) {
                switchStmt.setDefaultClause(seqStmt);
            }
        }
        return switchStmt;
    }
}
