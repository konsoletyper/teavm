/*
 *  Copyright 2025 Ashera Cordova.
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

import java.util.Objects;
import org.teavm.classlib.impl.unicode.CharMapping;
import org.teavm.classlib.impl.unicode.UnicodeHelper;
import org.teavm.platform.metadata.StringResource;

public class TCharacter extends TObject implements TComparable<TCharacter> {
    public static final int MIN_RADIX = 2;
    public static final int MAX_RADIX = 36;
    public static final char MIN_VALUE = '\0';
    public static final char MAX_VALUE = '\uFFFF';
    public static final Class<Character> TYPE = char.class;
    public static final byte UNASSIGNED = 0;
    public static final byte UPPERCASE_LETTER = 1;
    public static final byte LOWERCASE_LETTER = 2;
    public static final byte TITLECASE_LETTER = 3;
    public static final byte MODIFIER_LETTER = 4;
    public static final byte OTHER_LETTER = 5;
    public static final byte NON_SPACING_MARK = 6;
    public static final byte ENCLOSING_MARK = 7;
    public static final byte COMBINING_SPACING_MARK = 8;
    public static final byte DECIMAL_DIGIT_NUMBER = 9;
    public static final byte LETTER_NUMBER = 10;
    public static final byte OTHER_NUMBER = 11;
    public static final byte SPACE_SEPARATOR = 12;
    public static final byte LINE_SEPARATOR = 13;
    public static final byte PARAGRAPH_SEPARATOR = 14;
    public static final byte CONTROL = 15;
    public static final byte FORMAT = 16;
    public static final byte PRIVATE_USE = 17;
    public static final byte SURROGATE = 19;
    public static final byte DASH_PUNCTUATION = 20;
    public static final byte START_PUNCTUATION = 21;
    public static final byte END_PUNCTUATION = 22;
    public static final byte CONNECTOR_PUNCTUATION = 23;
    public static final byte OTHER_PUNCTUATION = 24;
    public static final byte MATH_SYMBOL = 25;
    public static final byte CURRENCY_SYMBOL = 26;
    public static final byte MODIFIER_SYMBOL = 27;
    public static final byte OTHER_SYMBOL = 28;
    public static final byte INITIAL_QUOTE_PUNCTUATION = 29;
    public static final byte FINAL_QUOTE_PUNCTUATION = 30;
    public static final byte DIRECTIONALITY_UNDEFINED = -1;
    public static final byte DIRECTIONALITY_LEFT_TO_RIGHT = 0;
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT = 1;
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC = 2;
    public static final byte DIRECTIONALITY_EUROPEAN_NUMBER = 3;
    public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR = 4;
    public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR = 5;
    public static final byte DIRECTIONALITY_ARABIC_NUMBER = 6;
    public static final byte DIRECTIONALITY_COMMON_NUMBER_SEPARATOR = 7;
    public static final byte DIRECTIONALITY_NONSPACING_MARK = 8;
    public static final byte DIRECTIONALITY_BOUNDARY_NEUTRAL = 9;
    public static final byte DIRECTIONALITY_PARAGRAPH_SEPARATOR = 10;
    public static final byte DIRECTIONALITY_SEGMENT_SEPARATOR = 11;
    public static final byte DIRECTIONALITY_WHITESPACE = 12;
    public static final byte DIRECTIONALITY_OTHER_NEUTRALS = 13;
    public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING = 14;
    public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE = 15;
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING = 16;
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE = 17;
    public static final byte DIRECTIONALITY_POP_DIRECTIONAL_FORMAT = 18;
    public static final char MIN_HIGH_SURROGATE = '\uD800';
    public static final char MAX_HIGH_SURROGATE = '\uDBFF';
    public static final char MIN_LOW_SURROGATE  = '\uDC00';
    public static final char MAX_LOW_SURROGATE  = '\uDFFF';
    public static final char MIN_SURROGATE = MIN_HIGH_SURROGATE;
    public static final char MAX_SURROGATE = MAX_LOW_SURROGATE;
    public static final int MIN_SUPPLEMENTARY_CODE_POINT = 0x010000;
    public static final int MIN_CODE_POINT = 0x000000;
    public static final int MAX_CODE_POINT = 0X10FFFF;
    public static final int SIZE = 16;
    public static final int BYTES = SIZE / Byte.SIZE;
    static final int ERROR = 0xFFFFFFFF;
    private static int[] digitMapping;
    private static CharMapping titleCaseMapping;
    private static CharMapping upperCaseMapping;
    private static CharMapping lowerCaseMapping;
    private static UnicodeHelper.Range[] classMapping;
    private final char value;
    private static TCharacter[] characterCache = new TCharacter[128];
    private static final int SURROGATE_NEUTRAL_BIT_MASK = 0xF800;
    private static final int SURROGATE_BITS = 0xD800;
    private static final int SURROGATE_BIT_MASK = 0xFC00;
    private static final int SURROGATE_BIT_INV_MASK = 0x03FF;
    private static final int HIGH_SURROGATE_BITS = 0xD800;
    private static final int LOW_SURROGATE_BITS = 0xDC00;
    private static final int MEANINGFUL_SURROGATE_BITS = 10;

    public TCharacter(char value) {
        this.value = value;
    }

    public char charValue() {
        return value;
    }

    public static TCharacter valueOf(char value) {
        if (value < characterCache.length) {
            TCharacter result = characterCache[value];
            if (result == null) {
                result = new TCharacter(value);
                characterCache[value] = result;
            }
            return result;
        }
        return new TCharacter(value);
    }

    @Override
    public String toString() {
        return toString(value);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof TCharacter && ((TCharacter) other).value == value;
    }

    @Override
    public int hashCode() {
        return hashCode(value);
    }

    public static int hashCode(char value) {
        return value;
    }

    public static String toString(char c) {
        return new String(new char[] { c });
    }

    public static boolean isValidCodePoint(int codePoint) {
        return codePoint >= 0 && codePoint <= MAX_CODE_POINT;
    }

    public static boolean isBmpCodePoint(int codePoint) {
        return codePoint > 0 && codePoint <= MAX_VALUE;
    }

    public static boolean isSupplementaryCodePoint(int codePoint) {
        return codePoint >= MIN_SUPPLEMENTARY_CODE_POINT && codePoint <= MAX_CODE_POINT;
    }

    public static boolean isHighSurrogate(char ch) {
        return (ch & SURROGATE_BIT_MASK) == HIGH_SURROGATE_BITS;
    }

    public static boolean isLowSurrogate(char ch) {
        return (ch & SURROGATE_BIT_MASK) == LOW_SURROGATE_BITS;
    }

    public static boolean isSurrogate(char ch) {
        return isHighSurrogate(ch) || isLowSurrogate(ch);
    }

    public static boolean isSurrogatePair(char high, char low) {
        return isHighSurrogate(high) && isLowSurrogate(low);
    }

    public static int charCount(int codePoint) {
        return codePoint >= MIN_SUPPLEMENTARY_CODE_POINT ? 2 : 1;
    }

    public static int toCodePoint(char high, char low) {
        return (((high & SURROGATE_BIT_INV_MASK) << MEANINGFUL_SURROGATE_BITS) | (low & SURROGATE_BIT_INV_MASK))
                + MIN_SUPPLEMENTARY_CODE_POINT;
    }

    public static int codePointAt(TCharSequence seq, int index) {
        if (index >= seq.length() - 1 || !isHighSurrogate(seq.charAt(index))
                || !isLowSurrogate(seq.charAt(index + 1))) {
            return seq.charAt(index);
        } else {
            return toCodePoint(seq.charAt(index), seq.charAt(index + 1));
        }
    }

    public static int codePointAt(char[] a, int index) {
        return codePointAt(a, index, a.length);
    }

    public static int codePointAt(char[] a, int index, int limit) {
        if (index >= limit || index < 0 || limit > a.length) {
            throw new IndexOutOfBoundsException();
        }
        if (index >= limit - 1 || !isHighSurrogate(a[index]) || !isLowSurrogate(a[index + 1])) {
            return a[index];
        } else {
            return toCodePoint(a[index], a[index + 1]);
        }
    }

    public static int codePointBefore(TCharSequence seq, int index) {
        if (index == 1 || !isLowSurrogate(seq.charAt(index - 1)) || !isHighSurrogate(seq.charAt(index - 2))) {
            return seq.charAt(index - 1);
        }
        return toCodePoint(seq.charAt(index - 2), seq.charAt(index - 1));
    }

    public static int codePointBefore(char[] a, int index) {
        return codePointBefore(a, index, 0);
    }

    public static int codePointBefore(char[] a, int index, int start) {
        if (index > a.length || index <= start || start < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (index <= start + 1 || !isLowSurrogate(a[index - 1]) || !isHighSurrogate(a[index - 2])) {
            return a[index - 1];
        } else {
            return toCodePoint(a[index - 2], a[index - 1]);
        }
    }

    public static char highSurrogate(int codePoint) {
        codePoint -= MIN_SUPPLEMENTARY_CODE_POINT;
        return (char) (HIGH_SURROGATE_BITS | (codePoint >> MEANINGFUL_SURROGATE_BITS) & SURROGATE_BIT_INV_MASK);
    }

    public static char lowSurrogate(int codePoint) {
        return (char) (LOW_SURROGATE_BITS | codePoint & SURROGATE_BIT_INV_MASK);
    }

    public static char toLowerCase(char ch) {
        return (char) toLowerCase((int) ch);
    }

    public static int toLowerCase(int ch) {
        return mapChar(getLowerCaseMapping(), ch);
    }

    private static CharMapping getLowerCaseMapping() {
        if (lowerCaseMapping == null) {
            lowerCaseMapping = UnicodeHelper.createCharMapping(
                    UnicodeHelper.decodeCaseMapping(acquireLowerCaseMapping().getValue()));
        }
        return lowerCaseMapping;
    }

    private static native StringResource acquireLowerCaseMapping();


    public static char toUpperCase(char ch) {
        return (char) toUpperCase((int) ch);
    }

    public static int toUpperCase(int codePoint) {
        return mapChar(getUpperCaseMapping(), codePoint);
    }

    private static CharMapping getUpperCaseMapping() {
        if (upperCaseMapping == null) {
            upperCaseMapping = UnicodeHelper.createCharMapping(
                    UnicodeHelper.decodeCaseMapping(acquireUpperCaseMapping().getValue()));
        }
        return upperCaseMapping;
    }

    private static native StringResource acquireUpperCaseMapping();

    public static int toTitleCase(int codePoint) {
        codePoint = mapChar(getTitleCaseMapping(), codePoint);
        if (codePoint == codePoint) {
            codePoint = toUpperCase(codePoint);
        }
        return codePoint;
    }

    public static char toTitleCase(char c) {
        return (char) toTitleCase((int) c);
    }

    private static CharMapping getTitleCaseMapping() {
        if (titleCaseMapping == null) {
            titleCaseMapping = UnicodeHelper.createCharMapping(
                    UnicodeHelper.decodeCaseMapping(acquireTitleCaseMapping().getValue()));
        }
        return titleCaseMapping;
    }

    private static native StringResource acquireTitleCaseMapping();

    private static int mapChar(CharMapping table, int codePoint) {
        if (codePoint < table.fastTable.length) {
            return codePoint + table.fastTable[codePoint];
        }

        var binSearchTable = table.binarySearchTable;
        int index = binarySearchTable(binSearchTable, codePoint);
        if (index < 0 || index * 2 >= binSearchTable.length) {
            return 0;
        }
        return codePoint + binSearchTable[index * 2 + 1];
    }

    private static int binarySearchTable(int[] data, int key) {
        int l = 0;
        int u = data.length / 2 - 1;
        while (true) {
            int i = (l + u) / 2;
            int e = data[i * 2];
            if (e == key) {
                return i;
            } else if (e > key) {
                u = i - 1;
                if (u < l) {
                    return i - 1;
                }
            } else {
                l = i + 1;
                if (l > u) {
                    return i;
                }
            }
        }
    }

    public static int digit(char ch, int radix) {
        return digit((int) ch, radix);
    }

    public static int digit(int codePoint, int radix) {
        if (radix < MIN_RADIX || radix > MAX_RADIX) {
            return -1;
        }
        int d = getNumericValue(codePoint);
        return d < radix ? d : -1;
    }

    public static int getNumericValue(char ch) {
        return getNumericValue((int) ch);
    }

    public static int getNumericValue(int codePoint) {
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
        if (radix < MIN_RADIX || radix > MAX_RADIX || digit < 0 || digit >= radix) {
            return '\0';
        }
        return digit < 10 ? (char) ('0' + digit) : (char) ('a' + digit - 10);
    }

    public static boolean isDigit(char ch) {
        return isDigit((int) ch);
    }

    public static boolean isDigit(int codePoint) {
        return getType(codePoint) == DECIMAL_DIGIT_NUMBER;
    }

    private static int[] getDigitMapping() {
        if (digitMapping == null) {
            digitMapping = UnicodeHelper.decodeIntPairsDiff(obtainDigitMapping().getValue());
        }
        return digitMapping;
    }

    private static native StringResource obtainDigitMapping();

    private static UnicodeHelper.Range[] getClasses() {
        if (classMapping == null) {
            classMapping = UnicodeHelper.extractRle(obtainClasses().getValue());
        }
        return classMapping;
    }

    private static native StringResource obtainClasses();

    public static int toChars(int codePoint, char[] dst, int dstIndex) {
        if (!isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException();
        }
        if (codePoint >= MIN_SUPPLEMENTARY_CODE_POINT) {
            dst[dstIndex] = highSurrogate(codePoint);
            dst[dstIndex + 1] = lowSurrogate(codePoint);
            return 2;
        } else {
            dst[dstIndex] = (char) codePoint;
            return 1;
        }
    }

    public static char[] toChars(int codePoint) {
        if (!isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException();
        }
        if (codePoint >= MIN_SUPPLEMENTARY_CODE_POINT) {
            return new char[] { highSurrogate(codePoint), lowSurrogate(codePoint) };
        } else {
            return new char[] { (char) codePoint };
        }
    }

    public static int codePointCount(TCharSequence seq, int beginIndex, int endIndex) {
        Objects.checkFromToIndex(beginIndex, endIndex, seq.length());
        int count = endIndex - beginIndex;
        --endIndex;
        for (int i = beginIndex; i < endIndex; ++i) {
            if (isHighSurrogate(seq.charAt(i)) && isLowSurrogate(seq.charAt(i + 1))) {
                --count;
                ++i;
            }
        }
        return count;
    }

    public static int codePointCount(char[] a, int offset, int count) {
        Objects.checkFromIndexSize(offset, count, a.length);
        int r = count;
        --count;
        for (int i = 0; i < count; ++i) {
            if (isHighSurrogate(a[offset]) && isLowSurrogate(a[offset + i + 1])) {
                --r;
                ++i;
            }
        }
        return r;
    }

    public static int offsetByCodePoints(TCharSequence seq, int index, int codePointOffset) {
        if (codePointOffset >= 0) {
            int i;
            for (i = 0; i < codePointOffset && index < seq.length(); ++i) {
                if (index < seq.length() - 1 && isHighSurrogate(seq.charAt(index))
                        && isLowSurrogate(seq.charAt(index + 1))) {
                    index += 2;
                } else {
                    index++;
                }
            }
            if (i < codePointOffset) {
                throw new IndexOutOfBoundsException();
            }
        } else {
            int i;
            for (i = codePointOffset; i < 0 && index > 0; ++i) {
                if (index > 0 && isLowSurrogate(seq.charAt(index - 1))
                        && isHighSurrogate(seq.charAt(index - 2))) {
                    index -= 2;
                } else {
                    index--;
                }
            }
            if (i < 0) {
                throw new IndexOutOfBoundsException();
            }
        }
        return index;
    }

    public static int offsetByCodePoints(char[] a, int start, int count, int index, int codePointOffset) {
        if (count > a.length - start || start < 0 || count < 0
                || index < start || index > start + count) {
            throw new IndexOutOfBoundsException();
        }
        if (codePointOffset >= 0) {
            int i;
            for (i = 0; i < codePointOffset && index < start + count; ++i) {
                if (index < count - 1 && isHighSurrogate(a[index])
                        && isLowSurrogate(a[index + 1])) {
                    index += 2;
                } else {
                    index++;
                }
            }
            if (i < codePointOffset) {
                throw new IndexOutOfBoundsException();
            }
        } else {
            int i;
            for (i = codePointOffset; i < 0 && index > start; ++i) {
                if (index > start && isLowSurrogate(a[index - 1])
                        && isHighSurrogate(a[index - 2])) {
                    index -= 2;
                } else {
                    index--;
                }
            }
            if (i < 0) {
                throw new IndexOutOfBoundsException();
            }
        }
        return index;
    }

    public static boolean isISOControl(char ch) {
        return isISOControl((int) ch);
    }

    public static boolean isISOControl(int codePoint) {
        return codePoint >= 0 && codePoint <= 0x1F || codePoint >= 0x7F && codePoint <= 0x9F;
    }

    public static int getType(char c) {
        return getType((int) c);
    }

    public static int getType(int codePoint) {
        if (isBmpCodePoint(codePoint) && isSurrogate((char) codePoint)) {
            return SURROGATE;
        }
        UnicodeHelper.Range[] classes = getClasses();
        int l = 0;
        int u = classes.length - 1;
        while (l <= u) {
            int i = (l + u) / 2;
            UnicodeHelper.Range range = classes[i];
            if (codePoint >= range.end) {
                l = i + 1;
            } else if (codePoint < range.start) {
                u = i - 1;
            } else {
                return range.data[codePoint - range.start];
            }
        }
        return 0;
    }

    public static boolean isLowerCase(char ch) {
        return isLowerCase((int) ch);
    }

    public static boolean isLowerCase(int codePoint) {
        return getType(codePoint) == LOWERCASE_LETTER;
    }

    public static boolean isUpperCase(char ch) {
        return isUpperCase((int) ch);
    }

    public static boolean isUpperCase(int codePoint) {
        return getType(codePoint) == UPPERCASE_LETTER;
    }

    public static boolean isTitleCase(char ch) {
        return isTitleCase((int) ch);
    }

    public static boolean isTitleCase(int codePoint) {
        return getType(codePoint) == TITLECASE_LETTER;
    }

    public static boolean isDefined(char ch) {
        return isDefined((int) ch);
    }

    public static boolean isDefined(int codePoint) {
        return getType(codePoint) != UNASSIGNED;
    }

    public static boolean isLetter(char ch) {
        return isLetter((int) ch);
    }

    public static boolean isLetter(int codePoint) {
        switch (getType(codePoint)) {
            case UPPERCASE_LETTER:
            case LOWERCASE_LETTER:
            case TITLECASE_LETTER:
            case MODIFIER_LETTER:
            case OTHER_LETTER:
                return true;
            default:
                return false;
        }
    }

    public static boolean isLetterOrDigit(char ch) {
        return isLetterOrDigit((int) ch);
    }

    public static boolean isLetterOrDigit(int codePoint) {
        switch (getType(codePoint)) {
            case UPPERCASE_LETTER:
            case LOWERCASE_LETTER:
            case TITLECASE_LETTER:
            case MODIFIER_LETTER:
            case OTHER_LETTER:
            case DECIMAL_DIGIT_NUMBER:
                return true;
            default:
                return false;
        }
    }

    @Deprecated
    public static boolean isJavaLetter(char ch) {
        return isJavaIdentifierStart(ch);
    }

    public static boolean isJavaIdentifierStart(char ch) {
        return isJavaIdentifierStart((int) ch);
    }

    public static boolean isJavaIdentifierStart(int codePoint) {
        switch (getType(codePoint)) {
            case UPPERCASE_LETTER:
            case LOWERCASE_LETTER:
            case TITLECASE_LETTER:
            case MODIFIER_LETTER:
            case OTHER_LETTER:
            case LETTER_NUMBER:
            case CONNECTOR_PUNCTUATION:
            case CURRENCY_SYMBOL:
                return true;
            default:
                return isIdentifierIgnorable(codePoint);
        }
    }

    @Deprecated
    public static boolean isJavaLetterOrDigit(char ch) {
        return isJavaIdentifierPart(ch);
    }

    public static boolean isJavaIdentifierPart(char ch) {
        return isJavaIdentifierPart((int) ch);
    }

    public static boolean isJavaIdentifierPart(int codePoint) {
        switch (getType(codePoint)) {
            case UPPERCASE_LETTER:
            case LOWERCASE_LETTER:
            case TITLECASE_LETTER:
            case MODIFIER_LETTER:
            case OTHER_LETTER:
            case LETTER_NUMBER:
            case DECIMAL_DIGIT_NUMBER:
            case COMBINING_SPACING_MARK:
            case NON_SPACING_MARK:
            case CONNECTOR_PUNCTUATION:
            case CURRENCY_SYMBOL:
                return true;
            default:
                return isIdentifierIgnorable(codePoint);
        }
    }

    public static boolean isAlphabetic(int codePoint) {
        switch (getType(codePoint)) {
            case UPPERCASE_LETTER:
            case LOWERCASE_LETTER:
            case TITLECASE_LETTER:
            case MODIFIER_LETTER:
            case OTHER_LETTER:
            case LETTER_NUMBER:
                return true;
            default:
                return false;
        }
    }

    public static boolean isUnicodeIdentifierStart(char ch) {
        return isUnicodeIdentifierStart((int) ch);
    }

    public static boolean isUnicodeIdentifierStart(int codePoint) {
        switch (getType(codePoint)) {
            case UPPERCASE_LETTER:
            case LOWERCASE_LETTER:
            case TITLECASE_LETTER:
            case MODIFIER_LETTER:
            case OTHER_LETTER:
            case LETTER_NUMBER:
                return true;
            default:
                return isIdentifierIgnorable(codePoint);
        }
    }

    public static boolean isUnicodeIdentifierPart(char ch) {
        return isUnicodeIdentifierPart((int) ch);
    }

    public static boolean isUnicodeIdentifierPart(int codePoint) {
        switch (getType(codePoint)) {
            case UPPERCASE_LETTER:
            case LOWERCASE_LETTER:
            case TITLECASE_LETTER:
            case MODIFIER_LETTER:
            case OTHER_LETTER:
            case LETTER_NUMBER:
            case CONNECTOR_PUNCTUATION:
            case DECIMAL_DIGIT_NUMBER:
            case COMBINING_SPACING_MARK:
            case NON_SPACING_MARK:
                return true;
            default:
                return isIdentifierIgnorable(codePoint);
        }
    }

    public static boolean isIdentifierIgnorable(char ch) {
        return isIdentifierIgnorable((int) ch);
    }

    public static boolean isIdentifierIgnorable(int codePoint) {
        if (codePoint >= 0x00 && codePoint <= 0x08 || codePoint >= 0x0E && codePoint <= 0x1B
                || codePoint >= 0x7F && codePoint <= 0x9F) {
            return true;
        }
        return getType(codePoint) == FORMAT;
    }

    @Deprecated
    public static boolean isSpace(char ch) {
        switch (ch) {
            case '\t':
            case '\n':
            case '\f':
            case '\r':
            case ' ':
                return true;
            default:
                return false;
        }
    }

    public static boolean isSpaceChar(char ch) {
        return isSpaceChar((int) ch);
    }

    public static boolean isSpaceChar(int codePoint) {
        switch (getType(codePoint)) {
            case SPACE_SEPARATOR:
            case LINE_SEPARATOR:
            case PARAGRAPH_SEPARATOR:
                return true;
            default:
                return false;
        }
    }

    public static boolean isWhitespace(char ch) {
        return isWhitespace((int) ch);
    }

    public static boolean isWhitespace(int codePoint) {
        switch (codePoint) {
            case '\t':
            case '\n':
            case 0xB:
            case '\f':
            case '\r':
            case 0x1C:
            case 0x1D:
            case 0x1E:
            case 0x1F:
                return true;
            case 0xA0:
            case 0x2007:
            case 0x202F:
                return false;
            default:
                return isSpaceChar(codePoint);
        }
    }

    public static byte getDirectionality(char ch) {
        return getDirectionality((int) ch);
    }
    
    //start - getDirectionality
    public static byte getDirectionality(int cp) {
        if ((cp >= 0x0000 && cp <= 0x0008)
         || (cp >= 0x000E && cp <= 0x001B)
         || (cp >= 0x007F && cp <= 0x0084)
         || (cp >= 0x0086 && cp <= 0x009F)
         || cp == 0x00AD
         || (cp >= 0x200B && cp <= 0x200D)
         || (cp >= 0x2060 && cp <= 0x2064)
         || (cp >= 0x206A && cp <= 0x206F)
         || cp == 0xFEFF
         || (cp >= 0x1D173 && cp <= 0x1D17A)
         || cp == 0xE0001
         || (cp >= 0xE0020 && cp <= 0xE007F)) {
            return (byte) 9;
        } else if (cp == 0x0009
         || cp == 0x000B
         || cp == 0x001F) {
            return (byte) 11;
        } else if (cp == 0x000A
         || cp == 0x000D
         || (cp >= 0x001C && cp <= 0x001E)
         || cp == 0x0085
         || cp == 0x2029) {
            return (byte) 10;
        } else if (cp == 0x000C
         || cp == 0x0020
         || cp == 0x1680
         || cp == 0x180E
         || (cp >= 0x2000 && cp <= 0x200A)
         || cp == 0x2028
         || cp == 0x205F
         || cp == 0x3000) {
            return (byte) 12;
        } else if ((cp >= 0x0021 && cp <= 0x0022)
         || (cp >= 0x0026 && cp <= 0x002A)
         || (cp >= 0x003B && cp <= 0x0040)
         || (cp >= 0x005B && cp <= 0x0060)
         || (cp >= 0x007B && cp <= 0x007E)
         || cp == 0x00A1
         || (cp >= 0x00A6 && cp <= 0x00A9)
         || (cp >= 0x00AB && cp <= 0x00AC)
         || (cp >= 0x00AE && cp <= 0x00AF)
         || cp == 0x00B4
         || (cp >= 0x00B6 && cp <= 0x00B8)
         || (cp >= 0x00BB && cp <= 0x00BF)
         || cp == 0x00D7
         || cp == 0x00F7
         || (cp >= 0x02B9 && cp <= 0x02BA)
         || (cp >= 0x02C2 && cp <= 0x02CF)
         || (cp >= 0x02D2 && cp <= 0x02DF)
         || (cp >= 0x02E5 && cp <= 0x02ED)
         || (cp >= 0x02EF && cp <= 0x02FF)
         || (cp >= 0x0374 && cp <= 0x0375)
         || cp == 0x037E
         || (cp >= 0x0384 && cp <= 0x0385)
         || cp == 0x0387
         || cp == 0x03F6
         || cp == 0x058A
         || (cp >= 0x0606 && cp <= 0x0607)
         || (cp >= 0x060E && cp <= 0x060F)
         || cp == 0x06DE
         || cp == 0x06E9
         || (cp >= 0x07F6 && cp <= 0x07F9)
         || (cp >= 0x0BF3 && cp <= 0x0BF8)
         || cp == 0x0BFA
         || (cp >= 0x0C78 && cp <= 0x0C7E)
         || (cp >= 0x0F3A && cp <= 0x0F3D)
         || (cp >= 0x1390 && cp <= 0x1399)
         || cp == 0x1400
         || (cp >= 0x169B && cp <= 0x169C)
         || (cp >= 0x17F0 && cp <= 0x17F9)
         || (cp >= 0x1800 && cp <= 0x180A)
         || cp == 0x1940
         || (cp >= 0x1944 && cp <= 0x1945)
         || (cp >= 0x19DE && cp <= 0x19FF)
         || cp == 0x1FBD
         || (cp >= 0x1FBF && cp <= 0x1FC1)
         || (cp >= 0x1FCD && cp <= 0x1FCF)
         || (cp >= 0x1FDD && cp <= 0x1FDF)
         || (cp >= 0x1FED && cp <= 0x1FEF)
         || (cp >= 0x1FFD && cp <= 0x1FFE)
         || (cp >= 0x2010 && cp <= 0x2027)
         || (cp >= 0x2035 && cp <= 0x2043)
         || (cp >= 0x2045 && cp <= 0x205E)
         || (cp >= 0x207C && cp <= 0x207E)
         || (cp >= 0x208C && cp <= 0x208E)
         || (cp >= 0x2100 && cp <= 0x2101)
         || (cp >= 0x2103 && cp <= 0x2106)
         || (cp >= 0x2108 && cp <= 0x2109)
         || cp == 0x2114
         || (cp >= 0x2116 && cp <= 0x2118)
         || (cp >= 0x211E && cp <= 0x2123)
         || cp == 0x2125
         || cp == 0x2127
         || cp == 0x2129
         || (cp >= 0x213A && cp <= 0x213B)
         || (cp >= 0x2140 && cp <= 0x2144)
         || (cp >= 0x214A && cp <= 0x214D)
         || (cp >= 0x2150 && cp <= 0x215F)
         || cp == 0x2189
         || (cp >= 0x2190 && cp <= 0x2211)
         || (cp >= 0x2214 && cp <= 0x2335)
         || (cp >= 0x237B && cp <= 0x2394)
         || (cp >= 0x2396 && cp <= 0x23F3)
         || (cp >= 0x2400 && cp <= 0x2426)
         || (cp >= 0x2440 && cp <= 0x244A)
         || (cp >= 0x2460 && cp <= 0x2487)
         || (cp >= 0x24EA && cp <= 0x26AB)
         || (cp >= 0x26AD && cp <= 0x26FF)
         || (cp >= 0x2701 && cp <= 0x27FF)
         || (cp >= 0x2900 && cp <= 0x2B4C)
         || (cp >= 0x2B50 && cp <= 0x2B59)
         || (cp >= 0x2CE5 && cp <= 0x2CEA)
         || (cp >= 0x2CF9 && cp <= 0x2CFF)
         || (cp >= 0x2E00 && cp <= 0x2E3B)
         || (cp >= 0x2E80 && cp <= 0x2E99)
         || (cp >= 0x2E9B && cp <= 0x2EF3)
         || (cp >= 0x2F00 && cp <= 0x2FD5)
         || (cp >= 0x2FF0 && cp <= 0x2FFB)
         || (cp >= 0x3001 && cp <= 0x3004)
         || (cp >= 0x3008 && cp <= 0x3020)
         || cp == 0x3030
         || (cp >= 0x3036 && cp <= 0x3037)
         || (cp >= 0x303D && cp <= 0x303F)
         || (cp >= 0x309B && cp <= 0x309C)
         || cp == 0x30A0
         || cp == 0x30FB
         || (cp >= 0x31C0 && cp <= 0x31E3)
         || (cp >= 0x321D && cp <= 0x321E)
         || (cp >= 0x3250 && cp <= 0x325F)
         || (cp >= 0x327C && cp <= 0x327E)
         || (cp >= 0x32B1 && cp <= 0x32BF)
         || (cp >= 0x32CC && cp <= 0x32CF)
         || (cp >= 0x3377 && cp <= 0x337A)
         || (cp >= 0x33DE && cp <= 0x33DF)
         || cp == 0x33FF
         || (cp >= 0x4DC0 && cp <= 0x4DFF)
         || (cp >= 0xA490 && cp <= 0xA4C6)
         || (cp >= 0xA60D && cp <= 0xA60F)
         || cp == 0xA673
         || (cp >= 0xA67E && cp <= 0xA67F)
         || (cp >= 0xA700 && cp <= 0xA721)
         || cp == 0xA788
         || (cp >= 0xA828 && cp <= 0xA82B)
         || (cp >= 0xA874 && cp <= 0xA877)
         || (cp >= 0xFD3E && cp <= 0xFD3F)
         || cp == 0xFDFD
         || (cp >= 0xFE10 && cp <= 0xFE19)
         || (cp >= 0xFE30 && cp <= 0xFE4F)
         || cp == 0xFE51
         || cp == 0xFE54
         || (cp >= 0xFE56 && cp <= 0xFE5E)
         || (cp >= 0xFE60 && cp <= 0xFE61)
         || (cp >= 0xFE64 && cp <= 0xFE66)
         || cp == 0xFE68
         || cp == 0xFE6B
         || (cp >= 0xFF01 && cp <= 0xFF02)
         || (cp >= 0xFF06 && cp <= 0xFF0A)
         || (cp >= 0xFF1B && cp <= 0xFF20)
         || (cp >= 0xFF3B && cp <= 0xFF40)
         || (cp >= 0xFF5B && cp <= 0xFF65)
         || (cp >= 0xFFE2 && cp <= 0xFFE4)
         || (cp >= 0xFFE8 && cp <= 0xFFEE)
         || (cp >= 0xFFF9 && cp <= 0xFFFD)
         || cp == 0x10101
         || (cp >= 0x10140 && cp <= 0x1018A)
         || (cp >= 0x10190 && cp <= 0x1019B)
         || cp == 0x1091F
         || (cp >= 0x10B39 && cp <= 0x10B3F)
         || (cp >= 0x11052 && cp <= 0x11065)
         || (cp >= 0x1D200 && cp <= 0x1D241)
         || cp == 0x1D245
         || (cp >= 0x1D300 && cp <= 0x1D356)
         || cp == 0x1D6DB
         || cp == 0x1D715
         || cp == 0x1D74F
         || cp == 0x1D789
         || cp == 0x1D7C3
         || (cp >= 0x1EEF0 && cp <= 0x1EEF1)
         || (cp >= 0x1F000 && cp <= 0x1F02B)
         || (cp >= 0x1F030 && cp <= 0x1F093)
         || (cp >= 0x1F0A0 && cp <= 0x1F0AE)
         || (cp >= 0x1F0B1 && cp <= 0x1F0BE)
         || (cp >= 0x1F0C1 && cp <= 0x1F0CF)
         || (cp >= 0x1F0D1 && cp <= 0x1F0DF)
         || (cp >= 0x1F16A && cp <= 0x1F16B)
         || (cp >= 0x1F300 && cp <= 0x1F320)
         || (cp >= 0x1F330 && cp <= 0x1F335)
         || (cp >= 0x1F337 && cp <= 0x1F37C)
         || (cp >= 0x1F380 && cp <= 0x1F393)
         || (cp >= 0x1F3A0 && cp <= 0x1F3C4)
         || (cp >= 0x1F3C6 && cp <= 0x1F3CA)
         || (cp >= 0x1F3E0 && cp <= 0x1F3F0)
         || (cp >= 0x1F400 && cp <= 0x1F43E)
         || cp == 0x1F440
         || (cp >= 0x1F442 && cp <= 0x1F4F7)
         || (cp >= 0x1F4F9 && cp <= 0x1F4FC)
         || (cp >= 0x1F500 && cp <= 0x1F53D)
         || (cp >= 0x1F540 && cp <= 0x1F543)
         || (cp >= 0x1F550 && cp <= 0x1F567)
         || (cp >= 0x1F5FB && cp <= 0x1F640)
         || (cp >= 0x1F645 && cp <= 0x1F64F)
         || (cp >= 0x1F680 && cp <= 0x1F6C5)
         || (cp >= 0x1F700 && cp <= 0x1F773)) {
            return (byte) 13;
        } else if ((cp >= 0x0023 && cp <= 0x0025)
         || (cp >= 0x00A2 && cp <= 0x00A5)
         || (cp >= 0x00B0 && cp <= 0x00B1)
         || cp == 0x058F
         || (cp >= 0x0609 && cp <= 0x060A)
         || cp == 0x066A
         || (cp >= 0x09F2 && cp <= 0x09F3)
         || cp == 0x09FB
         || cp == 0x0AF1
         || cp == 0x0BF9
         || cp == 0x0E3F
         || cp == 0x17DB
         || (cp >= 0x2030 && cp <= 0x2034)
         || (cp >= 0x20A0 && cp <= 0x20BF)
         || cp == 0x212E
         || cp == 0x2213
         || (cp >= 0xA838 && cp <= 0xA839)
         || cp == 0xFE5F
         || (cp >= 0xFE69 && cp <= 0xFE6A)
         || (cp >= 0xFF03 && cp <= 0xFF05)
         || (cp >= 0xFFE0 && cp <= 0xFFE1)
         || (cp >= 0xFFE5 && cp <= 0xFFE6)) {
            return (byte) 5;
        } else if (cp == 0x002B
         || cp == 0x002D
         || (cp >= 0x207A && cp <= 0x207B)
         || (cp >= 0x208A && cp <= 0x208B)
         || cp == 0x2212
         || cp == 0xFB29
         || (cp >= 0xFE62 && cp <= 0xFE63)
         || cp == 0xFF0B
         || cp == 0xFF0D) {
            return (byte) 4;
        } else if (cp == 0x002C
         || (cp >= 0x002E && cp <= 0x002F)
         || cp == 0x003A
         || cp == 0x00A0
         || cp == 0x060C
         || cp == 0x202F
         || cp == 0x2044
         || cp == 0xFE50
         || cp == 0xFE52
         || cp == 0xFE55
         || cp == 0xFF0C
         || (cp >= 0xFF0E && cp <= 0xFF0F)
         || cp == 0xFF1A) {
            return (byte) 7;
        } else if ((cp >= 0x0030 && cp <= 0x0039)
         || (cp >= 0x00B2 && cp <= 0x00B3)
         || cp == 0x00B9
         || (cp >= 0x06F0 && cp <= 0x06F9)
         || cp == 0x2070
         || (cp >= 0x2074 && cp <= 0x2079)
         || (cp >= 0x2080 && cp <= 0x2089)
         || (cp >= 0x2488 && cp <= 0x249B)
         || (cp >= 0xFF10 && cp <= 0xFF19)
         || (cp >= 0x1D7CE && cp <= 0x1D7FF)
         || (cp >= 0x1F100 && cp <= 0x1F10A)) {
            return (byte) 3;
        } else if ((cp >= 0x0041 && cp <= 0x005A)
         || (cp >= 0x0061 && cp <= 0x007A)
         || cp == 0x00AA
         || cp == 0x00B5
         || cp == 0x00BA
         || (cp >= 0x00C0 && cp <= 0x00D6)
         || (cp >= 0x00D8 && cp <= 0x00F6)
         || (cp >= 0x00F8 && cp <= 0x02B8)
         || (cp >= 0x02BB && cp <= 0x02C1)
         || (cp >= 0x02D0 && cp <= 0x02D1)
         || (cp >= 0x02E0 && cp <= 0x02E4)
         || cp == 0x02EE
         || (cp >= 0x0370 && cp <= 0x0373)
         || (cp >= 0x0376 && cp <= 0x0377)
         || (cp >= 0x037A && cp <= 0x037D)
         || cp == 0x0386
         || (cp >= 0x0388 && cp <= 0x038A)
         || cp == 0x038C
         || (cp >= 0x038E && cp <= 0x03A1)
         || (cp >= 0x03A3 && cp <= 0x03F5)
         || (cp >= 0x03F7 && cp <= 0x0482)
         || (cp >= 0x048A && cp <= 0x0527)
         || (cp >= 0x0531 && cp <= 0x0556)
         || (cp >= 0x0559 && cp <= 0x055F)
         || (cp >= 0x0561 && cp <= 0x0587)
         || cp == 0x0589
         || (cp >= 0x0903 && cp <= 0x0939)
         || cp == 0x093B
         || (cp >= 0x093D && cp <= 0x0940)
         || (cp >= 0x0949 && cp <= 0x094C)
         || (cp >= 0x094E && cp <= 0x0950)
         || (cp >= 0x0958 && cp <= 0x0961)
         || (cp >= 0x0964 && cp <= 0x0977)
         || (cp >= 0x0979 && cp <= 0x097F)
         || (cp >= 0x0982 && cp <= 0x0983)
         || (cp >= 0x0985 && cp <= 0x098C)
         || (cp >= 0x098F && cp <= 0x0990)
         || (cp >= 0x0993 && cp <= 0x09A8)
         || (cp >= 0x09AA && cp <= 0x09B0)
         || cp == 0x09B2
         || (cp >= 0x09B6 && cp <= 0x09B9)
         || (cp >= 0x09BD && cp <= 0x09C0)
         || (cp >= 0x09C7 && cp <= 0x09C8)
         || (cp >= 0x09CB && cp <= 0x09CC)
         || cp == 0x09CE
         || cp == 0x09D7
         || (cp >= 0x09DC && cp <= 0x09DD)
         || (cp >= 0x09DF && cp <= 0x09E1)
         || (cp >= 0x09E6 && cp <= 0x09F1)
         || (cp >= 0x09F4 && cp <= 0x09FA)
         || cp == 0x0A03
         || (cp >= 0x0A05 && cp <= 0x0A0A)
         || (cp >= 0x0A0F && cp <= 0x0A10)
         || (cp >= 0x0A13 && cp <= 0x0A28)
         || (cp >= 0x0A2A && cp <= 0x0A30)
         || (cp >= 0x0A32 && cp <= 0x0A33)
         || (cp >= 0x0A35 && cp <= 0x0A36)
         || (cp >= 0x0A38 && cp <= 0x0A39)
         || (cp >= 0x0A3E && cp <= 0x0A40)
         || (cp >= 0x0A59 && cp <= 0x0A5C)
         || cp == 0x0A5E
         || (cp >= 0x0A66 && cp <= 0x0A6F)
         || (cp >= 0x0A72 && cp <= 0x0A74)
         || cp == 0x0A83
         || (cp >= 0x0A85 && cp <= 0x0A8D)
         || (cp >= 0x0A8F && cp <= 0x0A91)
         || (cp >= 0x0A93 && cp <= 0x0AA8)
         || (cp >= 0x0AAA && cp <= 0x0AB0)
         || (cp >= 0x0AB2 && cp <= 0x0AB3)
         || (cp >= 0x0AB5 && cp <= 0x0AB9)
         || (cp >= 0x0ABD && cp <= 0x0AC0)
         || cp == 0x0AC9
         || (cp >= 0x0ACB && cp <= 0x0ACC)
         || cp == 0x0AD0
         || (cp >= 0x0AE0 && cp <= 0x0AE1)
         || (cp >= 0x0AE6 && cp <= 0x0AF0)
         || (cp >= 0x0B02 && cp <= 0x0B03)
         || (cp >= 0x0B05 && cp <= 0x0B0C)
         || (cp >= 0x0B0F && cp <= 0x0B10)
         || (cp >= 0x0B13 && cp <= 0x0B28)
         || (cp >= 0x0B2A && cp <= 0x0B30)
         || (cp >= 0x0B32 && cp <= 0x0B33)
         || (cp >= 0x0B35 && cp <= 0x0B39)
         || (cp >= 0x0B3D && cp <= 0x0B3E)
         || cp == 0x0B40
         || (cp >= 0x0B47 && cp <= 0x0B48)
         || (cp >= 0x0B4B && cp <= 0x0B4C)
         || cp == 0x0B57
         || (cp >= 0x0B5C && cp <= 0x0B5D)
         || (cp >= 0x0B5F && cp <= 0x0B61)
         || (cp >= 0x0B66 && cp <= 0x0B77)
         || cp == 0x0B83
         || (cp >= 0x0B85 && cp <= 0x0B8A)
         || (cp >= 0x0B8E && cp <= 0x0B90)
         || (cp >= 0x0B92 && cp <= 0x0B95)
         || (cp >= 0x0B99 && cp <= 0x0B9A)
         || cp == 0x0B9C
         || (cp >= 0x0B9E && cp <= 0x0B9F)
         || (cp >= 0x0BA3 && cp <= 0x0BA4)
         || (cp >= 0x0BA8 && cp <= 0x0BAA)
         || (cp >= 0x0BAE && cp <= 0x0BB9)
         || (cp >= 0x0BBE && cp <= 0x0BBF)
         || (cp >= 0x0BC1 && cp <= 0x0BC2)
         || (cp >= 0x0BC6 && cp <= 0x0BC8)
         || (cp >= 0x0BCA && cp <= 0x0BCC)
         || cp == 0x0BD0
         || cp == 0x0BD7
         || (cp >= 0x0BE6 && cp <= 0x0BF2)
         || (cp >= 0x0C01 && cp <= 0x0C03)
         || (cp >= 0x0C05 && cp <= 0x0C0C)
         || (cp >= 0x0C0E && cp <= 0x0C10)
         || (cp >= 0x0C12 && cp <= 0x0C28)
         || (cp >= 0x0C2A && cp <= 0x0C33)
         || (cp >= 0x0C35 && cp <= 0x0C39)
         || cp == 0x0C3D
         || (cp >= 0x0C41 && cp <= 0x0C44)
         || (cp >= 0x0C58 && cp <= 0x0C59)
         || (cp >= 0x0C60 && cp <= 0x0C61)
         || (cp >= 0x0C66 && cp <= 0x0C6F)
         || cp == 0x0C7F
         || (cp >= 0x0C82 && cp <= 0x0C83)
         || (cp >= 0x0C85 && cp <= 0x0C8C)
         || (cp >= 0x0C8E && cp <= 0x0C90)
         || (cp >= 0x0C92 && cp <= 0x0CA8)
         || (cp >= 0x0CAA && cp <= 0x0CB3)
         || (cp >= 0x0CB5 && cp <= 0x0CB9)
         || (cp >= 0x0CBD && cp <= 0x0CC4)
         || (cp >= 0x0CC6 && cp <= 0x0CC8)
         || (cp >= 0x0CCA && cp <= 0x0CCB)
         || (cp >= 0x0CD5 && cp <= 0x0CD6)
         || cp == 0x0CDE
         || (cp >= 0x0CE0 && cp <= 0x0CE1)
         || (cp >= 0x0CE6 && cp <= 0x0CEF)
         || (cp >= 0x0CF1 && cp <= 0x0CF2)
         || (cp >= 0x0D02 && cp <= 0x0D03)
         || (cp >= 0x0D05 && cp <= 0x0D0C)
         || (cp >= 0x0D0E && cp <= 0x0D10)
         || (cp >= 0x0D12 && cp <= 0x0D3A)
         || (cp >= 0x0D3D && cp <= 0x0D40)
         || (cp >= 0x0D46 && cp <= 0x0D48)
         || (cp >= 0x0D4A && cp <= 0x0D4C)
         || cp == 0x0D4E
         || cp == 0x0D57
         || (cp >= 0x0D60 && cp <= 0x0D61)
         || (cp >= 0x0D66 && cp <= 0x0D75)
         || (cp >= 0x0D79 && cp <= 0x0D7F)
         || (cp >= 0x0D82 && cp <= 0x0D83)
         || (cp >= 0x0D85 && cp <= 0x0D96)
         || (cp >= 0x0D9A && cp <= 0x0DB1)
         || (cp >= 0x0DB3 && cp <= 0x0DBB)
         || cp == 0x0DBD
         || (cp >= 0x0DC0 && cp <= 0x0DC6)
         || (cp >= 0x0DCF && cp <= 0x0DD1)
         || (cp >= 0x0DD8 && cp <= 0x0DDF)
         || (cp >= 0x0DF2 && cp <= 0x0DF4)
         || (cp >= 0x0E01 && cp <= 0x0E30)
         || (cp >= 0x0E32 && cp <= 0x0E33)
         || (cp >= 0x0E40 && cp <= 0x0E46)
         || (cp >= 0x0E4F && cp <= 0x0E5B)
         || (cp >= 0x0E81 && cp <= 0x0E82)
         || cp == 0x0E84
         || (cp >= 0x0E87 && cp <= 0x0E88)
         || cp == 0x0E8A
         || cp == 0x0E8D
         || (cp >= 0x0E94 && cp <= 0x0E97)
         || (cp >= 0x0E99 && cp <= 0x0E9F)
         || (cp >= 0x0EA1 && cp <= 0x0EA3)
         || cp == 0x0EA5
         || cp == 0x0EA7
         || (cp >= 0x0EAA && cp <= 0x0EAB)
         || (cp >= 0x0EAD && cp <= 0x0EB0)
         || (cp >= 0x0EB2 && cp <= 0x0EB3)
         || cp == 0x0EBD
         || (cp >= 0x0EC0 && cp <= 0x0EC4)
         || cp == 0x0EC6
         || (cp >= 0x0ED0 && cp <= 0x0ED9)
         || (cp >= 0x0EDC && cp <= 0x0EDF)
         || (cp >= 0x0F00 && cp <= 0x0F17)
         || (cp >= 0x0F1A && cp <= 0x0F34)
         || cp == 0x0F36
         || cp == 0x0F38
         || (cp >= 0x0F3E && cp <= 0x0F47)
         || (cp >= 0x0F49 && cp <= 0x0F6C)
         || cp == 0x0F7F
         || cp == 0x0F85
         || (cp >= 0x0F88 && cp <= 0x0F8C)
         || (cp >= 0x0FBE && cp <= 0x0FC5)
         || (cp >= 0x0FC7 && cp <= 0x0FCC)
         || (cp >= 0x0FCE && cp <= 0x0FDA)
         || (cp >= 0x1000 && cp <= 0x102C)
         || cp == 0x1031
         || cp == 0x1038
         || (cp >= 0x103B && cp <= 0x103C)
         || (cp >= 0x103F && cp <= 0x1057)
         || (cp >= 0x105A && cp <= 0x105D)
         || (cp >= 0x1061 && cp <= 0x1070)
         || (cp >= 0x1075 && cp <= 0x1081)
         || (cp >= 0x1083 && cp <= 0x1084)
         || (cp >= 0x1087 && cp <= 0x108C)
         || (cp >= 0x108E && cp <= 0x109C)
         || (cp >= 0x109E && cp <= 0x10C5)
         || cp == 0x10C7
         || cp == 0x10CD
         || (cp >= 0x10D0 && cp <= 0x1248)
         || (cp >= 0x124A && cp <= 0x124D)
         || (cp >= 0x1250 && cp <= 0x1256)
         || cp == 0x1258
         || (cp >= 0x125A && cp <= 0x125D)
         || (cp >= 0x1260 && cp <= 0x1288)
         || (cp >= 0x128A && cp <= 0x128D)
         || (cp >= 0x1290 && cp <= 0x12B0)
         || (cp >= 0x12B2 && cp <= 0x12B5)
         || (cp >= 0x12B8 && cp <= 0x12BE)
         || cp == 0x12C0
         || (cp >= 0x12C2 && cp <= 0x12C5)
         || (cp >= 0x12C8 && cp <= 0x12D6)
         || (cp >= 0x12D8 && cp <= 0x1310)
         || (cp >= 0x1312 && cp <= 0x1315)
         || (cp >= 0x1318 && cp <= 0x135A)
         || (cp >= 0x1360 && cp <= 0x137C)
         || (cp >= 0x1380 && cp <= 0x138F)
         || (cp >= 0x13A0 && cp <= 0x13F4)
         || (cp >= 0x1401 && cp <= 0x167F)
         || (cp >= 0x1681 && cp <= 0x169A)
         || (cp >= 0x16A0 && cp <= 0x16F0)
         || (cp >= 0x1700 && cp <= 0x170C)
         || (cp >= 0x170E && cp <= 0x1711)
         || (cp >= 0x1720 && cp <= 0x1731)
         || (cp >= 0x1735 && cp <= 0x1736)
         || (cp >= 0x1740 && cp <= 0x1751)
         || (cp >= 0x1760 && cp <= 0x176C)
         || (cp >= 0x176E && cp <= 0x1770)
         || (cp >= 0x1780 && cp <= 0x17B3)
         || cp == 0x17B6
         || (cp >= 0x17BE && cp <= 0x17C5)
         || (cp >= 0x17C7 && cp <= 0x17C8)
         || (cp >= 0x17D4 && cp <= 0x17DA)
         || cp == 0x17DC
         || (cp >= 0x17E0 && cp <= 0x17E9)
         || (cp >= 0x1810 && cp <= 0x1819)
         || (cp >= 0x1820 && cp <= 0x1877)
         || (cp >= 0x1880 && cp <= 0x18A8)
         || cp == 0x18AA
         || (cp >= 0x18B0 && cp <= 0x18F5)
         || (cp >= 0x1900 && cp <= 0x191C)
         || (cp >= 0x1923 && cp <= 0x1926)
         || (cp >= 0x1929 && cp <= 0x192B)
         || (cp >= 0x1930 && cp <= 0x1931)
         || (cp >= 0x1933 && cp <= 0x1938)
         || (cp >= 0x1946 && cp <= 0x196D)
         || (cp >= 0x1970 && cp <= 0x1974)
         || (cp >= 0x1980 && cp <= 0x19AB)
         || (cp >= 0x19B0 && cp <= 0x19C9)
         || (cp >= 0x19D0 && cp <= 0x19DA)
         || (cp >= 0x1A00 && cp <= 0x1A16)
         || (cp >= 0x1A19 && cp <= 0x1A1B)
         || (cp >= 0x1A1E && cp <= 0x1A55)
         || cp == 0x1A57
         || cp == 0x1A61
         || (cp >= 0x1A63 && cp <= 0x1A64)
         || (cp >= 0x1A6D && cp <= 0x1A72)
         || (cp >= 0x1A80 && cp <= 0x1A89)
         || (cp >= 0x1A90 && cp <= 0x1A99)
         || (cp >= 0x1AA0 && cp <= 0x1AAD)
         || (cp >= 0x1B04 && cp <= 0x1B33)
         || cp == 0x1B35
         || cp == 0x1B3B
         || (cp >= 0x1B3D && cp <= 0x1B41)
         || (cp >= 0x1B43 && cp <= 0x1B4B)
         || (cp >= 0x1B50 && cp <= 0x1B6A)
         || (cp >= 0x1B74 && cp <= 0x1B7C)
         || (cp >= 0x1B82 && cp <= 0x1BA1)
         || (cp >= 0x1BA6 && cp <= 0x1BA7)
         || cp == 0x1BAA
         || (cp >= 0x1BAC && cp <= 0x1BE5)
         || cp == 0x1BE7
         || (cp >= 0x1BEA && cp <= 0x1BEC)
         || cp == 0x1BEE
         || (cp >= 0x1BF2 && cp <= 0x1BF3)
         || (cp >= 0x1BFC && cp <= 0x1C2B)
         || (cp >= 0x1C34 && cp <= 0x1C35)
         || (cp >= 0x1C3B && cp <= 0x1C49)
         || (cp >= 0x1C4D && cp <= 0x1C7F)
         || (cp >= 0x1CC0 && cp <= 0x1CC7)
         || cp == 0x1CD3
         || cp == 0x1CE1
         || (cp >= 0x1CE9 && cp <= 0x1CEC)
         || (cp >= 0x1CEE && cp <= 0x1CF3)
         || (cp >= 0x1CF5 && cp <= 0x1CF6)
         || (cp >= 0x1D00 && cp <= 0x1DBF)
         || (cp >= 0x1E00 && cp <= 0x1F15)
         || (cp >= 0x1F18 && cp <= 0x1F1D)
         || (cp >= 0x1F20 && cp <= 0x1F45)
         || (cp >= 0x1F48 && cp <= 0x1F4D)
         || (cp >= 0x1F50 && cp <= 0x1F57)
         || cp == 0x1F59
         || cp == 0x1F5B
         || cp == 0x1F5D
         || (cp >= 0x1F5F && cp <= 0x1F7D)
         || (cp >= 0x1F80 && cp <= 0x1FB4)
         || (cp >= 0x1FB6 && cp <= 0x1FBC)
         || cp == 0x1FBE
         || (cp >= 0x1FC2 && cp <= 0x1FC4)
         || (cp >= 0x1FC6 && cp <= 0x1FCC)
         || (cp >= 0x1FD0 && cp <= 0x1FD3)
         || (cp >= 0x1FD6 && cp <= 0x1FDB)
         || (cp >= 0x1FE0 && cp <= 0x1FEC)
         || (cp >= 0x1FF2 && cp <= 0x1FF4)
         || (cp >= 0x1FF6 && cp <= 0x1FFC)
         || cp == 0x200E
         || cp == 0x2071
         || cp == 0x207F
         || (cp >= 0x2090 && cp <= 0x209C)
         || cp == 0x2102
         || cp == 0x2107
         || (cp >= 0x210A && cp <= 0x2113)
         || cp == 0x2115
         || (cp >= 0x2119 && cp <= 0x211D)
         || cp == 0x2124
         || cp == 0x2126
         || cp == 0x2128
         || (cp >= 0x212A && cp <= 0x212D)
         || (cp >= 0x212F && cp <= 0x2139)
         || (cp >= 0x213C && cp <= 0x213F)
         || (cp >= 0x2145 && cp <= 0x2149)
         || (cp >= 0x214E && cp <= 0x214F)
         || (cp >= 0x2160 && cp <= 0x2188)
         || (cp >= 0x2336 && cp <= 0x237A)
         || cp == 0x2395
         || (cp >= 0x249C && cp <= 0x24E9)
         || cp == 0x26AC
         || (cp >= 0x2800 && cp <= 0x28FF)
         || (cp >= 0x2C00 && cp <= 0x2C2E)
         || (cp >= 0x2C30 && cp <= 0x2C5E)
         || (cp >= 0x2C60 && cp <= 0x2CE4)
         || (cp >= 0x2CEB && cp <= 0x2CEE)
         || (cp >= 0x2CF2 && cp <= 0x2CF3)
         || (cp >= 0x2D00 && cp <= 0x2D25)
         || cp == 0x2D27
         || cp == 0x2D2D
         || (cp >= 0x2D30 && cp <= 0x2D67)
         || (cp >= 0x2D6F && cp <= 0x2D70)
         || (cp >= 0x2D80 && cp <= 0x2D96)
         || (cp >= 0x2DA0 && cp <= 0x2DA6)
         || (cp >= 0x2DA8 && cp <= 0x2DAE)
         || (cp >= 0x2DB0 && cp <= 0x2DB6)
         || (cp >= 0x2DB8 && cp <= 0x2DBE)
         || (cp >= 0x2DC0 && cp <= 0x2DC6)
         || (cp >= 0x2DC8 && cp <= 0x2DCE)
         || (cp >= 0x2DD0 && cp <= 0x2DD6)
         || (cp >= 0x2DD8 && cp <= 0x2DDE)
         || (cp >= 0x3005 && cp <= 0x3007)
         || (cp >= 0x3021 && cp <= 0x3029)
         || (cp >= 0x302E && cp <= 0x302F)
         || (cp >= 0x3031 && cp <= 0x3035)
         || (cp >= 0x3038 && cp <= 0x303C)
         || (cp >= 0x3041 && cp <= 0x3096)
         || (cp >= 0x309D && cp <= 0x309F)
         || (cp >= 0x30A1 && cp <= 0x30FA)
         || (cp >= 0x30FC && cp <= 0x30FF)
         || (cp >= 0x3105 && cp <= 0x312D)
         || (cp >= 0x3131 && cp <= 0x318E)
         || (cp >= 0x3190 && cp <= 0x31BA)
         || (cp >= 0x31F0 && cp <= 0x321C)
         || (cp >= 0x3220 && cp <= 0x324F)
         || (cp >= 0x3260 && cp <= 0x327B)
         || (cp >= 0x327F && cp <= 0x32B0)
         || (cp >= 0x32C0 && cp <= 0x32CB)
         || (cp >= 0x32D0 && cp <= 0x3376)
         || (cp >= 0x337B && cp <= 0x33DD)
         || (cp >= 0x33E0 && cp <= 0x33FE)
         || (cp >= 0x3400 && cp <= 0x4DB5)
         || (cp >= 0x4E00 && cp <= 0x9FCC)
         || (cp >= 0xA000 && cp <= 0xA48C)
         || (cp >= 0xA4D0 && cp <= 0xA60C)
         || (cp >= 0xA610 && cp <= 0xA62B)
         || (cp >= 0xA640 && cp <= 0xA66E)
         || (cp >= 0xA680 && cp <= 0xA697)
         || (cp >= 0xA6A0 && cp <= 0xA6EF)
         || (cp >= 0xA6F2 && cp <= 0xA6F7)
         || (cp >= 0xA722 && cp <= 0xA787)
         || (cp >= 0xA789 && cp <= 0xA78E)
         || (cp >= 0xA790 && cp <= 0xA793)
         || (cp >= 0xA7A0 && cp <= 0xA7AA)
         || (cp >= 0xA7F8 && cp <= 0xA801)
         || (cp >= 0xA803 && cp <= 0xA805)
         || (cp >= 0xA807 && cp <= 0xA80A)
         || (cp >= 0xA80C && cp <= 0xA824)
         || cp == 0xA827
         || (cp >= 0xA830 && cp <= 0xA837)
         || (cp >= 0xA840 && cp <= 0xA873)
         || (cp >= 0xA880 && cp <= 0xA8C3)
         || (cp >= 0xA8CE && cp <= 0xA8D9)
         || (cp >= 0xA8F2 && cp <= 0xA8FB)
         || (cp >= 0xA900 && cp <= 0xA925)
         || (cp >= 0xA92E && cp <= 0xA946)
         || (cp >= 0xA952 && cp <= 0xA953)
         || (cp >= 0xA95F && cp <= 0xA97C)
         || (cp >= 0xA983 && cp <= 0xA9B2)
         || (cp >= 0xA9B4 && cp <= 0xA9B5)
         || (cp >= 0xA9BA && cp <= 0xA9BB)
         || (cp >= 0xA9BD && cp <= 0xA9CD)
         || (cp >= 0xA9CF && cp <= 0xA9D9)
         || (cp >= 0xA9DE && cp <= 0xA9DF)
         || (cp >= 0xAA00 && cp <= 0xAA28)
         || (cp >= 0xAA2F && cp <= 0xAA30)
         || (cp >= 0xAA33 && cp <= 0xAA34)
         || (cp >= 0xAA40 && cp <= 0xAA42)
         || (cp >= 0xAA44 && cp <= 0xAA4B)
         || cp == 0xAA4D
         || (cp >= 0xAA50 && cp <= 0xAA59)
         || (cp >= 0xAA5C && cp <= 0xAA7B)
         || (cp >= 0xAA80 && cp <= 0xAAAF)
         || cp == 0xAAB1
         || (cp >= 0xAAB5 && cp <= 0xAAB6)
         || (cp >= 0xAAB9 && cp <= 0xAABD)
         || cp == 0xAAC0
         || cp == 0xAAC2
         || (cp >= 0xAADB && cp <= 0xAAEB)
         || (cp >= 0xAAEE && cp <= 0xAAF5)
         || (cp >= 0xAB01 && cp <= 0xAB06)
         || (cp >= 0xAB09 && cp <= 0xAB0E)
         || (cp >= 0xAB11 && cp <= 0xAB16)
         || (cp >= 0xAB20 && cp <= 0xAB26)
         || (cp >= 0xAB28 && cp <= 0xAB2E)
         || (cp >= 0xABC0 && cp <= 0xABE4)
         || (cp >= 0xABE6 && cp <= 0xABE7)
         || (cp >= 0xABE9 && cp <= 0xABEC)
         || (cp >= 0xABF0 && cp <= 0xABF9)
         || (cp >= 0xAC00 && cp <= 0xD7A3)
         || (cp >= 0xD7B0 && cp <= 0xD7C6)
         || (cp >= 0xD7CB && cp <= 0xD7FB)
         || (cp >= 0xE000 && cp <= 0xFA6D)
         || (cp >= 0xFA70 && cp <= 0xFAD9)
         || (cp >= 0xFB00 && cp <= 0xFB06)
         || (cp >= 0xFB13 && cp <= 0xFB17)
         || (cp >= 0xFF21 && cp <= 0xFF3A)
         || (cp >= 0xFF41 && cp <= 0xFF5A)
         || (cp >= 0xFF66 && cp <= 0xFFBE)
         || (cp >= 0xFFC2 && cp <= 0xFFC7)
         || (cp >= 0xFFCA && cp <= 0xFFCF)
         || (cp >= 0xFFD2 && cp <= 0xFFD7)
         || (cp >= 0xFFDA && cp <= 0xFFDC)
         || (cp >= 0x10000 && cp <= 0x1000B)
         || (cp >= 0x1000D && cp <= 0x10026)
         || (cp >= 0x10028 && cp <= 0x1003A)
         || (cp >= 0x1003C && cp <= 0x1003D)
         || (cp >= 0x1003F && cp <= 0x1004D)
         || (cp >= 0x10050 && cp <= 0x1005D)
         || (cp >= 0x10080 && cp <= 0x100FA)
         || cp == 0x10100
         || cp == 0x10102
         || (cp >= 0x10107 && cp <= 0x10133)
         || (cp >= 0x10137 && cp <= 0x1013F)
         || (cp >= 0x101D0 && cp <= 0x101FC)
         || (cp >= 0x10280 && cp <= 0x1029C)
         || (cp >= 0x102A0 && cp <= 0x102D0)
         || (cp >= 0x10300 && cp <= 0x1031E)
         || (cp >= 0x10320 && cp <= 0x10323)
         || (cp >= 0x10330 && cp <= 0x1034A)
         || (cp >= 0x10380 && cp <= 0x1039D)
         || (cp >= 0x1039F && cp <= 0x103C3)
         || (cp >= 0x103C8 && cp <= 0x103D5)
         || (cp >= 0x10400 && cp <= 0x1049D)
         || (cp >= 0x104A0 && cp <= 0x104A9)
         || cp == 0x11000
         || (cp >= 0x11002 && cp <= 0x11037)
         || (cp >= 0x11047 && cp <= 0x1104D)
         || (cp >= 0x11066 && cp <= 0x1106F)
         || (cp >= 0x11082 && cp <= 0x110B2)
         || (cp >= 0x110B7 && cp <= 0x110B8)
         || (cp >= 0x110BB && cp <= 0x110C1)
         || (cp >= 0x110D0 && cp <= 0x110E8)
         || (cp >= 0x110F0 && cp <= 0x110F9)
         || (cp >= 0x11103 && cp <= 0x11126)
         || cp == 0x1112C
         || (cp >= 0x11136 && cp <= 0x11143)
         || (cp >= 0x11182 && cp <= 0x111B5)
         || (cp >= 0x111BF && cp <= 0x111C8)
         || (cp >= 0x111D0 && cp <= 0x111D9)
         || (cp >= 0x11680 && cp <= 0x116AA)
         || cp == 0x116AC
         || (cp >= 0x116AE && cp <= 0x116AF)
         || cp == 0x116B6
         || (cp >= 0x116C0 && cp <= 0x116C9)
         || (cp >= 0x12000 && cp <= 0x1236E)
         || (cp >= 0x12400 && cp <= 0x12462)
         || (cp >= 0x12470 && cp <= 0x12473)
         || (cp >= 0x13000 && cp <= 0x1342E)
         || (cp >= 0x16800 && cp <= 0x16A38)
         || (cp >= 0x16F00 && cp <= 0x16F44)
         || (cp >= 0x16F50 && cp <= 0x16F7E)
         || (cp >= 0x16F93 && cp <= 0x16F9F)
         || (cp >= 0x1B000 && cp <= 0x1B001)
         || (cp >= 0x1D000 && cp <= 0x1D0F5)
         || (cp >= 0x1D100 && cp <= 0x1D126)
         || (cp >= 0x1D129 && cp <= 0x1D166)
         || (cp >= 0x1D16A && cp <= 0x1D172)
         || (cp >= 0x1D183 && cp <= 0x1D184)
         || (cp >= 0x1D18C && cp <= 0x1D1A9)
         || (cp >= 0x1D1AE && cp <= 0x1D1DD)
         || (cp >= 0x1D360 && cp <= 0x1D371)
         || (cp >= 0x1D400 && cp <= 0x1D454)
         || (cp >= 0x1D456 && cp <= 0x1D49C)
         || (cp >= 0x1D49E && cp <= 0x1D49F)
         || cp == 0x1D4A2
         || (cp >= 0x1D4A5 && cp <= 0x1D4A6)
         || (cp >= 0x1D4A9 && cp <= 0x1D4AC)
         || (cp >= 0x1D4AE && cp <= 0x1D4B9)
         || cp == 0x1D4BB
         || (cp >= 0x1D4BD && cp <= 0x1D4C3)
         || (cp >= 0x1D4C5 && cp <= 0x1D505)
         || (cp >= 0x1D507 && cp <= 0x1D50A)
         || (cp >= 0x1D50D && cp <= 0x1D514)
         || (cp >= 0x1D516 && cp <= 0x1D51C)
         || (cp >= 0x1D51E && cp <= 0x1D539)
         || (cp >= 0x1D53B && cp <= 0x1D53E)
         || (cp >= 0x1D540 && cp <= 0x1D544)
         || cp == 0x1D546
         || (cp >= 0x1D54A && cp <= 0x1D550)
         || (cp >= 0x1D552 && cp <= 0x1D6A5)
         || (cp >= 0x1D6A8 && cp <= 0x1D6DA)
         || (cp >= 0x1D6DC && cp <= 0x1D714)
         || (cp >= 0x1D716 && cp <= 0x1D74E)
         || (cp >= 0x1D750 && cp <= 0x1D788)
         || (cp >= 0x1D78A && cp <= 0x1D7C2)
         || (cp >= 0x1D7C4 && cp <= 0x1D7CB)
         || (cp >= 0x1F110 && cp <= 0x1F12E)
         || (cp >= 0x1F130 && cp <= 0x1F169)
         || (cp >= 0x1F170 && cp <= 0x1F19A)
         || (cp >= 0x1F1E6 && cp <= 0x1F202)
         || (cp >= 0x1F210 && cp <= 0x1F23A)
         || (cp >= 0x1F240 && cp <= 0x1F248)
         || (cp >= 0x1F250 && cp <= 0x1F251)
         || (cp >= 0x20000 && cp <= 0x2A6D6)
         || (cp >= 0x2A700 && cp <= 0x2B734)
         || (cp >= 0x2B740 && cp <= 0x2B81D)
         || (cp >= 0x2F800 && cp <= 0x2FA1D)
         || (cp >= 0xF0000 && cp <= 0xFFFFD)
         || (cp >= 0x100000 && cp <= 0x10FFFD)) {
            return (byte) 0;
        } else if ((cp >= 0x0300 && cp <= 0x036F)
         || (cp >= 0x0483 && cp <= 0x0489)
         || (cp >= 0x0591 && cp <= 0x05BD)
         || cp == 0x05BF
         || (cp >= 0x05C1 && cp <= 0x05C2)
         || (cp >= 0x05C4 && cp <= 0x05C5)
         || cp == 0x05C7
         || (cp >= 0x0610 && cp <= 0x061A)
         || (cp >= 0x064B && cp <= 0x065F)
         || cp == 0x0670
         || (cp >= 0x06D6 && cp <= 0x06DC)
         || (cp >= 0x06DF && cp <= 0x06E4)
         || (cp >= 0x06E7 && cp <= 0x06E8)
         || (cp >= 0x06EA && cp <= 0x06ED)
         || cp == 0x0711
         || (cp >= 0x0730 && cp <= 0x074A)
         || (cp >= 0x07A6 && cp <= 0x07B0)
         || (cp >= 0x07EB && cp <= 0x07F3)
         || (cp >= 0x0816 && cp <= 0x0819)
         || (cp >= 0x081B && cp <= 0x0823)
         || (cp >= 0x0825 && cp <= 0x0827)
         || (cp >= 0x0829 && cp <= 0x082D)
         || (cp >= 0x0859 && cp <= 0x085B)
         || (cp >= 0x08E4 && cp <= 0x08FE)
         || (cp >= 0x0900 && cp <= 0x0902)
         || cp == 0x093A
         || cp == 0x093C
         || (cp >= 0x0941 && cp <= 0x0948)
         || cp == 0x094D
         || (cp >= 0x0951 && cp <= 0x0957)
         || (cp >= 0x0962 && cp <= 0x0963)
         || cp == 0x0981
         || cp == 0x09BC
         || (cp >= 0x09C1 && cp <= 0x09C4)
         || cp == 0x09CD
         || (cp >= 0x09E2 && cp <= 0x09E3)
         || (cp >= 0x0A01 && cp <= 0x0A02)
         || cp == 0x0A3C
         || (cp >= 0x0A41 && cp <= 0x0A42)
         || (cp >= 0x0A47 && cp <= 0x0A48)
         || (cp >= 0x0A4B && cp <= 0x0A4D)
         || cp == 0x0A51
         || (cp >= 0x0A70 && cp <= 0x0A71)
         || cp == 0x0A75
         || (cp >= 0x0A81 && cp <= 0x0A82)
         || cp == 0x0ABC
         || (cp >= 0x0AC1 && cp <= 0x0AC5)
         || (cp >= 0x0AC7 && cp <= 0x0AC8)
         || cp == 0x0ACD
         || (cp >= 0x0AE2 && cp <= 0x0AE3)
         || cp == 0x0B01
         || cp == 0x0B3C
         || cp == 0x0B3F
         || (cp >= 0x0B41 && cp <= 0x0B44)
         || cp == 0x0B4D
         || cp == 0x0B56
         || (cp >= 0x0B62 && cp <= 0x0B63)
         || cp == 0x0B82
         || cp == 0x0BC0
         || cp == 0x0BCD
         || (cp >= 0x0C3E && cp <= 0x0C40)
         || (cp >= 0x0C46 && cp <= 0x0C48)
         || (cp >= 0x0C4A && cp <= 0x0C4D)
         || (cp >= 0x0C55 && cp <= 0x0C56)
         || (cp >= 0x0C62 && cp <= 0x0C63)
         || cp == 0x0CBC
         || (cp >= 0x0CCC && cp <= 0x0CCD)
         || (cp >= 0x0CE2 && cp <= 0x0CE3)
         || (cp >= 0x0D41 && cp <= 0x0D44)
         || cp == 0x0D4D
         || (cp >= 0x0D62 && cp <= 0x0D63)
         || cp == 0x0DCA
         || (cp >= 0x0DD2 && cp <= 0x0DD4)
         || cp == 0x0DD6
         || cp == 0x0E31
         || (cp >= 0x0E34 && cp <= 0x0E3A)
         || (cp >= 0x0E47 && cp <= 0x0E4E)
         || cp == 0x0EB1
         || (cp >= 0x0EB4 && cp <= 0x0EB9)
         || (cp >= 0x0EBB && cp <= 0x0EBC)
         || (cp >= 0x0EC8 && cp <= 0x0ECD)
         || (cp >= 0x0F18 && cp <= 0x0F19)
         || cp == 0x0F35
         || cp == 0x0F37
         || cp == 0x0F39
         || (cp >= 0x0F71 && cp <= 0x0F7E)
         || (cp >= 0x0F80 && cp <= 0x0F84)
         || (cp >= 0x0F86 && cp <= 0x0F87)
         || (cp >= 0x0F8D && cp <= 0x0F97)
         || (cp >= 0x0F99 && cp <= 0x0FBC)
         || cp == 0x0FC6
         || (cp >= 0x102D && cp <= 0x1030)
         || (cp >= 0x1032 && cp <= 0x1037)
         || (cp >= 0x1039 && cp <= 0x103A)
         || (cp >= 0x103D && cp <= 0x103E)
         || (cp >= 0x1058 && cp <= 0x1059)
         || (cp >= 0x105E && cp <= 0x1060)
         || (cp >= 0x1071 && cp <= 0x1074)
         || cp == 0x1082
         || (cp >= 0x1085 && cp <= 0x1086)
         || cp == 0x108D
         || cp == 0x109D
         || (cp >= 0x135D && cp <= 0x135F)
         || (cp >= 0x1712 && cp <= 0x1714)
         || (cp >= 0x1732 && cp <= 0x1734)
         || (cp >= 0x1752 && cp <= 0x1753)
         || (cp >= 0x1772 && cp <= 0x1773)
         || (cp >= 0x17B4 && cp <= 0x17B5)
         || (cp >= 0x17B7 && cp <= 0x17BD)
         || cp == 0x17C6
         || (cp >= 0x17C9 && cp <= 0x17D3)
         || cp == 0x17DD
         || (cp >= 0x180B && cp <= 0x180D)
         || cp == 0x18A9
         || (cp >= 0x1920 && cp <= 0x1922)
         || (cp >= 0x1927 && cp <= 0x1928)
         || cp == 0x1932
         || (cp >= 0x1939 && cp <= 0x193B)
         || (cp >= 0x1A17 && cp <= 0x1A18)
         || cp == 0x1A56
         || (cp >= 0x1A58 && cp <= 0x1A5E)
         || cp == 0x1A60
         || cp == 0x1A62
         || (cp >= 0x1A65 && cp <= 0x1A6C)
         || (cp >= 0x1A73 && cp <= 0x1A7C)
         || cp == 0x1A7F
         || (cp >= 0x1B00 && cp <= 0x1B03)
         || cp == 0x1B34
         || (cp >= 0x1B36 && cp <= 0x1B3A)
         || cp == 0x1B3C
         || cp == 0x1B42
         || (cp >= 0x1B6B && cp <= 0x1B73)
         || (cp >= 0x1B80 && cp <= 0x1B81)
         || (cp >= 0x1BA2 && cp <= 0x1BA5)
         || (cp >= 0x1BA8 && cp <= 0x1BA9)
         || cp == 0x1BAB
         || cp == 0x1BE6
         || (cp >= 0x1BE8 && cp <= 0x1BE9)
         || cp == 0x1BED
         || (cp >= 0x1BEF && cp <= 0x1BF1)
         || (cp >= 0x1C2C && cp <= 0x1C33)
         || (cp >= 0x1C36 && cp <= 0x1C37)
         || (cp >= 0x1CD0 && cp <= 0x1CD2)
         || (cp >= 0x1CD4 && cp <= 0x1CE0)
         || (cp >= 0x1CE2 && cp <= 0x1CE8)
         || cp == 0x1CED
         || cp == 0x1CF4
         || (cp >= 0x1DC0 && cp <= 0x1DE6)
         || (cp >= 0x1DFC && cp <= 0x1DFF)
         || (cp >= 0x20D0 && cp <= 0x20F0)
         || (cp >= 0x2CEF && cp <= 0x2CF1)
         || cp == 0x2D7F
         || (cp >= 0x2DE0 && cp <= 0x2DFF)
         || (cp >= 0x302A && cp <= 0x302D)
         || (cp >= 0x3099 && cp <= 0x309A)
         || (cp >= 0xA66F && cp <= 0xA672)
         || (cp >= 0xA674 && cp <= 0xA67D)
         || cp == 0xA69F
         || (cp >= 0xA6F0 && cp <= 0xA6F1)
         || cp == 0xA802
         || cp == 0xA806
         || cp == 0xA80B
         || (cp >= 0xA825 && cp <= 0xA826)
         || cp == 0xA8C4
         || (cp >= 0xA8E0 && cp <= 0xA8F1)
         || (cp >= 0xA926 && cp <= 0xA92D)
         || (cp >= 0xA947 && cp <= 0xA951)
         || (cp >= 0xA980 && cp <= 0xA982)
         || cp == 0xA9B3
         || (cp >= 0xA9B6 && cp <= 0xA9B9)
         || cp == 0xA9BC
         || (cp >= 0xAA29 && cp <= 0xAA2E)
         || (cp >= 0xAA31 && cp <= 0xAA32)
         || (cp >= 0xAA35 && cp <= 0xAA36)
         || cp == 0xAA43
         || cp == 0xAA4C
         || cp == 0xAAB0
         || (cp >= 0xAAB2 && cp <= 0xAAB4)
         || (cp >= 0xAAB7 && cp <= 0xAAB8)
         || (cp >= 0xAABE && cp <= 0xAABF)
         || cp == 0xAAC1
         || (cp >= 0xAAEC && cp <= 0xAAED)
         || cp == 0xAAF6
         || cp == 0xABE5
         || cp == 0xABE8
         || cp == 0xABED
         || cp == 0xFB1E
         || (cp >= 0xFE00 && cp <= 0xFE0F)
         || (cp >= 0xFE20 && cp <= 0xFE26)
         || cp == 0x101FD
         || (cp >= 0x10A01 && cp <= 0x10A03)
         || (cp >= 0x10A05 && cp <= 0x10A06)
         || (cp >= 0x10A0C && cp <= 0x10A0F)
         || (cp >= 0x10A38 && cp <= 0x10A3A)
         || cp == 0x10A3F
         || cp == 0x11001
         || (cp >= 0x11038 && cp <= 0x11046)
         || (cp >= 0x11080 && cp <= 0x11081)
         || (cp >= 0x110B3 && cp <= 0x110B6)
         || (cp >= 0x110B9 && cp <= 0x110BA)
         || (cp >= 0x11100 && cp <= 0x11102)
         || (cp >= 0x11127 && cp <= 0x1112B)
         || (cp >= 0x1112D && cp <= 0x11134)
         || (cp >= 0x11180 && cp <= 0x11181)
         || (cp >= 0x111B6 && cp <= 0x111BE)
         || cp == 0x116AB
         || cp == 0x116AD
         || (cp >= 0x116B0 && cp <= 0x116B5)
         || cp == 0x116B7
         || (cp >= 0x16F8F && cp <= 0x16F92)
         || (cp >= 0x1D167 && cp <= 0x1D169)
         || (cp >= 0x1D17B && cp <= 0x1D182)
         || (cp >= 0x1D185 && cp <= 0x1D18B)
         || (cp >= 0x1D1AA && cp <= 0x1D1AD)
         || (cp >= 0x1D242 && cp <= 0x1D244)
         || (cp >= 0xE0100 && cp <= 0xE01EF)) {
            return (byte) 8;
        } else if ((cp >= 0x0378 && cp <= 0x0379)
         || (cp >= 0x037F && cp <= 0x0383)
         || cp == 0x038B
         || cp == 0x038D
         || cp == 0x03A2
         || (cp >= 0x0528 && cp <= 0x0530)
         || (cp >= 0x0557 && cp <= 0x0558)
         || cp == 0x0560
         || cp == 0x0588
         || (cp >= 0x058B && cp <= 0x058E)
         || cp == 0x0590
         || (cp >= 0x05C8 && cp <= 0x05CF)
         || (cp >= 0x05EB && cp <= 0x05EF)
         || (cp >= 0x05F5 && cp <= 0x05FF)
         || cp == 0x0605
         || (cp >= 0x061C && cp <= 0x061D)
         || cp == 0x070E
         || (cp >= 0x074B && cp <= 0x074C)
         || (cp >= 0x07B2 && cp <= 0x07BF)
         || (cp >= 0x07FB && cp <= 0x07FF)
         || (cp >= 0x082E && cp <= 0x082F)
         || cp == 0x083F
         || (cp >= 0x085C && cp <= 0x085D)
         || (cp >= 0x085F && cp <= 0x089F)
         || cp == 0x08A1
         || (cp >= 0x08AD && cp <= 0x08E3)
         || cp == 0x08FF
         || cp == 0x0978
         || cp == 0x0980
         || cp == 0x0984
         || (cp >= 0x098D && cp <= 0x098E)
         || (cp >= 0x0991 && cp <= 0x0992)
         || cp == 0x09A9
         || cp == 0x09B1
         || (cp >= 0x09B3 && cp <= 0x09B5)
         || (cp >= 0x09BA && cp <= 0x09BB)
         || (cp >= 0x09C5 && cp <= 0x09C6)
         || (cp >= 0x09C9 && cp <= 0x09CA)
         || (cp >= 0x09CF && cp <= 0x09D6)
         || (cp >= 0x09D8 && cp <= 0x09DB)
         || cp == 0x09DE
         || (cp >= 0x09E4 && cp <= 0x09E5)
         || (cp >= 0x09FC && cp <= 0x0A00)
         || cp == 0x0A04
         || (cp >= 0x0A0B && cp <= 0x0A0E)
         || (cp >= 0x0A11 && cp <= 0x0A12)
         || cp == 0x0A29
         || cp == 0x0A31
         || cp == 0x0A34
         || cp == 0x0A37
         || (cp >= 0x0A3A && cp <= 0x0A3B)
         || cp == 0x0A3D
         || (cp >= 0x0A43 && cp <= 0x0A46)
         || (cp >= 0x0A49 && cp <= 0x0A4A)
         || (cp >= 0x0A4E && cp <= 0x0A50)
         || (cp >= 0x0A52 && cp <= 0x0A58)
         || cp == 0x0A5D
         || (cp >= 0x0A5F && cp <= 0x0A65)
         || (cp >= 0x0A76 && cp <= 0x0A80)
         || cp == 0x0A84
         || cp == 0x0A8E
         || cp == 0x0A92
         || cp == 0x0AA9
         || cp == 0x0AB1
         || cp == 0x0AB4
         || (cp >= 0x0ABA && cp <= 0x0ABB)
         || cp == 0x0AC6
         || cp == 0x0ACA
         || (cp >= 0x0ACE && cp <= 0x0ACF)
         || (cp >= 0x0AD1 && cp <= 0x0ADF)
         || (cp >= 0x0AE4 && cp <= 0x0AE5)
         || (cp >= 0x0AF2 && cp <= 0x0B00)
         || cp == 0x0B04
         || (cp >= 0x0B0D && cp <= 0x0B0E)
         || (cp >= 0x0B11 && cp <= 0x0B12)
         || cp == 0x0B29
         || cp == 0x0B31
         || cp == 0x0B34
         || (cp >= 0x0B3A && cp <= 0x0B3B)
         || (cp >= 0x0B45 && cp <= 0x0B46)
         || (cp >= 0x0B49 && cp <= 0x0B4A)
         || (cp >= 0x0B4E && cp <= 0x0B55)
         || (cp >= 0x0B58 && cp <= 0x0B5B)
         || cp == 0x0B5E
         || (cp >= 0x0B64 && cp <= 0x0B65)
         || (cp >= 0x0B78 && cp <= 0x0B81)
         || cp == 0x0B84
         || (cp >= 0x0B8B && cp <= 0x0B8D)
         || cp == 0x0B91
         || (cp >= 0x0B96 && cp <= 0x0B98)
         || cp == 0x0B9B
         || cp == 0x0B9D
         || (cp >= 0x0BA0 && cp <= 0x0BA2)
         || (cp >= 0x0BA5 && cp <= 0x0BA7)
         || (cp >= 0x0BAB && cp <= 0x0BAD)
         || (cp >= 0x0BBA && cp <= 0x0BBD)
         || (cp >= 0x0BC3 && cp <= 0x0BC5)
         || cp == 0x0BC9
         || (cp >= 0x0BCE && cp <= 0x0BCF)
         || (cp >= 0x0BD1 && cp <= 0x0BD6)
         || (cp >= 0x0BD8 && cp <= 0x0BE5)
         || (cp >= 0x0BFB && cp <= 0x0C00)
         || cp == 0x0C04
         || cp == 0x0C0D
         || cp == 0x0C11
         || cp == 0x0C29
         || cp == 0x0C34
         || (cp >= 0x0C3A && cp <= 0x0C3C)
         || cp == 0x0C45
         || cp == 0x0C49
         || (cp >= 0x0C4E && cp <= 0x0C54)
         || cp == 0x0C57
         || (cp >= 0x0C5A && cp <= 0x0C5F)
         || (cp >= 0x0C64 && cp <= 0x0C65)
         || (cp >= 0x0C70 && cp <= 0x0C77)
         || (cp >= 0x0C80 && cp <= 0x0C81)
         || cp == 0x0C84
         || cp == 0x0C8D
         || cp == 0x0C91
         || cp == 0x0CA9
         || cp == 0x0CB4
         || (cp >= 0x0CBA && cp <= 0x0CBB)
         || cp == 0x0CC5
         || cp == 0x0CC9
         || (cp >= 0x0CCE && cp <= 0x0CD4)
         || (cp >= 0x0CD7 && cp <= 0x0CDD)
         || cp == 0x0CDF
         || (cp >= 0x0CE4 && cp <= 0x0CE5)
         || cp == 0x0CF0
         || (cp >= 0x0CF3 && cp <= 0x0D01)
         || cp == 0x0D04
         || cp == 0x0D0D
         || cp == 0x0D11
         || (cp >= 0x0D3B && cp <= 0x0D3C)
         || cp == 0x0D45
         || cp == 0x0D49
         || (cp >= 0x0D4F && cp <= 0x0D56)
         || (cp >= 0x0D58 && cp <= 0x0D5F)
         || (cp >= 0x0D64 && cp <= 0x0D65)
         || (cp >= 0x0D76 && cp <= 0x0D78)
         || (cp >= 0x0D80 && cp <= 0x0D81)
         || cp == 0x0D84
         || (cp >= 0x0D97 && cp <= 0x0D99)
         || cp == 0x0DB2
         || cp == 0x0DBC
         || (cp >= 0x0DBE && cp <= 0x0DBF)
         || (cp >= 0x0DC7 && cp <= 0x0DC9)
         || (cp >= 0x0DCB && cp <= 0x0DCE)
         || cp == 0x0DD5
         || cp == 0x0DD7
         || (cp >= 0x0DE0 && cp <= 0x0DF1)
         || (cp >= 0x0DF5 && cp <= 0x0E00)
         || (cp >= 0x0E3B && cp <= 0x0E3E)
         || (cp >= 0x0E5C && cp <= 0x0E80)
         || cp == 0x0E83
         || (cp >= 0x0E85 && cp <= 0x0E86)
         || cp == 0x0E89
         || (cp >= 0x0E8B && cp <= 0x0E8C)
         || (cp >= 0x0E8E && cp <= 0x0E93)
         || cp == 0x0E98
         || cp == 0x0EA0
         || cp == 0x0EA4
         || cp == 0x0EA6
         || (cp >= 0x0EA8 && cp <= 0x0EA9)
         || cp == 0x0EAC
         || cp == 0x0EBA
         || (cp >= 0x0EBE && cp <= 0x0EBF)
         || cp == 0x0EC5
         || cp == 0x0EC7
         || (cp >= 0x0ECE && cp <= 0x0ECF)
         || (cp >= 0x0EDA && cp <= 0x0EDB)
         || (cp >= 0x0EE0 && cp <= 0x0EFF)
         || cp == 0x0F48
         || (cp >= 0x0F6D && cp <= 0x0F70)
         || cp == 0x0F98
         || cp == 0x0FBD
         || cp == 0x0FCD
         || (cp >= 0x0FDB && cp <= 0x0FFF)
         || cp == 0x10C6
         || (cp >= 0x10C8 && cp <= 0x10CC)
         || (cp >= 0x10CE && cp <= 0x10CF)
         || cp == 0x1249
         || (cp >= 0x124E && cp <= 0x124F)
         || cp == 0x1257
         || cp == 0x1259
         || (cp >= 0x125E && cp <= 0x125F)
         || cp == 0x1289
         || (cp >= 0x128E && cp <= 0x128F)
         || cp == 0x12B1
         || (cp >= 0x12B6 && cp <= 0x12B7)
         || cp == 0x12BF
         || cp == 0x12C1
         || (cp >= 0x12C6 && cp <= 0x12C7)
         || cp == 0x12D7
         || cp == 0x1311
         || (cp >= 0x1316 && cp <= 0x1317)
         || (cp >= 0x135B && cp <= 0x135C)
         || (cp >= 0x137D && cp <= 0x137F)
         || (cp >= 0x139A && cp <= 0x139F)
         || (cp >= 0x13F5 && cp <= 0x13FF)
         || (cp >= 0x169D && cp <= 0x169F)
         || (cp >= 0x16F1 && cp <= 0x16FF)
         || cp == 0x170D
         || (cp >= 0x1715 && cp <= 0x171F)
         || (cp >= 0x1737 && cp <= 0x173F)
         || (cp >= 0x1754 && cp <= 0x175F)
         || cp == 0x176D
         || cp == 0x1771
         || (cp >= 0x1774 && cp <= 0x177F)
         || (cp >= 0x17DE && cp <= 0x17DF)
         || (cp >= 0x17EA && cp <= 0x17EF)
         || (cp >= 0x17FA && cp <= 0x17FF)
         || cp == 0x180F
         || (cp >= 0x181A && cp <= 0x181F)
         || (cp >= 0x1878 && cp <= 0x187F)
         || (cp >= 0x18AB && cp <= 0x18AF)
         || (cp >= 0x18F6 && cp <= 0x18FF)
         || (cp >= 0x191D && cp <= 0x191F)
         || (cp >= 0x192C && cp <= 0x192F)
         || (cp >= 0x193C && cp <= 0x193F)
         || (cp >= 0x1941 && cp <= 0x1943)
         || (cp >= 0x196E && cp <= 0x196F)
         || (cp >= 0x1975 && cp <= 0x197F)
         || (cp >= 0x19AC && cp <= 0x19AF)
         || (cp >= 0x19CA && cp <= 0x19CF)
         || (cp >= 0x19DB && cp <= 0x19DD)
         || (cp >= 0x1A1C && cp <= 0x1A1D)
         || cp == 0x1A5F
         || (cp >= 0x1A7D && cp <= 0x1A7E)
         || (cp >= 0x1A8A && cp <= 0x1A8F)
         || (cp >= 0x1A9A && cp <= 0x1A9F)
         || (cp >= 0x1AAE && cp <= 0x1AFF)
         || (cp >= 0x1B4C && cp <= 0x1B4F)
         || (cp >= 0x1B7D && cp <= 0x1B7F)
         || (cp >= 0x1BF4 && cp <= 0x1BFB)
         || (cp >= 0x1C38 && cp <= 0x1C3A)
         || (cp >= 0x1C4A && cp <= 0x1C4C)
         || (cp >= 0x1C80 && cp <= 0x1CBF)
         || (cp >= 0x1CC8 && cp <= 0x1CCF)
         || (cp >= 0x1CF7 && cp <= 0x1CFF)
         || (cp >= 0x1DE7 && cp <= 0x1DFB)
         || (cp >= 0x1F16 && cp <= 0x1F17)
         || (cp >= 0x1F1E && cp <= 0x1F1F)
         || (cp >= 0x1F46 && cp <= 0x1F47)
         || (cp >= 0x1F4E && cp <= 0x1F4F)
         || cp == 0x1F58
         || cp == 0x1F5A
         || cp == 0x1F5C
         || cp == 0x1F5E
         || (cp >= 0x1F7E && cp <= 0x1F7F)
         || cp == 0x1FB5
         || cp == 0x1FC5
         || (cp >= 0x1FD4 && cp <= 0x1FD5)
         || cp == 0x1FDC
         || (cp >= 0x1FF0 && cp <= 0x1FF1)
         || cp == 0x1FF5
         || cp == 0x1FFF
         || (cp >= 0x2065 && cp <= 0x2069)
         || (cp >= 0x2072 && cp <= 0x2073)
         || cp == 0x208F
         || (cp >= 0x209D && cp <= 0x209F)
         || (cp >= 0x20C0 && cp <= 0x20CF)
         || (cp >= 0x20F1 && cp <= 0x20FF)
         || (cp >= 0x218A && cp <= 0x218F)
         || (cp >= 0x23F4 && cp <= 0x23FF)
         || (cp >= 0x2427 && cp <= 0x243F)
         || (cp >= 0x244B && cp <= 0x245F)
         || cp == 0x2700
         || (cp >= 0x2B4D && cp <= 0x2B4F)
         || (cp >= 0x2B5A && cp <= 0x2BFF)
         || cp == 0x2C2F
         || cp == 0x2C5F
         || (cp >= 0x2CF4 && cp <= 0x2CF8)
         || cp == 0x2D26
         || (cp >= 0x2D28 && cp <= 0x2D2C)
         || (cp >= 0x2D2E && cp <= 0x2D2F)
         || (cp >= 0x2D68 && cp <= 0x2D6E)
         || (cp >= 0x2D71 && cp <= 0x2D7E)
         || (cp >= 0x2D97 && cp <= 0x2D9F)
         || cp == 0x2DA7
         || cp == 0x2DAF
         || cp == 0x2DB7
         || cp == 0x2DBF
         || cp == 0x2DC7
         || cp == 0x2DCF
         || cp == 0x2DD7
         || cp == 0x2DDF
         || (cp >= 0x2E3C && cp <= 0x2E7F)
         || cp == 0x2E9A
         || (cp >= 0x2EF4 && cp <= 0x2EFF)
         || (cp >= 0x2FD6 && cp <= 0x2FEF)
         || (cp >= 0x2FFC && cp <= 0x2FFF)
         || cp == 0x3040
         || (cp >= 0x3097 && cp <= 0x3098)
         || (cp >= 0x3100 && cp <= 0x3104)
         || (cp >= 0x312E && cp <= 0x3130)
         || cp == 0x318F
         || (cp >= 0x31BB && cp <= 0x31BF)
         || (cp >= 0x31E4 && cp <= 0x31EF)
         || cp == 0x321F
         || (cp >= 0x4DB6 && cp <= 0x4DBF)
         || (cp >= 0x9FCD && cp <= 0x9FFF)
         || (cp >= 0xA48D && cp <= 0xA48F)
         || (cp >= 0xA4C7 && cp <= 0xA4CF)
         || (cp >= 0xA62C && cp <= 0xA63F)
         || (cp >= 0xA698 && cp <= 0xA69E)
         || (cp >= 0xA6F8 && cp <= 0xA6FF)
         || cp == 0xA78F
         || (cp >= 0xA794 && cp <= 0xA79F)
         || (cp >= 0xA7AB && cp <= 0xA7F7)
         || (cp >= 0xA82C && cp <= 0xA82F)
         || (cp >= 0xA83A && cp <= 0xA83F)
         || (cp >= 0xA878 && cp <= 0xA87F)
         || (cp >= 0xA8C5 && cp <= 0xA8CD)
         || (cp >= 0xA8DA && cp <= 0xA8DF)
         || (cp >= 0xA8FC && cp <= 0xA8FF)
         || (cp >= 0xA954 && cp <= 0xA95E)
         || (cp >= 0xA97D && cp <= 0xA97F)
         || cp == 0xA9CE
         || (cp >= 0xA9DA && cp <= 0xA9DD)
         || (cp >= 0xA9E0 && cp <= 0xA9FF)
         || (cp >= 0xAA37 && cp <= 0xAA3F)
         || (cp >= 0xAA4E && cp <= 0xAA4F)
         || (cp >= 0xAA5A && cp <= 0xAA5B)
         || (cp >= 0xAA7C && cp <= 0xAA7F)
         || (cp >= 0xAAC3 && cp <= 0xAADA)
         || (cp >= 0xAAF7 && cp <= 0xAB00)
         || (cp >= 0xAB07 && cp <= 0xAB08)
         || (cp >= 0xAB0F && cp <= 0xAB10)
         || (cp >= 0xAB17 && cp <= 0xAB1F)
         || cp == 0xAB27
         || (cp >= 0xAB2F && cp <= 0xABBF)
         || (cp >= 0xABEE && cp <= 0xABEF)
         || (cp >= 0xABFA && cp <= 0xABFF)
         || (cp >= 0xD7A4 && cp <= 0xD7AF)
         || (cp >= 0xD7C7 && cp <= 0xD7CA)
         || (cp >= 0xD7FC && cp <= 0xD7FF)
         || (cp >= 0xFA6E && cp <= 0xFA6F)
         || (cp >= 0xFADA && cp <= 0xFAFF)
         || (cp >= 0xFB07 && cp <= 0xFB12)
         || (cp >= 0xFB18 && cp <= 0xFB1C)
         || cp == 0xFB37
         || cp == 0xFB3D
         || cp == 0xFB3F
         || cp == 0xFB42
         || cp == 0xFB45
         || (cp >= 0xFBC2 && cp <= 0xFBD2)
         || (cp >= 0xFD40 && cp <= 0xFD4F)
         || (cp >= 0xFD90 && cp <= 0xFD91)
         || (cp >= 0xFDC8 && cp <= 0xFDEF)
         || (cp >= 0xFDFE && cp <= 0xFDFF)
         || (cp >= 0xFE1A && cp <= 0xFE1F)
         || (cp >= 0xFE27 && cp <= 0xFE2F)
         || cp == 0xFE53
         || cp == 0xFE67
         || (cp >= 0xFE6C && cp <= 0xFE6F)
         || cp == 0xFE75
         || (cp >= 0xFEFD && cp <= 0xFEFE)
         || cp == 0xFF00
         || (cp >= 0xFFBF && cp <= 0xFFC1)
         || (cp >= 0xFFC8 && cp <= 0xFFC9)
         || (cp >= 0xFFD0 && cp <= 0xFFD1)
         || (cp >= 0xFFD8 && cp <= 0xFFD9)
         || (cp >= 0xFFDD && cp <= 0xFFDF)
         || cp == 0xFFE7
         || (cp >= 0xFFEF && cp <= 0xFFF8)
         || (cp >= 0xFFFE && cp <= 0xFFFF)
         || cp == 0x1000C
         || cp == 0x10027
         || cp == 0x1003B
         || cp == 0x1003E
         || (cp >= 0x1004E && cp <= 0x1004F)
         || (cp >= 0x1005E && cp <= 0x1007F)
         || (cp >= 0x100FB && cp <= 0x100FF)
         || (cp >= 0x10103 && cp <= 0x10106)
         || (cp >= 0x10134 && cp <= 0x10136)
         || (cp >= 0x1018B && cp <= 0x1018F)
         || (cp >= 0x1019C && cp <= 0x101CF)
         || (cp >= 0x101FE && cp <= 0x1027F)
         || (cp >= 0x1029D && cp <= 0x1029F)
         || (cp >= 0x102D1 && cp <= 0x102FF)
         || cp == 0x1031F
         || (cp >= 0x10324 && cp <= 0x1032F)
         || (cp >= 0x1034B && cp <= 0x1037F)
         || cp == 0x1039E
         || (cp >= 0x103C4 && cp <= 0x103C7)
         || (cp >= 0x103D6 && cp <= 0x103FF)
         || (cp >= 0x1049E && cp <= 0x1049F)
         || (cp >= 0x104AA && cp <= 0x107FF)
         || (cp >= 0x10806 && cp <= 0x10807)
         || cp == 0x10809
         || cp == 0x10836
         || (cp >= 0x10839 && cp <= 0x1083B)
         || (cp >= 0x1083D && cp <= 0x1083E)
         || cp == 0x10856
         || (cp >= 0x10860 && cp <= 0x108FF)
         || (cp >= 0x1091C && cp <= 0x1091E)
         || (cp >= 0x1093A && cp <= 0x1093E)
         || (cp >= 0x10940 && cp <= 0x1097F)
         || (cp >= 0x109B8 && cp <= 0x109BD)
         || (cp >= 0x109C0 && cp <= 0x109FF)
         || cp == 0x10A04
         || (cp >= 0x10A07 && cp <= 0x10A0B)
         || cp == 0x10A14
         || cp == 0x10A18
         || (cp >= 0x10A34 && cp <= 0x10A37)
         || (cp >= 0x10A3B && cp <= 0x10A3E)
         || (cp >= 0x10A48 && cp <= 0x10A4F)
         || (cp >= 0x10A59 && cp <= 0x10A5F)
         || (cp >= 0x10A80 && cp <= 0x10AFF)
         || (cp >= 0x10B36 && cp <= 0x10B38)
         || (cp >= 0x10B56 && cp <= 0x10B57)
         || (cp >= 0x10B73 && cp <= 0x10B77)
         || (cp >= 0x10B80 && cp <= 0x10BFF)
         || (cp >= 0x10C49 && cp <= 0x10E5F)
         || (cp >= 0x10E7F && cp <= 0x10FFF)
         || (cp >= 0x1104E && cp <= 0x11051)
         || (cp >= 0x11070 && cp <= 0x1107F)
         || (cp >= 0x110C2 && cp <= 0x110CF)
         || (cp >= 0x110E9 && cp <= 0x110EF)
         || (cp >= 0x110FA && cp <= 0x110FF)
         || cp == 0x11135
         || (cp >= 0x11144 && cp <= 0x1117F)
         || (cp >= 0x111C9 && cp <= 0x111CF)
         || (cp >= 0x111DA && cp <= 0x1167F)
         || (cp >= 0x116B8 && cp <= 0x116BF)
         || (cp >= 0x116CA && cp <= 0x11FFF)
         || (cp >= 0x1236F && cp <= 0x123FF)
         || (cp >= 0x12463 && cp <= 0x1246F)
         || (cp >= 0x12474 && cp <= 0x12FFF)
         || (cp >= 0x1342F && cp <= 0x167FF)
         || (cp >= 0x16A39 && cp <= 0x16EFF)
         || (cp >= 0x16F45 && cp <= 0x16F4F)
         || (cp >= 0x16F7F && cp <= 0x16F8E)
         || (cp >= 0x16FA0 && cp <= 0x1AFFF)
         || (cp >= 0x1B002 && cp <= 0x1CFFF)
         || (cp >= 0x1D0F6 && cp <= 0x1D0FF)
         || (cp >= 0x1D127 && cp <= 0x1D128)
         || (cp >= 0x1D1DE && cp <= 0x1D1FF)
         || (cp >= 0x1D246 && cp <= 0x1D2FF)
         || (cp >= 0x1D357 && cp <= 0x1D35F)
         || (cp >= 0x1D372 && cp <= 0x1D3FF)
         || cp == 0x1D455
         || cp == 0x1D49D
         || (cp >= 0x1D4A0 && cp <= 0x1D4A1)
         || (cp >= 0x1D4A3 && cp <= 0x1D4A4)
         || (cp >= 0x1D4A7 && cp <= 0x1D4A8)
         || cp == 0x1D4AD
         || cp == 0x1D4BA
         || cp == 0x1D4BC
         || cp == 0x1D4C4
         || cp == 0x1D506
         || (cp >= 0x1D50B && cp <= 0x1D50C)
         || cp == 0x1D515
         || cp == 0x1D51D
         || cp == 0x1D53A
         || cp == 0x1D53F
         || cp == 0x1D545
         || (cp >= 0x1D547 && cp <= 0x1D549)
         || cp == 0x1D551
         || (cp >= 0x1D6A6 && cp <= 0x1D6A7)
         || (cp >= 0x1D7CC && cp <= 0x1D7CD)
         || (cp >= 0x1D800 && cp <= 0x1EDFF)
         || cp == 0x1EE04
         || cp == 0x1EE20
         || cp == 0x1EE23
         || (cp >= 0x1EE25 && cp <= 0x1EE26)
         || cp == 0x1EE28
         || cp == 0x1EE33
         || cp == 0x1EE38
         || cp == 0x1EE3A
         || (cp >= 0x1EE3C && cp <= 0x1EE41)
         || (cp >= 0x1EE43 && cp <= 0x1EE46)
         || cp == 0x1EE48
         || cp == 0x1EE4A
         || cp == 0x1EE4C
         || cp == 0x1EE50
         || cp == 0x1EE53
         || (cp >= 0x1EE55 && cp <= 0x1EE56)
         || cp == 0x1EE58
         || cp == 0x1EE5A
         || cp == 0x1EE5C
         || cp == 0x1EE5E
         || cp == 0x1EE60
         || cp == 0x1EE63
         || (cp >= 0x1EE65 && cp <= 0x1EE66)
         || cp == 0x1EE6B
         || cp == 0x1EE73
         || cp == 0x1EE78
         || cp == 0x1EE7D
         || cp == 0x1EE7F
         || cp == 0x1EE8A
         || (cp >= 0x1EE9C && cp <= 0x1EEA0)
         || cp == 0x1EEA4
         || cp == 0x1EEAA
         || (cp >= 0x1EEBC && cp <= 0x1EEEF)
         || (cp >= 0x1EEF2 && cp <= 0x1EFFF)
         || (cp >= 0x1F02C && cp <= 0x1F02F)
         || (cp >= 0x1F094 && cp <= 0x1F09F)
         || (cp >= 0x1F0AF && cp <= 0x1F0B0)
         || (cp >= 0x1F0BF && cp <= 0x1F0C0)
         || cp == 0x1F0D0
         || (cp >= 0x1F0E0 && cp <= 0x1F0FF)
         || (cp >= 0x1F10B && cp <= 0x1F10F)
         || cp == 0x1F12F
         || (cp >= 0x1F16C && cp <= 0x1F16F)
         || (cp >= 0x1F19B && cp <= 0x1F1E5)
         || (cp >= 0x1F203 && cp <= 0x1F20F)
         || (cp >= 0x1F23B && cp <= 0x1F23F)
         || (cp >= 0x1F249 && cp <= 0x1F24F)
         || (cp >= 0x1F252 && cp <= 0x1F2FF)
         || (cp >= 0x1F321 && cp <= 0x1F32F)
         || cp == 0x1F336
         || (cp >= 0x1F37D && cp <= 0x1F37F)
         || (cp >= 0x1F394 && cp <= 0x1F39F)
         || cp == 0x1F3C5
         || (cp >= 0x1F3CB && cp <= 0x1F3DF)
         || (cp >= 0x1F3F1 && cp <= 0x1F3FF)
         || cp == 0x1F43F
         || cp == 0x1F441
         || cp == 0x1F4F8
         || (cp >= 0x1F4FD && cp <= 0x1F4FF)
         || (cp >= 0x1F53E && cp <= 0x1F53F)
         || (cp >= 0x1F544 && cp <= 0x1F54F)
         || (cp >= 0x1F568 && cp <= 0x1F5FA)
         || (cp >= 0x1F641 && cp <= 0x1F644)
         || (cp >= 0x1F650 && cp <= 0x1F67F)
         || (cp >= 0x1F6C6 && cp <= 0x1F6FF)
         || (cp >= 0x1F774 && cp <= 0x1FFFF)
         || (cp >= 0x2A6D7 && cp <= 0x2A6FF)
         || (cp >= 0x2B735 && cp <= 0x2B73F)
         || (cp >= 0x2B81E && cp <= 0x2F7FF)
         || (cp >= 0x2FA1E && cp <= 0xE0000)
         || (cp >= 0xE0002 && cp <= 0xE001F)
         || (cp >= 0xE0080 && cp <= 0xE00FF)
         || (cp >= 0xE01F0 && cp <= 0xEFFFF)
         || (cp >= 0xFFFFE && cp <= 0xFFFFF)
         || (cp >= 0x10FFFE && cp <= 0x10FFFF)) {
            return (byte) -1;
        } else if (cp == 0x05BE
         || cp == 0x05C0
         || cp == 0x05C3
         || cp == 0x05C6
         || (cp >= 0x05D0 && cp <= 0x05EA)
         || (cp >= 0x05F0 && cp <= 0x05F4)
         || (cp >= 0x07C0 && cp <= 0x07EA)
         || (cp >= 0x07F4 && cp <= 0x07F5)
         || cp == 0x07FA
         || (cp >= 0x0800 && cp <= 0x0815)
         || cp == 0x081A
         || cp == 0x0824
         || cp == 0x0828
         || (cp >= 0x0830 && cp <= 0x083E)
         || (cp >= 0x0840 && cp <= 0x0858)
         || cp == 0x085E
         || cp == 0x200F
         || cp == 0xFB1D
         || (cp >= 0xFB1F && cp <= 0xFB28)
         || (cp >= 0xFB2A && cp <= 0xFB36)
         || (cp >= 0xFB38 && cp <= 0xFB3C)
         || cp == 0xFB3E
         || (cp >= 0xFB40 && cp <= 0xFB41)
         || (cp >= 0xFB43 && cp <= 0xFB44)
         || (cp >= 0xFB46 && cp <= 0xFB4F)
         || (cp >= 0x10800 && cp <= 0x10805)
         || cp == 0x10808
         || (cp >= 0x1080A && cp <= 0x10835)
         || (cp >= 0x10837 && cp <= 0x10838)
         || cp == 0x1083C
         || (cp >= 0x1083F && cp <= 0x10855)
         || (cp >= 0x10857 && cp <= 0x1085F)
         || (cp >= 0x10900 && cp <= 0x1091B)
         || (cp >= 0x10920 && cp <= 0x10939)
         || cp == 0x1093F
         || (cp >= 0x10980 && cp <= 0x109B7)
         || (cp >= 0x109BE && cp <= 0x109BF)
         || cp == 0x10A00
         || (cp >= 0x10A10 && cp <= 0x10A13)
         || (cp >= 0x10A15 && cp <= 0x10A17)
         || (cp >= 0x10A19 && cp <= 0x10A33)
         || (cp >= 0x10A40 && cp <= 0x10A47)
         || (cp >= 0x10A50 && cp <= 0x10A58)
         || (cp >= 0x10A60 && cp <= 0x10A7F)
         || (cp >= 0x10B00 && cp <= 0x10B35)
         || (cp >= 0x10B40 && cp <= 0x10B55)
         || (cp >= 0x10B58 && cp <= 0x10B72)
         || (cp >= 0x10B78 && cp <= 0x10B7F)
         || (cp >= 0x10C00 && cp <= 0x10C48)) {
            return (byte) 1;
        } else if ((cp >= 0x0600 && cp <= 0x0604)
         || (cp >= 0x0660 && cp <= 0x0669)
         || (cp >= 0x066B && cp <= 0x066C)
         || cp == 0x06DD
         || (cp >= 0x10E60 && cp <= 0x10E7E)) {
            return (byte) 6;
        } else if (cp == 0x0608
         || cp == 0x060B
         || cp == 0x060D
         || cp == 0x061B
         || (cp >= 0x061E && cp <= 0x064A)
         || (cp >= 0x066D && cp <= 0x066F)
         || (cp >= 0x0671 && cp <= 0x06D5)
         || (cp >= 0x06E5 && cp <= 0x06E6)
         || (cp >= 0x06EE && cp <= 0x06EF)
         || (cp >= 0x06FA && cp <= 0x070D)
         || (cp >= 0x070F && cp <= 0x0710)
         || (cp >= 0x0712 && cp <= 0x072F)
         || (cp >= 0x074D && cp <= 0x07A5)
         || cp == 0x07B1
         || cp == 0x08A0
         || (cp >= 0x08A2 && cp <= 0x08AC)
         || (cp >= 0xFB50 && cp <= 0xFBC1)
         || (cp >= 0xFBD3 && cp <= 0xFD3D)
         || (cp >= 0xFD50 && cp <= 0xFD8F)
         || (cp >= 0xFD92 && cp <= 0xFDC7)
         || (cp >= 0xFDF0 && cp <= 0xFDFC)
         || (cp >= 0xFE70 && cp <= 0xFE74)
         || (cp >= 0xFE76 && cp <= 0xFEFC)
         || (cp >= 0x1EE00 && cp <= 0x1EE03)
         || (cp >= 0x1EE05 && cp <= 0x1EE1F)
         || (cp >= 0x1EE21 && cp <= 0x1EE22)
         || cp == 0x1EE24
         || cp == 0x1EE27
         || (cp >= 0x1EE29 && cp <= 0x1EE32)
         || (cp >= 0x1EE34 && cp <= 0x1EE37)
         || cp == 0x1EE39
         || cp == 0x1EE3B
         || cp == 0x1EE42
         || cp == 0x1EE47
         || cp == 0x1EE49
         || cp == 0x1EE4B
         || (cp >= 0x1EE4D && cp <= 0x1EE4F)
         || (cp >= 0x1EE51 && cp <= 0x1EE52)
         || cp == 0x1EE54
         || cp == 0x1EE57
         || cp == 0x1EE59
         || cp == 0x1EE5B
         || cp == 0x1EE5D
         || cp == 0x1EE5F
         || (cp >= 0x1EE61 && cp <= 0x1EE62)
         || cp == 0x1EE64
         || (cp >= 0x1EE67 && cp <= 0x1EE6A)
         || (cp >= 0x1EE6C && cp <= 0x1EE72)
         || (cp >= 0x1EE74 && cp <= 0x1EE77)
         || (cp >= 0x1EE79 && cp <= 0x1EE7C)
         || cp == 0x1EE7E
         || (cp >= 0x1EE80 && cp <= 0x1EE89)
         || (cp >= 0x1EE8B && cp <= 0x1EE9B)
         || (cp >= 0x1EEA1 && cp <= 0x1EEA3)
         || (cp >= 0x1EEA5 && cp <= 0x1EEA9)
         || (cp >= 0x1EEAB && cp <= 0x1EEBB)) {
            return (byte) 2;
        } else if (cp == 0x202A) {
            return (byte) 14;
        } else if (cp == 0x202B) {
            return (byte) 16;
        } else if (cp == 0x202C) {
            return (byte) 18;
        } else if (cp == 0x202D) {
            return (byte) 15;
        } else if (cp == 0x202E) {
            return (byte) 17;
        }
        return Character.DIRECTIONALITY_UNDEFINED;
    }
    //end - getDirectionality

    //start - isMirrored
    public static boolean isMirrored(int cp) {
        if ((cp >= 0x0028 && cp <= 0x0029)
         || cp == 0x003C
         || cp == 0x003E
         || cp == 0x005B
         || cp == 0x005D
         || cp == 0x007B
         || cp == 0x007D
         || cp == 0x00AB
         || cp == 0x00BB
         || (cp >= 0x0F3A && cp <= 0x0F3D)
         || (cp >= 0x169B && cp <= 0x169C)
         || (cp >= 0x2039 && cp <= 0x203A)
         || (cp >= 0x2045 && cp <= 0x2046)
         || (cp >= 0x207D && cp <= 0x207E)
         || (cp >= 0x208D && cp <= 0x208E)
         || cp == 0x2140
         || (cp >= 0x2201 && cp <= 0x2204)
         || (cp >= 0x2208 && cp <= 0x220D)
         || cp == 0x2211
         || (cp >= 0x2215 && cp <= 0x2216)
         || (cp >= 0x221A && cp <= 0x221D)
         || (cp >= 0x221F && cp <= 0x2222)
         || cp == 0x2224
         || cp == 0x2226
         || (cp >= 0x222B && cp <= 0x2233)
         || cp == 0x2239
         || (cp >= 0x223B && cp <= 0x224C)
         || (cp >= 0x2252 && cp <= 0x2255)
         || (cp >= 0x225F && cp <= 0x2260)
         || cp == 0x2262
         || (cp >= 0x2264 && cp <= 0x226B)
         || (cp >= 0x226E && cp <= 0x228C)
         || (cp >= 0x228F && cp <= 0x2292)
         || cp == 0x2298
         || (cp >= 0x22A2 && cp <= 0x22A3)
         || (cp >= 0x22A6 && cp <= 0x22B8)
         || (cp >= 0x22BE && cp <= 0x22BF)
         || (cp >= 0x22C9 && cp <= 0x22CD)
         || (cp >= 0x22D0 && cp <= 0x22D1)
         || (cp >= 0x22D6 && cp <= 0x22ED)
         || (cp >= 0x22F0 && cp <= 0x22FF)
         || (cp >= 0x2308 && cp <= 0x230B)
         || (cp >= 0x2320 && cp <= 0x2321)
         || (cp >= 0x2329 && cp <= 0x232A)
         || (cp >= 0x2768 && cp <= 0x2775)
         || cp == 0x27C0
         || (cp >= 0x27C3 && cp <= 0x27C6)
         || (cp >= 0x27C8 && cp <= 0x27C9)
         || (cp >= 0x27CB && cp <= 0x27CD)
         || (cp >= 0x27D3 && cp <= 0x27D6)
         || (cp >= 0x27DC && cp <= 0x27DE)
         || (cp >= 0x27E2 && cp <= 0x27EF)
         || (cp >= 0x2983 && cp <= 0x2998)
         || (cp >= 0x299B && cp <= 0x29AF)
         || cp == 0x29B8
         || (cp >= 0x29C0 && cp <= 0x29C5)
         || cp == 0x29C9
         || (cp >= 0x29CE && cp <= 0x29D2)
         || (cp >= 0x29D4 && cp <= 0x29D5)
         || (cp >= 0x29D8 && cp <= 0x29DC)
         || cp == 0x29E1
         || (cp >= 0x29E3 && cp <= 0x29E5)
         || (cp >= 0x29E8 && cp <= 0x29E9)
         || (cp >= 0x29F4 && cp <= 0x29F9)
         || (cp >= 0x29FC && cp <= 0x29FD)
         || (cp >= 0x2A0A && cp <= 0x2A1C)
         || (cp >= 0x2A1E && cp <= 0x2A21)
         || cp == 0x2A24
         || cp == 0x2A26
         || cp == 0x2A29
         || (cp >= 0x2A2B && cp <= 0x2A2E)
         || (cp >= 0x2A34 && cp <= 0x2A35)
         || (cp >= 0x2A3C && cp <= 0x2A3E)
         || (cp >= 0x2A57 && cp <= 0x2A58)
         || (cp >= 0x2A64 && cp <= 0x2A65)
         || (cp >= 0x2A6A && cp <= 0x2A6D)
         || (cp >= 0x2A6F && cp <= 0x2A70)
         || (cp >= 0x2A73 && cp <= 0x2A74)
         || (cp >= 0x2A79 && cp <= 0x2AA3)
         || (cp >= 0x2AA6 && cp <= 0x2AAD)
         || (cp >= 0x2AAF && cp <= 0x2AD6)
         || cp == 0x2ADC
         || cp == 0x2ADE
         || (cp >= 0x2AE2 && cp <= 0x2AE6)
         || (cp >= 0x2AEC && cp <= 0x2AEE)
         || cp == 0x2AF3
         || (cp >= 0x2AF7 && cp <= 0x2AFB)
         || cp == 0x2AFD
         || (cp >= 0x2E02 && cp <= 0x2E05)
         || (cp >= 0x2E09 && cp <= 0x2E0A)
         || (cp >= 0x2E0C && cp <= 0x2E0D)
         || (cp >= 0x2E1C && cp <= 0x2E1D)
         || (cp >= 0x2E20 && cp <= 0x2E29)
         || (cp >= 0x3008 && cp <= 0x3011)
         || (cp >= 0x3014 && cp <= 0x301B)
         || (cp >= 0xFE59 && cp <= 0xFE5E)
         || (cp >= 0xFE64 && cp <= 0xFE65)
         || (cp >= 0xFF08 && cp <= 0xFF09)
         || cp == 0xFF1C
         || cp == 0xFF1E
         || cp == 0xFF3B
         || cp == 0xFF3D
         || cp == 0xFF5B
         || cp == 0xFF5D
         || (cp >= 0xFF5F && cp <= 0xFF60)
         || (cp >= 0xFF62 && cp <= 0xFF63)
         || cp == 0x1D6DB
         || cp == 0x1D715
         || cp == 0x1D74F
         || cp == 0x1D789
         || cp == 0x1D7C3) {
            return true;
        }
        return false;
    }
    //end - isMirrored
    
    public static boolean isMirrored(char ch) {
        return isMirrored((int) ch);
    }

    @Override
    public int compareTo(TCharacter anotherCharacter) {
        return compare(value, anotherCharacter.value);
    }

    public static int compare(char x, char y) {
        return x - y;
    }

    public static char reverseBytes(char ch) {
        return (char) (((ch & 0xFF00) >> 8) | (ch << 8));
    }
}
