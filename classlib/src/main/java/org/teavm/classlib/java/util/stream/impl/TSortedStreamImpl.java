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
package org.teavm.classlib.java.util.stream.impl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Predicate;

public class TSortedStreamImpl<T> extends TSimpleStreamImpl<T> {
    private TSimpleStreamImpl<T> sourceStream;
    private T[] array;
    private int index;

    @SuppressWarnings("unchecked")
    public TSortedStreamImpl(TSimpleStreamImpl<T> sourceStream, Comparator<? super T> comparator) {
        this.sourceStream = sourceStream;
        array = (T[]) sourceStream.toArray(Object[]::new);
        Arrays.sort(array, comparator);
    }

    @Override
    public boolean next(Predicate<? super T> consumer) {
        while (index < array.length) {
            if (!consumer.test(array[index++])) {
                break;
            }
        }
        return index < array.length;
    }

    @Override
    protected int estimateSize() {
        return sourceStream.estimateSize();
    }

    @Override
    public void close() throws Exception {
        sourceStream.close();
    }
}
