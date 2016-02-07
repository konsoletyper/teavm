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
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TCloneNotSupportedException;
import org.teavm.classlib.java.lang.TCloneable;
import org.teavm.classlib.java.lang.TNullPointerException;
import org.teavm.classlib.java.lang.TUnsupportedOperationException;

public class THashtable<K, V> extends TDictionary<K, V> implements TMap<K, V>,
        TCloneable, TSerializable {

    transient int elementCount;

    transient Entry<K, V>[] elementData;

    private float loadFactor;

    private int threshold;

    transient int firstSlot;

    transient int lastSlot = -1;

    transient int modCount;

    private static final TEnumeration<?> EMPTY_ENUMERATION = new TEnumeration<Object>() {
        @Override
        public boolean hasMoreElements() {
            return false;
        }

        @Override
        public Object nextElement() {
            throw new TNoSuchElementException();
        }
    };

    private static final TIterator<?> EMPTY_ITERATOR = new TIterator<Object>() {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new TNoSuchElementException();
        }

        @Override
        public void remove() {
            throw new IllegalStateException();
        }
    };

    private static <K, V> Entry<K, V> newEntry(K key, V value, @SuppressWarnings("unused") int hash) {
        return new Entry<>(key, value);
    }

    private static class Entry<K, V> extends TMapEntry<K, V> {
        Entry<K, V> next;

        final int hashcode;

        Entry(K theKey, V theValue) {
            super(theKey, theValue);
            hashcode = theKey.hashCode();
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object clone() {
            Entry<K, V> entry = (Entry<K, V>) super.clone();
            if (next != null) {
                entry.next = (Entry<K, V>) next.clone();
            }
            return entry;
        }

        @Override
        public V setValue(V object) {
            if (object == null) {
                throw new NullPointerException();
            }
            V result = value;
            value = object;
            return result;
        }

        public int getKeyHash() {
            return key.hashCode();
        }

        public boolean equalsKey(Object aKey, @SuppressWarnings("unused") int hash) {
            return hashcode == aKey.hashCode() && key.equals(aKey);
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }

    private class HashIterator<E> implements TIterator<E> {
        int position;
        int expectedModCount;

        final TMapEntry.Type<E, K, V> type;

        Entry<K, V> lastEntry;

        int lastPosition;

        boolean canRemove;

        HashIterator(TMapEntry.Type<E, K, V> value) {
            type = value;
            position = lastSlot;
            expectedModCount = modCount;
        }

        @Override
        public boolean hasNext() {
            if (lastEntry != null && lastEntry.next != null) {
                return true;
            }
            while (position >= firstSlot) {
                if (elementData[position] == null) {
                    position--;
                } else {
                    return true;
                }
            }
            return false;
        }

        @Override
        public E next() {
            if (expectedModCount == modCount) {
                if (lastEntry != null) {
                    lastEntry = lastEntry.next;
                }
                if (lastEntry == null) {
                    while (position >= firstSlot) {
                        lastEntry = elementData[position];
                        if (lastEntry != null) {
                            break;
                        }
                        position--;
                    }
                    if (lastEntry != null) {
                        lastPosition = position;
                        // decrement the position so we don't find the same slot
                        // next time
                        position--;
                    }
                }
                if (lastEntry != null) {
                    canRemove = true;
                    return type.get(lastEntry);
                }
                throw new TNoSuchElementException();
            }
            throw new TConcurrentModificationException();
        }

        @Override
        public void remove() {
            if (expectedModCount == modCount) {
                if (canRemove) {
                    canRemove = false;
                    synchronized (THashtable.this) {
                        boolean removed = false;
                        Entry<K, V> entry = elementData[lastPosition];
                        if (entry == lastEntry) {
                            elementData[lastPosition] = entry.next;
                            removed = true;
                        } else {
                            while (entry != null && entry.next != lastEntry) {
                                entry = entry.next;
                            }
                            if (entry != null) {
                                entry.next = lastEntry.next;
                                removed = true;
                            }
                        }
                        if (removed) {
                            modCount++;
                            elementCount--;
                            expectedModCount++;
                            return;
                        }
                        // the entry must have been (re)moved outside of the
                        // iterator
                        // but this condition wasn't caught by the modCount
                        // check
                        // throw ConcurrentModificationException() outside of
                        // synchronized block
                    }
                } else {
                    throw new IllegalStateException();
                }
            }
            throw new TConcurrentModificationException();
        }
    }

    public THashtable() {
        this(11);
    }

    public THashtable(int capacity) {
        if (capacity >= 0) {
            elementCount = 0;
            elementData = newElementArray(capacity == 0 ? 1 : capacity);
            firstSlot = elementData.length;
            loadFactor = 0.75f;
            computeMaxSize();
        } else {
            throw new IllegalArgumentException();
        }
    }

    public THashtable(int capacity, float loadFactor) {
        if (capacity >= 0 && loadFactor > 0) {
            elementCount = 0;
            firstSlot = capacity;
            elementData = newElementArray(capacity == 0 ? 1 : capacity);
            this.loadFactor = loadFactor;
            computeMaxSize();
        } else {
            throw new IllegalArgumentException();
        }
    }

    public THashtable(TMap<? extends K, ? extends V> map) {
        this(map.size() < 6 ? 11 : (map.size() * 4 / 3) + 11);
        putAll(map);
    }

    @SuppressWarnings("unchecked")
    private Entry<K, V>[] newElementArray(int size) {
        return new Entry[size];
    }

    @Override
    public synchronized void clear() {
        elementCount = 0;
        Arrays.fill(elementData, null);
        modCount++;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized Object clone() {
        try {
            THashtable<K, V> hashtable = (THashtable<K, V>) super.clone();
            hashtable.elementData = new Entry[elementData.length];
            Entry<K, V> entry;
            for (int i = elementData.length; --i >= 0;) {
                entry = elementData[i];
                if (entry != null) {
                    hashtable.elementData[i] = (Entry<K, V>) entry.clone();
                }
            }
            return hashtable;
        } catch (TCloneNotSupportedException e) {
            return null;
        }
    }

    private void computeMaxSize() {
        threshold = (int) (elementData.length * loadFactor);
    }

    public synchronized boolean contains(Object value) {
        if (value == null) {
            throw new TNullPointerException();
        }

        for (int i = elementData.length; --i >= 0;) {
            Entry<K, V> entry = elementData[i];
            while (entry != null) {
                if (entry.value.equals(value)) {
                    return true;
                }
                entry = entry.next;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        return contains(value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized TEnumeration<V> elements() {
        if (elementCount == 0) {
            return (TEnumeration<V>) EMPTY_ENUMERATION;
        }
        return new HashEnumIterator<>(entry -> entry.value, true);
    }

    @Override
    public TSet<TMap.Entry<K, V>> entrySet() {
       return new TAbstractSet<TMap.Entry<K, V>>() {
            @Override
            public int size() {
                return elementCount;
            }

            @Override
            public void clear() {
                THashtable.this.clear();
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean remove(Object object) {
                if (contains(object)) {
                    THashtable.this.remove(((TMap.Entry<K, V>) object)
                            .getKey());
                    return true;
                }
                return false;
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean contains(Object object) {
                Entry<K, V> entry = getEntry(((TMap.Entry<K, V>) object)
                        .getKey());
                return object.equals(entry);
            }

            @Override
            public TIterator<TMap.Entry<K, V>> iterator() {
                return new HashIterator<>(entry -> entry);
            }
       };
    }

    @Override
    public synchronized boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof TMap) {
            TMap<?, ?> map = (TMap<?, ?>) object;
            if (size() != map.size()) {
                return false;
            }

            TSet<TMap.Entry<K, V>> entries = entrySet();
            for (TIterator<? extends TMap.Entry<?, ?>> iter = map.entrySet().iterator(); iter.hasNext();) {
                TMap.Entry<?, ?> e = iter.next();
                if (!entries.contains(e)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public synchronized V get(Object key) {
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % elementData.length;
        Entry<K, V> entry = elementData[index];
        while (entry != null) {
            if (entry.equalsKey(key, hash)) {
                return entry.value;
            }
            entry = entry.next;
        }
        return null;
    }

    Entry<K, V> getEntry(Object key) {
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % elementData.length;
        Entry<K, V> entry = elementData[index];
        while (entry != null) {
            if (entry.equalsKey(key, hash)) {
                return entry;
            }
            entry = entry.next;
        }
        return null;
    }

    @Override
    public synchronized int hashCode() {
        int result = 0;
        TIterator<TMap.Entry<K, V>> it = entrySet().iterator();
        while (it.hasNext()) {
            TMap.Entry<K, V> entry = it.next();
            Object key = entry.getKey();
            if (key == this) {
                continue;
            }
            Object value = entry.getValue();
            if (value == this) {
                continue;
            }
            int hash = (key != null ? key.hashCode() : 0)
                    ^ (value != null ? value.hashCode() : 0);
            result += hash;
        }
        return result;
    }

    @Override
    public synchronized boolean isEmpty() {
        return elementCount == 0;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized TEnumeration<K> keys() {
        if (elementCount == 0) {
            return (TEnumeration<K>) EMPTY_ENUMERATION;
        }
        return new HashEnumIterator<>(entry -> entry.key, true);
    }

    @Override
    public TSet<K> keySet() {
        return new TAbstractSet<K>() {
            @Override
            public boolean contains(Object object) {
                return containsKey(object);
            }

            @Override
            public int size() {
                return elementCount;
            }

            @Override
            public void clear() {
                THashtable.this.clear();
            }

            @Override
            public boolean remove(Object key) {
                if (containsKey(key)) {
                    THashtable.this.remove(key);
                    return true;
                }
                return false;
            }

            @SuppressWarnings("unchecked")
            @Override
            public TIterator<K> iterator() {
                if (this.size() == 0) {
                    return (TIterator<K>) EMPTY_ITERATOR;
                }
                return new HashEnumIterator<>(entry -> entry.key);
            }
        };
    }

    class HashEnumIterator<E> extends HashIterator<E> implements TEnumeration<E> {

        private boolean isEnumeration;

        int start;

        Entry<K, V> entry;

        HashEnumIterator(TMapEntry.Type<E, K, V> value) {
            super(value);
        }

        HashEnumIterator(TMapEntry.Type<E, K, V> value, boolean isEnumeration) {
            super(value);
            this.isEnumeration = isEnumeration;
            start = lastSlot + 1;
        }

        @Override
        public boolean hasMoreElements() {
            if (isEnumeration) {
                if (entry != null) {
                    return true;
                }
                while (start > firstSlot) {
                    if (elementData[--start] != null) {
                        entry = elementData[start];
                        return true;
                    }
                }
                return false;
            }
            // iterator
            return super.hasNext();
        }

        @Override
        public boolean hasNext() {
            if (isEnumeration) {
                return hasMoreElements();
            }
            // iterator
            return super.hasNext();
        }

        @Override
        public E next() {
            if (isEnumeration) {
                if (expectedModCount == modCount) {
                    return nextElement();
                } else {
                    throw new TConcurrentModificationException();
                }
            }
            // iterator
            return super.next();
        }

        @Override
        @SuppressWarnings("unchecked")
        public E nextElement() {
            if (isEnumeration) {
                if (hasMoreElements()) {
                    Object result = type.get(entry);
                    entry = entry.next;
                    return (E) result;
                }
                throw new TNoSuchElementException();
            }
            // iterator
            return super.next();
        }

        @Override
        public void remove() {
            if (isEnumeration) {
                throw new TUnsupportedOperationException();
            } else {
                super.remove();
            }
        }
    }

    @Override
    public synchronized V put(K key, V value) {
        if (key != null && value != null) {
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % elementData.length;
            Entry<K, V> entry = elementData[index];
            while (entry != null && !entry.equalsKey(key, hash)) {
                entry = entry.next;
            }
            if (entry == null) {
                modCount++;
                if (++elementCount > threshold) {
                    rehash();
                    index = (hash & 0x7FFFFFFF) % elementData.length;
                }
                if (index < firstSlot) {
                    firstSlot = index;
                }
                if (index > lastSlot) {
                    lastSlot = index;
                }
                entry = newEntry(key, value, hash);
                entry.next = elementData[index];
                elementData[index] = entry;
                return null;
            }
            V result = entry.value;
            entry.value = value;
            return result;
        }
        throw new NullPointerException();
    }

    @Override
    public synchronized void putAll(TMap<? extends K, ? extends V> map) {
        for (TIterator<? extends TMap.Entry<? extends K, ? extends V>> iter = map.entrySet().iterator();
                iter.hasNext();) {
            TMap.Entry<? extends K, ? extends V> entry = iter.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    protected void rehash() {
        int length = (elementData.length << 1) + 1;
        if (length == 0) {
            length = 1;
        }
        int newFirst = length;
        int newLast = -1;
        Entry<K, V>[] newData = newElementArray(length);
        for (int i = lastSlot + 1; --i >= firstSlot;) {
            Entry<K, V> entry = elementData[i];
            while (entry != null) {
                int index = (entry.getKeyHash() & 0x7FFFFFFF) % length;
                if (index < newFirst) {
                    newFirst = index;
                }
                if (index > newLast) {
                    newLast = index;
                }
                Entry<K, V> next = entry.next;
                entry.next = newData[index];
                newData[index] = entry;
                entry = next;
            }
        }
        firstSlot = newFirst;
        lastSlot = newLast;
        elementData = newData;
        computeMaxSize();
    }

    @Override
    public synchronized V remove(Object key) {
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % elementData.length;
        Entry<K, V> last = null;
        Entry<K, V> entry = elementData[index];
        while (entry != null && !entry.equalsKey(key, hash)) {
            last = entry;
            entry = entry.next;
        }
        if (entry != null) {
            modCount++;
            if (last == null) {
                elementData[index] = entry.next;
            } else {
                last.next = entry.next;
            }
            elementCount--;
            V result = entry.value;
            entry.value = null;
            return result;
        }
        return null;
    }

    @Override
    public synchronized int size() {
        return elementCount;
    }

    @Override
    public synchronized String toString() {
        if (isEmpty()) {
            return "{}";
        }

        StringBuilder buffer = new StringBuilder(size() * 28);
        buffer.append('{');
        for (int i = lastSlot; i >= firstSlot; i--) {
            Entry<K, V> entry = elementData[i];
            while (entry != null) {
                if (entry.key != this) {
                    buffer.append(entry.key);
                } else {
                    buffer.append("(this Map)");
                }
                buffer.append('=');
                if (entry.value != this) {
                    buffer.append(entry.value);
                } else {
                    buffer.append("(this Map)");
                }
                buffer.append(", "); //$NON-NLS-1$
                entry = entry.next;
            }
        }
        // Remove the last ", "
        if (elementCount > 0) {
            buffer.setLength(buffer.length() - 2);
        }
        buffer.append('}');
        return buffer.toString();
    }

    @Override
    public TCollection<V> values() {
        return new TAbstractCollection<V>() {
            @Override
            public boolean contains(Object object) {
                return THashtable.this.contains(object);
            }

            @Override
            public int size() {
                return elementCount;
            }

            @Override
            public void clear() {
                THashtable.this.clear();
            }

            @Override
            public TIterator<V> iterator() {
                return new HashIterator<>(entry -> entry.value);
            }
        };
    }
}
