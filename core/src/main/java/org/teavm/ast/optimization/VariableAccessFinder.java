/*
 *  Copyright 2020 Alexey Andreev.
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

import java.util.function.IntPredicate;
import org.teavm.ast.RecursiveVisitor;
import org.teavm.ast.VariableExpr;

public class VariableAccessFinder extends RecursiveVisitor {
    private IntPredicate predicate;
    private boolean found;

    public VariableAccessFinder(IntPredicate predicate) {
        this.predicate = predicate;
    }

    public boolean isFound() {
        return found;
    }

    public void reset() {
        found = false;
    }

    @Override
    public void visit(VariableExpr expr) {
        if (predicate.test(expr.getIndex())) {
            found = true;
            cancel();
        }
    }
}
