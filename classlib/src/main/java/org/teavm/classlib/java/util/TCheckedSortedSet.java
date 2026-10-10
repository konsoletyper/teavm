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

class TCheckedSortedSet<E> extends TCheckedCollection<E> implements TSortedSet<E> {
    private final TSortedSet<E> set;

    TCheckedSortedSet(TSortedSet<E> set, Class<E> type) {
        super(set, type);
        this.set = set;
    }

    @Override
    public TComparator<? super E> comparator() {
        return set.comparator();
    }

    @Override
    public TSortedSet<E> subSet(E fromElement, E toElement) {
        return new TCheckedSortedSet<>(set.subSet(fromElement, toElement), type);
    }

    @Override
    public TSortedSet<E> headSet(E toElement) {
        return new TCheckedSortedSet<>(set.headSet(toElement), type);
    }

    @Override
    public TSortedSet<E> tailSet(E fromElement) {
        return new TCheckedSortedSet<>(set.tailSet(fromElement), type);
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
