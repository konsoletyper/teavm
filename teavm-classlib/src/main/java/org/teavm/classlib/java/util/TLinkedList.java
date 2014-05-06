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

import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/**
 *
 * @author Alexey Andreev
 */
public class TLinkedList<E> extends TAbstractSequentialList<E> {
    static class Entry<E> {
        E item;
        Entry<E> next;
        Entry<E> previous;
    }
    private Entry<E> firstEntry;
    private Entry<E> lastEntry;
    private int size;

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
        return new SequentialListIterator(firstEntry, null);
    }

    @Override
    public TListIterator<E> listIterator(int index) {
        if (index <= size / 2) {
            TListIterator<E> iter = listIterator();
            while (index-- > 0) {
                if (!iter.hasNext()) {
                    throw new IndexOutOfBoundsException();
                }
                iter.next();
            }
            return iter;
        } else {
            TListIterator<E> iter = new SequentialListIterator(null, lastEntry);
            while (index++ < size) {
                if (!iter.hasPrevious()) {
                    throw new IndexOutOfBoundsException();
                }
                iter.previous();
            }
            return iter;
        }
    }

    private class SequentialListIterator implements TListIterator<E> {
        private Entry<E> nextEntry = firstEntry;
        private Entry<E> prevEntry = null;
        private Entry<E> currentEntry;
        private int index;
        private int version = modCount;

        public SequentialListIterator(Entry<E> nextEntry, Entry<E> prevEntry) {
            this.nextEntry = nextEntry;
            this.prevEntry = prevEntry;
        }

        @Override
        public boolean hasNext() {
            return nextEntry != null;
        }

        @Override
        public E next() {
            checkConcurrentModification();
            if (nextEntry == null) {
                throw new NoSuchElementException();
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
            currentEntry.next.previous = currentEntry.next;
            currentEntry.previous.next = currentEntry.next;
            if (currentEntry == firstEntry) {
                firstEntry = firstEntry.next;
            } else if (currentEntry == lastEntry) {
                lastEntry = lastEntry.previous;
            }
            if (currentEntry == prevEntry) {
                prevEntry = nextEntry.previous;
            } else if (currentEntry == nextEntry) {
                nextEntry = prevEntry.next;
            }
            --index;
            --size;
            ++modCount;
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
                throw new NoSuchElementException();
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
                throw new ConcurrentModificationException();
            }
        }
    }
}
