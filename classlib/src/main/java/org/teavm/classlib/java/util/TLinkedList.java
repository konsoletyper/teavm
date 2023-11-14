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

import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import org.teavm.classlib.java.util.function.TUnaryOperator;
import org.teavm.classlib.java.util.stream.TStream;

public class TLinkedList<E> extends TAbstractSequentialList<E> implements TDeque<E> {
    static class Entry<E> {
        E item;
        Entry<E> next;
        Entry<E> previous;
    }
    private Entry<E> firstEntry;
    private Entry<E> lastEntry;
    private int size;

    public TLinkedList() {
    }

    public TLinkedList(TCollection<E> coll) {
        TIterator<E> iter = coll.iterator();
        Entry<E> prevEntry = null;
        while (iter.hasNext()) {
            Entry<E> entry = new Entry<>();
            entry.item = iter.next();
            entry.previous = prevEntry;
            if (prevEntry == null) {
                firstEntry = entry;
            } else {
                prevEntry.next = entry;
            }
            prevEntry = entry;
            ++size;
        }
        lastEntry = prevEntry;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void clear() {
        firstEntry = null;
        lastEntry = null;
        size = 0;
        modCount++;
    }

    @Override
    public TListIterator<E> listIterator() {
        return new SequentialListIterator(firstEntry, null, 0);
    }

    @Override
    public TListIterator<E> listIterator(int index) {
        if (index < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (index <= size / 2) {
            Entry<E> next = firstEntry;
            for (int i = 0; i < index; ++i) {
                next = next.next;
            }
            return new SequentialListIterator(next, next != null ? next.previous : null, index);
        } else {
            if (index > size) {
                throw new IndexOutOfBoundsException();
            }
            Entry<E> prev = lastEntry;
            for (int i = index; i < size; ++i) {
                prev = prev.previous;
            }
            return new SequentialListIterator(prev != null ? prev.next : null, prev, index);
        }
    }

    @Override
    public boolean offer(E e) {
        addLast(e);
        return true;
    }

    @Override
    public E remove() {
        if (isEmpty()) {
            throw new TNoSuchElementException();
        }
        return poll();
    }

    @Override
    public E poll() {
        if (firstEntry == null) {
            return null;
        }
        Entry<E> entry = firstEntry;
        firstEntry = firstEntry.next;
        if (firstEntry == null) {
            lastEntry = null;
        } else {
            firstEntry.previous = null;
        }
        --size;
        ++modCount;
        return entry.item;
    }

    @Override
    public E element() {
        if (firstEntry == null) {
            throw new TNoSuchElementException();
        }
        return firstEntry.item;
    }

    @Override
    public E peek() {
        return firstEntry != null ? firstEntry.item : null;
    }

    @Override
    public void addFirst(E e) {
        Entry<E> entry = new Entry<>();
        entry.item = e;
        entry.next = firstEntry;
        if (firstEntry != null) {
            firstEntry.previous = entry;
        } else {
            lastEntry = entry;
        }
        firstEntry = entry;
        ++modCount;
        ++size;
    }

    @Override
    public void addLast(E e) {
        Entry<E> entry = new Entry<>();
        entry.item = e;
        entry.previous = lastEntry;
        if (lastEntry != null) {
            lastEntry.next = entry;
        } else {
            firstEntry = entry;
        }
        lastEntry = entry;
        ++modCount;
        ++size;
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
        return remove();
    }

    @Override
    public E removeLast() {
        if (isEmpty()) {
            throw new TNoSuchElementException();
        }
        return pollLast();
    }

    @Override
    public E pollFirst() {
        return poll();
    }

    @Override
    public E pollLast() {
        if (lastEntry == null) {
            return null;
        }
        Entry<E> entry = lastEntry;
        lastEntry = lastEntry.previous;
        if (lastEntry == null) {
            firstEntry = null;
        } else {
            lastEntry.next = null;
        }
        --size;
        ++modCount;
        return entry.item;
    }

    @Override
    public E getFirst() {
        if (firstEntry == null) {
            throw new TNoSuchElementException();
        }
        return firstEntry.item;
    }

    @Override
    public E getLast() {
        if (lastEntry == null) {
            throw new TNoSuchElementException();
        }
        return lastEntry.item;
    }

    @Override
    public E peekFirst() {
        return firstEntry != null ? firstEntry.item : null;
    }

    @Override
    public E peekLast() {
        return lastEntry != null ? lastEntry.item : null;
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        Entry<E> entry = firstEntry;
        while (entry != null) {
            if (TObjects.equals(o, entry.item)) {
                removeEntry(entry);
                return true;
            }
            entry = entry.next;
        }
        return false;
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        Entry<E> entry = lastEntry;
        while (entry != null) {
            if (TObjects.equals(o, entry.item)) {
                removeEntry(entry);
                return true;
            }
            entry = entry.previous;
        }
        return false;
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
    public TIterator<E> descendingIterator() {
        return new DescendingIterator();
    }

    @Override
    public TLinkedList<E> reversed() {
        return new ReversedLinkedList<>(this);
    }

    private void removeEntry(Entry<E> entry) {
        if (entry.previous != null) {
            entry.previous.next = entry.next;
        } else {
            firstEntry = entry.next;
        }
        if (entry.next != null) {
            entry.next.previous = entry.previous;
        } else {
            lastEntry = entry.previous;
        }
        --size;
        ++modCount;
    }

    private class SequentialListIterator implements TListIterator<E> {
        private Entry<E> nextEntry;
        private Entry<E> prevEntry;
        private Entry<E> currentEntry;
        private int index;
        private int version = modCount;

        public SequentialListIterator(Entry<E> nextEntry, Entry<E> prevEntry, int index) {
            this.nextEntry = nextEntry;
            this.prevEntry = prevEntry;
            this.index = index;
        }

        @Override
        public boolean hasNext() {
            return nextEntry != null;
        }

        @Override
        public E next() {
            checkConcurrentModification();
            if (nextEntry == null) {
                throw new TNoSuchElementException();
            }
            E result = nextEntry.item;
            currentEntry = nextEntry;
            prevEntry = nextEntry;
            nextEntry = nextEntry.next;
            ++index;
            return result;
        }

        @Override
        public void remove() {
            if (currentEntry == null) {
                throw new IllegalStateException();
            }
            removeEntry(currentEntry);
            if (currentEntry == prevEntry) {
                prevEntry = hasNext() ? nextEntry.previous : null;
                --index;
            } else if (currentEntry == nextEntry) {
                nextEntry = hasPrevious() ? prevEntry.next : null;
            }
            version = modCount;
            currentEntry = null;
        }

        @Override
        public boolean hasPrevious() {
            return prevEntry != null;
        }

        @Override
        public E previous() {
            checkConcurrentModification();
            if (prevEntry == null) {
                throw new TNoSuchElementException();
            }
            currentEntry = prevEntry;
            E result = prevEntry.item;
            nextEntry = prevEntry;
            prevEntry = prevEntry.previous;
            --index;
            return result;
        }

        @Override
        public int nextIndex() {
            return index;
        }

        @Override
        public int previousIndex() {
            return index - 1;
        }

        @Override
        public void set(E e) {
            if (currentEntry == null) {
                throw new IllegalStateException();
            }
            checkConcurrentModification();
            currentEntry.item = e;
            currentEntry = null;
        }

        @Override
        public void add(E e) {
            checkConcurrentModification();
            Entry<E> newEntry = new Entry<>();
            newEntry.item = e;
            newEntry.previous = prevEntry;
            newEntry.next = nextEntry;
            if (prevEntry != null) {
                prevEntry.next = newEntry;
            } else {
                firstEntry = newEntry;
            }
            if (nextEntry != null) {
                nextEntry.previous = newEntry;
            } else {
                lastEntry = newEntry;
            }
            prevEntry = newEntry;
            ++size;
            ++modCount;
            version = modCount;
            currentEntry = null;
        }

        private void checkConcurrentModification() {
            if (version < modCount) {
                throw new TConcurrentModificationException();
            }
        }
    }

    private class DescendingIterator implements TIterator<E> {
        private Entry<E> prevEntry = lastEntry;
        private Entry<E> currentEntry;
        private int version = modCount;

        @Override
        public boolean hasNext() {
            return prevEntry != null;
        }

        @Override
        public E next() {
            if (version < modCount) {
                throw new TConcurrentModificationException();
            }
            if (prevEntry == null) {
                throw new TNoSuchElementException();
            }
            currentEntry = prevEntry;
            prevEntry = prevEntry.previous;
            return currentEntry.item;
        }

        @Override
        public void remove() {
            if (currentEntry == null) {
                throw new TNoSuchElementException();
            }
            removeEntry(currentEntry);
            version = modCount;
            currentEntry = null;
        }
    }

    private static class ReversedLinkedList<E> extends TLinkedList<E> {
        private final TLinkedList<E> list;
        private final TReversedList<E> reversed;

        private ReversedLinkedList(TLinkedList<E> list) {
            this.list = list;
            this.reversed = new TReversedList<>(list);
        }

        @Override
        public String toString() {
            return reversed.toString();
        }

        @Override
        public boolean retainAll(TCollection<?> c) {
            return reversed.retainAll(c);
        }

        @Override
        public boolean removeAll(TCollection<?> c) {
            return reversed.removeAll(c);
        }

        @Override
        public boolean containsAll(TCollection<?> c) {
            return reversed.containsAll(c);
        }

        @Override
        public boolean isEmpty() {
            return list.isEmpty();
        }

        @Override
        public TStream<E> stream() {
            return reversed.stream();
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            return list.removeIf(filter);
        }

        @Override
        public <T> T[] toArray(IntFunction<T[]> generator) {
            return reversed.toArray(generator);
        }

        @Override
        public void forEach(Consumer<? super E> action) {
            reversed.forEach(action);
        }

        @Override
        public TIterator<E> iterator() {
            return list.descendingIterator();
        }

        @Override
        public int hashCode() {
            return reversed.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return reversed.equals(o);
        }

        @Override
        public TList<E> subList(int fromIndex, int toIndex) {
            return reversed.subList(fromIndex, toIndex);
        }

        @Override
        public TListIterator<E> listIterator() {
            return reversed.listIterator();
        }

        @Override
        public void sort(TComparator<? super E> c) {
            reversed.sort(c);
        }

        @Override
        public void replaceAll(TUnaryOperator<E> operator) {
            list.replaceAll(operator);
        }

        @Override
        public TLinkedList<E> reversed() {
            return list;
        }

        @Override
        public TSpliterator<E> spliterator() {
            return reversed.spliterator();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return reversed.toArray(a);
        }

        @Override
        public Object[] toArray() {
            return reversed.toArray();
        }

        @Override
        public TIterator<E> descendingIterator() {
            return list.iterator();
        }

        @Override
        public TListIterator<E> listIterator(int index) {
            return reversed.listIterator(index);
        }

        @Override
        public boolean removeLastOccurrence(Object o) {
            return list.removeFirstOccurrence(o);
        }

        @Override
        public boolean removeFirstOccurrence(Object o) {
            return list.removeLastOccurrence(o);
        }

        @Override
        public E pop() {
            return list.removeLast();
        }

        @Override
        public void push(E e) {
            list.addLast(e);
        }

        @Override
        public E pollLast() {
            return list.pollFirst();
        }

        @Override
        public E pollFirst() {
            return list.pollLast();
        }

        @Override
        public E peekLast() {
            return list.peekFirst();
        }

        @Override
        public E peekFirst() {
            return list.peekLast();
        }

        @Override
        public boolean offerLast(E e) {
            return list.offerFirst(e);
        }

        @Override
        public boolean offerFirst(E e) {
            return list.offerLast(e);
        }

        @Override
        public boolean offer(E e) {
            return list.offerLast(e);
        }

        @Override
        public E remove() {
            return list.removeFirst();
        }

        @Override
        public E poll() {
            return list.pollLast();
        }

        @Override
        public E element() {
            if (list.lastEntry == null) {
                throw new TNoSuchElementException();
            }
            return list.lastEntry.item;
        }

        @Override
        public E peek() {
            return list.lastEntry != null ? list.lastEntry.item : null;
        }

        @Override
        public int lastIndexOf(Object o) {
            return list.size() - list.indexOf(o) - 1;
        }

        @Override
        public int indexOf(Object o) {
            return list.size() - list.lastIndexOf(o) - 1;
        }

        @Override
        public E remove(int index) {
            return reversed.remove(index);
        }

        @Override
        public void add(int index, E element) {
            reversed.add(index, element);
        }

        @Override
        public E set(int index, E element) {
            return reversed.set(index, element);
        }

        @Override
        public E get(int index) {
            return reversed.get(index);
        }

        @Override
        public void clear() {
            list.clear();
        }

        @Override
        public boolean addAll(int index, TCollection<? extends E> c) {
            return reversed.addAll(index, c);
        }

        @Override
        public boolean addAll(TCollection<? extends E> c) {
            return reversed.addAll(c);
        }

        @Override
        public boolean remove(Object o) {
            return list.removeLastOccurrence(o);
        }

        @Override
        public boolean add(E e) {
            list.addLast(e);
            return true;
        }

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public boolean contains(Object o) {
            return list.contains(o);
        }

        @Override
        public void addLast(E e) {
            list.addFirst(e);
        }

        @Override
        public void addFirst(E e) {
            list.addLast(e);
        }

        @Override
        public E removeLast() {
            return list.removeFirst();
        }

        @Override
        public E removeFirst() {
            return list.removeLast();
        }

        @Override
        public E getLast() {
            return list.getFirst();
        }

        @Override
        public E getFirst() {
            return list.getLast();
        }
    }
}
