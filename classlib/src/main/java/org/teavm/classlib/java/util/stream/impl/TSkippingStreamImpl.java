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

public class TSkippingStreamImpl<T> extends TSimpleStreamImpl<T> {
    private TSimpleStreamImpl<T> sourceStream;
    private int skip;
    private int remaining;

    public TSkippingStreamImpl(TSimpleStreamImpl<T> sourceStream, int skip) {
        this.sourceStream = sourceStream;
        this.skip = skip;
        remaining = skip;
    }

    @Override
    public boolean next(Predicate<? super T> consumer) {
        if (remaining > 0) {
            if (!sourceStream.next(e -> --remaining > 0)) {
                return false;
            }
        }
        return sourceStream.next(consumer);
    }

    @Override
    protected int estimateSize() {
        int sourceSize = sourceStream.estimateSize();
        return sourceSize >= 0 ? Math.max(0, sourceSize - skip) : -1;
    }

    @Override
    public void close() throws Exception {
        sourceStream.close();
    }
}
