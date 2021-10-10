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

import org.teavm.newir.type.IrType;

public final class IrLoopExpr extends IrExpr {
    private final IrLoopHeaderExpr header = new IrLoopHeaderExpr(this);
    private IrExpr preheader = IrExpr.VOID;
    private IrExpr body = header;

    public IrLoopHeaderExpr getHeader() {
        return header;
    }

    public IrExpr getPreheader() {
        return preheader;
    }

    public void setPreheader(IrExpr preheader) {
        this.preheader = preheader;
    }

    public IrExpr getBody() {
        return body;
    }

    public void setBody(IrExpr body) {
        this.body = body;
    }

    @Override
    public int getInputCount() {
        return 2;
    }

    @Override
    public IrExpr getInput(int index) {
        switch (index) {
            case 0:
                return preheader;
            case 1:
                return body;
            default:
                return super.getInput(index);
        }
    }

    @Override
    public void setInput(int index, IrExpr value) {
        switch (index) {
            case 0:
                preheader = value;
                break;
            case 1:
                body = value;
                break;
            default:
                super.setInput(index, value);
                break;
        }
    }

    @Override
    public IrType getType() {
        return body.getType();
    }

    @Override
    public IrType getInputType(int index) {
        switch (index) {
            case 0:
                return preheader.getType();
            case 1:
                return body.getType();
            default:
                return preheader.getType();
        }
    }

    @Override
    public void acceptVisitor(IrExprVisitor visitor) {
        visitor.visit(this);
    }
}
