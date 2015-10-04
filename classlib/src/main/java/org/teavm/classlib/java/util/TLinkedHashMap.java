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

import org.teavm.classlib.java.lang.TIllegalStateException;

public class TLinkedHashMap<K, V> extends THashMap<K, V> implements TMap<K, V> {
    private final boolean accessOrder;

    transient private LinkedHashMapEntry<K, V> head;
    transient private LinkedHashMapEntry<K, V> tail;

    public TLinkedHashMap() {
        accessOrder = false;
        head = null;
    }

    public TLinkedHashMap(int s) {
        super(s);
        accessOrder = false;
        head = null;
    }

    public TLinkedHashMap(int s, float lf) {
        super(s, lf);
        accessOrder = false;
        head = null;
        tail = null;
    }

    public TLinkedHashMap(int s, float lf, boolean order) {
        super(s, lf);
        accessOrder = order;
        head = null;
        tail = null;
    }

    public TLinkedHashMap(TMap<? extends K, ? extends V> m) {
        accessOrder = false;
        head = null;
        tail = null;
        putAll(m);
    }

    private static class AbstractMapIterator<K, V>  {
        int expectedModCount;
        LinkedHashMapEntry<K, V>  futureEntry;
        LinkedHashMapEntry<K, V>  currentEntry;
        final TLinkedHashMap<K, V> associatedMap;

        AbstractMapIterator(TLinkedHashMap<K, V> map) {
            expectedModCount = map.modCount;
            futureEntry = map.head;
            associatedMap = map;
        }

        public boolean hasNext() {
            return futureEntry != null;
        }

        final void checkConcurrentMod() throws TConcurrentModificationException {
            if (expectedModCount != associatedMap.modCount) {
                throw new TConcurrentModificationException();
            }
        }

        final void makeNext() {
            checkConcurrentMod();
            if (!hasNext()) {
                throw new TNoSuchElementException();
            }
            currentEntry = futureEntry;
            futureEntry = futureEntry.chainForward;
        }

        public void remove() {
            checkConcurrentMod();
            if (currentEntry == null) {
                throw new TIllegalStateException();
            }
            associatedMap.removeEntry(currentEntry);
            LinkedHashMapEntry<K, V> lhme =  currentEntry;
            LinkedHashMapEntry<K, V> p = lhme.chainBackward;
            LinkedHashMapEntry<K, V> n = lhme.chainForward;
            TLinkedHashMap<K, V> lhm = associatedMap;
            if (p != null) {
                p.chainForward = n;
                if (n != null) {
                    n.chainBackward = p;
                } else {
                    lhm.tail = p;
                }
            } else {
                lhm.head = n;
                if (n != null) {
                    n.chainBackward = null;
                } else {
                    lhm.tail = null;
                }
            }
            currentEntry = null;
            expectedModCount++;
        }
    }

    private static class EntryIterator<K, V> extends AbstractMapIterator<K, V> implements TIterator<Entry<K, V>> {
        EntryIterator(TLinkedHashMap<K, V> map) {
            super(map);
        }

        @Override
        public Entry<K, V> next() {
            makeNext();
            return currentEntry;
        }
    }

    private static class KeyIterator<K, V> extends AbstractMapIterator<K, V> implements TIterator<K> {
        KeyIterator(TLinkedHashMap<K, V> map) {
            super(map);
        }

        @Override
        public K next() {
            makeNext();
            return currentEntry.key;
        }
    }

    private static class ValueIterator<K, V> extends AbstractMapIterator<K, V> implements TIterator<V> {
        ValueIterator(TLinkedHashMap<K, V> map) {
            super(map);
        }

        @Override
        public V next() {
            makeNext();
            return currentEntry.value;
        }
    }

    static final class LinkedHashMapEntrySet<K, V> extends HashMapEntrySet<K, V> {
        public LinkedHashMapEntrySet(TLinkedHashMap<K, V> lhm) {
            super(lhm);
        }

