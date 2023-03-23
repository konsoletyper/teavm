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
package org.teavm.ast;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Statement {
    public abstract void acceptVisitor(StatementVisitor visitor);

    public static Statement empty() {
        return new SequentialStatement();
    }

    public static AssignmentStatement assign(Expr left, Expr right) {
        var stmt = new AssignmentStatement();
        stmt.setLeftValue(left);
        stmt.setRightValue(right);
        return stmt;
    }

    public static AssignmentStatement statementExpr(Expr expr) {
        var stmt = new AssignmentStatement();
        stmt.setRightValue(expr);
        return stmt;
    }

    public static ReturnStatement exitFunction(Expr result) {
        var stmt = new ReturnStatement();
        stmt.setResult(result);
        return stmt;
    }

    public static SequentialStatement sequence(Statement... statements) {
        var seq = new SequentialStatement();
        seq.getSequence().addAll(Arrays.asList(statements));
        return seq;
    }

    public static BlockStatement block(Function<BlockStatement, Collection<Statement>> body) {
        var statement = new BlockStatement();
        statement.getBody().addAll(body.apply(statement));
        return statement;
    }

    public static BreakStatement exitBlock(IdentifiedStatement identifiedStatement) {
        var statement = new BreakStatement();
        statement.setTarget(identifiedStatement);
        return statement;
    }

    public static WhileStatement loopWhile(Expr condition, Function<WhileStatement, Collection<Statement>> body) {
        var statement = new WhileStatement();
        statement.setCondition(condition);
        statement.getBody().addAll(body.apply(statement));
        return statement;
    }

    public static ThrowStatement raiseException(Expr exception) {
        var stmt = new ThrowStatement();
        stmt.setException(exception);
        return stmt;
    }

    public static Statement cond(Expr predicate, List<Statement> consequent, List<Statement> alternative) {
        var statement = new ConditionalStatement();
        statement.setCondition(predicate);
        statement.getConsequent().addAll(consequent);
        statement.getAlternative().addAll(alternative);
        return statement;
    }

    public static Statement cond(Expr predicate, List<Statement> consequent) {
        return cond(predicate, consequent, Collections.emptyList());
    }

    public static InitClassStatement initClass(String className) {
        var stmt = new InitClassStatement();
        stmt.setClassName(className);
        return stmt;
    }

    public static SwitchStatement switchStatement(Expr condition, List<Statement> defaultClause,
            SwitchClause... clauses) {
        var statement = new SwitchStatement();
        statement.setValue(condition);
        statement.getDefaultClause().addAll(defaultClause);
        statement.getClauses().addAll(List.of(clauses));
        return statement;
    }

    public static SwitchClause switchClause(int[] conditions, Statement... statements) {
        var clause = new SwitchClause();
        clause.setConditions(conditions);
        clause.getBody().addAll(List.of(statements));
        return clause;
    }

    public static SwitchClause switchClause(int condition, Statement... statements) {
        return switchClause(new int[] { condition }, statements);
    }

    public static TryCatchStatementBuilder doTry(Statement... statements) {
        return new TryCatchStatementBuilder(statements);
    }

    public static class TryCatchStatementBuilder {
        Statement[] protectedBody;

        TryCatchStatementBuilder(Statement[] protectedBody) {
            this.protectedBody = protectedBody;
        }

        public TryCatchStatementBuilder2 doCatch(String type, Integer variable) {
            var result = new TryCatchStatement();
            result.getProtectedBody().addAll(Arrays.asList(protectedBody));
            result.setExceptionType(type);
            result.setExceptionVariable(variable);
            return new TryCatchStatementBuilder2(result);
        }
    }

    public static class TryCatchStatementBuilder2 {
        TryCatchStatement statement;

        TryCatchStatementBuilder2(TryCatchStatement statement) {
            this.statement = statement;
        }

        public TryCatchStatement with(Statement... statements) {
            statement.getHandler().addAll(Arrays.asList(statements));
            return statement;
        }
    }
}
