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

import java.util.Arrays;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TCloneable;

public class TArrayDeque<E> extends TAbstractCollection<E> implements TDeque<E>, TCloneable, TSerializable {
    private int version;
    private Object[] array;
    private int head;
    private int tail;

    public TArrayDeque() {
        this(8);
    }

    public TArrayDeque(int numElements) {
        array = new Object[numElements + 1];
    }

    public TArrayDeque(TCollection<? extends E> c) {
        if (c.isEmpty()) {
            array = new Object[8];
        } else {
            array = new Object[c.size() + 1];
            int index = 0;
            for (var it = c.iterator(); it.hasNext();) {
                array[index++] = it.next();
            }
            tail = array.length - 1;
        }
    }

    @Override
    public void addFirst(E e) {
        TObjects.requireNonNull(e);
        ensureCapacity(size() + 1);
        head = modDec(head, array.length);
        array[head] = e;
        ++version;
    }

    @Override
    public void addLast(E e) {
        TObjects.requireNonNull(e);
        ensureCapacity(size() + 1);
        array[tail] = e;
        tail = modInc(tail, array.length);
        ++version;
    }

    @Override
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    @Override
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    @Override
    public E removeFirst() {
        E value = pollFirst();
        if (value == null) {
            throw new TNoSuchElementException();
        }
        return value;
    }

    @Override
    public E removeLast() {
        E value = pollLast();
        if (value == null) {
            throw new TNoSuchElementException();
        }
        return value;
    }

    @Override
    public E pollFirst() {
        if (head == tail) {
            return null;
        }
        @SuppressWarnings("unchecked")
        E result = (E) array[head];
        array[head] = null;
        head = modInc(head, array.length);
        ++version;
        return result;
    }

    @Override
    public E pollLast() {
        if (head == tail) {
            return null;
        }
        tail = modDec(tail, array.length);
        @SuppressWarnings("unchecked")
        E result = (E) array[tail];
        array[tail] = null;
        ++version;
        return result;
    }

    @Override
    public E getFirst() {
        E result = peekFirst();
        if (result == null) {
            throw new TNoSuchElementException();
        }
        return result;
    }

    @Override
    public E getLast() {
        E result = peekLast();
        if (result == null) {
            throw new TNoSuchElementException();
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E peekFirst() {
        return !isEmpty() ? (E) array[head] : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public E peekLast() {
        return !isEmpty() ? (E) array[modDec(tail, array.length)] : null;
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        if (o == null) {
            return false;
        }
        for (var it = iterator(); it.hasNext();) {
            if (it.next().equals(o)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        if (o == null) {
            return false;
        }
        for (var it = descendingIterator(); it.hasNext();) {
            if (it.next().equals(o)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean add(E e) {
        addLast(e);
        return true;
    }

    @Override
    public boolean offer(E e) {
        return offerLast(e);
    }

    @Override
    public E remove() {
        return removeFirst();
    }

    @Override
    public E poll() {
        return pollFirst();
    }

    @Override
    public E element() {
        return getFirst();
    }

    @Override
    public E peek() {
        return peekFirst();
    }

    @Override
    public void push(E e) {
        addFirst(e);
    }

    @Override
    public E pop() {
        return removeFirst();
    }

    @Override
    public int size() {
        return tail >= head ? tail - head : array.length - head + tail;
    }

    @Override
    public boolean isEmpty() {
        return head == tail;
    }

    private boolean removeAt(int index) {
        if (head < tail) {
            if (tail - index < index - head) {
                for (int i = index + 1; i < tail; ++i) {
                    array[i - 1] = array[i];
                }
                array[--tail] = null;
                return true;
            } else {
                for (int i = index - 1; i >= head; --i) {
                    array[i + 1] = array[i];
                }
                array[head++] = null;
                return false;
            }
        } else {
            if (index >= head) {
                for (int i = index - 1; i >= head; --i) {
                    array[i + 1] = array[i];
                }
                array[head] = null;
                head = modInc(head, array.length);
                return false;
            } else {
                for (int i = index + 1; i < tail; ++i) {
                    array[i - 1] = array[i];
                }
                tail = modDec(tail, array.length);
                array[tail] = null;
                return true;
            }
        }
    }

    private static int modInc(int i, int mod) {
        return ++i == mod ? 0 : i;
    }

    private static int modDec(int i, int mod) {
        return --i == -1 ? mod - 1 : i;
    }

    @Override
    public TIterator<E> iterator() {
        return new TIterator<>() {
            private int refVersion = version;
            private int index = head;
            private int lastIndex = -1;
            private int left = size();

            @Override
            public boolean hasNext() {
                return left > 0;
            }

            @Override
            public E next() {
                if (--left < 0) {
                    throw new TNoSuchElementException();
                }
                if (version > refVersion) {
                    throw new TConcurrentModificationException();
                }
                lastIndex = index;
                @SuppressWarnings("unchecked")
                E result = (E) array[index];
                index = modInc(index, array.length);
                return result;
            }

            @Override
            public void remove() {
                if (lastIndex < 0) {
                    throw new IllegalStateException();
                }
                boolean toLeft = removeAt(lastIndex);
                if (toLeft) {
                    index = modInc(index, array.length);
                }
                lastIndex = -1;
            }
        };
    }

    @Override
    public TIterator<E> descendingIterator() {
        return new TIterator<>() {
            private int refVersion = version;
            private int index = tail;
            private int lastIndex = -1;
            private int left = size();

            @Override
            public boolean hasNext() {
                return left > 0;
            }

            @Override
            public E next() {
                if (--left < 0) {
                    throw new TNoSuchElementException();
                }
                if (version > refVersion) {
                    throw new TConcurrentModificationException();
                }
                index = modDec(index, array.length);
                lastIndex = index;
                @SuppressWarnings("unchecked")
                E result = (E) array[index];
                return result;
            }

            @Override
            public void remove() {
                if (lastIndex < 0) {
                    throw new IllegalStateException();
                }
                boolean toLeft = removeAt(lastIndex);
                if (!toLeft) {
                    index = modInc(index, array.length);
                }
                lastIndex = -1;
            }
        };
    }

    private void ensureCapacity(int capacity) {
        if (capacity < array.length) {
            return;
        }
        int newArraySize = Math.max(array.length * 2, capacity * 3 / 2 + 1);
        if (newArraySize < 1) {
            newArraySize = Integer.MAX_VALUE;
        }
        Object[] newArray = new Object[newArraySize];
        int j = 0;
        if (head <= tail) {
            for (int i = head; i < tail; ++i) {
                newArray[j++] = array[i];
            }
        } else {
            for (int i = head; i < array.length; ++i) {
                newArray[j++] = array[i];
            }
            for (int i = 0; i < tail; ++i) {
                newArray[j++] = array[i];
            }
        }
        head = 0;
        tail = j;
        array = newArray;
    }

    @Override
    public void clear() {
        Arrays.fill(array, null);
        head = tail;
    }

    @Override
    protected TArrayDeque<E> clone() {
        return new TArrayDeque<>(this);
    }
}
