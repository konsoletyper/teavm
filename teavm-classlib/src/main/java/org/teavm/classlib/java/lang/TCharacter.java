/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

import org.teavm.classlib.impl.unicode.UnicodeHelper;
import org.teavm.dependency.PluggableDependency;
import org.teavm.javascript.ni.GeneratedBy;

/**
 *
 * @author Alexey Andreev
 */
public class TCharacter {
    public static final int MIN_RADIX = 2;
    public static final int MAX_RADIX = 36;
    private static int[] digitMapping;

    @GeneratedBy(CharacterNativeGenerator.class)
    public static native char toLowerCase(char ch);

    @GeneratedBy(CharacterNativeGenerator.class)
    public static native int toLowerCase(int ch);

    public static int digit(char ch, int radix) {
        return digit((int)ch, radix);
    }

    public static int digit(int codePoint, int radix) {
        if (radix < MIN_RADIX || radix > MAX_RADIX) {
            return -1;
        }
        int d = digit(codePoint);
        return d <= radix ? d : -1;
    }

    static int digit(int codePoint) {
        int[] digitMapping = getDigitMapping();
        int l = 0;
        int u = (digitMapping.length / 2) - 1;
        while (u >= l) {
            int idx = (l + u) / 2;
            int val = digitMapping[idx * 2];
            if (codePoint > val) {
                l = idx + 1;
            } else if (codePoint < val) {
                u = idx - 1;
            } else {
                return digitMapping[idx * 2 + 1];
            }
        }
        return -1;
    }

    public static char forDigit(int digit, int radix) {
        if (radix < MIN_RADIX || radix > MAX_RADIX || digit >= radix) {
            return '\0';
        }
        return digit < 10 ? (char)('0' + digit) : (char)('a' + digit - 10);
    }

    private static int[] getDigitMapping() {
        if (digitMapping == null) {
            digitMapping = UnicodeHelper.decodeIntByte(obtainDigitMapping());
        }
        return digitMapping;
    }

    @GeneratedBy(CharacterNativeGenerator.class)
    @PluggableDependency(CharacterNativeGenerator.class)
    private static native String obtainDigitMapping();
}
