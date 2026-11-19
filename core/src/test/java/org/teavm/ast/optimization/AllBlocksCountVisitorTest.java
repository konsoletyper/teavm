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
package org.teavm.ast.optimization;

import static org.junit.Assert.assertEquals;
import static org.teavm.ast.Statement.block;
import static org.teavm.ast.Statement.cond;
import static org.teavm.ast.Statement.exitBlock;
import static org.teavm.ast.Statement.exitFunction;
import static org.teavm.ast.Statement.loopWhile;
import static org.teavm.ast.Statement.statementExpr;
import static org.teavm.ast.Statement.switchClause;
import static org.teavm.ast.Statement.switchStatement;
import java.util.List;
import org.junit.Test;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.Expr;
import org.teavm.ast.Statement;

public class AllBlocksCountVisitorTest {
    @Test
    public void switchClauseEndingWithBreakDoesNotFallThrough() {
        var blocks = new BlockStatement[2];
        var root = block(a -> {
            blocks[0] = a;
            return List.of(
                    block(b -> {
                        blocks[1] = b;
                        return List.of(switchStatement(Expr.var(0), List.of(exitFunction()),
                                switchClause(0, statementExpr(Expr.var(1)), exitBlock(b)),
                                switchClause(1, statementExpr(Expr.var(2)), exitBlock(a))));
                    }),
                    statementExpr(Expr.var(3))
            );
        });

        var visitor = count(root);
        assertEquals(1, visitor.getCount(blocks[1]));
        assertEquals(2, visitor.getCount(blocks[0]));
    }

    @Test
    public void emptyBranchFallsThrough() {
        var blocks = new BlockStatement[1];
        var root = block(a -> {
            blocks[0] = a;
            return List.of(cond(Expr.var(0), List.of(exitFunction()), List.of(
                    cond(Expr.var(1), List.of(), List.of(exitFunction()))
            )));
        });

        assertEquals(1, count(root).getCount(blocks[0]));
    }

    @Test
    public void loopWithConditionFallsThrough() {
        var blocks = new BlockStatement[1];
        var root = block(a -> {
            blocks[0] = a;
            return List.of(loopWhile(Expr.var(0), w -> List.of(statementExpr(Expr.var(1)))));
        });

        assertEquals(1, count(root).getCount(blocks[0]));
    }

    private static AllBlocksCountVisitor count(Statement statement) {
        var visitor = new AllBlocksCountVisitor();
        statement.acceptVisitor(visitor);
        return visitor;
    }
}
