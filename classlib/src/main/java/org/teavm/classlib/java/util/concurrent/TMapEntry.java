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

import java.util.Objects;
import org.teavm.classlib.java.util.TMap;

class TMapEntry<K, V> implements TMap.Entry<K, V>, Cloneable {

    final K key;
    volatile V value;

    interface Type<RT, KT, VT> {
        RT get(TMapEntry<KT, VT> entry);
    }

    TMapEntry(K theKey) {
        key = theKey;
    }

    TMapEntry(K theKey, V theValue) {
        key = theKey;
        value = theValue;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof TMap.Entry) {
            TMap.Entry<?, ?> entry = (TMap.Entry<?, ?>) object;
            return Objects.equals(key, entry.getKey());
        }
        return false;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key) ^ Objects.hashCode(value);
    }

    @Override
    public V setValue(V object) {
        V result = value;
        value = object;
        return result;
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }
}
