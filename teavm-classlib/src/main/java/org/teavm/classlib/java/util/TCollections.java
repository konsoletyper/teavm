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

/**
 *
 * @author Alexey Andreev
 */
public class TCollections extends TObject {
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
        TList<Object> objList = (TList<Object>)list;
        Object tmp = objList.get(i);
        objList.set(i, objList.get(j));
        objList.set(j, tmp);
    }

    public static <T> void sort(TList<T> list, TComparator<? super T> c) {
        if (c == null) {
            c = naturalOrder;
        }
        @SuppressWarnings("unchecked")
        T[] array = (T[])new Object[list.size()];
        list.toArray(array);
        TArrays.sort(array, c);
        for (int i = 0; i < array.length; ++i) {
            list.set(i, array[i]);
        }
    }

    public static <T extends TComparable<? super T>> void sort(TList<T> list) {
        sort(list, naturalOrder);
    }

    public static void reverse(TList<?> list) {
        reverse(list, 0, list.size());
    }

    public static <T> int binarySearch(TList<? extends TComparable<? super T>> list, T key) {
        return binarySearch(list, key, naturalOrder);
    }

    private static TComparator<Object> naturalOrder = new TComparator<Object>() {
        @SuppressWarnings("unchecked") @Override public int compare(Object o1, Object o2) {
            return o1 != null ? ((TComparable<Object>)o1).compareTo(o2) : -((TComparable<Object>)o2).compareTo(o1);
        }
    };

    public static <T> int binarySearch(TList<? extends T> list, T key, TComparator<? super T> c) {
        if (!(list instanceof TRandomAccess)) {
            list = new TArrayList<>(list);
        }
        if (c == null) {
            c = naturalOrder;
        }
        int l = 0;
        int u = list.size() - 1;
        while (true) {
            int i = (l + u) / 2;
            T e = list.get(i);
            int cmp = c.compare(key, e);
            if (cmp == 0) {
                return i;
            } else if (cmp < 0) {
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
            shuffleRandomAccess(list, rnd);
            list.clear();
            ((TList<Object>)list).addAll(randomAccess);
        }
    }

    private static void shuffleRandomAccess(TList<?> list, TRandom rnd) {
        for (int i = list.size() - 1; i > 0; --i) {
            int j = rnd.next(i + 1);
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

    public static void rotate(TList<?> list, int distance) {
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
        TList<Object> safeList = (TList<Object>)list;
        int half = (from + to) / 2;
        int j = to - 1;
        for (int i = from; i < half; ++i, --j) {
            Object tmp = safeList.get(i);
            safeList.set(i, safeList.get(j));
            safeList.set(j, tmp);
        }
    }
}
