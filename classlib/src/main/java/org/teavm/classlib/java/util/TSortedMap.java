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
public interface TSortedMap<K, V> extends TSequencedMap<K, V> {
    TComparator<? super K> comparator();

    TSortedMap<K, V> subMap(K fromKey, K toKey);

    TSortedMap<K, V> headMap(K toKey);

    TSortedMap<K, V> tailMap(K fromKey);

    K firstKey();

    K lastKey();
}
