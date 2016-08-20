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

import java.util.HashSet;
import java.util.Set;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.BreakStatement;
import org.teavm.ast.ContinueStatement;
import org.teavm.ast.IdentifiedStatement;
import org.teavm.ast.RecursiveVisitor;
import org.teavm.ast.SwitchClause;
import org.teavm.ast.SwitchStatement;
import org.teavm.ast.WhileStatement;

class RedundantLabelEliminator extends RecursiveVisitor {
    private IdentifiedStatement currentBlock;
    private Set<IdentifiedStatement> hasRefs = new HashSet<>();

    @Override
    public void visit(SwitchStatement statement) {
        IdentifiedStatement currentBlockBackup = currentBlock;
        currentBlock = statement;
        for (SwitchClause clause : statement.getClauses()) {
            visit(clause.getBody());
        }
        visit(statement.getDefaultClause());
        if (!hasRefs.contains(currentBlock)) {
            currentBlock.setId(null);
        }
        currentBlock = currentBlockBackup;
    }

    @Override
    public void visit(WhileStatement statement) {
        IdentifiedStatement currentBlockBackup = currentBlock;
        currentBlock = statement;
        visit(statement.getBody());
        if (!hasRefs.contains(currentBlock)) {
            currentBlock.setId(null);
        }
        currentBlock = currentBlockBackup;
    }

    @Override
    public void visit(BlockStatement statement) {
        IdentifiedStatement currentBlockBackup = currentBlock;
        currentBlock = null;
        visit(statement.getBody());
        currentBlock = currentBlockBackup;
    }

    @Override
    public void visit(BreakStatement statement) {
        if (statement.getTarget() == currentBlock) {
            statement.setTarget(null);
        } else {
            hasRefs.add(statement.getTarget());
        }
    }

    @Override
    public void visit(ContinueStatement statement) {
        if (statement.getTarget() == currentBlock) {
            statement.setTarget(null);
        } else {
            hasRefs.add(statement.getTarget());
        }
    }
}
