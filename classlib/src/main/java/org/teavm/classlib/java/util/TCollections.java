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

import java.util.RandomAccess;
import org.teavm.classlib.java.lang.*;
import org.teavm.classlib.java.util.TMap.Entry;
import org.teavm.classlib.java.util.random.TRandomGenerator;

public final class TCollections extends TObject {
    @SuppressWarnings("rawtypes")
    public static final TSet EMPTY_SET = new TTemplateCollections.AbstractImmutableSet<>() {
        @Override public int size() {
            return 0;
        }
        @Override public TIterator<Object> iterator() {
            return emptyIterator();
        }
        @Override public boolean isEmpty() {
            return true;
        }
        @Override public boolean contains(Object o) {
            return false;
        }
        @Override public boolean containsAll(TCollection<?> c) {
            return c.isEmpty();
        }
    };

    @SuppressWarnings("rawtypes")
    public static final TMap EMPTY_MAP = new TTemplateCollections.AbstractImmutableMap<>() {
        @Override public TSet<Entry<Object, Object>> entrySet() {
            return emptySet();
        }
        @Override
        public int size() {
            return 0;
        }
        @Override public Object get(Object key) {
            return null;
        }
        @Override public boolean isEmpty() {
            return true;
        }
        @Override public boolean containsKey(Object key) {
            return false;
        }
        @Override public boolean containsValue(Object value) {
            return false;
        }
    };

    @SuppressWarnings("rawtypes")
    public static final TList EMPTY_LIST = new TTemplateCollections.AbstractImmutableList<>() {
        @Override public Object get(int index) {
            throw new TIndexOutOfBoundsException();
        }
        @Override public int size() {
            return 0;
        }
        @Override
        public TIterator<Object> iterator() {
            return emptyIterator();
        }
        @Override
        public TListIterator<Object> listIterator(int index) {
            if (index != 0) {
                throw new IndexOutOfBoundsException();
            }
            return emptyListIterator();
        }
        @Override public boolean isEmpty() {
            return true;
        }
    };

    private static final TIterator<?> EMPTY_ITERATOR = new TIterator<>() {
        @Override public boolean hasNext() {
            return false;
        }
        @Override public Object next() {
            throw new TNoSuchElementException();
        }
        @Override public void remove() {
            throw new TIllegalStateException();
        }
    };

