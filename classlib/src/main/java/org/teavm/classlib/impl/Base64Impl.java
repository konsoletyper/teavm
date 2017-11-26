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

public final class Base64Impl {
    private static byte[] alphabet = new byte[64];
    private static int[] reverse = new int[256];

    static {
        int i = 0;
        for (char c = 'A'; c <= 'Z'; ++c) {
            alphabet[i++] = (byte) c;
        }
        for (char c = 'a'; c <= 'z'; ++c) {
            alphabet[i++] = (byte) c;
        }
        for (char c = '0'; c <= '9'; ++c) {
            alphabet[i++] = (byte) c;
        }
        alphabet[i++] = '+';
        alphabet[i++] = '/';

        Arrays.fill(reverse, -1);
        for (i = 0; i < alphabet.length; ++i) {
            reverse[alphabet[i]] = i;
        }
    }

    private Base64Impl() {
    }

    public static byte[] decode(byte[] text) {
        int outputSize = (text.length / 4) * 3;
        int rem = text.length % 4;
        if (rem == 2 || rem == 3) {
            outputSize += rem - 1;
        }
        int i;
        for (i = text.length - 1; i >= 0 && text[i] == '='; --i) {
            --outputSize;
        }
        byte[] output = new byte[outputSize];
        decode(text, output);
        return output;
    }

    public static void decode(byte[] text, byte[] output) {
        int inputSize = text.length;
        int i;
        for (i = text.length - 1; i >= 0 && text[i] == '='; --i) {
            --inputSize;
        }
        int triples = (inputSize / 4) * 4;

        i = 0;
        int j = 0;
        while (i < triples) {
            int a = decode(text[i++]);
            int b = decode(text[i++]);
            int c = decode(text[i++]);
            int d = decode(text[i++]);
            int out = (a << 18) | (b << 12) | (c << 6) | d;
            output[j++] = (byte) (out >>> 16);
            output[j++] = (byte) (out >>> 8);
            output[j++] = (byte) out;
        }

        int rem = inputSize - i;
        if (rem == 2) {
            int a = decode(text[i]);
            int b = decode(text[i + 1]);
            output[j] = (byte) ((a << 2) | (b >>> 4));
        } else if (rem == 3) {
            int a = decode(text[i]);
            int b = decode(text[i + 1]);
            int c = decode(text[i + 2]);
            output[j] = (byte) ((a << 2) | (b >>> 4));
            output[j + 1] = (byte) ((b << 4) | (c >>> 2));
        }
    }

    private static int decode(byte c) {
        return reverse[c];
    }

    public static byte[] encode(byte[] data, boolean pad) {
        int outputSize = ((data.length + 2) / 3) * 4;
        if (!pad) {
            int rem = data.length % 3;
            if (rem != 0) {
                outputSize -= 3 - rem;
            }
        }
        byte[] output = new byte[outputSize];
        encode(data, output, pad);
        return output;
    }

    public static int encode(byte[] data, byte[] output, boolean pad) {
        int triples = (data.length / 3) * 3;

        int i = 0;
        int j;
        for (j = 0; j < triples;) {
            output[i++] = encode((byte) (data[j] >>> 2));
            output[i++] = encode((byte) ((data[j] << 4) | ((data[j + 1] & 0xFF) >>> 4)));
            ++j;
            output[i++] = encode((byte) ((data[j] << 2) | ((data[j + 1] & 0xFF) >>> 6)));
            ++j;
            output[i++] = encode(data[j]);
            ++j;
        }

        int rem = data.length - j;
        if (rem == 1) {
            output[i++] = encode((byte) (data[j] >>> 2));
            output[i++] = encode((byte) (data[j] << 4));
            if (pad) {
                output[i++] = '=';
                output[i++] = '=';
            }
        } else if (rem == 2) {
            output[i++] = encode((byte) (data[j] >>> 2));
            output[i++] = encode((byte) ((data[j] << 4) | (data[j + 1] >>> 4)));
            ++j;
            output[i++] = encode((byte) (data[j] << 2));
            if (pad) {
                output[i++] = '=';
            }
        }

        return i;
    }

    private static byte encode(byte b) {
        return alphabet[b & 63];
    }
}
