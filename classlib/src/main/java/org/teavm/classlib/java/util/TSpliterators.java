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

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class TSpliterators {

    private TSpliterators() {
    }

    public static <T> TSpliterator<T> spliterator(Object[] array, int additionalCharacteristics) {
        return new TSpliterator<T>() {
            private int index;

            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                if (index >= array.length) {
                    return false;
                }

                @SuppressWarnings("unchecked")
                T e = (T) array[index++];
                action.accept(e);
                return true;
            }

            @Override
            public TSpliterator<T> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                return array.length - index;
            }

            @Override
            public int characteristics() {
                return additionalCharacteristics | TSpliterator.SIZED;
            }
        };
    }
    
    public static <T> TSpliterator<T> spliterator(Collection<? extends T> c, int characteristics) {
        return spliterator(c.iterator(), c.size(), characteristics);
    }

    public static TSpliterator.OfInt spliterator(int[] array, int additionalCharacteristics) {
        return spliterator(array, 0, array.length, additionalCharacteristics);
    }

    public static TSpliterator.OfInt spliterator(int[] array, int fromIndex, int toIndex,
            int additionalCharacteristics) {
        return new TSpliterator.OfInt() {
            int index = fromIndex;

            @Override
            public TSpliterator.OfInt trySplit() {
                return null;
            }

            @Override
            public boolean tryAdvance(IntConsumer action) {
                if (index < toIndex) {
                    action.accept(array[index++]);
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public long estimateSize() {
                return toIndex - index;
            }

            @Override
            public int characteristics() {
                return TSpliterator.SIZED;
            }
        };
    }

    public static <T> TSpliterator<T> spliterator(Iterator<? extends T> iterator, long size, int characteristics) {
        return new TSpliterator<T>() {
            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                if (iterator.hasNext()) {
                    action.accept(iterator.next());
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public TSpliterator<T> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                return size;
            }

            @Override
            public int characteristics() {
                return characteristics;
            }
        };
    }
}
