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
import org.teavm.classlib.java.lang.*;

public class TTreeMap<K, V> extends TAbstractMap<K, V> implements TSortedMap<K, V>, TCloneable, TSerializable {
    transient int size;
    private TComparator<? super K> comparator;
    transient int modCount;
    transient TSet<Entry<K, V>> entrySet;
    transient Node<K, V> root;

    class MapEntry extends TObject implements Entry<K, V>, TCloneable {
        final int offset;
        final Node<K, V> node;
        final K key;

        MapEntry(Node<K, V> node, int offset) {
            this.node = node;
            this.offset = offset;
            key = node.keys[offset];
        }

        @Override
        public TObject clone() {
            try {
                return super.clone();
            } catch (TCloneNotSupportedException e) {
                return null;
            }
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object instanceof Entry) {
                Entry<?, ?> entry = (Entry<?, ?>) object;
                V value = getValue();
                return (key == null ? entry.getKey() == null : key.equals(entry.getKey()))
                        && (value == null ? entry.getValue() == null : value.equals(entry.getValue()));
            }
            return false;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            if (node.keys[offset] == key) {
                return node.values[offset];
            }
            if (containsKey(key)) {
                return get(key);
            }
            throw new TIllegalStateException();
        }

        @Override
        public int hashCode() {
            V value = getValue();
            return (key == null ? 0 : key.hashCode())
                    ^ (value == null ? 0 : value.hashCode());
        }

        @Override
        public V setValue(V object) {
            if (node.keys[offset] == key) {
                V res = node.values[offset];
                node.values[offset] = object;
                return res;
            }
            if (containsKey(key)) {
                return put(key, object);
            }
            throw new TIllegalStateException();
        }

