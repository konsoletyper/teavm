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
import org.teavm.interop.Rename;

/**
 *
 * @author Alexey Andreev
 * @param <E>
 */
public abstract class TAbstractList<E> extends TAbstractCollection<E> implements TList<E> {
    protected transient int modCount;

    protected TAbstractList() {
    }

    @Override
    public boolean add(E e) {
        add(size(), e);
        return true;
    }

    @Override
    public TIterator<E> iterator() {
        return new TIterator<E>() {
            private int index;
            private int modCount = TAbstractList.this.modCount;
            private int size = size();
            private int removeIndex = -1;
            @Override public boolean hasNext() {
                return index < size;
            }
            @Override public E next() {
                checkConcurrentModification();
                removeIndex = index;
                return get(index++);
            }
            @Override public void remove() {
                if (removeIndex < 0) {
                    throw new TIllegalStateException();
                }
                checkConcurrentModification();
                TAbstractList.this.remove(removeIndex);
                modCount = TAbstractList.this.modCount;
                if (removeIndex < index) {
                    --index;
                }
                --size;
                removeIndex = -1;
            }
            private void checkConcurrentModification() {
                if (modCount < TAbstractList.this.modCount) {
                    throw new TConcurrentModificationException();
                }
            }
        };
    }

    @Override
    public boolean addAll(int index, TCollection<? extends E> c) {
        if (index < 0 || index > size()) {
            throw new TIllegalArgumentException();
        }
        if (c.isEmpty()) {
            return false;
        }
        for (TIterator<? extends E> iter = c.iterator(); iter.hasNext();) {
            add(index++, iter.next());
        }
        return true;
    }

