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

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.lang.TCloneable;

public class TLinkedHashSet<E> extends THashSet<E> implements TSequencedSet<E>, TCloneable, TSerializable {
    public TLinkedHashSet() {
        super(new TLinkedHashMap<>());
    }

    public TLinkedHashSet(int capacity) {
        super(new TLinkedHashMap<>(capacity));
    }

    public TLinkedHashSet(int capacity, float loadFactor) {
        super(new TLinkedHashMap<>(capacity, loadFactor));
    }

    public TLinkedHashSet(TCollection<? extends E> collection) {
        super(new TLinkedHashMap<>(collection.size() < 6 ? 11 : collection.size() * 2));
        for (TIterator<? extends E> iter = collection.iterator(); iter.hasNext();) {
            add(iter.next());
        }
    }

    private TLinkedHashMap<E, THashSet<E>> map() {
        return (TLinkedHashMap<E, THashSet<E>>) backingMap;
    }

    @Override
    public void addFirst(E e) {
        map().putFirst(e, this);
    }

    @Override
    public void addLast(E e) {
        map().putLast(e, this);
    }

    @Override
    public E getFirst() {
        return map().sequencedKeySet().getFirst();
    }

    @Override
    public E getLast() {
        return map().sequencedKeySet().getLast();
    }

    @Override
    public E removeFirst() {
        return map().sequencedKeySet().removeFirst();
    }

    @Override
    public E removeLast() {
        return map().sequencedKeySet().removeLast();
    }

    @Override
    public TSequencedSet<E> reversed() {
        return new ReversedLinkedHashSet<>(this);
    }

    static class ReversedLinkedHashSet<E> extends TAbstractSet<E> implements TSequencedSet<E> {
        private final TLinkedHashSet<E> base;

        ReversedLinkedHashSet(TLinkedHashSet<E> base) {
            this.base = base;
        }

        @Override
        public int size() {
            return base.size();
        }

        @Override
        public TIterator<E> iterator() {
            return base.map().sequencedKeySet().reversed().iterator();
        }

        @Override
        public boolean add(E e) {
            return base.add(e);
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
        public E getFirst() {
            return base.getLast();
        }

        @Override
        public E getLast() {
            return base.getFirst();
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
        public TSequencedSet<E> reversed() {
            return base;
        }
    }

    public static <T> TLinkedHashSet<T> newLinkedHashSet(int size) {
        if (size < 0) {
            throw new IllegalArgumentException();
        }
        return new TLinkedHashSet<>(THashMap.capacity(size));
    }
}
