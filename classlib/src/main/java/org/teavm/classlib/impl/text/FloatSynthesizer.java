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
        var indexInTable = FloatAnalyzer.MAX_ABS_DEC_EXP - exp;
        if (mantissa == 0 || indexInTable > mantissa10Table.length || indexInTable < 0) {
            return Float.intBitsToFloat(negative ? (1 << 31) : 0);
        }

        var binMantissa = FloatAnalyzer.mulAndShiftRight(mantissa, mantissa10Table[indexInTable], 0);
        var binExp = exp10Table[indexInTable] - 1;
        while ((binMantissa & (-1L << 30L)) != 0) {
            binMantissa >>>= 1;
            binExp++;
        }
        while (binMantissa < (1L << 29)) {
            binMantissa <<= 1;
            binExp--;
        }
        binExp += 5;
        if (binExp >= 255) {
            return negative ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
        }
        binMantissa += 1 << 5;
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
            -1213479385,
            -1829776968,
            -350662770,
            -1139523676,
            -1770612400,
            -255999462,
            -1063793029,
            -1710027882,
            -159064234,
            -986244846,
            -1647989336,
            -59802560,
            -906835507,
            -1584461865,
            -2126562952,
            -825520345,
            -1519409735,
            -2074521247,
            -742253618,
            -1452796353,
            -2021230542,
            -656988489,
            -1384584251,
            -1966660860,
            -569676998,
            -1314735058,
            -1910781505,
            -480270031,
            -1243209484,
            -1853561046,
            -388717296,
            -1169967296,
            -1794967296,
            -294967296,
            -1094967296,
            -1734967296,
            -198967296,
            -1018167296,
            -1673527296,
            -100663296,
            -939524096,
            -1610612736,
            -2147483648,
            -858993460,
            -1546188227,
            -2095944041,
            -776530088,
            -1480217529,
            -2043167483,
            -692087595,
            -1412663535,
            -1989124287,
            -605618482,
            -1343488245,
            -1933784055,
            -517074110,
            -1272652747,
            -1877115657,
            -426404674,
            -1200117198,
            -1819087218,
            -333559171,
            -1125840796,
            -1759666096,
            -238485376,
            -1049781760,
            -1698818867,
            -141129810,
            -971897307,
            -1636511305,
            -41437710,
            -892143627,
            -1572708361,
            -2117160148,
            -810475859,
            -1507374147,
            -2064892777,
            -726848065,
            -1440471911,
            -2011370988,
            -641213203,
            -1371964022,
            -1956564677,
            -553523105,
            -1301811943,
            -1900443014,
            -463728444,
            -1229976215,
            -1842974431,
            -371778712,
            -1156416429,
            -1784126602,
            -277622186,
            -1081091208,
            -1723866426,
            -181205903,
            -1003958182,
            -1662160005,
            -82475630,
            -924973963,
    };

    private static int[] exp10Table = {
            292,
            289,
            285,
            282,
            279,
            275,
            272,
            269,
            265,
            262,
            259,
            255,
            252,
            249,
            246,
            242,
            239,
            236,
            232,
            229,
            226,
            222,
            219,
            216,
            212,
            209,
            206,
            202,
            199,
            196,
            192,
            189,
            186,
            182,
            179,
            176,
            172,
            169,
            166,
            162,
            159,
            156,
            153,
            149,
            146,
            143,
            139,
            136,
            133,
            129,
            126,
            123,
            119,
            116,
            113,
            109,
            106,
            103,
            99,
            96,
            93,
            89,
            86,
            83,
            79,
            76,
            73,
            69,
            66,
            63,
            59,
            56,
            53,
            50,
            46,
            43,
            40,
            36,
            33,
            30,
            26,
            23,
            20,
            16,
            13,
            10,
            6,
            3,
            0,
            -4,
            -7,
            -10,
            -14,
            -17,
            -20,
            -24,
            -27,
            -30,
            -34,
            -37,
    };
}
