/*
 *  Copyright 2026 Alexey Andreev.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.teavm.ast.Expr.constant;
import static org.teavm.ast.Expr.var;
import static org.teavm.ast.Statement.assign;
import static org.teavm.ast.Statement.block;
import static org.teavm.ast.Statement.doTry;
import static org.teavm.ast.Statement.exitFunction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.Test;
import org.teavm.ast.Expr;
import org.teavm.ast.Statement;

public class AstDefinitionUsageAnalysisTest {
    private Map<Statement, String> statementsLabels = new HashMap<>();
    private Map<Expr, String> expressionsLabels = new HashMap<>();

    @Test
    public void definitionsInTryBlock() {
        var body = block(label -> List.of(
            labeled("mainDef", assign(var(1), constant(0))),
            doTry(
                    labeled("def1", assign(var(1), constant(1))),
                    labeled("def2", assign(var(1), constant(2)))
            ).doCatch("java.lang.Throwable", 2).with(
                    exitFunction(labeled("use1", var(1)))
            ),
            exitFunction(labeled("use2", var(1)))
        ));
        var defuse = new AstDefinitionUsageAnalysis();
        defuse.analyze(body);

        var definitions = defuse.getDefinitions().stream()
                .filter(def -> def.getVariableIndex() == 1)
                .collect(Collectors.toList());
        assertEquals(3, definitions.size());

        for (var defName : List.of("def2", "def1", "mainDef")) {
            var definition = definitions.stream()
                    .filter(def -> defName.equals(statementsLabels.get(def.getStatement())))
                    .findFirst()
                    .orElse(null);
            assertNotNull("Definition not found: " + defName, definition);
            assertEquals("Wrong number of usages in definition " + defName, 2, definition.getUsages().size());
            var usages = definition.getUsages().stream()
                    .map(use -> expressionsLabels.get(use.getExpr()))
                    .filter(Objects::nonNull)
                    .sorted()
                    .collect(Collectors.toList());
            assertEquals("Unexpected usages of definition " + defName, List.of("use1", "use2"), usages);

            for (var usage : definition.getUsages()) {
                assertEquals("Unexpected live in catches in usage", 1, usage.getLiveInCatches().size());
            }
        }
    }

    private <T extends Statement> T labeled(String label, T statement) {
        statementsLabels.put(statement, label);
        return statement;
    }

    private <T extends Expr> T labeled(String label, T statement) {
        expressionsLabels.put(statement, label);
        return statement;
    }
}
