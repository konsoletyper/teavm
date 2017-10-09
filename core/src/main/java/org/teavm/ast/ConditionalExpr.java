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
package org.teavm.ast;

import java.util.Map;

public class ConditionalExpr extends Expr {
    private Expr condition;
    private Expr consequent;
    private Expr alternative;

    public Expr getCondition() {
        return condition;
    }

    public void setCondition(Expr condition) {
        this.condition = condition;
    }

    public Expr getConsequent() {
        return consequent;
    }

    public void setConsequent(Expr consequent) {
        this.consequent = consequent;
    }

    public Expr getAlternative() {
        return alternative;
    }

    public void setAlternative(Expr alternative) {
        this.alternative = alternative;
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
        ConditionalExpr copy = new ConditionalExpr();
        cache.put(this, copy);
        copy.setCondition(condition != null ? condition.clone(cache) : null);
        copy.setConsequent(consequent != null ? consequent.clone(cache) : null);
        copy.setAlternative(alternative != null ? alternative.clone(cache) : null);
        return copy;
    }
}
