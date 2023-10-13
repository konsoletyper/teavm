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
import org.teavm.classlib.java.lang.TCloneable;

public class TTreeMap<K, V> extends TAbstractMap<K, V> implements TCloneable, TSerializable, TNavigableMap<K, V> {
    static class TreeNode<K, V> extends SimpleEntry<K, V> {
        TreeNode<K, V> left;
        TreeNode<K, V> right;
        int height = 1;
        int size = 1;

        TreeNode(K key) {
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

        public TreeNode<K, V> forward(boolean reverse) {
            return !reverse ? left : right;
        }

        public TreeNode<K, V> down(boolean reverse) {
            return !reverse ? right : left;
        }
    }

    private static <K, V> TMap.Entry<K, V> clone(TMap.Entry<K, V> entry) {
        return entry == null ? null : TMap.entry(entry.getKey(), entry.getValue());
    }

    TreeNode<K, V> root;
    private TComparator<? super K> comparator;
    private TComparator<? super K> originalComparator;
    private TComparator<? super K> revertedComparator;
    private int modCount;
    private EntrySet<K, V> cachedEntrySet;
    private NavigableKeySet<K, V> cachedNavigableKeySet;

    public TTreeMap() {
        this((TComparator<? super K>) null);
    }

    public TTreeMap(TComparator<? super K> comparator) {
        this.originalComparator = comparator;
        if (comparator == null) {
            comparator = TComparator.NaturalOrder.instance();
        }
        this.comparator = comparator;
    }

    public TTreeMap(TMap<? extends K, ? extends V> m) {
        this((TComparator<? super K>) null);
        @SuppressWarnings("unchecked")
        Entry<K, V>[] entries = (Entry<K, V>[]) new Entry<?, ?>[m.size()];
        entries = m.entrySet().toArray(entries);
        TArrays.sort(entries, (o1, o2) -> comparator.compare(o1.getKey(), o2.getKey()));
        fillMap(entries);
    }

    public TTreeMap(TSortedMap<K, ? extends V> m) {
        this(m.comparator());
        @SuppressWarnings("unchecked")
        Entry<K, V>[] entries = (Entry<K, V>[]) new Entry<?, ?>[m.size()];
        entries = m.entrySet().toArray(entries);
        fillMap(entries);
    }

