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

class TLinkedHashMapEntrySet<K, V> extends TAbstractSet<TMap.Entry<K, V>>
        implements TSequencedSet<TMap.Entry<K, V>> {
    private final TLinkedHashMap<K, V> base;
    private final boolean reversed;

    TLinkedHashMapEntrySet(TLinkedHashMap<K, V> base, boolean reversed) {
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
    public final TIterator<TMap.Entry<K, V>> iterator() {
        return new TLinkedHashMapIterator.EntryIterator<>(base, reversed);
    }

    @Override
    public final boolean contains(Object o) {
        if (o instanceof TMap.Entry) {
            TMap.Entry<?, ?> oEntry = (TMap.Entry<?, ?>) o;
            TMap.Entry<K, V> entry = base.entryByKey(oEntry.getKey());
            return entry != null && TObjects.equals(entry.getValue(), oEntry.getValue());
        }
        return false;
    }

    @Override
    public boolean remove(Object object) {
        if (object instanceof TMap.Entry) {
            TMap.Entry<?, ?> oEntry = (TMap.Entry<?, ?>) object;
            TMap.Entry<K, V> entry = base.entryByKey(oEntry.getKey());
            if (entry != null && TObjects.equals(entry.getValue(), oEntry.getValue())) {
                base.remove(entry.getKey());
                return true;
            }
        }
        return false;
    }

    @Override
    public final void forEach(Consumer<? super TMap.Entry<K, V>> action) {
        if (base.elementCount > 0) {
            int prevModCount = base.modCount;
            TLinkedHashMap.LinkedHashMapEntry<K, V> entry = reversed ? base.tail : base.head;
            do {
                action.accept(entry);
                entry = reversed ? entry.chainBackward : entry.chainForward;
                if (base.modCount != prevModCount) {
                    throw new TConcurrentModificationException();
                }
            } while (entry != null);
        }
    }

    @Override
    public final void addFirst(TMap.Entry<K, V> e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void addLast(TMap.Entry<K, V> e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final TMap.Entry<K, V> getFirst() {
        return TLinkedHashMap.checkNotNull(reversed ? base.tail : base.head);
    }

    @Override
    public final TMap.Entry<K, V> getLast() {
        return TLinkedHashMap.checkNotNull(reversed ? base.head : base.tail);
    }

    @Override
    public final TMap.Entry<K, V> removeFirst() {
        var e = TLinkedHashMap.checkNotNull(reversed ? base.tail : base.head);
        base.remove(e.key);
        return e;
    }

    @Override
    public final TMap.Entry<K, V> removeLast() {
        var e = TLinkedHashMap.checkNotNull(reversed ? base.head : base.tail);
        base.remove(e.key);
        return e;
    }

    @Override
    public TSequencedSet<TMap.Entry<K, V>> reversed() {
        return new TLinkedHashMapEntrySet<>(base, !reversed);
    }
}
