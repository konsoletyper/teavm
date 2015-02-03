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

import java.util.List;
import org.teavm.javascript.ast.AsyncMethodNode;
import org.teavm.javascript.ast.AsyncMethodPart;
import org.teavm.javascript.ast.RegularMethodNode;
import org.teavm.model.Program;


/**
 *
 * @author Alexey Andreev
 */
public class Optimizer {
    public void optimize(RegularMethodNode method, Program program) {
        ReadWriteStatsBuilder stats = new ReadWriteStatsBuilder(method.getVariables().size());
        stats.analyze(program);
        OptimizingVisitor optimizer = new OptimizingVisitor(stats);
        method.getBody().acceptVisitor(optimizer);
        method.setBody(optimizer.resultStmt);
        int paramCount = method.getReference().parameterCount();
        UnusedVariableEliminator unusedEliminator = new UnusedVariableEliminator(paramCount, method.getVariables());
        method.getBody().acceptVisitor(unusedEliminator);
        method.getVariables().subList(unusedEliminator.lastIndex, method.getVariables().size()).clear();
        RedundantLabelEliminator labelEliminator = new RedundantLabelEliminator();
        method.getBody().acceptVisitor(labelEliminator);
        for (int i = 0; i < method.getVariables().size(); ++i) {
            method.getVariables().set(i, i);
        }
    }

    public void optimize(AsyncMethodNode method, List<Program> programs) {
        ReadWriteStatsBuilder stats = new ReadWriteStatsBuilder(method.getVariables().size());
        for (Program program : programs) {
            stats.analyze(program);
        }
        OptimizingVisitor optimizer = new OptimizingVisitor(stats);
        for (AsyncMethodPart part : method.getBody()) {
            part.getStatement().acceptVisitor(optimizer);
            part.setStatement(optimizer.resultStmt);
        }
        int paramCount = method.getReference().parameterCount();
        UnusedVariableEliminator unusedEliminator = new UnusedVariableEliminator(paramCount, method.getVariables());
        for (AsyncMethodPart part : method.getBody()) {
            part.getStatement().acceptVisitor(unusedEliminator);
        }
        method.getVariables().subList(unusedEliminator.lastIndex, method.getVariables().size()).clear();
        RedundantLabelEliminator labelEliminator = new RedundantLabelEliminator();
        for (AsyncMethodPart part : method.getBody()) {
            part.getStatement().acceptVisitor(labelEliminator);
        }
        for (int i = 0; i < method.getVariables().size(); ++i) {
            method.getVariables().set(i, i);
        }
    }
}
