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

class TLinkedHashMapKeySet<K> extends TAbstractSet<K> implements TSequencedSet<K> {
    private final TLinkedHashMap<K, ?> base;
    private final boolean reversed;

    TLinkedHashMapKeySet(TLinkedHashMap<K, ?> base, boolean reversed) {
        this.base = base;
        this.reversed = reversed;
    }

    @Override
    public final int size() {
        return base.elementCount;
    }

    @Override
    public final void clear() {
        base.clear();
    }

    @Override
    public final TIterator<K> iterator() {
        return new TLinkedHashMapIterator.KeyIterator<>(base, reversed);
    }

    @Override
    public final boolean contains(Object o) {
        return base.containsKey(o);
    }

    @Override
    public final boolean remove(Object key) {
        int befCount = base.elementCount;
        base.remove(key);
        return base.elementCount != befCount;
    }

    @Override
    public final void forEach(Consumer<? super K> action) {
        if (base.elementCount > 0) {
            int prevModCount = base.modCount;
            TLinkedHashMap.LinkedHashMapEntry<K, ?> entry = reversed ? base.tail : base.head;
            do {
                action.accept(entry.key);
                entry = reversed ? entry.chainBackward : entry.chainForward;
                if (base.modCount != prevModCount) {
                    throw new TConcurrentModificationException();
                }
            } while (entry != null);
        }
    }

    @Override
    public final K getFirst() {
        return TLinkedHashMap.checkNotNull(reversed ? base.tail : base.head).key;
    }

    @Override
    public final K getLast() {
        return TLinkedHashMap.checkNotNull(reversed ? base.head : base.tail).key;
    }

    @Override
    public final K removeFirst() {
        var e = TLinkedHashMap.checkNotNull(reversed ? base.tail : base.head);
        base.remove(e.key);
        return e.key;
    }

    @Override
    public final K removeLast() {
        var e = TLinkedHashMap.checkNotNull(reversed ? base.head : base.tail);
        base.remove(e.key);
        return e.key;
    }

    @Override
    public TSequencedSet<K> reversed() {
        if (reversed) {
            return base.sequencedKeySet();
        } else {
            return new TLinkedHashMapKeySet<>(base, true);
        }
    }
}
