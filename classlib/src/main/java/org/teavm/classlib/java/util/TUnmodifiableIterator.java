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

class TUnmodifiableIterator<E> implements TIterator<E> {
    final TIterator<? extends E> underlyingIterator;

    TUnmodifiableIterator(TIterator<? extends E> underlyingIterator) {
        this.underlyingIterator = underlyingIterator;
    }

    @Override
    public boolean hasNext() {
        return underlyingIterator.hasNext();
    }

    @Override
    public E next() {
        return underlyingIterator.next();
    }

    @Override
    public void remove() {
        throw new TUnsupportedOperationException();
    }
}
