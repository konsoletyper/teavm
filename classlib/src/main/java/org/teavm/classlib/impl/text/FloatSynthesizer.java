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

public final class FloatSynthesizer {
    private FloatSynthesizer() {
    }

    public static float synthesizeFloat(int mantissa, int exp, boolean negative) {
        var indexInTable = FloatAnalyzer.MAX_ABS_DEC_EXP + exp;
        if (mantissa == 0 || indexInTable > mantissa10Table.length || indexInTable < 0) {
            return Float.intBitsToFloat(negative ? (1 << 31) : 0);
        }

        var binMantissa = FloatAnalyzer.mulAndShiftRight(mantissa, mantissa10Table[indexInTable], 0);
        var binExp = exp10Table[indexInTable] - 1;

        var binMantissaShift = (32 - Integer.numberOfLeadingZeros(binMantissa)) - 30;
        if (binMantissaShift >= 0) {
            binMantissa >>>= binMantissaShift;
        } else {
            binMantissa <<= -binMantissaShift;
        }
        binExp += binMantissaShift;

        if (binExp >= 255) {
            return negative ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
        }
        binMantissa += 1 << 5;
        if ((binMantissa & (-1 << 30)) != 0) {
            binMantissa >>>= 1;
            binExp++;
        }
        if (binExp <= 0) {
            binMantissa >>= Math.min(-binExp + 1, 32);
            binExp = 0;
        }

        binMantissa = (binMantissa >>> 6) & (-1 << 9 >>> 9);
        var iee754 = binMantissa | (binExp << 23);
        if (negative) {
            iee754 ^= 1 << 31;
        }
        return Float.intBitsToFloat(iee754);
    }

    private static final int[] mantissa10Table = {
            -1598972629,
            -924973963,
            -82475629,
            -1662160004,
            -1003958181,
            -181205903,
            -1723866425,
            -1081091207,
            -277622185,
            -1784126602,
            -1156416428,
            -371778711,
            -1842974431,
            -1229976214,
            -463728444,
            -1900443013,
            -1301811943,
            -553523104,
            -1956564676,
            -1371964021,
            -641213203,
            -2011370988,
            -1440471911,
            -726848064,
            -2064892776,
            -1507374146,
            -810475859,
            -2117160148,
            -1572708361,
            -892143627,
            -41437709,
            -1636511304,
            -971897307,
            -141129809,
            -1698818867,
            -1049781759,
            -238485375,
            -1759666096,
            -1125840795,
            -333559170,
            -1819087217,
            -1200117198,
            -426404673,
            -1877115657,
            -1272652747,
            -517074110,
            -1933784055,
            -1343488244,
            -605618481,
            -1989124287,
            -1412663534,
            -692087594,
            -2043167482,
            -1480217529,
            -776530087,
            -2095944040,
            -1546188227,
            -858993459,
            -2147483648,
            -1610612736,
            -939524096,
            -100663296,
            -1673527296,
            -1018167296,
            -198967296,
            -1734967296,
            -1094967296,
            -294967296,
            -1794967296,
            -1169967296,
            -388717296,
            -1853561046,
            -1243209483,
            -480270030,
            -1910781505,
            -1314735057,
            -569676998,
            -1966660859,
            -1384584250,
            -656988489,
            -2021230542,
            -1452796353,
            -742253617,
            -2074521247,
            -1519409734,
            -825520344,
            -2126562951,
            -1584461865,
            -906835507,
            -59802560,
            -1647989336,
            -986244846,
            -159064233,
            -1710027882,
            -1063793028,
            -255999461,
            -1770612399,
            -1139523675,
            -350662770,
            -1829776967,
    };

    private static int[] exp10Table = {
            -35,
            -32,
            -29,
            -25,
            -22,
            -19,
            -15,
            -12,
            -9,
            -5,
            -2,
            1,
            5,
            8,
            11,
            15,
            18,
            21,
            25,
            28,
            31,
            35,
            38,
            41,
            45,
            48,
            51,
            55,
            58,
            61,
            64,
            68,
            71,
            74,
            78,
            81,
            84,
            88,
            91,
            94,
            98,
            101,
            104,
            108,
            111,
            114,
            118,
            121,
            124,
            128,
            131,
            134,
            138,
            141,
            144,
            148,
            151,
            154,
            158,
            161,
            164,
            167,
            171,
            174,
            177,
            181,
            184,
            187,
            191,
            194,
            197,
            201,
            204,
            207,
            211,
            214,
            217,
            221,
            224,
            227,
            231,
            234,
            237,
            241,
            244,
            247,
            251,
            254,
            257,
            260,
            264,
            267,
            270,
            274,
            277,
            280,
            284,
            287,
            290,
            294,
    };
}
