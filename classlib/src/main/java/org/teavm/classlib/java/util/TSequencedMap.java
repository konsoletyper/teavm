/*
 *  Copyright 2023 ihromant.
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

public interface TSequencedMap<K, V> extends TMap<K, V> {
    TSequencedMap<K, V> reversed();

    private static <K, V> TMap.Entry<K, V> clone(TMap.Entry<K, V> entry) {
        return TMap.entry(entry.getKey(), entry.getValue());
    }

    default TMap.Entry<K, V> firstEntry() {
        var it = entrySet().iterator();
        return it.hasNext() ? clone(it.next()) : null;
    }

    default TMap.Entry<K, V> lastEntry() {
        var it = reversed().entrySet().iterator();
        return it.hasNext() ? clone(it.next()) : null;
    }

    default TMap.Entry<K, V> pollFirstEntry() {
        var it = entrySet().iterator();
        if (it.hasNext()) {
            var entry = clone(it.next());
            it.remove();
            return entry;
        } else {
            return null;
        }
    }

    default TMap.Entry<K, V> pollLastEntry() {
        var it = reversed().entrySet().iterator();
        if (it.hasNext()) {
            var entry = clone(it.next());
            it.remove();
            return entry;
        } else {
            return null;
        }
    }

    default V putFirst(K k, V v) {
        throw new UnsupportedOperationException();
    }

    default V putLast(K k, V v) {
        throw new UnsupportedOperationException();
    }

    TSequencedSet<K> sequencedKeySet();

    TSequencedCollection<V> sequencedValues();

    TSequencedSet<TMap.Entry<K, V>> sequencedEntrySet();
}
