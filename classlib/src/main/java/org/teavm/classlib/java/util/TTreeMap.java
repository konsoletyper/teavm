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
/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teavm.classlib.java.util;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TCloneable;

public class TTreeMap<K, V> extends TAbstractMap<K, V> implements TNavigableMap<K, V>, TCloneable, TSerializable {
    private int size;
    private Node<K, V> root;
    private TComparator<? super K> comparator;
    private int modCount;
    private TSet<TMap.Entry<K, V>> entrySet;
    private TNavigableMap<K, V> descendingMap;
    private TNavigableSet<K> navigableKeySet;

    static class Node<K, V> implements Cloneable {
        static final int NODE_SIZE = 64;

        Node<K, V> prev;
        Node<K, V> next;
        Node<K, V> parent;
        Node<K, V> left;
        Node<K, V> right;
        V[] values;
        K[] keys;
        int leftIdx;
        int rightIdx = -1;
        int size;
        boolean color;

        @SuppressWarnings("unchecked")
        Node() {
            keys = (K[]) new Object[NODE_SIZE];
            values = (V[]) new Object[NODE_SIZE];
        }

        @SuppressWarnings("unchecked")
        Node<K, V> clone(Node<K, V> parent) throws CloneNotSupportedException {
            Node<K, V> clone = (Node<K, V>) super.clone();
            clone.keys = keys.clone();
            clone.values = values.clone();
            clone.parent = parent;
            if (left != null) {
                clone.left = left.clone(clone);
            }
            if (right != null) {
                clone.right = right.clone(clone);
            }
            clone.prev = null;
            clone.next = null;
            return clone;
        }
    }

    static class Entry<K, V> extends TMapEntry<K, V> {
        Node<K, V> node;
        int index;

        Entry(Node<K, V> node, int index) {
            super(node.keys[index], node.values[index]);
            this.node = node;
            this.index = index;
        }

        @Override
        public V setValue(V object) {
            V result = value;
            value = object;
            this.node.values[index] = value;
            return result;
        }
    }

    private static abstract class AbstractSubMapIterator<K, V> {
        final NavigableSubMap<K, V> subMap;

        int expectedModCount;

        Node<K, V> node;

        Node<K, V> lastNode;

        Entry<K, V> boundaryPair;

        int offset;

        int lastOffset;

        AbstractSubMapIterator(final NavigableSubMap<K, V> map) {
            subMap = map;
            expectedModCount = subMap.map.modCount;
        }

        public void remove() {
            if (lastNode == null) {
                throw new IllegalStateException();
            }
            if (expectedModCount != subMap.map.modCount) {
                throw new TConcurrentModificationException();
            }

            Entry<K, V> entry;
            int idx = lastOffset;
            if (idx == lastNode.leftIdx) {
                entry = subMap.map.removeLeftmost(lastNode, false);
            } else if (idx == lastNode.rightIdx) {
                entry = subMap.map.removeRightmost(lastNode, false);
            } else {
                entry = subMap.map.removeMiddleElement(lastNode, idx, false);
            }
            if (entry != null) {
                node = entry.node;
                offset = entry.index;
                boundaryPair = getBoundaryNode();
            } else {
                node = null;
            }
            if (node != null && !this.subMap.isInRange(node.keys[offset])) {
                node = null;
            }
            lastNode = null;
            expectedModCount++;
        }

        abstract Entry<K, V> getBoundaryNode();
    }

    private abstract static class AscendingSubMapIterator<K, V, T>
            extends AbstractSubMapIterator<K, V> implements TIterator<T> {

        AscendingSubMapIterator(NavigableSubMap<K, V> map) {
            super(map);
            Entry<K, V> entry = map.findStartNode();
            if (entry != null && map.checkUpperBound(entry.key)) {
                node = entry.node;
                offset = entry.index;
                boundaryPair = getBoundaryNode();
            }
        }

        @Override
        final TTreeMap.Entry<K, V> getBoundaryNode() {
            if (subMap.toEnd) {
                return subMap.hiInclusive ? subMap.smallerOrEqualEntry(subMap.hi) : subMap.smallerEntry(subMap.hi);
            }
            return subMap.theBiggestEntry();
        }

        @Override
        public T next() {
            if (node == null) {
                throw new TNoSuchElementException();
            }
            if (expectedModCount != subMap.map.modCount) {
                throw new TConcurrentModificationException();
            }

            lastNode = node;
            lastOffset = offset;
            if (offset != node.rightIdx) {
                offset++;
            } else {
                node = node.next;
                if (node != null) {
                    offset = node.leftIdx;
                }
            }
            // boundaryPair = getBoundaryNode();
            if (boundaryPair != null && boundaryPair.node == lastNode && boundaryPair.index == lastOffset) {
                node = null;
            }
            return export(lastNode, lastOffset);
        }

        abstract T export(Node<K, V> node, int offset);

        @Override
        public final boolean hasNext() {
            return null != node;
        }
    }

    static class AscendingSubMapEntryIterator<K, V> extends AscendingSubMapIterator<K, V, TMap.Entry<K, V>> {
        AscendingSubMapEntryIterator(NavigableSubMap<K, V> map) {
            super(map);
        }

        @Override
        TMap.Entry<K, V> export(Node<K, V> node, int offset) {
            return newEntry(node, offset);
        }
    }

    static class AscendingSubMapKeyIterator<K, V> extends AscendingSubMapIterator<K, V, K> {
        AscendingSubMapKeyIterator(NavigableSubMap<K, V> map) {
            super(map);
        }

        @Override
        K export(Node<K, V> node, int offset) {
            return node.keys[offset];
        }
    }

    static class AscendingSubMapValueIterator<K, V> extends AscendingSubMapIterator<K, V, V> {
        AscendingSubMapValueIterator(NavigableSubMap<K, V> map) {
            super(map);
        }

        @Override
        V export(Node<K, V> node, int offset) {
            return node.values[offset];
        }
    }

    private abstract static class DescendingSubMapIterator<K, V, T>
            extends AbstractSubMapIterator<K, V> implements TIterator<T> {

        DescendingSubMapIterator(NavigableSubMap<K, V> map) {
            super(map);
            TTreeMap.Entry<K, V> entry;
            if (map.fromStart) {
                entry = map.loInclusive ? map.map.findFloorEntry(map.lo) : map.map.findLowerEntry(map.lo);
            } else {
                entry = map.map.findBiggestEntry();
            }
            if (entry != null) {
                if (!map.isInRange(entry.key)) {
                    node = null;
                    return;
                }
                node = entry.node;
                offset = entry.index;
            } else {
                node = null;
                return;
            }
            boundaryPair = getBoundaryNode();
            if (boundaryPair != null) {
                if (map.map.keyCompare(boundaryPair.key, entry.key) > 0) {
                    node = null;
                }
            }
            if (map.toEnd && !map.hiInclusive) {
                // the last element may be the same with first one but it is not included
                if (map.map.keyCompare(map.hi, entry.key) == 0) {
                    node = null;
                }
            }
        }

        @Override
        final TTreeMap.Entry<K, V> getBoundaryNode() {
            if (subMap.toEnd) {
                return subMap.hiInclusive ? subMap.map.findCeilingEntry(subMap.hi)
                        : subMap.map.findHigherEntry(subMap.hi);
            }
            return subMap.map.findSmallestEntry();
        }

        @Override
        public T next() {
            if (node == null) {
                throw new TNoSuchElementException();
            }
            if (expectedModCount != subMap.map.modCount) {
                throw new TConcurrentModificationException();
            }

            lastNode = node;
            lastOffset = offset;
            if (offset != node.leftIdx) {
                offset--;
            } else {
                node = node.prev;
                if (node != null) {
                    offset = node.rightIdx;
                }
            }
            // boundaryPair = getBoundaryNode();
            if (boundaryPair != null && boundaryPair.node == lastNode && boundaryPair.index == lastOffset) {
                node = null;
            }
            return export(lastNode, lastOffset);
        }

        abstract T export(Node<K, V> node, int offset);

        @Override
        public final boolean hasNext() {
            return node != null;
        }

        @Override
        public final void remove() {
            if (lastNode == null) {
                throw new IllegalStateException();
            }
            if (expectedModCount != subMap.map.modCount) {
                throw new TConcurrentModificationException();
            }

            Entry<K, V> entry;
            int idx = lastOffset;
            if (idx == lastNode.leftIdx) {
                entry = subMap.map.removeLeftmost(lastNode, true);
            } else if (idx == lastNode.rightIdx) {
                entry = subMap.map.removeRightmost(lastNode, true);
            } else {
                entry = subMap.map.removeMiddleElement(lastNode, idx, true);
            }
            if (entry != null) {
                node = entry.node;
                offset = entry.index;
                boundaryPair = getBoundaryNode();
            } else {
                node = null;
            }
            if (node != null && !this.subMap.isInRange(node.keys[offset])) {
                node = null;
            }
            lastNode = null;
            expectedModCount++;
        }
    }

    static class DescendingSubMapEntryIterator<K, V> extends DescendingSubMapIterator<K, V, TMap.Entry<K, V>> {
        DescendingSubMapEntryIterator(NavigableSubMap<K, V> map) {
            super(map);
        }

        @Override
        TMap.Entry<K, V> export(Node<K, V> node, int offset) {
            return newEntry(node, offset);
        }
    }

    static class DescendingSubMapKeyIterator<K, V> extends DescendingSubMapIterator<K, V, K> {
        DescendingSubMapKeyIterator(NavigableSubMap<K, V> map) {
            super(map);
        }

        @Override
        K export(Node<K, V> node, int offset) {
            return node.keys[offset];
        }
    }

    static class DescendingSubMapValueIterator<K, V> extends DescendingSubMapIterator<K, V, V> {
        DescendingSubMapValueIterator(NavigableSubMap<K, V> map) {
            super(map);
        }

        @Override
        V export(Node<K, V> node, int offset) {
            return node.values[offset];
        }
    }

    static class AscendingSubMapEntrySet<K, V> extends TAbstractSet<TMap.Entry<K, V>> {
        NavigableSubMap<K, V> map;

        AscendingSubMapEntrySet(NavigableSubMap<K, V> map) {
            this.map = map;
        }

        @Override
        public final TIterator<TMap.Entry<K, V>> iterator() {
            return new AscendingSubMapEntryIterator<>(map);
        }

        @Override
        public int size() {
            int size = 0;
            TIterator<TMap.Entry<K, V>> it = new AscendingSubMapEntryIterator<>(map);
            while (it.hasNext()) {
                it.next();
                size++;
            }
            return size;
        }
    }

    static class DescendingSubMapEntrySet<K, V> extends TAbstractSet<TMap.Entry<K, V>> {
        NavigableSubMap<K, V> map;

        DescendingSubMapEntrySet(NavigableSubMap<K, V> map) {
            this.map = map;
        }

        @Override
        public final TIterator<TMap.Entry<K, V>> iterator() {
            return new DescendingSubMapEntryIterator<>(map);
        }

        @Override
        public int size() {
            int size = 0;
            TIterator<TMap.Entry<K, V>> it = new DescendingSubMapEntryIterator<>(map);
            while (it.hasNext()) {
                it.next();
                size++;
            }
            return size;
        }
    }

    static class AscendingSubMapKeySet<K, V> extends TAbstractSet<K> implements TNavigableSet<K> {
        NavigableSubMap<K, V> map;

        AscendingSubMapKeySet(NavigableSubMap<K, V> map) {
            this.map = map;
        }

        @Override
        public final TIterator<K> iterator() {
            return new AscendingSubMapKeyIterator<>(map);
        }

        @Override
        public final TIterator<K> descendingIterator() {
            return new DescendingSubMapKeyIterator<>(map.descendingSubMap());
        }

        @Override
        public int size() {
            int size = 0;
            TIterator<TMap.Entry<K, V>> it = new AscendingSubMapEntryIterator<>(map);
            while (it.hasNext()) {
                it.next();
                size++;
            }
            return size;
        }

        @Override
        public K ceiling(K e) {
            TTreeMap.Entry<K, V> ret = map.findCeilingEntry(e);
            if (ret != null && map.isInRange(ret.key)) {
                return ret.key;
            } else {
                return null;
            }
        }

        @Override
        public TNavigableSet<K> descendingSet() {
            return new DescendingSubMapKeySet<>(map.descendingSubMap());
        }

        @Override
        public K floor(K e) {
            Entry<K, V> ret = map.findFloorEntry(e);
            if (ret != null && map.isInRange(ret.key)) {
                return ret.key;
            } else {
                return null;
            }
        }

