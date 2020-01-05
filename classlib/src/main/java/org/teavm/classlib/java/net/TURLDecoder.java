/*
 *  Copyright 2019 Alexey Andreev.
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

package org.teavm.classlib.java.net;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Objects;

public final class TURLDecoder {
    private TURLDecoder() {
    }

    @Deprecated
    public static String decode(String s) {
        return decode(s, StandardCharsets.UTF_8);
    }

    public static String decode(String s, String enc) throws UnsupportedEncodingException {
        Objects.requireNonNull(enc);

        if (enc.isEmpty()) {
            throw new UnsupportedEncodingException("Invalid parameter: enc");
        }

        if (s.indexOf('%') == -1) {
            if (s.indexOf('+') == -1) {
                return s;
            }
            char[] str = s.toCharArray();
            for (int i = 0; i < str.length; i++) {
                if (str[i] == '+') {
                    str[i] = ' ';
                }
            }
            return new String(str);
        }

        Charset charset = null;
        try {
            charset = Charset.forName(enc);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            UnsupportedEncodingException toThrow = new UnsupportedEncodingException();
            toThrow.initCause(e);
            throw toThrow;
        }

        return decode(s, charset);
    }

    private static String decode(String s, Charset charset) {
        char[] strBuf = new char[s.length()];
        byte[] buf = new byte[s.length() / 3];
        int bufLen = 0;

        for (int i = 0; i < s.length();) {
            char c = s.charAt(i);
            if (c == '+') {
                strBuf[bufLen] = ' ';
            } else if (c == '%') {
                int len = 0;
                do {
                    if (i + 2 >= s.length()) {
                        throw new IllegalArgumentException("Incomplete % sequence at: " + i);
                    }
                    int d1 = Character.digit(s.charAt(i + 1), 16);
                    int d2 = Character.digit(s.charAt(i + 2), 16);
                    if (d1 == -1 || d2 == -1) {
                        throw new IllegalArgumentException("Invalid % sequence (" +  s.substring(i, i + 3)
                                + ") at " + i);
                    }
                    buf[len++] = (byte) ((d1 << 4) + d2);
                    i += 3;
                } while (i < s.length() && s.charAt(i) == '%');

                CharBuffer cb = charset.decode(ByteBuffer.wrap(buf, 0, len));
                len = cb.length();
                System.arraycopy(cb.array(), 0, strBuf, bufLen, len);
                bufLen += len;
                continue;
            } else {
                strBuf[bufLen] = c;
            }
            i++;
            bufLen++;
        }
        return new String(strBuf, 0, bufLen);
    }
}
