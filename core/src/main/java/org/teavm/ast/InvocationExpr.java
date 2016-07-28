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
import org.teavm.model.MethodReference;

public class InvocationExpr extends Expr {
    private MethodReference method;
    private InvocationType type;
    private List<Expr> arguments = new ArrayList<>();

    public MethodReference getMethod() {
        return method;
    }

    public void setMethod(MethodReference method) {
        this.method = method;
    }

    public InvocationType getType() {
        return type;
    }

    public void setType(InvocationType type) {
        this.type = type;
    }

    public List<Expr> getArguments() {
        return arguments;
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
        InvocationExpr copy = new InvocationExpr();
        cache.put(this, copy);
        copy.setMethod(method);
        for (Expr arg : arguments) {
            copy.getArguments().add(arg.clone(cache));
        }
        return copy;
    }
}
