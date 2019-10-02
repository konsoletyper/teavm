/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.backend.c.analyze;

import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.RecursiveVisitor;
import org.teavm.ast.Statement;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.VariableExpr;

public class VolatileDefinitionFinder {
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private Map<AssignmentStatement, StackElement> defHandlers = new HashMap<>();
    private Set<AssignmentStatement> definitionsToBackup = new HashSet<>();
    private Map<TryCatchStatement, IntSet> usagesToRestoreByHandler = new HashMap<>();

    public void findVolatileDefinitions(Statement statement) {
        AstDefinitionUsageAnalysis defuse = new AstDefinitionUsageAnalysis();
        defuse.analyze(statement);
        statement.acceptVisitor(handlerAnalyzer);

        for (AstDefinitionUsageAnalysis.Definition definition : defuse.getDefinitions()) {
            StackElement stack = defHandlers.get(definition.getStatement());
            if (stack == null || definition.getExceptionHandlingUsages().isEmpty()) {
                continue;
            }

            while (stack != null) {
                if (definition.getExceptionHandlingUsages().get(stack.statement) != null) {
                    IntSet usagesToRestore = usagesToRestoreByHandler.get(stack.statement);
                    if (usagesToRestore == null) {
                        usagesToRestore = new IntHashSet();
                        usagesToRestoreByHandler.put(stack.statement, usagesToRestore);
                    }
                    usagesToRestore.add(definition.getVariableIndex());
                    definitionsToBackup.add(definition.getStatement());
                }
                stack = stack.next;
            }
        }

        defHandlers = null;
    }

    public boolean shouldBackup(AssignmentStatement statement) {
        return definitionsToBackup.contains(statement);
    }

    public int[] variablesToRestore(TryCatchStatement tryCatch) {
        IntSet result = usagesToRestoreByHandler.get(tryCatch);
        if (result == null) {
            return EMPTY_INT_ARRAY;
        }
        int[] array = result.toArray();
        Arrays.sort(array);
        return array;
    }

    static class StackElement {
        final TryCatchStatement statement;
        final StackElement next;

        StackElement(TryCatchStatement statement, StackElement next) {
            this.statement = statement;
            this.next = next;
        }
    }

    private RecursiveVisitor handlerAnalyzer = new RecursiveVisitor() {
        StackElement surroundingTryCatches;

        @Override
        public void visit(TryCatchStatement statement) {
            surroundingTryCatches = new StackElement(statement, surroundingTryCatches);
            visit(statement.getProtectedBody());
            surroundingTryCatches = surroundingTryCatches.next;

            visit(statement.getHandler());
        }

        @Override
        public void visit(AssignmentStatement statement) {
            super.visit(statement);
            if (statement.getLeftValue() instanceof VariableExpr && surroundingTryCatches != null) {
                defHandlers.put(statement, surroundingTryCatches);
            }
        }
    };
}
