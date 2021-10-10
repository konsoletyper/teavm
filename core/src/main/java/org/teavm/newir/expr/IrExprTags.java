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

import java.util.function.Function;

public class IrExprTags<T> {
    private Tag first;
    private Function<IrExpr, T> defaultSupplier;

    public IrExprTags() {
        this(e -> null);
    }

    public IrExprTags(Function<IrExpr, T> defaultSupplier) {
        this.defaultSupplier = defaultSupplier;
    }

    @SuppressWarnings("unchecked")
    public T get(IrExpr target) {
        Tag tag = target.tag;
        if (tag == null) {
            T value;
            if (defaultSupplier != null) {
                value = defaultSupplier.apply(target);
                tag = new Tag(this, target, first);
                target.tag = tag;
                tag.value = value;
                first = tag;
            } else {
                value = null;
            }
            return value;
        }
        checkOwner(tag);
        return (T) tag.value;
    }

    public void set(IrExpr target, T value) {
        Tag tag = target.tag;
        if (tag == null) {
            tag = new Tag(this, target, first);
            first = tag;
            target.tag = tag;
        } else {
            checkOwner(tag);
        }
        tag.value = value;
    }

    private void checkOwner(Tag tag) {
        if (tag.owner != this) {
            throw new IllegalStateException("Another tagger still used for this expr");
        }
    }

    public void cleanup() {
        Tag tag = first;
        while (tag != null) {
            tag.target.tag = null;
            tag = tag.next;
        }
        first = null;
    }

    static class Tag {
        IrExprTags<?> owner;
        IrExpr target;
        Tag next;
        Object value;

        Tag(IrExprTags<?> owner, IrExpr target, Tag next) {
            this.owner = owner;
            this.target = target;
            this.next = next;
        }
    }
}
