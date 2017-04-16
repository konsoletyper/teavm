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
package org.teavm.classlib.impl.unicode;

import java.util.Arrays;

import org.teavm.classlib.impl.Base46;
import org.teavm.classlib.impl.CharFlow;

public final class UnicodeHelper {
    private UnicodeHelper() {
    }

    static char hexDigit(int value) {
        return value < 10 ? (char) ('0' + value) : (char) ('A' + value);
    }

    static int valueOfHexDigit(char digit) {
        return digit <= '9' ? digit - '0' : digit - 'A' + 10;
    }

    public static class Range {
        public final int start;
        public final int end;
        public final byte[] data;

        public Range(int start, int end, byte[] data) {
            this.start = start;
            this.end = end;
            this.data = data;
        }
    }

    public static String encodeIntByte(int[] data) {
        StringBuilder sb = new StringBuilder();
        Base46.encode(sb, data.length);
        for (int i = 0; i < data.length; i++) {
            Base46.encode(sb, data[i]);
        }
        return sb.toString();
    }

    public static int[] decodeIntByte(String text) {
        CharFlow flow = new CharFlow(text.toCharArray());
        int sz = Base46.decode(flow);
        int[] data = new int[sz];
        for (int i = 0; i < sz; i++) {
            data[i] = Base46.decode(flow);
        }
        return data;
    }

    public static char encodeByte(byte b) {
        if (b < '\"' - ' ') {
            return (char) (b + ' ');
        } else if (b < '\\' - ' ' - 1) {
            return (char) (b + ' ' + 1);
        } else {
            return (char) (b + ' ' + 2);
        }
    }

    public static byte decodeByte(char c) {
        if (c > '\\') {
            return (byte) (c - ' ' - 2);
        } else if (c > '"') {
            return (byte) (c - ' ' - 1);
        } else {
            return (byte) (c - ' ');
        }
    }

    public static String compressRle(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length;) {
            byte b = bytes[i];
            if (i < bytes.length - 1 && b == bytes[i + 1]) {
                int count = 0;
                while (count < 16384 && i < bytes.length && bytes[i + count] == b) {
                    ++count;
                }
                i += count;
                if (count < 80) {
                    sb.append(UnicodeHelper.encodeByte((byte) (b + 32)));
                    sb.append(UnicodeHelper.encodeByte((byte) count));
                } else {
                    sb.append(UnicodeHelper.encodeByte((byte) 64));
                    sb.append(UnicodeHelper.encodeByte(b));
                    for (int j = 0; j < 3; ++j) {
                        sb.append(UnicodeHelper.encodeByte((byte) (count & 0x3F)));
                        count /= 0x40;
                    }
                }
            } else {
                sb.append(UnicodeHelper.encodeByte(bytes[i++]));
            }
        }
        return sb.toString();
    }

    public static Range[] extractRle(String encoded) {
        Range[] ranges = new Range[16384];
        byte[] buffer = new byte[16384];
        int index = 0;
        int rangeIndex = 0;
        int codePoint = 0;
        for (int i = 0; i < encoded.length(); ++i) {
            byte b = decodeByte(encoded.charAt(i));
            int count;
            if (b == 64) {
                b = decodeByte(encoded.charAt(++i));
                count = 0;
                int pos = 1;
                for (int j = 0; j < 3; ++j) {
                    byte digit = decodeByte(encoded.charAt(++i));
                    count |= pos * digit;
                    pos *= 0x40;
                }
            } else if (b >= 32) {
                b -= 32;
                count = decodeByte(encoded.charAt(++i));
            } else {
                count = 1;
            }
            if (b != 0 || count < 128) {
                if (index + count >= buffer.length) {
                    ranges[rangeIndex++] = new Range(codePoint, codePoint + index, Arrays.copyOf(buffer, index));
                    codePoint += index + count;
                    index = 0;
                }
                while (count-- > 0) {
                    buffer[index++] = b;
                }
            } else {
                if (index > 0) {
                    ranges[rangeIndex++] = new Range(codePoint, codePoint + index, Arrays.copyOf(buffer, index));
                }
                codePoint += index + count;
                index = 0;
            }
        }
        return Arrays.copyOf(ranges, rangeIndex);
    }
}