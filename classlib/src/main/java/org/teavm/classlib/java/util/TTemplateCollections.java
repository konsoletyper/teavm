/*
 *  Copyright 2020 Alexey Andreev.
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

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public final class TTemplateCollections {

    private TTemplateCollections() {
    }

    public static class ImmutableArrayList<T> extends AbstractImmutableList<T> implements RandomAccess {
        private final T[] list;

        @SafeVarargs
        public ImmutableArrayList(T... list) {
            this.list = list;
        }

        @SuppressWarnings("unchecked")
        public ImmutableArrayList(TCollection<? extends T> collection) {
            T[] list = (T[]) new Object[collection.size()];

            TIterator<? extends T> iter = collection.iterator();
            int index = 0;
            while (iter.hasNext()) {
                T element = iter.next();
                if (element == null) {
                    throw new NullPointerException();
                }
                list[index++] = element;
            }

            this.list = list;
        }

        @Override
        public T get(int index) {
            return list[index];
        }

        @Override
        public int size() {
            return list.length;
        }
    }

    static class SingleElementList<T> extends AbstractImmutableList<T> implements RandomAccess {
        private T value;

        SingleElementList(T value) {
            this.value = value;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public T get(int index) {
            if (index == 0) {
                return value;
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        public void sort(TComparator<? super T> c) {
        }
    }

    static class TwoElementsList<T> extends AbstractImmutableList<T> implements RandomAccess {
        private T first;
        private T second;

        TwoElementsList(T first, T second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public T get(int index) {
            if (index == 0) {
                return first;
            } else if (index == 1) {
                return second;
            }
            throw new IndexOutOfBoundsException();
        }
    }

    static class SingleElementSet<T> extends AbstractImmutableSet<T> {
        private T element;

        SingleElementSet(T element) {
            this.element = element;
        }

        @Override
        public TIterator<T> iterator() {
            return new TIterator<T>() {
                private boolean more = true;

                @Override
                public boolean hasNext() {
                    return more;
                }

                @Override
                public T next() {
                    if (!more) {
                        throw new NoSuchElementException();
                    }
                    more = false;
                    return element;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean contains(Object o) {
            return Objects.equals(o, element);
        }
    }

    static class TwoElementsSet<T> extends AbstractImmutableSet<T> {
        private T first;
        private T second;

        TwoElementsSet(T first, T second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean contains(Object o) {
            return Objects.equals(o, first) || Objects.equals(o, second);
        }

        @Override
        public TIterator<T> iterator() {
            return new TIterator<T>() {
                private int index;

                @Override
                public boolean hasNext() {
                    return index < 2;
                }

                @Override
                public T next() {
                    switch (index++) {
                        case 0:
                            return first;
                        case 1:
                            return second;
                        default:
                            throw new NoSuchElementException();
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public int size() {
            return 2;
        }
    }

    static class NElementSet<T> extends AbstractImmutableSet<T> {
        private T[] data;

        /**
         * Throws an exception on duplicate elements.
         */
        @SafeVarargs
        NElementSet(T... data) {
            T[] table = data.clone();
            Arrays.fill(table, null);

            for (T element : data) {
                Objects.requireNonNull(element);

                int suggestedIndex = Math.abs(element.hashCode()) % data.length;
                int index = suggestedIndex;
                boolean found = false;
                while (index < data.length) {
                    T existingElement = table[index];
                    if (existingElement == null) {
                        found = true;
                        break;
                    } else if (existingElement.equals(element)) {
                        throw new IllegalArgumentException();
                    }
                    ++index;
                }
                if (!found) {
                    index = 0;
                    while (index < suggestedIndex) {
                        T existingElement = table[index];
                        if (existingElement == null) {
                            break;
                        } else if (existingElement.equals(element)) {
                            throw new IllegalArgumentException();
                        }
                        ++index;
                    }
                }
                table[index] = element;
            }

            this.data = table;
        }

        /**
         * Duplicate elements will be ignored (does NOT throw an exception).
         */
        @SuppressWarnings("unchecked")
        NElementSet(TCollection<T> collection) {
            T[] temp = (T[]) new Object[collection.size()];

            int index = 0;
            outerLoop:
            for (T element : (T[]) collection.toArray()) {
                if (element == null) {
                    throw new NullPointerException(String.format("Element at index %s is null", index));
                }

                int indexTemp = Math.abs(element.hashCode()) % temp.length;
                while (temp[indexTemp] != null) {
                    if (temp[indexTemp].equals(element)) {
                        continue outerLoop;
                    }
                    indexTemp = (indexTemp + 1) % temp.length;
                }
                temp[indexTemp] = element;
                index++;
            }

            T[] result = (T[]) new Object[index];
            index = 0;
            for (T element : temp) {
                if (element != null) {
                    result[index++] = element;
                }
            }

            this.data = result;
        }

        @Override
        public TIterator<T> iterator() {
            return new TIterator<T>() {
                private int index;

                @Override
                public boolean hasNext() {
                    return index < data.length;
                }

                @Override
                public T next() {
                    if (index >= data.length) {
                        throw new NoSuchElementException();
                    }
                    return data[index++];
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public int size() {
            return data.length;
        }

        @Override
        public boolean contains(Object o) {
            if (data.length == 0 || o == null) {
                return false;
            }

            int suggestedIndex = Math.abs(o.hashCode()) % data.length;
            for (int i = suggestedIndex; i < data.length; ++i) {
                if (data[i].equals(o)) {
                    return true;
                }
            }
            for (int i = 0; i < suggestedIndex; ++i) {
                if (data[i].equals(o)) {
                    return true;
                }
            }

            return false;
        }
    }

    static class SingleEntryMap<K, V> extends AbstractImmutableMap<K, V> {
        private Entry<K, V> entry;
        private AbstractImmutableSet<Entry<K, V>> entrySet;
        private AbstractImmutableSet<K> keySet;
        private AbstractImmutableList<V> values;

        SingleEntryMap(K key, V value) {
            this.entry = new ImmutableEntry<>(key, value);
        }

        @Override
        public TSet<Entry<K, V>> entrySet() {
            if (entrySet == null) {
                entrySet = new SingleElementSet<>(entry);
            }
            return entrySet;
        }

        @Override
        public V get(Object key) {
            return entry.getKey().equals(key) ? entry.getValue() : null;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return entry.getValue().equals(value);
        }

        @Override
        public boolean containsKey(Object key) {
            return entry.getKey().equals(key);
        }

        @Override
        public TSet<K> keySet() {
            if (keySet == null) {
                keySet = new SingleElementSet<>(entry.getKey());
            }
            return keySet;
        }

        @Override
        public TCollection<V> values() {
            if (values == null) {
                values = new SingleElementList<>(entry.getValue());
            }
            return values;
        }
    }

    static class TwoEntriesMap<K, V> extends AbstractImmutableMap<K, V> {
        private Entry<K, V> first;
        private Entry<K, V> second;
        private AbstractImmutableSet<Entry<K, V>> entrySet;
        private AbstractImmutableSet<K> keySet;
        private AbstractImmutableList<V> values;

        TwoEntriesMap(K k1, V v1, K k2, V v2) {
            this.first = new ImmutableEntry<>(k1, v1);
            this.second = new ImmutableEntry<>(k2, v2);
        }

        @Override
        public TSet<Entry<K, V>> entrySet() {
            if (entrySet == null) {
                entrySet = new TwoElementsSet<>(first, second);
            }
            return entrySet;
        }

        @Override
        public V get(Object key) {
            return first.getKey().equals(key)
                    ? first.getValue()
                    : second.getKey().equals(key) ? second.getValue() : null;
        }

        @Override
        public int size() {
            return 2;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return first.getValue().equals(value) || second.getValue().equals(value);
        }

        @Override
        public boolean containsKey(Object key) {
            return first.getKey().equals(key) || second.getKey().equals(key);
        }

        @Override
        public TSet<K> keySet() {
            if (keySet == null) {
                keySet = new TwoElementsSet<>(first.getKey(), second.getKey());
            }
            return keySet;
        }


        @Override
        public TCollection<V> values() {
            if (values == null) {
                values = new TwoElementsList<>(first.getValue(), second.getValue());
            }
            return values;
        }
    }

    static class NEtriesMap<K, V> extends AbstractImmutableMap<K, V> {
        private Entry<K, V>[] data;
        private AbstractImmutableSet<Entry<K, V>> entrySet;

        @SuppressWarnings("unchecked")
        @SafeVarargs
        NEtriesMap(Entry<K, V>... data) {
            this.data = toEntryArray(data);
        }

        @SuppressWarnings("unchecked")
        NEtriesMap(TMap<? extends K, ? extends V> map) {
            this.data = toEntryArray(map.entrySet().toArray(new TMap.Entry[0]));
        }

        /**
         * Creates an array where the {@code Entry} elements are positioned in such a way they
         * are compatible with the contract of {@link java.util.Map}.
         */
        @SuppressWarnings("unchecked")
        private Entry<K, V>[] toEntryArray(Entry<K, V>[] entries) {
            Entry<K, V>[] table = new Entry[entries.length];
            Arrays.fill(table, null);

            for (Entry<K, V> entry : entries) {
                Objects.requireNonNull(entry.getKey());
                Objects.requireNonNull(entry.getValue());

                int suggestedIndex = Math.abs(entry.getKey().hashCode()) % entries.length;
                int index = suggestedIndex;
                boolean found = false;
                while (index < entries.length) {
                    Entry<K, V> existingEntry = table[index];
                    if (existingEntry == null) {
                        found = true;
                        break;
                    } else if (existingEntry.getKey().equals(entry.getKey())) {
                        throw new IllegalArgumentException();
                    }
                    ++index;
                }
                if (!found) {
                    index = 0;
                    while (index < suggestedIndex) {
                        Entry<K, V> existingElement = table[index];
                        if (existingElement == null) {
                            break;
                        } else if (existingElement.getKey().equals(entry.getKey())) {
                            throw new IllegalArgumentException();
                        }
                        ++index;
                    }
                }
                table[index] = new ImmutableEntry<>(entry.getKey(), entry.getValue());
            }

            return table;
        }

        @Override
        public int size() {
            return data.length;
        }

        @Override
        public boolean isEmpty() {
            return data.length == 0;
        }

        @Override
        public boolean containsValue(Object value) {
            if (value == null) {
                return false;
            }
            for (Entry<K, V> entry : data) {
                if (entry.getValue().equals(value)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            if (key == null || data.length == 0) {
                return false;
            }
            int suggestedIndex = Math.abs(key.hashCode()) % data.length;
            for (int i = suggestedIndex; i < data.length; ++i) {
                if (data[i].getKey().equals(key)) {
                    return true;
                }
            }
            for (int i = 0; i < suggestedIndex; ++i) {
                if (data[i].getKey().equals(key)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public V get(Object key) {
            if (key == null) {
                return null;
            }
            int suggestedIndex = Math.abs(key.hashCode()) % data.length;
            for (int i = suggestedIndex; i < data.length; ++i) {
                Entry<K, V> entry = data[i];
                if (entry.getKey().equals(key)) {
                    return entry.getValue();
                }
            }
            for (int i = 0; i < suggestedIndex; ++i) {
                Entry<K, V> entry = data[i];
                if (entry.getKey().equals(key)) {
                    return entry.getValue();
                }
            }
            return null;
        }

        @Override
        public TSet<Entry<K, V>> entrySet() {
            if (entrySet == null) {
                entrySet = new AbstractImmutableSet<Entry<K, V>>() {
                    @Override
                    public int size() {
                        return data.length;
                    }

                    @Override
                    public TIterator<Entry<K, V>> iterator() {
                        return new TIterator<Entry<K, V>>() {
                            int index;

                            @Override
                            public boolean hasNext() {
                                return index < data.length;
                            }

                            @Override
                            public Entry<K, V> next() {
                                if (index == data.length) {
                                    throw new NoSuchElementException();
                                }
                                return data[index++];
                            }

                            @Override
                            public void remove() {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }

                    @Override
                    public boolean contains(Object o) {
                        if (!(o instanceof Entry)) {
                            return false;
                        }
                        Entry<?, ?> e = (Entry<?, ?>) o;

                        Object key = e.getKey();
                        if (key == null) {
                            return false;
                        }
                        int suggestedIndex = Math.abs(key.hashCode()) % data.length;
                        for (int i = suggestedIndex; i < data.length; ++i) {
                            Entry<K, V> entry = data[i];
                            if (entry.getKey().equals(key)) {
                                return entry.getValue().equals(e.getValue());
                            }
                        }
                        for (int i = 0; i < suggestedIndex; ++i) {
                            Entry<K, V> entry = data[i];
                            if (entry.getKey().equals(key)) {
                                return entry.getValue().equals(e.getValue());
                            }
                        }
                        return false;
                    }
                };
            }
            return entrySet;
        }
    }

    static abstract class AbstractImmutableList<T> extends TAbstractList<T> implements RandomAccess {
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public T remove(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void removeRange(int start, int end) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(TCollection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeIf(Predicate<? super T> filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(TCollection<?> c) {
            throw new UnsupportedOperationException();
        }
    }

    static abstract class AbstractImmutableSet<T> extends TAbstractSet<T> {
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(TCollection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeIf(Predicate<? super T> filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(TCollection<?> c) {
            throw new UnsupportedOperationException();
        }
    }

    static abstract class AbstractImmutableMap<K, V> extends TAbstractMap<K, V> {
        @Override
        public V put(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(TMap<? extends K, ? extends V> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean replace(K key, V value, V newValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V replace(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V putIfAbsent(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException();
        }
    }

    static class ImmutableEntry<K, V> implements TMap.Entry<K, V>, Cloneable {
        K key;
        V value;

        ImmutableEntry(K theKey, V theValue) {
            key = theKey;
            value = theValue;
        }

        @Override
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object instanceof TMap.Entry) {
                TMap.Entry<?, ?> entry = (TMap.Entry<?, ?>) object;
                return TObjects.equals(key, entry.getKey()) && TObjects.equals(value, entry.getValue());
            }
            return false;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public int hashCode() {
            return TObjects.hashCode(key) ^ TObjects.hashCode(value);
        }

        @Override
        public V setValue(V object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }
}
