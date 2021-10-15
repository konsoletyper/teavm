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
package org.teavm.newir.util;

import java.util.function.Function;
import org.teavm.newir.expr.IrExpr;
import org.teavm.newir.expr.IrExprTags;

public class ScheduledIrExprTags<T> {
    private IrExprTags<Tag<T>> tags;

    public ScheduledIrExprTags() {
        this(expr -> null);
    }

    public ScheduledIrExprTags(Function<IrExpr, T> defaultSupplier) {
        tags = new IrExprTags<>(expr -> new Tag<>(defaultSupplier.apply(expr)));
    }

    public void fill(IrExpr expr) {
        Tag<T> originTag = tags.get(expr);
        if (originTag.visited()) {
            return;
        }
        IrExpr origin = expr;

        while (expr.getDependencyCount() > 0) {
            IrExpr prev = expr.getDependency(0);
            Tag<T> prevTag = tags.get(prev);
            if (prevTag.visited()) {
                break;
            }
            prevTag.next = expr;
            prevTag.last = origin;
            tags.get(expr).previous = prev;
            expr = prev;
        }
        originTag.first = expr;
        originTag.last = origin;

        while (expr != null) {
            for (int i = 1; i < expr.getDependencyCount(); ++i) {
                fill(expr.getDependency(i));
            }
            expr = tags.get(expr).next;
        }
    }

    public IrExpr next(IrExpr expr) {
        return tags.get(expr).next;
    }

    public IrExpr previous(IrExpr expr) {
        return tags.get(expr).previous;
    }

    public IrExpr first(IrExpr expr) {
        Tag<T> tag = tags.get(expr);
        IrExpr first = tag.first;
        if (first == null) {
            first = first(tag.last);
            tag.first = first;
        }
        return first;
    }

    public boolean isLast(IrExpr expr) {
        Tag<T> tag = tags.get(expr);
        return tag.last == expr;
    }

    public IrExpr last(IrExpr expr) {
        Tag<T> tag = tags.get(expr);
        return tag.last != null ? tag.last : expr;
    }

    public T get(IrExpr expr) {
        return tags.get(expr).data;
    }

    public void set(IrExpr expr, T value) {
        tags.get(expr).data = value;
    }

    public void cleanup() {
        tags.cleanup();
    }

    static class Tag<T> {
        T data;
        IrExpr previous;
        IrExpr next;
        IrExpr last;
        IrExpr first;

        Tag(T data) {
            this.data = data;
        }

        boolean visited() {
            return next != null || previous != null;
        }
    }
}
