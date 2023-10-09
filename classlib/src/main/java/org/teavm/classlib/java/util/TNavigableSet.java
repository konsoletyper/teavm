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

/**
 *
 * @author Alexey Andreev
 * @param <E>
 */
public interface TNavigableSet<E> extends TSortedSet<E> {
    E lower(E e);

    E floor(E e);

    E ceiling(E e);

    E higher(E e);

    E pollFirst();

    E pollLast();

    TNavigableSet<E> descendingSet();

    TIterator<E> descendingIterator();

    TNavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive);

    TNavigableSet<E> headSet(E toElement, boolean inclusive);

    TNavigableSet<E> tailSet(E fromElement, boolean inclusive);

    @Override
    default E removeFirst() {
        if (isEmpty()) {
            throw new TNoSuchElementException();
        } else {
            return pollFirst();
        }
    }

    @Override
    default E removeLast() {
        if (isEmpty()) {
            throw new TNoSuchElementException();
        } else {
            return pollLast();
        }
    }

    @Override
    default TNavigableSet<E> reversed() {
        return descendingSet();
    }
}
