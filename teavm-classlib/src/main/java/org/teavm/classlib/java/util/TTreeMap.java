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

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.*;

public class TTreeMap<K, V> extends TAbstractMap<K, V> implements TCloneable, TSerializable, TSortedMap<K, V> {
    static class TreeNode<K, V> extends SimpleEntry<K, V> {
        TreeNode<K, V> left;
        TreeNode<K, V> right;
        int height = 1;
        int size = 1;

        public TreeNode(K key) {
            super(key, null);
        }

        public TreeNode<K, V> balance() {
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

        public TreeNode<K, V> rotateRight() {
            TreeNode<K, V> left = this.left;
            this.left = left.right;
            left.right = this;
            fix();
            left.fix();
            return left;
        }

        public TreeNode<K, V> rotateLeft() {
            TreeNode<K, V> right = this.right;
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

    TreeNode<K, V> root;
    private TComparator<? super K> comparator;
    private TComparator<? super K> originalComparator;
    private int modCount = 0;
    private EntrySet<K, V> cachedEntrySet;

    public TTreeMap() {
        this((TComparator<? super K>)null);
    }

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

    public TTreeMap(TMap<? extends K, ? extends V> m) {
        this((TComparator<? super K>)null);
        @SuppressWarnings("unchecked")
        Entry<K, V>[] entries = (Entry<K, V>[])new Entry<?, ?>[m.size()];
        entries = m.entrySet().toArray(entries);
        TArrays.sort(entries, new TComparator<Entry<K, V>>() {
            @Override public int compare(Entry<K, V> o1, Entry<K, V> o2) {
                return comparator.compare(o1.getKey(), o2.getKey());
            }
        });
        fillMap(entries);
    }

    public TTreeMap(TSortedMap<K, ? extends V> m) {
        this(m.comparator());
        @SuppressWarnings("unchecked")
        Entry<K, V>[] entries = (Entry<K, V>[])new Entry<?, ?>[m.size()];
        entries = m.entrySet().toArray(entries);
        fillMap(entries);
    }

    private void fillMap(Entry<? extends K, ? extends V>[] entries) {
        root = createNode(entries, 0, entries.length - 1);
    }

    private TreeNode<K, V> createNode(Entry<? extends K, ? extends V>[] entries, int l, int u) {
        if (l > u) {
            return null;
        }
        int mid = (l + u) / 2;
        Entry<? extends K, ? extends V> entry = entries[mid];
        TreeNode<K, V> node = new TreeNode<K, V>(entry.getKey());
        node.setValue(entry.getValue());
        node.left = createNode(entries, l, mid - 1);
        node.right = createNode(entries, mid + 1, u);
        node.fix();
        return node;
    }

    @Override
    public V get(Object key) {
        TreeNode<?, V> node = findExact(key);
        return node != null ? node.getValue() : null;
    }

    @Override
    public V put(K key, V value) {
        root = getOrCreateNode(root, key);
        TreeNode<?, V> node = findExact(key);
        V old = node.setValue(value);
        node.setValue(value);
        modCount++;
        return old;
    }

    @Override
    public V remove(Object key) {
        TreeNode<?, V> node = findExact(key);
        if (node == null) {
            return null;
        }
        root = deleteNode(root, key);
        modCount++;
        return node.getValue();
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
        return findExact(key) != null;
    }

    TreeNode<?, V> findExact(Object key) {
        TreeNode<K, V> node = root;
        while (node != null) {
            @SuppressWarnings("unchecked")
            int cmp = comparator.compare((K)key, node.getKey());
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

    TreeNode<K, V> findExactOrNext(Object key) {
        TreeNode<K, V> node = root;
        TreeNode<K, V> lastLeftTurn = null;
        while (node != null) {
            @SuppressWarnings("unchecked")
            int cmp = comparator.compare((K)key, node.getKey());
            if (cmp == 0) {
                return node;
            } else if (cmp < 0) {
                lastLeftTurn = node;
                node = node.left;
            } else {
                node = node.right;
            }
        }
        return lastLeftTurn;
    }

    TreeNode<K, V>[] pathToExactOrNext(Object key) {
        @SuppressWarnings("unchecked")
        TreeNode<K, V>[] path = (TreeNode<K, V>[])new TreeNode<?, ?>[height()];
        int depth = 0;
        TreeNode<K, V> node = root;
        while (node != null) {
            @SuppressWarnings("unchecked")
            int cmp = comparator.compare((K)key, node.getKey());
            if (cmp == 0) {
                path[depth++] = node;
                break;
            } else if (cmp < 0) {
                path[depth++] = node;
                node = node.left;
            } else {
                node = node.right;
            }
        }
        return TArrays.copyOf(path, depth);
    }

    TreeNode<K, V> findNext(Object key) {
        TreeNode<K, V> node = root;
        TreeNode<K, V> lastLeftTurn = null;
        while (node != null) {
            @SuppressWarnings("unchecked")
            int cmp = comparator.compare((K)key, node.getKey());
            if (cmp < 0) {
                lastLeftTurn = node;
                node = node.left;
            } else {
                node = node.right;
            }
        }
        return lastLeftTurn;
    }

    TreeNode<K, V>[] pathToNext(Object key) {
        @SuppressWarnings("unchecked")
        TreeNode<K, V>[] path = (TreeNode<K, V>[])new TreeNode<?, ?>[height()];
        int depth = 0;
        TreeNode<K, V> node = root;
        while (node != null) {
            @SuppressWarnings("unchecked")
            int cmp = comparator.compare((K)key, node.getKey());
            if (cmp < 0) {
                path[depth++] = node;
                node = node.left;
            } else {
                node = node.right;
            }
        }
        return TArrays.copyOf(path, depth);
    }

    TreeNode<K, V> findExactOrPrev(Object key) {
        TreeNode<K, V> node = root;
        TreeNode<K, V> lastRightTurn = null;
        while (node != null) {
            @SuppressWarnings("unchecked")
            int cmp = comparator.compare((K)key, node.getKey());
            if (cmp == 0) {
                return node;
            } else if (cmp > 0) {
                lastRightTurn = node;
                node = node.right;
            } else {
                node = node.left;
            }
        }
        return lastRightTurn;
    }

    TreeNode<K, V>[] pathToExactOrPrev(Object key) {
        @SuppressWarnings("unchecked")
        TreeNode<K, V>[] path = (TreeNode<K, V>[])new TreeNode<?, ?>[height()];
        int depth = 0;
        TreeNode<K, V> node = root;
        while (node != null) {
            @SuppressWarnings("unchecked")
            int cmp = comparator.compare((K)key, node.getKey());
            if (cmp == 0) {
                path[depth++] = node;
                break;
            } else if (cmp > 0) {
                path[depth++] = node;
                node = node.right;
            } else {
                node = node.left;
            }
        }
        return TArrays.copyOf(path, depth);
    }

    TreeNode<K, V>[] pathToFirst() {
        @SuppressWarnings("unchecked")
        TreeNode<K, V>[] path = (TreeNode<K, V>[])new TreeNode<?, ?>[height()];
        int depth = 0;
        TreeNode<K, V> node = root;
        while (node != null) {
            path[depth++] = node;
            node = node.left;
        }
        return TArrays.copyOf(path, depth);
    }

    TreeNode<K, V> findPrev(Object key) {
        TreeNode<K, V> node = root;
        TreeNode<K, V> lastRightTurn = null;
        while (node != null) {
            @SuppressWarnings("unchecked")
            int cmp = comparator.compare((K)key, node.getKey());
            if (cmp > 0) {
                lastRightTurn = node;
                node = node.right;
            } else {
                node = node.left;
            }
        }
        return lastRightTurn;
    }

    TreeNode<K, V>[] pathToPrev(Object key) {
        @SuppressWarnings("unchecked")
        TreeNode<K, V>[] path = (TreeNode<K, V>[])new TreeNode<?, ?>[height()];
        int depth = 0;
        TreeNode<K, V> node = root;
        while (node != null) {
            @SuppressWarnings("unchecked")
            int cmp = comparator.compare((K)key, node.getKey());
            if (cmp > 0) {
                path[depth++] = node;
                node = node.right;
            } else {
                node = node.left;
            }
        }
        return TArrays.copyOf(path, depth);
    }

    private TreeNode<K, V> getOrCreateNode(TreeNode<K, V> root, K key) {
        if (root == null) {
            return new TreeNode<>(key);
        }
        int cmp = comparator.compare(key, root.getKey());
        if (cmp == 0) {
            return root;
        } else if (cmp < 0) {
            root.left = getOrCreateNode(root.left, key);
        } else {
            root.right = getOrCreateNode(root.right, key);
        }
        root.fix();
        return root.balance();
    }

    private TreeNode<K, V> deleteNode(TreeNode<K, V> root, Object key) {
        if (root == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        int cmp = comparator.compare((K)key, root.getKey());
        if (cmp < 0) {
            root.left = deleteNode(root.left, key);
        } else if (cmp > 0) {
            root.right = deleteNode(root.right, key);
        } else  if (root.right == null) {
            return root.left;
        } else {
            TreeNode<K, V> left = root.left;
            TreeNode<K, V> right = root.right;
            TreeNode<K, V> min = right;
            @SuppressWarnings("unchecked")
            TreeNode<K, V>[] pathToMin = (TreeNode<K, V>[])new TreeNode<?, ?>[right.height];
            int minDepth = 0;
            while (min.left != null) {
                pathToMin[minDepth++] = min;
                min = min.left;
            }
            right = min.right;
            while (minDepth > 0) {
                TreeNode<K, V> node = pathToMin[--minDepth];
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
        if (cachedEntrySet == null) {
            cachedEntrySet = new EntrySet<>(this, null, true, false, null, true, false);
        }
        return cachedEntrySet;
    }

    @Override
    public TComparator<? super K> comparator() {
        return originalComparator;
    }

    @Override
    public TSortedMap<K, V> subMap(K fromKey, K toKey) {
        if (comparator.compare(fromKey, toKey) > 0) {
            throw new TIllegalArgumentException();
        }
        return new SubMap<>(this, fromKey, true, true, toKey, false, true);
    }

    @Override
    public TSortedMap<K, V> headMap(K toKey) {
        return new SubMap<>(this, null, true, false, toKey, false, true);
    }

    @Override
    public TSortedMap<K, V> tailMap(K fromKey) {
        return new SubMap<>(this, fromKey, true, true, null, false, false);
    }

    @Override
    public K firstKey() {
        TreeNode<K, V> node = firstNode();
        if (node == null) {
            throw new TNoSuchElementException();
        }
        return node.getKey();
    }

    @Override
    public K lastKey() {
        TreeNode<K, V> node = lastNode();
        if (node == null) {
            throw new TNoSuchElementException();
        }
        return node.getKey();
    }

    private TreeNode<K, V> firstNode() {
        TreeNode<K, V> node = root;
        TreeNode<K, V> prev = null;
        while (node != null) {
            prev = node;
            node = node.left;
        }
        return prev;
    }

    private TreeNode<K, V> lastNode() {
        TreeNode<K, V> node = root;
        TreeNode<K, V> prev = null;
        while (node != null) {
            prev = node;
            node = node.right;
        }
        return prev;
    }

    @Override
    public int size() {
        return root != null ? root.size : 0;
    }

    int height() {
        return root != null ? root.height : 0;
    }

    @Override
    public Object clone() {
        TTreeMap<?, ?> copy = (TTreeMap<?, ?>)super.clone();
        copy.cachedEntrySet = null;
        return copy;
    }

    private static class EntrySet<K, V> extends TAbstractSet<Entry<K, V>> {
        private int modCount = -1;
        private TTreeMap<K, V> owner;
        private K from;
        private boolean fromIncluded;
        private boolean fromChecked;
        private K to;
        private boolean toIncluded;
        private boolean toChecked;
        private int cachedSize;

        public EntrySet(TTreeMap<K, V> owner, K from, boolean fromIncluded, boolean fromChecked,
                K to, boolean toIncluded, boolean toChecked) {
            this.owner = owner;
            this.from = from;
            this.fromIncluded = fromIncluded;
            this.fromChecked = fromChecked;
            this.to = to;
            this.toIncluded = toIncluded;
            this.toChecked = toChecked;
        }

        @Override
        public int size() {
            int size = cachedSize;
            if (modCount != owner.modCount) {
                modCount = owner.modCount;
                size = owner.size();
                if (fromChecked) {
                    TreeNode<K, V>[] path = fromIncluded ? owner.pathToPrev(from) : owner.pathToExactOrPrev(from);
                    for (TreeNode<K, V> node : path) {
                        if (node.left != null) {
                            size -= node.left.size;
                        }
                    }
                    size -= path.length;
                }
                if (toChecked) {
                    TreeNode<K, V> path[] = toIncluded ? owner.pathToNext(to) : owner.pathToExactOrNext(to);
                    for (TreeNode<K, V> node : path) {
                        if (node.right != null) {
                            size -= node.right.size;
                        }
                    }
                    size -= path.length;
                }
                cachedSize = size;
            }
            return size;
        }

        @Override
        public TIterator<Entry<K, V>> iterator() {
            TreeNode<K, V>[] fromPath;
            if (fromChecked) {
                fromPath = fromIncluded ? owner.pathToExactOrNext(from) : owner.pathToNext(from);
            } else {
                fromPath = owner.pathToFirst();
            }
            TreeNode<K, V> toEntry;
            if (toChecked) {
                toEntry = toIncluded ? owner.findExactOrPrev(to) : owner.findPrev(to);
            } else {
                toEntry = owner.lastNode();
            }
            return new EntryIterator<>(owner, fromPath, toEntry);
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry<?, ?> entry = (Entry<?, ?>)o;
            Object key = entry.getKey();
            if (from != null) {
                @SuppressWarnings("unchecked")
                int cmp = owner.comparator.compare((K)key, from);
                if (fromIncluded ? cmp < 0 : cmp <= 0) {
                    return false;
                }
            }
            if (to != null) {
                @SuppressWarnings("unchecked")
                int cmp = owner.comparator.compare((K)key, to);
                if (toIncluded ? cmp > 0 : cmp >= 0) {
                    return false;
                }
            }
            TreeNode<?, V> node = owner.findExact(key);
            return node != null && node.equals(o);
        }

        @Override
        public boolean isEmpty() {
            return size() == 0;
        }
    }

    private static class EntryIterator<K, V> implements TIterator<Entry<K, V>> {
        private int modCount;
        private TTreeMap<K, V> owner;
        private TreeNode<K, V>[] path;
        private TreeNode<K, V> last;
        private TreeNode<K, V> to;
        private int depth;

        public EntryIterator(TTreeMap<K, V> owner, TreeNode<K, V>[] path, TreeNode<K, V> to) {
            this.owner = owner;
            modCount = owner.modCount;
            this.path = TArrays.copyOf(path, owner.root.height);
            depth = path.length;
            this.to = to;
        }

        @Override
        public boolean hasNext() {
            return depth > 0;
        }

        @Override
        public Entry<K, V> next() {
            if (modCount != owner.modCount) {
                throw new TConcurrentModificationException();
            }
            if (depth == 0) {
                throw new TNoSuchElementException();
            }
            TreeNode<K, V> node = path[--depth];
            last = node;
            if (node.right != null) {
                node = node.right;
                while (node != null) {
                    path[depth++] = node;
                    node = node.left;
                }
            }
            if (last == to) {
                depth = 0;
            }
            return last;
        }

        @Override
        public void remove() {
            if (modCount != owner.modCount) {
                throw new TConcurrentModificationException();
            }
            if (last == null) {
                throw new TNoSuchElementException();
            }
            owner.root = owner.deleteNode(owner.root, last.getKey());
            modCount = ++owner.modCount;
            last = null;
        }
    }

    private static class SubMap<K, V> extends TAbstractMap<K, V> implements TSortedMap<K, V>, TSerializable {
        private int modCount = -1;
        private int cachedSize;
        private TTreeMap<K, V> owner;
        private K from;
        private boolean fromIncluded;
        private boolean fromChecked;
        private K to;
        private boolean toIncluded;
        private boolean toChecked;
        private EntrySet<K, V> entrySetCache;

        public SubMap(TTreeMap<K, V> owner, K from, boolean fromIncluded, boolean fromChecked,
                K to, boolean toIncluded, boolean toChecked) {
            this.owner = owner;
            this.from = from;
            this.fromIncluded = fromIncluded;
            this.fromChecked = fromChecked;
            this.to = to;
            this.toIncluded = toIncluded;
            this.toChecked = toChecked;
        }

        @Override
        public TSet<Entry<K, V>> entrySet() {
            if (entrySetCache == null) {
                entrySetCache = new EntrySet<>(owner, from, fromIncluded, fromChecked, to, toIncluded, toChecked);
            }
            return entrySetCache;
        }

        @Override
        public TComparator<? super K> comparator() {
            return owner.originalComparator;
        }

        private void checkKey(K key) {
            if (fromChecked) {
                int cmp = owner.comparator.compare(key, from);
                if (fromIncluded ? cmp < 0 : cmp <= 0) {
                    throw new TIllegalArgumentException();
                }
            }
            if (toChecked) {
                int cmp = owner.comparator.compare(key, to);
                if (fromIncluded ? cmp > 0 : cmp >= 0) {
                    throw new TIllegalArgumentException();
                }
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public V get(Object key) {
            checkKey((K)key);
            return owner.get(key);
        }

        @SuppressWarnings("unchecked")
        @Override
        public V remove(Object key) {
            checkKey((K)key);
            return owner.remove(key);
        }

        @Override
        public V put(K key, V value) {
            checkKey(key);
            return owner.put(key, value);
        }

        @Override
        public int size() {
            int size = cachedSize;
            if (modCount != owner.modCount) {
                modCount = owner.modCount;
                size = owner.size();
                if (fromChecked) {
                    TreeNode<K, V>[] path = fromIncluded ? owner.pathToPrev(from) : owner.pathToExactOrPrev(from);
                    for (TreeNode<K, V> node : path) {
                        if (node.left != null) {
                            size -= node.left.size;
                        }
                    }
                    size -= path.length;
                }
                if (toChecked) {
                    TreeNode<K, V> path[] = toIncluded ? owner.pathToNext(to) : owner.pathToExactOrNext(to);
                    for (TreeNode<K, V> node : path) {
                        if (node.right != null) {
                            size -= node.right.size;
                        }
                    }
                    size -= path.length;
                }
                cachedSize = size;
            }
            return size;
        }

        @Override
        public boolean containsKey(Object key) {
            if (fromChecked) {
                @SuppressWarnings("unchecked")
                int cmp = owner.comparator.compare((K)key, from);
                if (fromIncluded ? cmp < 0 : cmp <= 0) {
                    return false;
                }
            }
            if (toChecked) {
                @SuppressWarnings("unchecked")
                int cmp = owner.comparator.compare((K)key, to);
                if (fromIncluded ? cmp > 0 : cmp >= 0) {
                    return false;
                }
            }
            return owner.containsKey(key);
        }

        @Override
        public TSortedMap<K, V> subMap(K fromKey, K toKey) {
            checkKey(fromKey);
            checkKey(toKey);
            return new SubMap<>(owner, fromKey, true, true, toKey, false, true);
        }

        @Override
        public TSortedMap<K, V> headMap(K toKey) {
            checkKey(toKey);
            return new SubMap<>(owner, from, fromIncluded, fromChecked, toKey, false, true);
        }

        @Override
        public TSortedMap<K, V> tailMap(K fromKey) {
            checkKey(fromKey);
            return new SubMap<>(owner, fromKey, true, true, to, toIncluded, toChecked);
        }

        @Override
        public K firstKey() {
            TreeNode<K, V> node = firstNode();
            if (node == null) {
                throw new TNoSuchElementException();
            }
            return node.getKey();
        }

        @Override
        public K lastKey() {
            TreeNode<K, V> node = lastNode();
            if (node == null) {
                throw new TNoSuchElementException();
            }
            return node.getKey();
        }

        private TreeNode<K, V> firstNode() {
            TreeNode<K, V> node;
            if (fromChecked) {
                node = fromIncluded ? owner.findExactOrNext(from) : owner.findNext(from);
            } else {
                node = owner.firstNode();
            }
            if (toChecked) {
                int cmp = owner.comparator.compare(node.getKey(), to);
                if (toIncluded ? cmp > 0 : cmp >= 0) {
                    return null;
                }
            }
            return node;
        }

        private TreeNode<K, V> lastNode() {
            TreeNode<K, V> node;
            if (toChecked) {
                node = toIncluded ? owner.findExactOrPrev(to) : owner.findPrev(to);
            } else {
                node = owner.lastNode();
            }
            if (fromChecked) {
                int cmp = owner.comparator.compare(node.getKey(), from);
                if (fromIncluded ? cmp < 0 : cmp <= 0) {
                    return null;
                }
            }
            return node;
        }
    }
}
