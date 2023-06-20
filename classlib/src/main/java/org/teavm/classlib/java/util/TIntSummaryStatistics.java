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

import org.teavm.classlib.java.util.function.TIntConsumer;

public class TIntSummaryStatistics implements TIntConsumer {
    private long count;
    private long sum;
    private int min = Integer.MAX_VALUE;
    private int max = Integer.MIN_VALUE;

    public TIntSummaryStatistics() {

    }

    public TIntSummaryStatistics(long count, int min, int max, long sum)
            throws IllegalArgumentException {
        if (count < 0L || count > 0L && min > max) {
            throw new IllegalArgumentException();
        }
        if (count == 0L) {
            return;
        }
        this.count = count;
        this.sum = sum;
        this.min = min;
        this.max = max;
    }

    @Override
    public void accept(int value) {
        ++count;
        sum = sum + value;
        min = Math.min(min, value);
        max = Math.max(max, value);
    }

    public void combine(TIntSummaryStatistics that) {
        this.count = this.count + that.count;
        this.sum = this.sum + that.sum;
        this.min = Math.min(this.min, that.min);
        this.max = Math.max(this.max, that.max);
    }

    public final long getCount() {
        return count;
    }

    public final long getSum() {
        return sum;
    }

    public final int getMin() {
        return min;
    }

    public final int getMax() {
        return max;
    }

    public final double getAverage() {
        return count > 0 ? (double) sum / count : 0.0d;
    }

    @Override
    public String toString() {
        return "IntSummaryStatistics{" + "count=" + count + ", sum=" + sum + ", min=" + min
                + ", max=" + max + ", avg=" + getAverage() + "}";
    }
}
