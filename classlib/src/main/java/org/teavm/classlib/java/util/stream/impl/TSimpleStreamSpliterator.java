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

public class TSimpleStreamSpliterator<T> implements Spliterator<T> {
    private TSimpleStreamImpl<T> stream;
    private boolean foundItems;
    private boolean done;

    public TSimpleStreamSpliterator(TSimpleStreamImpl<T> stream) {
        this.stream = stream;
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        while (true) {
            boolean hasMore = stream.next(x -> {
                action.accept(x);
                return true;
            });
            if (!hasMore) {
                break;
            }
        }
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (done) {
            return false;
        }
        foundItems = false;
        while (!foundItems && !done) {
            done = !stream.next(x -> {
                action.accept(x);
                foundItems = true;
                return false;
            });
        }
        return true;
    }

    @Override
    public Spliterator<T> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return stream.estimateSize();
    }

    @Override
    public int characteristics() {
        return 0;
    }
}
