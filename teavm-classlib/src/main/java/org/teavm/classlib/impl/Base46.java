/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.classlib.impl;

/**
 * <p>Base47 encoding is best fit for encoding varible length numbers in JavaScript strings.</p>
 *
 * <p>47 = (int)(93 / 2), where 94 is the number of ASCII characters representable in JavaScript string
 * without escaping. These characters are encoded by one byte in UTF-8 charset. All other character require
 * either escaping or two or more bytes in UTF-8.</p>
 *
 * <p>We divide 93 by 2 for the following trick. Representing integers takes 5 bytes in Base93. However,
 * we often need smaller integers that might be represented by one or two bytes. By each Base93 digit we
 * can encode both part of the number and a flag indicating whether the number contains one more digit.</p>
 *
 * @author Alexey Andreev
 */
public final class Base46 {
    private Base46() {
    }

    public static void encodeUnsigned(StringBuilder sb, int number) {
        boolean hasMore;
        do {
            int digit = number % 46;
            number /= 46;
            hasMore = number > 0;
            digit = digit * 2 + (hasMore ? 1 : 0);
            sb.append(encodeDigit(digit));
        } while (hasMore);
    }

    public static void encode(StringBuilder sb, int number) {
        encodeUnsigned(sb, Math.abs(number) * 2 + (number >= 0 ? 0 : 1));
    }

    public static void encodeUnsigned(StringBuilder sb, long number) {
        boolean hasMore;
        do {
            int digit = (int) (number % 46);
            number /= 46;
            hasMore = number > 0;
            digit = digit * 2 + (hasMore ? 1 : 0);
            sb.append(encodeDigit(digit));
        } while (hasMore);
    }

    public static void encode(StringBuilder sb, long number) {
        encodeUnsigned(sb, Math.abs(number) * 2 + (number >= 0 ? 0 : 1));
    }

    public static int decodeUnsigned(CharFlow seq) {
        int number = 0;
        int pos = 1;
        boolean hasMore;
        do {
            int digit = decodeDigit(seq.characters[seq.pointer++]);
            hasMore = digit % 2 == 1;
            number += pos * (digit / 2);
            pos *= 46;
        } while (hasMore);
        return number;
    }

    public static int decode(CharFlow seq) {
        int number = decodeUnsigned(seq);
        int result = number / 2;
        if (number % 2 != 0) {
            result = -result;
        }
        return result;
    }

    public static long decodeUnsignedLong(CharFlow seq) {
        long number = 0;
        long pos = 1;
        boolean hasMore;
        do {
            int digit = decodeDigit(seq.characters[seq.pointer++]);
            hasMore = digit % 2 == 1;
            number += pos * (digit / 2);
            pos *= 46;
        } while (hasMore);
        return number;
    }

    public static long decodeLong(CharFlow seq) {
        long number = decodeUnsigned(seq);
        long result = number / 2;
        if (number % 2 != 0) {
            result = -result;
        }
        return result;
    }

    public static char encodeDigit(int digit) {
        if (digit < 2) {
            return (char) (digit + ' ');
        } else if (digit < 59) {
            return (char) (digit + 1 + ' ');
        } else {
            return (char) (digit + 2 + ' ');
        }
    }

    public static int decodeDigit(char c) {
        if (c < '"') {
            return c - ' ';
        } else if (c < '\\') {
            return c - ' ' - 1;
        } else {
            return c - ' ' - 2;
        }
    }
}
