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

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.List;
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
import org.teavm.ast.SwitchStatement;
import org.teavm.ast.ThrowStatement;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.WhileStatement;

class AllBlocksCountVisitor implements StatementVisitor {
    private ObjectIntMap<IdentifiedStatement> blocksCount = new ObjectIntHashMap<>();
    private boolean exits;
    private IdentifiedStatement currentBlock;

    public void visit(List<Statement> statements) {
        if (statements == null) {
            return;
        }
        for (var statement : statements) {
            statement.acceptVisitor(this);
        }
    }

    public int getCount(IdentifiedStatement statement) {
        return blocksCount.get(statement);
    }

    @Override
    public void visit(AssignmentStatement statement) {
        exits = true;
    }

    @Override
    public void visit(SequentialStatement statement) {
        visit(statement.getSequence());
    }

    @Override
    public void visit(ConditionalStatement statement) {
        visit(statement.getConsequent());
        var consequentExits = exits;
        visit(statement.getAlternative());
        exits |= consequentExits;
    }

    @Override
    public void visit(SwitchStatement statement) {
        var outerBlock = currentBlock;
        currentBlock = statement;
        var hasExitClause = false;
        for (var clause : statement.getClauses()) {
            visit(clause.getBody());
            hasExitClause |= exits;
        }
        visit(statement.getDefaultClause());
        exits |= hasExitClause;
        incrementCurrentBlockIfExits();
        currentBlock = outerBlock;
    }

    @Override
    public void visit(WhileStatement statement) {
        var outerBlock = currentBlock;
        currentBlock = statement;
        visit(statement.getBody());
        exits = getCount(statement) > 0;
        currentBlock = outerBlock;
    }

    @Override
    public void visit(BlockStatement statement) {
        var outerBlock = currentBlock;
        currentBlock = statement;
        visit(statement.getBody());
        incrementCurrentBlockIfExits();
        currentBlock = outerBlock;
    }

    @Override
    public void visit(BreakStatement statement) {
        var target = statement.getTarget();
        incrementBlock(target != null ? target : currentBlock);
    }

    @Override
    public void visit(ContinueStatement statement) {
        var target = statement.getTarget();
        incrementBlock(target != null ? target : currentBlock);
    }

    private void incrementBlock(IdentifiedStatement statement) {
        blocksCount.put(statement, getCount(statement) + 1);
    }

    private void incrementCurrentBlockIfExits() {
        if (exits) {
            incrementBlock(currentBlock);
        } else {
            exits |= getCount(currentBlock) > 0;
        }
    }

    @Override
    public void visit(ReturnStatement statement) {
        exits = false;
    }

    @Override
    public void visit(ThrowStatement statement) {
        exits = false;
    }

    @Override
    public void visit(InitClassStatement statement) {
        exits = true;
    }

    @Override
    public void visit(TryCatchStatement statement) {
        visit(statement.getProtectedBody());
        var tryExits = exits;
        visit(statement.getHandler());
        exits |= tryExits;
    }

    @Override
    public void visit(GotoPartStatement statement) {
        exits = false;
    }

    @Override
    public void visit(MonitorEnterStatement statement) {
        exits = true;
    }

    @Override
    public void visit(MonitorExitStatement statement) {
        exits = true;
    }
}
