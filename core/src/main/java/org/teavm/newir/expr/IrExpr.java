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

public abstract class IrExpr {
    public static IrOperationExpr VOID = new IrOperationExpr.Impl0(IrOperation.VOID);
    public static IrOperationExpr START = new IrOperationExpr.Impl0(IrOperation.START);
    public static IrOperationExpr NULL = new IrOperationExpr.Impl0(IrOperation.NULL);

    IrExprTags.Tag tag;
    private IrExpr previous;

    public int getInputCount() {
        return 0;
    }

    public IrExpr getInput(int index) {
        throw new IndexOutOfBoundsException();
    }

    public void setInput(int index, IrExpr value) {
        throw new IndexOutOfBoundsException();
    }

    public IrType getInputType(int index) {
        throw new IndexOutOfBoundsException();
    }

    public abstract IrType getType();

    public abstract void acceptVisitor(IrExprVisitor visitor);

    public final int getDependencyCount() {
        return getInputCount() + (needsOrdering() ? 1 : 0);
    }

    public final IrExpr getDependency(int index) {
        if (needsOrdering()) {
            return index == 0 ? getPrevious() : getInput(index - 1);
        } else {
            return getInput(index);
        }
    }

    public final IrType getDependencyType(int index) {
        if (needsOrdering()) {
            return index == 0 ? IrType.ANY : getInputType(index - 1);
        } else {
            return getInputType(index);
        }
    }

    public boolean needsOrdering() {
        return false;
    }

    public final IrExpr getPrevious() {
        return previous != null ? previous : IrExpr.VOID;
    }

    public final void setPrevious(IrExpr expr) {
        if (!needsOrdering()) {
            throw new UnsupportedOperationException();
        }
        previous = expr;
    }
}
