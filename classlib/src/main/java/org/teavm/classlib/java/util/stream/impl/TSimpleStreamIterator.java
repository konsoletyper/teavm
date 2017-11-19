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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

public class TSimpleStreamIterator<T> implements Iterator<T> {
    private static final byte NEEDS_MORE = 0;
    private static final byte HAS_DATA = 1;
    private static final byte LAST_ELEMENT = 2;
    private static final byte DONE = 3;

    private TSimpleStreamImpl<T> stream;
    private T lastElement;
    private byte state;

    public TSimpleStreamIterator(TSimpleStreamImpl<T> stream) {
        this.stream = stream;
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        stream.next(x -> {
            action.accept(x);
            return true;
        });
    }

    @Override
    public boolean hasNext() {
        fetchIfNeeded();
        return state != DONE;
    }

    @Override
    public T next() {
        fetchIfNeeded();
        if (state == DONE) {
            throw new NoSuchElementException();
        }
        T result = lastElement;
        lastElement = null;
        state = state == LAST_ELEMENT ? DONE : NEEDS_MORE;
        return result;
    }

    private void fetchIfNeeded() {
        if (state != NEEDS_MORE) {
            return;
        }
        boolean hasMore = stream.next(e -> {
            lastElement = e;
            return false;
        });
        state = hasMore ? HAS_DATA : LAST_ELEMENT;
        if (state == LAST_ELEMENT) {
            stream = null;
        }
    }
}
