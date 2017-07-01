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
package org.teavm.ast.optimization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.BreakStatement;
import org.teavm.ast.ConditionalStatement;
import org.teavm.ast.ContinueStatement;
import org.teavm.ast.GotoPartStatement;
import org.teavm.ast.IdentifiedStatement;
import org.teavm.ast.InitClassStatement;
import org.teavm.ast.MonitorEnterStatement;
import org.teavm.ast.MonitorExitStatement;
import org.teavm.ast.ReturnStatement;
import org.teavm.ast.SequentialStatement;
import org.teavm.ast.Statement;
import org.teavm.ast.StatementVisitor;
import org.teavm.ast.SwitchClause;
import org.teavm.ast.SwitchStatement;
import org.teavm.ast.ThrowStatement;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.WhileStatement;

class AllBlocksCountVisitor implements StatementVisitor {
    private Map<IdentifiedStatement, Integer> blocksCount = new HashMap<>();
    private IdentifiedStatement currentBlock;
    private boolean last = true;

    public void visit(List<Statement> statements) {
        if (statements == null) {
            return;
        }
        if (statements.isEmpty()) {
            incrementCurrentBlock();
            return;
        }
        boolean oldLast = last;
        for (int i = 0; i < statements.size() - 1; ++i) {
            last = false;
            statements.get(i).acceptVisitor(this);
        }
        last = true;
        statements.get(statements.size() - 1).acceptVisitor(this);
        last = oldLast;
    }

    public int getCount(IdentifiedStatement statement) {
        Integer result = blocksCount.get(statement);
        return result != null ? result : 0;
    }

    @Override
    public void visit(AssignmentStatement statement) {
        if (last) {
            incrementCurrentBlock();
        }
    }

    @Override
    public void visit(SequentialStatement statement) {
        visit(statement.getSequence());
    }

    @Override
    public void visit(ConditionalStatement statement) {
        visit(statement.getConsequent());
        visit(statement.getAlternative());
    }

    @Override
    public void visit(SwitchStatement statement) {
        IdentifiedStatement oldCurrentBlock = currentBlock;
        currentBlock = statement;
        for (SwitchClause clause : statement.getClauses()) {
            visit(clause.getBody());
        }
        visit(statement.getDefaultClause());
        currentBlock = oldCurrentBlock;
        if (last && blocksCount.containsKey(statement)) {
            incrementCurrentBlock();
        }
    }

    @Override
    public void visit(WhileStatement statement) {
        IdentifiedStatement oldCurrentBlock = currentBlock;
        currentBlock = statement;
        visit(statement.getBody());
        currentBlock = oldCurrentBlock;
        if (last && (statement.getCondition() != null || blocksCount.containsKey(statement))) {
            incrementCurrentBlock();
        }
    }

    @Override
    public void visit(BlockStatement statement) {
        IdentifiedStatement oldCurrentBlock = currentBlock;
        currentBlock = statement;
        visit(statement.getBody());
        currentBlock = oldCurrentBlock;
        if (last && blocksCount.containsKey(statement)) {
            incrementCurrentBlock();
        }
    }

    @Override
    public void visit(BreakStatement statement) {
        IdentifiedStatement target = statement.getTarget();
        if (target == null) {
            target = currentBlock;
        }
        incrementBlock(target);
    }

    @Override
    public void visit(ContinueStatement statement) {
        IdentifiedStatement target = statement.getTarget();
        if (target == null) {
            target = currentBlock;
        }
        incrementBlock(target);
    }

    private void incrementBlock(IdentifiedStatement statement) {
        blocksCount.put(statement, getCount(statement) + 1);
    }

    private void incrementCurrentBlock() {
        if (currentBlock != null) {
            incrementBlock(currentBlock);
        }
    }

    @Override
    public void visit(ReturnStatement statement) {
    }

    @Override
    public void visit(ThrowStatement statement) {
    }

    @Override
    public void visit(InitClassStatement statement) {
    }

    @Override
    public void visit(TryCatchStatement statement) {
        visit(statement.getProtectedBody());
        visit(statement.getHandler());
    }

    @Override
    public void visit(GotoPartStatement statement) {
    }

    @Override
    public void visit(MonitorEnterStatement statement) {
        if (last) {
            incrementCurrentBlock();
        }
    }

    @Override
    public void visit(MonitorExitStatement statement) {
        if (last) {
            incrementCurrentBlock();
        }
    }
}
