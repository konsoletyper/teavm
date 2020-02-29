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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Objects;

public final class TURLEncoder {

    static final String digits = "0123456789ABCDEF";

    private TURLEncoder() {
    }

    @Deprecated
    public static String encode(String s) {
        // Guess a bit bigger for encoded form
        StringBuilder buf = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z'
                    || ch >= '0' && ch <= '9' || ".-*_".indexOf(ch) > -1) {
                buf.append(ch);
            } else if (ch == ' ') {
                buf.append('+');
            } else {
                byte[] bytes = new String(new char[] { ch }).getBytes(StandardCharsets.UTF_8);
                for (byte b : bytes) {
                    buf.append('%');
                    buf.append(digits.charAt((b & 0xf0) >> 4));
                    buf.append(digits.charAt(b & 0xf));
                }
            }
        }
        return buf.toString();
    }
    
    public static String encode(String s, Charset enc) {
        Objects.requireNonNull(s);
        Objects.requireNonNull(enc);

        // Guess a bit bigger for encoded form
        StringBuffer buf = new StringBuffer(s.length() + 16);
        int start = -1;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z'
                    || ch >= '0' && ch <= '9' || " .-*_".indexOf(ch) > -1) {
                if (start >= 0) {
                    convert(s.substring(start, i), buf, enc);
                    start = -1;
                }
                if (ch != ' ') {
                    buf.append(ch);
                } else {
                    buf.append('+');
                }
            } else {
                if (start < 0) {
                    start = i;
                }
            }
        }
        if (start >= 0) {
            convert(s.substring(start), buf, enc);
        }
        return buf.toString();
    }

    public static String encode(String s, String enc) throws UnsupportedEncodingException {
        Objects.requireNonNull(s);
        Objects.requireNonNull(enc);

        // check for UnsupportedEncodingException
        try {
            return encode(s, Charset.forName(enc));
        } catch (UnsupportedCharsetException e) {
            throw new UnsupportedEncodingException(enc);
        }
    }

    private static void convert(String s, StringBuffer buf, Charset enc) {
        byte[] bytes = s.getBytes(enc);
        for (int j = 0; j < bytes.length; j++) {
            buf.append('%');
            buf.append(digits.charAt((bytes[j] & 0xf0) >> 4));
            buf.append(digits.charAt(bytes[j] & 0xf));
        }
    }
}
