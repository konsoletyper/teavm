/*
 *  Copyright 2025 konsoletyper.
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
package org.teavm.classlib.java.util.concurrent;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import org.teavm.classlib.java.util.TAbstractSet;
import org.teavm.classlib.java.util.TCollection;
import org.teavm.classlib.java.util.TIterator;
import org.teavm.classlib.java.util.TSet;
import org.teavm.interop.Rename;

public class TCopyOnWriteArraySet<E> extends TAbstractSet<E> implements TSet {
    private final TCopyOnWriteArrayList<E> list;

    public TCopyOnWriteArraySet() {
        list = new TCopyOnWriteArrayList<>();
    }

    public TCopyOnWriteArraySet(Collection<? extends E> c) {
        list = new TCopyOnWriteArrayList<>();
        addAll(c);
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public TIterator<E> iterator() {
        return list.iterator();
    }

    @Override
    public Object[] toArray() {
        return list.toArray();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean add(E e) {
        return list.addIfAbsent(e);
    }

    @Override
    public boolean remove(Object o) {
        return list.remove(o);
    }

    @Override
    public boolean containsAll(TCollection c) {
        for (Object e : c) {
            if (!contains(e)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(TCollection<? extends E> c) {
        boolean modified = false;
        for (E e : c) {
            if (add(e)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean removeAll(TCollection c) {
        return list.removeAll(c);
    }

    @Override
    public boolean retainAll(TCollection c) {
        return list.retainAll(c);
    }

    @Override
    public void clear() {
        list.clear();
    }
}
