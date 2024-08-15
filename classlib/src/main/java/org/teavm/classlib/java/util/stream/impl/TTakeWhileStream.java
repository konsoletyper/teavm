/*
 *  Copyright 2022 ulugbek.abdullaev.
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

public class TTakeWhileStream<T> extends TSimpleStreamImpl<T> {
    private TSimpleStreamImpl<T> sourceStream;
    private Predicate<? super T> predicate;

    /* set to `true` as soon as we see a value `v` in the source stream for which `predicate.test(v)` is false */
    private boolean isStopped;

    TTakeWhileStream(TSimpleStreamImpl<T> sourceStream, Predicate<? super T> predicate) {
        this.sourceStream = sourceStream;
        this.predicate = predicate;
    }

    @Override
    public boolean next(Predicate<? super T> consumer) {
        if (isStopped) {
            return false;
        }
        var result = sourceStream.next(e -> {
            if (!predicate.test(e)) {
                isStopped = true;
                return false;
            }
            return consumer.test(e);
        });
        if (!result) {
            isStopped = true;
        }
        return result;
    }
}