    private void ensureRevertedComparator() {
        if (revertedComparator == null) {
            revertedComparator = (o1, o2) -> -originalComparator.compare(o1, o2);
        }
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
        TreeNode<K, V> node = new TreeNode<>(entry.getKey());
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
        @SuppressWarnings("unchecked")
        K k = (K) key;
        comparator.compare(k, k);
        while (node != null) {
            int cmp = comparator.compare(k, node.getKey());
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

    TreeNode<K, V> findExactOrNext(Object key, boolean reverse) {
        TreeNode<K, V> node = root;
        TreeNode<K, V> lastForward = null;
        while (node != null) {
            @SuppressWarnings("unchecked")
            int cmp = comparator.compare((K) key, node.getKey());
            if (reverse) {
                cmp = -cmp;
            }
            if (cmp == 0) {
                return node;
            } else if (cmp < 0) {
                lastForward = node;
                node = node.forward(reverse);
            } else {
                node = node.down(reverse);
            }
        }
        return lastForward;
    }

    TreeNode<K, V>[] pathToExactOrNext(Object key, boolean reverse) {
        @SuppressWarnings("unchecked")
        TreeNode<K, V>[] path = (TreeNode<K, V>[]) new TreeNode<?, ?>[height()];
        int depth = 0;
        TreeNode<K, V> node = root;
        while (node != null) {
            @SuppressWarnings("unchecked")
            int cmp = comparator.compare((K) key, node.getKey());
            if (reverse) {
                cmp = -cmp;
            }
            if (cmp == 0) {
                path[depth++] = node;
                break;
            } else if (cmp < 0) {
                path[depth++] = node;
                node = node.forward(reverse);
            } else {
                node = node.down(reverse);
            }
        }
        return TArrays.copyOf(path, depth);
    }

    TreeNode<K, V> findNext(Object key, boolean reverse) {
        TreeNode<K, V> node = root;
        TreeNode<K, V> lastForward = null;
        while (node != null) {
            @SuppressWarnings("unchecked")
            int cmp = comparator.compare((K) key, node.getKey());
            if (reverse) {
                cmp = -cmp;
            }
            if (cmp < 0) {
                lastForward = node;
                node = node.forward(reverse);
            } else {
                node = node.down(reverse);
            }
        }
        return lastForward;
    }

    TreeNode<K, V>[] pathToNext(Object key, boolean reverse) {
        @SuppressWarnings("unchecked")
        TreeNode<K, V>[] path = (TreeNode<K, V>[]) new TreeNode<?, ?>[height()];
        int depth = 0;
        TreeNode<K, V> node = root;
        while (node != null) {
            @SuppressWarnings("unchecked")
            int cmp = comparator.compare((K) key, node.getKey());
            if (reverse) {
                cmp = -cmp;
            }
            if (cmp < 0) {
                path[depth++] = node;
                node = node.forward(reverse);
            } else {
                node = node.down(reverse);
            }
        }
        return TArrays.copyOf(path, depth);
    }

    TreeNode<K, V>[] pathToFirst(boolean reverse) {
        @SuppressWarnings("unchecked")
        TreeNode<K, V>[] path = (TreeNode<K, V>[]) new TreeNode<?, ?>[height()];
        int depth = 0;
        TreeNode<K, V> node = root;
        while (node != null) {
            path[depth++] = node;
            node = node.forward(reverse);
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
        int cmp = comparator.compare((K) key, root.getKey());
        if (cmp < 0) {
            root.left = deleteNode(root.left, key);
        } else if (cmp > 0) {
            root.right = deleteNode(root.right, key);
        } else if (root.right == null) {
            return root.left;
        } else {
            TreeNode<K, V> left = root.left;
            TreeNode<K, V> right = root.right;
            TreeNode<K, V> min = right;
            @SuppressWarnings("unchecked")
            TreeNode<K, V>[] pathToMin = (TreeNode<K, V>[]) new TreeNode<?, ?>[right.height];
            int minDepth = 0;
            while (min.left != null) {
                pathToMin[minDepth++] = min;
                min = min.left;
            }
            right = min.right;
            while (minDepth > 0) {
                TreeNode<K, V> node = pathToMin[--minDepth];
                node.left = right;
                node.fix();
                node = node.balance();
                right = node;
            }
            min.right = right;
            min.left = left;
            root = min;
            root.fix();
        }
        root.fix();
        return root.balance();
    }

    @Override
    public TSet<Entry<K, V>> entrySet() {
        return sequencedEntrySet();
    }

    @Override
    public TComparator<? super K> comparator() {
        return originalComparator;
    }

    @Override
    public TSortedMap<K, V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    @Override
    public TNavigableMap<K, V> headMap(K toKey) {
        return headMap(toKey, false);
    }

    @Override
    public TNavigableMap<K, V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }

    @Override
    public K firstKey() {
        TreeNode<K, V> node = firstNode(false);
        if (node == null) {
            throw new TNoSuchElementException();
        }
        return node.getKey();
    }

    @Override
    public K lastKey() {
        TreeNode<K, V> node = firstNode(true);
        if (node == null) {
            throw new TNoSuchElementException();
        }
        return node.getKey();
    }

    @Override
    public Entry<K, V> lowerEntry(K key) {
        return clone(findNext(key, true));
    }

    @Override
    public K lowerKey(K key) {
        TreeNode<K, V> node = findNext(key, true);
        return node != null ? node.getKey() : null;
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
        return clone(findExactOrNext(key, true));
    }

    @Override
    public K floorKey(K key) {
        TreeNode<K, V> node = findExactOrNext(key, true);
        return node != null ? node.getKey() : null;
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
        return clone(findExactOrNext(key, false));
    }

    @Override
    public K ceilingKey(K key) {
        TreeNode<K, V> node = findExactOrNext(key, false);
        return node != null ? node.getKey() : null;
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
        return clone(findNext(key, false));
    }

    @Override
    public K higherKey(K key) {
        TreeNode<K, V> node = findNext(key, false);
        return node != null ? node.getKey() : null;
    }

    @Override
    public Entry<K, V> firstEntry() {
        return clone(firstNode(false));
    }

    @Override
    public Entry<K, V> lastEntry() {
        return clone(firstNode(true));
    }

    @Override
    public Entry<K, V> pollFirstEntry() {
        TreeNode<K, V> node = firstNode(false);
        if (node != null) {
            root = deleteNode(root, node.getKey());
            modCount++;
        }
        return clone(node);
    }

    @Override
    public Entry<K, V> pollLastEntry() {
        TreeNode<K, V> node = firstNode(true);
        if (node != null) {
            root = deleteNode(root, node.getKey());
            modCount++;
        }
        return clone(node);
    }

    @Override
    public TCollection<V> values() {
        return sequencedValues();
    }

    @Override
    public TSequencedCollection<V> sequencedValues() {
        if (cachedValues == null) {
            cachedValues = new NavigableMapValues<>(this);
        }
        return (TSequencedCollection<V>) cachedValues;
    }

    @Override
    public TSequencedSet<Entry<K, V>> sequencedEntrySet() {
        if (cachedEntrySet == null) {
            cachedEntrySet = new EntrySet<>(this, null, true, false, null, true, false, false);
        }
        return cachedEntrySet;
    }

    @Override
    public TNavigableMap<K, V> descendingMap() {
        return new MapView<>(this, null, false, false, null, false, false, true);
    }

    @Override
    public TNavigableSet<K> navigableKeySet() {
        if (cachedNavigableKeySet == null) {
            cachedNavigableKeySet = new NavigableKeySet<>(this);
        }
        return cachedNavigableKeySet;
    }

    @Override
    public TNavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    @Override
    public TNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return new MapView<>(this, fromKey, fromInclusive, true, toKey, toInclusive, true, false);
    }

    @Override
    public TNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return new MapView<>(this, null, false, false, toKey, inclusive, true, false);
    }

