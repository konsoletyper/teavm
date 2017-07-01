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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.teavm.model.ValueType;

public class NewMultiArrayExpr extends Expr {
    private ValueType type;
    private List<Expr> dimensions = new ArrayList<>();

    public ValueType getType() {
        return type;
    }

    public void setType(ValueType type) {
        this.type = type;
    }

    public List<Expr> getDimensions() {
        return dimensions;
    }

    @Override
    public void acceptVisitor(ExprVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected Expr clone(Map<Expr, Expr> cache) {
        NewMultiArrayExpr copy = new NewMultiArrayExpr();
        cache.put(this, copy);
        for (Expr dimension : dimensions) {
            copy.dimensions.add(dimension.clone(cache));
        }
        copy.type = type;
        return copy;
    }
}
