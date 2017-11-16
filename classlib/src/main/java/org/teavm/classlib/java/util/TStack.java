/*
 *  Copyright 2017 Alexey Andreev.
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

public class TStack<E> extends TVector<E> {
    public TStack() {
        super();
    }

    public boolean empty() {
        return isEmpty();
    }

    @SuppressWarnings("unchecked")
    public synchronized E peek() {
        try {
            return (E) elementData[elementCount - 1];
        } catch (IndexOutOfBoundsException e) {
            throw new TEmptyStackException();
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized E pop() {
        if (elementCount == 0) {
            throw new TEmptyStackException();
        }
        final int index = --elementCount;
        final E obj = (E) elementData[index];
        elementData[index] = null;
        modCount++;
        return obj;
    }

    public E push(E object) {
        addElement(object);
        return object;
    }

    public synchronized int search(Object o) {
        final Object[] dumpArray = elementData;
        final int size = elementCount;
        if (o != null) {
            for (int i = size - 1; i >= 0; i--) {
                if (o.equals(dumpArray[i])) {
                    return size - i;
                }
            }
        } else {
            for (int i = size - 1; i >= 0; i--) {
                if (dumpArray[i] == null) {
                    return size - i;
                }
            }
        }
        return -1;
    }
}
