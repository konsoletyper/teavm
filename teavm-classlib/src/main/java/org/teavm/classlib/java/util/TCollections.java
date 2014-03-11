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
        @SuppressWarnings("unchecked")
        T[] array = (T[])new Object[list.size()];
        list.toArray(array);
        TArrays.sort(array, c);
        for (int i = 0; i < array.length; ++i) {
            list.set(i, array[i]);
        }
    }

    public static <T extends TComparable<? super T>> void sort(TList<T> list) {
        sort(list, new TComparator<T>() {
            @Override public int compare(T o1, T o2) {
                return o1 != null ? o1.compareTo(o2) : -o2.compareTo(o1);
            }
        });
    }

    public static void reverse(TList<?> list) {
        @SuppressWarnings("unchecked")
        TList<Object> safeList = (TList<Object>)list;
        int half = safeList.size() / 2;
        for (int i = 0; i < half; ++i) {
            Object tmp = safeList.get(i);
            safeList.set(i, safeList.get(safeList.size() - i - 1));
            safeList.set(safeList.size() - i - 1, tmp);
        }
    }
}
