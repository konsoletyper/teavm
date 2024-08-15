/*
 *  Copyright 2024 Alexey Andreev.
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
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.teavm.classlib.java.util.concurrent;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import org.teavm.classlib.java.lang.TIndexOutOfBoundsException;
import org.teavm.classlib.java.util.TCollection;
import org.teavm.classlib.java.util.TIterator;
import org.teavm.classlib.java.util.TList;
import org.teavm.classlib.java.util.TListIterator;

public class TCopyOnWriteArrayList<E> implements TList<E>, RandomAccess, Cloneable, Serializable {
    private volatile E[] array;

    public TCopyOnWriteArrayList() {
        array = newElementArray(0);
    }

    @SuppressWarnings("unchecked")
    public TCopyOnWriteArrayList(Collection<? extends E> c) {
        this.array = (E[]) c.toArray();
    }

    public TCopyOnWriteArrayList(E[] array) {
        this.array = Arrays.copyOf(array, array.length);
    }

    @Override
    public boolean add(E e) {
        var copy = Arrays.copyOf(array, array.length + 1);
        copy[array.length] = e;
        array = copy;
        return true;
    }

    @Override
    public void add(int index, E e) {
        checkIndexInclusive(index, array.length);
        var copy = newElementArray(array.length + 1);
        System.arraycopy(array, 0, copy, 0, index);
        copy[index] = e;
        System.arraycopy(array, index, copy, index + 1, array.length - index);
        array = copy;
    }

    @Override
    public boolean addAll(TCollection<? extends E> c) {
        return addAll(size(), c);
    }

    @Override
    public boolean addAll(int index, TCollection<? extends E> c) {
        checkIndexInclusive(index, array.length);
        if (c.isEmpty()) {
            return false;
        }
        var copy = newElementArray(array.length + c.size());
        System.arraycopy(array, 0, copy, 0, index);
        var targetIndex = index;
        var iter = c.iterator();
        while (iter.hasNext()) {
            copy[targetIndex++] = iter.next();
        }
        System.arraycopy(array, index, copy, targetIndex, array.length - index);
        array = copy;
        return true;
    }

    public int addAllAbsent(Collection<? extends E> c) {
        if (c.isEmpty()) {
            return 0;
        }

        var currentArray = array;
        int count;
        var toAdd = newElementArray(c.size());
        repeat: do {
            count = 0;
            for (var o : c) {
                if (indexOf(o) < 0) {
                    if (currentArray != array) {
                        currentArray = array;
                        continue repeat;
                    }
                    toAdd[count++] = o;
                }
            }
        } while (false);
        if (count > 0) {
            var copy = newElementArray(array.length + count);
            System.arraycopy(array, 0, copy, 0, array.length);
            System.arraycopy(toAdd, 0, copy, array.length, count);
            array = copy;
        }
        return count;
    }

    public boolean addIfAbsent(E e) {
        if (!isEmpty() && indexOf(e) >= 0) {
            return false;
        }
        add(e);
        return true;
    }

    @Override
    public void clear() {
        if (!isEmpty()) {
            array = newElementArray(0);
        }
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("CloneNotSupportedException is not expected here");
        }
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @Override
    public boolean containsAll(TCollection<?> c) {
        return containsAll(c, array, 0, array.length);
    }

    private static boolean containsAll(TCollection<?> c, Object[] array, int start, int size) {
        if (size == 0) {
            return false;
        }
        var iter = c.iterator();
        while (iter.hasNext()) {
            if (indexOf(iter.next(), array, start, size) < 0) {
                return false;
            }
        }
        return true;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof List)) {
            return false;
        }
        var l = (List<?>) o;
        var it = l.listIterator();
        var ourIt = listIterator();
        while (it.hasNext()) {
            if (!ourIt.hasNext()) {
                return false;
            }
            var thisListElem = it.next();
            var anotherListElem = ourIt.next();
            if (!(Objects.equals(thisListElem, anotherListElem))) {
                return false;
            }
        }
        return !ourIt.hasNext();
    }

    @Override
    public E get(int index) {
        return array[index];
    }

    public int hashCode() {
        int hashCode = 1;
        var it = listIterator();
        while (it.hasNext()) {
            var obj = it.next();
            hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
        }
        return hashCode;
    }

    public int indexOf(E e, int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException();
        }
        return indexOf(e, array, index, array.length - index);
    }

    @SuppressWarnings("unchecked")
    @Override
    public int indexOf(Object o) {
        return indexOf((E) o, 0);
    }

    @Override
    public boolean isEmpty() {
        return array.length == 0;
    }

    @Override
    public TIterator<E> iterator() {
        return listIterator();
    }

    public int lastIndexOf(E e, int index) {
        if (index >= array.length) {
            throw new IndexOutOfBoundsException();
        }
        return lastIndexOf(e, array, index, 0);
    }

    @SuppressWarnings("unchecked")
    @Override
    public int lastIndexOf(Object o) {
        return lastIndexOf((E) o, size() - 1);
    }

    @Override
    public TListIterator<E> listIterator() {
        return new ListIteratorImpl<>(array, 0);
    }

    @Override
    public TListIterator<E> listIterator(int index) {
        checkIndexInclusive(index, array.length);
        return new ListIteratorImpl<>(array, index);
    }

    @Override
    public E remove(int index) {
        return removeRange(index, 1);
    }

    @Override
    public boolean remove(Object o) {
        int index;
        E[] currentArray;
        do {
            currentArray = array;
            index = indexOf(o);
        } while (currentArray != array);
        if (index >= 0) {
            remove(index);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeAll(TCollection<?> c) {
        return removeAll(c, 0, array.length) != 0;
    }

    @Override
    public boolean retainAll(TCollection<?> c) {
        Objects.requireNonNull(c);
        return retainAll(c, 0, array.length) != 0;
    }

    @Override
    public E set(int index, E e) {
        int size = size();
        checkIndexExclusive(index, size);
        var copy = newElementArray(size);
        System.arraycopy(array, 0, copy, 0, size);
        var old = copy[index];
        copy[index] = e;
        array = copy;
        return old;
    }

    @Override
    public int size() {
        return array.length;
    }

    @Override
    public TList<E> subList(int fromIndex, int toIndex) {
        return new SubList<>(this, fromIndex, toIndex);
    }

    @Override
    public Object[] toArray() {
        return toArray(array, 0, array.length);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] a) {
        return (T[]) toArray(a, array, 0, array.length);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder("[");

        var it = listIterator();
        while (it.hasNext()) {
            sb.append(it.next());
            sb.append(", ");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("]");
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private E[] newElementArray(int size) {
        return (E[]) new Object[size];
    }

    private int removeAll(TCollection<?> c, int start, int size) {
        if (c.isEmpty() || size == 0) {
            return 0;
        }

        var currentArray = array;
        var data = newElementArray(size);
        int dataSize;
        repeat: do {
            dataSize = 0;
            for (int i = start; i < (start + size); i++) {
                if (!c.contains(array[i])) {
                    if (currentArray != array) {
                        currentArray = array;
                        continue repeat;
                    }
                    data[dataSize++] = array[i];
                }
            }
        } while (false);

        if (dataSize != size) {
            var copy = newElementArray(array.length - (size - dataSize));
            System.arraycopy(array, 0, copy, 0, start);
            System.arraycopy(data, 0, copy, start, dataSize);
            System.arraycopy(array, start + size, copy, start + dataSize, array.length
                    - (start + size));
            array = copy;
            return size - dataSize;
        }
        return 0;
    }

    private int retainAll(TCollection<?> c, int start, int size) {
        if (size == 0) {
            return 0;
        }
        if (c.isEmpty()) {
            E[] copy;
            if (size == array.length) {
                copy = newElementArray(0);
            } else {
                copy = newElementArray(array.length - size);
                System.arraycopy(array, 0, copy, 0, start);
                System.arraycopy(array, start + size, copy, start, array.length - start - size);
            }
            array = copy;
            return size;
        }

        var temp = newElementArray(size);
        int pos;
        var currentArray = array;
        repeat: do {
            pos = 0;
            for (int i = start; i < (start + size); i++) {
                if (c.contains(array[i])) {
                    if (array != currentArray) {
                        currentArray = array;
                        continue repeat;
                    }
                    temp[pos++] = array[i];
                }
            }
        } while (false);
        if (pos == size) {
            return 0;
        }

        var copy = newElementArray(pos + array.length - size);
        System.arraycopy(array, 0, copy, 0, start);
        System.arraycopy(temp, 0, copy, start, pos);
        System.arraycopy(array, start + size, copy, start + pos, array.length - start - size);
        array = copy;
        return size - pos;
    }

    private E removeRange(int start, int size) {
        int sizeArr = size();
        checkIndexExclusive(start, sizeArr);
        checkIndexInclusive(start + size, sizeArr);
        var copy = newElementArray(sizeArr - size);
        System.arraycopy(array, 0, copy, 0, start);
        var old = array[start];
        if (sizeArr > (start + size)) {
            System.arraycopy(array, start + size, copy, start, sizeArr - (start + size));
        }
        array = copy;
        return old;
    }

    private static Object[] toArray(Object[] data, int start, int size) {
        var result = new Object[size];
        System.arraycopy(data, start, result, 0, size);
        return result;
    }

    private static Object[] toArray(Object[] to, Object[] data, int start, int size) {
        int l = data.length;
        if (to.length < l) {
            to = (Object[]) Array.newInstance(to.getClass().getComponentType(), l);
        } else {
            if (to.length > l) {
                to[l] = null;
            }
        }
        System.arraycopy(data, start, to, 0, size);
        return to;
    }

    private static int lastIndexOf(Object o, Object[] data, int index, int limit) {
        if (o != null) {
            while (index >= limit) {
                if (o.equals(data[index])) {
                    return index;
                }
                --index;
            }
        } else {
            while (index >= limit) {
                if (data[index] == null) {
                    return index;
                }
                --index;
            }
        }
        return -1;
    }

    private static int indexOf(Object o, Object[] data, int start, int size) {
        if (size == 0) {
            return -1;
        }
        if (o == null) {
            for (int i = start; i < start + size; i++) {
                if (data[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = start; i < start + size; i++) {
                if (o.equals(data[i])) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static void checkIndexInclusive(int index, int size) {
        if (index < 0 || index > size) {
            throw new TIndexOutOfBoundsException();
        }
    }

    private static void checkIndexExclusive(int index, int size) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index is " + index + ", size is " + size);
        }
    }

    private static class ListIteratorImpl<E> implements TListIterator<E> {
        private final E[] arr;
        private int current;
        private final int size;

        ListIteratorImpl(E[] data, int current) {
            this.current = current;
            arr = data;
            size = data.length;
        }

        @Override
        public void add(Object o) {
            throw new UnsupportedOperationException("Unsupported operation add");
        }

        @Override
        public boolean hasNext() {
            return current < size;
        }

        @Override
        public boolean hasPrevious() {
            return current > 0;
        }

        @Override
        public E next() {
            if (hasNext()) {
                return arr[current++];
            }
            throw new NoSuchElementException("pos is " + current + ", size is " + size);
        }

        @Override
        public int nextIndex() {
            return current;
        }

        @Override
        public E previous() {
            if (hasPrevious()) {
                return arr[--current];
            }
            throw new NoSuchElementException("pos is " + (current - 1) + ", size is " + size);
        }

        @Override
        public int previousIndex() {
            return current - 1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Unsupported operation remove");
        }

        @Override
        public void set(Object o) {
            throw new UnsupportedOperationException("Unsupported operation set");
        }
    }

    private static class SubList<E> implements TList<E> {
        private final TCopyOnWriteArrayList<E> list;
        private E[] data;
        private int size;
        private final int start;

        SubList(TCopyOnWriteArrayList<E> list, int fromIdx, int toIdx) {
            this.list = list;
            checkIndexExclusive(fromIdx, list.array.length);
            checkIndexInclusive(toIdx, list.array.length);
            size = toIdx - fromIdx;
            data = list.array;
            start = fromIdx;
        }

        private void checkModifications() {
            if (data != list.array) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        public TListIterator<E> listIterator(int startIdx) {
            return new SubListIterator(startIdx, data, size);
        }

        @Override
        public E set(int index, E obj) {
            checkIndexExclusive(index, size);
            checkModifications();
            var result = list.set(index + start, obj);
            data = list.array;
            return result;
        }

        @Override
        public E get(int index) {
            checkModifications();
            checkIndexExclusive(index, size);
            return data[index + start];
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public E remove(int index) {
            checkIndexExclusive(index, size);
            checkModifications();
            var obj = list.remove(index + start);
            data = list.array;
            size--;
            return obj;
        }

        @Override
        public void add(int index, E object) {
            checkIndexInclusive(index, size);
            checkModifications();
            list.add(index + start, object);
            data = list.array;
            size++;
        }

        @Override
        public boolean add(E o) {
            checkModifications();
            list.add(start + size, o);
            data = list.array;
            size++;
            return true;
        }

        @Override
        public boolean addAll(TCollection<? extends E> c) {
            checkModifications();
            int d = list.size();
            list.addAll(start + size, c);
            data = list.array;
            size += list.size() - d;
            return true;
        }

        @Override
        public void clear() {
            checkModifications();
            list.removeRange(start, size);
            data = list.array;
            size = 0;
        }

        @Override
        public boolean contains(Object o) {
            return indexOf(o) != -1;
        }

        @Override
        public boolean containsAll(TCollection<?> c) {
            return TCopyOnWriteArrayList.containsAll(c, data, start, size);
        }

        @Override
        public int indexOf(Object o) {
            int ind = TCopyOnWriteArrayList.indexOf(o, data, start, size);
            return ind < 0 ? -1 : ind - start;
        }

        @Override
        public boolean isEmpty() {
            return size == 0;
        }

        @Override
        public TIterator<E> iterator() {
            return new SubListIterator(0, data, size);
        }

        @Override
        public int lastIndexOf(Object o) {
            int ind = TCopyOnWriteArrayList.lastIndexOf(o, data, start + size - 1, start);
            return ind < 0 ? -1 : ind - start;
        }

        @Override
        public TListIterator<E> listIterator() {
            return new SubListIterator(0, data, size);
        }

        @Override
        public boolean remove(Object o) {
            checkModifications();
            int i;
            Object[] currentArray;
            do {
                currentArray = data;
                i = indexOf(o);
            } while (currentArray != data);
            if (i == -1) {
                return false;
            }
            boolean result = list.remove(i + start) != null;
            if (result) {
                data = list.array;
                size--;
            }
            return result;
        }

        @Override
        public boolean removeAll(TCollection<?> c) {
            checkModifications();
            int removed = list.removeAll(c, start, size);
            if (removed > 0) {
                data = list.array;
                size -= removed;
                return true;
            }
            return false;
        }

        @Override
        public boolean retainAll(TCollection<?> c) {
            checkModifications();
            int removed = list.retainAll(c, start, size);
            if (removed > 0) {
                data = list.array;
                size -= removed;
                return true;
            }
            return false;
        }

        @Override
        public TList<E> subList(int fromIndex, int toIndex) {
            return new SubList<>(list, start + fromIndex, start + toIndex);
        }

        @Override
        public Object[] toArray() {
            return TCopyOnWriteArrayList.toArray(data, start, size);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T[] toArray(T[] a) {
            return (T[]) TCopyOnWriteArrayList.toArray(a, data, start, size);
        }

        @Override
        public boolean addAll(int index, TCollection<? extends E> collection) {
            checkIndexInclusive(index, size);
            checkModifications();
            int d = list.size();
            boolean rt = list.addAll(index + start, collection);
            data = list.array;
            size += list.size() - d;
            return rt;
        }

        private class SubListIterator extends ListIteratorImpl<E> {
            int size;

            private SubListIterator(int index, E[] data, int size) {
                super(data, index + start);
                this.size = size;
            }

            @Override
            public int nextIndex() {
                return super.nextIndex() - start;
            }

            @Override
            public int previousIndex() {
                return super.previousIndex() - start;
            }

            @Override
            public boolean hasNext() {
                return nextIndex() < size;
            }

            @Override
            public boolean hasPrevious() {
                return previousIndex() > -1;
            }
        }
    }
}
