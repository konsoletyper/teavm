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

class TCheckedList<E> extends TCheckedCollection<E> implements TList<E> {
    private TList<E> innerList;

    public TCheckedList(TList<E> innerList, Class<E> type) {
        super(innerList, type);
        this.innerList = innerList;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean addAll(int index, TCollection<? extends E> c) {
        Object[] items = c.toArray();
        for (int i = 0; i < items.length; ++i) {
            items[i] = type.cast(items[i]);
        }
        return innerList.addAll(index, TArrays.asList((E[]) items));
    }

    @Override
    public E get(int index) {
        return innerList.get(index);
    }

    @Override
    public E set(int index, E element) {
        return innerList.set(index, type.cast(element));
    }

    @Override
    public void add(int index, E element) {
        innerList.add(index, type.cast(element));
    }

    @Override
    public E remove(int index) {
        return innerList.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return innerList.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return innerList.lastIndexOf(o);
    }

    @Override
    public TListIterator<E> listIterator() {
        return new TCheckedListIterator<>(innerList.listIterator(), type);
    }

    @Override
    public TListIterator<E> listIterator(int index) {
        return new TCheckedListIterator<>(innerList.listIterator(index), type);
    }

    @Override
    public TList<E> subList(int fromIndex, int toIndex) {
        return new TCheckedList<>(innerList.subList(fromIndex, toIndex), type);
    }
}
