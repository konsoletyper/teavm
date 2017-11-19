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

import java.lang.reflect.Array;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.lang.TUnsupportedOperationException;

public abstract class TAbstractCollection<E> extends TObject implements TCollection<E> {
    protected TAbstractCollection() {
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        for (TIterator<E> iter = iterator(); iter.hasNext();) {
            E e = iter.next();
            if (e == null ? o == null : e.equals(o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object[] toArray() {
        Object[] arr = new Object[size()];
        int i = 0;
        for (TIterator<E> iter = iterator(); iter.hasNext();) {
            arr[i++] = iter.next();
        }
        return arr;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        int size = size();
        if (a.length < size) {
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
        } else {
            for (int i = size; i < a.length; ++i) {
                a[i] = null;
            }
        }
        int i = 0;
        for (TIterator<E> iter = iterator(); iter.hasNext();) {
            a[i++] = (T) iter.next();
        }
        return a;
    }

    @Override
    public boolean add(E e) {
        throw new TUnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        for (TIterator<E> iter = iterator(); iter.hasNext();) {
            E e = iter.next();
            if (e == null ? o == null : e.equals(o)) {
                iter.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsAll(TCollection<?> c) {
        for (TIterator<?> iter = c.iterator(); iter.hasNext();) {
            if (!contains(iter.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(TCollection<? extends E> c) {
        boolean changed = false;
        for (TIterator<? extends E> iter = c.iterator(); iter.hasNext();) {
            if (add(iter.next())) {
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean removeAll(TCollection<?> c) {
        boolean changed = false;
        for (TIterator<E> iter = iterator(); iter.hasNext();) {
            E e = iter.next();
            if (c.contains(e)) {
                iter.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public boolean retainAll(TCollection<?> c) {
        boolean changed = false;
        for (TIterator<E> iter = iterator(); iter.hasNext();) {
            E e = iter.next();
            if (!c.contains(e)) {
                iter.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override
    public void clear() {
        for (TIterator<E> iter = iterator(); iter.hasNext();) {
            iter.next();
            iter.remove();
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        TIterator<E> iter = iterator();
        if (iter.hasNext()) {
            sb.append(String.valueOf(iter.next()));
        }
        while (iter.hasNext()) {
            sb.append(", ").append(String.valueOf(iter.next()));
        }
        sb.append("]");
        return sb.toString();
    }
}
