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
import org.teavm.classlib.java.lang.TComparable;
import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TIllegalStateException;
import org.teavm.classlib.java.lang.TNullPointerException;

/**
 *
 * @author Alexey Andreev
 * @param <E>
 */
public class TPriorityQueue<E> extends TAbstractQueue<E> implements TSerializable {
    private Object[] data;
    private TComparator<Object> comparator;
    private TComparator<? super E> originalComparator;
    private int size;
    private int version;

    public TPriorityQueue() {
        this(1);
    }

    public TPriorityQueue(int initialCapacity) {
        this(initialCapacity, null);
    }

    public TPriorityQueue(TCollection<? extends E> c) {
        if (c instanceof TPriorityQueue) {
            initFromPriorityQueue((TPriorityQueue<? extends E>) c);
        } else if (c instanceof TSortedSet) {
            initFromSortedSet((TSortedSet<? extends E>) c);
        } else {
            data = new Object[c.size()];
            fillFromCollection(c);
            setComparator(null);
        }
    }

    public TPriorityQueue(TPriorityQueue<? extends E> c) {
        initFromPriorityQueue(c);
    }

    public TPriorityQueue(TSortedSet<? extends E> c) {
        initFromSortedSet(c);
    }

    @SuppressWarnings("unchecked")
    private void initFromSortedSet(TSortedSet<? extends E> sortedSet) {
        data = new Object[sortedSet.size()];
        setComparator((TComparator<? super E>) sortedSet.comparator());
        fillFromCollection(sortedSet);
    }

    @SuppressWarnings("unchecked")
    private void initFromPriorityQueue(TPriorityQueue<? extends E> prirityQueue) {
        data = Arrays.copyOf(prirityQueue.data, prirityQueue.size);
        size = prirityQueue.size;
        setComparator((TComparator<? super E>) prirityQueue.comparator());
    }

    private void fillFromCollection(TCollection<? extends E> c) {
        for (TIterator<? extends E> iter = c.iterator(); iter.hasNext();) {
            offer(iter.next());
        }
        version = 0;
    }

    public TPriorityQueue(int initialCapacity, TComparator<? super E> comparator) {
        if (initialCapacity < 1) {
            throw new TIllegalArgumentException();
        }
        data = new Object[initialCapacity];
        setComparator(comparator);
    }

    @SuppressWarnings("unchecked")
    private void setComparator(TComparator<? super E> comparator) {
        this.originalComparator = comparator;
        if (comparator == null) {
            comparator = new TComparator<Object>() {
                @Override public int compare(Object o1, Object o2) {
                    if (o1 instanceof TComparable) {
                        return ((TComparable<Object>) o1).compareTo(o2);
                    } else {
                        return -((TComparable<Object>) o2).compareTo(o1);
                    }
                }
            };
        }
        this.comparator = (TComparator<Object>) comparator;
    }

    public TComparator<? super E> comparator() {
        return originalComparator;
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new TNullPointerException();
        }
        ensureCapacity(size + 1);
        int current = size;
        while (current > 0) {
            int parent = (current - 1) / 2;
            if (comparator.compare(e, data[parent]) < 0) {
                data[current] = data[parent];
                current = parent;
            } else {
                break;
            }
        }
        data[current] = e;
        ++size;
        ++version;
        return true;
    }

    @Override
    public E poll() {
        if (size == 0) {
            return null;
        }
        @SuppressWarnings("unchecked")
        E elem = (E) data[0];
        removeAt(0);
        return elem;
    }

    @SuppressWarnings("unchecked")
    @Override
    public E peek() {
        if (size == 0) {
            return null;
        }
        return (E) data[0];
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public TIterator<E> iterator() {
        return new TIterator<E>() {
            private int index;
            private int knownVersion = version;
            private int removeIndex = -1;
            @Override public boolean hasNext() {
                if (version != knownVersion) {
                    throw new TConcurrentModificationException();
                }
                return index < size;
            }
            @SuppressWarnings("unchecked")@Override public E next() {
                if (version != knownVersion) {
                    throw new TConcurrentModificationException();
                }
                removeIndex = index;
                return (E) data[index++];
            }
            @Override public void remove() {
                if (version != knownVersion) {
                    throw new TConcurrentModificationException();
                }
                if (removeIndex < 0) {
                    throw new TIllegalStateException();
                }
                removeAt(removeIndex);
                removeIndex = -1;
                --index;
                knownVersion = version;
            }
        };
    }

    @Override
    public void clear() {
        for (int i = 0; i < size; ++i) {
            data[i] = null;
        }
        size = 0;
        ++version;
    }

    private void removeAt(int index) {
        ++version;
        Object item = data[size - 1];
        while (true) {
            int left = index * 2 + 1;
            int right = left + 1;
            int next;
            if (left >= size) {
                break;
            } else if (right >= size || comparator.compare(data[left], data[right]) < 0) {
                next = left;
            } else {
                next = right;
            }
            if (comparator.compare(item, data[next]) <= 0) {
                break;
            }
            data[index] = data[next];
            index = next;
        }
        data[index] = item;
        data[--size] = null;
    }

    private void ensureCapacity(int capacity) {
        if (data.length >= capacity) {
            return;
        }
        capacity = Math.max(capacity, data.length * 3 / 2);
        data = Arrays.copyOf(data, capacity);
    }
}
