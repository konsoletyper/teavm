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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.teavm.ast.RecursiveVisitor;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.VariableExpr;
import org.teavm.ast.VariableNode;

class UnusedVariableEliminator extends RecursiveVisitor {
    private final VariableNode[] variableNodes;
    private final int[] variables;
    private final int[] indexes;
    private List<VariableNode> reorderedVariables = new ArrayList<>();
    int lastIndex;

    UnusedVariableEliminator(int parameterCount, List<VariableNode> variables) {
        variableNodes = new VariableNode[variables.size()];
        this.variables = new int[variables.size()];
        int variableCount = 0;
        for (int i = 0; i < variables.size(); ++i) {
            variableNodes[i] = variables.get(i);
            int var = variables.get(i).getIndex();
            this.variables[i] = var;
            variableCount = Math.max(variableCount, var + 1);
        }
        indexes = new int[variableCount];
        Arrays.fill(indexes, -1);
        parameterCount = Math.min(parameterCount, indexes.length - 1);
        for (int i = 0; i <= parameterCount; ++i) {
            indexes[i] = lastIndex++;
            reorderedVariables.add(variableNodes[i]);
        }
    }

    public List<VariableNode> getReorderedVariables() {
        return reorderedVariables;
    }

    private int renumber(int var) {
        int index = indexes[variables[var]];
        if (index == -1) {
            index = lastIndex++;
            indexes[variables[var]] = index;
            reorderedVariables.add(variableNodes[var]);
        }
        VariableNode node = variableNodes[var];
        if (node.getName() != null) {
            reorderedVariables.get(index).setName(node.getName());
        }
        return index;
    }

    @Override
    public void visit(VariableExpr expr) {
        expr.setIndex(renumber(expr.getIndex()));
    }

    @Override
    public void visit(TryCatchStatement statement) {
        super.visit(statement);
        if (statement.getExceptionVariable() != null) {
            if (variables[statement.getExceptionVariable()] < 0) {
                statement.setExceptionVariable(null);
            } else {
                statement.setExceptionVariable(renumber(statement.getExceptionVariable()));
            }
        }
    }
}
