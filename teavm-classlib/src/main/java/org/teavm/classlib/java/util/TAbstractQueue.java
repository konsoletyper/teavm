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

import org.teavm.classlib.java.lang.TIllegalStateException;

/**
 *
 * @author Alexey Andreev
 * @param <E>
 */
public abstract class TAbstractQueue<E> extends TAbstractCollection<E> implements TQueue<E> {
    @Override
    public boolean add(E e) {
        if (offer(e)) {
            return true;
        }
        throw new TIllegalStateException();
    }

    @Override
    public E remove() {
        if (isEmpty()) {
            throw new TNoSuchElementException();
        }
        return poll();
    }

    @Override
    public E element() {
        if (isEmpty()) {
            throw new TNoSuchElementException();
        }
        return peek();
    }

    @Override
    public void clear() {
        while (!isEmpty()) {
            poll();
        }
    }

    @Override
    public boolean addAll(TCollection<? extends E> c) {
        boolean oneAdded = false;
        for (TIterator<? extends E> iter = c.iterator(); iter.hasNext();) {
            oneAdded |= add(iter.next());
        }
        return oneAdded;
    }
}
