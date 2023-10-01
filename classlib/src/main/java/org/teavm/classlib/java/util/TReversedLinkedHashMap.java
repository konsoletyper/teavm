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

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

class TReversedLinkedHashMap<K, V> extends TAbstractMap<K, V> implements TSequencedMap<K, V> {
    private final TLinkedHashMap<K, V> base;

    TReversedLinkedHashMap(TLinkedHashMap<K, V> base) {
        this.base = base;
    }

    @Override
    public boolean equals(Object o) {
        return base.equals(o);
    }

    @Override
    public int hashCode() {
        return base.hashCode();
    }

    @Override
    public int size() {
        return base.size();
    }

    @Override
    public boolean isEmpty() {
        return base.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return base.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return base.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return base.get(key);
    }

    @Override
    public V put(K key, V value) {
        return base.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return base.remove(key);
    }

    @Override
    public void putAll(TMap<? extends K, ? extends V> m) {
        base.putAll(m);
    }

    @Override
    public void clear() {
        base.clear();
    }

    @Override
    public TSet<K> keySet() {
        return base.sequencedKeySet().reversed();
    }

    @Override
    public TCollection<V> values() {
        return base.sequencedValues().reversed();
    }

    @Override
    public TSet<TMap.Entry<K, V>> entrySet() {
        return base.sequencedEntrySet().reversed();
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return base.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (base.elementCount > 0) {
            int prevModCount = base.modCount;
            TLinkedHashMap.LinkedHashMapEntry<K, V> entry = base.tail;
            do {
                action.accept(entry.key, entry.value);
                entry = entry.chainBackward;
                if (base.modCount != prevModCount) {
                    throw new TConcurrentModificationException();
                }
            } while (entry != null);
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (base.elementCount > 0) {
            int prevModCount = base.modCount;
            TLinkedHashMap.LinkedHashMapEntry<K, V> entry = base.tail;
            do {
                entry.value = function.apply(entry.key, entry.value);
                entry = entry.chainBackward;
                if (base.modCount != prevModCount) {
                    throw new TConcurrentModificationException();
                }
            } while (entry != null);
        }
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return base.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return base.remove(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return base.replace(key, oldValue, newValue);
    }

    @Override
    public V replace(K key, V value) {
        return base.replace(key, value);
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return base.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return base.computeIfPresent(key, remappingFunction);
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return base.compute(key, remappingFunction);
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return base.merge(key, value, remappingFunction);
    }

    @Override
    public TSequencedMap<K, V> reversed() {
        return base;
    }

    @Override
    public TMap.Entry<K, V> firstEntry() {
        return base.lastEntry();
    }

    @Override
    public TMap.Entry<K, V> lastEntry() {
        return base.firstEntry();
    }

    @Override
    public TMap.Entry<K, V> pollFirstEntry() {
        return base.pollLastEntry();
    }

    @Override
    public TMap.Entry<K, V> pollLastEntry() {
        return base.pollFirstEntry();
    }

    @Override
    public V putFirst(K k, V v) {
        return base.putLast(k, v);
    }

    @Override
    public V putLast(K k, V v) {
        return base.putFirst(k, v);
    }

    @Override
    public TSequencedSet<K> sequencedKeySet() {
        return new LinkedHashMapKeySet<>(base, true);
    }

    @Override
    public TSequencedCollection<V> sequencedValues() {
        return new LinkedHashMapValues<>(base, true);
    }

    @Override
    public TSequencedSet<Entry<K, V>> sequencedEntrySet() {
        return new LinkedHashMapEntrySet<>(base, true);
    }

    private static <T> T checkNotNull(T node) {
        if (node == null) {
            throw new TNoSuchElementException();
        }
        return node;
    }

    static class LinkedHashMapEntrySet<K, V> extends TAbstractSet<TMap.Entry<K, V>>
            implements TSequencedSet<TMap.Entry<K, V>> {
        private final TLinkedHashMap<K, V> base;
        private final boolean reversed;

        LinkedHashMapEntrySet(TLinkedHashMap<K, V> base, boolean reversed) {
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
            return new EntryIterator<>(base, reversed);
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
            return checkNotNull(reversed ? base.tail : base.head);
        }

        @Override
        public final TMap.Entry<K, V> getLast() {
            return checkNotNull(reversed ? base.head : base.tail);
        }

        @Override
        public final TMap.Entry<K, V> removeFirst() {
            var e = checkNotNull(reversed ? base.tail : base.head);
            base.remove(e.key);
            return e;
        }

        @Override
        public final TMap.Entry<K, V> removeLast() {
            var e = checkNotNull(reversed ? base.head : base.tail);
            base.remove(e.key);
            return e;
        }

        @Override
        public TSequencedSet<TMap.Entry<K, V>> reversed() {
            return new LinkedHashMapEntrySet<>(base, !reversed);
        }
    }

    static class LinkedHashMapKeySet<K> extends TAbstractSet<K> implements TSequencedSet<K> {
        private final TLinkedHashMap<K, ?> base;
        private final boolean reversed;

        LinkedHashMapKeySet(TLinkedHashMap<K, ?> base, boolean reversed) {
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
            return new KeyIterator<>(base, reversed);
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
            return checkNotNull(reversed ? base.tail : base.head).key;
        }

        @Override
        public final K getLast() {
            return checkNotNull(reversed ? base.head : base.tail).key;
        }

        @Override
        public final K removeFirst() {
            var e = checkNotNull(reversed ? base.tail : base.head);
            base.remove(e.key);
            return e.key;
        }

        @Override
        public final K removeLast() {
            var e = checkNotNull(reversed ? base.head : base.tail);
            base.remove(e.key);
            return e.key;
        }

        @Override
        public TSequencedSet<K> reversed() {
            if (reversed) {
                return base.sequencedKeySet();
            } else {
                return new LinkedHashMapKeySet<>(base, true);
            }
        }
    }

    static class LinkedHashMapValues<V> extends TAbstractCollection<V> implements TSequencedCollection<V> {
        private final TLinkedHashMap<?, V> base;
        private final boolean reversed;

        LinkedHashMapValues(TLinkedHashMap<?, V> base, boolean reversed) {
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
            return new ValueIterator<>(base, reversed);
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
            return checkNotNull(reversed ? base.tail : base.head).value;
        }

        @Override
        public final V getLast() {
            return checkNotNull(reversed ? base.head : base.tail).value;
        }

        @Override
        public final V removeFirst() {
            THashMap.HashEntry<?, V> e = checkNotNull(reversed ? base.tail : base.head);
            return base.remove(e.key);
        }

        @Override
        public final V removeLast() {
            THashMap.HashEntry<?, V> e = checkNotNull(reversed ? base.head : base.tail);
            return base.remove(e.key);
        }

        @Override
        public TSequencedCollection<V> reversed() {
            if (reversed) {
                return base.sequencedValues();
            } else {
                return new LinkedHashMapValues<>(base, true);
            }
        }
    }

    private static class EntryIterator<K, V> extends AbstractMapIterator<K, V> implements TIterator<Entry<K, V>> {
        EntryIterator(TLinkedHashMap<K, V> map, boolean reversed) {
            super(map, reversed);
        }

        @Override
        public Entry<K, V> next() {
            makeNext();
            return currentEntry;
        }
    }

    private static class KeyIterator<K, V> extends AbstractMapIterator<K, V> implements TIterator<K> {
        KeyIterator(TLinkedHashMap<K, V> map, boolean reversed) {
            super(map, reversed);
        }

        @Override
        public K next() {
            makeNext();
            return currentEntry.key;
        }
    }

    private static class ValueIterator<K, V> extends AbstractMapIterator<K, V> implements TIterator<V> {
        ValueIterator(TLinkedHashMap<K, V> map, boolean reversed) {
            super(map, reversed);
        }

        @Override
        public V next() {
            makeNext();
            return currentEntry.value;
        }
    }

    static class AbstractMapIterator<K, V> {
        private final TLinkedHashMap<K, V> base;
        private final boolean reversed;
        private int expectedModCount;
        private TLinkedHashMap.LinkedHashMapEntry<K, V> futureEntry;
        TLinkedHashMap.LinkedHashMapEntry<K, V> currentEntry;

        AbstractMapIterator(TLinkedHashMap<K, V> base, boolean reversed) {
            this.base = base;
            this.reversed = reversed;
            expectedModCount = base.modCount;
            futureEntry = reversed ? base.tail : base.head;
        }

        public boolean hasNext() {
            return futureEntry != null;
        }

        final void checkConcurrentMod() throws TConcurrentModificationException {
            if (expectedModCount != base.modCount) {
                throw new TConcurrentModificationException();
            }
        }

        final void makeNext() {
            checkConcurrentMod();
            if (!hasNext()) {
                throw new TNoSuchElementException();
            }
            currentEntry = futureEntry;
            futureEntry = reversed ? futureEntry.chainBackward : futureEntry.chainForward;
        }

        public void remove() {
            if (currentEntry == null) {
                throw new IllegalStateException();
            }
            checkConcurrentMod();
            base.remove(currentEntry.key);
            currentEntry = null;
            expectedModCount++;
        }
    }
}
