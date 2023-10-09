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

class TReversedSortedSet<E> extends TAbstractSet<E> implements TSortedSet<E> {
    private final TSortedSet<E> base;

    TReversedSortedSet(TSortedSet<E> base) {
        this.base = base;
    }

    @Override
    public TIterator<E> iterator() {
        return new DescendingSortedSetIterator<>(base);
    }

    @Override
    public boolean add(E e) {
        base.add(e);
        return true;
    }

    @Override
    public boolean addAll(TCollection<? extends E> c) {
        return base.addAll(c);
    }

    @Override
    public void clear() {
        base.clear();
    }

    @Override
    public boolean contains(Object o) {
        return base.contains(o);
    }

    @Override
    public boolean containsAll(TCollection<?> c) {
        return base.containsAll(c);
    }

    @Override
    public boolean isEmpty() {
        return base.isEmpty();
    }

    @Override
    public boolean remove(Object o) {
        return base.remove(o);
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
    public boolean removeAll(TCollection<?> c) {
        return base.removeAll(c);
    }

    @Override
    public boolean retainAll(TCollection<?> c) {
        return base.retainAll(c);
    }

    @Override
    public int size() {
        return base.size();
    }

    @Override
    public TComparator<? super E> comparator() {
        TComparator<? super E> comp = base.comparator();
        return comp != null ? comp.reversed() : TCollections.reverseOrder();
    }

    @Override
    public E first() {
        return base.last();
    }

    @Override
    public E last() {
        return base.first();
    }

    @Override
    public E getFirst() {
        return base.last();
    }

    @Override
    public E getLast() {
        return base.first();
    }

    @Override
    public TSortedSet<E> headSet(E to) {
        return new DescSubset<>(base, null, to);
    }

    @Override
    public TSortedSet<E> subSet(E from, E to) {
        return new DescSubset<>(base, from, to);
    }

    @Override
    public TSortedSet<E> tailSet(E from) {
        return new DescSubset<>(base, from, null);
    }

    static class DescSubset<E> extends TAbstractSet<E> implements TSortedSet<E> {
        final TSortedSet<E> base;
        final E head;
        final E tail;
        private final TComparator<E> reversed;

        @SuppressWarnings("unchecked")
        DescSubset(TSortedSet<E> base, E head, E tail) {
            this.base = base;
            this.head = head;
            this.tail = tail;
            this.reversed = (TComparator<E>) TCollections.reverseOrder(base.comparator());
        }

        private boolean aboveHeadInc(E e) {
            return head == null || reversed.compare(e, head) >= 0;
        }

        private boolean belowTailExc(E e) {
            return tail == null || reversed.compare(e, tail) < 0;
        }

        @Override
        public TIterator<E> iterator() {
            return new TIterator<>() {
                private final TIterator<E> it = new DescendingSortedSetIterator<>(base);
                private E current;
                private boolean finished;

                @Override
                public boolean hasNext() {
                    if (finished) {
                        return false;
                    }
                    if (current != null) {
                        return true;
                    }
                    while (it.hasNext()) {
                        E e = it.next();
                        if (!aboveHeadInc(e)) {
                            continue;
                        }
                        if (!belowTailExc(e)) {
                            finished = true;
                            return false;
                        }
                        current = e;
                        return true;
                    }
                    return false;
                }

                @Override
                public E next() {
                    if (hasNext()) {
                        E e = current;
                        current = null;
                        return e;
                    } else {
                        throw new TNoSuchElementException();
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public boolean add(E e) {
            if (aboveHeadInc(e) && belowTailExc(e)) {
                return base.add(e);
            } else {
                throw new IllegalArgumentException();
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean remove(Object o) {
            E e = (E) o;
            if (aboveHeadInc(e) && belowTailExc(e)) {
                return base.remove(o);
            } else {
                return false;
            }
        }

        @Override
        public int size() {
            int size = 0;
            for (var it = iterator(); it.hasNext(); it.next()) {
                size++;
            }
            return size;
        }

        @Override
        public TComparator<? super E> comparator() {
            TComparator<? super E> comp = base.comparator();
            return comp != null ? comp.reversed() : TCollections.reverseOrder();
        }

        @Override
        public E first() {
            return iterator().next();
        }

        @Override
        public E last() {
            TIterator<E> it = iterator();
            if (!it.hasNext()) {
                throw new TNoSuchElementException();
            }
            E last = it.next();
            while (it.hasNext()) {
                last = it.next();
            }
            return last;
        }

        @Override
        public TSortedSet<E> subSet(E from, E to) {
            if (aboveHeadInc(from) && belowTailExc(from)
                    && aboveHeadInc(to) && belowTailExc(to)
                    && reversed.compare(from, to) <= 0) {
                return new DescSubset<>(base, from, to);
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public TSortedSet<E> headSet(E to) {
            if (aboveHeadInc(to) && belowTailExc(to)) {
                return new DescSubset<>(base, head, to);
            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public TSortedSet<E> tailSet(E from) {
            if (aboveHeadInc(from) && belowTailExc(from)) {
                return new DescSubset<>(base, null, tail);
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    private static class DescendingSortedSetIterator<E> implements TIterator<E> {
        private final TSortedSet<E> base;
        private TSortedSet<E> remaining;
        private E current;

        private DescendingSortedSetIterator(TSortedSet<E> base) {
            this.base = base;
            this.remaining = base;
        }

        @Override
        public boolean hasNext() {
            return !remaining.isEmpty();
        }

        @Override
        public E next() {
            if (remaining.isEmpty()) {
                throw new TNoSuchElementException();
            }
            current = remaining.last();
            remaining = base.headSet(current);
            return current;
        }

        @Override
        public void remove() {
            if (current == null) {
                throw new IllegalStateException();
            } else {
                base.remove(current);
                current = null;
            }
        }
    }
}
