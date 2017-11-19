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

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TStreamOverSpliterator<T> extends TSimpleStreamImpl<T> {
    private Spliterator<T> spliterator;

    public TStreamOverSpliterator(Spliterator<T> spliterator) {
        this.spliterator = spliterator;
    }

    @Override
    public boolean next(Predicate<? super T> consumer) {
        AdapterAction<T> action = new AdapterAction<>(consumer);
        while (spliterator.tryAdvance(action)) {
            if (!action.wantsMore) {
                return true;
            }
        }
        return false;
    }

    static class AdapterAction<T> implements Consumer<T> {
        private Predicate<? super T> consumer;
        boolean wantsMore;

        AdapterAction(Predicate<? super T> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void accept(T t) {
            wantsMore = consumer.test(t);
        }
    }

    @Override
    protected int estimateSize() {
        return (int) spliterator.estimateSize();
    }

    @Override
    public long count() {
        return spliterator.hasCharacteristics(Spliterator.SIZED) ? (int) spliterator.estimateSize() : super.count();
    }
}