        @Override
        public TNavigableSet<K> headSet(K end, boolean endInclusive) {
            boolean isInRange = true;
            int result;
            if (map.toEnd) {
                result = (null != comparator()) ? comparator().compare(end, map.hi)
                        : toComparable(end).compareTo(map.hi);
                isInRange = (map.hiInclusive || !endInclusive) ? result <= 0 : result < 0;
            }
            if (map.fromStart) {
                result = (null != comparator()) ? comparator().compare(end, map.lo)
                        : toComparable(end).compareTo(map.lo);
                isInRange = isInRange && ((map.loInclusive || !endInclusive) ? result >= 0 : result > 0);
            }
            if (isInRange) {
                if (map.fromStart) {
                    return new AscendingSubMapKeySet<>(
                            new AscendingSubMap<>(map.lo, map.loInclusive, map.map, end, endInclusive));
                } else {
                    return new AscendingSubMapKeySet<>(new AscendingSubMap<>(map.map, end, endInclusive));
                }
            }
            throw new IllegalArgumentException();
        }

        @Override
        public K higher(K e) {
            K ret = map.map.higherKey(e);
            if (ret != null && map.isInRange(ret)) {
                return ret;
            } else {
                return null;
            }
        }

        @Override
        public K lower(K e) {
            K ret = map.map.lowerKey(e);
            if (ret != null && map.isInRange(ret)) {
                return ret;
            } else {
                return null;
            }
        }

        @Override
        public K pollFirst() {
            TMap.Entry<K, V> ret = map.firstEntry();
            if (ret == null) {
                return null;
            }
            map.map.remove(ret.getKey());
            return ret.getKey();
        }

        @Override
        public K pollLast() {
            TMap.Entry<K, V> ret = map.lastEntry();
            if (ret == null) {
                return null;
            }
            map.map.remove(ret.getKey());
            return ret.getKey();
        }

        @Override
        public TNavigableSet<K> subSet(K start, boolean startInclusive, K end, boolean endInclusive) {
            if (map.fromStart && ((!map.loInclusive && startInclusive)
                    ? map.map.keyCompare(start, map.lo) <= 0 : map.map.keyCompare(start, map.lo) < 0)
                    || (map.toEnd && ((!map.hiInclusive && (endInclusive || (startInclusive && start.equals(end))))
                    ? map.map.keyCompare(end, map.hi) >= 0 : map.map.keyCompare(end, map.hi) > 0))) {
                throw new IllegalArgumentException();
            }
            if (map.map.keyCompare(start, end) > 0) {
                throw new IllegalArgumentException();
            }
            return new AscendingSubMapKeySet<>(
                    new AscendingSubMap<>(start, startInclusive, map.map, end, endInclusive));
        }

        @Override
        public TNavigableSet<K> tailSet(K start, boolean startInclusive) {
            boolean isInRange = true;
            int result;
            if (map.toEnd) {
                result = (null != comparator()) ? comparator().compare(start, map.hi)
                        : toComparable(start).compareTo(map.hi);
                isInRange = (map.hiInclusive || !startInclusive) ? result <= 0 : result < 0;
            }
            if (map.fromStart) {
                result = (null != comparator()) ? comparator().compare(start, map.lo)
                        : toComparable(start).compareTo(map.lo);
                isInRange = isInRange && ((map.loInclusive || !startInclusive) ? result >= 0 : result > 0);
            }

            if (isInRange) {
                if (map.toEnd) {
                    return new AscendingSubMapKeySet<>(
                            new AscendingSubMap<>(start, startInclusive, map.map, map.hi, map.hiInclusive));
                } else {
                    return new AscendingSubMapKeySet<>(new AscendingSubMap<>(start, startInclusive, map.map));
                }
            }
            throw new IllegalArgumentException();
        }

        @Override
        public TComparator<? super K> comparator() {
            return map.map.comparator;
        }

        @Override
        public K first() {
            return map.firstKey();
        }

        @Override
        public TSortedSet<K> headSet(K end) {
            return headSet(end, false);
        }

        @Override
        public K last() {
            return map.lastKey();
        }

        @Override
        public TSortedSet<K> subSet(K start, K end) {
            return subSet(start, true, end, false);
        }

        @Override
        public TSortedSet<K> tailSet(K start) {
            return tailSet(start, true);
        }

        @Override
        public boolean contains(Object object) {
            return map.containsKey(object);
        }

        @Override
        public boolean remove(Object object) {
            return this.map.remove(object) != null;
        }
    }

    static class DescendingSubMapKeySet<K, V> extends TAbstractSet<K> implements TNavigableSet<K> {
        NavigableSubMap<K, V> map;

        DescendingSubMapKeySet(NavigableSubMap<K, V> map) {
            this.map = map;
        }

        @Override
        public final TIterator<K> iterator() {
            return new DescendingSubMapKeyIterator<>(map);
        }

        @Override
        public final TIterator<K> descendingIterator() {
            if (map.fromStart && map.toEnd) {
                return new AscendingSubMapKeyIterator<>(
                        new AscendingSubMap<>(map.hi, map.hiInclusive, map.map, map.lo, map.loInclusive));
            }
            if (map.toEnd) {
                return new AscendingSubMapKeyIterator<>(new AscendingSubMap<>(map.hi, map.hiInclusive, map.map));
            }
            if (map.fromStart) {
                return new AscendingSubMapKeyIterator<>(new AscendingSubMap<>(map.map, map.lo, map.loInclusive));
            }
            return new AscendingSubMapKeyIterator<>(new AscendingSubMap<>(map.map));
        }

        @Override
        public int size() {
            int size = 0;
            TIterator<TMap.Entry<K, V>> it = new DescendingSubMapEntryIterator<>(map);
            while (it.hasNext()) {
                it.next();
                size++;
            }
            return size;
        }

        @Override
        public TNavigableSet<K> descendingSet() {
            if (map.fromStart && map.toEnd) {
                return new AscendingSubMapKeySet<>(
                        new AscendingSubMap<>(map.hi, map.hiInclusive, map.map, map.lo, map.loInclusive));
            }
            if (map.toEnd) {
                return new AscendingSubMapKeySet<>(new AscendingSubMap<>(map.hi, map.hiInclusive, map.map));
            }
            if (map.fromStart) {
                return new AscendingSubMapKeySet<>(new AscendingSubMap<>(map.map, map.lo, map.loInclusive));
            }
            return new AscendingSubMapKeySet<>(new AscendingSubMap<>(map.map));
        }

        @Override
        public K ceiling(K e) {
            Comparable<K> object = map.comparator() == null ? toComparable(e) : null;
            Entry<K, V> node = map.map.findFloorEntry(e);
            if (node != null && !map.checkUpperBound(node.key)) {
                return null;
            }

            if (node != null && !map.checkLowerBound(node.key)) {
                Entry<K, V> first = map.loInclusive ? map.map.findFloorEntry(map.lo) : map.map.findLowerEntry(map.lo);
                if (first != null && map.cmp(object, e, first.key) <= 0 && map.checkUpperBound(first.key)) {
                    node = first;
                } else {
                    node = null;
                }
            }
            return node == null ? null : node.key;
        }

        @Override
        public K floor(K e) {
            Entry<K, V> node = map.map.findCeilingEntry(e);
            if (node != null && !map.checkUpperBound(node.key)) {
                node = map.hiInclusive ? map.map.findCeilingEntry(map.hi) : map.map.findHigherEntry(map.hi);
            }

            if (node != null && !map.checkLowerBound(node.key)) {
                Comparable<K> object = map.comparator() == null ? toComparable(e) : null;
                Entry<K, V> first = map.loInclusive ? map.map.findFloorEntry(map.lo) : map.map.findLowerEntry(map.lo);
                if (first != null && map.cmp(object, e, first.key) > 0 && map.checkUpperBound(first.key)) {
                    node = first;
                } else {
                    node = null;
                }
            }
            return node == null ? null : node.key;
        }

        @Override
        public TNavigableSet<K> headSet(K end, boolean endInclusive) {
            checkInRange(end, endInclusive);
            if (map.fromStart) {
                return new DescendingSubMapKeySet<>(
                        new DescendingSubMap<>(map.lo, map.loInclusive, map.map, end, endInclusive));
            } else {
                return new DescendingSubMapKeySet<>(new DescendingSubMap<>(map.map, end, endInclusive));
            }
        }

        @Override
        public K higher(K e) {
            Comparable<K> object = map.comparator() == null ? toComparable(e) : null;
            Entry<K, V> node = map.map.findLowerEntry(e);
            if (node != null && !map.checkUpperBound(node.key)) {
                return null;
            }

            if (node != null && !map.checkLowerBound(node.key)) {
                Entry<K, V> first = map.loInclusive ? map.map.findFloorEntry(map.lo) : map.map.findLowerEntry(map.lo);
                if (first != null && map.cmp(object, e, first.key) < 0 && map.checkUpperBound(first.key)) {
                    node = first;
                } else {
                    node = null;
                }
            }
            return node == null ? null : node.key;
        }

        @Override
        public K lower(K e) {
            Entry<K, V> node = map.map.findHigherEntry(e);
            if (node != null && !map.checkUpperBound(node.key)) {
                node = map.hiInclusive ? map.map.findCeilingEntry(map.hi) : map.map.findHigherEntry(map.hi);
            }

            if (node != null && !map.checkLowerBound(node.key)) {
                Comparable<K> object = map.comparator() == null ? toComparable(e) : null;
                Entry<K, V> first = map.loInclusive ? map.map.findFloorEntry(map.lo) : map.map.findLowerEntry(map.lo);
                if (first != null && map.cmp(object, e, first.key) > 0 && map.checkUpperBound(first.key)) {
                    node = first;
                } else {
                    node = null;
                }
            }
            return node == null ? null : node.key;
        }

        @Override
        public K pollFirst() {
            TMap.Entry<K, V> ret = map.firstEntry();
            if (ret == null) {
                return null;
            }
            map.map.remove(ret.getKey());
            return ret.getKey();
        }

        @Override
        public K pollLast() {
            TMap.Entry<K, V> ret = map.lastEntry();
            if (ret == null) {
                return null;
            }
            map.map.remove(ret.getKey());
            return ret.getKey();
        }

        @Override
        public TNavigableSet<K> subSet(K start, boolean startInclusive, K end, boolean endInclusive) {
            checkInRange(start, startInclusive);
            checkInRange(end, endInclusive);
            if ((null != map.comparator()) ? map.comparator().compare(start, end) > 0
                    : toComparable(start).compareTo(end) > 0) {
                throw new IllegalArgumentException();
            }
            return new DescendingSubMapKeySet<>(
                    new DescendingSubMap<>(start, startInclusive, map.map, end, endInclusive));
        }

        @Override
        public TNavigableSet<K> tailSet(K start, boolean startInclusive) {
            checkInRange(start, startInclusive);
            if (map.toEnd) {
                return new DescendingSubMapKeySet<>(
                        new DescendingSubMap<>(start, startInclusive, map.map, map.hi, map.hiInclusive));
            } else {
                return new DescendingSubMapKeySet<>(new DescendingSubMap<>(start, startInclusive, map.map));
            }
        }

