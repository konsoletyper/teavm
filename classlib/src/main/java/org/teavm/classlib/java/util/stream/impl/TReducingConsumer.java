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

import java.util.function.BinaryOperator;
import java.util.function.Predicate;

class TReducingConsumer<T> implements Predicate<T> {
    private BinaryOperator<T> accumulator;
    T result;
    boolean initialized;

    TReducingConsumer(BinaryOperator<T> accumulator, T result, boolean initialized) {
        this.accumulator = accumulator;
        this.result = result;
        this.initialized = initialized;
    }

    @Override
    public boolean test(T t) {
        if (!initialized) {
            result = t;
            initialized = true;
        } else {
            result = accumulator.apply(result, t);
        }
        return true;
    }
}