        @Override
        public TIterator<Entry<K, V>> iterator() {
            return new EntryIterator<>((TLinkedHashMap<K, V>) hashMap());
        }
    }

    static final class LinkedHashMapEntry<K, V> extends HashEntry<K, V> {
        LinkedHashMapEntry<K, V> chainForward;
        LinkedHashMapEntry<K, V> chainBackward;

        LinkedHashMapEntry(K theKey, V theValue) {
            super(theKey, theValue);
            chainForward = null;
            chainBackward = null;
        }

        LinkedHashMapEntry(K theKey, int hash) {
            super(theKey, hash);
            chainForward = null;
            chainBackward = null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object clone() {
            LinkedHashMapEntry<K, V> entry = (LinkedHashMapEntry<K, V>) super.clone();
            entry.chainBackward = chainBackward;
            entry.chainForward = chainForward;
            LinkedHashMapEntry<K, V> lnext = (LinkedHashMapEntry<K, V>) entry.next;
            if (lnext != null) {
                entry.next = (LinkedHashMapEntry<K, V>) lnext.clone();
            }
            return entry;
        }
    }

    @Override
    public boolean containsValue(Object value) {
        LinkedHashMapEntry<K, V> entry = head;
        if (null == value) {
            while (null != entry) {
                if (null == entry.value) {
                    return true;
                }
                entry = entry.chainForward;
            }
        } else {
            while (null != entry) {
                if (value.equals(entry.value)) {
                    return true;
                }
                entry = entry.chainForward;
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    HashEntry<K, V>[] newElementArray(int s) {
        return new LinkedHashMapEntry[s];
    }

    @Override
    public V get(Object key) {
        LinkedHashMapEntry<K, V> m;
        if (key == null) {
            m = (LinkedHashMapEntry<K, V>) findNullKeyEntry();
        } else {
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % elementData.length;
            m = (LinkedHashMapEntry<K, V>) findNonNullKeyEntry(key, index, hash);
        }
        if (m == null) {
            return null;
        }
        if (accessOrder && tail != m) {
            LinkedHashMapEntry<K, V> p = m.chainBackward;
            LinkedHashMapEntry<K, V> n = m.chainForward;
            n.chainBackward = p;
            if (p != null) {
                p.chainForward = n;
            } else {
                head = n;
            }
            m.chainForward = null;
            m.chainBackward = tail;
            tail.chainForward = m;
            tail = m;
        }
        return m.value;
    }

    @Override
    HashEntry<K, V> createEntry(K key, int index, V value) {
        LinkedHashMapEntry<K, V> m = new LinkedHashMapEntry<>(key, value);
        m.next = elementData[index];
        elementData[index] = m;
        linkEntry(m);
        return m;
    }

    @Override
    HashEntry<K, V> createHashedEntry(K key, int index, int hash) {
        LinkedHashMapEntry<K, V> m = new LinkedHashMapEntry<>(key, hash);
        m.next = elementData[index];
        elementData[index] = m;
        linkEntry(m);
        return m;
    }

    @Override
    public V put(K key, V value) {
        V result = putImpl(key, value);

        if (removeEldestEntry(head)) {
            remove(head.key);
        }

        return result;
    }

    @Override
    V putImpl(K key, V value) {
        LinkedHashMapEntry<K, V> m;
        if (elementCount == 0) {
            head = null;
            tail = null;
        }
        if (key == null) {
            m = (LinkedHashMapEntry<K, V>) findNullKeyEntry();
            if (m == null) {
                modCount++;
                // Check if we need to remove the oldest entry. The check
                // includes accessOrder since an accessOrder LinkedHashMap does
                // not record the oldest member in 'head'.
                if (++elementCount > threshold) {
                    rehash();
                }
                m = (LinkedHashMapEntry<K, V>) createHashedEntry(null, 0, 0);
            } else {
                linkEntry(m);
            }
        } else {
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % elementData.length;
            m = (LinkedHashMapEntry<K, V>) findNonNullKeyEntry(key, index, hash);
            if (m == null) {
                modCount++;
                if (++elementCount > threshold) {
                    rehash();
                    index = (hash & 0x7FFFFFFF) % elementData.length;
                }
                m = (LinkedHashMapEntry<K, V>) createHashedEntry(key, index, hash);
            } else {
                linkEntry(m);
            }
        }

        V result = m.value;
        m.value = value;
        return result;
    }

    void linkEntry(LinkedHashMapEntry<K, V> m) {
        if (tail == m) {
            return;
        }

        if (head == null) {
            // Check if the map is empty
            head = m;
            tail = m;
            return;
        }

        // we need to link the new entry into either the head or tail
        // of the chain depending on if the LinkedHashMap is accessOrder or not
        LinkedHashMapEntry<K, V> p = m.chainBackward;
        LinkedHashMapEntry<K, V> n = m.chainForward;
        if (p == null) {
            if (n != null) {
                // The entry must be the head but not the tail
                if (accessOrder) {
                    head = n;
                    n.chainBackward = null;
                    m.chainBackward = tail;
                    m.chainForward = null;
                    tail.chainForward = m;
                    tail = m;
                }
            } else {
                // This is a new entry
                m.chainBackward = tail;
                m.chainForward = null;
                tail.chainForward = m;
                tail = m;
            }
            return;
        }

        if (n == null) {
            // The entry must be the tail so we can't get here
            return;
        }

        // The entry is neither the head nor tail
        if (accessOrder) {
            p.chainForward = n;
            n.chainBackward = p;
            m.chainForward = null;
            m.chainBackward = tail;
            tail.chainForward = m;
            tail = m;
        }
    }

    @Override
    public TSet<Entry<K, V>> entrySet() {
        return new LinkedHashMapEntrySet<>(this);
    }

    @Override
    public TSet<K> keySet() {
        if (cachedKeySet == null) {
            cachedKeySet = new TAbstractSet<K>() {
                @Override
                public boolean contains(Object object) {
                    return containsKey(object);
                }

                @Override
                public int size() {
                    return TLinkedHashMap.this.size();
                }

                @Override
                public void clear() {
                    TLinkedHashMap.this.clear();
                }

                @Override
                public boolean remove(Object key) {
                    if (containsKey(key)) {
                        TLinkedHashMap.this.remove(key);
                        return true;
                    }
                    return false;
                }

                @Override
                public TIterator<K> iterator() {
                    return new KeyIterator<>(TLinkedHashMap.this);
                }
            };
        }
        return cachedKeySet;
    }

    @Override
    public TCollection<V> values() {
        if (cachedValues == null) {
            cachedValues = new TAbstractCollection<V>() {
                @Override
                public boolean contains(Object object) {
                    return containsValue(object);
                }

                @Override
                public int size() {
                    return TLinkedHashMap.this.size();
                }

                @Override
                public void clear() {
                    TLinkedHashMap.this.clear();
                }

                @Override
                public TIterator<V> iterator() {
                    return new ValueIterator<>(TLinkedHashMap.this);
                }
            };
        }
        return cachedValues;
    }

    @Override
    public V remove(Object key) {
        LinkedHashMapEntry<K, V> m = (LinkedHashMapEntry<K, V>) removeEntry(key);
        if (m == null) {
            return null;
        }
        LinkedHashMapEntry<K, V> p = m.chainBackward;
        LinkedHashMapEntry<K, V> n = m.chainForward;
        if (p != null) {
            p.chainForward = n;
        } else {
            head = n;
        }
        if (n != null) {
            n.chainBackward = p;
        } else {
            tail = p;
        }
        return m.value;
    }

    protected boolean removeEldestEntry(@SuppressWarnings("unused") Entry<K, V> eldest) {
        return false;
    }

    @Override
    public void clear() {
        super.clear();
        head = null;
        tail = null;
    }
}
