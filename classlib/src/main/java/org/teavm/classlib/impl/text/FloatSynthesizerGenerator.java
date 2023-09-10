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

public final class FloatSynthesizerGenerator {
    private FloatSynthesizerGenerator() {
    }

    public static void main(String[] args) {
        var mantissaList = new int[100];
        var expList = new int[100];
        var shift = 57;

        var exp = 0;
        var dec = BigInteger.valueOf(100000000);
        for (var i = 0; i < 50; ++i) {
            while (BigInteger.ONE.shiftLeft(shift + exp + 1).divide(dec).bitLength() <= 32) {
                ++exp;
            }
            mantissaList[50 + i] = BigInteger.ONE.shiftLeft(shift + exp).divide(dec).intValue();
            dec = dec.multiply(BigInteger.valueOf(10));
            expList[50 + i] = 127 - exp;
        }

        exp = 1;
        dec = BigInteger.valueOf(100000000).multiply(BigInteger.ONE.shiftLeft(128));
        var q = BigInteger.valueOf(10L);
        for (var i = 1; i <= 50; ++i) {
            while (BigInteger.ONE.shiftLeft(shift + 128 - exp).multiply(q).divide(dec).bitLength() > 32) {
                ++exp;
            }
            mantissaList[50 - i] = BigInteger.ONE.shiftLeft(shift + 128 - exp).multiply(q).divide(dec).intValue();
            q = q.multiply(BigInteger.valueOf(10));
            expList[50 - i] = 127 + exp;
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
