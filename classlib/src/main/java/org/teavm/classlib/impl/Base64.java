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

import java.util.Arrays;

/**
 *
 * @author Alexey Andreev
 */
public class Base64 {
    private static char[] alphabet = new char[64];
    private static int[] reverse = new int[256];

    static {
        int i = 0;
        for (char c = 'A'; c <= 'Z'; ++c) {
            alphabet[i++] = c;
        }
        for (char c = 'a'; c <= 'z'; ++c) {
            alphabet[i++] = c;
        }
        for (char c = '0'; c <= '9'; ++c) {
            alphabet[i++] = c;
        }
        alphabet[i++] = '+';
        alphabet[i++] = '/';

        Arrays.fill(reverse, -1);
        for (i = 0; i < alphabet.length; ++i) {
            reverse[alphabet[i]] = i;
        }
    }

    public static byte[] decode(String text) {
        int outputSize = ((text.length() - 1) / 4 + 1) * 3;
        int i, j;
        for (i = text.length() - 1; i >= 0 && text.charAt(i) == '='; --i) {
            --outputSize;
        }
        byte[] output = new byte[outputSize];

        int triples = (outputSize / 3) * 3;
        i = 0;
        for (j = 0; i < triples;) {
            int a = decode(text.charAt(i++));
            int b = decode(text.charAt(i++));
            int c = decode(text.charAt(i++));
            int d = decode(text.charAt(i++));
            int out = (a << 18) | (b << 12) | (c << 6) | d;
            output[j++] = (byte) (out >>> 16);
            output[j++] = (byte) (out >>> 8);
            output[j++] = (byte) (out);
        }
        int rem = output.length - j;
        if (rem == 1) {
            int a = decode(text.charAt(i));
            int b = decode(text.charAt(i + 1));
            output[j] = (byte) ((a << 2) | (b >>> 4));
        } else if (rem == 2) {
            int a = decode(text.charAt(i));
            int b = decode(text.charAt(i + 1));
            int c = decode(text.charAt(i + 2));
            output[j] = (byte) ((a << 2) | (b >>> 4));
            output[j + 1] = (byte) ((b << 4) | (c >>> 2));
        }

        return output;
    }

    private static int decode(char c) {
        return c < 256 ? reverse[c] : -1;
    }
}