    @Override
    public E set(int index, E element) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public void add(int index, E element) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public E remove(int index) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public int indexOf(Object o) {
        int sz = size();
        for (int i = 0; i < sz; ++i) {
            Object e = get(i);
            if (o == null ? e == null : o.equals(e)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int sz = size();
        for (int i = sz - 1; i >= 0; --i) {
            Object e = get(i);
            if (o == null ? e == null : o.equals(e)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void clear() {
        removeRange(0, size());
    }

    @Override
    public TListIterator<E> listIterator() {
        return listIterator(0);
    }

    @Override
    public TListIterator<E> listIterator(int index) {
        return new TListIteratorImpl(index, modCount, size());
    }

    @Override
    public TList<E> subList(int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new TIllegalArgumentException();
        }
        if (fromIndex < 0 || toIndex > size()) {
            throw new TIndexOutOfBoundsException();
        }
        if (this instanceof TRandomAccess) {
            return new SubAbstractListRandomAccess<>(this, fromIndex, toIndex);
        }
        return new SubAbstractList<>(this, fromIndex, toIndex);

    }

    protected void removeRange(int start, int end) {
        for (int i = start; i < end; i++) {
            remove(i);
        }
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (TIterator<? extends E> iter = iterator(); iter.hasNext();) {
            E elem = iter.next();
            hashCode = 31 * hashCode + (elem != null ? elem.hashCode() : 0);
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return equals0((TObject) obj);
    }

    @Override
    @Rename("equals")
    public boolean equals0(TObject other) {
        if (!(other instanceof TList)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        TList<Object> list = (TList<Object>) other;
        if (size() != list.size()) {
            return false;
        }
        for (int i = 0; i < list.size(); ++i) {
            if (!TObjects.equals(get(i), list.get(i))) {
                return false;
            }
        }
        return true;
    }

    private class TListIteratorImpl implements TListIterator<E> {
        private int i;
        private int j;
        private int lastModCount;
        private int sz;
        public TListIteratorImpl(int i, int lastModCount, int sz) {
            this.i = i;
            this.j = i;
            this.lastModCount = lastModCount;
            this.sz = sz;
        }
        @Override public boolean hasNext() {
            return i < sz;
        }
        @Override public E next() {
            checkConcurrentModification();
            if (i == sz) {
                throw new TNoSuchElementException();
            }
            j = i;
            return get(i++);
        }
        @Override public void remove() {
            if (j == -1) {
                throw new TIllegalStateException();
            }
            checkConcurrentModification();
            TAbstractList.this.remove(j);
            if (j < i) {
                --i;
            }
            --sz;
            lastModCount = modCount;
        }
        @Override public boolean hasPrevious() {
            return i > 0;
        }
        @Override public E previous() {
            checkConcurrentModification();
            j = i - 1;
            if (j < 0) {
                throw new TNoSuchElementException();
            }
            return get((i--) - 1);
        }
        @Override public int nextIndex() {
            return i;
        }
        @Override public int previousIndex() {
            return i - 1;
        }
        @Override public void set(E e) {
            if (j == -1) {
                throw new TIllegalStateException();
            }
            checkConcurrentModification();
            TAbstractList.this.set(j, e);
        }
        @Override public void add(E e) {
            TAbstractList.this.add(i++, e);
            lastModCount = modCount;
            j = -1;
        }
        private void checkConcurrentModification() {
            if (lastModCount < modCount) {
                throw new TConcurrentModificationException();
            }
        }
    }

    private static final class SubAbstractListRandomAccess<E> extends SubAbstractList<E> implements TRandomAccess {
        SubAbstractListRandomAccess(TAbstractList<E> list, int start, int end) {
            super(list, start, end);
        }
    }

    private static class SubAbstractList<E> extends TAbstractList<E> {
        private final TAbstractList<E> fullList;
        private int offset;
        private int size;

        private static final class SubAbstractListIterator<E> implements TListIterator<E> {
            private final SubAbstractList<E> subList;
            private final TListIterator<E> iterator;
            private int start;
            private int end;

            SubAbstractListIterator(TListIterator<E> it, SubAbstractList<E> list, int offset, int length) {
                super();
                iterator = it;
                subList = list;
                start = offset;
                end = start + length;
            }

            @Override
            public void add(E object) {
                iterator.add(object);
                subList.sizeChanged(true);
                end++;
            }

            @Override
            public boolean hasNext() {
                return iterator.nextIndex() < end;
            }

            @Override
            public boolean hasPrevious() {
                return iterator.previousIndex() >= start;
            }

            @Override
            public E next() {
                if (iterator.nextIndex() < end) {
                    return iterator.next();
                }
                throw new TNoSuchElementException();
            }

            @Override
            public int nextIndex() {
                return iterator.nextIndex() - start;
            }

            @Override
            public E previous() {
                if (iterator.previousIndex() >= start) {
                    return iterator.previous();
                }
                throw new TNoSuchElementException();
            }

            @Override
            public int previousIndex() {
                int previous = iterator.previousIndex();
                if (previous >= start) {
                    return previous - start;
                }
                return -1;
            }

            @Override
            public void remove() {
                iterator.remove();
                subList.sizeChanged(false);
                end--;
            }

            @Override
            public void set(E object) {
                iterator.set(object);
            }
        }

        SubAbstractList(TAbstractList<E> list, int start, int end) {
            super();
            fullList = list;
            modCount = fullList.modCount;
            offset = start;
            size = end - start;
        }

        @Override
        public void add(int location, E object) {
            if (modCount == fullList.modCount) {
                if (0 <= location && location <= size) {
                    fullList.add(location + offset, object);
                    size++;
                    modCount = fullList.modCount;
                } else {
                    throw new TIndexOutOfBoundsException();
                }
            } else {
                throw new TConcurrentModificationException();
            }
        }

        @Override
        public boolean addAll(int location, TCollection<? extends E> collection) {
            if (modCount == fullList.modCount) {
                if (0 <= location && location <= size) {
                    boolean result = fullList.addAll(location + offset, collection);
                    if (result) {
                        size += collection.size();
                        modCount = fullList.modCount;
                    }
                    return result;
                }
                throw new TIndexOutOfBoundsException();
            }
            throw new TConcurrentModificationException();
        }

        @Override
        public boolean addAll(TCollection<? extends E> collection) {
            if (modCount == fullList.modCount) {
                boolean result = fullList.addAll(offset + size, collection);
                if (result) {
                    size += collection.size();
                    modCount = fullList.modCount;
                }
                return result;
            }
            throw new TConcurrentModificationException();
        }

        @Override
        public E get(int location) {
            if (modCount == fullList.modCount) {
                if (0 <= location && location < size) {
                    return fullList.get(location + offset);
                }
                throw new IndexOutOfBoundsException();
            }
            throw new TConcurrentModificationException();
        }

        @Override
        public TIterator<E> iterator() {
            return listIterator(0);
        }

        @Override
        public TListIterator<E> listIterator(int location) {
            if (modCount == fullList.modCount) {
                if (0 <= location && location <= size) {
                    return new SubAbstractListIterator<>(fullList.listIterator(location + offset), this, offset, size);
                }
                throw new TIndexOutOfBoundsException();
            }
            throw new TConcurrentModificationException();
        }

        @Override
        public E remove(int location) {
            if (modCount == fullList.modCount) {
                if (0 <= location && location < size) {
                    E result = fullList.remove(location + offset);
                    size--;
                    modCount = fullList.modCount;
                    return result;
                }
                throw new IndexOutOfBoundsException();
            }
            throw new TConcurrentModificationException();
        }

        @Override
        protected void removeRange(int start, int end) {
            if (start != end) {
                if (modCount == fullList.modCount) {
                    fullList.removeRange(start + offset, end + offset);
                    size -= end - start;
                    modCount = fullList.modCount;
                } else {
                    throw new TConcurrentModificationException();
                }
            }
        }

        @Override
        public E set(int location, E object) {
            if (modCount == fullList.modCount) {
                if (0 <= location && location < size) {
                    return fullList.set(location + offset, object);
                }
                throw new TIndexOutOfBoundsException();
            }
            throw new TConcurrentModificationException();
        }

        @Override
        public int size() {
            if (modCount == fullList.modCount) {
                return size;
            }
            throw new TConcurrentModificationException();
        }

        void sizeChanged(boolean increment) {
            if (increment) {
                size++;
            } else {
                size--;
            }
            modCount = fullList.modCount;
        }
    }
}
