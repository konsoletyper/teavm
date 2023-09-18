/*
 *  Copyright 2023 konsoletyper.
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

public final class FloatAnalyzerGenerator {
    private FloatAnalyzerGenerator() {
    }

    public static void main(String[] args) {
        var mantissaList = new int[100];
        var expList = new int[100];

        var dec = BigInteger.valueOf(1000000000).shiftLeft(128 + 32);
        for (var i = 0; i < 50; ++i) {
            var shift = dec.bitLength() - 32;
            mantissaList[50 + i] = dec.shiftRight(shift).intValue();
            dec = dec.divide(BigInteger.valueOf(10));
            var exp = 128 + 32 - shift;
            expList[50 + i] = 127 + exp;
        }

        dec = BigInteger.valueOf(1000000000);
        for (var i = 1; i <= 50; ++i) {
            dec = dec.multiply(BigInteger.valueOf(10));
            var shift = dec.bitLength() - 32;
            mantissaList[50 - i] = dec.shiftRight(shift).intValue();
            expList[50 - i] = 127 - shift;
        }

        System.out.println("[mantissa]");
        for (var value : mantissaList) {
            System.out.println(value + ",");
        }
        System.out.println();

        System.out.println("[exponent]");
        for (var value : expList) {
            System.out.println(value + ",");
        }
    }
}
