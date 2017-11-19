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

import java.util.function.Predicate;

public class TLimitingStreamImpl<T> extends TSimpleStreamImpl<T> {
    private TSimpleStreamImpl<T> sourceStream;
    private int limit;
    private int remaining;

    public TLimitingStreamImpl(TSimpleStreamImpl<T> sourceStream, int limit) {
        this.sourceStream = sourceStream;
        this.limit = limit;
        remaining = limit;
    }

    @Override
    public boolean next(Predicate<? super T> consumer) {
        if (remaining == 0) {
            return false;
        }
        boolean result = sourceStream.next(e -> {
            if (remaining-- == 0) {
                return false;
            }
            return consumer.test(e);
        });
        if (!result) {
            remaining = 0;
        }
        return remaining > 0;
    }

    @Override
    protected int estimateSize() {
        int sourceEstimation = sourceStream.estimateSize();
        return sourceEstimation < 0 ? limit : Math.min(limit, sourceEstimation);
    }

    @Override
    public void close() throws Exception {
        sourceStream.close();
    }
}