    @Override
    public TNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return new MapView<>(this, fromKey, inclusive, true, null, false, false, false);
    }

    private TreeNode<K, V> firstNode(boolean reverse) {
        TreeNode<K, V> node = root;
        TreeNode<K, V> prev = null;
        while (node != null) {
            prev = node;
            node = node.forward(reverse);
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
        TTreeMap<?, ?> copy = (TTreeMap<?, ?>) super.clone();
        copy.cachedEntrySet = null;
        return copy;
    }

    static class EntrySet<K, V> extends TAbstractSet<Entry<K, V>> implements TSequencedSet<Entry<K, V>> {
        private int modCount = -1;
        private TTreeMap<K, V> owner;
        private K from;
        private boolean fromIncluded;
        private boolean fromChecked;
        private K to;
        private boolean toIncluded;
        private boolean toChecked;
        private int cachedSize;
        private boolean reverse;

         EntrySet(TTreeMap<K, V> owner, K from, boolean fromIncluded, boolean fromChecked,
                K to, boolean toIncluded, boolean toChecked, boolean reverse) {
            this.owner = owner;
            this.from = from;
            this.fromIncluded = fromIncluded;
            this.fromChecked = fromChecked;
            this.to = to;
            this.toIncluded = toIncluded;
            this.toChecked = toChecked;
            this.reverse = reverse;
        }

        @Override
        public boolean isEmpty() {
            return !iterator().hasNext();
        }

        @Override
        public int size() {
            if (modCount != owner.modCount) {
                modCount = owner.modCount;
                int size = owner.size();
                TreeNode<K, V>[] fromPath = null;
                if (fromChecked) {
                    fromPath = fromIncluded ? owner.pathToNext(from, true)
                            : owner.pathToExactOrNext(from, true);
                    for (TreeNode<K, V> node : fromPath) {
                        if (node.left != null) {
                            size -= node.left.size;
                        }
                    }
                    size -= fromPath.length;
                }
                if (toChecked) {
                    TreeNode<K, V>[] path = toIncluded ? owner.pathToNext(to, false)
                            : owner.pathToExactOrNext(to, false);
                    for (TreeNode<K, V> node : path) {
                        if (node.right != null) {
                            size -= node.right.size;
                        }
                    }
                    size -= path.length;
                    if (fromPath != null && fromPath.length > 0 && path.length > 0
                            && fromPath[fromPath.length - 1] == path[path.length - 1]) {
                        size++;
                    }
                }
                cachedSize = size;
            }
            return cachedSize;
        }

        @Override
        public TIterator<Entry<K, V>> iterator() {
            return !reverse ? ascendingIterator() : descendingIterator();
        }

        private TIterator<Entry<K, V>> ascendingIterator() {
            TreeNode<K, V>[] fromPath;
            if (fromChecked) {
                fromPath = fromIncluded ? owner.pathToExactOrNext(from, false) : owner.pathToNext(from, false);
            } else {
                fromPath = owner.pathToFirst(false);
            }
            return new EntryIterator<>(owner, fromPath, to, toChecked, toIncluded, false);
        }

        private TIterator<Entry<K, V>> descendingIterator() {
            TreeNode<K, V>[] toPath;
            if (toChecked) {
                toPath = toIncluded ? owner.pathToExactOrNext(to, true) : owner.pathToNext(to, true);
            } else {
                toPath = owner.pathToFirst(true);
            }
            return new EntryIterator<>(owner, toPath, from, fromChecked, fromIncluded, true);
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry<?, ?> entry = (Entry<?, ?>) o;
            Object key = entry.getKey();
            if (from != null) {
                @SuppressWarnings("unchecked")
                int cmp = owner.comparator.compare((K) key, from);
                if (fromIncluded ? cmp < 0 : cmp <= 0) {
                    return false;
                }
            }
            if (to != null) {
                @SuppressWarnings("unchecked")
                int cmp = owner.comparator.compare((K) key, to);
                if (toIncluded ? cmp > 0 : cmp >= 0) {
                    return false;
                }
            }
            TreeNode<?, V> node = owner.findExact(key);
            return node != null && node.equals(o);
        }

        @Override
        public TSequencedSet<Entry<K, V>> reversed() {
            return new EntrySet<>(owner, from, fromIncluded, fromChecked, to, toIncluded, toChecked, !reverse);
        }
    }

    static class EntryIterator<K, V> implements TIterator<Entry<K, V>> {
        private int modCount;
        private TTreeMap<K, V> owner;
        private TreeNode<K, V>[] path;
        private TreeNode<K, V> last;
        private K to;
        private boolean toChecked;
        private boolean toIncluded;
        private int depth;
        private boolean reverse;

        EntryIterator(TTreeMap<K, V> owner, TreeNode<K, V>[] path, K to, boolean toChecked, boolean toIncluded,
                boolean reverse) {
            this.owner = owner;
            modCount = owner.modCount;
            this.path = TArrays.copyOf(path, owner.root == null ? 0 : owner.root.height);
            depth = path.length;
            this.to = to;
            this.toChecked = toChecked;
            this.toIncluded = toIncluded;
            this.reverse = reverse;
            checkFinished();
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
            TreeNode<K, V> down = node.down(reverse);
            if (down != null) {
                node = down;
                while (node != null) {
                    path[depth++] = node;
                    node = node.forward(reverse);
                }
            }

            checkFinished();
            return last;
        }

        private void checkFinished() {
            if (!toChecked || depth == 0) {
                return;
            }
            int cmp = owner.comparator.compare(path[depth - 1].getKey(), to);
            if (reverse) {
                cmp = -cmp;
            }
            if (toIncluded) {
                if (cmp > 0) {
                    depth = 0;
                }
            } else {
                if (cmp >= 0) {
                    depth = 0;
                }
            }
        }

        @Override
        public void remove() {
            if (modCount != owner.modCount) {
                throw new TConcurrentModificationException();
            }
            if (last == null) {
                throw new IllegalStateException();
            }
            owner.root = owner.deleteNode(owner.root, last.getKey());
            var newPath = owner.pathToNext(last.getKey(), reverse);
            System.arraycopy(newPath, 0, path, 0, newPath.length);
            depth = newPath.length;
            modCount = ++owner.modCount;
            last = null;
        }
    }

    static class MapView<K, V> extends TAbstractMap<K, V> implements TNavigableMap<K, V>, TSerializable {
        private TTreeMap<K, V> owner;
        private K from;
        private boolean fromIncluded;
        private boolean fromChecked;
        private K to;
        private boolean toIncluded;
        private boolean toChecked;
        private EntrySet<K, V> entrySetCache;
        private boolean reverse;
        private NavigableKeySet<K, V> cachedNavigableKeySet;

        MapView(TTreeMap<K, V> owner, K from, boolean fromIncluded, boolean fromChecked,
                K to, boolean toIncluded, boolean toChecked, boolean reverse) {
            check(owner, from, fromChecked, to, toChecked);
            this.owner = owner;
            this.from = from;
            this.fromIncluded = fromIncluded;
            this.fromChecked = fromChecked;
            this.to = to;
            this.toIncluded = toIncluded;
            this.toChecked = toChecked;
            this.reverse = reverse;
            if (reverse) {
                owner.ensureRevertedComparator();
            }
        }

        private void check(TTreeMap<K, V> owner, K from, boolean fromChecked, K to, boolean toChecked) {
            if (fromChecked) {
                if (toChecked) {
                    if (owner.comparator.compare(from, to) > 0) {
                        throw new IllegalArgumentException();
                    }
                } else {
                    owner.comparator.compare(from, from);
                }
            } else {
                if (toChecked) {
                    owner.comparator.compare(to, to);
                }
            }
        }

        @Override
        public TSet<Entry<K, V>> entrySet() {
            return sequencedEntrySet();
        }

        @Override
        public TComparator<? super K> comparator() {
            return !reverse ? owner.originalComparator : owner.revertedComparator;
        }

        private void checkKey(K key, boolean inclusive) {
            boolean inRange = inclusive ? keyInRange(key) : keyInClosedRange(key);
            if (!inRange) {
                throw new IllegalArgumentException();
            }
        }

        private boolean keyInClosedRange(K key) {
            if (fromChecked) {
                if (owner.comparator.compare(key, from) < 0) {
                    return false;
                }
            }
            if (toChecked) {
                if (owner.comparator.compare(key, to) > 0) {
                    return false;
                }
            }
            return true;
        }

        private boolean keyInRange(K key) {
            if (fromChecked) {
                int cmp = owner.comparator.compare(key, from);
                if (fromIncluded ? cmp < 0 : cmp <= 0) {
                    return false;
                }
            }
            if (toChecked) {
                int cmp = owner.comparator.compare(key, to);
                if (toIncluded ? cmp > 0 : cmp >= 0) {
                    return false;
                }
            }
            return true;
        }

        @SuppressWarnings("unchecked")
        @Override
        public V get(Object key) {
            if (!keyInRange((K) key)) {
                return null;
            }
            return owner.get(key);
        }

        @SuppressWarnings("unchecked")
        @Override
        public V remove(Object key) {
            if (!keyInRange((K) key)) {
                return null;
            }
            return owner.remove(key);
        }

        @Override
        public V put(K key, V value) {
            if (!keyInRange(key)) {
                throw new IllegalArgumentException();
            }
            return owner.put(key, value);
        }

        @Override
        public void clear() {
            if (fromChecked || toChecked) {
                entrySet().clear();
            } else {
                owner.clear();
            }
        }

        @Override
        public boolean isEmpty() {
            return fromChecked || toChecked ? entrySet().isEmpty() : owner.isEmpty();
        }

        @Override
        public int size() {
            return fromChecked || toChecked ? entrySet().size() : owner.size();
        }

        @Override
        public boolean containsKey(Object key) {
            if (fromChecked) {
                @SuppressWarnings("unchecked")
                int cmp = owner.comparator.compare((K) key, from);
                if (fromIncluded ? cmp < 0 : cmp <= 0) {
                    return false;
                }
            }
            if (toChecked) {
                @SuppressWarnings("unchecked")
                int cmp = owner.comparator.compare((K) key, to);
                if (toIncluded ? cmp > 0 : cmp >= 0) {
                    return false;
                }
            }
            return owner.containsKey(key);
        }

        @Override
        public K firstKey() {
            TreeNode<K, V> node = !reverse ? firstNode() : lastNode();
            if (node == null) {
                throw new TNoSuchElementException();
            }
            return node.getKey();
        }

        @Override
        public K lastKey() {
            TreeNode<K, V> node = !reverse ? lastNode() : firstNode();
            if (node == null) {
                throw new TNoSuchElementException();
            }
            return node.getKey();
        }

        private TreeNode<K, V> firstNode() {
            TreeNode<K, V> node;
            if (fromChecked) {
                node = fromIncluded ? owner.findExactOrNext(from, false) : owner.findNext(from, false);
            } else {
                node = owner.firstNode(false);
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
                node = toIncluded ? owner.findExactOrNext(to, true) : owner.findNext(to, true);
            } else {
                node = owner.firstNode(true);
            }
            if (fromChecked) {
                int cmp = owner.comparator.compare(node.getKey(), from);
                if (fromIncluded ? cmp < 0 : cmp <= 0) {
                    return null;
                }
            }
            return node;
        }

        @Override
        public Entry<K, V> lowerEntry(K key) {
            // TODO: implement
            return null;
        }

        @Override
        public K lowerKey(K key) {
            // TODO: implement
            return null;
        }

        @Override
        public Entry<K, V> floorEntry(K key) {
            // TODO: implement
            return null;
        }

        @Override
        public K floorKey(K key) {
            // TODO: implement
            return null;
        }

        @Override
        public Entry<K, V> ceilingEntry(K key) {
            // TODO: implement
            return null;
        }

        @Override
        public K ceilingKey(K key) {
            // TODO: implement
            return null;
        }

        @Override
        public Entry<K, V> higherEntry(K key) {
            // TODO: implement
            return null;
        }

        @Override
        public K higherKey(K key) {
            // TODO: implement
            return null;
        }

        @Override
        public Entry<K, V> firstEntry() {
            return TTreeMap.clone(!reverse ? firstNode() : lastNode());
        }

        @Override
        public Entry<K, V> lastEntry() {
            return TTreeMap.clone(!reverse ? lastNode() : firstNode());
        }

        @Override
        public Entry<K, V> pollFirstEntry() {
            TreeNode<K, V> node = !reverse ? firstNode() : lastNode();
            if (node != null) {
                owner.root = owner.deleteNode(owner.root, node.getKey());
                owner.modCount++;
            }
            return TTreeMap.clone(node);
        }

        @Override
        public Entry<K, V> pollLastEntry() {
            TreeNode<K, V> node = !reverse ? lastNode() : firstNode();
            if (node != null) {
                owner.root = owner.deleteNode(owner.root, node.getKey());
                owner.modCount++;
            }
            return TTreeMap.clone(node);
        }

        @Override
        public TCollection<V> values() {
            return sequencedValues();
        }

        @Override
        public TSequencedCollection<V> sequencedValues() {
            if (cachedValues == null) {
                cachedValues = new NavigableMapValues<>(this);
            }
            return (TSequencedCollection<V>) cachedValues;
        }

        @Override
        public TSequencedSet<Entry<K, V>> sequencedEntrySet() {
            if (entrySetCache == null) {
                entrySetCache = new EntrySet<>(owner, from, fromIncluded,
                        fromChecked, to, toIncluded, toChecked, reverse);
            }
            return entrySetCache;
        }

        @Override
        public TNavigableMap<K, V> descendingMap() {
            return new MapView<>(owner, from, fromIncluded, fromChecked, to, toIncluded, toChecked, !reverse);
        }

        @Override
        public TNavigableSet<K> navigableKeySet() {
            if (cachedNavigableKeySet == null) {
                cachedNavigableKeySet = new NavigableKeySet<>(this);
            }
            return cachedNavigableKeySet;
        }

        @Override
        public TNavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }


        @Override
        public TSortedMap<K, V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        @Override
        public TNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            checkKey(fromKey, fromInclusive);
            checkKey(toKey, toInclusive);
            if (!reverse) {
                return new MapView<>(owner, fromKey, fromInclusive, true, toKey, toInclusive, true, false);
            } else {
                return new MapView<>(owner, toKey, toInclusive, true, fromKey, fromInclusive, true, true);
            }
        }

