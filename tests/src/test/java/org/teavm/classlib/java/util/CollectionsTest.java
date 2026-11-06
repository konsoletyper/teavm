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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class CollectionsTest {
    @Test
    public void listSorted() {
        List<Integer> list = new ArrayList<>();
        list.addAll(Arrays.asList(2, 5, 7, 3, 5, 6));
        Collections.sort(list);
        assertEquals(Integer.valueOf(2), list.get(0));
        assertEquals(Integer.valueOf(3), list.get(1));
        assertEquals(Integer.valueOf(5), list.get(2));
        assertEquals(Integer.valueOf(5), list.get(3));
        assertEquals(Integer.valueOf(6), list.get(4));
        assertEquals(Integer.valueOf(7), list.get(5));
    }

    @Test
    public void binarySearchWorks() {
        List<Integer> list = new ArrayList<>(Arrays.asList(2, 4, 6, 8, 10, 12, 14, 16));
        assertEquals(3, Collections.binarySearch(list, 8));
        assertEquals(7, Collections.binarySearch(list, 16));
        assertEquals(0, Collections.binarySearch(list, 2));
        assertEquals(-1, Collections.binarySearch(list, 1));
        assertEquals(-2, Collections.binarySearch(list, 3));
        assertEquals(-3, Collections.binarySearch(list, 5));
        assertEquals(-8, Collections.binarySearch(list, 15));
        assertEquals(-9, Collections.binarySearch(list, 17));
    }

    @Test
    public void findsMinimum() {
        List<Integer> list = Arrays.asList(6, 5, 7, 3, 5, 6);
        assertEquals((Integer) 3, Collections.min(list));
    }

    @Test
    public void findsMaximum() {
        List<Integer> list = Arrays.asList(6, 5, 7, 3, 5, 6);
        assertEquals((Integer) 7, Collections.max(list));
    }

    @Test
    public void fills() {
        List<Integer> list = new ArrayList<>(Arrays.asList(6, 5, 7, 3, 5, 6));
        Collections.fill(list, 9);
        assertEquals(6, list.size());
        assertEquals((Integer) 9, list.get(0));
        assertEquals((Integer) 9, list.get(5));
        assertEquals((Integer) 9, list.get(2));
    }

    @Test
    public void copies() {
        List<Integer> list = new ArrayList<>(Arrays.asList(6, 5, 7, 3, 5, 6));
        List<Integer> dest = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
        Collections.copy(dest, list);
        assertEquals(7, dest.size());
        assertEquals((Integer) 6, dest.get(0));
        assertEquals((Integer) 5, dest.get(1));
        assertEquals((Integer) 5, dest.get(4));
        assertEquals((Integer) 6, dest.get(5));
        assertEquals((Integer) 7, dest.get(6));
    }

    @Test
    public void rotates() {
        List<Integer> list = new ArrayList<>(Arrays.asList(2, 5, 7, 3, 5, 6));
        Collections.rotate(list, 2);
        assertArrayEquals(new Integer[] { 5, 6, 2, 5, 7, 3 }, list.toArray(new Integer[0]));
    }

    @Test
    public void replaces() {
        List<Integer> list = new ArrayList<>(Arrays.asList(2, 5, 7, 3, 5, 6));
        assertTrue(Collections.replaceAll(list, 5, 9));
        assertArrayEquals(new Integer[] { 2, 9, 7, 3, 9, 6 }, list.toArray(new Integer[0]));
    }

    @Test
    public void findIndex() {
        List<Integer> list = new ArrayList<>(Arrays.asList(2, 5, 6, 3, 5, 6));
        assertEquals(1, Collections.indexOfSubList(list, Arrays.asList(5, 6)));
        assertEquals(-1, Collections.indexOfSubList(list, Arrays.asList(5, 1)));
        assertEquals(0, Collections.indexOfSubList(list, list));
    }

    @Test
    public void findsLastIndex() {
        List<Integer> list = new ArrayList<>(Arrays.asList(2, 5, 6, 3, 5, 6));
        assertEquals(4, Collections.lastIndexOfSubList(list, Arrays.asList(5, 6)));
        assertEquals(-1, Collections.lastIndexOfSubList(list, Arrays.asList(5, 1)));
        assertEquals(0, Collections.lastIndexOfSubList(list, list));
    }

    @Test
    public void shuffleWorksOnArrayAsList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4);
        Collections.shuffle(list);
        for (int i = 1; i <= 4; ++i) {
            assertTrue(list.contains(i), "List expected to contain " + i);
        }
    }
    
    @Test
    public void emptySet() {
        assertTrue(Collections.emptySet().containsAll(new HashSet<>()),
                "Collections.emptySet should produce 'true' for empty set argument");
    }

    @Test
    public void unmodifiableCollectionRejectsModifications() {
        var source = new ArrayList<>(List.of(1, 2, 3));
        var c = Collections.unmodifiableCollection(source);
        assertEquals(3, c.size());
        assertTrue(c.contains(2));
        assertTrue(c.containsAll(List.of(1, 3)));
        assertEquals("[1, 2, 3]", c.toString());
        assertUnmodifiableCollection(c);
        assertUnmodifiableCollection(Collections.unmodifiableCollection(new ArrayList<>()));
        source.add(4);
        assertEquals(4, c.size());
    }

    @Test
    public void unmodifiableSetRejectsModifications() {
        var source = new HashSet<>(Set.of("a", "b"));
        var s = Collections.unmodifiableSet(source);
        assertEquals(Set.of("a", "b"), s);
        assertEquals(s, Set.of("a", "b"));
        assertEquals(Set.of("a", "b").hashCode(), s.hashCode());
        assertUnmodifiableCollection(s);
        assertUnmodifiableCollection(Collections.unmodifiableSet(new HashSet<>()));
    }

    @Test
    public void unmodifiableMapRejectsModifications() {
        var source = new HashMap<String, Integer>();
        source.put("a", 1);
        source.put("b", 2);
        var m = Collections.unmodifiableMap(source);
        assertEquals(2, m.size());
        assertEquals(Integer.valueOf(1), m.get("a"));
        assertEquals(Integer.valueOf(3), m.getOrDefault("c", 3));
        assertTrue(m.containsKey("b"));
        assertTrue(m.containsValue(2));
        assertEquals(Map.of("a", 1, "b", 2), m);
        assertEquals(m, Map.of("a", 1, "b", 2));
        assertEquals(Map.of("a", 1, "b", 2).hashCode(), m.hashCode());
        assertUnmodifiableMap(m);
        assertUnmodifiableMap(Collections.unmodifiableMap(new HashMap<>()));

        source.put("c", 3);
        assertEquals(3, m.size());
        assertEquals(Integer.valueOf(3), m.get("c"));
        assertEquals(Integer.valueOf(1), source.get("a"));
    }

    @Test
    public void unmodifiableSequencedMap() {
        var source = new LinkedHashMap<String, Integer>();
        source.put("c", 3);
        source.put("a", 1);
        source.put("b", 2);
        var m = Collections.unmodifiableSequencedMap(source);

        assertEquals(List.of("c", "a", "b"), new ArrayList<>(m.keySet()));
        assertEquals(List.of("c", "a", "b"), new ArrayList<>(m.sequencedKeySet()));
        assertEquals(List.of("b", "a", "c"), new ArrayList<>(m.sequencedKeySet().reversed()));
        assertEquals("b", m.sequencedKeySet().getLast());
        assertEquals(List.of(3, 1, 2), new ArrayList<>(m.values()));
        assertEquals(List.of(2, 1, 3), new ArrayList<>(m.sequencedValues().reversed()));
        assertEquals(Integer.valueOf(3), m.sequencedValues().getFirst());
        assertEquals(Map.entry("c", 3), m.entrySet().iterator().next());
        assertEquals(Map.entry("b", 2), m.sequencedEntrySet().getLast());
        assertEquals(Map.entry("c", 3), m.firstEntry());
        assertEquals(Map.entry("b", 2), m.lastEntry());

        var reversed = m.reversed();
        assertEquals(List.of("b", "a", "c"), new ArrayList<>(reversed.keySet()));
        assertEquals(Map.entry("b", 2), reversed.firstEntry());
        assertUnmodifiableSequencedMap(reversed);
        assertUnmodifiableSequencedMap(m);
        assertUnmodifiableSequencedMap(Collections.unmodifiableSequencedMap(new LinkedHashMap<>()));

        var empty = Collections.unmodifiableSequencedMap(new LinkedHashMap<String, Integer>());
        assertNull(empty.firstEntry());
        assertNull(empty.lastEntry());
    }

    @Test
    public void unmodifiableSortedMap() {
        var source = new TreeMap<String, Integer>();
        source.put("b", 2);
        source.put("d", 4);
        source.put("a", 1);
        source.put("c", 3);
        var m = Collections.unmodifiableSortedMap(source);

        assertEquals(4, m.size());
        assertNull(m.comparator());
        assertEquals("a", m.firstKey());
        assertEquals("d", m.lastKey());
        assertEquals(Integer.valueOf(3), m.get("c"));
        assertEquals(List.of("a", "b", "c", "d"), new ArrayList<>(m.keySet()));
        assertEquals(List.of(1, 2, 3, 4), new ArrayList<>(m.values()));
        assertEquals("{a=1, b=2, c=3, d=4}", m.toString());
        assertEquals(source, m);
        assertEquals(Map.entry("a", 1), m.firstEntry());
        assertEquals(Map.entry("d", 4), m.lastEntry());

        SequencedMap<String, Integer> reversed = m;
        reversed = reversed.reversed();
        assertEquals(List.of("d", "c", "b", "a"), new ArrayList<>(reversed.keySet()));
        var finalReversed = reversed;
        assertThrows(UnsupportedOperationException.class, () -> finalReversed.put("x", 0));
        assertThrows(UnsupportedOperationException.class, () -> finalReversed.remove("a"));
        assertThrows(UnsupportedOperationException.class, () -> finalReversed.firstEntry().setValue(0));

        SortedMap<String, Integer> sub = m.subMap("b", "d");
        assertEquals(List.of("b", "c"), new ArrayList<>(sub.keySet()));
        assertEquals("b", sub.firstKey());
        assertEquals("c", sub.lastKey());
        assertUnmodifiableSortedMap(sub);

        SortedMap<String, Integer> head = m.headMap("c");
        assertEquals(List.of("a", "b"), new ArrayList<>(head.keySet()));
        assertUnmodifiableSortedMap(head);

        SortedMap<String, Integer> tail = m.tailMap("c");
        assertEquals(List.of("c", "d"), new ArrayList<>(tail.keySet()));
        assertUnmodifiableSortedMap(tail);

        assertUnmodifiableSortedMap(m);
        assertUnmodifiableSortedMap(Collections.unmodifiableSortedMap(new TreeMap<>()));

        source.put("bb", 5);
        assertEquals(5, m.size());
        assertEquals(List.of("b", "bb", "c"), new ArrayList<>(sub.keySet()));
        assertEquals(List.of("a", "b", "bb", "c", "d"), new ArrayList<>(m.sequencedKeySet()));
    }

    @Test
    public void unmodifiableSortedMapWithComparator() {
        var source = new TreeMap<String, Integer>(Comparator.reverseOrder());
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);
        var m = Collections.unmodifiableSortedMap(source);
        assertEquals(Comparator.reverseOrder(), m.comparator());
        assertEquals("c", m.firstKey());
        assertEquals("a", m.lastKey());
        assertEquals(List.of("c", "b", "a"), new ArrayList<>(m.keySet()));
        assertEquals(List.of("b", "a"), new ArrayList<>(m.tailMap("b").keySet()));
    }

    @Test
    public void unmodifiableSortedMapViewsCachedConsistently() {
        var source = new TreeMap<String, Integer>(Map.of("a", 1, "b", 2));
        var m = Collections.unmodifiableSortedMap(source);
        assertEquals(List.of("a", "b"), new ArrayList<>(m.keySet()));
        assertEquals(List.of(1, 2), new ArrayList<>(m.values()));
        assertEquals(2, m.entrySet().size());
        assertEquals("b", m.sequencedKeySet().getLast());
        assertEquals(Integer.valueOf(2), m.sequencedValues().getLast());
        assertEquals(Map.entry("b", 2), m.sequencedEntrySet().getLast());
    }

    @Test
    public void unmodifiableSortedMapRequiresNonNull() {
        assertThrows(NullPointerException.class, () -> Collections.unmodifiableSortedMap(null));
    }

    @Test
    public void unmodifiableSortedSet() {
        var source = new TreeSet<>(List.of("b", "d", "a", "c"));
        var s = Collections.unmodifiableSortedSet(source);

        assertEquals(4, s.size());
        assertNull(s.comparator());
        assertEquals("a", s.first());
        assertEquals("d", s.last());
        assertEquals("a", s.getFirst());
        assertEquals("d", s.getLast());
        assertTrue(s.contains("c"));
        assertEquals(List.of("a", "b", "c", "d"), new ArrayList<>(s));
        assertEquals("[a, b, c, d]", s.toString());
        assertEquals(Set.of("a", "b", "c", "d"), s);
        assertEquals(s, Set.of("a", "b", "c", "d"));
        assertEquals(Set.of("a", "b", "c", "d").hashCode(), s.hashCode());

        SortedSet<String> reversed = s.reversed();
        assertEquals(List.of("d", "c", "b", "a"), new ArrayList<>(reversed));
        assertEquals("d", reversed.first());
        assertThrows(UnsupportedOperationException.class, () -> reversed.add("x"));
        assertThrows(UnsupportedOperationException.class, () -> reversed.remove("a"));

        SortedSet<String> sub = s.subSet("b", "d");
        assertEquals(List.of("b", "c"), new ArrayList<>(sub));
        assertUnmodifiableSortedSet(sub);

        SortedSet<String> head = s.headSet("c");
        assertEquals(List.of("a", "b"), new ArrayList<>(head));
        assertUnmodifiableSortedSet(head);

        SortedSet<String> tail = s.tailSet("c");
        assertEquals(List.of("c", "d"), new ArrayList<>(tail));
        assertUnmodifiableSortedSet(tail);

        assertUnmodifiableSortedSet(s);
        assertUnmodifiableSortedSet(Collections.unmodifiableSortedSet(new TreeSet<>()));

        source.add("bb");
        assertEquals(5, s.size());
        assertEquals(List.of("b", "bb", "c"), new ArrayList<>(sub));
        assertEquals(List.of("a", "b", "bb", "c", "d"), new ArrayList<>(s));
    }

    @Test
    public void unmodifiableSortedSetWithComparator() {
        var source = new TreeSet<String>(Comparator.reverseOrder());
        source.addAll(List.of("a", "b", "c"));
        var s = Collections.unmodifiableSortedSet(source);
        assertEquals(Comparator.reverseOrder(), s.comparator());
        assertEquals("c", s.first());
        assertEquals("a", s.last());
        assertEquals(List.of("c", "b", "a"), new ArrayList<>(s));
        assertEquals(List.of("b", "a"), new ArrayList<>(s.tailSet("b")));
        assertEquals(List.of("a", "b", "c"), new ArrayList<>(s.reversed()));
    }

    @Test
    public void unmodifiableSortedSetRequiresNonNull() {
        assertThrows(NullPointerException.class, () -> Collections.unmodifiableSortedSet(null));
    }

    @SuppressWarnings("unchecked")
    private static void assertUnmodifiableSortedSet(SortedSet<?> set) {
        assertUnmodifiableCollection(set);
        var s = (SortedSet<Object>) set;
        assertThrows(UnsupportedOperationException.class, () -> s.addFirst("x"));
        assertThrows(UnsupportedOperationException.class, () -> s.addLast("x"));
        if (!s.isEmpty()) {
            assertThrows(UnsupportedOperationException.class, s::removeFirst);
            assertThrows(UnsupportedOperationException.class, s::removeLast);
            var first = s.first();
            assertUnmodifiableCollection(s.headSet(first));
            assertUnmodifiableCollection(s.tailSet(first));
            assertUnmodifiableCollection(s.subSet(first, first));
        }
    }

    @SuppressWarnings("unchecked")
    private static void assertUnmodifiableCollection(Collection<?> collection) {
        var c = (Collection<Object>) collection;
        assertThrows(UnsupportedOperationException.class, () -> c.add("x"));
        assertThrows(UnsupportedOperationException.class, () -> c.addAll(List.of("x")));
        assertThrows(UnsupportedOperationException.class, () -> c.remove("x"));
        assertThrows(UnsupportedOperationException.class, () -> c.removeAll(List.of("x")));
        assertThrows(UnsupportedOperationException.class, () -> c.retainAll(List.of("x")));
        assertThrows(UnsupportedOperationException.class, () -> c.removeIf(e -> true));
        assertThrows(UnsupportedOperationException.class, c::clear);
        var it = c.iterator();
        if (it.hasNext()) {
            it.next();
            assertThrows(UnsupportedOperationException.class, it::remove);
        }
    }

    @SuppressWarnings("unchecked")
    private static <K, V> void assertUnmodifiableMap(Map<K, V> m) {
        var om = (Map<Object, Object>) m;
        assertThrows(UnsupportedOperationException.class, () -> om.put("x", 0));
        assertThrows(UnsupportedOperationException.class, () -> om.putAll(Map.of("x", 0)));
        assertThrows(UnsupportedOperationException.class, () -> om.remove("x"));
        assertThrows(UnsupportedOperationException.class, () -> om.remove("x", 0));
        assertThrows(UnsupportedOperationException.class, om::clear);
        assertThrows(UnsupportedOperationException.class, () -> om.putIfAbsent("x", 0));
        assertThrows(UnsupportedOperationException.class, () -> om.replace("x", 0));
        assertThrows(UnsupportedOperationException.class, () -> om.replace("x", 0, 1));
        assertThrows(UnsupportedOperationException.class, () -> om.replaceAll((k, v) -> v));
        assertThrows(UnsupportedOperationException.class, () -> om.computeIfAbsent("x", k -> 0));
        assertThrows(UnsupportedOperationException.class, () -> om.computeIfPresent("x", (k, v) -> v));
        assertThrows(UnsupportedOperationException.class, () -> om.compute("x", (k, v) -> v));
        assertThrows(UnsupportedOperationException.class, () -> om.merge("x", 0, (a, b) -> a));
        assertUnmodifiableCollection(om.keySet());
        assertUnmodifiableCollection(om.values());
        assertUnmodifiableCollection(om.entrySet());
        for (var entry : om.entrySet()) {
            assertThrows(UnsupportedOperationException.class, () -> entry.setValue(0));
        }
        var array = om.entrySet().toArray(new Map.Entry<?, ?>[0]);
        for (var entry : array) {
            assertThrows(UnsupportedOperationException.class, () -> ((Map.Entry<Object, Object>) entry).setValue(0));
        }
    }

    @SuppressWarnings("unchecked")
    private static <K, V> void assertUnmodifiableSequencedMap(SequencedMap<K, V> m) {
        assertUnmodifiableMap(m);
        var om = (SequencedMap<Object, Object>) m;
        assertThrows(UnsupportedOperationException.class, () -> om.putFirst("x", 0));
        assertThrows(UnsupportedOperationException.class, () -> om.putLast("x", 0));
        assertThrows(UnsupportedOperationException.class, () -> om.sequencedKeySet().addFirst("x"));
        assertThrows(UnsupportedOperationException.class, () -> om.sequencedValues().addLast(0));
        if (!om.isEmpty()) {
            assertThrows(UnsupportedOperationException.class, om::pollFirstEntry);
            assertThrows(UnsupportedOperationException.class, om::pollLastEntry);
            assertThrows(UnsupportedOperationException.class, () -> om.sequencedKeySet().removeLast());
            assertThrows(UnsupportedOperationException.class, () -> om.sequencedValues().removeFirst());
            assertThrows(UnsupportedOperationException.class, () -> om.sequencedEntrySet().removeFirst());
            assertThrows(UnsupportedOperationException.class, () -> om.firstEntry().setValue(0));
            assertThrows(UnsupportedOperationException.class, () -> om.sequencedEntrySet().getFirst().setValue(0));
            assertThrows(UnsupportedOperationException.class,
                    () -> om.sequencedEntrySet().reversed().iterator().next().setValue(0));
        }
    }

    private static <K, V> void assertUnmodifiableSortedMap(SortedMap<K, V> m) {
        assertUnmodifiableSequencedMap(m);
        if (!m.isEmpty()) {
            var key = m.firstKey();
            assertUnmodifiableSequencedMap(m.headMap(key));
            assertUnmodifiableSequencedMap(m.tailMap(key));
            assertUnmodifiableSequencedMap(m.subMap(key, key));
        }
    }
}
