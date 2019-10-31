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
package org.teavm.classlib.impl;

public final class IntegerUtil {
    private IntegerUtil() {
    }

    public static String toUnsignedLogRadixString(int value, int radixLog2) {
        if (value == 0) {
            return "0";
        }

        int radix = 1 << radixLog2;
        int mask = radix - 1;
        int sz = (Integer.SIZE - Integer.numberOfLeadingZeros(value) + radixLog2 - 1) / radixLog2;
        char[] chars = new char[sz];

        int pos = (sz - 1) * radixLog2;
        int target = 0;
        while (pos >= 0) {
            chars[target++] = Character.forDigit((value >>> pos) & mask, radix);
            pos -= radixLog2;
        }

        return new String(chars);
    }

    public static String toUnsignedLogRadixString(long value, int radixLog2) {
        if (value == 0) {
            return "0";
        }

        int radix = 1 << radixLog2;
        int mask = radix - 1;
        int sz = (Long.SIZE - Long.numberOfLeadingZeros(value) + radixLog2 - 1) / radixLog2;
        char[] chars = new char[sz];

        int pos = (sz - 1) * radixLog2;
        int target = 0;
        while (pos >= 0) {
            chars[target++] = Character.forDigit((int) (value >>> pos) & mask, radix);
            pos -= radixLog2;
        }

        return new String(chars);
    }
}
