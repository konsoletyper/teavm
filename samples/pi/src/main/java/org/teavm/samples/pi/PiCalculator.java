/*
 *  Copyright 2023 Alexey Andreev.
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
package org.teavm.samples.pi;

import java.math.BigInteger;

public final class PiCalculator {
    private static final int L = 10;

    private PiCalculator() {
    }

    public static void main(String[] args) {
        var start = System.currentTimeMillis();
        int n = Integer.parseInt(args[0]);
        int j = 0;

        var digits = new PiDigitSpigot();

        while (n > 0) {
            if (n >= L) {
                for (int i = 0; i < L; i++) {
                    System.out.print(digits.next());
                }
                j += L;
            } else {
                for (int i = 0; i < n; i++) {
                    System.out.print(digits.next());
                }
                for (int i = n; i < L; i++) {
                    System.out.print(" ");
                }
                j += n;
            }
            System.out.print("\t:");
            System.out.println(j);
            System.out.flush();
            n -= L;
        }

        System.out.println("Time in millis: " + (System.currentTimeMillis() - start));
    }
}

class PiDigitSpigot {
    private Transformation z;
    private Transformation x;
    private Transformation inverse;

    PiDigitSpigot() {
        z = new Transformation(1, 0, 0, 1);
        x = new Transformation(0, 0, 0, 0);
        inverse = new Transformation(0, 0, 0, 0);
    }

    int next() {
        int y = digit();
        if (isSafe(y)) {
            z = produce(y);
            return y;
        } else {
            z = consume(x.next());
            return next();
        }
    }

    private int digit() {
        return z.extract(3);
    }

    private boolean isSafe(int digit) {
        return digit == z.extract(4);
    }

    private Transformation produce(int i) {
        return inverse.qrst(10, -10 * i, 0, 1).compose(z);
    }

    private Transformation consume(Transformation a) {
        return z.compose(a);
    }
}


class Transformation {
    private BigInteger q;
    private BigInteger r;
    private BigInteger s;
    private BigInteger t;
    private int k;

    Transformation(int q, int r, int s, int t) {
        this.q = BigInteger.valueOf(q);
        this.r = BigInteger.valueOf(r);
        this.s = BigInteger.valueOf(s);
        this.t = BigInteger.valueOf(t);
        k = 0;
    }

    private Transformation(BigInteger q, BigInteger r, BigInteger s, BigInteger t) {
        this.q = q;
        this.r = r;
        this.s = s;
        this.t = t;
        k = 0;
    }

    Transformation next() {
        k++;
        q = BigInteger.valueOf(k);
        r = BigInteger.valueOf(4 * k + 2);
        s = BigInteger.valueOf(0);
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