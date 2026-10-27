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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SequencedMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
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
    public void unmodifiableSortedAndNavigableViews() {
        var treeSet = new TreeSet<>(List.of(5, 1, 3));
        SortedSet<Integer> sortedSet = Collections.unmodifiableSortedSet(treeSet);
        assertEquals((Object) 1, sortedSet.first());
        assertEquals(Set.of(1), sortedSet.headSet(3));
        assertThrows(UnsupportedOperationException.class, () -> sortedSet.add(2));
        assertThrows(UnsupportedOperationException.class, () -> sortedSet.tailSet(3).remove(5));
        NavigableSet<Integer> navigableSet = Collections.unmodifiableNavigableSet(treeSet);
        assertEquals((Object) 3, navigableSet.ceiling(2));
        assertEquals((Object) 5, navigableSet.descendingSet().first());
        assertThrows(UnsupportedOperationException.class, navigableSet::pollFirst);

        var treeMap = new TreeMap<>(Map.of("b", 2, "a", 1, "c", 3));
        SortedMap<String, Integer> sortedMap = Collections.unmodifiableSortedMap(treeMap);
        assertEquals("a", sortedMap.firstKey());
        assertThrows(UnsupportedOperationException.class, () -> sortedMap.put("d", 4));
        NavigableMap<String, Integer> navigableMap = Collections.unmodifiableNavigableMap(treeMap);
        assertEquals((Object) 2, navigableMap.floorEntry("bb").getValue());
        assertEquals("c", navigableMap.descendingMap().firstKey());
        assertThrows(UnsupportedOperationException.class, () -> navigableMap.firstEntry().setValue(9));
        assertThrows(UnsupportedOperationException.class, () -> navigableMap.headMap("c").clear());
    }

    @Test
    public void emptySortedAndNavigableCollections() {
        assertTrue(Collections.emptySortedSet().isEmpty());
        assertTrue(Collections.emptyNavigableSet().isEmpty());
        assertTrue(Collections.emptySortedMap().isEmpty());
        NavigableMap<String, Integer> empty = Collections.emptyNavigableMap();
        assertTrue(empty.isEmpty());
        assertNull(empty.firstEntry());
        assertThrows(UnsupportedOperationException.class, () -> empty.put("a", 1));
    }

    // Collections.unmodifiableSortedSet etc.: "Query operations on the returned sorted set "read through" to the
    // specified sorted set. Attempts to modify the returned sorted set, whether direct, via its iterator, or via
    // its subSet, headSet, or tailSet views, result in an UnsupportedOperationException." The navigable forms add
    // the descendingSet / descendingMap views, and the map forms the collection views (keySet, values, entrySet).
    // Each check below walks the view and its sub-views in parallel with the same views of the backing
    // collection, and requires equal contents and lookups, and that every mutator throws.

    private static final int VIEW_DEPTH = 2;

    // Probes around and between the elements 1, 3, 5, 7, 9 and 4.
    private static final int[] PROBES = { 0, 1, 2, 3, 4, 5, 6, 9, 10 };

    @Test
    public void unmodifiableSortedSetViewsReadThroughAndRejectWrites() {
        for (var backing : sortedSets()) {
            checkUnmodifiableSet(Collections.unmodifiableSortedSet(backing), backing, 100, VIEW_DEPTH);
            checkUnmodifiableSet(Collections.unmodifiableNavigableSet(backing), backing, 100, VIEW_DEPTH);
        }
    }

    @Test
    public void unmodifiableSortedMapViewsReadThroughAndRejectWrites() {
        for (var backing : sortedMaps()) {
            checkUnmodifiableMap(Collections.unmodifiableSortedMap(backing), backing, 100, VIEW_DEPTH);
            checkUnmodifiableMap(Collections.unmodifiableNavigableMap(backing), backing, 100, VIEW_DEPTH);
        }
    }

    @Test
    public void unmodifiableSortedViewsAreLive() {
        var set = new TreeSet<>(List.of(1, 3, 5));
        var setView = Collections.unmodifiableNavigableSet(set);
        var setHead = setView.headSet(4, true);
        set.add(2);
        set.remove(5);
        assertEquals(List.of(1, 2, 3), new ArrayList<>(setView));
        assertEquals(List.of(1, 2, 3), new ArrayList<>(setHead));
        assertEquals((Object) 3, setView.last());

        var map = new TreeMap<>(Map.of("a", 1, "c", 3));
        var mapView = Collections.unmodifiableNavigableMap(map);
        var mapTail = mapView.tailMap("b", true);
        map.put("b", 2);
        map.put("c", 30);
        assertEquals(Map.of("a", 1, "b", 2, "c", 30), mapView);
        assertEquals(List.of("b", "c"), new ArrayList<>(mapTail.keySet()));
        assertEquals((Object) 30, mapTail.get("c"));
    }

    @Test
    public void unmodifiableSortedViewsRejectNull() {
        // Collections: "The methods of this class all throw a NullPointerException if the collections or class
        // objects provided to them are null."
        assertThrows(NullPointerException.class, () -> Collections.unmodifiableSortedSet(null));
        assertThrows(NullPointerException.class, () -> Collections.unmodifiableNavigableSet(null));
        assertThrows(NullPointerException.class, () -> Collections.unmodifiableSortedMap(null));
        assertThrows(NullPointerException.class, () -> Collections.unmodifiableNavigableMap(null));
    }

    @Test
    public void emptySortedCollectionsAreEmptyAndImmutable() {
        // Collections.emptySortedSet: "Returns an empty sorted set (immutable)."; likewise emptyNavigableSet,
        // emptySortedMap ("an empty sorted map (immutable)") and emptyNavigableMap.
        List<SortedSet<String>> sets = List.of(Collections.emptySortedSet(), Collections.emptyNavigableSet());
        for (var set : sets) {
            checkUnmodifiableSet(set, new TreeSet<>(), "x", VIEW_DEPTH);
            assertThrows(NoSuchElementException.class, set::first);
            assertThrows(NoSuchElementException.class, set::last);
            assertNull(set.comparator());
        }
        List<SortedMap<String, String>> maps = List.of(Collections.emptySortedMap(),
                Collections.emptyNavigableMap());
        for (var map : maps) {
            checkUnmodifiableMap(map, new TreeMap<>(), "x", VIEW_DEPTH);
            assertThrows(NoSuchElementException.class, map::firstKey);
            assertThrows(NoSuchElementException.class, map::lastKey);
            assertNull(map.comparator());
        }
        NavigableMap<String, Integer> empty = Collections.emptyNavigableMap();
        assertNull(empty.firstEntry());
        assertNull(empty.ceilingKey("a"));
        NavigableSet<String> emptySet = Collections.emptyNavigableSet();
        assertNull(emptySet.floor("a"));
    }

    @Test
    public void reverseOrderReversesComparatorReturningMinValue() {
        // Collections.reverseOrder(cmp): "Returns a comparator that imposes the reverse ordering of the specified
        // comparator." Negating Integer.MIN_VALUE gives Integer.MIN_VALUE, so negation would not reverse this one.
        Comparator<Integer> extreme = (a, b) -> a < b ? Integer.MIN_VALUE : a > b ? 1 : 0;
        Comparator<Integer> reversed = Collections.reverseOrder(extreme);
        assertTrue(reversed.compare(1, 2) > 0);
        assertTrue(reversed.compare(2, 1) < 0);
        assertEquals(0, reversed.compare(1, 1));
    }

    @Test
    public void synchronizedSortedCollectionsAreBackedByTheArgument() {
        // Collections.synchronizedSortedSet: "Returns a synchronized (thread-safe) sorted set backed by the
        // specified sorted set."; likewise the navigable and map forms.
        var set = new TreeSet<>(List.of(2, 1));
        SortedSet<Integer> sortedSet = Collections.synchronizedSortedSet(set);
        NavigableSet<Integer> navigableSet = Collections.synchronizedNavigableSet(set);
        sortedSet.add(3);
        navigableSet.add(0);
        assertEquals(List.of(0, 1, 2, 3), new ArrayList<>(set));
        assertEquals((Object) 1, navigableSet.higher(0));
        assertEquals((Object) 0, sortedSet.first());

        var map = new TreeMap<String, Integer>();
        SortedMap<String, Integer> sortedMap = Collections.synchronizedSortedMap(map);
        NavigableMap<String, Integer> navigableMap = Collections.synchronizedNavigableMap(map);
        sortedMap.put("b", 2);
        navigableMap.put("a", 1);
        assertEquals(Map.of("a", 1, "b", 2), map);
        assertEquals("a", navigableMap.lowerKey("b"));
        assertEquals("a", sortedMap.firstKey());
    }

    private static List<TreeSet<Integer>> sortedSets() {
        var reversed = new TreeSet<Integer>(Comparator.reverseOrder());
        reversed.addAll(List.of(1, 3, 5, 7, 9));
        return List.of(new TreeSet<>(), new TreeSet<>(List.of(4)), new TreeSet<>(List.of(1, 3, 5, 7, 9)), reversed);
    }

    private static List<TreeMap<Integer, String>> sortedMaps() {
        var reversed = new TreeMap<Integer, String>(Comparator.reverseOrder());
        reversed.putAll(Map.of(1, "a", 3, "c", 5, "e", 7, "g", 9, "i"));
        var mutableEntries = new MutableEntryTreeMap();
        mutableEntries.putAll(Map.of(1, "a", 3, "c", 5, "e", 7, "g", 9, "i"));
        return List.of(new TreeMap<>(), new TreeMap<>(Map.of(4, "d")),
                new TreeMap<>(Map.of(1, "a", 3, "c", 5, "e", 7, "g", 9, "i")), reversed, mutableEntries);
    }

    // A map whose navigation methods return entries that allow setValue, so the unmodifiable view has to wrap
    // them itself.
    static class MutableEntryTreeMap extends TreeMap<Integer, String> {
        private static Map.Entry<Integer, String> mutable(Map.Entry<Integer, String> e) {
            return e == null ? null : new AbstractMap.SimpleEntry<>(e);
        }

        @Override
        public Map.Entry<Integer, String> firstEntry() {
            return mutable(super.firstEntry());
        }

        @Override
        public Map.Entry<Integer, String> lastEntry() {
            return mutable(super.lastEntry());
        }

        @Override
        public Map.Entry<Integer, String> lowerEntry(Integer key) {
            return mutable(super.lowerEntry(key));
        }

        @Override
        public Map.Entry<Integer, String> floorEntry(Integer key) {
            return mutable(super.floorEntry(key));
        }

        @Override
        public Map.Entry<Integer, String> ceilingEntry(Integer key) {
            return mutable(super.ceilingEntry(key));
        }

        @Override
        public Map.Entry<Integer, String> higherEntry(Integer key) {
            return mutable(super.higherEntry(key));
        }
    }

    private static void assertUnsupported(Executable executable) {
        assertThrows(UnsupportedOperationException.class, executable);
    }

    // Adds a fresh element, which may lie outside a sub-view's range. Collection.add allows
    // IllegalArgumentException then, and the JDK's reversed() views of a SortedSet or SortedMap check the range
    // first.
    private static void assertAddRejected(Executable executable) {
        var e = assertThrows(RuntimeException.class, executable);
        assertTrue(e instanceof UnsupportedOperationException || e instanceof IllegalArgumentException,
                e.toString());
    }

    @SuppressWarnings("unchecked")
    private static <E> void checkUnmodifiableSet(SortedSet<E> view, SortedSet<E> backing, E fresh, int depth) {
        var before = new ArrayList<>(backing);
        assertEquals(before, new ArrayList<>(view));
        assertEquals(backing.size(), view.size());
        assertEquals(backing.isEmpty(), view.isEmpty());
        assertEquals(backing, view);
        assertEquals(view, backing);
        assertEquals(backing.hashCode(), view.hashCode());
        assertSameOrder(backing.comparator(), view.comparator(), backing);
        assertTrue(view.containsAll(backing));
        E some = backing.isEmpty() ? null : backing.first();
        if (some != null) {
            assertEquals(backing.first(), view.first());
            assertEquals(backing.last(), view.last());
            assertEquals(backing.getFirst(), view.getFirst());
            assertEquals(backing.getLast(), view.getLast());
        }

        assertAddRejected(() -> view.add(fresh));
        assertAddRejected(() -> view.addAll(List.of(fresh)));
        if (some != null) {
            // The JDK's reversed() view of a SortedSet clears through its iterator, so it only throws when there
            // is something to remove; the same holds for the map views below.
            assertUnsupported(view::clear);
            assertUnsupported(() -> view.add(some));
            assertUnsupported(() -> view.remove(some));
            assertUnsupported(() -> view.removeAll(List.of(some)));
            assertUnsupported(() -> view.retainAll(List.of()));
            assertUnsupported(() -> view.removeIf(e -> true));
            assertUnsupported(view::removeFirst);
            assertUnsupported(view::removeLast);
            assertUnsupported(() -> removeFirstThroughIterator(view.iterator()));
        }
        if (view instanceof NavigableSet) {
            var nview = (NavigableSet<E>) view;
            var nbacking = (NavigableSet<E>) backing;
            assertUnsupported(nview::pollFirst);
            assertUnsupported(nview::pollLast);
            assertEquals(new ArrayList<>(nbacking.descendingSet()), toList(nview.descendingIterator()));
            if (some != null) {
                assertUnsupported(() -> removeFirstThroughIterator(nview.descendingIterator()));
            }
            if (some instanceof Integer) {
                for (int probe : PROBES) {
                    E e = (E) (Object) probe;
                    assertEquals(nbacking.lower(e), nview.lower(e));
                    assertEquals(nbacking.floor(e), nview.floor(e));
                    assertEquals(nbacking.ceiling(e), nview.ceiling(e));
                    assertEquals(nbacking.higher(e), nview.higher(e));
                    assertEquals(nbacking.contains(e), nview.contains(e));
                }
            }
        }
        assertEquals(before, new ArrayList<>(backing));

        if (depth == 0) {
            return;
        }
        int next = depth - 1;
        checkUnmodifiableSet(view.reversed(), backing.reversed(), fresh, next);
        if (some != null) {
            E last = backing.last();
            checkUnmodifiableSet(view.headSet(last), backing.headSet(last), fresh, next);
            checkUnmodifiableSet(view.tailSet(last), backing.tailSet(last), fresh, next);
            checkUnmodifiableSet(view.subSet(some, last), backing.subSet(some, last), fresh, next);
        }
        if (view instanceof NavigableSet) {
            var nview = (NavigableSet<E>) view;
            var nbacking = (NavigableSet<E>) backing;
            checkUnmodifiableSet(nview.descendingSet(), nbacking.descendingSet(), fresh, next);
            if (some != null) {
                E last = backing.last();
                checkUnmodifiableSet(nview.headSet(some, true), nbacking.headSet(some, true), fresh, next);
                checkUnmodifiableSet(nview.tailSet(some, false), nbacking.tailSet(some, false), fresh, next);
                checkUnmodifiableSet(nview.subSet(some, true, last, true), nbacking.subSet(some, true, last, true),
                        fresh, next);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <K, V> void checkUnmodifiableMap(SortedMap<K, V> view, SortedMap<K, V> backing, K freshKey,
            int depth) {
        var before = new ArrayList<>(backing.entrySet());
        assertEquals(before, new ArrayList<>(view.entrySet()));
        assertEquals(new ArrayList<>(backing.keySet()), new ArrayList<>(view.keySet()));
        assertEquals(new ArrayList<>(backing.values()), new ArrayList<>(view.values()));
        assertEquals(backing.size(), view.size());
        assertEquals(backing.isEmpty(), view.isEmpty());
        assertEquals(backing, view);
        assertEquals(view, backing);
        assertEquals(backing.hashCode(), view.hashCode());
        assertSameOrder(backing.comparator(), view.comparator(), backing.keySet());

        K someKey = backing.isEmpty() ? null : backing.firstKey();
        V someValue = backing.isEmpty() ? null : backing.get(someKey);
        if (someKey != null) {
            assertEquals(backing.firstKey(), view.firstKey());
            assertEquals(backing.lastKey(), view.lastKey());
            assertEquals(someValue, view.get(someKey));
            assertTrue(view.containsKey(someKey));
            assertTrue(view.containsValue(someValue));
        }
        V freshValue = (V) (Object) "fresh";
        assertAddRejected(() -> view.put(freshKey, freshValue));
        assertAddRejected(() -> view.putAll(Map.of(freshKey, freshValue)));
        assertAddRejected(() -> view.putIfAbsent(freshKey, freshValue));
        assertAddRejected(() -> view.computeIfAbsent(freshKey, k -> freshValue));
        assertAddRejected(() -> view.compute(freshKey, (k, v) -> freshValue));
        assertAddRejected(() -> view.merge(freshKey, freshValue, (a, b) -> b));
        assertUnsupported(() -> view.putFirst(freshKey, freshValue));
        assertUnsupported(() -> view.putLast(freshKey, freshValue));
        if (someKey != null) {
            assertUnsupported(view::clear);
            assertUnsupported(() -> view.keySet().clear());
            assertUnsupported(() -> view.values().clear());
            assertUnsupported(() -> view.entrySet().clear());
            assertUnsupported(() -> view.sequencedKeySet().clear());
            assertUnsupported(() -> view.sequencedValues().clear());
            assertUnsupported(() -> view.sequencedEntrySet().clear());
            // An unmodifiable SortedMap in the JDK inherits these two from SequencedMap, which only removes
            // (and so only throws) when the map is not empty.
            assertUnsupported(view::pollFirstEntry);
            assertUnsupported(view::pollLastEntry);
            assertUnsupported(() -> view.put(someKey, freshValue));
            assertUnsupported(() -> view.remove(someKey));
            assertUnsupported(() -> view.replace(someKey, freshValue));
            assertUnsupported(() -> view.replaceAll((k, v) -> freshValue));
            assertUnsupported(() -> view.computeIfPresent(someKey, (k, v) -> null));
            assertUnsupported(() -> view.keySet().remove(someKey));
            assertUnsupported(() -> view.values().remove(someValue));
            assertUnsupported(() -> view.entrySet().remove(view.entrySet().iterator().next()));
            assertUnsupported(() -> view.keySet().removeIf(k -> true));
            assertUnsupported(() -> view.values().removeIf(v -> true));
            assertUnsupported(() -> view.entrySet().removeIf(e -> true));
            assertUnsupported(() -> view.entrySet().iterator().next().setValue(freshValue));
            assertUnsupported(() -> view.sequencedEntrySet().iterator().next().setValue(freshValue));
            assertUnsupported(() -> view.sequencedEntrySet().reversed().iterator().next().setValue(freshValue));
            assertUnsupported(() -> view.firstEntry().setValue(freshValue));
            assertUnsupported(() -> view.lastEntry().setValue(freshValue));
            assertUnsupported(() -> removeFirstThroughIterator(view.keySet().iterator()));
            assertUnsupported(() -> removeFirstThroughIterator(view.values().iterator()));
            assertUnsupported(() -> removeFirstThroughIterator(view.entrySet().iterator()));
            assertUnsupported(() -> removeFirstThroughIterator(view.sequencedKeySet().iterator()));
            assertUnsupported(() -> removeFirstThroughIterator(view.sequencedValues().iterator()));
            assertUnsupported(() -> removeFirstThroughIterator(view.sequencedEntrySet().iterator()));
            assertUnsupported(() -> view.sequencedKeySet().removeFirst());
            assertUnsupported(() -> view.sequencedValues().removeLast());
            assertUnsupported(() -> view.sequencedValues().reversed().removeFirst());
            assertUnsupported(() -> view.sequencedEntrySet().reversed().removeFirst());
        }
        if (view instanceof NavigableMap) {
            var nview = (NavigableMap<K, V>) view;
            var nbacking = (NavigableMap<K, V>) backing;
            checkUnmodifiableSet(nview.navigableKeySet(), nbacking.navigableKeySet(), freshKey, 0);
            checkUnmodifiableSet(nview.descendingKeySet(), nbacking.descendingKeySet(), freshKey, 0);
            // Only on a whole TreeMap: TeaVM's TreeMap sub-map views do not implement lowerEntry, floorKey and
            // the rest yet (they return null).
            if (someKey instanceof Integer && depth == VIEW_DEPTH) {
                for (int probe : PROBES) {
                    K k = (K) (Object) probe;
                    assertEquals(nbacking.lowerKey(k), nview.lowerKey(k));
                    assertEquals(nbacking.floorKey(k), nview.floorKey(k));
                    assertEquals(nbacking.ceilingKey(k), nview.ceilingKey(k));
                    assertEquals(nbacking.higherKey(k), nview.higherKey(k));
                    checkUnmodifiableEntry(nview.lowerEntry(k), nbacking.lowerEntry(k));
                    checkUnmodifiableEntry(nview.floorEntry(k), nbacking.floorEntry(k));
                    checkUnmodifiableEntry(nview.ceilingEntry(k), nbacking.ceilingEntry(k));
                    checkUnmodifiableEntry(nview.higherEntry(k), nbacking.higherEntry(k));
                }
            }
            // TeaVM's TreeMap sub-map views throw NullPointerException from firstEntry when empty.
            if (someKey != null) {
                checkUnmodifiableEntry(nview.firstEntry(), nbacking.firstEntry());
                checkUnmodifiableEntry(nview.lastEntry(), nbacking.lastEntry());
            }
        }
        assertEquals(before, new ArrayList<>(backing.entrySet()));

        if (depth == 0) {
            return;
        }
        int next = depth - 1;
        // Called through SequencedMap: TeaVM's SortedMap lacks the covariant SortedMap.reversed() override.
        checkUnmodifiableMap((SortedMap<K, V>) ((SequencedMap<K, V>) view).reversed(),
                (SortedMap<K, V>) ((SequencedMap<K, V>) backing).reversed(), freshKey, next);
        if (someKey != null) {
            K lastKey = backing.lastKey();
            checkUnmodifiableMap(view.headMap(lastKey), backing.headMap(lastKey), freshKey, next);
            checkUnmodifiableMap(view.tailMap(lastKey), backing.tailMap(lastKey), freshKey, next);
            checkUnmodifiableMap(view.subMap(someKey, lastKey), backing.subMap(someKey, lastKey), freshKey, next);
        }
        if (view instanceof NavigableMap) {
            var nview = (NavigableMap<K, V>) view;
            var nbacking = (NavigableMap<K, V>) backing;
            checkUnmodifiableMap(nview.descendingMap(), nbacking.descendingMap(), freshKey, next);
            if (someKey != null) {
                K lastKey = backing.lastKey();
                checkUnmodifiableMap(nview.headMap(someKey, true), nbacking.headMap(someKey, true), freshKey, next);
                checkUnmodifiableMap(nview.tailMap(someKey, false), nbacking.tailMap(someKey, false), freshKey, next);
                checkUnmodifiableMap(nview.subMap(someKey, true, lastKey, true),
                        nbacking.subMap(someKey, true, lastKey, true), freshKey, next);
            }
        }
    }

    // TeaVM's TreeMap creates a new reversed comparator for each call, so compare what the comparators do.
    private static <E> void assertSameOrder(Comparator<? super E> expected, Comparator<? super E> actual,
            Collection<E> elements) {
        if (expected == null) {
            assertNull(actual);
            return;
        }
        for (E a : elements) {
            for (E b : elements) {
                assertEquals(Integer.signum(expected.compare(a, b)), Integer.signum(actual.compare(a, b)));
            }
        }
    }

    private static <K, V> void checkUnmodifiableEntry(Map.Entry<K, V> view, Map.Entry<K, V> backing) {
        if (backing == null) {
            assertNull(view);
            return;
        }
        assertEquals(backing, view);
        assertUnsupported(() -> view.setValue(null));
    }

    private static void removeFirstThroughIterator(Iterator<?> iterator) {
        iterator.next();
        iterator.remove();
    }

    private static <E> List<E> toList(Iterator<E> iterator) {
        var result = new ArrayList<E>();
        iterator.forEachRemaining(result::add);
        return result;
    }
}
