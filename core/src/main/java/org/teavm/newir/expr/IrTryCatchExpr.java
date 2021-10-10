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
import org.teavm.newir.decl.IrClass;
import org.teavm.newir.type.IrType;

public final class IrTryCatchExpr extends IrExpr {
    private IrTryCatchStartExpr startExpr = new IrTryCatchStartExpr(this);
    private IrCaughtExceptionExpr caughtExceptionExpr = new IrCaughtExceptionExpr(this);
    private IrExpr body = IrExpr.VOID;
    private IrExpr handler = IrExpr.VOID;
    private IrClass[] exceptionTypes;
    private List<IrCaughtValueExpr> caughtValues = new ArrayList<>();

    public IrTryCatchExpr(IrClass... exceptionTypes) {
        this.exceptionTypes = exceptionTypes.clone();
    }

    public IrClass[] getExceptionTypes() {
        return exceptionTypes.clone();
    }

    public int getExceptionTypesCount() {
        return exceptionTypes.length;
    }

    public IrClass getExceptionType(int index) {
        return exceptionTypes[index];
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

    public IrTryCatchStartExpr getStartExpr() {
        return startExpr;
    }

    public IrCaughtExceptionExpr getCaughtExceptionExpr() {
        return caughtExceptionExpr;
    }

    public Iterable<IrCaughtValueExpr> getCaughtValues() {
        return caughtValues;
    }

    public int getCaughtValuesCount() {
        return caughtValues.size();
    }

    public IrCaughtValueExpr getCaughtValue(int index) {
        return caughtValues.get(index);
    }

    public IrCaughtValueExpr addCaughtValue(IrType type) {
        IrCaughtValueExpr caughtValue = new IrCaughtValueExpr(this, caughtValues.size(), type);
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
    public int getInputCount() {
        return 2;
    }

    @Override
    public IrExpr getInput(int index) {
        switch (index) {
            case 0:
                return body;
            case 1:
                return handler;
            default:
                return super.getInput(index);
        }
    }

    @Override
    public void setInput(int index, IrExpr value) {
        switch (index) {
            case 0:
                body = value;
                break;
            case 1:
                handler = value;
                break;
            default:
                super.setInput(index, value);
                break;
        }
    }

    @Override
    public IrType getInputType(int index) {
        switch (index) {
            case 0:
            case 1:
                return body.getType();
            default:
                return super.getInputType(index);
        }
    }

    @Override
    public boolean needsOrdering() {
        return true;
    }

    @Override
    public void acceptVisitor(IrExprVisitor visitor) {
        visitor.visit(this);
    }
}
