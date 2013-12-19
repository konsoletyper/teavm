/*
 *  Copyright 2011 Alexey Andreev.
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
package org.teavm.javascript.ast;

/**
 *
 * @author Alexey Andreev
 */
public abstract class Statement {
    public abstract void acceptVisitor(StatementVisitor visitor);

    public static Statement empty() {
        return new SequentialStatement();
    }

    public static Statement assign(Expr left, Expr right) {
        AssignmentStatement stmt = new AssignmentStatement();
        stmt.setLeftValue(left);
        stmt.setRightValue(right);
        return stmt;
    }

    public static Statement exitFunction(Expr result) {
        ReturnStatement stmt = new ReturnStatement();
        stmt.setResult(result);
        return stmt;
    }

    public static Statement raiseException(Expr exception) {
        ThrowStatement stmt = new ThrowStatement();
        stmt.setException(exception);
        return stmt;
    }

    public static Statement increment(int var, int amount) {
        IncrementStatement stmt = new IncrementStatement();
        stmt.setVar(var);
        stmt.setAmount(amount);
        return stmt;
    }

    public static Statement cond(Expr predicate, Statement consequent, Statement alternative) {
        ConditionalStatement statement = new ConditionalStatement();
        statement.setCondition(predicate);
        statement.setConsequent(consequent);
        statement.setAlternative(alternative);
        return statement;
    }

    public static Statement cond(Expr predicate, Statement consequent) {
        return cond(predicate, consequent, null);
    }

    public static Statement initClass(String className) {
        InitClassStatement stmt = new InitClassStatement();
        stmt.setClassName(className);
        return stmt;
    }
}
