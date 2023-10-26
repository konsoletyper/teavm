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
import java.util.Arrays;
import org.teavm.classlib.java.lang.TCloneNotSupportedException;
import org.teavm.interop.Rename;

public class TEnumMap<K extends Enum<K>, V> extends TAbstractMap<K, V> implements Serializable, Cloneable {
    private Class<K> keyType;
    private Object[] data;
    private boolean[] provided;
    private int size;
    private TSet<Entry<K, V>> entrySet;

    public TEnumMap(Class<K> keyType) {
        initFromKeyType(keyType);
    }

    public TEnumMap(TEnumMap<K, V> m) {
        initFromOtherEnumMap(m);
    }

    public TEnumMap(TMap<K, V> m) {
        if (m instanceof TEnumMap) {
            initFromOtherEnumMap((TEnumMap<K, V>) m);
        } else {
            if (m.isEmpty()) {
                throw new IllegalArgumentException();
            }
            for (TIterator<? extends TMap.Entry<K, V>> it = m.entrySet().iterator(); it.hasNext();) {
                TMap.Entry<K, V> entry = it.next();
                K key = entry.getKey();
                if (keyType == null) {
                    initFromKeyType(key.getDeclaringClass());
                }
                Class<?> cls = key.getClass();
                if (cls != keyType && cls.getSuperclass() != keyType) {
                    throw new ClassCastException();
                }
                int index = key.ordinal();
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
            if (provided[i] && TObjects.equals(value, data[i])) {
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
        Class<?> cls = key.getClass();
        if (cls != keyType && cls.getSuperclass() != keyType) {
            throw new ClassCastException();
        }
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
    @SuppressWarnings("unchecked")
    public void putAll(TMap<? extends K, ? extends V> m) {
        if (m instanceof TEnumMap) {
            TEnumMap<K, V> em = (TEnumMap<K, V>) m;
            if (!em.isEmpty() && this.keyType != em.keyType) {
                throw new ClassCastException(em.keyType + " != " + keyType);
            }
            for (int i = 0; i < data.length; i++) {
                if (em.provided[i]) {
                    this.data[i] = em.data[i];
                    if (!this.provided[i]) {
                        this.provided[i] = true;
                        size++;
                    }
                }
            }
        } else {
            super.putAll(m);
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

    @Rename("clone")
    @SuppressWarnings("unchecked")
    public TEnumMap<K, V> clone0() {
        try {
            TEnumMap<K, V> map = (TEnumMap<K, V>) super.clone();
            map.keyType = this.keyType;
            map.provided = this.provided.clone();
            map.data = this.data.clone();
            map.size = this.size;

            return map;
        } catch (TCloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public TSet<Entry<K, V>> entrySet() {
        if (entrySet == null) {
            entrySet = new TAbstractSet<>() {
                @Override
                public TIterator<Entry<K, V>> iterator() {
                    return new TIterator<>() {
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
                                throw new TNoSuchElementException();
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
                            if (provided[removeIndex]) {
                                data[removeIndex] = null;
                                provided[removeIndex] = false;
                                size--;
                            }
                            removeIndex = -1;
                        }
                    };
                }

                @Override
                public int size() {
                    return size;
                }

                @Override
                public boolean contains(Object o) {
                    if (!(o instanceof TMap.Entry<?, ?>)) {
                        return false;
                    }
                    TMap.Entry<?, ?> e = (TMap.Entry<?, ?>) o;
                    Class<?> cls = e.getKey().getClass();
                    if (cls != keyType && cls.getSuperclass() != keyType) {
                        return false;
                    }
                    int index = ((Enum<?>) e.getKey()).ordinal();
                    return provided[index] && TObjects.equals(data[index], e.getValue());
                }

                @Override
                public boolean remove(Object o) {
                    if (!(o instanceof TMap.Entry<?, ?>)) {
                        return false;
                    }
                    TMap.Entry<?, ?> e = (TMap.Entry<?, ?>) o;

                    Class<?> cls = e.getKey().getClass();
                    if (cls != keyType && cls.getSuperclass() != keyType) {
                        return false;
                    }
                    int index = ((Enum<?>) e.getKey()).ordinal();
                    if (provided[index] && TObjects.equals(e.getValue(), data[index])) {
                        provided[index] = false;
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

                    @SuppressWarnings("unchecked")
                    private K key() {
                        return (K) TGenericEnumSet.getConstants(keyType)[index];
                    }

                    @SuppressWarnings("unchecked")
                    private V value() {
                        return (V) data[index];
                    }

                    @Override
                    public K getKey() {
                        if (!provided[index]) {
                            throw new IllegalStateException();
                        }
                        return key();
                    }

                    @Override
                    public V getValue() {
                        if (!provided[index]) {
                            throw new IllegalStateException();
                        }
                        return value();
                    }

                    @Override
                    public V setValue(V value) {
                        if (!provided[index]) {
                            throw new IllegalStateException();
                        }
                        @SuppressWarnings("unchecked")
                        V old = (V) data[index];
                        data[index] = value;
                        return old;
                    }

                    @Override
                    public boolean equals(Object obj) {
                        if (this == obj) {
                            return true;
                        }
                        if (obj instanceof TMap.Entry) {
                            TMap.Entry<?, ?> entry = (TMap.Entry<?, ?>) obj;
                            return TObjects.equals(key(), entry.getKey()) && TObjects.equals(value(), entry.getValue());
                        }
                        return false;
                    }

                    @Override
                    public int hashCode() {
                        return TObjects.hashCode(key()) ^ TObjects.hashCode(value());
                    }

                    @Override
                    public String toString() {
                        return key() + "=" + value();
                    }
                }
            };
        }
        return entrySet;
    }

    @Override
    public TCollection<V> values() {
        if (cachedValues == null) {
            cachedValues = new TAbstractCollection<>() {
                @Override
                public int size() {
                    return size;
                }

                @Override
                public boolean contains(Object o) {
                    return containsValue(o);
                }

                @Override
                public boolean remove(Object o) {
                    for (int i = 0; i < data.length; i++) {
                        if (provided[i] && TObjects.equals(o, data[i])) {
                            data[i] = null;
                            provided[i] = false;
                            size--;
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public void clear() {
                    TEnumMap.this.clear();
                }

                @Override
                public TIterator<V> iterator() {
                    final TIterator<TMap.Entry<K, V>> it = entrySet().iterator();
                    return new TIterator<>() {
                        @Override public boolean hasNext() {
                            return it.hasNext();
                        }
                        @Override public V next() {
                            return it.next().getValue();
                        }
                        @Override public void remove() {
                            it.remove();
                        }
                    };
                }
            };
        }
        return cachedValues;
    }
}
