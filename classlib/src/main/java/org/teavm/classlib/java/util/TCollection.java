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

import java.util.Spliterator;
import org.teavm.classlib.java.lang.TIterable;
import org.teavm.classlib.java.util.stream.TStream;
import org.teavm.classlib.java.util.stream.impl.TSpliteratorOverCollection;
import org.teavm.classlib.java.util.stream.impl.TStreamOverSpliterator;

public interface TCollection<E> extends TIterable<E> {
    int size();

    boolean isEmpty();

    boolean contains(Object o);

    Object[] toArray();

    <T> T[] toArray(T[] a);

    boolean add(E e);

    boolean remove(Object o);

    boolean containsAll(TCollection<?> c);

    boolean addAll(TCollection<? extends E> c);

    boolean removeAll(TCollection<?> c);

    boolean retainAll(TCollection<?> c);

    void clear();

    @Override
    default TSpliterator<E> spliterator() {
        return new TSpliteratorOverCollection<>(this);
    }

    @SuppressWarnings("unchecked")
    default TStream<E> stream() {
        return new TStreamOverSpliterator<>((Spliterator<E>) spliterator());
    }
}
