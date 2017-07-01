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
import org.teavm.model.FieldReference;

public class QualificationExpr extends Expr {
    private Expr qualified;
    private FieldReference field;

    public Expr getQualified() {
        return qualified;
    }

    public void setQualified(Expr qualified) {
        this.qualified = qualified;
    }

    public FieldReference getField() {
        return field;
    }

    public void setField(FieldReference field) {
        this.field = field;
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
        QualificationExpr copy = new QualificationExpr();
        cache.put(this, copy);
        copy.setField(field);
        copy.setQualified(qualified != null ? qualified.clone(cache) : null);
        return copy;
    }
}
