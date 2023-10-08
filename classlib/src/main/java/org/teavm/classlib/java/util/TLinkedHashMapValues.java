/*
 *  Copyright 2023 ihromant.
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
package org.teavm.classlib.java.util;

import java.util.function.Consumer;

class TLinkedHashMapValues<V> extends TAbstractCollection<V> implements TSequencedCollection<V> {
    private final TLinkedHashMap<?, V> base;
    private final boolean reversed;

    TLinkedHashMapValues(TLinkedHashMap<?, V> base, boolean reversed) {
        this.base = base;
        this.reversed = reversed;
    }

    @Override
    public final int size() {
        return base.size();
    }

    @Override
    public final void clear() {
        base.clear();
    }

    @Override
    public final TIterator<V> iterator() {
        return new TLinkedHashMapIterator.ValueIterator<>(base, reversed);
    }

    @Override
    public final boolean contains(Object o) {
        return base.containsValue(o);
    }

    @Override
    public final void forEach(Consumer<? super V> action) {
        if (base.elementCount > 0) {
            int prevModCount = base.modCount;
            TLinkedHashMap.LinkedHashMapEntry<?, V> entry = reversed ? base.tail : base.head;
            do {
                action.accept(entry.value);
                entry = reversed ? entry.chainBackward : entry.chainForward;
                if (base.modCount != prevModCount) {
                    throw new TConcurrentModificationException();
                }
            } while (entry != null);
        }
    }

    @Override
    public final V getFirst() {
        return TLinkedHashMap.checkNotNull(reversed ? base.tail : base.head).value;
    }

    @Override
    public final V getLast() {
        return TLinkedHashMap.checkNotNull(reversed ? base.head : base.tail).value;
    }

    @Override
    public final V removeFirst() {
        THashMap.HashEntry<?, V> e = TLinkedHashMap.checkNotNull(reversed ? base.tail : base.head);
        return base.remove(e.key);
    }

    @Override
    public final V removeLast() {
        THashMap.HashEntry<?, V> e = TLinkedHashMap.checkNotNull(reversed ? base.head : base.tail);
        return base.remove(e.key);
    }

    @Override
    public TSequencedCollection<V> reversed() {
        if (reversed) {
            return base.sequencedValues();
        } else {
            return new TLinkedHashMapValues<>(base, true);
        }
    }
}
