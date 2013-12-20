/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.classlib.impl.charset;

/**
 *
 * @author Alexey Andreev
 */
public class UTF16Helper {
    public static final int SURROGATE_NEUTRAL_BIT_MASK = 0xF800;
    public static final int SURROGATE_BITS = 0xD800;
    public static final int SURROGATE_BIT_MASK = 0xFC00;
    public static final int SURROGATE_BIT_INV_MASK = 0x03FF;
    public static final int HIGH_SURROGATE_BITS = 0xD800;
    public static final int LOW_SURROGATE_BITS = 0xDC00;
    public static final int MEANINGFUL_SURROGATE_BITS = 10;
    public static final int SUPPLEMENTARY_PLANE = 0x10000;

    public static char highSurrogate(int codePoint) {
        return (char)(HIGH_SURROGATE_BITS | (codePoint >> MEANINGFUL_SURROGATE_BITS) & SURROGATE_BIT_INV_MASK);
    }

    public static char lowSurrogate(int codePoint) {
        return (char)(LOW_SURROGATE_BITS | codePoint & SURROGATE_BIT_INV_MASK);
    }

    public static boolean isHighSurrogate(char c) {
        return (c & SURROGATE_BIT_MASK) == HIGH_SURROGATE_BITS;
    }

    public static boolean isLowSurrogate(char c) {
        return (c & SURROGATE_BIT_MASK) == LOW_SURROGATE_BITS;
    }

    public static boolean isSurrogatePair(char a, char b) {
        return isHighSurrogate(a) && isLowSurrogate(b);
    }

    public static int buildCodePoint(char a, char b) {
        return ((a & SURROGATE_BIT_INV_MASK) << MEANINGFUL_SURROGATE_BITS) |
                (b & SURROGATE_BIT_INV_MASK) + SUPPLEMENTARY_PLANE;
    }

    public static boolean isSurrogate(char c) {
        return (c & SURROGATE_NEUTRAL_BIT_MASK) == SURROGATE_BITS;
    }
}
