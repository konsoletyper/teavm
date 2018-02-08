/*
 *  Copyright 2018 Alexey Andreev.
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

import org.teavm.ast.InvocationExpr;
import org.teavm.ast.InvocationType;
import org.teavm.ast.RecursiveVisitor;

public class TemporaryVariableEstimator extends RecursiveVisitor {
    private int currentReceiverIndex;
    private int maxReceiverIndex;

    public int getMaxReceiverIndex() {
        return maxReceiverIndex;
    }

    @Override
    public void visit(InvocationExpr expr) {
        if (expr.getType() == InvocationType.DYNAMIC || expr.getType() == InvocationType.CONSTRUCTOR) {
            currentReceiverIndex++;
            maxReceiverIndex = Math.max(maxReceiverIndex, currentReceiverIndex);
        }

        super.visit(expr);

        if (expr.getType() == InvocationType.DYNAMIC) {
            currentReceiverIndex--;
        }
    }
}
