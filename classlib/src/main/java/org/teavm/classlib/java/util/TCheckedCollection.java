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

class TCheckedCollection<E> implements TCollection<E> {
    TCollection<E> innerCollection;
    Class<E> type;

    public TCheckedCollection(TCollection<E> innerCollection, Class<E> type) {
        this.innerCollection = innerCollection;
        this.type = type;
    }

    @Override
    public TIterator<E> iterator() {
        return innerCollection.iterator();
    }

    @Override
    public int size() {
        return innerCollection.size();
    }

    @Override
    public boolean isEmpty() {
        return innerCollection.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return innerCollection.contains(o);
    }

    @Override
    public Object[] toArray() {
        return innerCollection.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return innerCollection.toArray(a);
    }

    @Override
    public boolean add(E e) {
        return innerCollection.add(type.cast(e));
    }

    @Override
    public boolean remove(Object o) {
        return innerCollection.remove(o);
    }

    @Override
    public boolean containsAll(TCollection<?> c) {
        return innerCollection.containsAll(c);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean addAll(TCollection<? extends E> c) {
        Object[] items = c.toArray();
        for (int i = 0; i < items.length; ++i) {
            items[i] = type.cast(items[i]);
        }
        return innerCollection.addAll(TArrays.asList((E[]) items));
    }

    @Override
    public boolean removeAll(TCollection<?> c) {
        return innerCollection.removeAll(c);
    }

    @Override
    public boolean retainAll(TCollection<?> c) {
        return innerCollection.retainAll(c);
    }

    @Override
    public void clear() {
        innerCollection.clear();
    }
}
