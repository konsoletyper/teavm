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
package org.teavm.classlib.java.util;

import org.teavm.classlib.java.util.function.TDoubleConsumer;

public class TDoubleSummaryStatistics implements TDoubleConsumer {
    private long count;
    private double sum;
    private double min = Double.POSITIVE_INFINITY;
    private double max = Double.NEGATIVE_INFINITY;

    public TDoubleSummaryStatistics() { }

    public TDoubleSummaryStatistics(long count, double min, double max, double sum)
            throws IllegalArgumentException {
        if (count < 0L || count > 0L && min > max) {
            throw new IllegalArgumentException();
        }
        if (count == 0L) {
            return;
        }
        boolean minNan = Double.isNaN(min);
        boolean maxNan = Double.isNaN(max);
        boolean sumNan = Double.isNaN(sum);
        if ((!minNan || !maxNan || !sumNan) && (minNan || maxNan || sumNan)) {
            throw new IllegalArgumentException();
        }

        this.count = count;
        this.sum = sum;
        this.min = min;
        this.max = max;
    }

    @Override
    public void accept(double value) {
        ++count;
        sum = sum + value;
        min = Math.min(min, value);
        max = Math.max(max, value);
    }

    public void combine(TDoubleSummaryStatistics that) {
        this.count = this.count + that.count;
        this.sum = this.sum + that.sum;

        this.min = Math.min(this.min, that.min);
        this.max = Math.max(this.max, that.max);
    }

    public final long getCount() {
        return count;
    }

    public final double getSum() {
        return sum;
    }

    public final double getMin() {
        return min;
    }

    public final double getMax() {
        return max;
    }

    public final double getAverage() {
        return count > 0 ? sum / count : 0.0d;
    }

    @Override
    public String toString() {
        return "DoubleSummaryStatistics{" + "count=" + count + ", sum=" + sum + ", min=" + min
                + ", max=" + max + ", avg=" + getAverage() + "}";
    }
}
