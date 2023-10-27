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

    // TODO: public static byte getDirectionality(char ch)
    // TODO: public static boolean isMirrored(char ch)

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
