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

import java.util.List;
import org.teavm.ast.BreakStatement;
import org.teavm.ast.ContinueStatement;
import org.teavm.ast.IdentifiedStatement;
import org.teavm.ast.RecursiveVisitor;
import org.teavm.ast.Statement;

class BreakToContinueReplacer extends RecursiveVisitor {
    private IdentifiedStatement replacedBreak;
    private IdentifiedStatement replacement;
    private ContinueStatement replaceBy;

    public BreakToContinueReplacer(IdentifiedStatement replacedBreak, IdentifiedStatement replacement) {
        this.replacedBreak = replacedBreak;
        this.replacement = replacement;
    }

    @Override
    public void visit(List<Statement> statements) {
        if (statements == null) {
            return;
        }
        for (int i = 0; i < statements.size(); ++i) {
            Statement stmt = statements.get(i);
            stmt.acceptVisitor(this);
            if (replaceBy != null) {
                statements.set(i, replaceBy);
                replaceBy = null;
            }
        }
    }

    @Override
    public void visit(BreakStatement statement) {
        if (statement.getTarget() == replacedBreak) {
            replaceBy = new ContinueStatement();
            replaceBy.setTarget(replacement);
            replaceBy.setLocation(statement.getLocation());
        }
    }

    @Override
    public void visit(ContinueStatement statement) {
        if (statement.getTarget() == replacedBreak) {
            statement.setTarget(replacement);
        }
    }
}
