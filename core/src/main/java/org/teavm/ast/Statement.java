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

import java.util.Collections;
import java.util.List;

public abstract class Statement {
    public abstract void acceptVisitor(StatementVisitor visitor);

    public static Statement empty() {
        return new SequentialStatement();
    }

    public static AssignmentStatement assign(Expr left, Expr right) {
        AssignmentStatement stmt = new AssignmentStatement();
        stmt.setLeftValue(left);
        stmt.setRightValue(right);
        return stmt;
    }

    public static ReturnStatement exitFunction(Expr result) {
        ReturnStatement stmt = new ReturnStatement();
        stmt.setResult(result);
        return stmt;
    }

    public static ThrowStatement raiseException(Expr exception) {
        ThrowStatement stmt = new ThrowStatement();
        stmt.setException(exception);
        return stmt;
    }

    public static Statement cond(Expr predicate, List<Statement> consequent, List<Statement> alternative) {
        ConditionalStatement statement = new ConditionalStatement();
        statement.setCondition(predicate);
        statement.getConsequent().addAll(consequent);
        statement.getAlternative().addAll(alternative);
        return statement;
    }

    public static Statement cond(Expr predicate, List<Statement> consequent) {
        return cond(predicate, consequent, Collections.emptyList());
    }

    public static InitClassStatement initClass(String className) {
        InitClassStatement stmt = new InitClassStatement();
        stmt.setClassName(className);
        return stmt;
    }
}
