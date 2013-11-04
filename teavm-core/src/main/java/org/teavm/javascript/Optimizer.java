/*
 *  Copyright 2012 Alexey Andreev.
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
package org.teavm.javascript;

import org.teavm.javascript.ast.RegularMethodNode;
import org.teavm.javascript.ast.Statement;
import org.teavm.model.MethodHolder;


/**
 *
 * @author Alexey Andreev
 */
public class Optimizer {
    public Statement optimize(MethodHolder method, Statement statement) {
        ReadWriteStatsBuilder stats = new ReadWriteStatsBuilder(method.getProgram().variableCount());
        statement.acceptVisitor(stats);
        OptimizingVisitor optimizer = new OptimizingVisitor(stats);
        statement.acceptVisitor(optimizer);
        return optimizer.resultStmt;
    }

    public void optimize(RegularMethodNode method) {
        ReadWriteStatsBuilder stats = new ReadWriteStatsBuilder(method.getVariableCount());
        method.getBody().acceptVisitor(stats);
        OptimizingVisitor optimizer = new OptimizingVisitor(stats);
        method.getBody().acceptVisitor(optimizer);
        method.setBody(optimizer.resultStmt);
        int paramCount = method.getReference().parameterCount();
        UnusedVariableEliminator unusedEliminator = new UnusedVariableEliminator(paramCount, method.getVariableCount());
        method.getBody().acceptVisitor(unusedEliminator);
        method.setVariableCount(unusedEliminator.lastIndex);
    }
}