    private static final TListIterator<?> EMPTY_LIST_ITERATOR = new TListIterator<>() {
        @Override public boolean hasNext() {
            return false;
        }
        @Override public Object next() {
            throw new TNoSuchElementException();
        }
        @Override public void remove() {
            throw new TIllegalStateException();
        }
        @Override public boolean hasPrevious() {
            return false;
        }
        @Override public Object previous() {
            throw new TNoSuchElementException();
        }
        @Override public int nextIndex() {
            return 0;
        }
        @Override public int previousIndex() {
            return -1;
        }
        @Override public void set(Object e) {
            throw new TUnsupportedOperationException();
        }
        @Override public void add(Object e) {
            throw new TUnsupportedOperationException();
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> TIterator<T> emptyIterator() {
        return (TIterator<T>) EMPTY_ITERATOR;
    }

    @SuppressWarnings("unchecked")
    public static <T> TListIterator<T> emptyListIterator() {
        return (TListIterator<T>) EMPTY_LIST_ITERATOR;
    }

    @SuppressWarnings("unchecked")
    public static <T> TList<T> emptyList() {
        return (TList<T>) EMPTY_LIST;
    }

    public static <T> TEnumeration<T> emptyEnumeration() {
        return new TEnumeration<>() {
            @Override public boolean hasMoreElements() {
                return false;
            }
            @Override public T nextElement() {
                throw new TNoSuchElementException();
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> TSet<T> emptySet() {
        return (TSet<T>) EMPTY_SET;
    }

    @SuppressWarnings("unchecked")
    public static <K, V> TMap<K, V> emptyMap() {
        return (TMap<K, V>) EMPTY_MAP;
    }

    public static <T> TList<T> singletonList(T o) {
        return new TTemplateCollections.SingleElementList<>(o);
    }

    public static <T> TSet<T> singleton(T o) {
        return new TTemplateCollections.SingleElementSet<>(o);
    }

    public static <K, V> TMap<K, V> singletonMap(final K key, final V value) {
        final TSet<Entry<K, V>> entries = singleton(new TAbstractMap.SimpleImmutableEntry<>(key, value));
        return new TAbstractMap<>() {
            @Override public TSet<Entry<K, V>> entrySet() {
                return entries;
            }
        };
    }

    public static <T> TList<T> nCopies(final int n, final T o) {
        if (n < 0) {
            throw new IllegalArgumentException();
        }
        return new TAbstractList<>() {
            @Override public T get(int index) {
                if (index < 0 || index >= n) {
                    throw new TIndexOutOfBoundsException();
                }
                return o;
            }
            @Override public int size() {
                return n;
            }
        };
    }

    public static void swap(TList<?> list, int i, int j) {
        @SuppressWarnings("unchecked")
        TList<Object> objList = (TList<Object>) list;
        Object tmp = objList.get(i);
        objList.set(i, objList.get(j));
        objList.set(j, tmp);
    }

    public static <T> void sort(TList<T> list, TComparator<? super T> c) {
        if (c == null) {
            c = TComparator.NaturalOrder.instance();
        }
        @SuppressWarnings("unchecked")
        T[] array = (T[]) new Object[list.size()];
        list.toArray(array);
        TArrays.sort(array, c);
        for (int i = 0; i < array.length; ++i) {
            list.set(i, array[i]);
        }
    }

    public static <T extends TComparable<? super T>> void sort(TList<T> list) {
        sort(list, TComparator.NaturalOrder.instance());
    }

    @SuppressWarnings("unchecked")
    public static void reverse(TList<?> list) {
        if (list instanceof RandomAccess) {
            reverse(list, 0, list.size());
        } else {
            TList<Object> randomAccess = new TArrayList<>(list);
            reverse(randomAccess, 0, randomAccess.size());
            list.clear();
            ((TList<Object>) list).addAll(randomAccess);
        }
    }

    public static <T> int binarySearch(TList<? extends TComparable<? super T>> list, T key) {
        return binarySearch(list, key, TComparator.NaturalOrder.instance());
    }

    public static <T> int binarySearch(TList<? extends T> list, T key, TComparator<? super T> c) {
        if (!(list instanceof TRandomAccess)) {
            list = new TArrayList<>(list);
        }
        if (c == null) {
            c = TComparator.NaturalOrder.instance();
        }
        int l = 0;
        int u = list.size() - 1;
        if (u < 0) {
            return -1;
        }
        while (true) {
            int i = (l + u) / 2;
            T e = list.get(i);
            int cmp = c.compare(e, key);
            if (cmp == 0) {
                return i;
            } else if (cmp > 0) {
                u = i - 1;
                if (u < l) {
                    return -i - 1;
                }
            } else {
                l = i + 1;
                if (l > u) {
                    return -i - 2;
                }
            }
        }
    }

    public static void shuffle(TList<?> list) {
        shuffle(list, new TRandom());
    }

    public static void shuffle(TList<?> list, TRandom rnd) {
        shuffle(list, (TRandomGenerator) rnd);
    }

    @SuppressWarnings("unchecked")
    public static void shuffle(TList<?> list, TRandomGenerator rnd) {
        if (list instanceof TRandomAccess) {
            shuffleRandomAccess(list, rnd);
        } else {
            TList<Object> randomAccess = new TArrayList<>(list);
            shuffleRandomAccess(randomAccess, rnd);
            list.clear();
            ((TList<Object>) list).addAll(randomAccess);
        }
    }

    private static void shuffleRandomAccess(TList<?> list, TRandomGenerator rnd) {
        for (int i = list.size() - 1; i > 0; --i) {
            int j = rnd.nextInt(i + 1);
            swap(list, i, j);
        }
    }

    public static <T> void fill(TList<? super T> list, T obj) {
        if (list instanceof RandomAccess) {
            for (int i = 0; i < list.size(); ++i) {
                list.set(i, obj);
            }
        } else {
            for (TListIterator<? super T> iter = list.listIterator(); iter.hasNext();) {
                iter.next();
                iter.set(obj);
            }
        }
    }

    public static <T> void copy(TList<? super T> dest, TList<? extends T> src) {
        if (src.size() > dest.size()) {
            throw new IndexOutOfBoundsException();
        }
        if (src instanceof RandomAccess && dest instanceof RandomAccess) {
            for (int i = 0; i < src.size(); ++i) {
                dest.set(i, src.get(i));
            }
        } else {
            TListIterator<? extends T> srcIter = src.listIterator();
            TListIterator<? super T> destIter = dest.listIterator();
            while (srcIter.hasNext()) {
                destIter.next();
                destIter.set(srcIter.next());
            }
        }
    }

    public static <T extends Object & TComparable<? super T>> T min(TCollection<? extends T> coll) {
        return min(coll, TComparator.NaturalOrder.instance());
    }

    public static <T> T min(TCollection<? extends T> coll, TComparator<? super T> comp) {
        if (comp == null) {
            comp = TComparator.NaturalOrder.instance();
        }
        TIterator<? extends T> iter = coll.iterator();
        T min = iter.next();
        while (iter.hasNext()) {
            T elem = iter.next();
            if (comp.compare(elem, min) < 0) {
                min = elem;
            }
        }
        return min;
    }

    public static <T extends Object & TComparable<? super T>> T max(TCollection<? extends T> coll) {
        return max(coll, TComparator.NaturalOrder.instance());
    }

    public static <T> T max(TCollection<? extends T> coll, TComparator<? super T> comp) {
        if (comp == null) {
            comp = TComparator.NaturalOrder.instance();
        }
        TIterator<? extends T> iter = coll.iterator();
        T max = iter.next();
        while (iter.hasNext()) {
            T elem = iter.next();
            if (comp.compare(elem, max) > 0) {
                max = elem;
            }
        }
        return max;
    }

    @SuppressWarnings("unchecked")
    public static void rotate(TList<?> list, int distance) {
        if (list instanceof TRandomAccess) {
            rotateRandomAccess(list, distance);
        } else {
            TList<Object> randomAccess = new TArrayList<>(list);
            rotateRandomAccess(randomAccess, distance);
            list.clear();
            ((TList<Object>) list).addAll(randomAccess);
        }
    }

    private static void rotateRandomAccess(TList<?> list, int distance) {
        distance %= list.size();
        if (distance < 0) {
            distance += list.size();
        }
        if (distance == 0) {
            return;
        }
        reverse(list, 0, list.size());
        reverse(list, 0, distance);
        reverse(list, distance, list.size());
    }

    private static void reverse(TList<?> list, int from, int to) {
        @SuppressWarnings("unchecked")
        TList<Object> safeList = (TList<Object>) list;
        int half = (from + to) / 2;
        int j = to - 1;
        for (int i = from; i < half; ++i, --j) {
            Object tmp = safeList.get(i);
            safeList.set(i, safeList.get(j));
            safeList.set(j, tmp);
        }
    }

    public static <T> boolean replaceAll(TList<T> list, T oldVal, T newVal) {
        TListIterator<T> iter = list.listIterator();
        boolean hasValue = false;
        while (iter.hasNext()) {
            if (TObjects.equals(iter.next(), oldVal)) {
                iter.set(newVal);
                hasValue = true;
            }
        }
        return hasValue;
    }

    public static int indexOfSubList(TList<?> source, TList<?> target) {
        int sz = source.size() - target.size();
        if (sz < 0) {
            return -1;
        }
        if (source instanceof RandomAccess) {
            source = new TArrayList<>(source);
        }
        outer: for (int i = 0; i <= sz; ++i) {
            int j = i;
            for (TIterator<?> iter = target.iterator(); iter.hasNext();) {
                if (!iter.next().equals(source.get(j++))) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public static int lastIndexOfSubList(TList<?> source, TList<?> target) {
        int start = source.size() - target.size();
        if (start < 0) {
            return -1;
        }
        if (source instanceof RandomAccess) {
            source = new TArrayList<>(source);
        }
        outer: for (int i = start; i >= 0; --i) {
            int j = i;
            for (TIterator<?> iter = target.iterator(); iter.hasNext();) {
                if (!iter.next().equals(source.get(j++))) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public static <T> TList<T> unmodifiableList(final TList<? extends T> list) {
        TObjects.requireNonNull(list);
        return new TAbstractList<>() {
            @Override public T get(int index) {
                return list.get(index);
            }
            @Override public int size() {
                return list.size();
            }
            @Override public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }
        };
    }

    public static <T> TCollection<T> unmodifiableCollection(final TCollection<? extends T> c) {
        TObjects.requireNonNull(c);
        return new TAbstractCollection<>() {
            @Override public TIterator<T> iterator() {
                return unmodifiableIterator(c.iterator());
            }
            @Override public int size() {
                return c.size();
            }
            @Override public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static <T> TIterator<T> unmodifiableIterator(final TIterator<? extends T> c) {
        return new TIterator<>() {
            @Override public boolean hasNext() {
                return c.hasNext();
            }
            @Override public T next() {
                return c.next();
            }
            @Override public void remove() {
                throw new TUnsupportedOperationException();
            }
        };
    }

    public static <T> TSet<T> unmodifiableSet(final TSet<? extends T> s) {
        TObjects.requireNonNull(s);
        return new TAbstractSet<>() {
            @Override public TIterator<T> iterator() {
                return unmodifiableIterator(s.iterator());
            }
            @Override public int size() {
                return s.size();
            }
            @Override public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }
            @Override public boolean contains(Object o) {
                return s.contains(o);
            }
            @Override public boolean containsAll(TCollection<?> c) {
                return s.containsAll(c);
            }
            @Override public boolean isEmpty() {
                return s.isEmpty();
            }
        };
    }

    public static <K, V> TMap<K, V> unmodifiableMap(final TMap<? extends K, ? extends V> m) {
        TObjects.requireNonNull(m);
        return new TAbstractMap<>() {
            private TSet<Entry<K, V>> entrySetCache;

            @Override public TSet<Entry<K, V>> entrySet() {
                if (entrySetCache == null) {
                    entrySetCache = unmodifiableMapEntrySet(m.entrySet());
                }
                return entrySetCache;
            }
            @Override public V remove(Object o) {
                throw new UnsupportedOperationException();
            }
            @Override public boolean containsKey(Object key) {
                return m.containsKey(key);
            }
            @Override public boolean containsValue(Object value) {
                return m.containsValue(value);
            }
            @Override public int size() {
                return m.size();
            }
            @Override public V get(Object key) {
                return m.get(key);
            }
            @Override public boolean isEmpty() {
                return m.isEmpty();
            }
        };
    }

    private static <K, V> TSet<Entry<K, V>> unmodifiableMapEntrySet(
            final TSet<? extends Entry<? extends K, ? extends V>> c) {
        return new TAbstractSet<>() {
            @Override public int size() {
                return c.size();
            }
            @Override public TIterator<Entry<K, V>> iterator() {
                return unmodifiableMapEntryIterator(c.iterator());
            }
            @Override public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static <K, V> TIterator<Entry<K, V>> unmodifiableMapEntryIterator(
            final TIterator<? extends Entry<? extends K, ? extends V>> c) {
        return new TIterator<>() {
            @Override public boolean hasNext() {
                return c.hasNext();
            }
            @Override public Entry<K, V> next() {
                return new TAbstractMap.SimpleImmutableEntry<>(c.next());
            }
            @Override public void remove() {
                throw new TUnsupportedOperationException();
            }
        };
    }

    // The sorted and navigable views. Each view delegates reads and throws UnsupportedOperationException for
    // writes, as in the JDK; a view of a part of the collection (subSet, headMap, descendingSet, ...) is
    // unmodifiable too.

    public static <T> TSortedSet<T> unmodifiableSortedSet(TSortedSet<T> s) {
        return new UnmodifiableSortedSet<>(TObjects.requireNonNull(s));
    }

    public static <T> TNavigableSet<T> unmodifiableNavigableSet(TNavigableSet<T> s) {
        return new UnmodifiableNavigableSet<>(TObjects.requireNonNull(s));
    }

    public static <K, V> TSortedMap<K, V> unmodifiableSortedMap(TSortedMap<K, ? extends V> m) {
        return new UnmodifiableSortedMap<>(TObjects.requireNonNull(m));
    }

    public static <K, V> TNavigableMap<K, V> unmodifiableNavigableMap(TNavigableMap<K, ? extends V> m) {
        return new UnmodifiableNavigableMap<>(TObjects.requireNonNull(m));
    }

    public static <E> TSortedSet<E> emptySortedSet() {
        return emptyNavigableSet();
    }

    public static <E> TNavigableSet<E> emptyNavigableSet() {
        return new UnmodifiableNavigableSet<>(new TTreeSet<>());
    }

    public static <K, V> TSortedMap<K, V> emptySortedMap() {
        return emptyNavigableMap();
    }

    public static <K, V> TNavigableMap<K, V> emptyNavigableMap() {
        return new UnmodifiableNavigableMap<>(new TTreeMap<>());
    }

    public static <T> TSortedSet<T> synchronizedSortedSet(TSortedSet<T> s) {
        return s;
    }

    public static <T> TNavigableSet<T> synchronizedNavigableSet(TNavigableSet<T> s) {
        return s;
    }

    public static <K, V> TSortedMap<K, V> synchronizedSortedMap(TSortedMap<K, V> m) {
        return m;
    }

    public static <K, V> TNavigableMap<K, V> synchronizedNavigableMap(TNavigableMap<K, V> m) {
        return m;
    }

    static class UnmodifiableSortedSet<E> extends TTemplateCollections.AbstractImmutableSet<E>
            implements TSortedSet<E> {
        private final TSortedSet<E> s;

        UnmodifiableSortedSet(TSortedSet<E> s) {
            this.s = s;
        }

        @Override public TIterator<E> iterator() {
            return unmodifiableIterator(s.iterator());
        }
        @Override public int size() {
            return s.size();
        }
        @Override public boolean isEmpty() {
            return s.isEmpty();
        }
        @Override public boolean contains(Object o) {
            return s.contains(o);
        }
        @Override public boolean containsAll(TCollection<?> c) {
            return s.containsAll(c);
        }
        @Override public boolean add(E e) {
            throw new TUnsupportedOperationException();
        }
        @Override public boolean addAll(TCollection<? extends E> c) {
            throw new TUnsupportedOperationException();
        }
        @Override public boolean equals(Object o) {
            return o == this || s.equals(o);
        }
        @Override public int hashCode() {
            return s.hashCode();
        }
        @Override public TComparator<? super E> comparator() {
            return s.comparator();
        }
        @Override public TSortedSet<E> subSet(E fromElement, E toElement) {
            return new UnmodifiableSortedSet<>(s.subSet(fromElement, toElement));
        }
        @Override public TSortedSet<E> headSet(E toElement) {
            return new UnmodifiableSortedSet<>(s.headSet(toElement));
        }
        @Override public TSortedSet<E> tailSet(E fromElement) {
            return new UnmodifiableSortedSet<>(s.tailSet(fromElement));
        }
        @Override public E first() {
            return s.first();
        }
        @Override public E last() {
            return s.last();
        }
        @Override public E removeFirst() {
            throw new TUnsupportedOperationException();
        }
        @Override public E removeLast() {
            throw new TUnsupportedOperationException();
        }
        @Override public TSortedSet<E> reversed() {
            return new UnmodifiableSortedSet<>(s.reversed());
        }
    }

    static class UnmodifiableNavigableSet<E> extends UnmodifiableSortedSet<E> implements TNavigableSet<E> {
        private final TNavigableSet<E> ns;

        UnmodifiableNavigableSet(TNavigableSet<E> ns) {
            super(ns);
            this.ns = ns;
        }

        @Override public E lower(E e) {
            return ns.lower(e);
        }
        @Override public E floor(E e) {
            return ns.floor(e);
        }
        @Override public E ceiling(E e) {
            return ns.ceiling(e);
        }
        @Override public E higher(E e) {
            return ns.higher(e);
        }
        @Override public E pollFirst() {
            throw new TUnsupportedOperationException();
        }
        @Override public E pollLast() {
            throw new TUnsupportedOperationException();
        }
        @Override public TNavigableSet<E> descendingSet() {
            return new UnmodifiableNavigableSet<>(ns.descendingSet());
        }
        @Override public TIterator<E> descendingIterator() {
            return unmodifiableIterator(ns.descendingIterator());
        }
        @Override public TNavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement,
                boolean toInclusive) {
            return new UnmodifiableNavigableSet<>(ns.subSet(fromElement, fromInclusive, toElement, toInclusive));
        }
        @Override public TNavigableSet<E> headSet(E toElement, boolean inclusive) {
            return new UnmodifiableNavigableSet<>(ns.headSet(toElement, inclusive));
        }
        @Override public TNavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return new UnmodifiableNavigableSet<>(ns.tailSet(fromElement, inclusive));
        }
        @Override public TNavigableSet<E> reversed() {
            return descendingSet();
        }
    }

    static class UnmodifiableSequencedCollection<E> extends TAbstractCollection<E>
            implements TSequencedCollection<E> {
        final TSequencedCollection<? extends E> c;

        UnmodifiableSequencedCollection(TSequencedCollection<? extends E> c) {
            this.c = c;
        }

        @Override public TIterator<E> iterator() {
            return unmodifiableIterator(c.iterator());
        }
        @Override public int size() {
            return c.size();
        }
        @Override public boolean isEmpty() {
            return c.isEmpty();
        }
        @Override public boolean contains(Object o) {
            return c.contains(o);
        }
        @Override public boolean add(E e) {
            throw new TUnsupportedOperationException();
        }
        @Override public boolean remove(Object o) {
            throw new TUnsupportedOperationException();
        }
        @Override public void clear() {
            throw new TUnsupportedOperationException();
        }
        @Override public E removeFirst() {
            throw new TUnsupportedOperationException();
        }
        @Override public E removeLast() {
            throw new TUnsupportedOperationException();
        }
        @Override public TSequencedCollection<E> reversed() {
            return new UnmodifiableSequencedCollection<>(c.reversed());
        }
    }

    static class UnmodifiableSequencedSet<E> extends UnmodifiableSequencedCollection<E> implements TSequencedSet<E> {
        UnmodifiableSequencedSet(TSequencedSet<? extends E> s) {
            super(s);
        }

        @Override public boolean equals(Object o) {
            return o == this || c.equals(o);
        }
        @Override public int hashCode() {
            return c.hashCode();
        }
        @Override public TSequencedSet<E> reversed() {
            return new UnmodifiableSequencedSet<>((TSequencedSet<? extends E>) c.reversed());
        }
    }

    /** The entries of an unmodifiable map: each entry's setValue throws. */
    static class UnmodifiableSequencedEntrySet<K, V> extends UnmodifiableSequencedSet<Entry<K, V>> {
        private final TSequencedSet<Entry<K, V>> entries;

        UnmodifiableSequencedEntrySet(TSequencedSet<Entry<K, V>> entries) {
            super(entries);
            this.entries = entries;
        }

        @Override public TIterator<Entry<K, V>> iterator() {
            return unmodifiableMapEntryIterator(entries.iterator());
        }
        @Override public TSequencedSet<Entry<K, V>> reversed() {
            return new UnmodifiableSequencedEntrySet<>(entries.reversed());
        }
    }

    static class UnmodifiableSortedMap<K, V> extends TTemplateCollections.AbstractImmutableMap<K, V>
            implements TSortedMap<K, V> {
        private final TSortedMap<K, ? extends V> m;
        private TSet<Entry<K, V>> entrySetCache;

        UnmodifiableSortedMap(TSortedMap<K, ? extends V> m) {
            this.m = m;
        }

        @Override public TSet<Entry<K, V>> entrySet() {
            if (entrySetCache == null) {
                entrySetCache = unmodifiableMapEntrySet(m.entrySet());
            }
            return entrySetCache;
        }
        @Override public int size() {
            return m.size();
        }
        @Override public boolean isEmpty() {
            return m.isEmpty();
        }
        @Override public boolean containsKey(Object key) {
            return m.containsKey(key);
        }
        @Override public boolean containsValue(Object value) {
            return m.containsValue(value);
        }
        @Override public V get(Object key) {
            return m.get(key);
        }
        @Override public boolean equals(Object o) {
            return o == this || m.equals(o);
        }
        @Override public int hashCode() {
            return m.hashCode();
        }
        @Override public TComparator<? super K> comparator() {
            return m.comparator();
        }
        @Override public TSortedMap<K, V> subMap(K fromKey, K toKey) {
            return new UnmodifiableSortedMap<>(m.subMap(fromKey, toKey));
        }
        @Override public TSortedMap<K, V> headMap(K toKey) {
            return new UnmodifiableSortedMap<>(m.headMap(toKey));
        }
        @Override public TSortedMap<K, V> tailMap(K fromKey) {
            return new UnmodifiableSortedMap<>(m.tailMap(fromKey));
        }
        @Override public K firstKey() {
            return m.firstKey();
        }
        @Override public K lastKey() {
            return m.lastKey();
        }
        @Override public Entry<K, V> pollFirstEntry() {
            throw new TUnsupportedOperationException();
        }
        @Override public Entry<K, V> pollLastEntry() {
            throw new TUnsupportedOperationException();
        }
        @Override public TSequencedSet<K> sequencedKeySet() {
            return new UnmodifiableSequencedSet<>(m.sequencedKeySet());
        }
        @Override public TSequencedCollection<V> sequencedValues() {
            return new UnmodifiableSequencedCollection<>(m.sequencedValues());
        }
        @Override
        @SuppressWarnings("unchecked")
        public TSequencedSet<Entry<K, V>> sequencedEntrySet() {
            return new UnmodifiableSequencedEntrySet<>((TSequencedSet<Entry<K, V>>) (TSequencedSet<?>)
                    m.sequencedEntrySet());
        }
        @Override
        @SuppressWarnings("unchecked")
        public TSequencedMap<K, V> reversed() {
            TSequencedMap<K, ? extends V> r = m.reversed();
            if (r instanceof TNavigableMap) {
                return new UnmodifiableNavigableMap<>((TNavigableMap<K, ? extends V>) r);
            }
            if (r instanceof TSortedMap) {
                return new UnmodifiableSortedMap<>((TSortedMap<K, ? extends V>) r);
            }
            // TSortedMap has no default reversed() view yet (the JDK's returns a SortedMap), so a backing map
            // whose reversed() is not sorted cannot be viewed here.
            throw new TUnsupportedOperationException();
        }
    }

    static class UnmodifiableNavigableMap<K, V> extends UnmodifiableSortedMap<K, V> implements TNavigableMap<K, V> {
        private final TNavigableMap<K, ? extends V> nm;

        UnmodifiableNavigableMap(TNavigableMap<K, ? extends V> nm) {
            super(nm);
            this.nm = nm;
        }

        private static <K, V> Entry<K, V> entry(Entry<K, ? extends V> e) {
            return e == null ? null : new TAbstractMap.SimpleImmutableEntry<>(e);
        }

        @Override public Entry<K, V> lowerEntry(K key) {
            return entry(nm.lowerEntry(key));
        }
        @Override public K lowerKey(K key) {
            return nm.lowerKey(key);
        }
        @Override public Entry<K, V> floorEntry(K key) {
            return entry(nm.floorEntry(key));
        }
        @Override public K floorKey(K key) {
            return nm.floorKey(key);
        }
        @Override public Entry<K, V> ceilingEntry(K key) {
            return entry(nm.ceilingEntry(key));
        }
        @Override public K ceilingKey(K key) {
            return nm.ceilingKey(key);
        }
        @Override public Entry<K, V> higherEntry(K key) {
            return entry(nm.higherEntry(key));
        }
        @Override public K higherKey(K key) {
            return nm.higherKey(key);
        }
        @Override public Entry<K, V> firstEntry() {
            return entry(nm.firstEntry());
        }
        @Override public Entry<K, V> lastEntry() {
            return entry(nm.lastEntry());
        }
        @Override public TNavigableMap<K, V> descendingMap() {
            return new UnmodifiableNavigableMap<>(nm.descendingMap());
        }
        @Override public TNavigableSet<K> navigableKeySet() {
            return new UnmodifiableNavigableSet<>(nm.navigableKeySet());
        }
        @Override public TNavigableSet<K> descendingKeySet() {
            return new UnmodifiableNavigableSet<>(nm.descendingKeySet());
        }
        @Override public TNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            return new UnmodifiableNavigableMap<>(nm.subMap(fromKey, fromInclusive, toKey, toInclusive));
        }
        @Override public TNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            return new UnmodifiableNavigableMap<>(nm.headMap(toKey, inclusive));
        }
        @Override public TNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            return new UnmodifiableNavigableMap<>(nm.tailMap(fromKey, inclusive));
        }
        @Override public TNavigableMap<K, V> reversed() {
            return descendingMap();
        }
    }

    public static <T> TCollection<T> synchronizedCollection(TCollection<T> c) {
        return c;
    }

    public static <T> TSet<T> synchronizedSet(TSet<T> s) {
        return s;
    }

    public static <T> TList<T> synchronizedList(TList<T> list) {
        return list;
    }

    public static <K, V> TMap<K, V> synchronizedMap(TMap<K, V> m) {
        return m;
    }

    @SuppressWarnings("unchecked")
    public static <T> TComparator<T> reverseOrder() {
        return (TComparator<T>) reverseOrder;
    }

    @SuppressWarnings("unchecked")
    private static TComparator<Object> reverseOrder = (o1, o2) -> ((TComparable<Object>) o2).compareTo(o1);

    public static <T> TComparator<T> reverseOrder(final TComparator<T> cmp) {
        if (cmp == null) {
            return reverseOrder();
        }
        // Swapping the arguments, rather than negating the result, also reverses a comparator that returns
        // Integer.MIN_VALUE.
        return (o1, o2) -> cmp.compare(o2, o1);
    }

    public static <T> TEnumeration<T> enumeration(TCollection<T> c) {
        final TIterator<T> iter = c.iterator();
        return new TEnumeration<>() {
            @Override public boolean hasMoreElements() {
                return iter.hasNext();
            }
            @Override public T nextElement() {
                return iter.next();
            }
        };
    }

    public static <T> TArrayList<T> list(TEnumeration<T> e) {
        TArrayList<T> list = new TArrayList<>();
        while (e.hasMoreElements()) {
            list.add(e.nextElement());
        }
        return list;
    }

    public static <E> TCollection<E> checkedCollection(TCollection<E> c, Class<E> type) {
        return new TCheckedCollection<>(c, type);
    }

    public static <E> TSet<E> checkedSet(TSet<E> s, Class<E> type) {
        return new TCheckedSet<>(s, type);
    }

    public static <E> TList<E> checkedList(TList<E> list, Class<E> type) {
        return new TCheckedList<>(list, type);
    }

    public static <K, V> TMap<K, V> checkedMap(TMap<K, V> m, Class<K> keyType, Class<V> valueType) {
        return new TCheckedMap<>(m, keyType, valueType);
    }

    public static int frequency(TCollection<?> c, Object o) {
        int freq = 0;
        for (TIterator<?> iter = c.iterator(); iter.hasNext();) {
            if (TObjects.equals(iter.next(), o)) {
                ++freq;
            }
        }
        return freq;
    }

    public static boolean disjoint(TCollection<?> c1, TCollection<?> c2) {
        if (c1.size() > c2.size()) {
            TCollection<?> tmp = c1;
            c1 = c2;
            c2 = tmp;
        }
        for (TIterator<?> iter = c1.iterator(); iter.hasNext();) {
            if (c2.contains(iter.next())) {
                return false;
            }
        }
        return true;
    }

    @SafeVarargs
    public static <T> boolean addAll(TCollection<? super T> c, T... elements) {
        return c.addAll(TArrays.asList(elements));
    }

    public static <E> TSet<E> newSetFromMap(TMap<E, TBoolean> map) {
        if (!map.isEmpty()) {
            throw new IllegalArgumentException();
        }
        return new TSetFromMap<>(map);
    }

    public static <E> TSequencedSet<E> newSequencedSetFromMap(TSequencedMap<E, TBoolean> map) {
        if (!map.isEmpty()) {
            throw new IllegalArgumentException();
        }
        return new TSetFromMap.SequencedSetFromMap<E>(map);
    }
}
