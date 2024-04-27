/*
 *  Copyright 2023 ihromant.
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
package org.teavm.classlib.java.util.stream.doubleimpl;

import java.util.function.DoublePredicate;

public class TDropWhileDoubleStream extends TSimpleDoubleStreamImpl {
    private TSimpleDoubleStreamImpl sourceStream;
    private DoublePredicate predicate;

    /* set to `true` as soon as we see a value `v` in the source stream for which `predicate.test(v)` is true */
    private boolean isStarted;

    TDropWhileDoubleStream(TSimpleDoubleStreamImpl sourceStream, DoublePredicate predicate) {
        this.sourceStream = sourceStream;
        this.predicate = predicate;
    }

    @Override
    public boolean next(DoublePredicate consumer) {
        if (!isStarted) {
            var skippingPredicate = new DoublePredicate() {
                boolean consumerCanTakeMore;

                @Override
                public boolean test(double t) {
                    if (predicate.test(t)) {
                        return true;
                    }
                    isStarted = true;
                    consumerCanTakeMore = consumer.test(t);
                    return false;
                }
            };
            var result = sourceStream.next(skippingPredicate);
            if (!result) {
                return false;
            }
            if (!skippingPredicate.consumerCanTakeMore) {
                return true;
            }
        }
        return sourceStream.next(consumer);
    }
}
