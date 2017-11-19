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

import java.util.function.DoublePredicate;
import java.util.function.ToDoubleFunction;
import org.teavm.classlib.java.util.stream.doubleimpl.TSimpleDoubleStreamImpl;

public class TMappingToDoubleStreamImpl<T> extends TSimpleDoubleStreamImpl {
    private TSimpleStreamImpl<T> source;
    private ToDoubleFunction<? super T> mapper;

    public TMappingToDoubleStreamImpl(TSimpleStreamImpl<T> source, ToDoubleFunction<? super T> mapper) {
        this.source = source;
        this.mapper = mapper;
    }

    @Override
    public boolean next(DoublePredicate consumer) {
        return source.next(e -> consumer.test(mapper.applyAsDouble(e)));
    }

    @Override
    public void close() throws Exception {
        source.close();
    }

    @Override
    protected int estimateSize() {
        return source.estimateSize();
    }

    @Override
    public long count() {
        return source.count();
    }
}
