/*
 *  Copyright 2021 konsoletyper.
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
package org.teavm.newir.analysis;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import org.teavm.newir.expr.IrExpr;
import org.teavm.newir.expr.IrSequenceExpr;
import org.teavm.newir.expr.IrUnaryExpr;
import org.teavm.newir.expr.IrUnaryOperation;
import org.teavm.newir.expr.RecursiveIrExprVisitor;

public class ExprConsumerCount extends RecursiveIrExprVisitor {
    private ObjectIntMap<IrExpr> countMap = new ObjectIntHashMap<>();
    private ObjectIntMap<IrExpr> ignoreMap = new ObjectIntHashMap<>();

    public int get(IrExpr expr) {
        return countMap.get(expr);
    }

    public int getIgnored(IrExpr expr) {
        return ignoreMap.get(expr);
    }

    public int decAndGet(IrExpr expr) {
        int count = countMap.get(expr);
        if (count > 0) {
            countMap.put(expr, --count);
        } else {
            throw new IllegalStateException("Trying to decrement beyond zero");
        }
        return count;
    }

    @Override
    protected void visitDefault(IrExpr expr) {
        int count = countMap.get(expr);
        countMap.put(expr, count + 1);
        if (count == 0) {
            visitInputs(expr);
        }
    }

    @Override
    public void visit(IrSequenceExpr expr) {
        ignore(expr.getFirst());
        visitDefault(expr);
    }

    @Override
    public void visit(IrUnaryExpr expr) {
        if (expr.getOperation() == IrUnaryOperation.IGNORE) {
            ignore(expr.getArgument());
        }
        visitDefault(expr);
    }

    private void ignore(IrExpr expr) {
        ignoreMap.put(expr, ignoreMap.get(expr) + 1);
    }
}
