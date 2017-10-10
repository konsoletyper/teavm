/*
 *  Copyright 2017 Alexey Andreev.
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

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public class TEnumMap<K extends Enum<K>, V> extends AbstractMap<K, V> implements Serializable, Cloneable {
    private Class<K> keyType;
    private Object[] data;
    private boolean[] provided;
    private int size;
    private Set<Entry<K, V>> entrySet;

    public TEnumMap(Class<K> keyType) {
        initFromKeyType(keyType);
    }

    public TEnumMap(TEnumMap<K, V> m) {
        initFromOtherEnumMap(m);
    }

    public TEnumMap(Map<K, V> m) {
        if (m instanceof TEnumMap) {
            initFromOtherEnumMap((TEnumMap<K, V>) m);
        } else {
            if (m.isEmpty()) {
                throw new IllegalArgumentException();
            }
            initFromKeyType(m.keySet().iterator().next().getDeclaringClass());
            for (Entry<K, V> entry : m.entrySet()) {
                int index = entry.getKey().ordinal();
                provided[index] = true;
                data[index] = entry.getValue();
            }
            size = m.size();
        }
    }

    private void initFromKeyType(Class<K> keyType) {
        this.keyType = keyType;
        this.data = new Object[TGenericEnumSet.getConstants(keyType).length];
        this.provided = new boolean[data.length];
    }

    private void initFromOtherEnumMap(TEnumMap<K, V> m) {
        this.keyType = m.keyType;
        this.data = Arrays.copyOf(m.data, m.data.length);
        this.provided = Arrays.copyOf(m.provided, m.provided.length);
        this.size = m.size;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean containsValue(Object value) {
        for (int i = 0; i < data.length; ++i) {
            if (provided[i] && Objects.equals(value, data[i])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        if (!keyType.isInstance(key)) {
            return false;
        }
        int index = ((Enum<?>) key).ordinal();
        return provided[index];
    }

    @Override
    public V get(Object key) {
        if (!keyType.isInstance(key)) {
            return null;
        }
        int index = ((Enum<?>) key).ordinal();
        @SuppressWarnings("unchecked")
        V value = (V) data[index];
        return value;
    }

    @Override
    public V put(K key, V value) {
        int index = key.ordinal();
        @SuppressWarnings("unchecked")
        V old = (V) data[index];
        if (!provided[index]) {
            provided[index] = true;
            size++;
        }
        data[index] = value;
        return old;
    }

    @Override
    public V remove(Object key) {
        if (!keyType.isInstance(key)) {
            return null;
        }
        int index = ((Enum<?>) key).ordinal();
        @SuppressWarnings("unchecked")
        V old = (V) data[index];
        if (provided[index]) {
            provided[index] = false;
            data[index] = null;
            size--;
        }
        return old;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            int index = entry.getKey().ordinal();
            if (!provided[index]) {
                provided[index] = true;
                size++;
            }
            data[index] = entry.getValue();
        }
    }

    @Override
    public void clear() {
        if (size > 0) {
            size = 0;
            Arrays.fill(provided, false);
            Arrays.fill(data, null);
        }
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        if (entrySet == null) {
            entrySet = new AbstractSet<Entry<K, V>>() {
                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return new Iterator<Entry<K, V>>() {
                        int index;
                        int removeIndex = -1;

                        {
                            find();
                        }

                        @Override
                        public boolean hasNext() {
                            return index < data.length;
                        }

                        @Override
                        public Entry<K, V> next() {
                            if (index >= data.length) {
                                throw new NoSuchElementException();
                            }
                            removeIndex = index;
                            EntryImpl result = new EntryImpl(index++);
                            find();
                            return result;
                        }

                        private void find() {
                            while (index < provided.length && !provided[index]) {
                                index++;
                            }
                        }

                        @Override
                        public void remove() {
                            if (removeIndex < 0) {
                                throw new IllegalStateException();
                            }
                            data[removeIndex] = null;
                            provided[removeIndex] = false;
                            size--;
                            removeIndex = -1;
                        }
                    };
                }

                @Override
                public int size() {
                    return size;
                }

                @Override
                public boolean remove(Object o) {
                    if (!keyType.isInstance(o)) {
                        return false;
                    }
                    int index = ((Enum<?>) o).ordinal();
                    if (provided[index]) {
                        provided[index] = true;
                        data[index] = null;
                        size--;
                        return true;
                    } else {
                        return false;
                    }
                }

                @Override
                public void clear() {
                    TEnumMap.this.clear();
                }

                class EntryImpl implements Entry<K, V> {
                    int index;

                    EntryImpl(int index) {
                        this.index = index;
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public K getKey() {
                        return (K) TGenericEnumSet.getConstants(keyType)[index];
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public V getValue() {
                        return (V) data[index];
                    }

                    @Override
                    public V setValue(V value) {
                        @SuppressWarnings("unchecked")
                        V old = (V) data[index];
                        data[index] = value;
                        return old;
                    }
                }
            };
        }
        return entrySet;
    }
}
