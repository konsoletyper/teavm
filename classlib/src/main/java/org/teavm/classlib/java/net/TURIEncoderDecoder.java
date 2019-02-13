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
package org.teavm.classlib.java.net;

import org.teavm.classlib.java.io.TByteArrayOutputStream;
import org.teavm.classlib.java.lang.TCharacter;
import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TString;
import org.teavm.classlib.java.lang.TStringBuilder;

class TURIEncoderDecoder {
    static final String digits = "0123456789ABCDEF";

    private TURIEncoderDecoder() {
    }

    static void validate(String s, String legal) throws TURISyntaxException {
        for (int i = 0; i < s.length();) {
            char ch = s.charAt(i);
            if (ch == '%') {
                do {
                    if (i + 2 >= s.length()) {
                        throw new TURISyntaxException(s, "", i);
                    }
                    int d1 = TCharacter.digit(s.charAt(i + 1), 16);
                    int d2 = TCharacter.digit(s.charAt(i + 2), 16);
                    if (d1 == -1 || d2 == -1) {
                        throw new TURISyntaxException(s, "", i);
                    }

                    i += 3;
                } while (i < s.length() && s.charAt(i) == '%');

                continue;
            }
            if (!((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9') || legal.indexOf(ch) > -1 || (ch > 127
                    && !isSpaceChar(ch) && !TCharacter.isISOControl(ch)))) {
                throw new TURISyntaxException(s, "", i);
            }
            i++;
        }
    }

    private static boolean isSpaceChar(char c) {
        switch (c) {
            case ' ':
            case '\t':
                return true;
            default:
                return false;
        }
    }

    static void validateSimple(String s, String legal)
            throws TURISyntaxException {
        for (int i = 0; i < s.length();) {
            char ch = s.charAt(i);
            if (!((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9') || legal.indexOf(ch) > -1)) {
                throw new TURISyntaxException(s, "", i);
            }
            i++;
        }
    }

    static String quoteIllegal(String s, String legal) {
        TStringBuilder buf = new TStringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || legal.indexOf(ch) > -1
                    || (ch > 127 && !isSpaceChar(ch) && !TCharacter.isISOControl(ch))) {
                buf.append(ch);
            } else {
                byte[] bytes = new TString(new char[] { ch }).getBytes();
                for (int j = 0; j < bytes.length; j++) {
                    buf.append('%');
                    buf.append(digits.charAt((bytes[j] & 0xf0) >> 4));
                    buf.append(digits.charAt(bytes[j] & 0xf));
                }
            }
        }
        return buf.toString();
    }

    static String encodeOthers(String s) {
        TStringBuilder buf = new TStringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch <= 127) {
                buf.append(ch);
            } else {
                byte[] bytes = new TString(new char[] { ch }).getBytes();
                for (int j = 0; j < bytes.length; j++) {
                    buf.append('%');
                    buf.append(digits.charAt((bytes[j] & 0xf0) >> 4));
                    buf.append(digits.charAt(bytes[j] & 0xf));
                }
            }
        }
        return buf.toString();
    }

    static String decode(String s) {
        TStringBuilder result = new TStringBuilder();
        TByteArrayOutputStream out = new TByteArrayOutputStream();
        for (int i = 0; i < s.length();) {
            char c = s.charAt(i);
            if (c == '%') {
                out.reset();
                do {
                    if (i + 2 >= s.length()) {
                        throw new TIllegalArgumentException();
                    }
                    int d1 = TCharacter.digit(s.charAt(i + 1), 16);
                    int d2 = TCharacter.digit(s.charAt(i + 2), 16);
                    if (d1 == -1 || d2 == -1) {
                        throw new TIllegalArgumentException();
                    }
                    out.write((byte) ((d1 << 4) + d2));
                    i += 3;
                } while (i < s.length() && s.charAt(i) == '%');
                result.append(out.toString());
                continue;
            }
            result.append(c);
            i++;
        }
        return result.toString();
    }

}
