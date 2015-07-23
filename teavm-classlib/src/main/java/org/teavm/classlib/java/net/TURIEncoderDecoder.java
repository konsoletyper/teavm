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

/**
 * This class is used to encode a string using the format required by {@code
 * application/x-www-form-urlencoded} MIME content type. It contains helper
 * methods used by the URI class, and performs encoding and decoding in a
 * slightly different way than {@code URLEncoder} and {@code URLDecoder}.
 */
class TURIEncoderDecoder {
    static final TString digits = TString.wrap("0123456789ABCDEF");

    private TURIEncoderDecoder() {
    }

    /**
     * Validate a string by checking if it contains any characters other than:
     * 1. letters ('a'..'z', 'A'..'Z') 2. numbers ('0'..'9') 3. characters in
     * the legalset parameter 4. others (unicode characters that are not in
     * US-ASCII set, and are not ISO Control or are not ISO Space characters)
     * <p>
     * called from {@code URI.Helper.parseURI()} to validate each component
     *
     * @param s
     *            {@code java.lang.String} the string to be validated
     * @param legal
     *            {@code java.lang.String} the characters allowed in the String
     *            s
     */
    static void validate(TString s, TString legal) throws TURISyntaxException {
        for (int i = 0; i < s.length();) {
            char ch = s.charAt(i);
            if (ch == '%') {
                do {
                    if (i + 2 >= s.length()) {
                        throw new TURISyntaxException(s, TString.wrap(""), i);
                    }
                    int d1 = TCharacter.digit(s.charAt(i + 1), 16);
                    int d2 = TCharacter.digit(s.charAt(i + 2), 16);
                    if (d1 == -1 || d2 == -1) {
                        throw new TURISyntaxException(s, TString.wrap(""), i);
                    }

                    i += 3;
                } while (i < s.length() && s.charAt(i) == '%');

                continue;
            }
            if (!((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9') || legal.indexOf(ch) > -1 || (ch > 127
                    && !isSpaceChar(ch) && !TCharacter.isISOControl(ch)))) {
                throw new TURISyntaxException(s, TString.wrap(""), i); //$NON-NLS-1$
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

    static void validateSimple(TString s, TString legal)
            throws TURISyntaxException {
        for (int i = 0; i < s.length();) {
            char ch = s.charAt(i);
            if (!((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9') || legal.indexOf(ch) > -1)) {
                throw new TURISyntaxException(s, TString.wrap(""), i); //$NON-NLS-1$
            }
            i++;
        }
    }

    /**
     * All characters except letters ('a'..'z', 'A'..'Z') and numbers ('0'..'9')
     * and legal characters are converted into their hexidecimal value prepended
     * by '%'.
     * <p>
     * For example: '#' -> %23
     * Other characters, which are unicode chars that are not US-ASCII, and are
     * not ISO Control or are not ISO Space chars, are preserved.
     * <p>
     * Called from {@code URI.quoteComponent()} (for multiple argument
     * constructors)
     *
     * @param s
     *            java.lang.String the string to be converted
     * @param legal
     *            java.lang.String the characters allowed to be preserved in the
     *            string s
     * @return java.lang.String the converted string
     */
    static TString quoteIllegal(TString s, TString legal) {
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
        return TString.wrap(buf.toString());
    }

    /**
     * Other characters, which are Unicode chars that are not US-ASCII, and are
     * not ISO Control or are not ISO Space chars are not preserved. They are
     * converted into their hexidecimal value prepended by '%'.
     * <p>
     * For example: Euro currency symbol -> "%E2%82%AC".
     * <p>
     * Called from URI.toASCIIString()
     *
     * @param s
     *            java.lang.String the string to be converted
     * @return java.lang.String the converted string
     */
    static TString encodeOthers(TString s) {
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
        return TString.wrap(buf.toString());
    }

    /**
     * Decodes the string argument which is assumed to be encoded in the {@code
     * x-www-form-urlencoded} MIME content type using the UTF-8 encoding scheme.
     * <p>
     *'%' and two following hex digit characters are converted to the
     * equivalent byte value. All other characters are passed through
     * unmodified.
     * <p>
     * e.g. "A%20B%20C %24%25" -> "A B C $%"
     * <p>
     * Called from URI.getXYZ() methods
     *
     * @param s
     *            java.lang.String The encoded string.
     * @return java.lang.String The decoded version.
     */
    static TString decode(TString s) {

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
                result.append(TString.wrap(out.toString()));
                continue;
            }
            result.append(c);
            i++;
        }
        return TString.wrap(result.toString());
    }

}
