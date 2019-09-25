/*
 *  Copyright 2018 Alexey Andreev.
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

import java.util.Arrays;

public final class FloatAnalyzer {
    public static final int PRECISION = 9;
    public static final int MAX_POS = 100000000;

    private static final int MAX_ABS_DEC_EXP = 50;
    private static final int[] mantissa10Table = new int[MAX_ABS_DEC_EXP * 2];
    private static final int[] exp10Table = new int[MAX_ABS_DEC_EXP * 2];

    private FloatAnalyzer() {
    }

    static {
        int decMantissaOne = 2000000000;

        int mantissa = decMantissaOne;
        int exponent = 127;

        for (int i = 0; i < MAX_ABS_DEC_EXP; ++i) {
            mantissa10Table[i + MAX_ABS_DEC_EXP] = Integer.divideUnsigned(mantissa, 20);
            exp10Table[i + MAX_ABS_DEC_EXP] = exponent;

            mantissa = Integer.divideUnsigned(mantissa, 10);
            int remainder = Integer.remainderUnsigned(mantissa, 10);
            while (mantissa <= decMantissaOne && (mantissa & (1 << 31)) == 0) {
                mantissa <<= 1;
                exponent++;
                remainder <<= 1;
            }
            mantissa += remainder / 10;
        }

        int maxMantissa = Integer.MAX_VALUE / 10;
        mantissa = decMantissaOne;
        exponent = 127;
        for (int i = 0; i < MAX_ABS_DEC_EXP; ++i) {
            int nextMantissa = mantissa;
            int shift = 0;
            while (nextMantissa > maxMantissa) {
                nextMantissa >>= 1;
                shift++;
                exponent--;
            }

            nextMantissa *= 10;
            if (shift > 0) {
                long shiftedOffPart = mantissa & ((1 << shift) - 1);
                nextMantissa += (shiftedOffPart * 10) >> shift;
            }
            mantissa = nextMantissa;

            mantissa10Table[MAX_ABS_DEC_EXP - i - 1] = Integer.divideUnsigned(mantissa, 20);
            exp10Table[MAX_ABS_DEC_EXP - i - 1] = exponent;
        }
    }

    public static void analyze(float d, Result result) {
        int bits = Float.floatToIntBits(d);
        result.sign = (bits & (1 << 31)) != 0;
        int mantissa = bits & ((1 << 23) - 1);
        int exponent = (bits >> 23) & ((1 << 8) - 1);
        if (mantissa == 0 && exponent == 0) {
            result.mantissa = 0;
            result.exponent = 0;
            return;
        }

        int errorShift = 0;
        if (exponent == 0) {
            mantissa <<= 1;
            while ((mantissa & (1L << 23)) == 0) {
                mantissa <<= 1;
                exponent--;
                ++errorShift;
            }
        } else {
            mantissa |= 1 << 23;
        }

        int decExponent = Arrays.binarySearch(exp10Table, exponent);
        if (decExponent < 0) {
            decExponent = -decExponent - 2;
        }
        int binExponentCorrection = exponent - exp10Table[decExponent];
        int mantissaShift = 9 + binExponentCorrection;

        int decMantissa = (int) (((long) mantissa * mantissa10Table[decExponent]) >>> (32 - mantissaShift));
        if (decMantissa >= 1000000000) {
            ++decExponent;
            binExponentCorrection = exponent - exp10Table[decExponent];
            mantissaShift = 9 + binExponentCorrection;
            decMantissa = (int) (((long) mantissa * mantissa10Table[decExponent]) >>> (32 - mantissaShift));
        }

        errorShift = 31 - mantissaShift - errorShift;
        int error = errorShift >= 0
                ? mantissa10Table[decExponent] >>> errorShift
                : mantissa10Table[decExponent] << (-errorShift);
        int upError = (error + 1) >> 1;
        int downError = error >> 1;
        if (mantissa == (1 << 22)) {
            downError >>= 2;
        }

        int lowerPos = findLowerDistanceToZero(decMantissa, downError);
        int upperPos = findUpperDistanceToZero(decMantissa, upError);
        if (lowerPos > upperPos) {
            decMantissa = (decMantissa / lowerPos) * lowerPos;
        } else if (lowerPos < upperPos) {
            decMantissa = (decMantissa / upperPos) * upperPos + upperPos;
        } else {
            decMantissa = ((decMantissa + upperPos / 2) / upperPos) * upperPos;
        }

        if (decMantissa >= 1000000000) {
            decExponent++;
            decMantissa /= 10;
        } else if (decMantissa < 100000000) {
            decExponent--;
            decMantissa *= 10;
        }

        result.mantissa = decMantissa;
        result.exponent = decExponent - MAX_ABS_DEC_EXP;
    }

    private static int findLowerDistanceToZero(int mantissa, int error) {
        int pos = 10;
        while (pos <= error) {
            pos *= 10;
        }
        int mantissaRight = mantissa % pos;
        if (mantissaRight >= error / 2) {
            pos /= 10;
        }
        return pos;
    }

    private static int findUpperDistanceToZero(int mantissa, int error) {
        int pos = 10;
        while (pos <= error) {
            pos *= 10;
        }
        int mantissaRight = mantissa % pos;
        if (pos - mantissaRight > error / 2) {
            pos /= 10;
        }
        return pos;
    }

    public static class Result {
        public int mantissa;
        public int exponent;
        public boolean sign;
    }
}
