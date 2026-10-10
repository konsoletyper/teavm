/*
 *  Copyright 2026 Gregory Mitchell.
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

import org.teavm.classlib.java.lang.TUnsupportedOperationException;

class TUnmodifiableSequencedMap<K, V> extends TAbstractMap<K, V> implements TSequencedMap<K, V> {
    private final TSequencedMap<K, V> map;

    TUnmodifiableSequencedMap(TSequencedMap<K, V> map) {
        this.map = TObjects.requireNonNull(map);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public V remove(Object key) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public void putAll(TMap<? extends K, ? extends V> m) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new TUnsupportedOperationException();
    }

    @Override
    public TMap.Entry<K, V> pollFirstEntry() {
        throw new TUnsupportedOperationException();
    }

    @Override
    public TMap.Entry<K, V> pollLastEntry() {
        throw new TUnsupportedOperationException();
    }

    @Override
    public TSet<K> keySet() {
        return TCollections.unmodifiableSet(map.keySet());
    }

    @Override
    public TCollection<V> values() {
        return TCollections.unmodifiableCollection(map.values());
    }

    @Override
    public TSet<TMap.Entry<K, V>> entrySet() {
        return TCollections.unmodifiableMapEntrySet(map.entrySet());
    }

    @Override
    public TSequencedMap<K, V> reversed() {
        return new TUnmodifiableSequencedMap<>(map.reversed());
    }

    @Override
    public TSequencedSet<K> sequencedKeySet() {
        return new SequencedSet<>(map.sequencedKeySet(), false);
    }

    @Override
    public TSequencedCollection<V> sequencedValues() {
        return new SequencedValues<>(map.sequencedValues());
    }

    @Override
    public TSequencedSet<TMap.Entry<K, V>> sequencedEntrySet() {
        return new SequencedSet<>(map.sequencedEntrySet(), true);
    }

    @Override
    public boolean equals(Object o) {
        return o == this || map.equals(o);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public String toString() {
        return map.toString();
    }

    private static class SequencedSet<E> extends TAbstractSet<E> implements TSequencedSet<E> {
        private final TSequencedSet<E> set;
        private final boolean entries;

        SequencedSet(TSequencedSet<E> set, boolean entries) {
            this.set = set;
            this.entries = entries;
        }

        @Override
        public TIterator<E> iterator() {
            TIterator<E> iterator = set.iterator();
            return new TIterator<>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @SuppressWarnings({ "unchecked", "rawtypes" })
                @Override
                public E next() {
                    E e = iterator.next();
                    return entries ? (E) new TAbstractMap.SimpleImmutableEntry((TMap.Entry) e) : e;
                }

                @Override
                public void remove() {
                    throw new TUnsupportedOperationException();
                }
            };
        }

        @Override
        public int size() {
            return set.size();
        }

        @Override
        public boolean remove(Object o) {
            throw new TUnsupportedOperationException();
        }

        @Override
        public TSequencedSet<E> reversed() {
            return new SequencedSet<>(set.reversed(), entries);
        }
    }

    private static class SequencedValues<V> extends TAbstractCollection<V> implements TSequencedCollection<V> {
        private final TSequencedCollection<V> values;

        SequencedValues(TSequencedCollection<V> values) {
            this.values = values;
        }

        @Override
        public TIterator<V> iterator() {
            return TCollections.unmodifiableIterator(values.iterator());
        }

        @Override
        public int size() {
            return values.size();
        }

        @Override
        public boolean remove(Object o) {
            throw new TUnsupportedOperationException();
        }

        @Override
        public TSequencedCollection<V> reversed() {
            return new SequencedValues<>(values.reversed());
        }
    }
}
