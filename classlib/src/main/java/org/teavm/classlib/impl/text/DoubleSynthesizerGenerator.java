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

        var binOneShift = 1024 + 256;
        var binOne = BigInteger.ONE.shiftLeft(binOneShift);
        var dec = BigInteger.valueOf(1000000000000000000L);
        for (var i = 0; i <= 330; ++i) {
            var quot = binOne.divide(dec);
            mantissaList[330 - i] = extractLong(quot);
            var exp = quot.bitLength() - binOneShift + 57;
            expList[330 - i] = 1023 + exp;
            dec = dec.multiply(BigInteger.valueOf(10));
        }

        dec = BigInteger.valueOf(1000000000000000000L);
        var q = BigInteger.TEN;
        for (var i = 1; i < 330; ++i) {
            var quot = q.shiftLeft(binOneShift).divide(dec);
            mantissaList[330 + i] = extractLong(quot);
            var exp = quot.bitLength() - binOneShift + 57;
            expList[330 + i] = 1023 + exp;
            q = q.multiply(BigInteger.TEN);
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

    private static long extractLong(BigInteger n) {
        return n.shiftRight(n.bitLength() - 65).add(BigInteger.ONE).shiftRight(1).longValue();
    }
}
