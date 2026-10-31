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

class TUnmodifiableSequencedCollection<E> extends TUnmodifiableCollection<E> implements TSequencedCollection<E> {
    TUnmodifiableSequencedCollection(TSequencedCollection<? extends E> underlyingCollection) {
        super(underlyingCollection);
    }

    private TSequencedCollection<? extends E> underlyingSequencedCollection() {
        return (TSequencedCollection<? extends E>) underlyingCollection;
    }

    @Override
    public TSequencedCollection<E> reversed() {
        return new TUnmodifiableSequencedCollection<>(underlyingSequencedCollection().reversed());
    }

    @Override
    public E getFirst() {
        return underlyingSequencedCollection().getFirst();
    }

    @Override
    public E getLast() {
        return underlyingSequencedCollection().getLast();
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
