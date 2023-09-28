/*
 *  Copyright 2023 ihromant.
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

import java.lang.reflect.Array;

class TReversedDeque<E> implements TDeque<E> {
    private final TDeque<E> base;

    TReversedDeque(TDeque<E> base) {
        this.base = base;
    }

    @Override
    public TDeque<E> reversed() {
        return base;
    }

    @Override
    public TIterator<E> iterator() {
        return base.descendingIterator();
    }

    @Override
    public int size() {
        return base.size();
    }

    @Override
    public boolean isEmpty() {
        return base.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return base.contains(o);
    }

    @Override
    public Object[] toArray() {
        Object[] arr = new Object[size()];
        int i = 0;
        for (var it = base.descendingIterator(); it.hasNext();) {
            arr[i++] = it.next();
        }
        return arr;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] a) {
        int size = size();
        if (a.length < size) {
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
        } else {
            for (int i = size; i < a.length; ++i) {
                a[i] = null;
            }
        }
        int i = 0;
        for (var it = base.descendingIterator(); it.hasNext();) {
            a[i++] = (T) it.next();
        }
        return a;
    }

    @Override
    public boolean add(E e) {
        base.addFirst(e);
        return true;
    }

    @Override
    public void addFirst(E e) {
        base.addLast(e);
    }

    @Override
    public void addLast(E e) {
        base.addFirst(e);
    }

    @Override
    public boolean remove(Object o) {
        for (var it = base.descendingIterator(); it.hasNext();) {
            E e = it.next();
            if (TObjects.equals(e, o)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(TCollection<?> c) {
        return base.contains(c);
    }

    @Override
    public boolean addAll(TCollection<? extends E> c) {
        boolean changed = false;
        for (var it = c.iterator(); it.hasNext();) {
            addFirst(it.next());
            changed = true;
        }
        return changed;
    }

    @Override
    public boolean removeAll(TCollection<?> c) {
        boolean changed = false;
        for (var it = base.descendingIterator(); it.hasNext();) {
            E e = it.next();
            if (c.contains(e)) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean retainAll(TCollection<?> c) {
        boolean changed = false;
        for (var it = base.descendingIterator(); it.hasNext();) {
            E e = it.next();
            if (!c.contains(e)) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void clear() {
        base.clear();
    }

    @Override
    public boolean offerFirst(E e) {
        return base.offerLast(e);
    }

    @Override
    public boolean offerLast(E e) {
        return base.offerFirst(e);
    }

    @Override
    public E pollFirst() {
        return base.pollLast();
    }

    @Override
    public E pollLast() {
        return base.pollFirst();
    }

    @Override
    public E peekFirst() {
        return base.peekLast();
    }

    @Override
    public E peekLast() {
        return base.peekFirst();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        return base.removeLastOccurrence(o);
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        return base.removeFirstOccurrence(o);
    }

    @Override
    public void push(E e) {
        base.addLast(e);
    }

    @Override
    public E pop() {
        return base.removeLast();
    }

    @Override
    public TIterator<E> descendingIterator() {
        return base.iterator();
    }

    @Override
    public boolean offer(E e) {
        return base.offerFirst(e);
    }

    @Override
    public E remove() {
        return base.removeLast();
    }

    @Override
    public E removeFirst() {
        return base.removeLast();
    }

    @Override
    public E removeLast() {
        return base.removeFirst();
    }

    @Override
    public E poll() {
        return base.pollLast();
    }

    @Override
    public E element() {
        return base.getLast();
    }

    @Override
    public E getFirst() {
        return base.getLast();
    }

    @Override
    public E getLast() {
        return base.getFirst();
    }

    @Override
    public E peek() {
        return base.peekLast();
    }
}
