/*
 *  Copyright 2024 Alexey Andreev.
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
/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.java.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

public class TWeakHashMap<K, V> extends TAbstractMap<K, V> implements TMap<K, V> {
    private static final int DEFAULT_SIZE = 16;

    private final ReferenceQueue<K> referenceQueue;
    private int elementCount;
    private Entry<K, V>[] elementData;
    private final int loadFactor;
    private int threshold;
    private int modCount;

    // Simple utility method to isolate unchecked cast for array creation
    @SuppressWarnings("unchecked")
    private static <K, V> Entry<K, V>[] newEntryArray(int size) {
        return new Entry[size];
    }

    private static final class Entry<K, V> extends WeakReference<K> implements TMap.Entry<K, V> {
        int hash;
        boolean isNull;
        V value;
        Entry<K, V> next;

        interface Type<R, K, V> {
            R get(TMap.Entry<K, V> entry);
        }

        Entry(K key, V object, ReferenceQueue<K> queue) {
            super(key, queue);
            isNull = key == null;
            hash = isNull ? 0 : key.hashCode();
            value = object;
        }

        @Override
        public K getKey() {
            return super.get();
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V object) {
            V result = value;
            value = object;
            return result;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Map.Entry)) {
                return false;
            }
            var entry = (Map.Entry<?, ?>) other;
            Object key = super.get();
            return Objects.equals(key, entry.getKey()) && Objects.equals(value, entry.getValue());
        }

        @Override
        public int hashCode() {
            return hash ^ Objects.hashCode(value);
        }

        @Override
        public String toString() {
            return super.get() + "=" + value;
        }
    }

    class HashIterator<R> implements TIterator<R> {
        private int position;
        private int expectedModCount;

        private Entry<K, V> currentEntry;
        private Entry<K, V> nextEntry;

        private K nextKey;

        final Entry.Type<R, K, V> type;

        HashIterator(Entry.Type<R, K, V> type) {
            this.type = type;
            expectedModCount = modCount;
        }

        @Override
        public boolean hasNext() {
            if (nextEntry != null && (nextKey != null || nextEntry.isNull)) {
                return true;
            }
            while (true) {
                if (nextEntry == null) {
                    while (position < elementData.length) {
                        nextEntry = elementData[position++];
                        if (nextEntry != null) {
                            break;
                        }
                    }
                    if (nextEntry == null) {
                        return false;
                    }
                }
                // ensure key of next entry is not gc'ed
                nextKey = nextEntry.get();
                if (nextKey != null || nextEntry.isNull) {
                    return true;
                }
                nextEntry = nextEntry.next;
            }
        }

        @Override
        public R next() {
            if (expectedModCount == modCount) {
                if (hasNext()) {
                    currentEntry = nextEntry;
                    nextEntry = currentEntry.next;
                    R result = type.get(currentEntry);
                    // free the key
                    nextKey = null;
                    return result;
                }
                throw new NoSuchElementException();
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public void remove() {
            if (expectedModCount == modCount) {
                if (currentEntry != null) {
                    removeEntry(currentEntry);
                    currentEntry = null;
                    expectedModCount++;
                    // cannot poll() as that would change the expectedModCount
                } else {
                    throw new IllegalStateException();
                }
            } else {
                throw new ConcurrentModificationException();
            }
        }
    }

    public TWeakHashMap() {
        this(DEFAULT_SIZE);
    }

    public TWeakHashMap(int capacity) {
        if (capacity >= 0) {
            elementCount = 0;
            elementData = newEntryArray(capacity == 0 ? 1 : capacity);
            loadFactor = 7500; // Default load factor of 0.75
            computeMaxSize();
            referenceQueue = new ReferenceQueue<>();
        } else {
            throw new IllegalArgumentException();
        }
    }

    public TWeakHashMap(int capacity, float loadFactor) {
        if (capacity >= 0 && loadFactor > 0) {
            elementCount = 0;
            elementData = newEntryArray(capacity == 0 ? 1 : capacity);
            this.loadFactor = (int) (loadFactor * 10000);
            computeMaxSize();
            referenceQueue = new ReferenceQueue<>();
        } else {
            throw new IllegalArgumentException();
        }
    }

    public TWeakHashMap(TMap<? extends K, ? extends V> map) {
        this(map.size() < 6 ? 11 : map.size() * 2);
        putAllImpl(map);
    }

    @Override
    public void clear() {
        if (elementCount > 0) {
            elementCount = 0;
            Arrays.fill(elementData, null);
            modCount++;
            while (referenceQueue.poll() != null) {
                // do nothing
            }
        }
    }

    private void computeMaxSize() {
        threshold = (int) ((long) elementData.length * loadFactor / 10000);
    }

    @Override
    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    @Override
    public TSet<TMap.Entry<K, V>> entrySet() {
        poll();
        return new TAbstractSet<>() {
            @Override
            public int size() {
                return TWeakHashMap.this.size();
            }

            @Override
            public void clear() {
                TWeakHashMap.this.clear();
            }

            @Override
            public boolean remove(Object object) {
                if (contains(object)) {
                    TWeakHashMap.this.remove(((Map.Entry<?, ?>) object).getKey());
                    return true;
                }
                return false;
            }

            @Override
            public boolean contains(Object object) {
                if (object instanceof TMap.Entry) {
                    var entry = getEntry(((TMap.Entry<?, ?>) object).getKey());
                    if (entry != null) {
                        Object key = entry.get();
                        if (key != null || entry.isNull) {
                            return object.equals(entry);
                        }
                    }
                }
                return false;
            }

            @Override
            public TIterator<TMap.Entry<K, V>> iterator() {
                return new HashIterator<>(entry -> entry);
            }
        };
    }

    @Override
    public TSet<K> keySet() {
        poll();
        if (cachedKeySet == null) {
            cachedKeySet = new TAbstractSet<K>() {
                @Override
                public boolean contains(Object object) {
                    return containsKey(object);
                }

                @Override
                public int size() {
                    return TWeakHashMap.this.size();
                }

                @Override
                public void clear() {
                    TWeakHashMap.this.clear();
                }

                @Override
                public boolean remove(Object key) {
                    if (containsKey(key)) {
                        TWeakHashMap.this.remove(key);
                        return true;
                    }
                    return false;
                }

                @Override
                public TIterator<K> iterator() {
                    return new HashIterator<>(TMap.Entry::getKey);
                }

                @Override
                public Object[] toArray() {
                    var coll = new TArrayList<K>(size());

                    for (var iter = iterator(); iter.hasNext();) {
                        coll.add(iter.next());
                    }
                    return coll.toArray();
                }

                @Override
                public <T> T[] toArray(T[] contents) {
                    var coll = new ArrayList<K>(size());

                    for (var iter = iterator(); iter.hasNext();) {
                        coll.add(iter.next());
                    }
                    return coll.toArray(contents);
                }
            };
        }
        return cachedKeySet;
    }

    @Override
    public TCollection<V> values() {
        poll();
        if (cachedValues == null) {
            cachedValues = new TAbstractCollection<V>() {
                @Override
                public int size() {
                    return TWeakHashMap.this.size();
                }

                @Override
                public void clear() {
                    TWeakHashMap.this.clear();
                }

                @Override
                public boolean contains(Object object) {
                    return containsValue(object);
                }

                @Override
                public TIterator<V> iterator() {
                    return new HashIterator<>(TMap.Entry::getValue);
                }
            };
        }
        return cachedValues;
    }

    @Override
    public V get(Object key) {
        poll();
        if (key != null) {
            int index = (key.hashCode() & 0x7FFFFFFF) % elementData.length;
            var entry = elementData[index];
            while (entry != null) {
                if (key.equals(entry.get())) {
                    return entry.value;
                }
                entry = entry.next;
            }
            return null;
        }
        var entry = elementData[0];
        while (entry != null) {
            if (entry.isNull) {
                return entry.value;
            }
            entry = entry.next;
        }
        return null;
    }

    private Entry<K, V> getEntry(Object key) {
        poll();
        if (key != null) {
            int index = (key.hashCode() & 0x7FFFFFFF) % elementData.length;
            Entry<K, V> entry = elementData[index];
            while (entry != null) {
                if (key.equals(entry.get())) {
                    return entry;
                }
                entry = entry.next;
            }
            return null;
        }
        Entry<K, V> entry = elementData[0];
        while (entry != null) {
            if (entry.isNull) {
                return entry;
            }
            entry = entry.next;
        }
        return null;
    }

    @Override
    public boolean containsValue(Object value) {
        poll();
        if (value != null) {
            for (int i = elementData.length; --i >= 0;) {
                var entry = elementData[i];
                while (entry != null) {
                    K key = entry.get();
                    if ((key != null || entry.isNull) && value.equals(entry.value)) {
                        return true;
                    }
                    entry = entry.next;
                }
            }
        } else {
            for (int i = elementData.length; --i >= 0;) {
                var entry = elementData[i];
                while (entry != null) {
                    K key = entry.get();
                    if ((key != null || entry.isNull) && entry.value == null) {
                        return true;
                    }
                    entry = entry.next;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @SuppressWarnings("unchecked")
    private void poll() {
        Entry<K, V> toRemove;
        while ((toRemove = (Entry<K, V>) referenceQueue.poll()) != null) {
            removeEntry(toRemove);
        }
    }

    private void removeEntry(Entry<K, V> toRemove) {
        Entry<K, V> entry;
        Entry<K, V> last = null;
        int index = (toRemove.hash & 0x7FFFFFFF) % elementData.length;
        entry = elementData[index];
        // Ignore queued entries which cannot be found, the user could
        // have removed them before they were queued, i.e. using clear()
        while (entry != null) {
            if (toRemove == entry) {
                modCount++;
                if (last == null) {
                    elementData[index] = entry.next;
                } else {
                    last.next = entry.next;
                }
                elementCount--;
                break;
            }
            last = entry;
            entry = entry.next;
        }
    }

    @Override
    public V put(K key, V value) {
        poll();
        int index = 0;
        Entry<K, V> entry;
        if (key != null) {
            index = (key.hashCode() & 0x7FFFFFFF) % elementData.length;
            entry = elementData[index];
            while (entry != null && !key.equals(entry.get())) {
                entry = entry.next;
            }
        } else {
            entry = elementData[0];
            while (entry != null && !entry.isNull) {
                entry = entry.next;
            }
        }
        if (entry == null) {
            modCount++;
            if (++elementCount > threshold) {
                rehash();
                index = key == null ? 0 : (key.hashCode() & 0x7FFFFFFF) % elementData.length;
            }
            entry = new Entry<>(key, value, referenceQueue);
            entry.next = elementData[index];
            elementData[index] = entry;
            return null;
        }
        V result = entry.value;
        entry.value = value;
        return result;
    }

    private void rehash() {
        int length = elementData.length << 1;
        if (length == 0) {
            length = 1;
        }
        Entry<K, V>[] newData = newEntryArray(length);
        for (var elementDatum : elementData) {
            var entry = elementDatum;
            while (entry != null) {
                int index = entry.isNull ? 0 : (entry.hash & 0x7FFFFFFF) % length;
                var next = entry.next;
                entry.next = newData[index];
                newData[index] = entry;
                entry = next;
            }
        }
        elementData = newData;
        computeMaxSize();
    }

    @Override
    public void putAll(TMap<? extends K, ? extends V> map) {
        putAllImpl(map);
    }

    /**
     * Removes the mapping with the specified key from this map.
     *
     * @param key
     *            the key of the mapping to remove.
     * @return the value of the removed mapping or {@code null} if no mapping
     *         for the specified key was found.
     */
    @Override
    public V remove(Object key) {
        poll();
        int index = 0;
        Entry<K, V> entry;
        Entry<K, V> last = null;
        if (key != null) {
            index = (key.hashCode() & 0x7FFFFFFF) % elementData.length;
            entry = elementData[index];
            while (entry != null && !key.equals(entry.get())) {
                last = entry;
                entry = entry.next;
            }
        } else {
            entry = elementData[0];
            while (entry != null && !entry.isNull) {
                last = entry;
                entry = entry.next;
            }
        }
        if (entry != null) {
            modCount++;
            if (last == null) {
                elementData[index] = entry.next;
            } else {
                last.next = entry.next;
            }
            elementCount--;
            return entry.value;
        }
        return null;
    }

    @Override
    public int size() {
        poll();
        return elementCount;
    }

    private void putAllImpl(TMap<? extends K, ? extends V> map) {
        if (map.entrySet() != null) {
            super.putAll(map);
        }
    }
}
