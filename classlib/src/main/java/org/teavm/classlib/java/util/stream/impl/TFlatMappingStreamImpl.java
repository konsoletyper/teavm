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
import java.util.function.Function;
import java.util.function.Predicate;
import org.teavm.classlib.java.util.stream.TStream;

public class TFlatMappingStreamImpl<T, S> extends TSimpleStreamImpl<T> {
    private TSimpleStreamImpl<S> sourceStream;
    private boolean currentSet;
    private TStream<? extends T> current;
    private Iterator<? extends T> iterator;
    private Function<? super S, ? extends TStream<? extends T>> mapper;
    private boolean done;

    public TFlatMappingStreamImpl(TSimpleStreamImpl<S> sourceStream,
            Function<? super S, ? extends TStream<? extends T>> mapper) {
        this.sourceStream = sourceStream;
        this.mapper = mapper;
    }

    @Override
    public boolean next(Predicate<? super T> consumer) {
        if (current == null) {
            if (done) {
                return false;
            }
            currentSet = false;
            while (!currentSet) {
                boolean hasMore = sourceStream.next(e -> {
                    current = mapper.apply(e);
                    currentSet = true;
                    return false;
                });
                if (!hasMore) {
                    done = true;
                    break;
                }
            }
            if (current == null) {
                return false;
            }
        }
        if (current instanceof TSimpleStreamImpl) {
            var castCurrent = (TSimpleStreamImpl<? extends T>) current;
            if (castCurrent.next(consumer)) {
                return true;
            }
            current.close();
            current = null;
        } else {
            iterator = current.iterator();
            while (iterator.hasNext()) {
                T e = iterator.next();
                if (!consumer.test(e)) {
                    return true;
                }
            }
            current.close();
            iterator = null;
            current = null;
        }
        return true;
    }

    @Override
    public void close() {
        current = null;
        iterator = null;
        sourceStream.close();
    }
}
