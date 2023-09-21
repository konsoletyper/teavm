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

package org.teavm.classlib.java.util.concurrent;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TCloneNotSupportedException;
import org.teavm.classlib.java.lang.TCloneable;
import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TIllegalStateException;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.util.TAbstractCollection;
import org.teavm.classlib.java.util.TAbstractMap;
import org.teavm.classlib.java.util.TAbstractSet;
import org.teavm.classlib.java.util.TCollection;
import org.teavm.classlib.java.util.TConcurrentModificationException;
import org.teavm.classlib.java.util.TIterator;
import org.teavm.classlib.java.util.TMap;
import org.teavm.classlib.java.util.TNoSuchElementException;
import org.teavm.classlib.java.util.TSet;
import org.teavm.interop.Rename;

public class TConcurrentHashMap<K, V> extends TAbstractMap<K, V>
        implements TConcurrentMap<K, V>, TCloneable, TSerializable {
    private transient int elementCount;
    private transient HashEntry<K, V>[] elementData;
    private transient int modCount;
    private static final int DEFAULT_SIZE = 16;
    private final float loadFactor;
    private int threshold;
    private transient TSet<K> cachedKeySet;
    private transient TCollection<V> cachedValues;

    static class HashEntry<K, V> extends TMapEntry<K, V> {
        final int origKeyHash;
        HashEntry<K, V> next;
        boolean removed;

        HashEntry(K theKey, int hash) {
            super(theKey, null);
            this.origKeyHash = hash;
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

        final TConcurrentHashMap<K, V> associatedMap;

        AbstractMapIterator(TConcurrentHashMap<K, V> hm) {
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
            implements TIterator<Entry<K, V>> {
        EntryIterator(TConcurrentHashMap<K, V> map) {
            super(map);
        }

        @Override
        public Entry<K, V> next() {
            makeNext();
            return currentEntry;
        }
    }

    private static class KeyIterator<K, V> extends AbstractMapIterator<K, V> implements TIterator<K> {
        KeyIterator(TConcurrentHashMap<K, V> map) {
            super(map);
        }

        @Override
        public K next() {
            makeNext();
            return currentEntry.key;
        }
    }

    private static class ValueIterator<K, V> extends AbstractMapIterator<K, V> implements TIterator<V> {
        ValueIterator(TConcurrentHashMap<K, V> map) {
            super(map);
        }

        @Override
        public V next() {
            makeNext();
            return currentEntry.value;
        }
    }

    static class HashMapEntrySet<K, V> extends TAbstractSet<Entry<K, V>> {
        private final TConcurrentHashMap<K, V> associatedMap;

        HashMapEntrySet(TConcurrentHashMap<K, V> hm) {
            associatedMap = hm;
        }

        TConcurrentHashMap<K, V> hashMap() {
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
                var oEntry = (Entry<?, ?>) object;
                var entry = associatedMap.getEntryByKeyAndValue(oEntry.getKey(), oEntry.getValue());
                if (entry != null) {
                    associatedMap.removeEntry(entry);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean contains(Object object) {
            if (object instanceof TMap.Entry) {
                var oEntry = (Entry<?, ?>) object;
                return associatedMap.getEntryByKeyAndValue(oEntry.getKey(), oEntry.getValue()) != null;
            }
            return false;
        }

        @Override
        public TIterator<Entry<K, V>> iterator() {
            return new EntryIterator<>(associatedMap);
        }
    }

    @SuppressWarnings("unchecked")
    HashEntry<K, V>[] newElementArray(int s) {
        return new HashEntry[s];
    }

    public TConcurrentHashMap() {
        this(DEFAULT_SIZE);
    }

    public TConcurrentHashMap(int capacity) {
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

    public TConcurrentHashMap(int capacity, float loadFactor) {
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

    public TConcurrentHashMap(TMap<? extends K, ? extends V> map) {
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
            var map = (TConcurrentHashMap<K, V>) super.clone();
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
        return getEntry(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        repeatTable: do {
            var table = elementData;

            if (value != null) {
                for (var i = 0; i < table.length; i++) {
                    do {
                        var first = table[i];
                        if (first == null) {
                            break;
                        }
                        var entry = first;

                        while (entry != null) {
                            boolean equal = areEqualValues(value, entry.value);
                            if (table != elementData) {
                                continue repeatTable;
                            }
                            if (equal) {
                                return true;
                            }
                            entry = entry.next;
                        }

                        if (first == table[i]) {
                            break;
                        }
                    } while (true);
                }
            } else {
                for (var i = 0; i < elementData.length; i++) {
                    var entry = elementData[i];
                    while (entry != null) {
                        if (entry.value == null) {
                            return true;
                        }
                        entry = entry.next;
                    }
                }
            }
            return false;
        } while (true);
    }

    @Override
    public TSet<Entry<K, V>> entrySet() {
        return new HashMapEntrySet<>(this);
    }

    @Override
    public V get(Object key) {
        var m = getEntry(key);
        if (m != null) {
            return m.value;
        }
        return null;
    }

    @Override
    public boolean remove(Object key, Object value) {
        var entry = getEntryByKeyAndValue(key, value);
        if (entry != null) {
            removeEntry(entry);
            return true;
        }
        return false;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        var entry = getEntryByKeyAndValue(key, oldValue);
        if (entry != null) {
            entry.setValue(newValue);
            return true;
        }
        return false;
    }

    @Override
    public V replace(K key, V value) {
        var entry = getEntry(key);
        if (entry == null) {
            return null;
        }

        var result = entry.getValue();
        entry.setValue(value);
        return result;
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        var m = getEntry(key);
        return m != null ? m.getValue() : defaultValue;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        var table = elementData;
        for (var entry : table) {
            while (entry != null) {
                if (!entry.removed) {
                    action.accept(entry.getKey(), entry.getValue());
                }
                entry = entry.next;
            }
        }
    }

    @Override
    public V putIfAbsent(K key, V value) {
        var hash = Objects.hashCode(key);
        var entry = getEntry(key, hash);

        if (entry != null) {
            return entry.getValue();
        }
        var index = computeIndex(hash);
        entry = placeHashedEntry(key, index, hash);
        entry.setValue(value);
        return null;
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        var table = elementData;
        for (var entry : table) {
            while (entry != null) {
                if (!entry.removed) {
                    var newValue = function.apply(entry.getKey(), entry.getValue());
                    Objects.requireNonNull(newValue);
                    entry.setValue(newValue);
                }
                entry = entry.next;
            }
        }
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);

        var hash = Objects.hashCode(key);
        var entry = getEntry(key, hash);
        if (entry != null) {
            return entry.getValue();
        }

        var newValue = mappingFunction.apply(key);
        entry = getEntry(key, hash);
        if (entry != null) {
            return entry.getValue();
        }
        var index = computeIndex(hash);
        entry = placeHashedEntry(key, index, hash);
        entry.setValue(newValue);
        return newValue;
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        int hash = Objects.hashCode(key);

        V newValue = null;
        var newValueComputed = false;
        while (true) {
            var entry = getEntry(key, hash);
            if (entry == null) {
                return null;
            }

            var oldValue = entry.getValue();
            if (!newValueComputed) {
                newValueComputed = true;
                newValue = remappingFunction.apply(key, oldValue);
            }
            entry = getEntry(key, hash);
            if (entry == null) {
                return null;
            } else if (entry.getValue() == oldValue) {
                entry.setValue(newValue);
                return newValue;
            }
        }
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        var hash = Objects.hashCode(key);

        while (true) {
            var entry = getEntry(key, hash);
            if (entry == null) {
                var newValue = remappingFunction.apply(key, null);
                if (getEntry(key, hash) == null) {
                    if (newValue != null) {
                        var index = computeIndex(hash);
                        entry = placeHashedEntry(key, index, hash);
                        entry.setValue(newValue);
                    }
                    return newValue;
                }
            } else {
                var oldValue = entry.getValue();
                var newValue = remappingFunction.apply(key, oldValue);
                entry = getEntry(key, hash);
                if (entry != null && entry.getValue() == oldValue) {
                    if (newValue == null) {
                        removeEntry(entry);
                    } else {
                        entry.setValue(newValue);
                    }
                    return newValue;
                }
            }
        }
    }

    private HashEntry<K, V> getEntry(Object key) {
        return getEntry(key, Objects.hashCode(key));
    }

    private HashEntry<K, V> getEntry(Object key, int hash) {
        if (key == null) {
            return findNullKeyEntry();
        } else {
            repeatTable:
            do {
                var table = elementData;
                int index = hash & (table.length - 1);

                repeatElement:
                do {
                    var first = table[index];
                    if (first == null) {
                        return null;
                    }
                    var m = first;

                    while (m != null) {
                        if (!m.removed && m.origKeyHash == hash) {
                            var equal = areEqualKeys(key, m.key);
                            if (table != elementData) {
                                continue repeatTable;
                            }
                            if (equal) {
                                if (m.removed) {
                                    continue repeatElement;
                                }
                                return m;
                            }
                        }
                        m = m.next;
                    }

                    if (first == table[index]) {
                        return null;
                    }
                } while (true);
            } while (true);
        }
    }

    private HashEntry<K, V> getEntryByKeyAndValue(Object key, Object value) {
        var hash = Objects.hashCode(key);
        repeatTable:
        do {
            var table = elementData;
            int index = hash & (table.length - 1);

            repeatElement:
            do {
                var first = table[index];
                if (first == null) {
                    return null;
                }
                var m = first;

                while (m != null) {
                    if (m.origKeyHash == hash) {
                        boolean equal = key != null ? areEqualKeys(key, m.key) : m.key == null;
                        if (table != elementData) {
                            continue repeatTable;
                        }
                        if (m.removed) {
                            continue repeatElement;
                        }
                        if (equal) {
                            equal = areEqualValues(value, m.value);
                            if (table != elementData) {
                                continue repeatTable;
                            }
                            if (m.removed) {
                                continue repeatElement;
                            }
                            return equal ? m : null;
                        }
                    }
                    m = m.next;
                }

                if (first == table[index]) {
                    return null;
                }
            } while (true);
        } while (true);
    }

    private HashEntry<K, V> findNullKeyEntry() {
        var m = elementData[0];
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
            cachedKeySet = new TAbstractSet<>() {
                @Override public boolean contains(Object object) {
                    return containsKey(object);
                }
                @Override public int size() {
                    return TConcurrentHashMap.this.size();
                }
                @Override public void clear() {
                    TConcurrentHashMap.this.clear();
                }
                @Override public boolean remove(Object key) {
                    HashEntry<K, V> entry = TConcurrentHashMap.this.getEntry(key);
                    if (entry != null) {
                        TConcurrentHashMap.this.removeEntry(entry);
                        return true;
                    }
                    return false;
                }
                @Override public TIterator<K> iterator() {
                    return new KeyIterator<>(TConcurrentHashMap.this);
                }
            };
        }
        return cachedKeySet;
    }

    @Override
    public V put(K key, V value) {
        return putImpl(key, value);
    }

    private V putImpl(K key, V value) {
        var hash = Objects.hashCode(key);
        var entry = getEntry(key, hash);
        var index = computeIndex(hash);

        if (entry == null) {
            entry = placeHashedEntry(key, index, hash);
        }

        V result = entry.value;
        entry.value = value;
        return result;
    }

    private HashEntry<K, V> placeHashedEntry(K key, int index, int hash) {
        var entry = createHashedEntry(key, index, hash);
        modCount++;
        if (++elementCount > threshold) {
            rehash();
        }
        return entry;
    }

    private HashEntry<K, V> createHashedEntry(K key, int index, int hash) {
        var entry = new HashEntry<K, V>(key, hash);
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
        for (var iter = map.entrySet().iterator(); iter.hasNext();) {
            var entry = iter.next();
            putImpl(entry.getKey(), entry.getValue());
        }
    }

    private void rehash(int capacity) {
        int length = calculateCapacity(capacity == 0 ? 1 : capacity << 1);

        var newData = newElementArray(length);
        for (int i = 0; i < elementData.length; i++) {
            var entry = elementData[i];
            elementData[i] = null;
            while (entry != null) {
                int index = entry.origKeyHash & (length - 1);
                var next = entry.next;
                entry.next = newData[index];
                newData[index] = entry;
                entry = next;
            }
        }
        elementData = newData;
        computeThreshold();
    }

    private void rehash() {
        rehash(elementData.length);
    }

    @Override
    public V remove(Object key) {
        var entry = getEntry(key);
        if (entry == null) {
            return null;
        }
        removeEntry(entry);
        return entry.value;
    }

    private void removeEntry(HashEntry<K, V> entry) {
        var index = entry.origKeyHash & (elementData.length - 1);
        var m = elementData[index];
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
        entry.removed = true;
    }

    @Override
    public int size() {
        return elementCount;
    }

    @Override
    public TCollection<V> values() {
        if (cachedValues == null) {
            cachedValues = new TAbstractCollection<>() {
                @Override public boolean contains(Object object) {
                    return containsValue(object);
                }
                @Override public int size() {
                    return TConcurrentHashMap.this.size();
                }
                @Override public void clear() {
                    TConcurrentHashMap.this.clear();
                }
                @Override public TIterator<V> iterator() {
                    return new ValueIterator<>(TConcurrentHashMap.this);
                }
            };
        }
        return cachedValues;
    }

    private static boolean areEqualKeys(Object key1, Object key2) {
        return (key1 == key2) || key1.equals(key2);
    }

    private static boolean areEqualValues(Object value1, Object value2) {
        return (value1 == value2) || value1.equals(value2);
    }

    private int computeIndex(int hash) {
        return (hash & 0x7FFFFFFF) % elementData.length;
    }
}
