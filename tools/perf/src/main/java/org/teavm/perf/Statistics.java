/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.perf;

import java.util.Arrays;
import java.util.Collection;

/**
 * Computes the same statistics that JMH reports: mean, error at 99.9% confidence level using Student's
 * t-distribution, and percentiles.
 */
public final class Statistics {
    public static final double CONFIDENCE = 0.999;
    public static final double[] PERCENTILES = { 0, 50, 90, 95, 99, 99.9, 99.99, 99.999, 99.9999, 100 };

    private final double[] values;

    public Statistics(Collection<Double> values) {
        this.values = values.stream().mapToDouble(Double::doubleValue).sorted().toArray();
    }

    public int getCount() {
        return values.length;
    }

    public double getMean() {
        if (values.length == 0) {
            return Double.NaN;
        }
        double sum = 0;
        for (var value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    public double getStandardDeviation() {
        if (values.length < 2) {
            return Double.NaN;
        }
        var mean = getMean();
        double sum = 0;
        for (var value : values) {
            sum += (value - mean) * (value - mean);
        }
        return Math.sqrt(sum / (values.length - 1));
    }

    /**
     * Half-width of confidence interval for the mean.
     */
    public double getError() {
        if (values.length < 2) {
            return Double.NaN;
        }
        var t = inverseStudentT(1 - (1 - CONFIDENCE) / 2, values.length - 1);
        return t * getStandardDeviation() / Math.sqrt(values.length);
    }

    public double getMin() {
        return values.length > 0 ? values[0] : Double.NaN;
    }

    public double getMax() {
        return values.length > 0 ? values[values.length - 1] : Double.NaN;
    }

    /**
     * Estimates percentile as {@code (n + 1) * p} position in a sorted array with linear interpolation
     * between neighbour values.
     */
    public double getPercentile(double percentile) {
        if (values.length == 0) {
            return Double.NaN;
        }
        if (values.length == 1) {
            return values[0];
        }
        var position = percentile * (values.length + 1) / 100;
        if (position < 1) {
            return values[0];
        }
        if (position >= values.length) {
            return values[values.length - 1];
        }
        var lowerIndex = (int) Math.floor(position);
        var fraction = position - lowerIndex;
        var lower = values[lowerIndex - 1];
        var upper = values[lowerIndex];
        return lower + fraction * (upper - lower);
    }

    public double[] getValues() {
        return Arrays.copyOf(values, values.length);
    }

    /**
     * Finds such {@code t} that {@code P(T <= t) = p}, where {@code T} has Student's t-distribution with
     * given degrees of freedom.
     */
    public static double inverseStudentT(double p, int degreesOfFreedom) {
        if (p == 0.5) {
            return 0;
        }
        if (p < 0.5) {
            return -inverseStudentT(1 - p, degreesOfFreedom);
        }
        double low = 0;
        double high = 1;
        while (studentTCdf(high, degreesOfFreedom) < p) {
            high *= 2;
            if (high > 1e12) {
                return Double.POSITIVE_INFINITY;
            }
        }
        for (int i = 0; i < 200; ++i) {
            var mid = (low + high) / 2;
            if (studentTCdf(mid, degreesOfFreedom) < p) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return (low + high) / 2;
    }

    static double studentTCdf(double t, int degreesOfFreedom) {
        double v = degreesOfFreedom;
        var x = v / (v + t * t);
        var tail = 0.5 * regularizedIncompleteBeta(x, v / 2, 0.5);
        return t >= 0 ? 1 - tail : tail;
    }

    static double regularizedIncompleteBeta(double x, double a, double b) {
        if (x <= 0) {
            return 0;
        }
        if (x >= 1) {
            return 1;
        }
        var logFront = logGamma(a + b) - logGamma(a) - logGamma(b) + a * Math.log(x) + b * Math.log(1 - x);
        var front = Math.exp(logFront);
        if (x < (a + 1) / (a + b + 2)) {
            return front * betaContinuedFraction(x, a, b) / a;
        } else {
            return 1 - front * betaContinuedFraction(1 - x, b, a) / b;
        }
    }

    // Evaluates continued fraction for incomplete beta function using modified Lentz's method
    private static double betaContinuedFraction(double x, double a, double b) {
        final double tiny = 1e-300;
        final double epsilon = 1e-15;
        double c = 1;
        double d = 1 - (a + b) * x / (a + 1);
        if (Math.abs(d) < tiny) {
            d = tiny;
        }
        d = 1 / d;
        double result = d;
        for (int m = 1; m <= 1000; ++m) {
            var m2 = 2 * m;
            var numerator = m * (b - m) * x / ((a + m2 - 1) * (a + m2));
            d = 1 + numerator * d;
            if (Math.abs(d) < tiny) {
                d = tiny;
            }
            c = 1 + numerator / c;
            if (Math.abs(c) < tiny) {
                c = tiny;
            }
            d = 1 / d;
            result *= d * c;

            numerator = -(a + m) * (a + b + m) * x / ((a + m2) * (a + m2 + 1));
            d = 1 + numerator * d;
            if (Math.abs(d) < tiny) {
                d = tiny;
            }
            c = 1 + numerator / c;
            if (Math.abs(c) < tiny) {
                c = tiny;
            }
            d = 1 / d;
            var delta = d * c;
            result *= delta;
            if (Math.abs(delta - 1) < epsilon) {
                break;
            }
        }
        return result;
    }

    // Lanczos approximation (g = 7, n = 9)
    private static final double[] LANCZOS = {
        0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313,
        -176.61502916214059, 12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6,
        1.5056327351493116e-7
    };

    static double logGamma(double x) {
        if (x < 0.5) {
            return Math.log(Math.PI / Math.abs(Math.sin(Math.PI * x))) - logGamma(1 - x);
        }
        x -= 1;
        var sum = LANCZOS[0];
        for (int i = 1; i < LANCZOS.length; ++i) {
            sum += LANCZOS[i] / (x + i);
        }
        var t = x + 7.5;
        return 0.5 * Math.log(2 * Math.PI) + (x + 0.5) * Math.log(t) - t + Math.log(sum);
    }
}
