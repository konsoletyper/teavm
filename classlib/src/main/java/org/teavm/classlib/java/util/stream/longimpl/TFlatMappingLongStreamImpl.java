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
package org.teavm.classlib.java.util.stream.longimpl;

import java.util.PrimitiveIterator;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import org.teavm.classlib.java.util.stream.TLongStream;

public class TFlatMappingLongStreamImpl extends TSimpleLongStreamImpl {
    private TSimpleLongStreamImpl sourceStream;
    private boolean currentSet;
    private TLongStream current;
    private PrimitiveIterator.OfLong iterator;
    private LongFunction<? extends TLongStream> mapper;
    private boolean done;

    public TFlatMappingLongStreamImpl(TSimpleLongStreamImpl sourceStream, LongFunction<? extends TLongStream> mapper) {
        this.sourceStream = sourceStream;
        this.mapper = mapper;
    }

    @Override
    public boolean next(LongPredicate consumer) {
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
        if (current instanceof TSimpleLongStreamImpl) {
            TSimpleLongStreamImpl castCurrent = (TSimpleLongStreamImpl) current;
            if (castCurrent.next(consumer)) {
                return true;
            }
            current = null;
        } else {
            iterator = current.iterator();
            while (iterator.hasNext()) {
                long e = iterator.next();
                if (!consumer.test(e)) {
                    return true;
                }
            }
            iterator = null;
            current = null;
        }
        return true;
    }

    @Override
    public void close() throws Exception {
        current = null;
        iterator = null;
        sourceStream.close();
    }
}
