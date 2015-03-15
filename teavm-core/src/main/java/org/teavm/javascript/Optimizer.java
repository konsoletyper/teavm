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

import org.teavm.javascript.ast.AsyncMethodNode;
import org.teavm.javascript.ast.AsyncMethodPart;
import org.teavm.javascript.ast.RegularMethodNode;
import org.teavm.model.Program;
import org.teavm.model.util.AsyncProgramSplitter;

/**
 *
 * @author Alexey Andreev
 */
public class Optimizer {
    public void optimize(RegularMethodNode method, Program program) {
        ReadWriteStatsBuilder stats = new ReadWriteStatsBuilder(method.getVariables().size());
        stats.analyze(program);
        boolean[] preservedVars = new boolean[stats.writes.length];
        for (int i = 0; i < preservedVars.length; ++i) {
            if (stats.writes[i] != 1) {
                preservedVars[i] = true;
            }
        }
        BreakEliminator breakEliminator = new BreakEliminator();
        breakEliminator.eliminate(method.getBody());
        OptimizingVisitor optimizer = new OptimizingVisitor(preservedVars, stats.reads);
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

    public void optimize(AsyncMethodNode method, AsyncProgramSplitter splitter) {
        boolean[] preservedVars = new boolean[method.getVariables().size()];
        int[][] readFrequencies = new int[splitter.size()][];
        for (int i = 0; i < splitter.size(); ++i) {
            ReadWriteStatsBuilder stats = new ReadWriteStatsBuilder(method.getVariables().size());
            stats.analyze(splitter.getProgram(i));
            readFrequencies[i] = stats.reads;
            for (int j = 0; j < stats.writes.length; ++j) {
                if (stats.readUninitialized[j] || stats.writes[j] != 1 && stats.reads[j] > 0) {
                    preservedVars[j] = true;
                }
            }
        }
        for (int i = 0; i < splitter.size(); ++i) {
            AsyncMethodPart part = method.getBody().get(i);
            BreakEliminator breakEliminator = new BreakEliminator();
            breakEliminator.eliminate(part.getStatement());
            OptimizingVisitor optimizer = new OptimizingVisitor(preservedVars, readFrequencies[i]);
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
