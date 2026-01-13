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

public class VolatileDefinitionFinder {
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private Set<TryCatchStatement> outerTryCatches = new HashSet<>();
    private Set<AssignmentStatement> definitionsToBackup = new HashSet<>();
    private Map<TryCatchStatement, IntSet> usagesToRestoreByHandler = new HashMap<>();
    private Map<TryCatchStatement, IntSet> definitionsToBackupOnEntry = new HashMap<>();
    private Map<TryCatchStatement, IntHashSet> mutatedVars = new HashMap<>();
    private AstDefinitionUsageAnalysis defuse;

    public void findVolatileDefinitions(Statement statement) {
        defuse = new AstDefinitionUsageAnalysis();
        defuse.analyze(statement);
        statement.acceptVisitor(handlerAnalyzer);
        defuse = null;
        usagesToRestoreByHandler.keySet().retainAll(mutatedVars.keySet());
        definitionsToBackupOnEntry.keySet().retainAll(mutatedVars.keySet());
        for (var entry : mutatedVars.entrySet()) {
            var set = usagesToRestoreByHandler.get(entry.getKey());
            if (set != null) {
                set.retainAll(entry.getValue());
            }
            set = definitionsToBackupOnEntry.get(entry.getKey());
            if (set != null) {
                set.retainAll(entry.getValue());
            }
        }
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

    public int[] variablesToBackup(TryCatchStatement tryCatch) {
        var result = definitionsToBackupOnEntry.get(tryCatch);
        if (result == null) {
            return EMPTY_INT_ARRAY;
        }
        var array = result.toArray();
        Arrays.sort(array);
        return array;
    }

    private RecursiveVisitor handlerAnalyzer = new RecursiveVisitor() {
        @Override
        public void visit(TryCatchStatement statement) {
            outerTryCatches.add(statement);
            visit(statement.getProtectedBody());
            outerTryCatches.remove(statement);
            visit(statement.getHandler());
        }

        @Override
        public void visit(AssignmentStatement statement) {
            var definition = defuse.getDefinition(statement);
            if (definition != null) {
                for (var usage : definition.getUsages()) {
                    for (var tryCatch : usage.getLiveInCatches()) {
                        if (outerTryCatches.contains(tryCatch)) {
                            definitionsToBackup.add(definition.getStatement());
                            mutatedVars.computeIfAbsent(tryCatch, k -> new IntHashSet())
                                    .add(definition.getVariableIndex());
                        } else {
                            definitionsToBackupOnEntry.computeIfAbsent(tryCatch, k -> new IntHashSet())
                                    .add(definition.getVariableIndex());
                        }
                        usagesToRestoreByHandler.computeIfAbsent(tryCatch, k -> new IntHashSet())
                                .add(definition.getVariableIndex());
                    }
                }
            }
        }
    };
}