        @Override
        public TSortedMap<K, V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        @Override
        public TNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            checkKey(toKey, inclusive);
            if (!reverse) {
                return new MapView<>(owner, from, fromIncluded, fromChecked, toKey, inclusive, true, false);
            } else {
                return new MapView<>(owner, toKey, inclusive, true, to, toIncluded, toChecked, true);
            }
        }

        @Override
        public TSortedMap<K, V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }

        @Override
        public TNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            checkKey(fromKey, inclusive);
            if (!reverse) {
                return new MapView<>(owner, fromKey, inclusive, true, to, toIncluded, toChecked, false);
            } else {
                return new MapView<>(owner, from, fromIncluded, fromChecked, fromKey, inclusive, true, true);
            }
        }
    }

    static class NavigableKeySet<K, V> extends TAbstractSet<K> implements TNavigableSet<K> {
        private final TNavigableMap<K, V> map;

        NavigableKeySet(TNavigableMap<K, V> map) {
            this.map = map;
        }

        @Override
        public TComparator<? super K> comparator() {
            return map.comparator();
        }

        @Override
        public TSortedSet<K> subSet(K fromElement, K toElement) {
            return map.subMap(fromElement, true, toElement, false).navigableKeySet();
        }

        @Override
        public TSortedSet<K> headSet(K toElement) {
            return map.headMap(toElement, false).navigableKeySet();
        }

        @Override
        public TSortedSet<K> tailSet(K fromElement) {
            return map.tailMap(fromElement, true).navigableKeySet();
        }

        @Override
        public K first() {
            return map.firstKey();
        }

        @Override
        public K last() {
            return map.lastKey();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return map.containsKey(o);
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public void clear() {
            map.clear();
        }

        @Override
        public boolean remove(Object o) {
            int old = map.size();
            map.remove(o);
            return map.size() != old;
        }

        @Override
        public TIterator<K> iterator() {
            return map.keySet().iterator();
        }

        @Override
        public K lower(K e) {
            return map.lowerKey(e);
        }

        @Override
        public K floor(K e) {
            return map.floorKey(e);
        }

        @Override
        public K ceiling(K e) {
            return map.ceilingKey(e);
        }

        @Override
        public K higher(K e) {
            return map.higherKey(e);
        }

        @Override
        public K pollFirst() {
            TMap.Entry<K, V> entry = map.pollFirstEntry();
            return entry != null ? entry.getKey() : null;
        }

        @Override
        public K pollLast() {
            TMap.Entry<K, V> entry = map.pollLastEntry();
            return entry != null ? entry.getKey() : null;
        }

        @Override
        public TNavigableSet<K> descendingSet() {
            return map.descendingMap().navigableKeySet();
        }

        @Override
        public TIterator<K> descendingIterator() {
            return descendingSet().iterator();
        }

        @Override
        public TNavigableSet<K> subSet(K fromElement, boolean fromInclusive, K toElement, boolean toInclusive) {
            return map.subMap(fromElement, fromInclusive, toElement, toInclusive).navigableKeySet();
        }

        @Override
        public TNavigableSet<K> headSet(K toElement, boolean inclusive) {
            return map.headMap(toElement, inclusive).navigableKeySet();
        }

        @Override
        public TNavigableSet<K> tailSet(K fromElement, boolean inclusive) {
            return map.tailMap(fromElement, inclusive).navigableKeySet();
        }
    }

    static class NavigableMapValues<K, V> extends TAbstractCollection<V> implements TSequencedCollection<V> {
        private final TNavigableMap<K, V> map;

        NavigableMapValues(TNavigableMap<K, V> map) {
            this.map = map;
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public TIterator<V> iterator() {
            final TIterator<TMap.Entry<K, V>> it = map.entrySet().iterator();
            return new TIterator<>() {
                @Override public boolean hasNext() {
                    return it.hasNext();
                }
                @Override public V next() {
                    return it.next().getValue();
                }
                @Override public void remove() {
                    it.remove();
                }
            };
        }

        @Override
        public boolean remove(Object o) {
            for (TIterator<TMap.Entry<K, V>> it = map.entrySet().iterator(); it.hasNext();) {
                TMap.Entry<K, V> e = it.next();
                if (TObjects.equals(e.getValue(), o)) {
                    it.remove();
                    return true;
                }
            }
            return false;
        }

        @Override
        public TSequencedCollection<V> reversed() {
            return new NavigableMapValues<>(map.reversed());
        }
    }
}
