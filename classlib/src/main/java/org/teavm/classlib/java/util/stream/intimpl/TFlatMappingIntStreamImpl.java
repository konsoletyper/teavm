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
package org.teavm.classlib.java.util.stream.intimpl;

import java.util.PrimitiveIterator;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import org.teavm.classlib.java.util.stream.TIntStream;

public class TFlatMappingIntStreamImpl extends TSimpleIntStreamImpl {
    private TSimpleIntStreamImpl sourceStream;
    private boolean currentSet;
    private TIntStream current;
    private PrimitiveIterator.OfInt iterator;
    private IntFunction<? extends TIntStream> mapper;
    private boolean done;

    public TFlatMappingIntStreamImpl(TSimpleIntStreamImpl sourceStream, IntFunction<? extends TIntStream> mapper) {
        this.sourceStream = sourceStream;
        this.mapper = mapper;
    }

    @Override
    public boolean next(IntPredicate consumer) {
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
        if (current instanceof TSimpleIntStreamImpl) {
            TSimpleIntStreamImpl castCurrent = (TSimpleIntStreamImpl) current;
            if (castCurrent.next(consumer)) {
                return true;
            }
            current.close();
            current = null;
        } else {
            iterator = current.iterator();
            while (iterator.hasNext()) {
                int e = iterator.next();
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
    public void close()  {
        current = null;
        iterator = null;
        sourceStream.close();
    }
}
