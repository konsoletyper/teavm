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
public interface TSortedSet<E> extends TSet<E>, TSequencedSet<E> {
    TComparator<? super E> comparator();

    TSortedSet<E> subSet(E fromElement, E toElement);

    TSortedSet<E> headSet(E toElement);

    TSortedSet<E> tailSet(E fromElement);

    E first();

    E last();

    @Override
    default E getFirst() {
        return first();
    }

    @Override
    default E getLast() {
        return last();
    }

    @Override
    default E removeFirst() {
        E e = this.first();
        this.remove(e);
        return e;
    }

    @Override
    default E removeLast() {
        E e = this.last();
        this.remove(e);
        return e;
    }

    @Override
    default TSortedSet<E> reversed() {
        return new TReversedSortedSet<>(this);
    }
}
