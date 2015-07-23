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

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TCloneNotSupportedException;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.lang.TUnsupportedOperationException;

/**
 *
 * @author Alexey Andreev
 * @param <K>
 * @param <V>
 */
public abstract class TAbstractMap<K, V> extends TObject implements TMap<K, V> {
    public static class SimpleEntry<K, V> implements TMap.Entry<K, V>, TSerializable {
        private K key;
        private V value;

        public SimpleEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public SimpleEntry(TMap.Entry<? extends K, ? extends V> entry) {
            this(entry.getKey(), entry.getValue());
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V old = this.value;
            this.value = value;
            return old;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TMap.Entry)) {
                return false;
            }
            TMap.Entry<?, ?> other = (TMap.Entry<?, ?>) obj;
            if (getKey() == null ? other.getKey() != null : !getKey().equals(other.getKey())) {
                return false;
            }
            return getValue() == null ? other.getValue() == null : getValue().equals(other.getValue());
        }

        @Override
        public int hashCode() {
            return (getKey() == null ? 0 : getKey().hashCode()) ^ (getValue() == null ? 0 : getValue().hashCode());
        }

        @Override
        public String toString() {
            return String.valueOf(getKey()) + "=" + String.valueOf(getValue());
        }
    }

    public static class SimpleImmutableEntry<K, V> implements TMap.Entry<K, V>, TSerializable {
        private K key;
        private V value;

        public SimpleImmutableEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public SimpleImmutableEntry(TMap.Entry<? extends K, ? extends V> entry) {
            this(entry.getKey(), entry.getValue());
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            throw new TUnsupportedOperationException();
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof TMap.Entry)) {
                return false;
            }
            TMap.Entry<?, ?> other = (TMap.Entry<?, ?>) obj;
            if (getKey() == null ? other.getKey() != null : !getKey().equals(other.getKey())) {
                return false;
            }
            return getValue() == null ? other.getValue() == null : getValue().equals(other.getValue());
        }

        @Override
        public int hashCode() {
            return (getKey() == null ? 0 : getKey().hashCode()) ^ (getValue() == null ? 0 : getValue().hashCode());
        }

        @Override
        public String toString() {
            return String.valueOf(getKey()) + "=" + String.valueOf(getValue());
        }
    }

    TSet<K> cachedKeySet;
    TCollection<V> cachedValues;

    protected TAbstractMap() {
    }

    @Override
    public int size() {
        return entrySet().size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsValue(Object value) {
        for (TIterator<TMap.Entry<K, V>> iter = entrySet().iterator(); iter.hasNext();) {
            V knownValue = iter.next().getValue();
            if (TObjects.equals(value, knownValue)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        for (TIterator<TMap.Entry<K, V>> iter = entrySet().iterator(); iter.hasNext();) {
            K knownKey = iter.next().getKey();
            if (TObjects.equals(key, knownKey)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        for (TIterator<TMap.Entry<K, V>> iter = entrySet().iterator(); iter.hasNext();) {
            TMap.Entry<K, V> entry = iter.next();
            if (TObjects.equals(key, entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public void putAll(TMap<? extends K, ? extends V> m) {
        for (TIterator<? extends TMap.Entry<? extends K, ? extends V>> iter = m.entrySet().iterator();
                iter.hasNext();) {
            TMap.Entry<? extends K, ? extends V> entry = iter.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V remove(Object key) {
        for (TIterator<TMap.Entry<K, V>> iter = entrySet().iterator(); iter.hasNext();) {
            TMap.Entry<K, V> entry = iter.next();
            if (TObjects.equals(key, entry.getKey())) {
                iter.remove();
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public abstract TSet<TMap.Entry<K, V>> entrySet();

    @Override
    public void clear() {
        entrySet().clear();
    }

    @Override
    public TSet<K> keySet() {
        if (cachedKeySet == null) {
            cachedKeySet = new KeySet();
        }
        return cachedKeySet;
    }

    @Override
    public TCollection<V> values() {
        if (cachedValues == null) {
            cachedValues = new Values();
        }
        return cachedValues;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TMap)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        TMap<Object, Object> other = (TMap<Object, Object>) obj;
        if (size() != other.size()) {
            return false;
        }
        for (TIterator<? extends TMap.Entry<K, V>> iter = entrySet().iterator(); iter.hasNext();) {
            TMap.Entry<K, V> entry = iter.next();
            if (!other.containsKey(entry.getKey())) {
                return false;
            }
            if (!TObjects.equals(entry.getValue(), other.get(entry.getKey()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 0;
        for (TIterator<? extends TMap.Entry<K, V>> iter = entrySet().iterator(); iter.hasNext();) {
            TMap.Entry<K, V> entry = iter.next();
            result ^= entry.hashCode();
        }
        return result;
    }

    @Override
    protected Object clone() throws TCloneNotSupportedException {
        TAbstractMap<?, ?> copy = (TAbstractMap<?, ?>) super.clone();
        copy.cachedKeySet = null;
        copy.cachedValues = null;
        return copy;
    }

    private class KeySet extends TAbstractSet<K> {
        @Override
        public TIterator<K> iterator() {
            final TIterator<TMap.Entry<K, V>> iter = TAbstractMap.this.entrySet().iterator();
            return new TIterator<K>() {
                @Override public boolean hasNext() {
                    return iter.hasNext();
                }
                @Override public K next() {
                    return iter.next().getKey();
                }
                @Override public void remove() {
                    iter.remove();
                }
            };
        }

        @Override
        public int size() {
            return TAbstractMap.this.size();
        }
    }

    private class Values extends TAbstractCollection<V> {
        @Override
        public int size() {
            return TAbstractMap.this.size();
        }

        @Override
        public TIterator<V> iterator() {
            final TIterator<TMap.Entry<K, V>> iter = TAbstractMap.this.entrySet().iterator();
            return new TIterator<V>() {
                @Override public boolean hasNext() {
                    return iter.hasNext();
                }
                @Override public V next() {
                    return iter.next().getValue();
                }
                @Override public void remove() {
                    iter.remove();
                }
            };
        }
    }
}
