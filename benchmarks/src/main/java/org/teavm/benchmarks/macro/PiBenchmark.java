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
package org.teavm.benchmarks.macro;

import java.math.BigInteger;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Macrobenchmark that computes digits of PI using spigot algorithm, which mostly stresses BigInteger
 * arithmetic and memory allocation. The same algorithm is used in {@code samples/pi}.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class PiBenchmark {
    @Param("1000")
    public int digits;

    @Benchmark
    public int computePi() {
        var spigot = new PiDigitSpigot();
        var checksum = 0;
        for (var i = 0; i < digits; ++i) {
            checksum = checksum * 31 + spigot.next();
        }
        return checksum;
    }

    static class PiDigitSpigot {
        private Transformation z = new Transformation(1, 0, 0, 1);
        private final Transformation x = new Transformation(0, 0, 0, 0);
        private final Transformation inverse = new Transformation(0, 0, 0, 0);

        int next() {
            while (true) {
                var y = z.extract(3);
                if (y == z.extract(4)) {
                    z = inverse.qrst(10, -10 * y, 0, 1).compose(z);
                    return y;
                }
                z = z.compose(x.next());
            }
        }
    }

    static class Transformation {
        private BigInteger q;
        private BigInteger r;
        private BigInteger s;
        private BigInteger t;
        private int k;

        Transformation(int q, int r, int s, int t) {
            this(BigInteger.valueOf(q), BigInteger.valueOf(r), BigInteger.valueOf(s), BigInteger.valueOf(t));
        }

        private Transformation(BigInteger q, BigInteger r, BigInteger s, BigInteger t) {
            this.q = q;
            this.r = r;
            this.s = s;
            this.t = t;
        }

        Transformation next() {
            k++;
            q = BigInteger.valueOf(k);
            r = BigInteger.valueOf(4 * k + 2);
            s = BigInteger.ZERO;
            t = BigInteger.valueOf(2 * k + 1);
            return this;
        }

        int extract(int j) {
            var bigj = BigInteger.valueOf(j);
            var numerator = q.multiply(bigj).add(r);
            var denominator = s.multiply(bigj).add(t);
            return numerator.divide(denominator).intValue();
        }

        Transformation qrst(int q, int r, int s, int t) {
            this.q = BigInteger.valueOf(q);
            this.r = BigInteger.valueOf(r);
            this.s = BigInteger.valueOf(s);
            this.t = BigInteger.valueOf(t);
            k = 0;
            return this;
        }

        Transformation compose(Transformation a) {
            return new Transformation(
                    q.multiply(a.q),
                    q.multiply(a.r).add(r.multiply(a.t)),
                    s.multiply(a.q).add(t.multiply(a.s)),
                    s.multiply(a.r).add(t.multiply(a.t))
            );
        }
    }
}
