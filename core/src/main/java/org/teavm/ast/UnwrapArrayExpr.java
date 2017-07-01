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
import org.teavm.model.instructions.ArrayElementType;

public class UnwrapArrayExpr extends Expr {
    private ArrayElementType elementType;
    private Expr array;

    public UnwrapArrayExpr(ArrayElementType elementType) {
        this.elementType = elementType;
    }

    public ArrayElementType getElementType() {
        return elementType;
    }

    public Expr getArray() {
        return array;
    }

    public void setArray(Expr array) {
        this.array = array;
    }

    @Override
    public void acceptVisitor(ExprVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    protected UnwrapArrayExpr clone(Map<Expr, Expr> cache) {
        UnwrapArrayExpr copy = (UnwrapArrayExpr) cache.get(this);
        if (copy == null) {
            copy = new UnwrapArrayExpr(elementType);
            copy.array = array != null ? array.clone(cache) : null;
            cache.put(copy, copy);
        }
        return copy;
    }
}
