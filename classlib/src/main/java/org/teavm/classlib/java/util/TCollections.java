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

public class TCollections extends TObject {
    @SuppressWarnings("rawtypes")
    public static final TSet EMPTY_SET = emptySet();

    @SuppressWarnings("rawtypes")
    public static final TMap EMPTY_MAP = emptyMap();

    @SuppressWarnings("rawtypes")
    public static final TList EMPTY_LIST = emptyList();

    public static <T> TIterator<T> emptyIterator() {
        return new TIterator<T>() {
            @Override public boolean hasNext() {
                return false;
            }
            @Override public T next() {
                throw new TNoSuchElementException();
            }
            @Override public void remove() {
                throw new TIllegalStateException();
            }
        };
    }

    public static <T> TListIterator<T> emptyListIterator() {
        return new TListIterator<T>() {
            @Override public boolean hasNext() {
                return false;
            }
            @Override public T next() {
                throw new TNoSuchElementException();
            }
            @Override public void remove() {
                throw new TIllegalStateException();
            }
            @Override public boolean hasPrevious() {
                return false;
            }
            @Override public T previous() {
                throw new TNoSuchElementException();
            }
            @Override public int nextIndex() {
                return 0;
            }
            @Override public int previousIndex() {
                return -1;
            }
            @Override public void set(T e) {
                throw new TUnsupportedOperationException();
            }
            @Override public void add(T e) {
                throw new TUnsupportedOperationException();
            }
        };
    }

    public static final <T> TList<T> emptyList() {
        return new TAbstractList<T>() {
            @Override public T get(int index) {
                throw new TIndexOutOfBoundsException();
            }
            @Override public int size() {
                return 0;
            }
        };
    }

    public static <T> TEnumeration<T> emptyEnumeration() {
        return new TEnumeration<T>() {
            @Override public boolean hasMoreElements() {
                return false;
            }
            @Override public T nextElement() {
                throw new TNoSuchElementException();
            }
        };
    }

    public static final <T> TSet<T> emptySet() {
        return new TAbstractSet<T>() {
            @Override public int size() {
                return 0;
            }
            @Override public TIterator<T> iterator() {
                return emptyIterator();
            }
        };
    }

    public static final <K, V> TMap<K, V> emptyMap() {
        return new TAbstractMap<K, V>() {
            @Override public TSet<Entry<K, V>> entrySet() {
                return emptySet();
            }
        };
    }

    public static <T> TList<T> singletonList(final T o) {
        return new TAbstractList<T>() {
            @Override public T get(int index) {
                if (index != 0) {
                    throw new TIndexOutOfBoundsException();
                }
                return o;
            }
            @Override public int size() {
                return 1;
            }
        };
    }

    public static <T> TSet<T> singleton(final T o) {
        return new TAbstractSet<T>() {
            @Override public int size() {
                return 1;
            }
            @Override public TIterator<T> iterator() {
                return new TIterator<T>() {
                    private boolean read;
                    @Override public boolean hasNext() {
                        return !read;
                    }
                    @Override public T next() {
                        if (read) {
                            throw new TNoSuchElementException();
                        }
                        read = true;
                        return o;
                    }
                    @Override public void remove() {
                        throw new TUnsupportedOperationException();
                    }
                };
            }
            @Override public boolean contains(Object o2) {
                return TObjects.equals(o, o2);
            }
        };
    }

    public static <K, V> TMap<K, V> singletonMap(final K key, final V value) {
        final TSet<Entry<K, V>> entries = singleton(new TAbstractMap.SimpleImmutableEntry<>(key, value));
        return new TAbstractMap<K, V>() {
            @Override public TSet<Entry<K, V>> entrySet() {
                return entries;
            }
        };
    }

    public static <T> TList<T> unmodifiableList(final TList<? extends T> list) {
        return new TAbstractList<T>() {
            @Override public T get(int index) {
                return list.get(index);
            }
            @Override public int size() {
                return list.size();
            }
        };
    }

