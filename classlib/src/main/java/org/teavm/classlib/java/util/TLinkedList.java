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
        offer(e);
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
}
