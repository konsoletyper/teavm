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

class TCheckedMap<K, V> implements TMap<K, V> {
    private TMap<K, V> innerMap;
    private Class<K> keyType;
    private Class<V> valueType;

    public TCheckedMap(TMap<K, V> innerMap, Class<K> keyType, Class<V> valueType) {
        this.innerMap = innerMap;
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public int size() {
        return innerMap.size();
    }

    @Override
    public boolean isEmpty() {
        return innerMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return innerMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return innerMap.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return innerMap.get(key);
    }

    @Override
    public V put(K key, V value) {
        return innerMap.put(keyType.cast(key), valueType.cast(value));
    }

    @Override
    public V remove(Object key) {
        return innerMap.remove(key);
    }

    @Override
    public void putAll(TMap<? extends K, ? extends V> m) {
        m = new THashMap<>(m);
        for (TIterator<? extends Entry<? extends K, ? extends V>> iter = m.entrySet().iterator(); iter.hasNext();) {
            Entry<? extends K, ? extends V> entry = iter.next();
            keyType.cast(entry.getKey());
            valueType.cast(entry.getValue());
        }
        innerMap.putAll(m);
    }

    @Override
    public void clear() {
        innerMap.clear();
    }

    @Override
    public TSet<K> keySet() {
        return new TCheckedSet<>(innerMap.keySet(), keyType);
    }

    @Override
    public TCollection<V> values() {
        return new TCheckedCollection<>(innerMap.values(), valueType);
    }

    @Override
    public TSet<TMap.Entry<K, V>> entrySet() {
        return innerMap.entrySet();
    }
}
