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

import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;

public class TIterateLongStream extends TSimpleLongStreamImpl {
    private long value;
    private LongPredicate pr;
    private LongUnaryOperator f;

    public TIterateLongStream(long value, LongUnaryOperator f) {
        this(value, t -> true, f);
    }

    public TIterateLongStream(long value, LongPredicate pr, LongUnaryOperator f) {
        this.value = value;
        this.pr = pr;
        this.f = f;
    }

    @Override
    public boolean next(LongPredicate consumer) {
        while (true) {
            if (!pr.test(value)) {
                return false;
            }
            long valueToReport = value;
            value = f.applyAsLong(value);
            if (!consumer.test(valueToReport)) {
                return true;
            }
        }
    }
}
