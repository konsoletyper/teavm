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
package org.teavm.newir.expr;

import java.util.ArrayList;
import java.util.List;

public final class IrTryCatchExpr extends IrExpr {
    private IrExpr body = IrExpr.VOID;
    private IrExpr handler = IrExpr.VOID;
    private String[] exceptionTypes;
    private List<IrCaughtValueExpr> caughtValues = new ArrayList<>();

    public IrTryCatchExpr(String... exceptionTypes) {
        this.exceptionTypes = exceptionTypes.clone();
    }

    public String[] getExceptionTypes() {
        return exceptionTypes.clone();
    }

    public IrExpr getBody() {
        return body;
    }

    public void setBody(IrExpr body) {
        this.body = body;
    }

    public IrExpr getHandler() {
        return handler;
    }

    public void setHandler(IrExpr handler) {
        this.handler = handler;
    }

    public Iterable<IrCaughtValueExpr> getCaughtValues() {
        return caughtValues;
    }

    public IrCaughtValueExpr addCaughtValue(IrExpr[] inputs) {
        IrCaughtValueExpr caughtValue = new IrCaughtValueExpr(this, caughtValues.size(), inputs);
        caughtValues.add(caughtValue);
        return caughtValue;
    }

    public void removeCaughtValue(IrCaughtValueExpr caughtValue) {
        if (caughtValue.tryCatchExpr != this) {
            throw new IllegalArgumentException("Caught value does not belong this try/catch block");
        }
        caughtValues.remove(caughtValue.index);
        for (int i = caughtValue.index; i < caughtValues.size(); ++i) {
            caughtValues.get(i).index = i;
        }
        caughtValue.index = -1;
        caughtValue.tryCatchExpr = null;
    }

    @Override
    public IrType getType() {
        return body.getType();
    }

    @Override
    public void acceptVisitor(IrExprVisitor visitor) {
        visitor.visit(this);
    }
}
