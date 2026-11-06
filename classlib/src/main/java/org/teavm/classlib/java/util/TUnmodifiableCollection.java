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

import java.util.function.Predicate;
import org.teavm.classlib.java.lang.TUnsupportedOperationException;

class TUnmodifiableCollection<E> extends TAbstractCollection<E> {
    final TCollection<? extends E> underlyingCollection;

    TUnmodifiableCollection(TCollection<? extends E> underlyingCollection) {
        this.underlyingCollection = underlyingCollection;
    }

    @Override
    public TIterator<E> iterator() {
        return new TUnmodifiableIterator<>(underlyingCollection.iterator());
    }

    @Override
    public int size() {
        return underlyingCollection.size();
    }

    @Override
    public boolean isEmpty() {
        return underlyingCollection.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return underlyingCollection.contains(o);
    }

    @Override
    public boolean containsAll(TCollection<?> c) {
        return underlyingCollection.containsAll(c);
    }

    @Override
    public boolean remove(Object o) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public boolean addAll(TCollection<? extends E> c) {
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
    public String toString() {
        return underlyingCollection.toString();
    }
}
