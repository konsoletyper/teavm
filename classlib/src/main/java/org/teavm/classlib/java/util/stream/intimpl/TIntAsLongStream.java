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

import java.util.function.LongPredicate;
import org.teavm.classlib.java.util.stream.longimpl.TSimpleLongStreamImpl;

public class TIntAsLongStream extends TSimpleLongStreamImpl {
    private TSimpleIntStreamImpl source;

    public TIntAsLongStream(TSimpleIntStreamImpl source) {
        this.source = source;
    }

    @Override
    public boolean next(LongPredicate consumer) {
        return source.next(consumer::test);
    }

    @Override
    protected int estimateSize() {
        return source.estimateSize();
    }

    @Override
    public long count() {
        return source.count();
    }

    @Override
    public void close() throws Exception {
        source.close();
    }
}
