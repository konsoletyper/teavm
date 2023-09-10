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
package org.teavm.classlib.impl.text;

import java.math.BigInteger;

public final class DoubleSynthesizerGenerator {
    private DoubleSynthesizerGenerator() {
    }

    public static void main(String[] args) {
        var mantissaList = new long[660];
        var expList = new long[660];
        var shift = 122;

        var exp = 0;
        var dec = BigInteger.valueOf(1000000000000000000L);
        for (var i = 0; i < 330; ++i) {
            while (BigInteger.ONE.shiftLeft(shift + exp + 1).divide(dec).bitLength() <= 64) {
                ++exp;
            }
            mantissaList[330 + i] = BigInteger.ONE.shiftLeft(shift + exp).divide(dec).longValue();
            dec = dec.multiply(BigInteger.valueOf(10));
            expList[330 + i] = 1023 - exp;
        }

        exp = 1;
        dec = BigInteger.valueOf(1000000000000000000L).multiply(BigInteger.ONE.shiftLeft(1024));
        var q = BigInteger.valueOf(10L);
        for (var i = 1; i <= 330; ++i) {
            while (BigInteger.ONE.shiftLeft(shift + 1024 - exp).multiply(q).divide(dec).bitLength() > 64) {
                ++exp;
            }
            mantissaList[330 - i] = BigInteger.ONE.shiftLeft(shift + 1024 - exp).multiply(q).divide(dec).longValue();
            q = q.multiply(BigInteger.valueOf(10));
            expList[330 - i] = 1023 + exp;
        }

        System.out.println("[mantissa]");
        for (var value : mantissaList) {
            System.out.println(value + "L,");
        }
        System.out.println();

        System.out.println("[exponent]");
        for (var value : expList) {
            System.out.println(value + ",");
        }
    }
}
