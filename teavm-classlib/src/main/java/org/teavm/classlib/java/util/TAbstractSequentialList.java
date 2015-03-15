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

import org.teavm.classlib.java.lang.TIndexOutOfBoundsException;

/**
 *
 * @author Alexey Andreev
 * @param <E>
 */
public abstract class TAbstractSequentialList<E> extends TAbstractList<E> {
    @Override
    public E get(int index) {
        if (index < 0) {
            throw new TIndexOutOfBoundsException();
        }
        TIterator<E> iter = listIterator(index);
        return iter.next();
    }

    @Override
    public E set(int index, E element) {
        if (index < 0) {
            throw new TIndexOutOfBoundsException();
        }
        TListIterator<E> iter = listIterator(index);
        E old = iter.next();
        iter.set(element);
        return old;
    }

    @Override
    public void add(int index, E element) {
        if (index < 0) {
            throw new TIndexOutOfBoundsException();
        }
        TListIterator<E> iter = listIterator(index);
        iter.add(element);
    }

    @Override
    public E remove(int index) {
        if (index < 0) {
            throw new TIndexOutOfBoundsException();
        }
        TListIterator<E> iter = listIterator(index);
        E elem = iter.next();
        iter.remove();
        return elem;
    }

    @Override
    public boolean addAll(int index, TCollection<? extends E> c) {
        if (index < 0) {
            throw new TIndexOutOfBoundsException();
        }
        TListIterator<E> iter = listIterator(index);
        boolean added = false;
        for (TIterator<? extends E> srcIter = c.iterator(); srcIter.hasNext();) {
            iter.add(srcIter.next());
            iter.next();
            added = true;
        }
        return added;
    }

    @Override
    public TIterator<E> iterator() {
        return listIterator();
    }

    @Override
    public abstract TListIterator<E> listIterator(int index);
}
