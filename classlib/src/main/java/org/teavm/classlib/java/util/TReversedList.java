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

class TReversedList<E> extends TAbstractList<E> {
    private final TList<E> base;

    TReversedList(TList<E> base) {
        this.base = base;
    }

    @Override
    public E get(int index) {
        return base.get(size() - index - 1);
    }

    @Override
    public int size() {
        return base.size();
    }

    @Override
    public E set(int index, E element) {
        return base.set(size() - index - 1, element);
    }

    @Override
    public void add(int index, E element) {
        base.add(size() - index, element);
    }

    @Override
    public E remove(int index) {
        return base.remove(size() - index - 1);
    }

    @Override
    public void addFirst(E element) {
        base.addLast(element);
    }

    @Override
    public void addLast(E element) {
        base.addFirst(element);
    }

    @Override
    public E removeFirst() {
        return base.remove(size() - 1);
    }

    @Override
    public E removeLast() {
        return base.remove(0);
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
    public TIterator<E> iterator() {
        TListIterator<E> lit = base.listIterator(size());
        return new TIterator<>() {
            @Override
            public boolean hasNext() {
                return lit.hasPrevious();
            }

            @Override
            public E next() {
                return lit.previous();
            }

            @Override
            public void remove() {
                lit.remove();
            }
        };
    }

    @Override
    public TListIterator<E> listIterator() {
        return new ReversedListIterator<>(this, base.listIterator(base.size()));
    }

    @Override
    public TListIterator<E> listIterator(int idx) {
        return new ReversedListIterator<>(this, base.listIterator(base.size() - idx));
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
    public TList<E> reversed() {
        return base;
    }

    static class RandomAccess<E> extends TReversedList<E> implements TRandomAccess {
        RandomAccess(TList<E> base) {
            super(base);
        }
    }

    private static class ReversedListIterator<E> implements TListIterator<E> {
        private final TList<E> list;
        private final TListIterator<E> lit;

        private ReversedListIterator(TList<E> list, TListIterator<E> lit) {
            this.list = list;
            this.lit = lit;
        }

        @Override
        public boolean hasPrevious() {
            return lit.hasNext();
        }

        @Override
        public E previous() {
            return lit.next();
        }

        @Override
        public int nextIndex() {
            return list.size() - lit.previousIndex() - 1;
        }

        @Override
        public int previousIndex() {
            return list.size() - lit.nextIndex() - 1;
        }

        @Override
        public void set(E e) {
            lit.set(e);
        }

        @Override
        public void add(E e) {
            lit.add(e);
        }

        @Override
        public boolean hasNext() {
            return lit.hasPrevious();
        }

        @Override
        public E next() {
            return lit.previous();
        }

        @Override
        public void remove() {
            lit.remove();
        }
    }
}
