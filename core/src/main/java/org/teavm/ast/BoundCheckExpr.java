/*
 *  Copyright 2019 konsoletyper.
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
package org.teavm.ast;

import java.util.Map;

public class BoundCheckExpr extends Expr {
    private Expr index;
    private Expr array;
    private boolean lower;

    public Expr getIndex() {
        return index;
    }

    public void setIndex(Expr index) {
        this.index = index;
    }

    public Expr getArray() {
        return array;
    }

    public void setArray(Expr array) {
        this.array = array;
    }

    public boolean isLower() {
        return lower;
    }

    public void setLower(boolean lower) {
        this.lower = lower;
    }

    @Override
    public void acceptVisitor(ExprVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Expr clone(Map<Expr, Expr> cache) {
        Expr known = cache.get(this);
        if (known != null) {
            return known;
        }
        BoundCheckExpr copy = new BoundCheckExpr();
        cache.put(this, copy);
        copy.setIndex(index.clone(cache));
        copy.setArray(array != null ? array.clone(cache) : null);
        copy.setLower(lower);
        return copy;
    }
}
