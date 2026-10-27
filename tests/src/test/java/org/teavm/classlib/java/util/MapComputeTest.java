/*
 *  Copyright 2026 Ethan Resnick.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

/**
 * computeIfAbsent, computeIfPresent, compute and merge of HashMap, LinkedHashMap, TreeMap and Hashtable.
 */
@TeaVMTest
public class MapComputeTest {
    private static final List<String> KINDS = List.of("HashMap", "LinkedHashMap", "TreeMap", "Hashtable");

    private static Map<Integer, Integer> newMap(String kind) {
        switch (kind) {
            case "HashMap":
                return new HashMap<>();
            case "LinkedHashMap":
                return new LinkedHashMap<>();
            case "TreeMap":
                return new TreeMap<>();
            default:
                return new Hashtable<>();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<Integer, Integer> cloneMap(Map<Integer, Integer> map) {
        if (map instanceof HashMap) {
            return (Map<Integer, Integer>) ((HashMap<Integer, Integer>) map).clone();
        } else if (map instanceof TreeMap) {
            return (Map<Integer, Integer>) ((TreeMap<Integer, Integer>) map).clone();
        } else {
            return (Map<Integer, Integer>) ((Hashtable<Integer, Integer>) map).clone();
        }
    }

    // The Javadoc of each of these methods in HashMap and TreeMap: "This method will, on a best-effort basis, throw a
    // ConcurrentModificationException if it is detected that the (re)mapping function modifies this map during
    // computation." Hashtable: "... if the (re)mapping function modified this map during computation."
    // LinkedHashMap inherits HashMap's methods.
    @Test
    public void functionThatAddsAMappingIsDetected() {
        for (var kind : KINDS) {
            checkDetected(kind, "computeIfAbsent", map -> map.computeIfAbsent(1, k -> {
                map.put(100, 100);
                return 1;
            }));
            checkDetected(kind, "computeIfPresent", map -> map.computeIfPresent(2, (k, v) -> {
                map.put(100, 100);
                return 1;
            }));
            checkDetected(kind, "compute", map -> map.compute(1, (k, v) -> {
                map.put(100, 100);
                return 1;
            }));
            checkDetected(kind, "compute of present key", map -> map.compute(2, (k, v) -> {
                map.put(100, 100);
                return null;
            }));
            checkDetected(kind, "merge", map -> map.merge(2, 5, (a, b) -> {
                map.put(100, 100);
                return a + b;
            }));
        }
    }

    @Test
    public void functionThatRemovesAMappingIsDetected() {
        for (var kind : KINDS) {
            checkDetected(kind, "computeIfAbsent", map -> map.computeIfAbsent(1, k -> {
                map.remove(3);
                return 1;
            }));
            checkDetected(kind, "computeIfPresent", map -> map.computeIfPresent(2, (k, v) -> {
                map.remove(3);
                return 1;
            }));
            checkDetected(kind, "compute", map -> map.compute(1, (k, v) -> {
                map.remove(3);
                return 1;
            }));
            checkDetected(kind, "merge", map -> map.merge(2, 5, (a, b) -> {
                map.remove(3);
                return a + b;
            }));
        }
    }

    // Before the call, the map is {2=20, 3=30}. The function either adds 100=100 or removes 3. Its own change
    // stays, and the computed value is not stored.
    private static void checkDetected(String kind, String what, Consumer<Map<Integer, Integer>> call) {
        var map = newMap(kind);
        map.put(2, 20);
        map.put(3, 30);
        assertThrows(ConcurrentModificationException.class, () -> call.accept(map), kind + " " + what);
        var expected = new HashMap<Integer, Integer>();
        expected.put(2, 20);
        if (map.containsKey(100)) {
            expected.put(100, 100);
            expected.put(3, 30);
        }
        assertEquals(expected, new HashMap<>(map), kind + " " + what);
    }

    // Replacing a value is not a structural modification ("merely changing the value associated with an existing key
    // is not a structural modification"), and reading the map is no modification at all. Neither may throw.
    @Test
    public void functionThatOnlyReadsOrReplacesValuesIsNotDetected() {
        for (var kind : KINDS) {
            var map = newMap(kind);
            map.put(2, 20);
            map.put(3, 30);
            assertEquals(1, map.computeIfAbsent(1, k -> {
                map.put(3, map.get(3) + 1);
                return 1;
            }), kind);
            assertEquals(5, map.computeIfPresent(2, (k, v) -> {
                map.put(3, map.get(3) + 1);
                return 5;
            }), kind);
            assertEquals(6, map.compute(2, (k, v) -> {
                map.put(3, map.get(3) + 1);
                return v + 1;
            }), kind);
            assertEquals(13, map.merge(2, 7, (a, b) -> {
                map.put(3, map.get(3) + 1);
                return a + b;
            }), kind);
            assertEquals(Map.of(1, 1, 2, 13, 3, 34), new HashMap<>(map), kind);
        }
    }

    // Map.merge: "NullPointerException - if ... the value or remappingFunction is null". IdentityHashMap and
    // WeakHashMap use Map's default method, ConcurrentHashMap uses ConcurrentMap's. Hashtable is left out: the
    // JDK's Hashtable accepts a null value for an absent key, and TeaVM's follows it.
    @Test
    public void mergeRejectsNullValue() {
        var maps = new ArrayList<Map<Integer, Integer>>();
        for (var kind : List.of("HashMap", "LinkedHashMap", "TreeMap")) {
            maps.add(newMap(kind));
        }
        maps.add(new IdentityHashMap<>());
        maps.add(new WeakHashMap<>());
        maps.add(new ConcurrentHashMap<>());
        for (var map : maps) {
            var kind = map.getClass().getName();
            Integer key = 2;
            map.put(key, 20);
            assertThrows(NullPointerException.class, () -> map.merge(1, null, (a, b) -> a), kind);
            assertThrows(NullPointerException.class, () -> map.merge(key, null, (a, b) -> a), kind);
            assertEquals(Map.of(2, 20), new HashMap<>(map), kind);
        }
    }

    // Map.compute: "If the remapping function returns null, the mapping is removed (or remains absent if initially
    // absent)". That includes a key mapped to null, in the maps that allow null values.
    @Test
    public void computeReturningNullRemovesMappingToNull() {
        for (var kind : List.of("HashMap", "LinkedHashMap", "TreeMap")) {
            var map = newMap(kind);
            map.put(1, null);
            map.put(2, 20);
            assertNull(map.compute(1, (k, v) -> null), kind);
            assertFalse(map.containsKey(1), kind);
            assertEquals(1, map.size(), kind);
        }
    }

    /**
     * A model of a map as a list of key/value pairs in insertion order, with the Map Javadoc's rules for each method.
     * Re-putting a key keeps its position, which is LinkedHashMap's insertion order.
     */
    private static final class Model {
        final List<Integer> keys = new ArrayList<>();
        final List<Integer> values = new ArrayList<>();

        Model copy() {
            var copy = new Model();
            copy.keys.addAll(keys);
            copy.values.addAll(values);
            return copy;
        }

        boolean contains(Integer key) {
            return keys.contains(key);
        }

        Integer get(Integer key) {
            int i = keys.indexOf(key);
            return i < 0 ? null : values.get(i);
        }

        void put(Integer key, Integer value) {
            int i = keys.indexOf(key);
            if (i < 0) {
                keys.add(key);
                values.add(value);
            } else {
                values.set(i, value);
            }
        }

        void remove(Integer key) {
            int i = keys.indexOf(key);
            if (i >= 0) {
                keys.remove(i);
                values.remove(i);
            }
        }

        void clear() {
            keys.clear();
            values.clear();
        }
    }

    /**
     * A random function result: null in a quarter of the calls, otherwise a value derived from its arguments.
     */
    private static Integer result(Random random, Integer a, Integer b) {
        if (random.nextInt(4) == 0) {
            return null;
        }
        return Objects.hashCode(a) * 31 + Objects.hashCode(b) + random.nextInt(3);
    }

    // Differential test of the contents (and, for LinkedHashMap and TreeMap, the order) against the model, for random
    // operation sequences mixing put/remove/clear/putAll, the four computing methods with functions that may return
    // null, null keys and values where the map allows them, and clones that must stay independent of the original.
    @Test
    public void contentsMatchModelOnRandomOperations() {
        for (var kind : KINDS) {
            boolean nullKeys = kind.equals("HashMap") || kind.equals("LinkedHashMap");
            boolean nullValues = nullKeys || kind.equals("TreeMap");
            for (int seed = 0; seed < 150; ++seed) {
                checkRandomOperations(kind, seed, nullKeys, nullValues);
            }
        }
    }

    private static void checkRandomOperations(String kind, int seed, boolean nullKeys, boolean nullValues) {
        var random = new Random(seed);
        Supplier<Integer> keys = () -> nullKeys && random.nextInt(15) == 0 ? null : random.nextInt(24);
        Supplier<Integer> values = () -> nullValues && random.nextInt(8) == 0 ? null : random.nextInt(1000);
        var map = newMap(kind);
        var model = new Model();
        int ops = 20 + random.nextInt(150);
        for (int i = 0; i < ops; ++i) {
            var key = keys.get();
            var message = kind + ", seed " + seed + ", op " + i;
            // Function results are taken before the call, so that the model and the map see the same ones.
            var fnResult = result(random, key, random.nextInt(5));
            int op = random.nextInt(13);
            switch (op) {
                case 0:
                case 1:
                case 2: {
                    var value = values.get();
                    assertEquals(model.get(key), map.put(key, value), message);
                    model.put(key, value);
                    break;
                }
                case 3:
                    assertEquals(model.get(key), map.remove(key), message);
                    model.remove(key);
                    break;
                case 4: {
                    var old = model.get(key);
                    var expected = old != null ? old : fnResult;
                    if (old == null && fnResult != null) {
                        model.put(key, fnResult);
                    }
                    assertEquals(expected, map.computeIfAbsent(key, k -> fnResult), message);
                    break;
                }
                case 5: {
                    var old = model.get(key);
                    Integer expected = null;
                    if (old != null) {
                        expected = fnResult;
                        if (fnResult == null) {
                            model.remove(key);
                        } else {
                            model.put(key, fnResult);
                        }
                    }
                    assertEquals(expected, map.computeIfPresent(key, (k, v) -> fnResult), message);
                    break;
                }
                case 6:
                case 7: {
                    var old = model.get(key);
                    if (fnResult == null) {
                        model.remove(key);
                    } else {
                        model.put(key, fnResult);
                    }
                    assertEquals(fnResult, map.compute(key, (k, v) -> {
                        assertEquals(old, v, message);
                        return fnResult;
                    }), message);
                    break;
                }
                case 8:
                case 9: {
                    var old = model.get(key);
                    int value = random.nextInt(1000);
                    var expected = old == null ? Integer.valueOf(value) : fnResult;
                    if (expected == null) {
                        model.remove(key);
                    } else {
                        model.put(key, expected);
                    }
                    assertEquals(expected, map.merge(key, value, (a, b) -> {
                        assertEquals(old, a, message);
                        assertEquals(value, b, message);
                        return fnResult;
                    }), message);
                    break;
                }
                case 10: {
                    var source = new LinkedHashMap<Integer, Integer>();
                    int n = random.nextInt(8);
                    for (int j = 0; j < n; ++j) {
                        source.put(keys.get(), values.get());
                    }
                    map.putAll(source);
                    for (var entry : source.entrySet()) {
                        model.put(entry.getKey(), entry.getValue());
                    }
                    break;
                }
                case 11:
                    if (random.nextInt(4) == 0) {
                        map.clear();
                        model.clear();
                    }
                    break;
                default: {
                    // Object.clone: the clone is independent of the original. Change each one, check both.
                    var clone = cloneMap(map);
                    assertEquals(map.getClass(), clone.getClass(), message);
                    var cloneModel = model.copy();
                    var key2 = keys.get();
                    var value = values.get();
                    map.put(key, value);
                    model.put(key, value);
                    clone.remove(key2);
                    cloneModel.remove(key2);
                    check(kind, clone, cloneModel, message + " (clone)");
                    check(kind, map, model, message + " (original)");
                    if (random.nextBoolean()) {
                        map = clone;
                        model = cloneModel;
                    }
                    break;
                }
            }
            check(kind, map, model, message);
        }
    }

    private static void check(String kind, Map<Integer, Integer> map, Model model, String message) {
        assertEquals(model.keys.size(), map.size(), message);
        var actualKeys = new ArrayList<Integer>();
        var actualValues = new ArrayList<Integer>();
        for (var entry : map.entrySet()) {
            actualKeys.add(entry.getKey());
            actualValues.add(entry.getValue());
        }
        for (int i = 0; i < actualKeys.size(); ++i) {
            var key = actualKeys.get(i);
            assertTrue(model.contains(key), message + ": unexpected key " + key);
            assertEquals(model.get(key), actualValues.get(i), message + ": value of " + key);
            assertEquals(model.get(key), map.get(key), message + ": get " + key);
        }
        assertEquals(new HashSet<>(model.keys), new HashSet<>(actualKeys), message);
        if (kind.equals("LinkedHashMap")) {
            assertEquals(model.keys, actualKeys, message + ": order");
        } else if (kind.equals("TreeMap")) {
            var sorted = new ArrayList<>(model.keys);
            sorted.sort(null);
            assertEquals(sorted, actualKeys, message + ": order");
        }
    }
}
