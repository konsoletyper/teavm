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

/**
 *
 * @author Alexey Andreev
 * @param <K>
 * @param <V>
 */
public interface TMap<K, V> {
    interface Entry<K1, V1> {
        K1 getKey();

        V1 getValue();

        V1 setValue(V1 value);
    }

    int size();

    boolean isEmpty();

    boolean containsKey(Object key);

    boolean containsValue(Object value);

    V get(Object key);

    V put(K key, V value);

    V remove(Object key);

    void putAll(TMap<? extends K, ? extends V> m);

    void clear();

    TSet<K> keySet();

    TCollection<V> values();

    TSet<Entry<K, V>> entrySet();
}
