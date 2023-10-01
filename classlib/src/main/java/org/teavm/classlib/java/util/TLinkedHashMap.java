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
package org.teavm.classlib.java.util;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class TLinkedHashMap<K, V> extends THashMap<K, V> implements TSequencedMap<K, V> {
    private boolean accessOrder;

    transient LinkedHashMapEntry<K, V> head;
    transient LinkedHashMapEntry<K, V> tail;

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

    @Override
    void putAllImpl(TMap<? extends K, ? extends V> map) {
        int capacity = elementCount + map.size();
        if (capacity > threshold) {
            rehash(capacity);
        }
        for (var it = map.entrySet().iterator(); it.hasNext();) {
            TMap.Entry<? extends K, ? extends V> entry = it.next();
            putImpl(entry.getKey(), entry.getValue(), false);
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
    public V getOrDefault(Object key, V defaultValue) {
        LinkedHashMapEntry<K, V> m;
        if (key == null) {
            m = (LinkedHashMapEntry<K, V>) findNullKeyEntry();
        } else {
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % elementData.length;
            m = (LinkedHashMapEntry<K, V>) findNonNullKeyEntry(key, index, hash);
        }
        if (m == null) {
            return defaultValue;
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
    public V get(Object key) {
        return getOrDefault(key, null);
    }

    private HashEntry<K, V> createHashedEntry(K key, int index, int hash, boolean first) {
        LinkedHashMapEntry<K, V> m = new LinkedHashMapEntry<>(key, hash);
        m.next = elementData[index];
        elementData[index] = m;
        linkEntry(m, first);
        return m;
    }

    @Override
    public V put(K key, V value) {
        return putImpl(key, value, false);
    }

    V putImpl(K key, V value, boolean first) {
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
                m = (LinkedHashMapEntry<K, V>) createHashedEntry(null, 0, 0, first);
            } else {
                linkEntry(m, first);
            }
        } else {
            int hash = key.hashCode();
            int index = (hash & Integer.MAX_VALUE) % elementData.length;
            m = (LinkedHashMapEntry<K, V>) findNonNullKeyEntry(key, index, hash);
            if (m == null) {
                modCount++;
                if (++elementCount > threshold) {
                    rehash();
                    index = (hash & Integer.MAX_VALUE) % elementData.length;
                }
                m = (LinkedHashMapEntry<K, V>) createHashedEntry(key, index, hash, first);
            } else {
                linkEntry(m, first);
            }
        }

        V result = m.value;
        m.value = value;

        if (removeEldestEntry(head)) {
            remove(head.key);
        }

        return result;
    }

    void linkEntry(LinkedHashMapEntry<K, V> m, boolean first) {
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
                // Existing entry must be the head but not the tail
                if (!first && accessOrder) {
                    head = n;
                    n.chainBackward = null;
                    m.chainBackward = tail;
                    m.chainForward = null;
                    tail.chainForward = m;
                    tail = m;
                }
            } else {
                // This is a new entry
                m.chainBackward = first ? null : tail;
                m.chainForward = first ? head : null;
                if (first) {
                    head.chainBackward = m;
                    head = m;
                } else {
                    tail.chainForward = m;
                    tail = m;
                }
            }
        } else {
            if (n == null) {
                // Existing entry must be the tail but not the head
                if (first && accessOrder) {
                    tail = p;
                    p.chainForward = null;
                    m.chainBackward = null;
                    m.chainForward = head;
                    head.chainBackward = m;
                    head = m;
                }
            } else {
                if (elementCount > 1 && accessOrder) {
                    // Existing entry is neither the head nor tail
                    p.chainForward = n;
                    n.chainBackward = p;
                    if (first) {
                        m.chainForward = head;
                        m.chainBackward = null;
                        head.chainBackward = m;
                        head = m;
                    } else {
                        m.chainForward = null;
                        m.chainBackward = tail;
                        tail.chainForward = m;
                        tail = m;
                    }
                }
            }
        }
    }

    @Override
    public TSet<Entry<K, V>> entrySet() {
        return new TReversedLinkedHashMap.LinkedHashMapEntrySet<>(this, false);
    }

    @Override
    public TSet<K> keySet() {
        return sequencedKeySet();
    }

    @Override
    public TSequencedSet<K> sequencedKeySet() {
        if (cachedKeySet == null) {
            cachedKeySet = new TReversedLinkedHashMap.LinkedHashMapKeySet<>(this, false);
        }
        return (TSequencedSet<K>) cachedKeySet;
    }

    @Override
    public TCollection<V> values() {
        return sequencedValues();
    }

    @Override
    public TSequencedCollection<V> sequencedValues() {
        if (cachedValues == null) {
            cachedValues = new TReversedLinkedHashMap.LinkedHashMapValues<>(this, false);
        }
        return (TSequencedCollection<V>) cachedValues;
    }

    @Override
    public TSequencedSet<Entry<K, V>> sequencedEntrySet() {
        return new TReversedLinkedHashMap.LinkedHashMapEntrySet<>(this, false);
    }

    @Override
    public V remove(Object key) {
        LinkedHashMapEntry<K, V> m = (LinkedHashMapEntry<K, V>) removeByKey(key);
        if (m == null) {
            return null;
        }
        LinkedHashMapEntry<K, V> p = m.chainBackward;
        LinkedHashMapEntry<K, V> n = m.chainForward;
        if (p != null) {
            p.chainForward = n;
            if (n != null) {
                n.chainBackward = p;
            } else {
                tail = p;
            }
        } else {
            head = n;
            if (n != null) {
                n.chainBackward = null;
            } else {
                tail = null;
            }
        }
        return m.value;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (elementCount > 0) {
            int prevModCount = modCount;
            LinkedHashMapEntry<K, V> entry = head;
            do {
                action.accept(entry.key, entry.value);
                entry = entry.chainForward;
                if (modCount != prevModCount) {
                    throw new TConcurrentModificationException();
                }
            } while (entry != null);
        }
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

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (elementCount > 0) {
            int prevModCount = modCount;
            TLinkedHashMap.LinkedHashMapEntry<K, V> entry = head;
            do {
                entry.value = function.apply(entry.key, entry.value);
                entry = entry.chainForward;
                if (modCount != prevModCount) {
                    throw new TConcurrentModificationException();
                }
            } while (entry != null);
        }
    }

    @Override
    public V putFirst(K k, V v) {
        return putImpl(k, v, true);
    }

    @Override
    public V putLast(K k, V v) {
        return putImpl(k, v, false);
    }

    @Override
    public TSequencedMap<K, V> reversed() {
        return new TReversedLinkedHashMap<>(this);
    }
}
