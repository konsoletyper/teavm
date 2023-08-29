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
package org.teavm.classlib.java.util.stream.doubleimpl;

import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;

public class TIterateDoubleStream extends TSimpleDoubleStreamImpl {
    private double value;
    private DoublePredicate pr;
    private DoubleUnaryOperator f;

    public TIterateDoubleStream(double value, DoubleUnaryOperator f) {
        this(value, t -> true, f);
    }

    public TIterateDoubleStream(double value, DoublePredicate pr, DoubleUnaryOperator f) {
        this.value = value;
        this.pr = pr;
        this.f = f;
    }

    @Override
    public boolean next(DoublePredicate consumer) {
        while (true) {
            if (!pr.test(value)) {
                return false;
            }
            double valueToReport = value;
            value = f.applyAsDouble(value);
            if (!consumer.test(valueToReport)) {
                return true;
            }
        }
    }
}
