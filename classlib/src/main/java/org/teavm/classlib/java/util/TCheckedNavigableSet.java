/*
 *  Copyright 2026 Gregory Mitchell.
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

class TCheckedNavigableSet<E> extends TCheckedSortedSet<E> implements TNavigableSet<E> {
    private final TNavigableSet<E> set;

    TCheckedNavigableSet(TNavigableSet<E> set, Class<E> type) {
        super(set, type);
        this.set = set;
    }

    @Override
    public E lower(E e) {
        return set.lower(e);
    }

    @Override
    public E floor(E e) {
        return set.floor(e);
    }

    @Override
    public E ceiling(E e) {
        return set.ceiling(e);
    }

    @Override
    public E higher(E e) {
        return set.higher(e);
    }

    @Override
    public E pollFirst() {
        return set.pollFirst();
    }

    @Override
    public E pollLast() {
        return set.pollLast();
    }

    @Override
    public TNavigableSet<E> descendingSet() {
        return new TCheckedNavigableSet<>(set.descendingSet(), type);
    }

    @Override
    public TIterator<E> descendingIterator() {
        return set.descendingIterator();
    }

    @Override
    public TNavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return new TCheckedNavigableSet<>(set.subSet(fromElement, fromInclusive, toElement, toInclusive), type);
    }

    @Override
    public TNavigableSet<E> headSet(E toElement, boolean inclusive) {
        return new TCheckedNavigableSet<>(set.headSet(toElement, inclusive), type);
    }

    @Override
    public TNavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return new TCheckedNavigableSet<>(set.tailSet(fromElement, inclusive), type);
    }
}
