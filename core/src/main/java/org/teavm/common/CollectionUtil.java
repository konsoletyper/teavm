/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.common;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public final class CollectionUtil {
    private CollectionUtil() {
    }

    public static <T, K extends Comparable<K>> int binarySearch(List<? extends T> list, K key,
            Function<T, K> keyExtractor) {
        return binarySearch(list, key, keyExtractor, Comparable::compareTo);
    }

    public static <T, K> int binarySearch(List<? extends T> list, K key, Function<T, K> keyExtractor,
            Comparator<K> comparator) {
        var l = 0;
        var u = list.size() - 1;
        while (true) {
            var i = (l + u) / 2;
            var t = keyExtractor.apply(list.get(i));
            var cmp = comparator.compare(key, t);
            if (cmp == 0) {
                return i;
            } else if (cmp > 0) {
                l = i + 1;
                if (l > u) {
                    return -i - 2;
                }
            } else {
                u = i - 1;
                if (u < l) {
                    return -i - 1;
                }
            }
        }
    }
}
