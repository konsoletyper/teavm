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
package org.teavm.classlib.java.lang;

import java.util.function.Consumer;
import org.teavm.classlib.java.util.TIterator;
import org.teavm.classlib.java.util.TSpliterator;

public interface TIterable<T> {
    TIterator<T> iterator();

    default TSpliterator<T> spliterator() {
        TIterator<T> iterator = iterator();
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
            public void forEachRemaining(Consumer<? super T> action) {
                while (iterator.hasNext()) {
                    action.accept(iterator.next());
                }
            }

            @Override
            public TSpliterator<T> trySplit() {
                return null;
            }

            @Override
            public long estimateSize() {
                return -1;
            }

            @Override
            public int characteristics() {
                return 0;
            }
        };
    }
}
