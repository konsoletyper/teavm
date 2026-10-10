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

import java.util.function.Predicate;
import org.teavm.classlib.java.lang.TUnsupportedOperationException;

class TUnmodifiableSortedSet<E> extends TAbstractSet<E> implements TSortedSet<E> {
    private final TSortedSet<E> set;

    TUnmodifiableSortedSet(TSortedSet<E> set) {
        this.set = TObjects.requireNonNull(set);
    }

    @Override
    public TIterator<E> iterator() {
        return TCollections.unmodifiableIterator(set.iterator());
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return set.contains(o);
    }

    @Override
    public boolean containsAll(TCollection<?> c) {
        return set.containsAll(c);
    }

    @Override
    public boolean add(E e) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public boolean addAll(TCollection<? extends E> c) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public boolean removeAll(TCollection<?> c) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public boolean retainAll(TCollection<?> c) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new TUnsupportedOperationException();
    }

    @Override
    public E removeFirst() {
        throw new TUnsupportedOperationException();
    }

    @Override
    public E removeLast() {
        throw new TUnsupportedOperationException();
    }

    @Override
    public TComparator<? super E> comparator() {
        return set.comparator();
    }

    @Override
    public TSortedSet<E> subSet(E fromElement, E toElement) {
        return new TUnmodifiableSortedSet<>(set.subSet(fromElement, toElement));
    }

    @Override
    public TSortedSet<E> headSet(E toElement) {
        return new TUnmodifiableSortedSet<>(set.headSet(toElement));
    }

    @Override
    public TSortedSet<E> tailSet(E fromElement) {
        return new TUnmodifiableSortedSet<>(set.tailSet(fromElement));
    }

    @Override
    public E first() {
        return set.first();
    }

    @Override
    public E last() {
        return set.last();
    }

    @Override
    public boolean equals(Object o) {
        return o == this || set.equals(o);
    }

    @Override
    public int hashCode() {
        return set.hashCode();
    }

    @Override
    public String toString() {
        return set.toString();
    }
}
