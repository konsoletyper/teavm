/*
 *  Copyright 2026 Alexey Andreev.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class ConcurrentMapTest {
    @Test
    public void computeIfAbsentReturnsNewValue() {
        ConcurrentMap<String, Integer> map = new SimpleConcurrentMap<>();
        assertEquals(Integer.valueOf(23), map.computeIfAbsent("a", key -> 23));
        assertEquals(Integer.valueOf(23), map.get("a"));
        assertEquals(Integer.valueOf(23), map.computeIfAbsent("a", key -> 42));
        assertNull(map.computeIfAbsent("b", key -> null));
        assertFalse(map.containsKey("b"));
    }

    @Test
    public void mergeRemovesEntryWhenFunctionReturnsNull() {
        ConcurrentMap<String, Integer> map = new SimpleConcurrentMap<>();
        map.put("a", 1);
        assertNull(map.merge("a", 2, (oldValue, value) -> null));
        assertFalse(map.containsKey("a"));
    }

    // Implements only the abstract methods, so the default methods of ConcurrentMap apply.
    static class SimpleConcurrentMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {
        private final Map<K, V> map = new HashMap<>();

        @Override
        public Set<Entry<K, V>> entrySet() {
            return map.entrySet();
        }

        @Override
        public V get(Object key) {
            return map.get(key);
        }

        @Override
        public V put(K key, V value) {
            return map.put(key, value);
        }

        @Override
        public V putIfAbsent(K key, V value) {
            V existing = map.get(key);
            if (existing == null) {
                map.put(key, value);
            }
            return existing;
        }

        @Override
        public boolean remove(Object key, Object value) {
            if (map.containsKey(key) && map.get(key).equals(value)) {
                map.remove(key);
                return true;
            }
            return false;
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            if (map.containsKey(key) && map.get(key).equals(oldValue)) {
                map.put(key, newValue);
                return true;
            }
            return false;
        }

        @Override
        public V replace(K key, V value) {
            return map.containsKey(key) ? map.put(key, value) : null;
        }
    }
}
