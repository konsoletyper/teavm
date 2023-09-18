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

public final class DoubleAnalyzerGenerator {
    private DoubleAnalyzerGenerator() {
    }

    public static void main(String[] args) {
        var mantissaList = new long[660];
        var expList = new long[660];

        var dec = BigInteger.valueOf(1000000000000000000L).shiftLeft(1024 + 64);
        for (var i = 0; i < 330; ++i) {
            var shift = dec.bitLength() - 64;
            mantissaList[330 + i] = dec.shiftRight(shift).longValue();
            dec = dec.divide(BigInteger.valueOf(10));
            var exp = 1024 + 64 - shift;
            expList[330 + i] = 1023 + exp;
        }

        dec = BigInteger.valueOf(1000000000000000000L);
        for (var i = 1; i <= 330; ++i) {
            dec = dec.multiply(BigInteger.valueOf(10));
            var shift = dec.bitLength() - 64;
            mantissaList[330 - i] = dec.shiftRight(shift).longValue();
            expList[330 - i] = 1023 - shift;
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
