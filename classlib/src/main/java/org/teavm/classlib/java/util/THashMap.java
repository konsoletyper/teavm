/*
 *  Copyright 2014 Alexey Andreev.
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

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.*;
import org.teavm.interop.Rename;

public class THashMap<K, V> extends TAbstractMap<K, V> implements TCloneable, TSerializable {
    transient int elementCount;
    transient HashEntry<K, V>[] elementData;
    transient int modCount;
    private static final int DEFAULT_SIZE = 16;
    final float loadFactor;
    int threshold;

    static class HashEntry<K, V> extends TMapEntry<K, V> {
        final int origKeyHash;

        HashEntry<K, V> next;

        HashEntry(K theKey, int hash) {
            super(theKey, null);
            this.origKeyHash = hash;
        }

        HashEntry(K theKey, V theValue) {
            super(theKey, theValue);
            origKeyHash = theKey == null ? 0 : computeHashCode(theKey);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object clone() {
            HashEntry<K, V> entry = (HashEntry<K, V>) super.clone();
            if (next != null) {
                entry.next = (HashEntry<K, V>) next.clone();
            }
            return entry;
        }
    }

    private static class AbstractMapIterator<K, V>  {
        private int position;
        int expectedModCount;
        HashEntry<K, V> futureEntry;
        HashEntry<K, V> currentEntry;
        HashEntry<K, V> prevEntry;

        final THashMap<K, V> associatedMap;

        AbstractMapIterator(THashMap<K, V> hm) {
            associatedMap = hm;
            expectedModCount = hm.modCount;
            futureEntry = null;
        }

        public boolean hasNext() {
            if (futureEntry != null) {
                return true;
            }
            while (position < associatedMap.elementData.length) {
                if (associatedMap.elementData[position] == null) {
                    position++;
                } else {
                    return true;
                }
            }
            return false;
        }

        final void checkConcurrentMod() throws ConcurrentModificationException {
            if (expectedModCount != associatedMap.modCount) {
                throw new TConcurrentModificationException();
            }
        }

        final void makeNext() {
            checkConcurrentMod();
            if (!hasNext()) {
                throw new TNoSuchElementException();
            }
            if (futureEntry == null) {
                currentEntry = associatedMap.elementData[position++];
                futureEntry = currentEntry.next;
                prevEntry = null;
            } else {
                if (currentEntry != null) {
                    prevEntry = currentEntry;
                }
                currentEntry = futureEntry;
                futureEntry = futureEntry.next;
            }
        }

        public final void remove() {
            checkConcurrentMod();
            if (currentEntry == null) {
                throw new TIllegalStateException();
            }
            if (prevEntry == null) {
                int index = currentEntry.origKeyHash & (associatedMap.elementData.length - 1);
                associatedMap.elementData[index] = associatedMap.elementData[index].next;
            } else {
                prevEntry.next = currentEntry.next;
            }
            currentEntry = null;
            expectedModCount++;
            associatedMap.modCount++;
            associatedMap.elementCount--;

        }
    }


    private static class EntryIterator<K, V> extends AbstractMapIterator<K, V>
            implements TIterator<TMap.Entry<K, V>> {
        EntryIterator(THashMap<K, V> map) {
            super(map);
        }

        @Override
        public TMap.Entry<K, V> next() {
            makeNext();
            return currentEntry;
        }
    }

    private static class KeyIterator<K, V> extends AbstractMapIterator<K, V> implements TIterator<K> {
        KeyIterator(THashMap<K, V> map) {
            super(map);
        }

        @Override
        public K next() {
            makeNext();
            return currentEntry.key;
        }
    }

    private static class ValueIterator<K, V> extends AbstractMapIterator<K, V> implements TIterator<V> {
        ValueIterator(THashMap<K, V> map) {
            super(map);
        }

        @Override
        public V next() {
            makeNext();
            return currentEntry.value;
        }
    }

    static class HashMapEntrySet<K, V> extends TAbstractSet<TMap.Entry<K, V>> {
        private final THashMap<K, V> associatedMap;

        public HashMapEntrySet(THashMap<K, V> hm) {
            associatedMap = hm;
        }

        THashMap<K, V> hashMap() {
            return associatedMap;
        }

        @Override
        public int size() {
            return associatedMap.elementCount;
        }

        @Override
        public void clear() {
            associatedMap.clear();
        }

        @Override
        public boolean remove(Object object) {
            if (object instanceof TMap.Entry) {
                TMap.Entry<?, ?> oEntry = (TMap.Entry<?, ?>) object;
                TMap.Entry<K, V> entry = associatedMap.getEntry(oEntry.getKey());
                if (valuesEq(entry, oEntry)) {
                    associatedMap.removeEntry(entry);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean contains(Object object) {
            if (object instanceof TMap.Entry) {
                TMap.Entry<?, ?> oEntry = (TMap.Entry<?, ?>) object;
                TMap.Entry<K, V> entry = associatedMap.getEntry(oEntry.getKey());
                return valuesEq(entry, oEntry);
            }
            return false;
        }

        private static boolean valuesEq(TMap.Entry<?, ?> entry, TMap.Entry<?, ?> oEntry) {
            return entry != null
                    && (entry.getValue() == null
                    ? oEntry.getValue() == null
                    : areEqualValues(entry.getValue(), oEntry.getValue()));
        }

        @Override
        public TIterator<TMap.Entry<K, V>> iterator() {
            return new EntryIterator<>(associatedMap);
        }
    }

    @SuppressWarnings("unchecked")
    HashEntry<K, V>[] newElementArray(int s) {
        return new HashEntry[s];
    }

    public THashMap() {
        this(DEFAULT_SIZE);
    }

    public THashMap(int capacity) {
        this(capacity, 0.75f);  // default load factor of 0.75
    }

    private static int calculateCapacity(int x) {
        if (x >= 1 << 30) {
            return 1 << 30;
        }
        if (x == 0) {
            return 16;
        }
        x = x - 1;
        x |= x >> 1;
        x |= x >> 2;
        x |= x >> 4;
        x |= x >> 8;
        x |= x >> 16;
        return x + 1;
    }

    /**
     * Constructs a new {@code HashMap} instance with the specified capacity and
     * load factor.
     *
     * @param capacity
     *            the initial capacity of this hash map.
     * @param loadFactor
     *            the initial load factor.
     * @throws IllegalArgumentException
     *                when the capacity is less than zero or the load factor is
     *                less or equal to zero.
     */
    public THashMap(int capacity, float loadFactor) {
        if (capacity >= 0 && loadFactor > 0) {
            capacity = calculateCapacity(capacity);
            elementCount = 0;
            elementData = newElementArray(capacity);
            this.loadFactor = loadFactor;
            computeThreshold();
        } else {
            throw new TIllegalArgumentException();
        }
    }

    public THashMap(TMap<? extends K, ? extends V> map) {
        this(calculateCapacity(map.size()));
        putAllImpl(map);
    }

    @Override
    public void clear() {
        if (elementCount > 0) {
            elementCount = 0;
            Arrays.fill(elementData, null);
            modCount++;
        }
    }

    @Rename("clone")
    @SuppressWarnings("unchecked")
    public TObject clone0() {
        try {
            THashMap<K, V> map = (THashMap<K, V>) super.clone();
            map.elementCount = 0;
            map.elementData = newElementArray(elementData.length);
            map.putAll(this);

            return map;
        } catch (TCloneNotSupportedException e) {
            return null;
        }
    }

    private void computeThreshold() {
        threshold = (int) (elementData.length * loadFactor);
    }

    @Override
    public boolean containsKey(Object key) {
        HashEntry<K, V> m = getEntry(key);
        return m != null;
    }

    @Override
    public boolean containsValue(Object value) {
        if (value != null) {
            for (int i = 0; i < elementData.length; i++) {
                HashEntry<K, V> entry = elementData[i];
                while (entry != null) {
                    if (areEqualValues(value, entry.value)) {
                        return true;
                    }
                    entry = entry.next;
                }
            }
        } else {
            for (int i = 0; i < elementData.length; i++) {
                HashEntry<K, V> entry = elementData[i];
                while (entry != null) {
                    if (entry.value == null) {
                        return true;
                    }
                    entry = entry.next;
                }
            }
        }
        return false;
    }

    @Override
    public TSet<TMap.Entry<K, V>> entrySet() {
        return new HashMapEntrySet<>(this);
    }

    @Override
    public V get(Object key) {
        HashEntry<K, V> m = getEntry(key);
        if (m != null) {
            return m.value;
        }
        return null;
    }

    final HashEntry<K, V> getEntry(Object key) {
        HashEntry<K, V> m;
        if (key == null) {
            m = findNullKeyEntry();
        } else {
            int hash = computeHashCode(key);
            int index = hash & (elementData.length - 1);
            m = findNonNullKeyEntry(key, index, hash);
        }
        return m;
    }

    final HashEntry<K, V> findNonNullKeyEntry(Object key, int index, int keyHash) {
        HashEntry<K, V> m = elementData[index];
        while (m != null
                && (m.origKeyHash != keyHash || !areEqualKeys(key, m.key))) {
            m = m.next;
        }
        return m;
    }

    final HashEntry<K, V> findNullKeyEntry() {
        HashEntry<K, V> m = elementData[0];
        while (m != null && m.key != null) {
            m = m.next;
        }
        return m;
    }

    @Override
    public boolean isEmpty() {
        return elementCount == 0;
    }

    @Override
    public TSet<K> keySet() {
        if (cachedKeySet == null) {
            cachedKeySet = new TAbstractSet<K>() {
                @Override public boolean contains(Object object) {
                    return containsKey(object);
                }
                @Override public int size() {
                    return THashMap.this.size();
                }
                @Override public void clear() {
                    THashMap.this.clear();
                }
                @Override public boolean remove(Object key) {
                    HashEntry<K, V> entry = THashMap.this.removeEntry(key);
                    return entry != null;
                }
                @Override public TIterator<K> iterator() {
                    return new KeyIterator<>(THashMap.this);
                }
            };
        }
        return cachedKeySet;
    }

    @Override
    public V put(K key, V value) {
        return putImpl(key, value);
    }

    V putImpl(K key, V value) {
        HashEntry<K, V> entry;
        if (key == null) {
            entry = findNullKeyEntry();
            if (entry == null) {
                modCount++;
                entry = createHashedEntry(null, 0, 0);
                if (++elementCount > threshold) {
                    rehash();
                }
            }
        } else {
            int hash = computeHashCode(key);
            int index = hash & (elementData.length - 1);
            entry = findNonNullKeyEntry(key, index, hash);
            if (entry == null) {
                modCount++;
                entry = createHashedEntry(key, index, hash);
                if (++elementCount > threshold) {
                    rehash();
                }
            }
        }

        V result = entry.value;
        entry.value = value;
        return result;
    }

    HashEntry<K, V> createEntry(K key, int index, V value) {
        HashEntry<K, V> entry = new HashEntry<>(key, value);
        entry.next = elementData[index];
        elementData[index] = entry;
        return entry;
    }

    HashEntry<K, V> createHashedEntry(K key, int index, int hash) {
        HashEntry<K, V> entry = new HashEntry<>(key, hash);
        entry.next = elementData[index];
        elementData[index] = entry;
        return entry;
    }

    @Override
    public void putAll(TMap<? extends K, ? extends V> map) {
        if (!map.isEmpty()) {
            putAllImpl(map);
        }
    }

    private void putAllImpl(TMap<? extends K, ? extends V> map) {
        int capacity = elementCount + map.size();
        if (capacity > threshold) {
            rehash(capacity);
        }
        for (TIterator<? extends TMap.Entry<? extends K, ? extends V>> iter = map.entrySet().iterator();
                iter.hasNext();) {
            TMap.Entry<? extends K, ? extends V> entry = iter.next();
            putImpl(entry.getKey(), entry.getValue());
        }
    }

    void rehash(int capacity) {
        int length = calculateCapacity(capacity == 0 ? 1 : capacity << 1);

        HashEntry<K, V>[] newData = newElementArray(length);
        for (int i = 0; i < elementData.length; i++) {
            HashEntry<K, V> entry = elementData[i];
            elementData[i] = null;
            while (entry != null) {
                int index = entry.origKeyHash & (length - 1);
                HashEntry<K, V> next = entry.next;
                entry.next = newData[index];
                newData[index] = entry;
                entry = next;
            }
        }
        elementData = newData;
        computeThreshold();
    }

    void rehash() {
        rehash(elementData.length);
    }

    @Override
    public V remove(Object key) {
        HashEntry<K, V> entry = removeEntry(key);
        if (entry != null) {
            return entry.value;
        }
        return null;
    }

    final void removeEntry(HashEntry<K, V> entry) {
        int index = entry.origKeyHash & (elementData.length - 1);
        HashEntry<K, V> m = elementData[index];
        if (m == entry) {
            elementData[index] = entry.next;
        } else {
            while (m.next != entry) {
                m = m.next;
            }
            m.next = entry.next;

        }
        modCount++;
        elementCount--;
    }

    final HashEntry<K, V> removeEntry(Object key) {
        int index = 0;
        HashEntry<K, V> entry;
        HashEntry<K, V> last = null;
        if (key != null) {
            int hash = computeHashCode(key);
            index = hash & (elementData.length - 1);
            entry = elementData[index];
            while (entry != null && !(entry.origKeyHash == hash && areEqualKeys(key, entry.key))) {
                last = entry;
                entry = entry.next;
            }
        } else {
            entry = elementData[0];
            while (entry != null && entry.key != null) {
                last = entry;
                entry = entry.next;
            }
        }
        if (entry == null) {
            return null;
        }
        if (last == null) {
            elementData[index] = entry.next;
        } else {
            last.next = entry.next;
        }
        modCount++;
        elementCount--;
        return entry;
    }

    @Override
    public int size() {
        return elementCount;
    }

    @Override
    public TCollection<V> values() {
        if (cachedValues == null) {
            cachedValues = new TAbstractCollection<V>() {
                @Override public boolean contains(Object object) {
                    return containsValue(object);
                }
                @Override public int size() {
                    return THashMap.this.size();
                }
                @Override public void clear() {
                    THashMap.this.clear();
                }
                @Override public TIterator<V> iterator() {
                    return new ValueIterator<>(THashMap.this);
                }
            };
        }
        return cachedValues;
    }

    static int computeHashCode(Object key) {
        return key.hashCode();
    }

    static boolean areEqualKeys(Object key1, Object key2) {
        return (key1 == key2) || key1.equals(key2);
    }

    static boolean areEqualValues(Object value1, Object value2) {
        return (value1 == value2) || value1.equals(value2);
    }
}
