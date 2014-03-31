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

public class TTreeMap<K, V> extends TAbstractMap<K, V> implements TCloneable, TSerializable, TSortedMap<K, V> {
    class TreeNode implements Entry<K, V> {
        K key;
        V value;
        TreeNode left;
        TreeNode right;
        int height = 1;
        int size = 1;

        public TreeNode(K key) {
            this.key = key;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            V old = this.value;
            this.value = value;
            return old;
        }

        public TreeNode balance() {
            int factor = factor();
            if (factor == 2) {
                if (right.factor() < 0) {
                    right = right.rotateRight();
                }
                return rotateLeft();
            } else if (factor == -2) {
                if (left.factor() > 0) {
                    left = left.rotateLeft();
                }
                return rotateRight();
            } else {
                return this;
            }
        }

        public int factor() {
            return (right != null ? right.height : 0) - (left != null ? left.height : 0);
        }

        public TreeNode rotateRight() {
            TreeNode left = this.left;
            this.left = left.right;
            left.right = this;
            fix();
            left.fix();
            return left;
        }

        public TreeNode rotateLeft() {
            TreeNode right = this.right;
            this.right = right.left;
            right.left = this;
            fix();
            right.fix();
            return right;
        }

        public void fix() {
            height = Math.max(right != null ? right.height : 0, left != null ? left.height : 0) + 1;
            size = 1;
            if (left != null) {
                size += left.size;
            }
            if (right != null) {
                size += right.size;
            }
        }
    }

    TreeNode root;
    private TComparator<? super K> comparator;
    private TComparator<? super K> originalComparator;
    private int modCount = 0;

    public TTreeMap(TComparator<? super K> comparator) {
        this.originalComparator = comparator;
        if (comparator == null) {
            comparator = new TComparator<Object>() {
                @SuppressWarnings("unchecked") @Override public int compare(Object o1, Object o2) {
                    return o1 != null ? ((TComparable<Object>)o1).compareTo(o2) :
                            ((TComparable<Object>)o2).compareTo(o1);
                }
            };
        }
        this.comparator = comparator;
    }

    @Override
    public V get(Object key) {
        TreeNode node = findNode(key);
        return node != null ? node.value : null;
    }

    @Override
    public V put(K key, V value) {
        root = getOrCreateNode(root, key);
        TreeNode node = findNode(key);
        V old = node.value;
        node.value = value;
        modCount++;
        return old;
    }

    @Override
    public V remove(Object key) {
        TreeNode node = findNode(key);
        if (node == null) {
            return null;
        }
        root = deleteNode(root, key);
        modCount++;
        return node.value;
    }

    @Override
    public void clear() {
        root = null;
        modCount++;
    }

    @Override
    public boolean isEmpty() {
        return root == null;
    }

    @Override
    public boolean containsKey(Object key) {
        return findNode(key) != null;
    }

    TreeNode findNode(Object key) {
        TreeNode node = root;
        while (node != null) {
            @SuppressWarnings("unchecked")
            int cmp = comparator.compare((K)key, node.key);
            if (cmp == 0) {
                return node;
            } else if (cmp < 0) {
                node = node.left;
            } else {
                node = node.right;
            }
        }
        return null;
    }

    private TreeNode getOrCreateNode(TreeNode root, K key) {
        if (root == null) {
            return new TreeNode(key);
        }
        int cmp = comparator.compare(key, root.key);
        if (cmp == 0) {
            return root;
        } else if (cmp < 0) {
            root.left = getOrCreateNode(root.left, key);
        } else {
            root.right = getOrCreateNode(root.right, key);
        }
        return root.balance();
    }

    private TreeNode deleteNode(TreeNode root, Object key) {
        if (root == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        int cmp = comparator.compare((K)key, root.key);
        if (cmp < 0) {
            root.left = deleteNode(root.left, key);
        } else if (cmp > 0) {
            root.right = deleteNode(root.right, key);
        } else  if (root.right == null) {
            return root.left;
        } else {
            TreeNode left = root.left;
            TreeNode right = root.right;
            TreeNode min = right;
            Object[] pathToMin = new Object[right.height];
            int minDepth = 0;
            while (min.left != null) {
                pathToMin[minDepth++] = min;
                min = min.left;
            }
            right = min.right;
            while (minDepth >= 0) {
                @SuppressWarnings("unchecked")
                TreeNode node = (TreeNode)pathToMin[--minDepth];
                node.left = right;
                right = node;
                node.balance();
            }
            min.right = right;
            min.left = left;
            root = min;
        }
        return root.balance();
    }

    @Override
    public TSet<Entry<K, V>> entrySet() {
        return new EntrySet(new Object[0], null);
    }

    @Override
    public TComparator<? super K> comparator() {
        return originalComparator;
    }

    @Override
    public TSortedMap<K, V> subMap(K fromKey, K toKey) {
        return null;
    }

    @Override
    public TSortedMap<K, V> headMap(K toKey) {
        return null;
    }

    @Override
    public TSortedMap<K, V> tailMap(K fromKey) {
        return null;
    }

    @Override
    public K firstKey() {
        return firstEntry().key;
    }

    @Override
    public K lastKey() {
        return lastEntry().key;
    }

    private TreeNode firstEntry() {
        if (isEmpty()) {
            throw new TNoSuchElementException();
        }
        TreeNode node = root;
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

    private TreeNode lastEntry() {
        if (isEmpty()) {
            throw new TNoSuchElementException();
        }
        TreeNode node = root;
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

    @Override
    public int size() {
        return root != null ? root.size : 0;
    }

    private class EntrySet extends TAbstractSet<Entry<K, V>> {
        private Object[] path;
        private TreeNode from;
        private TreeNode to;

        @SuppressWarnings("unchecked")
        public EntrySet(Object[] path, TreeNode to) {
            this.path = path;
            this.from = this.path.length > 0 ? (TreeNode)this.path[this.path.length - 1] : null;
            this.to = to;
        }

        @Override
        public int size() {
            int size = TTreeMap.this.size();
            if (from != null && from.left != null) {
                size -= from.left.size;
            }
            if (to != null) {
                size -= 1;
                if (to.right != null) {
                    size -= to.right.size;
                }
            }
            return size;
        }

        @Override
        public TIterator<Entry<K, V>> iterator() {
            return null;
        }
    }

    private class EntryIterator implements TIterator<Entry<K, V>> {
        private int modCount = TTreeMap.this.modCount;
        private Object[] path = new Object[root != null ? root.height : 0];
        private TreeNode last;
        private TreeNode to;
        private int pathLength;
        public EntryIterator(Object[] path, TreeNode to) {
            TreeNode node = root;
            while (node != null) {
                path[pathLength++] = node;
                node = node.left;
            }
        }
        @Override public boolean hasNext() {
            return pathLength > 0;
        }
        @SuppressWarnings("unchecked") @Override public Entry<K, V> next() {
            if (modCount != TTreeMap.this.modCount) {
                throw new TConcurrentModificationException();
            }
            if (pathLength == 0) {
                throw new TNoSuchElementException();
            }
            TreeNode node = (TreeNode)path[--pathLength];
            last = node;
            if (node.right != null) {
                node = node.right;
                while (node != null) {
                    path[pathLength++] = node;
                    node = node.left;
                }
            }
            return last;
        }
        @Override public void remove() {
            if (last == null) {
                throw new TNoSuchElementException();
            }
            deleteNode(root, last);
            last = null;
        }
    }
}
