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
 * @param <E>
 * @author Alexey Andreev
 */
public interface TDeque<E> extends TQueue<E>, TSequencedCollection<E> {
    boolean offerFirst(E e);

    boolean offerLast(E e);

    E pollFirst();

    E pollLast();

    E peekFirst();

    E peekLast();

    boolean removeFirstOccurrence(Object o);

    boolean removeLastOccurrence(Object o);

    void push(E e);

    E pop();

    TIterator<E> descendingIterator();

    @Override
    default TDeque<E> reversed() {
        return new TReversedDeque<>(this);
    }
}
