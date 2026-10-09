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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
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
    public void unmodifiableSortedSetIsReadOnlyView() {
        SortedSet<Integer> source = new TreeSet<>(Arrays.asList(5, 1, 3));
        SortedSet<Integer> set = Collections.unmodifiableSortedSet(source);
        assertEquals(3, set.size());
        assertEquals((Integer) 1, set.first());
        assertEquals((Integer) 5, set.last());
        assertNull(set.comparator());
        assertTrue(set.contains(3));
        assertArrayEquals(new Integer[] { 1, 3, 5 }, set.toArray(new Integer[0]));

        source.add(4);
        assertEquals(4, set.size());
        assertEquals(source, set);
        assertEquals(source.hashCode(), set.hashCode());
        assertEquals(source.toString(), set.toString());

        assertThrows(UnsupportedOperationException.class, () -> set.add(2));
        assertThrows(UnsupportedOperationException.class, () -> set.addAll(Arrays.asList(7, 8)));
        assertThrows(UnsupportedOperationException.class, () -> set.remove(1));
        assertThrows(UnsupportedOperationException.class, () -> set.remove(100));
        assertThrows(UnsupportedOperationException.class, () -> set.removeAll(Arrays.asList(1)));
        assertThrows(UnsupportedOperationException.class, () -> set.retainAll(Arrays.asList(1)));
        assertThrows(UnsupportedOperationException.class, () -> set.removeIf(x -> true));
        assertThrows(UnsupportedOperationException.class, set::clear);
        assertThrows(UnsupportedOperationException.class, set::removeFirst);
        assertThrows(UnsupportedOperationException.class, set::removeLast);
        Iterator<Integer> iterator = set.iterator();
        iterator.next();
        assertThrows(UnsupportedOperationException.class, iterator::remove);
        assertEquals(4, source.size());
    }

    @Test
    public void unmodifiableSortedSetSubViewsAreReadOnly() {
        SortedSet<Integer> set = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(1, 3, 5, 7)));
        assertEquals(new TreeSet<>(Arrays.asList(1, 3)), set.headSet(5));
        assertEquals(new TreeSet<>(Arrays.asList(5, 7)), set.tailSet(5));
        assertEquals(new TreeSet<>(Arrays.asList(3, 5)), set.subSet(3, 7));
        assertThrows(UnsupportedOperationException.class, () -> set.headSet(5).add(0));
        assertThrows(UnsupportedOperationException.class, () -> set.tailSet(5).clear());
        assertThrows(UnsupportedOperationException.class, () -> set.subSet(3, 7).remove(3));
    }

    @Test
    public void unmodifiableNavigableSet() {
        NavigableSet<Integer> source = new TreeSet<>(Arrays.asList(5, 1, 3));
        NavigableSet<Integer> set = Collections.unmodifiableNavigableSet(source);
        assertEquals((Integer) 1, set.lower(3));
        assertEquals((Integer) 3, set.floor(3));
        assertEquals((Integer) 5, set.ceiling(4));
        assertNull(set.higher(5));
        assertThrows(UnsupportedOperationException.class, set::pollFirst);
        assertThrows(UnsupportedOperationException.class, set::pollLast);
        assertThrows(UnsupportedOperationException.class, () -> set.add(2));
        assertEquals(3, source.size());

        NavigableSet<Integer> descending = set.descendingSet();
        assertArrayEquals(new Integer[] { 5, 3, 1 }, descending.toArray(new Integer[0]));
        assertThrows(UnsupportedOperationException.class, () -> descending.add(2));
        Iterator<Integer> iterator = set.descendingIterator();
        assertEquals((Integer) 5, iterator.next());
        assertThrows(UnsupportedOperationException.class, iterator::remove);

        assertEquals(new TreeSet<>(Arrays.asList(1, 3)), set.subSet(1, true, 5, false));
        assertEquals(new TreeSet<>(Arrays.asList(1, 3, 5)), set.headSet(5, true));
        assertEquals(new TreeSet<>(Arrays.asList(3, 5)), set.tailSet(3, true));
        assertThrows(UnsupportedOperationException.class, () -> set.headSet(5, true).add(0));
        assertThrows(UnsupportedOperationException.class, () -> set.reversed().add(0));
    }

    @Test
    public void unmodifiableSortedMapIsReadOnlyView() {
        TreeMap<String, Integer> source = new TreeMap<>();
        source.put("b", 2);
        source.put("a", 1);
        source.put("c", 3);
        SortedMap<String, Integer> map = Collections.unmodifiableSortedMap(source);
        assertEquals(3, map.size());
        assertEquals("a", map.firstKey());
        assertEquals("c", map.lastKey());
        assertNull(map.comparator());
        assertEquals((Integer) 2, map.get("b"));
        assertTrue(map.containsKey("c"));
        assertTrue(map.containsValue(3));
        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(map.keySet()));
        assertEquals(Arrays.asList(1, 2, 3), new ArrayList<>(map.values()));

        source.put("d", 4);
        assertEquals(4, map.size());
        assertEquals(source, map);
        assertEquals(source.hashCode(), map.hashCode());
        assertEquals(source.toString(), map.toString());

        assertThrows(UnsupportedOperationException.class, () -> map.put("e", 5));
        assertThrows(UnsupportedOperationException.class, () -> map.remove("a"));
        assertThrows(UnsupportedOperationException.class, () -> map.remove("zz"));
        assertThrows(UnsupportedOperationException.class, () -> map.putAll(Map.of("e", 5)));
        assertThrows(UnsupportedOperationException.class, map::clear);
        assertThrows(UnsupportedOperationException.class, () -> map.keySet().remove("a"));
        assertThrows(UnsupportedOperationException.class, () -> map.values().remove(1));
        assertThrows(UnsupportedOperationException.class, () -> map.entrySet().iterator().next().setValue(9));
        assertEquals(4, source.size());
    }

    @Test
    public void unmodifiableSortedMapSubViewsAreReadOnly() {
        TreeMap<String, Integer> source = new TreeMap<>(Map.of("a", 1, "b", 2, "c", 3, "d", 4));
        SortedMap<String, Integer> map = Collections.unmodifiableSortedMap(source);
        assertEquals(new TreeMap<>(Map.of("a", 1, "b", 2)), map.headMap("c"));
        assertEquals(new TreeMap<>(Map.of("c", 3, "d", 4)), map.tailMap("c"));
        assertEquals(new TreeMap<>(Map.of("b", 2, "c", 3)), map.subMap("b", "d"));
        assertThrows(UnsupportedOperationException.class, () -> map.headMap("c").put("0", 0));
        assertThrows(UnsupportedOperationException.class, () -> map.tailMap("c").clear());
        assertThrows(UnsupportedOperationException.class, () -> map.subMap("b", "d").remove("b"));
    }

    @Test
    public void unmodifiableNavigableMap() {
        TreeMap<String, Integer> source = new TreeMap<>(Map.of("a", 1, "b", 2, "c", 3));
        NavigableMap<String, Integer> map = Collections.unmodifiableNavigableMap(source);
        assertEquals("a", map.lowerKey("b"));
        assertEquals((Integer) 2, map.floorEntry("b").getValue());
        assertEquals("c", map.ceilingKey("bb"));
        assertNull(map.higherKey("c"));
        assertNull(map.lowerEntry("a"));
        assertEquals("a", map.firstEntry().getKey());
        assertEquals("c", map.lastEntry().getKey());
        assertThrows(UnsupportedOperationException.class, () -> map.firstEntry().setValue(9));
        assertThrows(UnsupportedOperationException.class, () -> map.floorEntry("b").setValue(9));
        assertThrows(UnsupportedOperationException.class, map::pollFirstEntry);
        assertThrows(UnsupportedOperationException.class, map::pollLastEntry);
        assertThrows(UnsupportedOperationException.class, () -> map.put("d", 4));
        assertEquals(3, source.size());

        NavigableMap<String, Integer> descending = map.descendingMap();
        assertEquals("c", descending.firstKey());
        assertThrows(UnsupportedOperationException.class, () -> descending.put("d", 4));
        assertEquals("c", map.reversed().firstKey());
        assertEquals(Arrays.asList("c", "b", "a"), new ArrayList<>(map.descendingKeySet()));
        assertThrows(UnsupportedOperationException.class, () -> map.navigableKeySet().pollFirst());
        assertThrows(UnsupportedOperationException.class, () -> map.descendingKeySet().add("d"));

        assertEquals(new TreeMap<>(Map.of("a", 1, "b", 2)), map.subMap("a", true, "c", false));
        assertEquals(new TreeMap<>(Map.of("a", 1)), map.headMap("b", false));
        assertEquals(new TreeMap<>(Map.of("b", 2, "c", 3)), map.tailMap("b", true));
        assertThrows(UnsupportedOperationException.class, () -> map.headMap("b", true).put("0", 0));
    }

    @Test
    public void unmodifiableSequencedViewsOfMapAreReadOnly() {
        NavigableMap<String, Integer> map = Collections.unmodifiableNavigableMap(
                new TreeMap<>(Map.of("a", 1, "b", 2, "c", 3)));
        assertEquals(Arrays.asList("c", "b", "a"), new ArrayList<>(map.sequencedKeySet().reversed()));
        assertEquals(Arrays.asList(3, 2, 1), new ArrayList<>(map.sequencedValues().reversed()));
        assertThrows(UnsupportedOperationException.class, () -> map.sequencedKeySet().remove("a"));
        assertThrows(UnsupportedOperationException.class, () -> map.sequencedValues().remove(1));
        assertThrows(UnsupportedOperationException.class,
                () -> map.sequencedEntrySet().iterator().next().setValue(9));
        assertThrows(UnsupportedOperationException.class,
                () -> map.sequencedEntrySet().reversed().iterator().next().setValue(9));
    }

    @Test
    public void synchronizedSortedAndNavigableCollections() {
        SortedSet<String> sortedSet = Collections.synchronizedSortedSet(new TreeSet<>(Arrays.asList("b", "a")));
        sortedSet.add("c");
        assertEquals("a", sortedSet.first());
        assertEquals("c", sortedSet.last());
        assertEquals(3, sortedSet.size());

        NavigableSet<String> navigableSet = Collections.synchronizedNavigableSet(
                new TreeSet<>(Arrays.asList("b", "a")));
        assertEquals("a", navigableSet.lower("b"));
        assertEquals("b", navigableSet.pollLast());
        assertEquals(1, navigableSet.size());

        TreeMap<String, Integer> source = new TreeMap<>(Map.of("a", 1, "b", 2));
        SortedMap<String, Integer> sortedMap = Collections.synchronizedSortedMap(source);
        sortedMap.put("c", 3);
        assertEquals(3, source.size());
        assertEquals("c", sortedMap.lastKey());
        assertEquals(new TreeMap<>(Map.of("a", 1, "b", 2)), sortedMap.headMap("c"));

        NavigableMap<String, Integer> navigableMap = Collections.synchronizedNavigableMap(new TreeMap<>(source));
        assertEquals("a", navigableMap.firstKey());
        assertEquals("b", navigableMap.lowerKey("c"));
        assertEquals("c", navigableMap.pollLastEntry().getKey());
        assertEquals(2, navigableMap.size());
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void checkedSortedAndNavigableSetsRejectWrongType() {
        SortedSet<String> sortedSet = Collections.checkedSortedSet(new TreeSet<>(), String.class);
        sortedSet.add("a");
        sortedSet.addAll(Arrays.asList("b", "c"));
        assertEquals("a", sortedSet.first());
        assertEquals("c", sortedSet.last());
        assertEquals(new TreeSet<>(Arrays.asList("a", "b", "c")), sortedSet);
        SortedSet rawSorted = sortedSet;
        assertThrows(ClassCastException.class, () -> rawSorted.add(1));
        assertThrows(ClassCastException.class, () -> rawSorted.addAll(Arrays.asList("d", 2)));
        SortedSet rawHead = sortedSet.headSet("c");
        assertThrows(ClassCastException.class, () -> rawHead.add(1));
        SortedSet rawTail = sortedSet.tailSet("a");
        assertThrows(ClassCastException.class, () -> rawTail.add(1));
        SortedSet rawSub = sortedSet.subSet("a", "c");
        assertThrows(ClassCastException.class, () -> rawSub.add(1));
        assertEquals(3, sortedSet.size());

        NavigableSet<String> navigableSet = Collections.checkedNavigableSet(
                new TreeSet<>(Arrays.asList("a", "b", "c")), String.class);
        assertEquals("a", navigableSet.lower("b"));
        assertEquals("b", navigableSet.ceiling("b"));
        assertEquals("a", navigableSet.pollFirst());
        NavigableSet rawNavigable = navigableSet;
        assertThrows(ClassCastException.class, () -> rawNavigable.add(1));
        NavigableSet rawDescending = navigableSet.descendingSet();
        assertEquals("c", rawDescending.first());
        assertThrows(ClassCastException.class, () -> rawDescending.add(1));
        NavigableSet rawSubSet = navigableSet.subSet("b", true, "c", true);
        assertThrows(ClassCastException.class, () -> rawSubSet.add(1));
        NavigableSet rawHeadSet = navigableSet.headSet("c", true);
        assertThrows(ClassCastException.class, () -> rawHeadSet.add(1));
        NavigableSet rawTailSet = navigableSet.tailSet("b", true);
        assertThrows(ClassCastException.class, () -> rawTailSet.add(1));
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void checkedSortedAndNavigableMapsRejectWrongType() {
        SortedMap<String, Integer> sortedMap = Collections.checkedSortedMap(new TreeMap<>(), String.class,
                Integer.class);
        sortedMap.put("a", 1);
        sortedMap.putAll(Map.of("b", 2, "c", 3));
        assertEquals("a", sortedMap.firstKey());
        assertEquals("c", sortedMap.lastKey());
        assertEquals(new TreeMap<>(Map.of("a", 1, "b", 2, "c", 3)), sortedMap);
        SortedMap rawSorted = sortedMap;
        assertThrows(ClassCastException.class, () -> rawSorted.put(1, 1));
        assertThrows(ClassCastException.class, () -> rawSorted.put("d", "x"));
        SortedMap rawHead = sortedMap.headMap("c");
        assertThrows(ClassCastException.class, () -> rawHead.put(1, 1));
        SortedMap rawTail = sortedMap.tailMap("a");
        assertThrows(ClassCastException.class, () -> rawTail.put("d", "x"));
        SortedMap rawSub = sortedMap.subMap("a", "c");
        assertThrows(ClassCastException.class, () -> rawSub.put(1, 1));
        assertEquals(3, sortedMap.size());

        NavigableMap<String, Integer> navigableMap = Collections.checkedNavigableMap(
                new TreeMap<>(Map.of("a", 1, "b", 2, "c", 3)), String.class, Integer.class);
        assertEquals("a", navigableMap.lowerKey("b"));
        assertEquals((Integer) 2, navigableMap.ceilingEntry("b").getValue());
        assertEquals("a", navigableMap.pollFirstEntry().getKey());
        NavigableMap rawNavigable = navigableMap;
        assertThrows(ClassCastException.class, () -> rawNavigable.put(1, 1));
        NavigableMap rawDescending = navigableMap.descendingMap();
        assertEquals("c", rawDescending.firstKey());
        assertThrows(ClassCastException.class, () -> rawDescending.put("d", "x"));
        NavigableMap rawReversed = navigableMap.reversed();
        assertThrows(ClassCastException.class, () -> rawReversed.put(1, 1));
        NavigableMap rawSubMap = navigableMap.subMap("b", true, "c", true);
        assertThrows(ClassCastException.class, () -> rawSubMap.put(1, 1));
        NavigableMap rawHeadMap = navigableMap.headMap("c", true);
        assertThrows(ClassCastException.class, () -> rawHeadMap.put("d", "x"));
        NavigableMap rawTailMap = navigableMap.tailMap("b", true);
        assertThrows(ClassCastException.class, () -> rawTailMap.put(1, 1));
        NavigableSet rawKeys = navigableMap.navigableKeySet();
        assertThrows(ClassCastException.class, () -> rawKeys.add(1));
    }

    @Test
    public void emptySortedAndNavigableCollections() {
        SortedSet<String> sortedSet = Collections.emptySortedSet();
        assertTrue(sortedSet.isEmpty());
        assertNull(sortedSet.comparator());
        assertThrows(NoSuchElementException.class, sortedSet::first);
        assertThrows(NoSuchElementException.class, sortedSet::last);
        assertThrows(UnsupportedOperationException.class, () -> sortedSet.add("a"));
        assertFalse(sortedSet.iterator().hasNext());

        NavigableSet<String> navigableSet = Collections.emptyNavigableSet();
        assertTrue(navigableSet.isEmpty());
        assertNull(navigableSet.lower("a"));
        assertNull(navigableSet.ceiling("a"));
        assertThrows(UnsupportedOperationException.class, () -> navigableSet.add("a"));
        assertTrue(navigableSet.descendingSet().isEmpty());

        SortedMap<String, Integer> sortedMap = Collections.emptySortedMap();
        assertTrue(sortedMap.isEmpty());
        assertNull(sortedMap.comparator());
        assertNull(sortedMap.get("a"));
        assertThrows(NoSuchElementException.class, sortedMap::firstKey);
        assertThrows(NoSuchElementException.class, sortedMap::lastKey);
        assertThrows(UnsupportedOperationException.class, () -> sortedMap.put("a", 1));

        NavigableMap<String, Integer> navigableMap = Collections.emptyNavigableMap();
        assertTrue(navigableMap.isEmpty());
        assertNull(navigableMap.firstEntry());
        assertNull(navigableMap.lowerKey("a"));
        assertTrue(navigableMap.descendingMap().isEmpty());
        assertTrue(navigableMap.navigableKeySet().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> navigableMap.put("a", 1));
    }
}
