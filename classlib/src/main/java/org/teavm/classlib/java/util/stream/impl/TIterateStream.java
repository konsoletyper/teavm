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
import java.util.function.UnaryOperator;

public class TIterateStream<T> extends TSimpleStreamImpl<T> {
    private T value;
    private Predicate<? super T> pr;
    private UnaryOperator<T> f;

    public TIterateStream(T value, UnaryOperator<T> f) {
        this(value, t -> true, f);
    }

    public TIterateStream(T value, Predicate<? super T> pr, UnaryOperator<T> f) {
        this.value = value;
        this.pr = pr;
        this.f = f;
    }

    @Override
    public boolean next(Predicate<? super T> consumer) {
        while (true) {
            if (!pr.test(value)) {
                return false;
            }
            T valueToReport = value;
            value = f.apply(value);
            if (!consumer.test(valueToReport)) {
                return true;
            }
        }
    }
}
