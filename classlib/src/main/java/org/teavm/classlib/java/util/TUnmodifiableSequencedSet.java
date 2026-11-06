/*
 *  Copyright 2026 Alexey Andreev.
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

import org.teavm.classlib.java.lang.TUnsupportedOperationException;

class TUnmodifiableSequencedSet<E> extends TUnmodifiableSet<E> implements TSequencedSet<E> {
    TUnmodifiableSequencedSet(TSequencedSet<? extends E> underlyingSet) {
        super(underlyingSet);
    }

    private TSequencedSet<? extends E> underlyingSequencedSet() {
        return (TSequencedSet<? extends E>) underlyingCollection;
    }

    @Override
    public TSequencedSet<E> reversed() {
        return new TUnmodifiableSequencedSet<>(underlyingSequencedSet().reversed());
    }

    @Override
    public E getFirst() {
        return underlyingSequencedSet().getFirst();
    }

    @Override
    public E getLast() {
        return underlyingSequencedSet().getLast();
    }

    @Override
    public void addFirst(E e) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public void addLast(E e) {
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
}