        void checkInRange(K key, boolean keyInclusive) {
            boolean isInRange = true;
            int result;
            if (map.toEnd) {
                result = (null != map.comparator()) ? map.comparator().compare(key, map.hi)
                        : toComparable(key).compareTo(map.hi);
                isInRange = ((!map.hiInclusive) && keyInclusive) ? result < 0 : result <= 0;
            }
            if (map.fromStart) {
                result = (null != map.comparator()) ? map.comparator().compare(key, map.lo)
                        : toComparable(key).compareTo(map.lo);
                isInRange = isInRange && (((!map.loInclusive) && keyInclusive) ? result > 0 : result >= 0);
            }
            if (!isInRange) {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public TComparator<? super K> comparator() {
            return map.comparator();
        }

        @Override
        public K first() {
            return map.firstKey();
        }

        @Override
        public TSortedSet<K> headSet(K end) {
            return headSet(end, false);
        }

        @Override
        public K last() {
            return map.lastKey();
        }

        @Override
        public TSortedSet<K> subSet(K start, K end) {
            return subSet(start, true, end, false);
        }

        @Override
        public TSortedSet<K> tailSet(K start) {
            return tailSet(start, true);
        }
    }

    static abstract class NavigableSubMap<K, V>
            extends TAbstractMap<K, V> implements TNavigableMap<K, V>, TSerializable {
        final TTreeMap<K, V> map;

        final K lo;
        final K hi;

        final boolean fromStart;
        final boolean toEnd;

        final boolean loInclusive;
        final boolean hiInclusive;

        NavigableSubMap(K lo, boolean startKeyInclusive, TTreeMap<K, V> map, K hi, boolean endKeyInclusive) {
            this.map = map;
            fromStart = true;
            toEnd = true;
            this.lo = lo;
            this.hi = hi;
            loInclusive = startKeyInclusive;
            hiInclusive = endKeyInclusive;
        }

        NavigableSubMap(K lo, boolean startKeyInclusive, TTreeMap<K, V> map) {
            this.map = map;
            fromStart = true;
            toEnd = false;
            this.lo = lo;
            hi = null;
            loInclusive = startKeyInclusive;
            hiInclusive = false;
        }

        NavigableSubMap(final TTreeMap<K, V> map, final K hi, final boolean endKeyInclusive) {
            this.map = map;
            fromStart = false;
            toEnd = true;
            lo = null;
            this.hi = hi;
            loInclusive = false;
            hiInclusive = endKeyInclusive;
        }

        // the whole TreeMap
        NavigableSubMap(final TTreeMap<K, V> map) {
            this.map = map;
            fromStart = false;
            toEnd = false;
            lo = null;
            hi = null;
            loInclusive = false;
            hiInclusive = false;
        }

        @Override
        public TComparator<? super K> comparator() {
            return map.comparator();
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean containsKey(Object key) {
            checkNull(key);
            if (isInRange((K) key)) {
                return map.containsKey(key);
            }
            return false;
        }

        private void checkNull(Object key) {
            if (null == key && null == comparator()) {
                throw new NullPointerException();
            }
        }

        @Override
        public boolean isEmpty() {
            return !this.keySet().iterator().hasNext();
        }

        @Override
        public int size() {
            return entrySet().size();
        }

        @Override
        public V put(K key, V value) {
            checkNull(key);
            if (isInRange(key)) {
                return map.put(key, value);
            }
            throw new IllegalArgumentException();
        }

        @SuppressWarnings("unchecked")
        @Override
        public V get(Object key) {
            checkNull(key);
            if (isInRange((K) key)) {
                return map.get(key);
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public V remove(Object key) {
            checkNull(key);
            if (isInRange((K) key)) {
                return map.remove(key);
            }
            return null;
        }

        /*
         * The navigable methods.
         */

        abstract NavigableSubMap<K, V> descendingSubMap();

        @Override
        public K firstKey() {
            TMap.Entry<K, V> node = firstEntry();
            if (node != null) {
                return node.getKey();
            }
            throw new TNoSuchElementException();
        }

        @Override
        public K lastKey() {
            TMap.Entry<K, V> node = lastEntry();
            if (node != null) {
                return node.getKey();
            }
            throw new TNoSuchElementException();
        }

        @Override
        public K higherKey(K key) {
            TMap.Entry<K, V> entry = higherEntry(key);
            return (null == entry) ? null : entry.getKey();
        }

        @Override
        public K lowerKey(K key) {
            TMap.Entry<K, V> entry = lowerEntry(key);
            return (null == entry) ? null : entry.getKey();
        }

        @Override
        public K ceilingKey(K key) {
            TMap.Entry<K, V> entry = ceilingEntry(key);
            return (null == entry) ? null : entry.getKey();
        }

        @Override
        public K floorKey(K key) {
            TMap.Entry<K, V> entry = floorEntry(key);
            return (null == entry) ? null : entry.getKey();
        }

        @Override
        public TSet<K> keySet() {
            return navigableKeySet();
        }

        @Override
        public TNavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        @Override
        public TSortedMap<K, V> subMap(K start, K end) {
            return subMap(start, true, end, false);
        }

        @Override
        public TSortedMap<K, V> headMap(K end) {
            return headMap(end, false);
        }

        @Override
        public TSortedMap<K, V> tailMap(K start) {
            return tailMap(start, true);
        }

        final boolean checkUpperBound(K key) {
            if (toEnd) {
                int result = (null != comparator()) ? comparator().compare(key, hi) : toComparable(key).compareTo(hi);
                return hiInclusive ? result <= 0 : result < 0;
            }
            return true;
        }

        final boolean checkLowerBound(K key) {
            if (fromStart) {
                int result = -((null != comparator()) ? comparator().compare(lo, key)
                        : toComparable(lo).compareTo(key));
                return loInclusive ? result >= 0 : result > 0;
            }
            return true;
        }

        final boolean isInRange(K key) {
            return checkUpperBound(key) && checkLowerBound(key);
        }

        final TTreeMap.Entry<K, V> theSmallestEntry() {
            TTreeMap.Entry<K, V> result;
            if (!fromStart) {
                result = map.findSmallestEntry();
            } else {
                result = loInclusive ? map.findCeilingEntry(lo) : map.findHigherEntry(lo);
            }
            return (null != result && checkUpperBound(result.getKey())) ? result : null;
        }

        final TTreeMap.Entry<K, V> theBiggestEntry() {
            TTreeMap.Entry<K, V> result;
            if (!toEnd) {
                result = map.findBiggestEntry();
            } else {
                result = hiInclusive ? map.findFloorEntry(hi) : map.findLowerEntry(hi);
            }
            return (null != result && checkLowerBound(result.getKey())) ? result : null;
        }

        final TTreeMap.Entry<K, V> smallerOrEqualEntry(K key) {
            TTreeMap.Entry<K, V> result = findFloorEntry(key);
            return (null != result && checkLowerBound(result.getKey())) ? result : null;
        }

        private TTreeMap.Entry<K, V> findFloorEntry(K key) {
            TTreeMap.Entry<K, V> node = findFloorEntryImpl(key);

            if (node == null) {
                return null;
            }

            if (!checkUpperBound(node.key)) {
                node = findEndNode();
            }

            if (node != null && !checkLowerBound(node.key)) {
                Comparable<K> object = map.comparator == null ? toComparable(key) : null;
                if (cmp(object, key, this.lo) > 0) {
                    node = findStartNode();
                    if (node == null || cmp(object, key, node.key) < 0) {
                        return null;
                    }
                } else {
                    node = null;
                }
            }
            return node;
        }

        private int cmp(Comparable<K> object, K key1, K key2) {
            return object != null ? object.compareTo(key2) : comparator().compare(key1, key2);
        }

        private TTreeMap.Entry<K, V> findFloorEntryImpl(K key) {
            Comparable<K> object = comparator() == null ? toComparable(key) : null;
            Node<K, V> node = this.map.root;
            Node<K, V> foundNode = null;
            int foundIndex = 0;
            while (node != null) {
                K[] keys = node.keys;
                int leftIdx = node.leftIdx;
                int result = cmp(object, key, keys[leftIdx]);
                if (result < 0) {
                    node = node.left;
                } else {
                    foundNode = node;
                    foundIndex = leftIdx;
                    if (result == 0) {
                        break;
                    }
                    int rightIdx = node.rightIdx;
                    if (leftIdx != rightIdx) {
                        result = cmp(object, key, keys[rightIdx]);
                    }
                    if (result >= 0) {
                        foundNode = node;
                        foundIndex = rightIdx;
                        if (result == 0) {
                            break;
                        }
                        node = node.right;
                    } else { /* search in node */
                        int low = leftIdx + 1;
                        int mid;
                        int high = rightIdx - 1;
                        while (low <= high && result != 0) {
                            mid = (low + high) >> 1;
                            result = cmp(object, key, keys[mid]);
                            if (result >= 0) {
                                foundNode = node;
                                foundIndex = mid;
                                low = mid + 1;
                            } else {
                                high = mid;
                            }
                            if (low == high && high == mid) {
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            if (foundNode != null && cmp(object, key, foundNode.keys[foundIndex]) < 0) {
                foundNode = null;
            }
            if (foundNode != null) {
                return newEntry(foundNode, foundIndex);
            }
            return null;
        }

        private TTreeMap.Entry<K, V> findStartNode() {
            if (fromStart) {
                if (loInclusive) {
                    return map.findCeilingEntry(lo);
                } else {
                    return map.findHigherEntry(lo);
                }
            } else {
                return theSmallestEntry();
            }
        }

        // find the node whose key equals endKey if any, or the next smaller
        // one than endKey if end exclusive
        private TTreeMap.Entry<K, V> findEndNode() {
            if (hiInclusive) {
                return findFloorEntryImpl(hi);
            } else {
                return findLowerEntryImpl(hi);
            }
        }

        private TTreeMap.Entry<K, V> findCeilingEntry(K key) {
            TTreeMap.Entry<K, V> node = findCeilingEntryImpl(key);

            if (null == node) {
                return null;
            }

            if (!checkUpperBound(node.key)) {
                Comparable<K> object = map.comparator == null ? toComparable(key) : null;
                if (cmp(object, key, this.hi) < 0) {
                    node = findEndNode();
                    if (node != null && cmp(object, key, node.key) > 0) {
                        return null;
                    }
                } else {
                    return null;
                }
            }

            if (node != null && !checkLowerBound(node.key)) {
                node = findStartNode();
            }

            return node;
        }

        private TTreeMap.Entry<K, V> findLowerEntryImpl(K key) {
            Comparable<K> object = comparator() == null ? toComparable(key) : null;
            Node<K, V> node = map.root;
            Node<K, V> foundNode = null;
            int foundIndex = 0;
            while (node != null) {
                K[] keys = node.keys;
                int leftIdx = node.leftIdx;
                int result = cmp(object, key, keys[leftIdx]);
                if (result <= 0) {
                    node = node.left;
                } else {
                    foundNode = node;
                    foundIndex = leftIdx;
                    int rightIdx = node.rightIdx;
                    if (leftIdx != rightIdx) {
                        result = cmp(object, key, keys[rightIdx]);
                    }
                    if (result > 0) {
                        foundNode = node;
                        foundIndex = rightIdx;
                        node = node.right;
                    } else { /* search in node */
                        int low = leftIdx + 1;
                        int mid;
                        int high = rightIdx - 1;
                        while (low <= high) {
                            mid = (low + high) >> 1;
                            result = cmp(object, key, keys[mid]);
                            if (result > 0) {
                                foundNode = node;
                                foundIndex = mid;
                                low = mid + 1;
                            } else {
                                high = mid;
                            }
                            if (low == high && high == mid) {
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            if (foundNode != null && cmp(object, key, foundNode.keys[foundIndex]) <= 0) {
                foundNode = null;
            }
            if (foundNode != null) {
                return newEntry(foundNode, foundIndex);
            }
            return null;
        }

        private TTreeMap.Entry<K, V> findCeilingEntryImpl(K key) {
            Comparable<K> object = comparator() == null ? toComparable(key) : null;
            Node<K, V> node = map.root;
            Node<K, V> foundNode = null;
            int foundIndex = 0;
            while (node != null) {
                K[] keys = node.keys;
                int leftIdx = node.leftIdx;
                int rightIdx = node.rightIdx;
                int result = cmp(object, key, keys[leftIdx]);
                if (result < 0) {
                    foundNode = node;
                    foundIndex = leftIdx;
                    node = node.left;
                } else if (result == 0) {
                    foundNode = node;
                    foundIndex = leftIdx;
                    break;
                } else {
                    if (leftIdx != rightIdx) {
                        result = cmp(object, key, keys[rightIdx]);
                    }
                    if (result > 0) {
                        node = node.right;
                    } else { /* search in node */
                        foundNode = node;
                        foundIndex = rightIdx;
                        if (result == 0) {
                            break;
                        }
                        int low = leftIdx + 1;
                        int mid;
                        int high = rightIdx - 1;
                        while (low <= high && result != 0) {
                            mid = (low + high) >> 1;
                            result = cmp(object, key, keys[mid]);
                            if (result <= 0) {
                                foundNode = node;
                                foundIndex = mid;
                                high = mid - 1;
                            } else {
                                low = mid + 1;
                            }
                            if (result == 0 || (low == high && high == mid)) {
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            if (foundNode != null && cmp(object, key, foundNode.keys[foundIndex]) > 0) {
                foundNode = null;
            }
            if (foundNode != null) {
                return newEntry(foundNode, foundIndex);
            }
            return null;
        }

        final TTreeMap.Entry<K, V> smallerEntry(K key) {
            TTreeMap.Entry<K, V> result = findLowerEntry(key);
            return (null != result && checkLowerBound(result.getKey())) ? result : null;
        }

        private TTreeMap.Entry<K, V> findLowerEntry(K key) {
            TTreeMap.Entry<K, V> node = findLowerEntryImpl(key);

            if (null == node) {
                return null;
            }

            if (!checkUpperBound(node.key)) {
                node = findEndNode();
            }

            if (!checkLowerBound(node.key)) {
                Comparable<K> object = map.comparator == null ? toComparable(key) : null;
                if (cmp(object, key, this.lo) > 0) {
                    node = findStartNode();
                    if (node == null || cmp(object, key, node.key) <= 0) {
                        return null;
                    }
                } else {
                    node = null;
                }
            }

            return node;
        }

        private TTreeMap.Entry<K, V> findHigherEntry(K key) {
            TTreeMap.Entry<K, V> node = findHigherEntryImpl(key);

            if (node == null) {
                return null;
            }

            if (!checkUpperBound(node.key)) {
                Comparable<K> object = map.comparator == null ? toComparable(key) : null;
                if (cmp(object, key, this.hi) < 0) {
                    node = findEndNode();
                    if (node != null && cmp(object, key, node.key) >= 0) {
                        return null;
                    }
                } else {
                    return null;
                }
            }

            if (node != null && !checkLowerBound(node.key)) {
                node = findStartNode();
            }

            return node;
        }

        TTreeMap.Entry<K, V> findHigherEntryImpl(K key) {
            Comparable<K> object = map.comparator == null ? toComparable(key) : null;
            Node<K, V> node = map.root;
            Node<K, V> foundNode = null;
            int foundIndex = 0;
            while (node != null) {
                K[] keys = node.keys;
                int rightIdx = node.rightIdx;
                int result = cmp(object, key, keys[rightIdx]);
                if (result >= 0) {
                    node = node.right;
                } else {
                    int leftIdx = node.leftIdx;
                    if (leftIdx != rightIdx) {
                        result = cmp(object, key, keys[leftIdx]);
                    }
                    foundNode = node;
                    if (result < 0) {
                        foundIndex = leftIdx;
                        node = node.left;
                    } else { /* search in node */
                        foundIndex = rightIdx;
                        int low = leftIdx + 1;
                        int mid;
                        int high = rightIdx - 1;
                        while (low <= high) {
                            mid = (low + high) >> 1;
                            result = cmp(object, key, keys[mid]);
                            if (result < 0) {
                                foundNode = node;
                                foundIndex = mid;
                                high = mid - 1;
                            } else {
                                low = mid + 1;
                            }
                            if (low == high && high == mid) {
                                break;
                            }
                        }
                        break;
                    }
                }
            }
            if (foundNode != null && cmp(object, key, foundNode.keys[foundIndex]) >= 0) {
                foundNode = null;
            }
            if (foundNode != null) {
                return newEntry(foundNode, foundIndex);
            }
            return null;
        }
    }

    static class AscendingSubMap<K, V> extends NavigableSubMap<K, V> implements TSerializable {

        AscendingSubMap(K start, boolean startKeyInclusive, TTreeMap<K, V> map, K end, boolean endKeyInclusive) {
            super(start, startKeyInclusive, map, end, endKeyInclusive);
        }

        AscendingSubMap(TTreeMap<K, V> map, K end, boolean endKeyInclusive) {
            super(map, end, endKeyInclusive);
        }

        AscendingSubMap(K start, boolean startKeyInclusive, TTreeMap<K, V> map) {
            super(start, startKeyInclusive, map);
        }

        AscendingSubMap(TTreeMap<K, V> map) {
            super(map);
        }

        @Override
        public TMap.Entry<K, V> firstEntry() {
            TTreeMap.Entry<K, V> ret = theSmallestEntry();
            if (ret != null) {
                return newImmutableEntry(ret);
            } else {
                return null;
            }
        }

        @Override
        public TMap.Entry<K, V> lastEntry() {
            TTreeMap.Entry<K, V> ret = theBiggestEntry();
            if (ret != null) {
                return newImmutableEntry(ret);
            } else {
                return null;
            }
        }

        @Override
        public TMap.Entry<K, V> pollFirstEntry() {
            TTreeMap.Entry<K, V> node = theSmallestEntry();
            TMap.Entry<K, V> result = newImmutableEntry(node);
            if (null != node) {
                map.remove(node.key);
            }
            return result;
        }

        @Override
        public TMap.Entry<K, V> pollLastEntry() {
            TTreeMap.Entry<K, V> node = theBiggestEntry();
            TMap.Entry<K, V> result = newImmutableEntry(node);
            if (null != node) {
                map.remove(node.key);
            }
            return result;
        }

        @Override
        public TMap.Entry<K, V> higherEntry(K key) {
            TTreeMap.Entry<K, V> entry = super.findHigherEntry(key);
            if (null != entry && isInRange(entry.key)) {
                return newImmutableEntry(entry);
            } else {
                return null;
            }
        }

        @Override
        public Entry<K, V> lowerEntry(K key) {
            TTreeMap.Entry<K, V> entry = super.findLowerEntry(key);
            if (null != entry && isInRange(entry.key)) {
                return newImmutableEntry(entry);
            } else {
                return null;
            }
        }

        @Override
        public Entry<K, V> ceilingEntry(K key) {
            TTreeMap.Entry<K, V> entry = super.findCeilingEntry(key);
            if (null != entry && isInRange(entry.key)) {
                return newImmutableEntry(entry);
            } else {
                return null;
            }
        }

        @Override
        public Entry<K, V> floorEntry(K key) {
            TTreeMap.Entry<K, V> entry = super.findFloorEntry(key);
            if (null != entry && isInRange(entry.key)) {
                return newImmutableEntry(entry);
            } else {
                return null;
            }
        }

        @Override
        public TSet<TMap.Entry<K, V>> entrySet() {
            return new AscendingSubMapEntrySet<>(this);
        }

        @Override
        public TNavigableSet<K> navigableKeySet() {
            return new AscendingSubMapKeySet<>(this);
        }

        @Override
        public TNavigableMap<K, V> descendingMap() {
            if (fromStart && toEnd) {
                return new DescendingSubMap<>(hi, hiInclusive, map, lo, loInclusive);
            }
            if (fromStart) {
                return new DescendingSubMap<>(map, lo, loInclusive);
            }
            if (toEnd) {
                return new DescendingSubMap<>(hi, hiInclusive, map);
            }
            return new DescendingSubMap<>(map);
        }

        @Override
        NavigableSubMap<K, V> descendingSubMap() {
            if (fromStart && toEnd) {
                return new DescendingSubMap<>(hi, hiInclusive, map, lo, loInclusive);
            }
            if (fromStart) {
                return new DescendingSubMap<>(map, lo, loInclusive);
            }
            if (toEnd) {
                return new DescendingSubMap<>(hi, hiInclusive, map);
            }
            return new DescendingSubMap<>(map);
        }

        @Override
        public TNavigableMap<K, V> subMap(K start, boolean startKeyInclusive, K end, boolean endKeyInclusive) {
            if (fromStart && ((!loInclusive && startKeyInclusive)
                    ? map.keyCompare(start, lo) <= 0 : map.keyCompare(start, lo) < 0)
                    || (toEnd && ((!hiInclusive && (endKeyInclusive || (startKeyInclusive && start.equals(end))))
                    ? map.keyCompare(end, hi) >= 0 : map.keyCompare(
                    end, hi) > 0))) {
                throw new IllegalArgumentException();
            }
            if (map.keyCompare(start, end) > 0) {
                throw new IllegalArgumentException();
            }
            return new AscendingSubMap<>(start, startKeyInclusive, map, end, endKeyInclusive);
        }

        @Override
        public TNavigableMap<K, V> headMap(K end, boolean inclusive) {
            if (fromStart && ((!loInclusive && inclusive) ? map.keyCompare(end, lo) <= 0
                    : map.keyCompare(end, lo) < 0)) {
                throw new IllegalArgumentException();
            }
            if (toEnd && ((!hiInclusive && inclusive) ? map.keyCompare(end, hi) >= 0 : map.keyCompare(end, hi) > 0)) {
                throw new IllegalArgumentException();
            }
            if (checkUpperBound(end)) {
                if (this.fromStart) {
                    return new AscendingSubMap<>(this.lo, this.loInclusive, map, end, inclusive);
                }
                return new AscendingSubMap<>(map, end, inclusive);
            } else {
                return this;
            }
        }

        @Override
        public TNavigableMap<K, V> tailMap(K start, boolean inclusive) {
            if (fromStart && ((!loInclusive && inclusive) ? map.keyCompare(start, lo) <= 0
                    : map.keyCompare(start, lo) < 0)) {
                throw new IllegalArgumentException();
            }
            if (toEnd && ((!hiInclusive && inclusive) ? map.keyCompare(start, hi) >= 0
                    : map.keyCompare(start, hi) > 0)) {
                throw new IllegalArgumentException();
            }
            if (checkLowerBound(start)) {
                if (this.toEnd) {
                    return new AscendingSubMap<>(start, inclusive, map, this.hi, this.hiInclusive);
                }
                return new AscendingSubMap<>(start, inclusive, map);
            } else {
                return this;
            }
        }

        @Override
        public TCollection<V> values() {
            if (cachedValues == null) {
                cachedValues = new AscendingSubMapValuesCollection<>(this);
            }
            return cachedValues;
        }

        static class AscendingSubMapValuesCollection<K, V> extends TAbstractCollection<V> {
            AscendingSubMap<K, V> subMap;

            AscendingSubMapValuesCollection(AscendingSubMap<K, V> subMap) {
                this.subMap = subMap;
            }

            @Override
            public TIterator<V> iterator() {
                return new AscendingSubMapValueIterator<>(subMap);
            }

            @Override
            public int size() {
                return subMap.size();
            }

            @Override
            public boolean isEmpty() {
                return subMap.isEmpty();
            }
        }
    }

    static class DescendingSubMap<K, V> extends NavigableSubMap<K, V> implements TSerializable {
        private final TComparator<? super K> reverseComparator = TCollections.reverseOrder(map.comparator);

        DescendingSubMap(K start, boolean startKeyInclusive, TTreeMap<K, V> map, K end, boolean endKeyInclusive) {
            super(start, startKeyInclusive, map, end, endKeyInclusive);
        }

        DescendingSubMap(K start, boolean startKeyInclusive, TTreeMap<K, V> map) {
            super(start, startKeyInclusive, map);
        }

        DescendingSubMap(TTreeMap<K, V> map, K end, boolean endKeyInclusive) {
            super(map, end, endKeyInclusive);
        }

        DescendingSubMap(TTreeMap<K, V> map) {
            super(map);
        }

        @Override
        public TComparator<? super K> comparator() {
            return reverseComparator;
        }

        @Override
        public TMap.Entry<K, V> firstEntry() {
            TTreeMap.Entry<K, V> result;
            if (!fromStart) {
                result = map.findBiggestEntry();
            } else {
                result = loInclusive ? map.findFloorEntry(lo) : map.findLowerEntry(lo);
            }
            if (result == null || !isInRange(result.key)) {
                return null;
            }
            return newImmutableEntry(result);
        }

        @Override
        public TMap.Entry<K, V> lastEntry() {
            TTreeMap.Entry<K, V> result;
            if (!toEnd) {
                result = map.findSmallestEntry();
            } else {
                result = hiInclusive ? map.findCeilingEntry(hi) : map.findHigherEntry(hi);
            }
            if (result != null && !isInRange(result.key)) {
                return null;
            }
            return newImmutableEntry(result);
        }

        @Override
        public TMap.Entry<K, V> pollFirstEntry() {
            TTreeMap.Entry<K, V> node;
            if (fromStart) {
                node = loInclusive ? map.findFloorEntry(lo) : this.map.findLowerEntry(lo);
            } else {
                node = map.findBiggestEntry();
            }
            if (node != null && fromStart && (loInclusive ? this.map.keyCompare(lo, node.key) < 0 : map
                    .keyCompare(lo, node.key) <= 0)) {
                node = null;
            }
            if (node != null && toEnd && (hiInclusive ? map.keyCompare(hi, node.key) > 0
                    : map.keyCompare(hi, node.key) >= 0)) {
                node = null;
            }
            TMap.Entry<K, V> result = newImmutableEntry(node);
            if (null != node) {
                map.remove(node.key);
            }
            return result;
        }

        @Override
        public Entry<K, V> pollLastEntry() {
            TTreeMap.Entry<K, V> node;
            if (toEnd) {
                node = hiInclusive ? map.findCeilingEntry(hi) : map.findHigherEntry(hi);
            } else {
                node = this.map.findSmallestEntry();
            }
            if (node != null && fromStart && (loInclusive ? map.keyCompare(lo, node.key) < 0
                    : map.keyCompare(lo, node.key) <= 0)) {
                node = null;
            }
            if (node != null && toEnd && (hiInclusive ? map.keyCompare(hi, node.key) > 0
                    : map.keyCompare(hi, node.key) >= 0)) {
                node = null;
            }
            TMap.Entry<K, V> result = newImmutableEntry(node);
            if (null != node) {
                map.remove(node.key);
            }
            return result;
        }

        @Override
        public Entry<K, V> higherEntry(K key) {
            TTreeMap.Entry<K, V> entry = this.map.findLowerEntry(key);
            if (null != entry && !checkLowerBound(entry.getKey())) {
                entry = loInclusive ? this.map.findFloorEntry(this.lo) : this.map.findLowerEntry(this.lo);
            }
            if (null != entry && !isInRange(entry.getKey())) {
                entry = null;
            }
            return newImmutableEntry(entry);
        }

        @Override
        public TMap.Entry<K, V> lowerEntry(K key) {
            TTreeMap.Entry<K, V> entry = this.map.findHigherEntry(key);
            if (null != entry && !checkUpperBound(entry.getKey())) {
                entry = hiInclusive ? this.map.findCeilingEntry(this.hi) : this.map.findHigherEntry(this.hi);
            }
            if (null != entry && !isInRange(entry.getKey())) {
                entry = null;
            }
            return newImmutableEntry(entry);
        }

        @Override
        public TMap.Entry<K, V> ceilingEntry(K key) {
            Comparable<K> object = map.comparator == null ? toComparable(key) : null;
            TTreeMap.Entry<K, V> entry;
            if (fromStart && map.cmp(object, key, lo) >= 0) {
                entry = loInclusive ? this.map.findFloorEntry(lo) : this.map.findLowerEntry(lo);
            } else {
                entry = this.map.findFloorEntry(key);
            }
            if (null != entry && !checkUpperBound(entry.getKey())) {
                entry = null;
            }
            return newImmutableEntry(entry);
        }

        @Override
        public TMap.Entry<K, V> floorEntry(K key) {
            Comparable<K> object = map.comparator == null ? toComparable(key) : null;
            TTreeMap.Entry<K, V> entry;
            if (toEnd && map.cmp(object, key, hi) <= 0) {
                entry = hiInclusive ? this.map.findCeilingEntry(hi) : this.map.findHigherEntry(hi);
            } else {
                entry = this.map.findCeilingEntry(key);
            }
            if (null != entry && !checkLowerBound(entry.getKey())) {
                entry = null;
            }
            return newImmutableEntry(entry);
        }

        @Override
        public TSet<TMap.Entry<K, V>> entrySet() {
            return new DescendingSubMapEntrySet<>(this);
        }

        @Override
        public TNavigableSet<K> navigableKeySet() {
            return new DescendingSubMapKeySet<>(this);
        }

        @Override
        public TNavigableMap<K, V> descendingMap() {
            if (fromStart && toEnd) {
                return new AscendingSubMap<>(hi, hiInclusive, map, lo, loInclusive);
            }
            if (fromStart) {
                return new AscendingSubMap<>(map, lo, loInclusive);
            }
            if (toEnd) {
                return new AscendingSubMap<>(hi, hiInclusive, map);
            }
            return new AscendingSubMap<>(map);
        }

        int keyCompare(K left, K right) {
            return (null != reverseComparator) ? reverseComparator.compare(left, right)
                    : toComparable(left).compareTo(right);
        }

        @Override
        public TNavigableMap<K, V> subMap(K start, boolean startKeyInclusive, K end, boolean endKeyInclusive) {
            // special judgement, the same reason as subMap(K,K)
            if (!checkUpperBound(start)) {
                throw new IllegalArgumentException();
            }
            if (fromStart && ((!loInclusive && (startKeyInclusive || (endKeyInclusive && start.equals(end))))
                    ? keyCompare(start, lo) <= 0 : keyCompare(start, lo) < 0)
                    || (toEnd && ((!hiInclusive && endKeyInclusive) ? keyCompare(end, hi) >= 0
                    : keyCompare(end, hi) > 0))) {
                throw new IllegalArgumentException();
            }
            if (keyCompare(start, end) > 0) {
                throw new IllegalArgumentException();
            }
            return new DescendingSubMap<>(start, startKeyInclusive, map, end, endKeyInclusive);
        }

        @Override
        public TNavigableMap<K, V> headMap(K end, boolean inclusive) {
            // check for error
            this.keyCompare(end, end);
            K inclusiveEnd = end; // inclusive ? end : m.higherKey(end);
            boolean isInRange = true;
            if (null != inclusiveEnd) {
                int result;
                if (toEnd) {
                    result = (null != comparator()) ? comparator().compare(inclusiveEnd, hi)
                            : toComparable(inclusiveEnd).compareTo(hi);
                    isInRange = (hiInclusive || !inclusive) ? result <= 0 : result < 0;
                }
                if (fromStart) {
                    result = (null != comparator()) ? comparator().compare(inclusiveEnd, lo)
                            : toComparable(inclusiveEnd).compareTo(lo);
                    isInRange = isInRange && ((loInclusive || !inclusive) ? result >= 0 : result > 0);
                }
            }
            if (isInRange) {
                if (this.fromStart) {
                    return new DescendingSubMap<>(this.lo, this.loInclusive, map, end, inclusive);
                }
                return new DescendingSubMap<>(map, end, inclusive);
            }
            throw new IllegalArgumentException();
        }

        @Override
        public TNavigableMap<K, V> tailMap(K start, boolean inclusive) {
            // check for error
            this.keyCompare(start, start);
            K inclusiveStart = start; // inclusive ? start : m.lowerKey(start);
            boolean isInRange = true;
            int result;
            if (null != inclusiveStart) {
                if (toEnd) {
                    result = (null != comparator()) ? comparator().compare(inclusiveStart, hi)
                            : toComparable(inclusiveStart).compareTo(hi);
                    isInRange = (hiInclusive || !inclusive) ? result <= 0 : result < 0;
                }
                if (fromStart) {
                    result = (null != comparator()) ? comparator().compare(inclusiveStart, lo)
                            : toComparable(inclusiveStart).compareTo(lo);
                    isInRange = isInRange && ((loInclusive || !inclusive) ? result >= 0 : result > 0);
                }
            }
            if (isInRange) {
                if (this.toEnd) {
                    return new DescendingSubMap<>(start, inclusive, map, this.hi, this.hiInclusive);
                }
                return new DescendingSubMap<>(start, inclusive, map);

            }
            throw new IllegalArgumentException();
        }

        @Override
        public TCollection<V> values() {
            if (cachedValues == null) {
                cachedValues = new DescendingSubMapValuesCollection<>(this);
            }
            return cachedValues;
        }

        static class DescendingSubMapValuesCollection<K, V> extends TAbstractCollection<V> {
            DescendingSubMap<K, V> subMap;

            DescendingSubMapValuesCollection(DescendingSubMap<K, V> subMap) {
                this.subMap = subMap;
            }

            @Override
            public boolean isEmpty() {
                return subMap.isEmpty();
            }

            @Override
            public TIterator<V> iterator() {
                return new DescendingSubMapValueIterator<>(subMap);
            }

            @Override
            public int size() {
                return subMap.size();
            }
        }

        @Override
        NavigableSubMap<K, V> descendingSubMap() {
            if (fromStart && toEnd) {
                return new AscendingSubMap<>(hi, hiInclusive, map, lo, loInclusive);
            }
            if (fromStart) {
                return new AscendingSubMap<>(map, hi, hiInclusive);
            }
            if (toEnd) {
                return new AscendingSubMap<>(lo, loInclusive, map);
            }
            return new AscendingSubMap<>(map);
        }
    }

    public TTreeMap() {
        super();
    }

    public TTreeMap(TComparator<? super K> comparator) {
        this.comparator = comparator;
    }

    public TTreeMap(TMap<? extends K, ? extends V> map) {
        this();
        putAll(map);
    }

    public TTreeMap(TSortedMap<K, ? extends V> map) {
        this(map.comparator());
        Node<K, V> lastNode = null;
        for (TIterator<? extends TMap.Entry<K, ? extends V>> it = map.entrySet().iterator(); it.hasNext();) {
            TMap.Entry<K, ? extends V> entry = it.next();
            lastNode = addToLast(lastNode, entry.getKey(), entry.getValue());
        }
    }

    private Node<K, V> addToLast(Node<K, V> last, K key, V value) {
        if (last == null) {
            root = createNode(key, value);
            last = root;
            size = 1;
        } else if (last.size == Node.NODE_SIZE) {
            Node<K, V> newNode = createNode(key, value);
            attachToRight(last, newNode);
            balance(newNode);
            size++;
            last = newNode;
        } else {
            appendFromRight(last, key, value);
            size++;
        }
        return last;
    }

    @Override
    public void clear() {
        root = null;
        size = 0;
        modCount++;
    }

    @SuppressWarnings("unchecked")
    @Override
    public TTreeMap<K, V> clone() {
        try {
            TTreeMap<K, V> clone = (TTreeMap<K, V>) super.clone();
            clone.entrySet = null;
            clone.descendingMap = null;
            clone.navigableKeySet = null;
            if (root != null) {
                clone.root = root.clone(null);
                // restore prev/next chain
                Node<K, V> node = minimum(clone.root);
                while (true) {
                    Node<K, V> nxt = successor(node);
                    if (nxt == null) {
                        break;
                    }
                    nxt.prev = node;
                    node.next = nxt;
                    node = nxt;
                }
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    @Override
    public TComparator<? super K> comparator() {
        return comparator;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean containsKey(Object key) {
        Comparable<K> object = comparator == null ? toComparable((K) key) : null;
        K keyK = (K) key;
        Node<K, V> node = root;
        while (node != null) {
            K[] keys = node.keys;
            int leftIdx = node.leftIdx;
            int result = cmp(object, keyK, keys[leftIdx]);
            if (result < 0) {
                node = node.left;
            } else if (result == 0) {
                return true;
            } else {
                int rightIdx = node.rightIdx;
                if (leftIdx != rightIdx) {
                    result = cmp(object, keyK, keys[rightIdx]);
                }
                if (result > 0) {
                    node = node.right;
                } else if (result == 0) {
                    return true;
                } else { /* search in node */
                    int low = leftIdx + 1;
                    int mid;
                    int high = rightIdx - 1;
                    while (low <= high) {
                        mid = (low + high) >>> 1;
                        result = cmp(object, keyK, keys[mid]);
                        if (result > 0) {
                            low = mid + 1;
                        } else if (result == 0) {
                            return true;
                        } else {
                            high = mid - 1;
                        }
                    }
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        if (root == null) {
            return false;
        }
        Node<K, V> node = minimum(root);
        if (value != null) {
            while (node != null) {
                int to = node.rightIdx;
                V[] values = node.values;
                for (int i = node.leftIdx; i <= to; i++) {
                    if (value.equals(values[i])) {
                        return true;
                    }
                }
                node = node.next;
            }
        } else {
            while (node != null) {
                int to = node.rightIdx;
                V[] values = node.values;
                for (int i = node.leftIdx; i <= to; i++) {
                    if (values[i] == null) {
                        return true;
                    }
                }
                node = node.next;
            }
        }
        return false;
    }

    private static <K, V> TTreeMap.Entry<K, V> newEntry(Node<K, V> node, int index) {
        return new TTreeMap.Entry<>(node, index);
    }

    private static <K, V> TMap.Entry<K, V> newImmutableEntry(TTreeMap.Entry<K, V> entry) {
        return (null == entry) ? null : new SimpleImmutableEntry<>(entry);
    }

    private TTreeMap.Entry<K, V> findSmallestEntry() {
        if (null != root) {
            Node<K, V> node = minimum(root);
            return newEntry(node, node.leftIdx);
        }
        return null;
    }

    private TTreeMap.Entry<K, V> findBiggestEntry() {
        if (null != root) {
            Node<K, V> node = maximum(root);
            return newEntry(node, node.rightIdx);
        }
        return null;
    }

    private TTreeMap.Entry<K, V> findCeilingEntry(K key) {
        if (root == null) {
            return null;
        }
        Comparable<K> object = comparator == null ? toComparable(key) : null;
        Node<K, V> node = root;
        Node<K, V> foundNode = null;
        int foundIndex = 0;
        while (node != null) {
            K[] keys = node.keys;
            int leftIdx = node.leftIdx;
            int rightIdx = node.rightIdx;
            int result = cmp(object, key, keys[leftIdx]);
            if (result < 0) {
                foundNode = node;
                foundIndex = leftIdx;
                node = node.left;
            } else if (result == 0) {
                foundNode = node;
                foundIndex = leftIdx;
                break;
            } else {
                if (leftIdx != rightIdx) {
                    result = cmp(object, key, keys[rightIdx]);
                }
                if (result > 0) {
                    node = node.right;
                } else { /* search in node */
                    foundNode = node;
                    foundIndex = rightIdx;
                    if (result == 0) {
                        break;
                    }
                    int low = leftIdx + 1;
                    int mid;
                    int high = rightIdx - 1;
                    while (low <= high && result != 0) {
                        mid = (low + high) >> 1;
                        result = cmp(object, key, keys[mid]);
                        if (result <= 0) {
                            foundNode = node;
                            foundIndex = mid;
                            high = mid - 1;
                        } else {
                            low = mid + 1;
                        }
                        if (result == 0 || (low == high && high == mid)) {
                            break;
                        }
                    }
                    break;
                }
            }
        }
        if (foundNode != null && cmp(object, key, foundNode.keys[foundIndex]) > 0) {
            foundNode = null;
        }
        if (foundNode != null) {
            return newEntry(foundNode, foundIndex);
        }
        return null;
    }

    private TTreeMap.Entry<K, V> findFloorEntry(K key) {
        if (root == null) {
            return null;
        }
        Comparable<K> object = comparator == null ? toComparable(key) : null;
        Node<K, V> node = root;
        Node<K, V> foundNode = null;
        int foundIndex = 0;
        while (node != null) {
            K[] keys = node.keys;
            int leftIdx = node.leftIdx;
            int result = cmp(object, key, keys[leftIdx]);
            if (result < 0) {
                node = node.left;
            } else {
                foundNode = node;
                foundIndex = leftIdx;
                if (result == 0) {
                    break;
                }
                int rightIdx = node.rightIdx;
                if (leftIdx != rightIdx) {
                    result = cmp(object, key, keys[rightIdx]);
                }
                if (result >= 0) {
                    foundNode = node;
                    foundIndex = rightIdx;
                    if (result == 0) {
                        break;
                    }
                    node = node.right;
                } else { /* search in node */
                    int low = leftIdx + 1;
                    int mid;
                    int high = rightIdx - 1;
                    while (low <= high && result != 0) {
                        mid = (low + high) >> 1;
                        result = cmp(object, key, keys[mid]);
                        if (result >= 0) {
                            foundNode = node;
                            foundIndex = mid;
                            low = mid + 1;
                        } else {
                            high = mid;
                        }
                        if (result == 0 || (low == high && high == mid)) {
                            break;
                        }
                    }
                    break;
                }
            }
        }
        if (foundNode != null && cmp(object, key, foundNode.keys[foundIndex]) < 0) {
            foundNode = null;
        }
        if (foundNode != null) {
            return newEntry(foundNode, foundIndex);
        }
        return null;
    }

    private TTreeMap.Entry<K, V> findLowerEntry(K key) {
        if (root == null) {
            return null;
        }
        Comparable<K> object = comparator == null ? toComparable(key) : null;
        Node<K, V> node = root;
        Node<K, V> foundNode = null;
        int foundIndex = 0;
        while (node != null) {
            K[] keys = node.keys;
            int leftIdx = node.leftIdx;
            int result = cmp(object, key, keys[leftIdx]);
            if (result <= 0) {
                node = node.left;
            } else {
                foundNode = node;
                foundIndex = leftIdx;
                int rightIdx = node.rightIdx;
                if (leftIdx != rightIdx) {
                    result = cmp(object, key, keys[rightIdx]);
                }
                if (result > 0) {
                    foundNode = node;
                    foundIndex = rightIdx;
                    node = node.right;
                } else { /* search in node */
                    int low = leftIdx + 1;
                    int mid;
                    int high = rightIdx - 1;
                    while (low <= high) {
                        mid = (low + high) >> 1;
                        result = cmp(object, key, keys[mid]);
                        if (result > 0) {
                            foundNode = node;
                            foundIndex = mid;
                            low = mid + 1;
                        } else {
                            high = mid;
                        }
                        if (low == high && high == mid) {
                            break;
                        }
                    }
                    break;
                }
            }
        }
        if (foundNode != null && cmp(object, key, foundNode.keys[foundIndex]) <= 0) {
            foundNode = null;
        }
        if (foundNode != null) {
            return newEntry(foundNode, foundIndex);
        }
        return null;
    }

    private Entry<K, V> findHigherEntry(K key) {
        if (root == null) {
            return null;
        }
        Comparable<K> object = comparator == null ? toComparable(key) : null;
        Node<K, V> node = root;
        Node<K, V> foundNode = null;
        int foundIndex = 0;
        while (node != null) {
            K[] keys = node.keys;
            int rightIdx = node.rightIdx;
            int result = cmp(object, key, keys[rightIdx]);
            if (result >= 0) {
                node = node.right;
            } else {
                int leftIdx = node.leftIdx;
                if (leftIdx != rightIdx) {
                    result = cmp(object, key, keys[leftIdx]);
                }
                foundNode = node;
                if (result < 0) {
                    foundIndex = leftIdx;
                    node = node.left;
                } else { /* search in node */
                    foundIndex = rightIdx;
                    int low = leftIdx + 1;
                    int mid;
                    int high = rightIdx - 1;
                    while (low <= high) {
                        mid = (low + high) >> 1;
                        result = cmp(object, key, keys[mid]);
                        if (result < 0) {
                            foundNode = node;
                            foundIndex = mid;
                            high = mid - 1;
                        } else {
                            low = mid + 1;
                        }
                        if (low == high && high == mid) {
                            break;
                        }
                    }
                    break;
                }
            }
        }
        if (foundNode != null && cmp(object, key, foundNode.keys[foundIndex]) >= 0) {
            foundNode = null;
        }
        if (foundNode != null) {
            return newEntry(foundNode, foundIndex);
        }
        return null;
    }

    @Override
    public K firstKey() {
        if (root != null) {
            Node<K, V> node = minimum(root);
            return node.keys[node.leftIdx];
        }
        throw new TNoSuchElementException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        Comparable<K> object = comparator == null ? toComparable((K) key) : null;
        K keyK = (K) key;
        Node<K, V> node = root;
        while (node != null) {
            K[] keys = node.keys;
            int leftIdx = node.leftIdx;
            int result = cmp(object, keyK, keys[leftIdx]);
            if (result < 0) {
                node = node.left;
            } else if (result == 0) {
                return node.values[leftIdx];
            } else {
                int rightIdx = node.rightIdx;
                if (leftIdx != rightIdx) {
                    result = cmp(object, keyK, keys[rightIdx]);
                }
                if (result > 0) {
                    node = node.right;
                } else if (result == 0) {
                    return node.values[rightIdx];
                } else { /* search in node */
                    int low = leftIdx + 1;
                    int mid;
                    int high = rightIdx - 1;
                    while (low <= high) {
                        mid = (low + high) >>> 1;
                        result = cmp(object, keyK, keys[mid]);
                        if (result > 0) {
                            low = mid + 1;
                        } else if (result == 0) {
                            return node.values[mid];
                        } else {
                            high = mid - 1;
                        }
                    }
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public TSet<K> keySet() {
        return navigableKeySet();
    }

    @Override
    public K lastKey() {
        if (root != null) {
            Node<K, V> node = maximum(root);
            return node.keys[node.rightIdx];
        }
        throw new TNoSuchElementException();
    }

    private static <K, V> Node<K, V> successor(Node<K, V> x) {
        if (x.right != null) {
            return minimum(x.right);
        }
        Node<K, V> y = x.parent;
        while (y != null && x == y.right) {
            x = y;
            y = y.parent;
        }
        return y;
    }

    private int cmp(Comparable<K> object, K key1, K key2) {
        return object != null ? object.compareTo(key2) : comparator.compare(key1, key2);
    }

    @Override
    public V put(K key, V value) {
        if (key == null && comparator == null) {
            throw new NullPointerException();
        }
        if (root == null) {
            root = createNode(key, value);
            size = 1;
            modCount++;
            return null;
        }
        Comparable<K> object = comparator == null ? toComparable(key) : null;
        Node<K, V> node = root;
        Node<K, V> prevNode = null;
        int result = 0;
        while (node != null) {
            prevNode = node;
            K[] keys = node.keys;
            int leftIdx = node.leftIdx;
            result = cmp(object, key, keys[leftIdx]);
            if (result < 0) {
                node = node.left;
            } else if (result == 0) {
                V res = node.values[leftIdx];
                node.values[leftIdx] = value;
                return res;
            } else {
                int rightIdx = node.rightIdx;
                if (leftIdx != rightIdx) {
                    result = cmp(object, key, keys[rightIdx]);
                }
                if (result > 0) {
                    node = node.right;
                } else if (result == 0) {
                    V res = node.values[rightIdx];
                    node.values[rightIdx] = value;
                    return res;
                } else { /* search in node */
                    int low = leftIdx + 1;
                    int mid;
                    int high = rightIdx - 1;
                    while (low <= high) {
                        mid = (low + high) >>> 1;
                        result = cmp(object, key, keys[mid]);
                        if (result > 0) {
                            low = mid + 1;
                        } else if (result == 0) {
                            V res = node.values[mid];
                            node.values[mid] = value;
                            return res;
                        } else {
                            high = mid - 1;
                        }
                    }
                    result = low;
                    break;
                }
            }
        } /* while */
        /*
         * if(node == null) { if(prevNode==null) { - case of empty Tree } else {
         * result < 0 - prevNode.left==null - attach here result > 0 -
         * prevNode.right==null - attach here } } else { insert into node.
         * result - index where it should be inserted. }
         */
        size++;
        modCount++;
        if (node == null) {
            if (prevNode == null) {
                // case of empty Tree
                root = createNode(key, value);
            } else if (prevNode.size < Node.NODE_SIZE) {
                // there is a place for insert
                if (result < 0) {
                    appendFromLeft(prevNode, key, value);
                } else {
                    appendFromRight(prevNode, key, value);
                }
            } else {
                // create and link
                Node<K, V> newNode = createNode(key, value);
                if (result < 0) {
                    attachToLeft(prevNode, newNode);
                } else {
                    attachToRight(prevNode, newNode);
                }
                balance(newNode);
            }
        } else {
            // insert into node.
            // result - index where it should be inserted.
            if (node.size < Node.NODE_SIZE) { // insert and ok
                int leftIdx = node.leftIdx;
                int rightIdx = node.rightIdx;
                if (leftIdx == 0 || ((rightIdx != Node.NODE_SIZE - 1) && (rightIdx - result <= result - leftIdx))) {
                    int rightIdxPlus1 = rightIdx + 1;
                    System.arraycopy(node.keys, result, node.keys, result + 1, rightIdxPlus1 - result);
                    System.arraycopy(node.values, result, node.values, result + 1, rightIdxPlus1 - result);
                    node.rightIdx = rightIdxPlus1;
                    node.keys[result] = key;
                    node.values[result] = value;
                } else {
                    int leftIdxMinus1 = leftIdx - 1;
                    System.arraycopy(node.keys, leftIdx, node.keys, leftIdxMinus1, result - leftIdx);
                    System.arraycopy(node.values, leftIdx, node.values, leftIdxMinus1, result - leftIdx);
                    node.leftIdx = leftIdxMinus1;
                    node.keys[result - 1] = key;
                    node.values[result - 1] = value;
                }
                node.size++;
            } else {
                // there are no place here
                // insert and push old pair
                Node<K, V> previous = node.prev;
                Node<K, V> nextNode = node.next;
                boolean removeFromStart;
                boolean attachFromLeft = false;
                Node<K, V> attachHere = null;
                if (previous == null) {
                    if (nextNode != null && nextNode.size < Node.NODE_SIZE) {
                        // move last pair to next
                        removeFromStart = false;
                    } else {
                        // next node doesn't exist or full
                        // left==null
                        // drop first pair to new node from left
                        removeFromStart = true;
                        attachFromLeft = true;
                        attachHere = node;
                    }
                } else if (nextNode == null) {
                    if (previous.size < Node.NODE_SIZE) {
                        // move first pair to prev
                        removeFromStart = true;
                    } else {
                        // right == null;
                        // drop last pair to new node from right
                        removeFromStart = false;
                        attachFromLeft = false;
                        attachHere = node;
                    }
                } else {
                    if (previous.size < Node.NODE_SIZE) {
                        if (nextNode.size < Node.NODE_SIZE) {
                            // choose prev or next for moving
                            removeFromStart = previous.size < nextNode.size;
                        } else {
                            // move first pair to prev
                            removeFromStart = true;
                        }
                    } else {
                        if (nextNode.size >= Node.NODE_SIZE) {
                            // prev & next are full
                            // if node.right!=null then node.next.left==null
                            // if node.left!=null then node.prev.right==null
                            if (node.right == null) {
                                attachHere = node;
                                attachFromLeft = false;
                            } else {
                                attachHere = nextNode;
                                attachFromLeft = true;
                            }
                        }
                        removeFromStart = false;
                    }
                }
                K movedKey;
                V movedValue;
                if (removeFromStart) {
                    // node.leftIdx == 0
                    movedKey = node.keys[0];
                    movedValue = node.values[0];
                    int resMinus1 = result - 1;
                    System.arraycopy(node.keys, 1, node.keys, 0, resMinus1);
                    System.arraycopy(node.values, 1, node.values, 0, resMinus1);
                    node.keys[resMinus1] = key;
                    node.values[resMinus1] = value;
                } else {
                    // node.rightIdx == Node.NODE_SIZE - 1
                    movedKey = node.keys[Node.NODE_SIZE - 1];
                    movedValue = node.values[Node.NODE_SIZE - 1];
                    System.arraycopy(node.keys, result, node.keys, result + 1, Node.NODE_SIZE - 1 - result);
                    System.arraycopy(node.values, result, node.values, result + 1, Node.NODE_SIZE - 1 - result);
                    node.keys[result] = key;
                    node.values[result] = value;
                }
                if (attachHere == null) {
                    if (removeFromStart) {
                        appendFromRight(previous, movedKey, movedValue);
                    } else {
                        appendFromLeft(nextNode, movedKey, movedValue);
                    }
                } else {
                    Node<K, V> newNode = createNode(movedKey, movedValue);
                    if (attachFromLeft) {
                        attachToLeft(attachHere, newNode);
                    } else {
                        attachToRight(attachHere, newNode);
                    }
                    balance(newNode);
                }
            }
        }
        return null;
    }

    private void appendFromLeft(Node<K, V> node, K keyObj, V value) {
        if (node.leftIdx == 0) {
            int newRight = node.rightIdx + 1;
            System.arraycopy(node.keys, 0, node.keys, 1, newRight);
            System.arraycopy(node.values, 0, node.values, 1, newRight);
            node.rightIdx = newRight;
        } else {
            node.leftIdx--;
        }
        node.size++;
        node.keys[node.leftIdx] = keyObj;
        node.values[node.leftIdx] = value;
    }

    private void attachToLeft(Node<K, V> node, Node<K, V> newNode) {
        newNode.parent = node;
        // node.left==null - attach here
        node.left = newNode;
        Node<K, V> predecessor = node.prev;
        newNode.prev = predecessor;
        newNode.next = node;
        if (predecessor != null) {
            predecessor.next = newNode;
        }
        node.prev = newNode;
    }

    /*
     * add pair into node; existence free room in the node should be checked
     * before call
     */
    private void appendFromRight(Node<K, V> node, K keyObj, V value) {
        if (node.rightIdx == Node.NODE_SIZE - 1) {
            int leftIdx = node.leftIdx;
            int newLeft = leftIdx - 1;
            System.arraycopy(node.keys, leftIdx, node.keys, newLeft, Node.NODE_SIZE - leftIdx);
            System.arraycopy(node.values, leftIdx, node.values, newLeft, Node.NODE_SIZE - leftIdx);
            node.leftIdx = newLeft;
        } else {
            node.rightIdx++;
        }
        node.size++;
        node.keys[node.rightIdx] = keyObj;
        node.values[node.rightIdx] = value;
    }

    private void attachToRight(Node<K, V> node, Node<K, V> newNode) {
        newNode.parent = node;
        // - node.right==null - attach here
        node.right = newNode;
        newNode.prev = node;
        Node<K, V> successor = node.next;
        newNode.next = successor;
        if (successor != null) {
            successor.prev = newNode;
        }
        node.next = newNode;
    }

    private Node<K, V> createNode(K keyObj, V value) {
        Node<K, V> node = new Node<>();
        node.keys[0] = keyObj;
        node.values[0] = value;
        node.leftIdx = 0;
        node.rightIdx = 0;
        node.size = 1;
        return node;
    }

    private void balance(Node<K, V> x) {
        Node<K, V> y;
        x.color = true;
        while (x != root && x.parent.color) {
            if (x.parent == x.parent.parent.left) {
                y = x.parent.parent.right;
                if (y != null && y.color) {
                    x.parent.color = false;
                    y.color = false;
                    x.parent.parent.color = true;
                    x = x.parent.parent;
                } else {
                    if (x == x.parent.right) {
                        x = x.parent;
                        leftRotate(x);
                    }
                    x.parent.color = false;
                    x.parent.parent.color = true;
                    rightRotate(x.parent.parent);
                }
            } else {
                y = x.parent.parent.left;
                if (y != null && y.color) {
                    x.parent.color = false;
                    y.color = false;
                    x.parent.parent.color = true;
                    x = x.parent.parent;
                } else {
                    if (x == x.parent.left) {
                        x = x.parent;
                        rightRotate(x);
                    }
                    x.parent.color = false;
                    x.parent.parent.color = true;
                    leftRotate(x.parent.parent);
                }
            }
        }
        root.color = false;
    }

    private void rightRotate(Node<K, V> x) {
        Node<K, V> y = x.left;
        x.left = y.right;
        if (y.right != null) {
            y.right.parent = x;
        }
        y.parent = x.parent;
        if (x.parent == null) {
            root = y;
        } else {
            if (x == x.parent.right) {
                x.parent.right = y;
            } else {
                x.parent.left = y;
            }
        }
        y.right = x;
        x.parent = y;
    }

    private void leftRotate(Node<K, V> x) {
        Node<K, V> y = x.right;
        x.right = y.left;
        if (y.left != null) {
            y.left.parent = x;
        }
        y.parent = x.parent;
        if (x.parent == null) {
            root = y;
        } else {
            if (x == x.parent.left) {
                x.parent.left = y;
            } else {
                x.parent.right = y;
            }
        }
        y.left = x;
        x.parent = y;
    }

    @Override
    public void putAll(TMap<? extends K, ? extends V> map) {
        for (TIterator<? extends TMap.Entry<? extends K, ? extends V>> it = map.entrySet().iterator(); it.hasNext();) {
            TMap.Entry<? extends K, ? extends V> entry = it.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public V remove(Object key) {
        Comparable<K> object = comparator == null ? toComparable((K) key) : null;
        if (size == 0) {
            return null;
        }
        K keyK = (K) key;
        Node<K, V> node = root;
        while (node != null) {
            K[] keys = node.keys;
            int leftIdx = node.leftIdx;
            int result = cmp(object, keyK, keys[leftIdx]);
            if (result < 0) {
                node = node.left;
            } else if (result == 0) {
                V value = node.values[leftIdx];
                removeLeftmost(node, false);
                return value;
            } else {
                int rightIdx = node.rightIdx;
                if (leftIdx != rightIdx) {
                    result = cmp(object, keyK, keys[rightIdx]);
                }
                if (result > 0) {
                    node = node.right;
                } else if (result == 0) {
                    V value = node.values[rightIdx];
                    removeRightmost(node, false);
                    return value;
                } else { /* search in node */
                    int low = leftIdx + 1;
                    int mid;
                    int high = rightIdx - 1;
                    while (low <= high) {
                        mid = (low + high) >>> 1;
                        result = cmp(object, keyK, keys[mid]);
                        if (result > 0) {
                            low = mid + 1;
                        } else if (result == 0) {
                            V value = node.values[mid];
                            removeMiddleElement(node, mid, false);
                            return value;
                        } else {
                            high = mid - 1;
                        }
                    }
                    return null;
                }
            }
        }
        return null;
    }

    private static <K, V> int natural(Node<K, V> node, boolean reverse) {
        if (node == null) {
            return 0;
        }
        return reverse ? node.rightIdx : node.leftIdx;
    }

    private Entry<K, V> removeLeftmost(Node<K, V> node, boolean reverse) {
        int index = node.leftIdx;
        Node<K, V> sn;
        int si;
        if (node.size == 1) {
            deleteNode(node);
            sn = reverse ? node.prev : node.next;
            si = natural(sn, reverse);
        } else if (node.prev != null && (Node.NODE_SIZE - 1 - node.prev.rightIdx) > node.size) {
            // move all to prev node and kill it
            Node<K, V> prev = node.prev;
            int size = node.rightIdx - index;
            System.arraycopy(node.keys, index + 1, prev.keys, prev.rightIdx + 1, size);
            System.arraycopy(node.values, index + 1, prev.values, prev.rightIdx + 1, size);
            prev.rightIdx += size;
            prev.size += size;
            deleteNode(node);
            sn = prev;
            si = reverse ? prev.rightIdx - size : prev.rightIdx - size + 1;
        } else if (node.next != null && (node.next.leftIdx) > node.size) {
            // move all to next node and kill it
            Node<K, V> next = node.next;
            int size = node.rightIdx - index;
            int nextNewLeft = next.leftIdx - size;
            next.leftIdx = nextNewLeft;
            System.arraycopy(node.keys, index + 1, next.keys, nextNewLeft, size);
            System.arraycopy(node.values, index + 1, next.values, nextNewLeft, size);
            next.size += size;
            deleteNode(node);
            sn = reverse ? node.prev : next;
            si = natural(sn, reverse);
        } else {
            node.keys[index] = null;
            node.values[index] = null;
            node.leftIdx++;
            node.size--;
            Node<K, V> prev = node.prev;
            sn = reverse ? prev : node;
            si = natural(sn, reverse);
            if (prev != null && prev.size == 1) {
                node.size++;
                node.leftIdx--;
                node.keys[node.leftIdx] = prev.keys[prev.leftIdx];
                node.values[node.leftIdx] = prev.values[prev.leftIdx];
                deleteNode(prev);
                if (reverse) {
                    sn = node;
                    si = node.leftIdx;
                }
            }
        }
        modCount++;
        size--;
        return sn == null ? null : newEntry(sn, si);
    }

    private Entry<K, V> removeRightmost(Node<K, V> node, boolean reverse) {
        int index = node.rightIdx;
        Node<K, V> sn;
        int si;
        if (node.size == 1) {
            deleteNode(node);
            sn = reverse ? node.prev : node.next;
            si = natural(sn, reverse);
        } else if (node.prev != null && (Node.NODE_SIZE - 1 - node.prev.rightIdx) > node.size) {
            // move all to prev node and kill it
            Node<K, V> prev = node.prev;
            int leftIdx = node.leftIdx;
            int size = index - leftIdx;
            System.arraycopy(node.keys, leftIdx, prev.keys, prev.rightIdx + 1, size);
            System.arraycopy(node.values, leftIdx, prev.values, prev.rightIdx + 1, size);
            prev.rightIdx += size;
            prev.size += size;
            deleteNode(node);
            sn = reverse ? prev : node.next;
            si = natural(sn, reverse);
        } else if (node.next != null && (node.next.leftIdx) > node.size) {
            // move all to next node and kill it
            Node<K, V> next = node.next;
            int leftIdx = node.leftIdx;
            int size = index - leftIdx;
            int nextNewLeft = next.leftIdx - size;
            next.leftIdx = nextNewLeft;
            System.arraycopy(node.keys, leftIdx, next.keys, nextNewLeft, size);
            System.arraycopy(node.values, leftIdx, next.values, nextNewLeft, size);
            next.size += size;
            deleteNode(node);
            sn = next;
            si = reverse ? next.leftIdx + size - 1 : next.leftIdx + size;
        } else {
            node.keys[index] = null;
            node.values[index] = null;
            node.rightIdx--;
            node.size--;
            Node<K, V> next = node.next;
            sn = reverse ? node : next;
            si = natural(sn, reverse);
            if (next != null && next.size == 1) {
                node.size++;
                node.rightIdx++;
                node.keys[node.rightIdx] = next.keys[next.leftIdx];
                node.values[node.rightIdx] = next.values[next.leftIdx];
                deleteNode(next);
                if (!reverse) {
                    sn = node;
                    si = node.rightIdx;
                }
            }
        }
        modCount++;
        size--;
        return sn == null ? null : newEntry(sn, si);
    }

    private Entry<K, V> removeMiddleElement(Node<K, V> node, int index, boolean reverse) {
        // this function is called iff index if some middle element;
        // so node.leftIdx < index < node.rightIdx
        // condition above assume that node.size > 1
        Node<K, V> sn;
        int si;
        if (node.prev != null && (Node.NODE_SIZE - 1 - node.prev.rightIdx) > node.size) {
            // move all to prev node and kill it
            Node<K, V> prev = node.prev;
            int leftIdx = node.leftIdx;
            int size = index - leftIdx;
            System.arraycopy(node.keys, leftIdx, prev.keys, prev.rightIdx + 1, size);
            System.arraycopy(node.values, leftIdx, prev.values, prev.rightIdx + 1, size);
            prev.rightIdx += size;
            size = node.rightIdx - index;
            System.arraycopy(node.keys, index + 1, prev.keys, prev.rightIdx + 1, size);
            System.arraycopy(node.values, index + 1, prev.values, prev.rightIdx + 1, size);
            prev.rightIdx += size;
            prev.size += node.size - 1;
            deleteNode(node);
            sn = prev;
            si = reverse ? prev.rightIdx - size : prev.rightIdx - size + 1;
        } else if (node.next != null && (node.next.leftIdx) > node.size) {
            // move all to next node and kill it
            Node<K, V> next = node.next;
            int leftIdx = node.leftIdx;
            int nextNewLeft = next.leftIdx - node.size + 1;
            next.leftIdx = nextNewLeft;
            int size = index - leftIdx;
            System.arraycopy(node.keys, leftIdx, next.keys, nextNewLeft, size);
            System.arraycopy(node.values, leftIdx, next.values, nextNewLeft, size);
            nextNewLeft += size;
            size = node.rightIdx - index;
            System.arraycopy(node.keys, index + 1, next.keys, nextNewLeft, size);
            System.arraycopy(node.values, index + 1, next.values, nextNewLeft, size);
            next.size += node.size - 1;
            deleteNode(node);
            sn = next;
            si = reverse ? next.leftIdx - node.leftIdx + index - 1 : next.leftIdx - node.leftIdx + index;
        } else {
            int moveFromRight = node.rightIdx - index;
            int leftIdx = node.leftIdx;
            int moveFromLeft = index - leftIdx;
            if (moveFromRight <= moveFromLeft) {
                System.arraycopy(node.keys, index + 1, node.keys, index, moveFromRight);
                System.arraycopy(node.values, index + 1, node.values, index, moveFromRight);
                Node<K, V> next = node.next;
                if (next != null && next.size == 1) {
                    node.keys[node.rightIdx] = next.keys[next.leftIdx];
                    node.values[node.rightIdx] = next.values[next.leftIdx];
                    deleteNode(next);
                } else {
                    node.keys[node.rightIdx] = null;
                    node.values[node.rightIdx] = null;
                    node.rightIdx--;
                    node.size--;
                }
                sn = node;
                si = reverse ? index - 1 : index;
            } else {
                System.arraycopy(node.keys, leftIdx, node.keys, leftIdx + 1, moveFromLeft);
                System.arraycopy(node.values, leftIdx, node.values, leftIdx + 1, moveFromLeft);
                Node<K, V> prev = node.prev;
                if (prev != null && prev.size == 1) {
                    node.keys[leftIdx] = prev.keys[prev.leftIdx];
                    node.values[leftIdx] = prev.values[prev.leftIdx];
                    deleteNode(prev);
                } else {
                    node.keys[leftIdx] = null;
                    node.values[leftIdx] = null;
                    node.leftIdx++;
                    node.size--;
                }
                sn = node;
                si = reverse ? index : index + 1;
            }
        }
        modCount++;
        size--;
        return newEntry(sn, si);
    }

    private void deleteNode(Node<K, V> node) {
        if (node.right == null) {
            if (node.left != null) {
                attachToParent(node, node.left);
            } else {
                attachNullToParent(node);
            }
            fixNextChain(node);
        } else if (node.left == null) { // node.right != null
            attachToParent(node, node.right);
            fixNextChain(node);
        } else {
            // Here node.left!=nul && node.right!=null
            // node.next should replace node in tree
            // node.next!=null by tree logic.
            // node.next.left==null by tree logic.
            // node.next.right may be null or non-null
            Node<K, V> toMoveUp = node.next;
            fixNextChain(node);
            if (toMoveUp.right == null) {
                attachNullToParent(toMoveUp);
            } else {
                attachToParent(toMoveUp, toMoveUp.right);
            }
            // Here toMoveUp is ready to replace node
            toMoveUp.left = node.left;
            if (node.left != null) {
                node.left.parent = toMoveUp;
            }
            toMoveUp.right = node.right;
            if (node.right != null) {
                node.right.parent = toMoveUp;
            }
            attachToParentNoFixup(node, toMoveUp);
            toMoveUp.color = node.color;
        }
    }

    private void attachToParentNoFixup(Node<K, V> toDelete, Node<K, V> toConnect) {
        // assert toConnect!=null
        Node<K, V> parent = toDelete.parent;
        toConnect.parent = parent;
        if (parent == null) {
            root = toConnect;
        } else if (toDelete == parent.left) {
            parent.left = toConnect;
        } else {
            parent.right = toConnect;
        }
    }

    private void attachToParent(Node<K, V> toDelete, Node<K, V> toConnect) {
        // assert toConnect!=null
        attachToParentNoFixup(toDelete, toConnect);
        if (!toDelete.color) {
            fixup(toConnect);
        }
    }

    private void attachNullToParent(Node<K, V> toDelete) {
        Node<K, V> parent = toDelete.parent;
        if (parent == null) {
            root = null;
        } else {
            if (toDelete == parent.left) {
                parent.left = null;
            } else {
                parent.right = null;
            }
            if (!toDelete.color) {
                fixup(parent);
            }
        }
    }

    private void fixNextChain(Node<K, V> node) {
        if (node.prev != null) {
            node.prev.next = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        }
    }

    private void fixup(Node<K, V> x) {
        Node<K, V> w;
        while (x != root && !x.color) {
            if (x == x.parent.left) {
                w = x.parent.right;
                if (w == null) {
                    x = x.parent;
                    continue;
                }
                if (w.color) {
                    w.color = false;
                    x.parent.color = true;
                    leftRotate(x.parent);
                    w = x.parent.right;
                    if (w == null) {
                        x = x.parent;
                        continue;
                    }
                }
                if ((w.left == null || !w.left.color) && (w.right == null || !w.right.color)) {
                    w.color = true;
                    x = x.parent;
                } else {
                    if (w.right == null || !w.right.color) {
                        w.left.color = false;
                        w.color = true;
                        rightRotate(w);
                        w = x.parent.right;
                    }
                    w.color = x.parent.color;
                    x.parent.color = false;
                    w.right.color = false;
                    leftRotate(x.parent);
                    x = root;
                }
            } else {
                w = x.parent.left;
                if (w == null) {
                    x = x.parent;
                    continue;
                }
                if (w.color) {
                    w.color = false;
                    x.parent.color = true;
                    rightRotate(x.parent);
                    w = x.parent.left;
                    if (w == null) {
                        x = x.parent;
                        continue;
                    }
                }
                if ((w.left == null || !w.left.color) && (w.right == null || !w.right.color)) {
                    w.color = true;
                    x = x.parent;
                } else {
                    if (w.left == null || !w.left.color) {
                        w.right.color = false;
                        w.color = true;
                        leftRotate(w);
                        w = x.parent.left;
                    }
                    w.color = x.parent.color;
                    x.parent.color = false;
                    w.left.color = false;
                    rightRotate(x.parent);
                    x = root;
                }
            }
        }
        x.color = false;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public TCollection<V> values() {
        if (cachedValues == null) {
            cachedValues = new TAbstractCollection<V>() {
                @Override
                public boolean contains(Object object) {
                    return containsValue(object);
                }

                @Override
                public int size() {
                    return size;
                }

                @Override
                public void clear() {
                    TTreeMap.this.clear();
                }

                @Override
                public TIterator<V> iterator() {
                    return new UnboundedValueIterator<>(TTreeMap.this);
                }
            };
        }
        return cachedValues;
    }

    @Override
    public TMap.Entry<K, V> firstEntry() {
        return newImmutableEntry(findSmallestEntry());
    }

    @Override
    public TMap.Entry<K, V> lastEntry() {
        return newImmutableEntry(findBiggestEntry());
    }

    @Override
    public TMap.Entry<K, V> pollFirstEntry() {
        TTreeMap.Entry<K, V> node = findSmallestEntry();
        TMap.Entry<K, V> result = newImmutableEntry(node);
        if (null != node) {
            remove(node.key);
        }
        return result;
    }

    @Override
    public TMap.Entry<K, V> pollLastEntry() {
        TTreeMap.Entry<K, V> node = findBiggestEntry();
        TMap.Entry<K, V> result = newImmutableEntry(node);
        if (null != node) {
            remove(node.key);
        }
        return result;
    }

    @Override
    public TMap.Entry<K, V> higherEntry(K key) {
        return newImmutableEntry(findHigherEntry(key));
    }

    @Override
    public K higherKey(K key) {
        TMap.Entry<K, V> entry = higherEntry(key);
        return (null == entry) ? null : entry.getKey();
    }

    @Override
    public TMap.Entry<K, V> lowerEntry(K key) {
        return newImmutableEntry(findLowerEntry(key));
    }

    @Override
    public K lowerKey(K key) {
        TMap.Entry<K, V> entry = lowerEntry(key);
        return (null == entry) ? null : entry.getKey();
    }

    @Override
    public TMap.Entry<K, V> ceilingEntry(K key) {
        return newImmutableEntry(findCeilingEntry(key));
    }

    @Override
    public K ceilingKey(K key) {
        TMap.Entry<K, V> entry = ceilingEntry(key);
        return (null == entry) ? null : entry.getKey();
    }

    @Override
    public TMap.Entry<K, V> floorEntry(K key) {
        return newImmutableEntry(findFloorEntry(key));
    }

    @Override
    public K floorKey(K key) {
        TMap.Entry<K, V> entry = floorEntry(key);
        return (null == entry) ? null : entry.getKey();
    }

    @SuppressWarnings("unchecked")
    private static <T> Comparable<T> toComparable(T obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        return (Comparable<T>) obj;
    }

    private int keyCompare(K left, K right) {
        return (null != comparator()) ? comparator().compare(left, right) : toComparable(left).compareTo(right);
    }

    private static <K, V> Node<K, V> minimum(Node<K, V> x) {
        if (x == null) {
            return null;
        }
        while (x.left != null) {
            x = x.left;
        }
        return x;
    }

    private static <K, V> Node<K, V> maximum(Node<K, V> x) {
        if (x == null) {
            return null;
        }
        while (x.right != null) {
            x = x.right;
        }
        return x;
    }

    @Override
    public TSet<TMap.Entry<K, V>> entrySet() {
        if (entrySet == null) {
            entrySet = new TAbstractSet<TMap.Entry<K, V>>() {
                @Override
                public int size() {
                    return size;
                }

                @Override
                public void clear() {
                    TTreeMap.this.clear();
                }

                @SuppressWarnings("unchecked")
                @Override
                public boolean contains(Object object) {
                    if (object instanceof TMap.Entry) {
                        TMap.Entry<K, V> entry = (TMap.Entry<K, V>) object;
                        K key = entry.getKey();
                        Object v1 = get(key);
                        Object v2 = entry.getValue();
                        return v1 == null ? (v2 == null && TTreeMap.this.containsKey(key)) : v1.equals(v2);
                    }
                    return false;
                }

                @Override
                public TIterator<TMap.Entry<K, V>> iterator() {
                    return new UnboundedEntryIterator<>(TTreeMap.this);
                }
            };
        }
        return entrySet;
    }

    @Override
    public TNavigableSet<K> navigableKeySet() {
        if (navigableKeySet == null) {
            navigableKeySet = (new AscendingSubMap<K, V>(this)).navigableKeySet();
        }
        return navigableKeySet;
    }

    @Override
    public TNavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    @Override
    public TNavigableMap<K, V> descendingMap() {
        if (descendingMap == null) {
            descendingMap = new DescendingSubMap<>(this);
        }
        return descendingMap;
    }

    @Override
    public TNavigableMap<K, V> subMap(K start, boolean startInclusive, K end, boolean endInclusive) {
        if (keyCompare(start, end) <= 0) {
            return new AscendingSubMap<>(start, startInclusive, this, end, endInclusive);
        }
        throw new IllegalArgumentException();
    }

    @Override
    public TNavigableMap<K, V> headMap(K end, boolean inclusive) {
        // check for error
        keyCompare(end, end);
        return new AscendingSubMap<>(this, end, inclusive);
    }

    @Override
    public TNavigableMap<K, V> tailMap(K start, boolean inclusive) {
        // check for error
        keyCompare(start, start);
        return new AscendingSubMap<>(start, inclusive, this);
    }

    @Override
    public TSortedMap<K, V> subMap(K startKey, K endKey) {
        return subMap(startKey, true, endKey, false);
    }

    @Override
    public TSortedMap<K, V> headMap(K endKey) {
        return headMap(endKey, false);
    }

    @Override
    public TSortedMap<K, V> tailMap(K startKey) {
        return tailMap(startKey, true);
    }

    static class AbstractMapIterator<K, V> {
        TTreeMap<K, V> backingMap;

        int expectedModCount;

        Node<K, V> node;

        Node<K, V> lastNode;

        int offset;

        int lastOffset;

        AbstractMapIterator(TTreeMap<K, V> map) {
            backingMap = map;
            expectedModCount = map.modCount;
            node = minimum(map.root);
            if (node != null) {
                offset = node.leftIdx;
            }
        }

        public boolean hasNext() {
            return node != null;
        }

        final void makeNext() {
            if (expectedModCount != backingMap.modCount) {
                throw new TConcurrentModificationException();
            } else if (node == null) {
                throw new TNoSuchElementException();
            }
            lastNode = node;
            lastOffset = offset;
            if (offset != node.rightIdx) {
                offset++;
            } else {
                node = node.next;
                if (node != null) {
                    offset = node.leftIdx;
                }
            }
        }

        final public void remove() {
            if (lastNode == null) {
                throw new IllegalStateException();
            }
            if (expectedModCount != backingMap.modCount) {
                throw new TConcurrentModificationException();
            }

            Entry<K, V> entry;
            int idx = lastOffset;
            if (idx == lastNode.leftIdx) {
                entry = backingMap.removeLeftmost(lastNode, false);
            } else if (idx == lastNode.rightIdx) {
                entry = backingMap.removeRightmost(lastNode, false);
            } else {
                entry = backingMap.removeMiddleElement(node, idx, false);
            }
            if (entry != null) {
                node = entry.node;
                offset = entry.index;
            }
            lastNode = null;
            expectedModCount++;
        }
    }

    static class UnboundedEntryIterator<K, V> extends AbstractMapIterator<K, V> implements TIterator<TMap.Entry<K, V>> {

        UnboundedEntryIterator(TTreeMap<K, V> map) {
            super(map);
        }

        @Override
        public TMap.Entry<K, V> next() {
            makeNext();
            return newEntry(lastNode, lastOffset);
        }
    }

    static class UnboundedValueIterator<K, V> extends AbstractMapIterator<K, V> implements TIterator<V> {

        UnboundedValueIterator(TTreeMap<K, V> map) {
            super(map);
        }

        @Override
        public V next() {
            makeNext();
            return lastNode.values[lastOffset];
        }
    }
}