    public static <T> TList<T> nCopies(final int n, final T o) {
        return new TAbstractList<T>() {
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
            c = naturalOrder;
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
        sort(list, naturalOrder);
    }

    @SuppressWarnings("unchecked")
    public static void reverse(TList<?> list) {
        if (list instanceof RandomAccess) {
            reverse(list, 0, list.size());
        } else {
            TList<Object> randomAccess = new TArrayList<>(list);
            reverse(list, 0, list.size());
            list.clear();
            ((TList<Object>) list).addAll(randomAccess);
        }
    }

    public static <T> int binarySearch(TList<? extends TComparable<? super T>> list, T key) {
        return binarySearch(list, key, naturalOrder);
    }

    @SuppressWarnings("unchecked")
    private static TComparator<Object> naturalOrder = (o1, o2) -> o1 != null
            ? ((TComparable<Object>) o1).compareTo(o2)
            : -((TComparable<Object>) o2).compareTo(o1);

    public static <T> int binarySearch(TList<? extends T> list, T key, TComparator<? super T> c) {
        if (!(list instanceof TRandomAccess)) {
            list = new TArrayList<>(list);
        }
        if (c == null) {
            c = naturalOrder;
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

    @SuppressWarnings("unchecked")
    public static void shuffle(TList<?> list, TRandom rnd) {
        if (list instanceof TRandomAccess) {
            shuffleRandomAccess(list, rnd);
        } else {
            TList<Object> randomAccess = new TArrayList<>(list);
            shuffleRandomAccess(randomAccess, rnd);
            list.clear();
            ((TList<Object>) list).addAll(randomAccess);
        }
    }

    private static void shuffleRandomAccess(TList<?> list, TRandom rnd) {
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
        return min(coll, naturalOrder);
    }

    public static <T> T min(TCollection<? extends T> coll, TComparator<? super T> comp) {
        if (comp == null) {
            comp = naturalOrder;
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
        return max(coll, naturalOrder);
    }

    public static <T> T max(TCollection<? extends T> coll, TComparator<? super T> comp) {
        if (comp == null) {
            comp = naturalOrder;
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

    public static <T> TCollection<T> unmodifiableCollection(final TCollection<? extends T> c) {
        return new TAbstractCollection<T>() {
            @Override public TIterator<T> iterator() {
                return unmodifiableIterator(c.iterator());
            }
            @Override public int size() {
                return c.size();
            }
        };
    }

    private static <T> TIterator<T> unmodifiableIterator(final TIterator<? extends T> c) {
        return new TIterator<T>() {
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
        return new TAbstractSet<T>() {
            @Override public TIterator<T> iterator() {
                return unmodifiableIterator(s.iterator());
            }
            @Override public int size() {
                return s.size();
            }
        };
    }

    public static <K, V> TMap<K, V> unmodifiableMap(final TMap<? extends K, ? extends V> m) {
        return new TAbstractMap<K, V>() {
            @Override public TSet<Entry<K, V>> entrySet() {
                return unmodifiableMapEntrySet(m.entrySet());
            }
        };
    }

    private static <K, V> TSet<Entry<K, V>> unmodifiableMapEntrySet(
            final TSet<? extends Entry<? extends K, ? extends V>> c) {
        return new TAbstractSet<Entry<K, V>>() {
            @Override public int size() {
                return c.size();
            }
            @Override public TIterator<Entry<K, V>> iterator() {
                return unmodifiableMapEntryIterator(c.iterator());
            }
        };
    }

    private static <K, V> TIterator<Entry<K, V>> unmodifiableMapEntryIterator(
            final TIterator<? extends Entry<? extends K, ? extends V>> c) {
        return new TIterator<Entry<K, V>>() {
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

    private static TComparator<Object> reverseOrder = (o1, o2) -> o1 != null
            ? -((TComparable<Object>) o1).compareTo(o2)
            : ((TComparable<Object>) o2).compareTo(o1);

    public static <T> TComparator<T> reverseOrder(final TComparator<T> cmp) {
        return (o1, o2) -> -cmp.compare(o1, o2);
    }

    public static <T> TEnumeration<T> enumeration(TCollection<T> c) {
        final TIterator<T> iter = c.iterator();
        return new TEnumeration<T>() {
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

    public static <E> TCollection<E> checkedCollection(TCollection<E> c, TClass<E> type) {
        return new TCheckedCollection<>(c, type);
    }

    public static <E> TSet<E> checkedSet(TSet<E> s, TClass<E> type) {
        return new TCheckedSet<>(s, type);
    }

    public static <E> TList<E> checkedList(TList<E> list, TClass<E> type) {
        return new TCheckedList<>(list, type);
    }

    public static <K, V> TMap<K, V> checkedMap(TMap<K, V> m, TClass<K> keyType, TClass<V> valueType) {
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
        return new TSetFromMap<>(map);
    }
}