        @Override
        public String toString() {
            return key + "=" + getValue();
        }
    }

    static class Node <K, V> extends TObject implements TCloneable {
        static final int NODE_SIZE = 64;
        Node<K, V> prev, next;
        Node<K, V> parent, left, right;
        V[] values;
        K[] keys;
        int left_idx = 0;
        int right_idx = -1;
        int size = 0;
        boolean color;

        @SuppressWarnings("unchecked")
        public Node() {
            keys = (K[]) new Object[NODE_SIZE];
            values = (V[]) new Object[NODE_SIZE];
        }

        @SuppressWarnings("unchecked")
        Node<K, V> clone(Node<K, V> parent) throws TCloneNotSupportedException {
            Node<K, V> clone = (Node<K, V>) super.clone();
            clone.keys   = TArrays.copyOf(keys, keys.length);
            clone.values = TArrays.copyOf(values, values.length);
            clone.left_idx  = left_idx;
            clone.right_idx = right_idx;
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

    @SuppressWarnings("unchecked")
    private static <T> TComparable<T> toComparable(T obj) {
        if (obj == null) {
            throw new TNullPointerException();
        }
        return (TComparable<T>) obj;
    }

    static class AbstractMapIterator <K,V> {
        TTreeMap<K, V> backingMap;
        int expectedModCount;
        Node<K, V> node;
        Node<K, V> lastNode;
        int offset;
        int lastOffset;

        AbstractMapIterator(TTreeMap<K, V> map, Node<K, V> startNode, int startOffset) {
            backingMap = map;
            expectedModCount = map.modCount;
            node = startNode;
            offset = startOffset;
        }

        AbstractMapIterator(TTreeMap<K, V> map, Node<K, V> startNode) {
            this(map, startNode, startNode != null ? startNode.right_idx - startNode.left_idx : 0);
        }

        AbstractMapIterator(TTreeMap<K, V> map) {
            this(map, minimum(map.root));
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
            if (offset != 0) {
                offset--;
            } else {
                node = node.next;
                if (node != null) {
                    offset = node.right_idx - node.left_idx;
                }
            }
        }

        final public void remove() {
            if (expectedModCount == backingMap.modCount) {
                if (lastNode != null) {
                    int idx = lastNode.right_idx - lastOffset;
                    backingMap.removeFromIterator(lastNode, idx);
                    lastNode = null;
                    expectedModCount++;
                } else {
                    throw new TIllegalStateException();
                }
            } else {
                throw new TConcurrentModificationException();
            }
        }
    }

    static class UnboundedEntryIterator <K, V> extends AbstractMapIterator<K, V>
            implements TIterator<Entry<K, V>> {

        UnboundedEntryIterator(TTreeMap<K, V> map, Node<K, V> startNode, int startOffset) {
            super(map, startNode, startOffset);
        }

        UnboundedEntryIterator(TTreeMap<K, V> map) {
            super(map);
        }

        @Override
        public Entry<K, V> next() {
            makeNext();
            int idx = lastNode.right_idx - lastOffset;
            return backingMap.new MapEntry(lastNode, idx);
        }
    }

    static class UnboundedKeyIterator <K, V> extends AbstractMapIterator<K, V>
                                                          implements TIterator<K> {

        UnboundedKeyIterator(TTreeMap<K, V> map, Node<K, V> startNode, int startOffset) {
            super(map, startNode, startOffset);
        }

        UnboundedKeyIterator(TTreeMap<K, V> map) {
            super(map);
        }

        @Override
        public K next() {
            makeNext();
            return lastNode.keys[lastNode.right_idx - lastOffset];
        }
    }

    static class UnboundedValueIterator <K, V> extends AbstractMapIterator<K, V>
                                                          implements TIterator<V> {

        UnboundedValueIterator(TTreeMap<K, V> map, Node<K, V> startNode, int startOffset) {
            super(map, startNode, startOffset);
        }

        UnboundedValueIterator(TTreeMap<K, V> map) {
            super(map);
        }

        @Override
        public V next() {
            makeNext();
            return lastNode.values[lastNode.right_idx - lastOffset];
        }
    }

    static class BoundedMapIterator <K, V> extends AbstractMapIterator<K, V> {

        Node<K, V> finalNode;
        int finalOffset;

        BoundedMapIterator(Node<K, V> startNode, int startOffset, TTreeMap<K, V> map,
                           Node<K, V> finalNode, int finalOffset) {
            super(map, finalNode == null ? null : startNode, startOffset);
            this.finalNode = finalNode;
            this.finalOffset = finalOffset;
        }

        BoundedMapIterator(Node<K, V> startNode, TTreeMap<K, V> map,
                           Node<K, V> finalNode, int finalOffset) {
            this(startNode, startNode != null ?
                            startNode.right_idx - startNode.left_idx : 0,
                            map, finalNode, finalOffset);
        }

        BoundedMapIterator(Node<K, V> startNode, int startOffset,
                           TTreeMap<K, V> map, Node<K, V> finalNode) {
            this(startNode, startOffset, map, finalNode,
                         finalNode.right_idx - finalNode.left_idx);
        }

        void makeBoundedNext() {
            makeNext();
            if (lastNode == finalNode && lastOffset == finalOffset) {
                node = null;
            }
        }
    }

    static class BoundedEntryIterator <K, V> extends BoundedMapIterator<K, V>
                                          implements TIterator<Entry<K, V>> {

        public BoundedEntryIterator(Node<K, V> startNode, int startOffset, TTreeMap<K, V> map,
                                    Node<K, V> finalNode, int finalOffset) {
            super(startNode, startOffset, map, finalNode, finalOffset);
        }

        @Override
        public Entry<K, V> next() {
            makeBoundedNext();
            int idx = lastNode.right_idx - lastOffset;
            return backingMap.new MapEntry(lastNode, idx);
        }
    }

    static class BoundedKeyIterator <K, V> extends BoundedMapIterator<K, V>
                                                     implements TIterator<K> {

        public BoundedKeyIterator(Node<K, V> startNode, int startOffset, TTreeMap<K, V> map,
                                  Node<K, V> finalNode, int finalOffset) {
            super(startNode, startOffset, map, finalNode, finalOffset);
        }

        @Override
        public K next() {
            makeBoundedNext();
            return lastNode.keys[lastNode.right_idx - lastOffset];
        }
    }

    static class BoundedValueIterator <K, V> extends BoundedMapIterator<K, V>
                                                       implements TIterator<V> {

        public BoundedValueIterator(Node<K, V> startNode, int startOffset, TTreeMap<K, V> map,
                                    Node<K, V> finalNode, int finalOffset) {
            super(startNode, startOffset, map, finalNode, finalOffset);
        }

        @Override
        public V next() {
            makeBoundedNext();
            return lastNode.values[lastNode.right_idx - lastOffset];
        }
    }

    static final class SubMap <K,V> extends TAbstractMap<K, V>
                                 implements TSortedMap<K, V>, TSerializable {
        private TTreeMap<K, V> backingMap;
        boolean hasStart, hasEnd;
        K startKey, endKey;
        transient TSet<Entry<K, V>> entrySet = null;
        transient int firstKeyModCount = -1;
        transient int lastKeyModCount = -1;
        transient Node<K, V> firstKeyNode;
        transient int firstKeyIndex;
        transient Node<K, V> lastKeyNode;
        transient int lastKeyIndex;

        SubMap(K start, TTreeMap<K, V> map) {
            backingMap = map;
            hasStart = true;
            startKey = start;
        }

        SubMap(K start, TTreeMap<K, V> map, K end) {
            backingMap = map;
            hasStart = hasEnd = true;
            startKey = start;
            endKey = end;
        }

        SubMap(TTreeMap<K, V> map, K end) {
            backingMap = map;
            hasEnd = true;
            endKey = end;
        }

        private void checkRange(K key) {
            TComparator<? super K> cmp = backingMap.comparator;
            if (cmp == null) {
                TComparable<K> object = toComparable(key);
                if (hasStart && object.compareTo(startKey) < 0) {
                    throw new TIllegalArgumentException();
                }
                if (hasEnd && object.compareTo(endKey) > 0) {
                    throw new TIllegalArgumentException();
                }
            } else {
                if (hasStart
                    && backingMap.comparator().compare(key, startKey) < 0) {
                    throw new TIllegalArgumentException();
                }
                if (hasEnd && backingMap.comparator().compare(key, endKey) > 0) {
                    throw new TIllegalArgumentException();
                }
            }
        }

        private boolean isInRange(K key) {
            TComparator<? super K> cmp = backingMap.comparator;
            if (cmp == null) {
                TComparable<K> object = toComparable(key);
                if (hasStart && object.compareTo(startKey) < 0) {
                    return false;
                }
                if (hasEnd && object.compareTo(endKey) >= 0) {
                    return false;
                }
            } else {
                if (hasStart && cmp.compare(key, startKey) < 0) {
                    return false;
                }
                if (hasEnd && cmp.compare(key, endKey) >= 0) {
                    return false;
                }
            }
            return true;
        }

        private boolean checkUpperBound(K key) {
            if (hasEnd) {
                TComparator<? super K> cmp = backingMap.comparator;
                if (cmp == null) {
                    return (toComparable(key).compareTo(endKey) < 0);
                }
                return (cmp.compare(key, endKey) < 0);
            }
            return true;
        }

        private boolean checkLowerBound(K key) {
            if (hasStart) {
                TComparator<? super K> cmp = backingMap.comparator;
                if (cmp == null) {
                    return (toComparable(key).compareTo(startKey) >= 0);
                }
                return (cmp.compare(key, startKey) >= 0);
            }
            return true;
        }

        @Override
        public TComparator<? super K> comparator() {
            return backingMap.comparator();
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean containsKey(Object key) {
            if (isInRange((K) key)) {
                return backingMap.containsKey(key);
            }
            return false;
        }

        @Override
         public void clear() {
            keySet().clear();
        }

        @Override
         public boolean containsValue(Object value) {
            TIterator<V> it = values().iterator();
            if (value != null) {
                while (it.hasNext()) {
                    if (value.equals(it.next())) {
                        return true;
                    }
                }
            } else {
                while (it.hasNext()) {
                    if (it.next() == null) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public TSet<Entry<K, V>> entrySet() {
            if (entrySet == null) {
                entrySet = new SubMapEntrySet<>(this);
            }
            return entrySet;
        }

        private void setFirstKey() {
            if (firstKeyModCount == backingMap.modCount) {
                return;
            }
            TComparable<K> object = backingMap.comparator == null ? toComparable(startKey) : null;
            K key = startKey;
            Node<K, V> node = backingMap.root;
            Node<K, V> foundNode = null;
            int foundIndex = -1;
            TOP_LOOP:
            while (node != null) {
                K[] keys = node.keys;
                int left_idx = node.left_idx;
                int result = backingMap.cmp(object, key, keys[left_idx]);
                if (result < 0) {
                    foundNode = node;
                    foundIndex = left_idx;
                    node = node.left;
                } else if (result == 0) {
                    foundNode = node;
                    foundIndex = left_idx;
                    break;
                } else {
                    int right_idx = node.right_idx;
                    if (left_idx != right_idx) {
                        result = backingMap.cmp(object, key, keys[right_idx]);
                    }
                    if (result > 0) {
                        node = node.right;
                    } else if (result == 0) {
                        foundNode = node;
                        foundIndex = right_idx;
                        break;
                    } else { /*search in node*/
                        foundNode = node;
                        foundIndex = right_idx;
                        int low = left_idx + 1, mid = 0, high = right_idx - 1;
                        while (low <= high) {
                            mid = (low + high) >>> 1;
                            result = backingMap.cmp(object, key, keys[mid]);
                            if (result > 0) {
                                low = mid + 1;
                            } else if (result == 0) {
                                foundNode = node;
                                foundIndex = mid;
                                break TOP_LOOP;
                            } else {
                                foundNode = node;
                                foundIndex = mid;
                                high = mid - 1;
                            }
                        }
                        break TOP_LOOP;
                    }
                }
            }
            if (foundNode != null && !checkUpperBound(foundNode.keys[foundIndex])) {
                foundNode = null;
            }
            firstKeyNode = foundNode;
            firstKeyIndex = foundIndex;
            firstKeyModCount = backingMap.modCount;
        }

        @Override
        public K firstKey() {
            if (backingMap.size > 0) {
                if (!hasStart) {
                    Node<K, V> node = minimum(backingMap.root);
                    if (node != null && checkUpperBound(node.keys[node.left_idx])) {
                        return node.keys[node.left_idx];
                    }
                } else {
                    setFirstKey();
                    if (firstKeyNode != null) {
                        return firstKeyNode.keys[firstKeyIndex];
                    }
                }
            }
            throw new TNoSuchElementException();
        }


        @SuppressWarnings("unchecked")
        @Override
        public V get(Object key) {
            if (isInRange((K) key)) {
                return backingMap.get(key);
            }
            return null;
        }

        @Override
        public TSortedMap<K, V> headMap(K endKey) {
            checkRange(endKey);
            if (hasStart) {
                return new SubMap<>(startKey, backingMap, endKey);
            }
            return new SubMap<>(backingMap, endKey);
        }

        @Override
        public boolean isEmpty() {
            if (hasStart) {
                setFirstKey();
                return firstKeyNode == null;
            } else {
                setLastKey();
                return lastKeyNode == null;
            }
        }

        @Override
        public TSet<K> keySet() {
            if (cachedKeySet == null) {
                cachedKeySet = new SubMapKeySet<>(this);
            }
            return cachedKeySet;
        }

        private void setLastKey() {
            if (lastKeyModCount == backingMap.modCount) {
                return;
            }
            TComparable<K> object = backingMap.comparator == null ? toComparable(endKey) : null;
            K key = endKey;
            Node<K, V> node = backingMap.root;
            Node<K, V> foundNode = null;
            int foundIndex = -1;
            TOP_LOOP:
            while (node != null) {
                K[] keys = node.keys;
                int left_idx = node.left_idx;
                int result = backingMap.cmp(object, key, keys[left_idx]);
                if (result <= 0) {
                    node = node.left;
                } else {
                    int right_idx = node.right_idx;
                    if (left_idx != right_idx) {
                        result = backingMap.cmp(object, key, keys[right_idx]);
                    }
                    if (result > 0) {
                        foundNode = node;
                        foundIndex = right_idx;
                        node = node.right;
                    } else if (result == 0) {
                        if (node.left_idx == node.right_idx) {
                            foundNode = node.prev;
                            if (foundNode != null) {
                                foundIndex = foundNode.right_idx - 1;
                            }
                        } else {
                            foundNode = node;
                            foundIndex = right_idx - 1;
                        }
                        break;
                    } else { /*search in node*/
                        foundNode = node;
                        foundIndex = left_idx;
                        int low = left_idx + 1, mid = 0, high = right_idx - 1;
                        while (low <= high) {
                            mid = (low + high) >>> 1;
                            result = backingMap.cmp(object, key, keys[mid]);
                            if (result > 0) {
                                foundNode = node;
                                foundIndex = mid;
                                low = mid + 1;
                            } else if (result == 0) {
                                foundNode = node;
                                foundIndex = mid - 1;
                                break TOP_LOOP;
                            } else {
                                high = mid - 1;
                            }
                        }
                        break TOP_LOOP;
                    }
                }
            }
            if (foundNode != null && !checkLowerBound(foundNode.keys[foundIndex])) {
                foundNode = null;
            }
            lastKeyNode = foundNode;
            lastKeyIndex = foundIndex;
            lastKeyModCount = backingMap.modCount;
        }

        @Override
        public K lastKey() {
            if (backingMap.size > 0) {
                if (!hasEnd) {
                    Node<K, V> node = maximum(backingMap.root);
                    if (node != null && checkLowerBound(node.keys[node.right_idx])) {
                        return node.keys[node.right_idx];
                    }
                } else {
                    setLastKey();
                    if (lastKeyNode != null) {
                        return lastKeyNode.keys[lastKeyIndex];
                    }
                }
            }
            throw new TNoSuchElementException();
        }


        @Override
        public V put(K key, V value) {
            if (isInRange(key)) {
                return backingMap.put(key, value);
            }
            throw new TIllegalArgumentException();
        }

        @SuppressWarnings("unchecked")
        @Override
        public V remove(Object key) {
            if (isInRange((K) key)) {
                return backingMap.remove(key);
            }
            return null;
        }

        @Override
        public TSortedMap<K, V> subMap(K startKey, K endKey) {
            checkRange(startKey);
            checkRange(endKey);
            TComparator<? super K> c = backingMap.comparator();
            if (c == null) {
                if (toComparable(startKey).compareTo(endKey) <= 0) {
                    return new SubMap<>(startKey, backingMap, endKey);
                }
            } else {
                if (c.compare(startKey, endKey) <= 0) {
                    return new SubMap<>(startKey, backingMap, endKey);
                }
            }
            throw new IllegalArgumentException();
        }

        @Override
        public TSortedMap<K, V> tailMap(K startKey) {
            checkRange(startKey);
            if (hasEnd) {
                return new SubMap<>(startKey, backingMap, endKey);
            }
            return new SubMap<>(startKey, backingMap);
        }

        @Override
        public TCollection<V> values() {
            if (cachedValues == null) {
                cachedValues = new SubMapValuesCollection<>(this);
            }
            return cachedValues;
        }

        @Override
        public int size() {
            Node<K, V> from, to;
            int fromIndex, toIndex;
            if (hasStart) {
                setFirstKey();
                from = firstKeyNode;
                fromIndex = firstKeyIndex;
            } else {
                from = minimum(backingMap.root);
                fromIndex = from == null ? 0 : from.left_idx;
            }
            if (from == null) {
                return 0;
            }
            if (hasEnd) {
                setLastKey();
                to = lastKeyNode;
                toIndex = lastKeyIndex;
            } else {
                to = maximum(backingMap.root);
                toIndex = to == null ? 0 : to.right_idx;
            }
            if (to == null) {
                return 0;
            }
            if (from == to) {
                return toIndex - fromIndex + 1;
            }
            int sum = 0;
            while (from != to) {
                sum += (from.right_idx - fromIndex + 1);
                from = from.next;
                fromIndex = from.left_idx;
            }
            return sum + toIndex - fromIndex + 1;
        }

    }

    static class SubMapEntrySet<K,V> extends TAbstractSet<Entry<K, V>> {
        SubMap<K, V> subMap;

        SubMapEntrySet(SubMap<K, V> map) {
            subMap = map;
        }

        @Override
        public boolean isEmpty() {
            return subMap.isEmpty();
        }

        @Override
        public TIterator<Entry<K, V>> iterator() {
            Node<K, V> from;
            int fromIndex;
            if (subMap.hasStart) {
                subMap.setFirstKey();
                from = subMap.firstKeyNode;
                fromIndex = subMap.firstKeyIndex;
            } else {
                from = minimum(subMap.backingMap.root);
                fromIndex = from != null ? from.left_idx : 0;
            }
            if (!subMap.hasEnd) {
                return new UnboundedEntryIterator<>(subMap.backingMap, from,
                        from == null ? 0 : from.right_idx - fromIndex);
            }
            subMap.setLastKey();
            Node<K, V> to = subMap.lastKeyNode;
            int toIndex = subMap.lastKeyIndex;
            return new BoundedEntryIterator<>(from, from == null ? 0 : from.right_idx - fromIndex,
                    subMap.backingMap, to, to == null ? 0 : to.right_idx - toIndex);
        }

        @Override
        public int size() {
            return subMap.size();
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean contains(Object object) {
            if (object instanceof Entry) {
                Entry<K, V> entry = (Entry<K, V>) object;
                K key = entry.getKey();
                if (subMap.isInRange(key)) {
                    V v1 = subMap.get(key), v2 = entry.getValue();
                    return v1 == null ? ( v2 == null && subMap.containsKey(key) ) : v1.equals(v2);
                }
            }
            return false;
        }

        @Override
        public boolean remove(Object object) {
            if (contains(object)) {
                @SuppressWarnings("unchecked")
                Entry<K, V> entry = (Entry<K, V>) object;
                K key = entry.getKey();
                subMap.remove(key);
                return true;
            }
            return false;
        }
    }

    static class SubMapKeySet <K,V> extends TAbstractSet<K> {
        SubMap<K, V> subMap;

        SubMapKeySet(SubMap<K, V> map) {
            subMap = map;
        }

        @Override
        public boolean contains(Object object) {
            return subMap.containsKey(object);
        }

        @Override
        public boolean isEmpty() {
            return subMap.isEmpty();
        }

        @Override
        public int size() {
            return subMap.size();
        }

        @Override
        public boolean remove(Object object) {
            if (subMap.containsKey(object)) {
                subMap.remove(object);
                return true;
            }
            return false;
        }

        @Override
        public TIterator<K> iterator() {
            Node<K, V> from;
            int fromIndex;
            if (subMap.hasStart) {
                subMap.setFirstKey();
                from = subMap.firstKeyNode;
                fromIndex = subMap.firstKeyIndex;
            } else {
                from = minimum(subMap.backingMap.root);
                fromIndex = from != null ? from.left_idx : 0;
            }
            if (!subMap.hasEnd) {
                return new UnboundedKeyIterator<>(subMap.backingMap, from,
                                   from == null ? 0 : from.right_idx - fromIndex);
            }
            subMap.setLastKey();
            Node<K, V> to = subMap.lastKeyNode;
            int toIndex = subMap.lastKeyIndex;
            return new BoundedKeyIterator<>(from,
               from == null ? 0 : from.right_idx - fromIndex, subMap.backingMap, to,
                 to == null ? 0 : to.right_idx   - toIndex);
        }
    }

    static class SubMapValuesCollection <K,V> extends TAbstractCollection<V> {
        SubMap<K, V> subMap;

        public SubMapValuesCollection(SubMap<K, V> subMap) {
            this.subMap = subMap;
        }

        @Override
        public boolean isEmpty() {
            return subMap.isEmpty();
        }

        @Override
        public TIterator<V> iterator() {
            Node<K, V> from;
            int fromIndex;
            if (subMap.hasStart) {
                subMap.setFirstKey();
                from = subMap.firstKeyNode;
                fromIndex = subMap.firstKeyIndex;
            } else {
                from = minimum(subMap.backingMap.root);
                fromIndex = from != null ? from.left_idx : 0;
            }
            if (!subMap.hasEnd) {
                return new UnboundedValueIterator<>(subMap.backingMap, from,
                        from == null ? 0 : from.right_idx - fromIndex);
            }
            subMap.setLastKey();
            Node<K, V> to = subMap.lastKeyNode;
            int toIndex = subMap.lastKeyIndex;
            return new BoundedValueIterator<>(from,
               from == null ? 0 : from.right_idx - fromIndex, subMap.backingMap, to,
                 to == null ? 0 : to.right_idx - toIndex);
        }

        @Override
        public int size() {
            return subMap.size();
        }
    }

    public TTreeMap() {
    }

    public TTreeMap(TComparator<? super K> comparator) {
        this.comparator = comparator;
    }

    public TTreeMap(TMap<? extends K, ? extends V> map) {
        putAll(map);
    }

    public TTreeMap(TSortedMap<K, ? extends V> map) {
        this(map.comparator());
        Node<K, V> lastNode = null;
        TIterator<? extends Entry<K, ? extends V>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Entry<K, ? extends V> entry = it.next();
            lastNode = addToLast(lastNode, entry.getKey(), entry.getValue());
        }
    }

    Node<K, V> addToLast(Node<K, V> last, K key, V value) {
        if (last == null) {
            root = last = createNode(key, value);
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
    public TObject clone() {
        try {
            TTreeMap<K, V> clone = (TTreeMap<K, V>) super.clone();
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
        } catch (TCloneNotSupportedException e) {
            return null;
        }
    }

    static private <K, V> Node<K, V> successor(Node<K, V> x) {
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

    @Override
    public TComparator<? super K> comparator() {
        return comparator;
    }

    @Override
    public boolean containsKey(Object key) {
        @SuppressWarnings("unchecked")
        TComparable<K> object = comparator == null ? toComparable((K) key) : null;
        @SuppressWarnings("unchecked")
        K keyK = (K)key;
        Node<K, V> node = root;
        while (node != null) {
            K[] keys = node.keys;
            int left_idx = node.left_idx;
            int result = cmp(object, keyK, keys[left_idx]);
            if (result < 0) {
                node = node.left;
            } else if (result == 0) {
                return true;
            } else {
                int right_idx = node.right_idx;
                if (left_idx != right_idx) {
                    result = cmp(object, keyK, keys[right_idx]);
                }
                if (result > 0) {
                    node = node.right;
                } else if (result == 0) {
                    return true;
                } else { /*search in node*/
                    int low = left_idx + 1, mid = 0, high = right_idx - 1;
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
                int to = node.right_idx;
                V[] values = node.values;
                for (int i = node.left_idx; i <= to; i++) {
                    if (value.equals(values[i])) {
                        return true;
                    }
                }
                node = node.next;
            }
        } else {
            while (node != null) {
                int to = node.right_idx;
                V[] values = node.values;
                for (int i = node.left_idx; i <= to; i++) {
                    if (values[i] == null) {
                        return true;
                    }
                }
                node = node.next;
            }
        }
        return false;
    }

    @Override
    public TSet<Entry<K, V>> entrySet() {
        if (entrySet == null) {
            entrySet = new TAbstractSet<Entry<K, V>>() {
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
                    if (object instanceof Entry) {
                        Entry<K, V> entry = (Entry<K, V>) object;
                        K key = entry.getKey();
                        Object v1 = TTreeMap.this.get(key), v2 = entry.getValue();
                        return v1 == null ? ( v2 == null && TTreeMap.this.containsKey(key) ) : v1.equals(v2);
                    }
                    return false;
                }

                @Override
                public boolean remove(Object object) {
                    if (contains(object)) {
                        @SuppressWarnings("unchecked")
                        Entry<K, V> entry = (Entry<K, V>) object;
                        K key = entry.getKey();
                        TTreeMap.this.remove(key);
                        return true;
                    }
                    return false;
                }

                @Override
                public TIterator<Entry<K, V>> iterator() {
                    return new UnboundedEntryIterator<>(TTreeMap.this);
                }
            };
        }
        return entrySet;
    }

    @Override
    public K firstKey() {
        if (root != null) {
            Node<K, V> node = minimum(root);
            return node.keys[node.left_idx];
        }
        throw new TNoSuchElementException();
    }


    @Override
    public V get(Object key) {
        @SuppressWarnings("unchecked")
        TComparable<K> object = comparator == null ? toComparable((K) key) : null;
        @SuppressWarnings("unchecked")
        K keyK = (K) key;
        Node<K, V> node = root;
        while (node != null) {
            K[] keys = node.keys;
            int left_idx = node.left_idx;
            int result = cmp(object, keyK, keys[left_idx]);
            if (result < 0) {
                node = node.left;
            } else if (result == 0) {
                return node.values[left_idx];
            } else {
                int right_idx = node.right_idx;
                if (left_idx != right_idx) {
                    result = cmp(object, keyK, keys[right_idx]);
                }
                if (result > 0) {
                    node = node.right;
                } else if (result == 0) {
                    return node.values[right_idx];
                } else { /*search in node*/
                    int low = left_idx + 1, mid = 0, high = right_idx - 1;
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

    private int cmp(TComparable<K> object, K key1, K key2) {
        return object != null ? object.compareTo(key2) : comparator.compare(key1, key2);
    }

    @Override
    public TSortedMap<K, V> headMap(K endKey) {
        // Check for errors
        if (comparator == null) {
            toComparable(endKey).compareTo(endKey);
        } else {
            comparator.compare(endKey, endKey);
        }
        return new SubMap<>(this, endKey);
    }

    @Override
    public TSet<K> keySet() {
        if (cachedKeySet == null) {
            cachedKeySet = new TAbstractSet<K>() {
                @Override
                public boolean contains(Object object) {
                    return TTreeMap.this.containsKey(object);
                }

                @Override
                public int size() {
                    return TTreeMap.this.size;
                }

                @Override
                public void clear() {
                    TTreeMap.this.clear();
                }

                @Override
                public boolean remove(Object object) {
                    if (contains(object)) {
                        TTreeMap.this.remove(object);
                        return true;
                    }
                    return false;
                }

                @Override
                public TIterator<K> iterator() {
                    return new UnboundedKeyIterator<>(TTreeMap.this);
                }
            };
        }
        return cachedKeySet;
    }

    @Override
    public K lastKey() {
        if (root != null) {
            Node<K, V> node = maximum(root);
            return node.keys[node.right_idx];
        }
        throw new TNoSuchElementException();
    }

    static <K,V> Node<K, V> minimum(Node<K, V> x) {
        if (x == null) {
            return null;
        }
        while (x.left != null) {
            x = x.left;
        }
        return x;
    }

    static <K,V> Node<K, V> maximum(Node<K, V> x) {
        if (x == null) {
            return null;
        }
        while (x.right != null) {
            x = x.right;
        }
        return x;
    }

    @Override
    public V put(K key, V value) {
        if (root == null) {
            root = createNode(key, value);
            size = 1;
            modCount++;
            return null;
        }
        TComparable<K> object = comparator == null ? toComparable(key) : null;
        K keyK = key;
        Node<K, V> node = root;
        Node<K, V> prevNode = null;
        int result = 0;
        while (node != null) {
            prevNode = node;
            K[] keys = node.keys;
            int left_idx = node.left_idx;
            result = cmp(object, keyK, keys[left_idx]);
            if (result < 0) {
                node = node.left;
            } else if (result == 0) {
                V res = node.values[left_idx];
                node.values[left_idx] = value;
                return res;
            } else {
                int right_idx = node.right_idx;
                if (left_idx != right_idx) {
                    result = cmp(object, keyK, keys[right_idx]);
                }
                if (result > 0) {
                    node = node.right;
                } else if (result == 0) {
                    V res = node.values[right_idx];
                    node.values[right_idx] = value;
                    return res;
                } else { /*search in node*/
                    int low = left_idx + 1, mid = 0, high = right_idx - 1;
                    while (low <= high) {
                        mid = (low + high) >>> 1;
                        result = cmp(object, keyK, keys[mid]);
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
          if(node == null) {
             if(prevNode==null) {
                - case of empty Tree
             } else {
                result < 0 - prevNode.left==null - attach here
                result > 0 - prevNode.right==null - attach here
             }
          } else {
             insert into node.
             result - index where it should be inserted.
          }
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
                int left_idx = node.left_idx;
                int right_idx = node.right_idx;
                if (left_idx == 0 || ((right_idx != Node.NODE_SIZE - 1) && (right_idx - result <= result - left_idx))) {
                    int right_idxPlus1 = right_idx + 1;
                    System.arraycopy(node.keys,   result, node.keys,   result + 1, right_idxPlus1 - result);
                    System.arraycopy(node.values, result, node.values, result + 1, right_idxPlus1 - result);
                    node.right_idx = right_idxPlus1;
                    node.keys[result] = key;
                    node.values[result] = value;
                } else {
                    int left_idxMinus1 = left_idx - 1;
                    System.arraycopy(node.keys,   left_idx, node.keys,   left_idxMinus1, result - left_idx);
                    System.arraycopy(node.values, left_idx, node.values, left_idxMinus1, result - left_idx);
                    node.left_idx = left_idxMinus1;
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
                        if (nextNode.size < Node.NODE_SIZE) {
                            // move last pair to next
                            removeFromStart = false;
                        } else {
                            // prev & next are full
                            // if node.right!=null then node.next.left==null
                            // if node.left!=null then node.prev.right==null
                            if (node.right == null) {
                                attachHere = node;
                                attachFromLeft = false;
                                removeFromStart = false;
                            } else {
                                attachHere = nextNode;
                                attachFromLeft = true;
                                removeFromStart = false;
                            }
                        }
                    }
                }
                K movedKey;
                V movedValue;
                if (removeFromStart) {
                    // node.left_idx == 0
                    movedKey = node.keys[0];
                    movedValue = node.values[0];
                    int resMunus1 = result - 1;
                    System.arraycopy(node.keys,   1, node.keys,   0, resMunus1);
                    System.arraycopy(node.values, 1, node.values, 0, resMunus1);
                    node.keys  [resMunus1] = key;
                    node.values[resMunus1] = value;
                } else {
                    // node.right_idx == Node.NODE_SIZE - 1
                    movedKey   = node.keys[Node.NODE_SIZE - 1];
                    movedValue = node.values[Node.NODE_SIZE - 1];
                    System.arraycopy(node.keys,   result, node.keys,   result + 1, Node.NODE_SIZE - 1 - result);
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
        if (node.left_idx == 0) {
            int new_right = node.right_idx + 1;
            System.arraycopy(node.keys,   0, node.keys,   1, new_right);
            System.arraycopy(node.values, 0, node.values, 1, new_right);
            node.right_idx = new_right;
        } else {
            node.left_idx--;
        }
        node.size++;
        node.keys[node.left_idx] = keyObj;
        node.values[node.left_idx] = value;
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

    /* add pair into node; existence free room in the node should be checked
     * before call
     */
    private void appendFromRight(Node<K, V> node, K keyObj, V value) {
        if (node.right_idx == Node.NODE_SIZE - 1) {
            int left_idx = node.left_idx;
            int left_idxMinus1 = left_idx - 1;
            System.arraycopy(node.keys,   left_idx, node.keys,   left_idxMinus1, Node.NODE_SIZE - left_idx);
            System.arraycopy(node.values, left_idx, node.values, left_idxMinus1, Node.NODE_SIZE - left_idx);
            node.left_idx = left_idxMinus1;
        } else {
            node.right_idx++;
        }
        node.size++;
        node.keys[node.right_idx] = keyObj;
        node.values[node.right_idx] = value;
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
        node.left_idx = 0;
        node.right_idx = 0;
        node.size = 1;
        return node;
    }

    void balance(Node<K, V> x) {
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
        super.putAll(map);
    }

    @Override
    public V remove(Object key) {
        if (size == 0) {
            return null;
        }
        @SuppressWarnings("unchecked")
        TComparable<K> object = comparator == null ? toComparable((K) key) : null;
        @SuppressWarnings("unchecked")
        K keyK = (K) key;
        Node<K, V> node = root;
        while (node != null) {
            K[] keys = node.keys;
            int left_idx = node.left_idx;
            int result = cmp(object, keyK, keys[left_idx]);
            if (result < 0) {
                node = node.left;
            } else if (result == 0) {
                V value = node.values[left_idx];
                removeLeftmost(node);
                return value;
            } else {
                int right_idx = node.right_idx;
                if (left_idx != right_idx) {
                    result = cmp(object, keyK, keys[right_idx]);
                }
                if (result > 0) {
                    node = node.right;
                } else if (result == 0) {
                    V value = node.values[right_idx];
                    removeRightmost(node);
                    return value;
                } else { /*search in node*/
                    int low = left_idx + 1, mid = 0, high = right_idx - 1;
                    while (low <= high) {
                        mid = (low + high) >>> 1;
                        result = cmp(object, keyK, keys[mid]);
                        if (result > 0) {
                            low = mid + 1;
                        } else if (result == 0) {
                            V value = node.values[mid];
                            removeMiddleElement(node, mid);
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

    void removeLeftmost(Node<K, V> node) {
        int index = node.left_idx;
        if (node.size == 1) {
            deleteNode(node);
        } else if (node.prev != null && (Node.NODE_SIZE - 1 - node.prev.right_idx) > node.size) {
            // move all to prev node and kill it
            Node<K, V> prev = node.prev;
            int size = node.right_idx - index;
            System.arraycopy(node.keys,   index + 1, prev.keys,   prev.right_idx + 1, size);
            System.arraycopy(node.values, index + 1, prev.values, prev.right_idx + 1, size);
            prev.right_idx += size;
            prev.size += size;
            deleteNode(node);
        } else if (node.next != null && (node.next.left_idx) > node.size) {
            // move all to next node and kill it
            Node<K, V> next = node.next;
            int size = node.right_idx - index;
            int next_new_left = next.left_idx - size;
            next.left_idx = next_new_left;
            System.arraycopy(node.keys,   index + 1, next.keys,   next_new_left, size);
            System.arraycopy(node.values, index + 1, next.values, next_new_left, size);
            next.size += size;
            deleteNode(node);
        } else {
            node.keys[index] = null;
            node.values[index] = null;
            node.left_idx++;
            node.size--;
            Node<K, V> prev = node.prev;
            if (prev != null && prev.size == 1) {
                node.size++;
                node.left_idx--;
                node.keys  [node.left_idx] = prev.keys  [prev.left_idx];
                node.values[node.left_idx] = prev.values[prev.left_idx];
                deleteNode(prev);
            }
        }
        modCount++;
        size--;
    }

    void removeRightmost(Node<K, V> node) {
        int index = node.right_idx;
        if (node.size == 1) {
            deleteNode(node);
        } else if (node.prev != null && (Node.NODE_SIZE - 1 - node.prev.right_idx) > node.size) {
            // move all to prev node and kill it
            Node<K, V> prev = node.prev;
            int left_idx = node.left_idx;
            int size = index - left_idx;
            System.arraycopy(node.keys,   left_idx, prev.keys,   prev.right_idx + 1, size);
            System.arraycopy(node.values, left_idx, prev.values, prev.right_idx + 1, size);
            prev.right_idx += size;
            prev.size += size;
            deleteNode(node);
        } else if (node.next != null && (node.next.left_idx) > node.size) {
            // move all to next node and kill it
            Node<K, V> next = node.next;
            int left_idx = node.left_idx;
            int size = index - left_idx;
            int next_new_left = next.left_idx - size;
            next.left_idx = next_new_left;
            System.arraycopy(node.keys,   left_idx, next.keys,   next_new_left, size);
            System.arraycopy(node.values, left_idx, next.values, next_new_left, size);
            next.size += size;
            deleteNode(node);
        } else {
            node.keys[index] = null;
            node.values[index] = null;
            node.right_idx--;
            node.size--;
            Node<K, V> next = node.next;
            if (next != null && next.size == 1) {
                node.size++;
                node.right_idx++;
                node.keys[node.right_idx]   = next.keys[next.left_idx];
                node.values[node.right_idx] = next.values[next.left_idx];
                deleteNode(next);
            }
        }
        modCount++;
        size--;
    }

    void removeMiddleElement(Node<K, V> node, int index) {
        // this function is called iff index if some middle element;
        // so node.left_idx < index < node.right_idx
        // condition above assume that node.size > 1
        if (node.prev != null && (Node.NODE_SIZE - 1 - node.prev.right_idx) > node.size) {
            // move all to prev node and kill it
            Node<K, V> prev = node.prev;
            int left_idx = node.left_idx;
            int size = index - left_idx;
            System.arraycopy(node.keys,   left_idx, prev.keys,   prev.right_idx + 1, size);
            System.arraycopy(node.values, left_idx, prev.values, prev.right_idx + 1, size);
            prev.right_idx += size;
            size = node.right_idx - index;
            System.arraycopy(node.keys,   index + 1, prev.keys,   prev.right_idx + 1, size);
            System.arraycopy(node.values, index + 1, prev.values, prev.right_idx + 1, size);
            prev.right_idx += size;
            prev.size += (node.size - 1);
            deleteNode(node);
        } else if (node.next != null && (node.next.left_idx) > node.size) {
            // move all to next node and kill it
            Node<K, V> next = node.next;
            int left_idx = node.left_idx;
            int next_new_left = next.left_idx - node.size + 1;
            next.left_idx = next_new_left;
            int size = index - left_idx;
            System.arraycopy(node.keys,   left_idx, next.keys,   next_new_left, size);
            System.arraycopy(node.values, left_idx, next.values, next_new_left, size);
            next_new_left += size;
            size = node.right_idx - index;
            System.arraycopy(node.keys,   index + 1, next.keys,   next_new_left, size);
            System.arraycopy(node.values, index + 1, next.values, next_new_left, size);
            next.size += (node.size - 1);
            deleteNode(node);
        } else {
            int moveFromRight = node.right_idx - index;
            int left_idx = node.left_idx;
            int moveFromLeft = index - left_idx ;
            if (moveFromRight <= moveFromLeft) {
                System.arraycopy(node.keys,   index + 1, node.keys,   index, moveFromRight);
                System.arraycopy(node.values, index + 1, node.values, index, moveFromRight);
                Node<K, V> next = node.next;
                if (next != null && next.size == 1) {
                    node.keys  [node.right_idx] = next.keys  [next.left_idx];
                    node.values[node.right_idx] = next.values[next.left_idx];
                    deleteNode(next);
                } else {
                    node.keys  [node.right_idx] = null;
                    node.values[node.right_idx] = null;
                    node.right_idx--;
                    node.size--;
                }
            } else {
                System.arraycopy(node.keys,   left_idx , node.keys,   left_idx  + 1, moveFromLeft);
                System.arraycopy(node.values, left_idx , node.values, left_idx + 1, moveFromLeft);
                Node<K, V> prev = node.prev;
                if (prev != null && prev.size == 1) {
                    node.keys  [left_idx ] = prev.keys  [prev.left_idx];
                    node.values[left_idx ] = prev.values[prev.left_idx];
                    deleteNode(prev);
                } else {
                    node.keys  [left_idx ] = null;
                    node.values[left_idx ] = null;
                    node.left_idx++;
                    node.size--;
                }
            }
        }
        modCount++;
        size--;
    }

    void removeFromIterator(Node<K, V> node, int index) {
        if (node.size == 1) {
            // it is safe to delete the whole node here.
            // iterator already moved to the next node;
            deleteNode(node);
        } else {
            int left_idx = node.left_idx;
            if (index == left_idx) {
                Node<K, V> prev = node.prev;
                if (prev != null && prev.size == 1) {
                    node.keys  [left_idx] = prev.keys  [prev.left_idx];
                    node.values[left_idx] = prev.values[prev.left_idx];
                    deleteNode(prev);
                } else {
                    node.keys  [left_idx] = null;
                    node.values[left_idx] = null;
                    node.left_idx++;
                    node.size--;
                }
            } else if (index == node.right_idx) {
                node.keys  [index] = null;
                node.values[index] = null;
                node.right_idx--;
                node.size--;
            } else {
                int moveFromRight = node.right_idx - index;
                int moveFromLeft = index - left_idx;
                if (moveFromRight <= moveFromLeft) {
                    System.arraycopy(node.keys,   index + 1, node.keys,   index, moveFromRight );
                    System.arraycopy(node.values, index + 1, node.values, index, moveFromRight );
                    node.keys  [node.right_idx] = null;
                    node.values[node.right_idx] = null;
                    node.right_idx--;
                    node.size--;
                } else {
                    System.arraycopy(node.keys,   left_idx, node.keys,   left_idx+ 1, moveFromLeft);
                    System.arraycopy(node.values, left_idx, node.values, left_idx+ 1, moveFromLeft);
                    node.keys  [left_idx] = null;
                    node.values[left_idx] = null;
                    node.left_idx++;
                    node.size--;
               }
            }
        }
        modCount++;
        size--;
    }

    private void deleteNode(Node<K, V> node) {
        if (node.right == null) {
            if (node.left != null) {
                attachToParent(node, node.left);
           } else {
                attachNullToParent(node);
            }
            fixNextChain(node);
        } else if(node.left == null) { // node.right != null
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
            if(toMoveUp.right==null){
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
            attachToParentNoFixup(node,toMoveUp);
            toMoveUp.color = node.color;
        }
    }

    private void attachToParentNoFixup(Node<K, V> toDelete, Node<K, V> toConnect) {
        // assert toConnect!=null
        Node<K,V> parent = toDelete.parent;
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
        attachToParentNoFixup(toDelete,toConnect);
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
                if ((w.left == null || !w.left.color)
                    && (w.right == null || !w.right.color)) {
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
                if ((w.left == null || !w.left.color)
                    && (w.right == null || !w.right.color)) {
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
    public TSortedMap<K, V> subMap(K startKey, K endKey) {
        if (comparator == null) {
            if (toComparable(startKey).compareTo(endKey) <= 0) {
                return new SubMap<>(startKey, this, endKey);
            }
        } else {
            if (comparator.compare(startKey, endKey) <= 0) {
                return new SubMap<>(startKey, this, endKey);
            }
        }
        throw new TIllegalArgumentException();
    }

    @Override
    public TSortedMap<K, V> tailMap(K startKey) {
        // Check for errors
        if (comparator == null) {
            toComparable(startKey).compareTo(startKey);
        } else {
            comparator.compare(startKey, startKey);
        }
        return new SubMap<>(startKey, this);
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
}
