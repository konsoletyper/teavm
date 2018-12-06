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

public final class DoubleAnalyzer {
    private DoubleAnalyzer() {
    }

    private static final int MAX_ABS_DEC_EXP = 330;
    public static final int DECIMAL_PRECISION = 18;
    public static final long DOUBLE_MAX_POS = 100000000000000000L;
    private static long[] mantissa10Table = new long[MAX_ABS_DEC_EXP * 2];
    private static int[] exp10Table = new int[MAX_ABS_DEC_EXP * 2];

    static {
        long decimalMantissaOne = 8000000000000000000L;

        long mantissa = decimalMantissaOne;
        long remainder = 0;
        int exponent = 1023;

        for (int i = 0; i < MAX_ABS_DEC_EXP; ++i) {
            mantissa10Table[i + MAX_ABS_DEC_EXP] = Long.divideUnsigned(mantissa, 80);
            exp10Table[i + MAX_ABS_DEC_EXP] = exponent;

            mantissa = Long.divideUnsigned(mantissa, 10);
            remainder = Long.remainderUnsigned(mantissa, 10);
            while (mantissa <= decimalMantissaOne && (mantissa & (1L << 63)) == 0) {
                mantissa <<= 1;
                exponent++;
                remainder <<= 1;
            }
            mantissa += remainder / 10;
        }

        long maxMantissa = Long.MAX_VALUE / 10;
        mantissa = decimalMantissaOne;
        exponent = 1023;
        for (int i = 0; i < MAX_ABS_DEC_EXP; ++i) {
            long nextMantissa = mantissa;
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

            mantissa10Table[MAX_ABS_DEC_EXP - i - 1] = Long.divideUnsigned(mantissa, 80);
            exp10Table[MAX_ABS_DEC_EXP - i - 1] = exponent;
        }
    }

    public static void analyze(double d, Result result) {
        long bits = Double.doubleToLongBits(d);
        result.sign = (bits & (1L << 63)) != 0;
        long mantissa = bits & ((1L << 52) - 1);
        int exponent = (int) (bits >> 52) & ((1 << 11) - 1);
        if (mantissa == 0 && exponent == 0) {
            result.mantissa = 0;
            result.exponent = 0;
            return;
        }

        int errorShift = 0;
        if (exponent == 0) {
            mantissa <<= 1;
            while ((mantissa & (1L << 52)) == 0) {
                mantissa <<= 1;
                exponent--;
                ++errorShift;
            }
        } else {
            mantissa |= 1L << 52;
        }

        int decExponent = Arrays.binarySearch(exp10Table, exponent);
        if (decExponent < 0) {
            decExponent = -decExponent - 2;
        }
        int binExponentCorrection = exponent - exp10Table[decExponent];
        int mantissaShift = 12 + binExponentCorrection;

        long decMantissa = mulAndShiftRight(mantissa, mantissa10Table[decExponent], mantissaShift);
        if (decMantissa >= 1000000000000000000L) {
            ++decExponent;
            binExponentCorrection = exponent - exp10Table[decExponent];
            mantissaShift = 12 + binExponentCorrection;
            decMantissa = mulAndShiftRight(mantissa, mantissa10Table[decExponent], mantissaShift);
        }

        long error = mantissa10Table[decExponent] >>> (63 - mantissaShift - errorShift);
        long upError = (error + 1) >> 1;
        long downError = error >> 1;
        if (mantissa == (1L << 52)) {
            downError >>= 2;
        }

        long lowerPos = findLowerDistanceToZero(decMantissa, downError);
        long upperPos = findUpperDistanceToZero(decMantissa, upError);
        if (lowerPos > upperPos) {
            decMantissa = (decMantissa / lowerPos) * lowerPos;
        } else if (lowerPos < upperPos) {
            decMantissa = (decMantissa / upperPos) * upperPos + upperPos;
        } else {
            decMantissa = ((decMantissa + upperPos / 2) / upperPos) * upperPos;
        }

        if (decMantissa >= 1000000000000000000L) {
            decExponent++;
            decMantissa /= 10;
        } else if (decMantissa < 100000000000000000L) {
            decExponent--;
            decMantissa *= 10;
        }

        result.mantissa = decMantissa;
        result.exponent = decExponent - MAX_ABS_DEC_EXP;
    }

    private static long findLowerDistanceToZero(long mantissa, long error) {
        long pos = 10;
        while (pos <= error) {
            pos *= 10;
        }
        long mantissaRight = mantissa % pos;
        if (mantissaRight >= error / 2) {
            pos /= 10;
        }
        return pos;
    }

    private static long findUpperDistanceToZero(long mantissa, long error) {
        long pos = 1;
        while (pos <= error) {
            pos *= 10;
        }
        long mantissaRight = mantissa % pos;
        if (pos - mantissaRight > error / 2) {
            pos /= 10;
        }
        return pos;
    }

    // Multiply two longs and shft result right by 64-shift bits.
    private static long mulAndShiftRight(long a, long b, int shift) {
        long a1 = a & 0xFFFF;
        long a2 = (a >>> 16) & 0xFFFF;
        long a3 = (a >>> 32) & 0xFFFF;
        long a4 = (a >>> 48) & 0xFFFF;

        long b1 = b & 0xFFFF;
        long b2 = (b >>> 16) & 0xFFFF;
        long b3 = (b >>> 32) & 0xFFFF;
        long b4 = (b >>> 48) & 0xFFFF;

        long cm = b3 * a1 + b2 * a2 + b1 * a3;
        long c0 = b4 * a1 + b3 * a2 + b2 * a3 + b1 * a4;
        long c1 = b4 * a2 + b3 * a3 + b2 * a4;
        long c2 = b4 * a3 + b3 * a4;
        long c3 = b4 * a4;

        long c = (c3 << (32 + shift)) + (c2 << (16 + shift)) + (c1 << shift);
        if (shift <= 16) {
            c += c0 >>> (16 - shift);
        } else {
            c += c0 << (shift - 16);
        }
        c += cm >>> (32 - shift);

        return c;
    }

    public static class Result {
        public long mantissa;
        public int exponent;
        public boolean sign;
    }
}
