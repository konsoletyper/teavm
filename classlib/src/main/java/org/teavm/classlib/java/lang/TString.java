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
package org.teavm.classlib.java.lang;

import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Locale;
import java.util.Objects;
import org.teavm.classlib.PlatformDetector;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.io.TUnsupportedEncodingException;
import org.teavm.classlib.java.nio.TByteBuffer;
import org.teavm.classlib.java.nio.TCharBuffer;
import org.teavm.classlib.java.nio.charset.TCharset;
import org.teavm.classlib.java.nio.charset.impl.TUTF8Charset;
import org.teavm.classlib.java.util.TArrays;
import org.teavm.classlib.java.util.TComparator;
import org.teavm.classlib.java.util.TFormatter;
import org.teavm.classlib.java.util.TLocale;
import org.teavm.classlib.java.util.regex.TPattern;
import org.teavm.dependency.PluggableDependency;
import org.teavm.interop.NoSideEffects;

public class TString extends TObject implements TSerializable, TComparable<TString>, TCharSequence {
    private static final char[] EMPTY_CHARS = new char[0];
    private static final TString EMPTY = new TString();
    public static final TComparator<TString> CASE_INSENSITIVE_ORDER = (o1, o2) -> o1.compareToIgnoreCase(o2);
    private transient int hashCode;

    public TString() {
        initWithEmptyChars();
    }

    @NoSideEffects
    private native void initWithEmptyChars();

    public TString(TString other) {
        borrowChars(other);
    }

    @NoSideEffects
    private native void borrowChars(TString other);

    public TString(char[] characters) {
        initWithCharArray(characters, 0, characters.length);
    }

    public TString(Object nativeString) {
    }

    private native Object nativeString();

    public TString(char[] value, int offset, int count) {
        Objects.checkFromIndexSize(offset, count, value.length);
        initWithCharArray(value, offset, count);
    }

    @NoSideEffects
    private native void initWithCharArray(char[] value, int offset, int count);

    static TString fromArray(char[] characters) {
        var s = new TString();
        s.takeCharArray(characters);
        return s;
    }

    @NoSideEffects
    private native void takeCharArray(char[] characters);

    public TString(byte[] bytes, int offset, int length, TString charsetName) throws TUnsupportedEncodingException {
        initWithBytes(bytes, offset, length, TCharset.forName(charsetName.toString()));
    }

    public TString(byte[] bytes, int offset, int length, TCharset charset) {
        initWithBytes(bytes, offset, length, charset);
    }

    public TString(byte[] bytes, int offset, int length) {
        initWithBytes(bytes, offset, length, TUTF8Charset.INSTANCE);
    }

    public TString(byte[] bytes) {
        initWithBytes(bytes, 0, bytes.length, TUTF8Charset.INSTANCE);
    }

    public TString(byte[] bytes, TString charsetName) throws TUnsupportedEncodingException {
        initWithBytes(bytes, 0, bytes.length, TCharset.forName(charsetName.toString()));
    }

    public TString(byte[] bytes, TCharset charset) {
        initWithBytes(bytes, 0, bytes.length, charset);
    }

    public TString(int[] codePoints, int offset, int count) {
        var characters = new char[count * 2];
        int charCount = 0;
        for (int i = 0; i < count; ++i) {
            int codePoint = codePoints[offset++];
            if (codePoint >= TCharacter.MIN_SUPPLEMENTARY_CODE_POINT) {
                characters[charCount++] = TCharacter.highSurrogate(codePoint);
                characters[charCount++] = TCharacter.lowSurrogate(codePoint);
            } else {
                characters[charCount++] = (char) codePoint;
            }
        }
        if (charCount < characters.length) {
            characters = TArrays.copyOf(characters, charCount);
        }
        takeCharArray(characters);
    }

    private void initWithBytes(byte[] bytes, int offset, int length, TCharset charset) {
        TCharBuffer buffer = charset.decode(TByteBuffer.wrap(bytes, offset, length));
        char[] characters;
        if (buffer.hasArray() && buffer.position() == 0 && buffer.limit() == buffer.capacity()) {
            characters = buffer.array();
        } else {
            characters = new char[buffer.remaining()];
            buffer.get(characters);
        }
        takeCharArray(characters);
    }

    public TString(TStringBuffer sb) {
        initWithCharArray(sb.buffer, 0, sb.length());
    }

    public TString(TStringBuilder sb) {
        initWithCharArray(sb.buffer, 0, sb.length());
    }

    private TString(int length) {
        takeCharArray(new char[length]);
    }

    private static TString allocate(int size) {
        return new TString(size);
    }

    @Override
    public char charAt(int index) {
        if (index < 0 || index >= charactersLength()) {
            throw new TStringIndexOutOfBoundsException();
        }
        return charactersGet(index);
    }

    public int codePointAt(int index) {
        return TCharacter.codePointAt(this, index);
    }

    public int codePointBefore(int index) {
        return TCharacter.codePointBefore(this, index);
    }

    public int codePointCount(int beginIndex, int endIndex) {
        return TCharacter.codePointCount(this, beginIndex, endIndex);
    }

    public int offsetByCodePoints(int index, int codePointOffset) {
        return TCharacter.offsetByCodePoints(this, index, codePointOffset);
    }

    @Override
    public int length() {
        return charactersLength();
    }

    @NoSideEffects
    private native int charactersLength();

    @NoSideEffects
    private native char charactersGet(int index);

    @Override
    public boolean isEmpty() {
        return charactersLength() == 0;
    }
    
    public boolean isBlank() {
        for (int i = 0; i < charactersLength(); i++) {
            if (charactersGet(i) != ' ') {
                return false;
            }
        }
        return true;
    }

    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        if (srcBegin < 0 || srcBegin > srcEnd || srcEnd > length() || dstBegin < 0
                || dstBegin + (srcEnd - srcBegin) > dst.length) {
            throw new TIndexOutOfBoundsException();
        }
        copyCharsToArray(srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }

    @NoSideEffects
    private native void copyCharsToArray(int begin, char[] dst, int dstBegin, int length);

    public boolean contentEquals(TStringBuffer buffer) {
        if (charactersLength() != buffer.length()) {
            return false;
        }
        for (int i = 0; i < charactersLength(); ++i) {
            if (charactersGet(i) != buffer.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean contentEquals(TCharSequence charSeq) {
        if (this == charSeq) {
            return true;
        }
        if (charactersLength() != charSeq.length()) {
            return false;
        }
        for (int i = 0; i < charactersLength(); ++i) {
            if (charactersGet(i) != charSeq.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int compareTo(TString anotherString) {
        if (this == anotherString) {
            return 0;
        }
        int l = TMath.min(length(), anotherString.length());
        for (int i = 0; i < l; ++i) {
            char a = charAt(i);
            char b = anotherString.charAt(i);
            if (a - b != 0) {
                return a - b;
            }
        }
        return length() - anotherString.length();
    }

    public int compareToIgnoreCase(TString anotherString) {
        if (this == anotherString) {
            return 0;
        }
        int l = TMath.min(length(), anotherString.length());
        for (int i = 0; i < l; ++i) {
            char a = TCharacter.toLowerCase(charAt(i));
            char b = TCharacter.toLowerCase(anotherString.charAt(i));
            if (a - b != 0) {
                return a - b;
            }
        }
        return length() - anotherString.length();
    }

    public boolean startsWith(TString prefix, int toffset) {
        if (toffset + prefix.length() > length()) {
            return false;
        }
        for (int i = 0; i < prefix.length(); ++i) {
            if (prefix.charAt(i) != charAt(toffset++)) {
                return false;
            }
        }
        return true;
    }

    public boolean startsWith(TString prefix) {
        if (this == prefix) {
            return true;
        }
        return startsWith(prefix, 0);
    }

    public boolean regionMatches(boolean ignoreCase, int toffset, String other, int ooffset, int len) {
        if (toffset < 0 || ooffset < 0 || toffset + len > length() || ooffset + len > other.length()) {
            return false;
        }
        for (int i = 0; i < len; ++i) {
            char a = charAt(toffset++);
            char b = other.charAt(ooffset++);
            if (ignoreCase) {
                a = TCharacter.toLowerCase(a);
                b = TCharacter.toLowerCase(b);
            }
            if (a != b) {
                return false;
            }
        }
        return true;
    }

    public boolean regionMatches(int toffset, TString other, int ooffset, int len) {
        if (toffset < 0 || ooffset < 0 || toffset + len > length() || ooffset + len > other.length()) {
            return false;
        }
        for (int i = 0; i < len; ++i) {
            if (charAt(toffset++) != other.charAt(ooffset++)) {
                return false;
            }
        }
        return true;
    }

    public boolean endsWith(TString suffix) {
        if (this == suffix) {
            return true;
        }
        if (suffix.length() > length()) {
            return false;
        }
        int j = 0;
        for (int i = length() - suffix.length(); i < length(); ++i) {
            if (charAt(i) != suffix.charAt(j++)) {
                return false;
            }
        }
        return true;
    }

    public int indexOf(int ch, int fromIndex) {
        fromIndex = Math.max(0, fromIndex);
        if (ch < TCharacter.MIN_SUPPLEMENTARY_CODE_POINT) {
            char bmpChar = (char) ch;
            for (int i = fromIndex; i < charactersLength(); ++i) {
                if (charactersGet(i) == bmpChar) {
                    return i;
                }
            }
            return -1;
        } else {
            char hi = TCharacter.highSurrogate(ch);
            char lo = TCharacter.lowSurrogate(ch);
            for (int i = fromIndex; i < charactersLength() - 1; ++i) {
                if (charactersGet(i) == hi && charactersGet(i + 1) == lo) {
                    return i;
                }
            }
            return -1;
        }
    }

    public int indexOf(int ch) {
        return indexOf(ch, 0);
    }

    public int lastIndexOf(int ch, int fromIndex) {
        fromIndex = Math.min(fromIndex, length() - 1);
        if (ch < TCharacter.MIN_SUPPLEMENTARY_CODE_POINT) {
            char bmpChar = (char) ch;
            for (int i = fromIndex; i >= 0; --i) {
                if (charactersGet(i) == bmpChar) {
                    return i;
                }
            }
            return -1;
        } else {
            char hi = TCharacter.highSurrogate(ch);
            char lo = TCharacter.lowSurrogate(ch);
            for (int i = fromIndex; i >= 1; --i) {
                if (charactersGet(i) == lo && charactersGet(i - 1) == hi) {
                    return i - 1;
                }
            }
            return -1;
        }
    }

    public int lastIndexOf(int ch) {
        return lastIndexOf(ch, length() - 1);
    }

    public int indexOf(TString str, int fromIndex) {
        fromIndex = Math.max(0, fromIndex);
        int toIndex = length() - str.length();
        outer:
        for (int i = fromIndex; i <= toIndex; ++i) {
            for (int j = 0; j < str.length(); ++j) {
                if (charAt(i + j) != str.charAt(j)) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public int indexOf(TString str) {
        return indexOf(str, 0);
    }

    public int lastIndexOf(TString str, int fromIndex) {
        fromIndex = Math.min(fromIndex, length() - str.length());
        outer:
        for (int i = fromIndex; i >= 0; --i) {
            for (int j = 0; j < str.length(); ++j) {
                if (charAt(i + j) != str.charAt(j)) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public int lastIndexOf(TString str) {
        return lastIndexOf(str, length());
    }

    public TString substring(int beginIndex, int endIndex) {
        int length = charactersLength();
        if (beginIndex == endIndex) {
            return EMPTY;
        }

        if (beginIndex == 0 && endIndex == length) {
            return this;
        }

        if (PlatformDetector.isJavaScript()) {
            if (beginIndex < 0 || beginIndex > endIndex || endIndex > length) {
                throw new TStringIndexOutOfBoundsException();
            }
            return new TString(substringJS(nativeString(), beginIndex, endIndex));
        }
        return new TString(fastCharArray(), beginIndex, endIndex - beginIndex);
    }

    @NoSideEffects
    private native static Object substringJS(Object nativeString, int start, int end);

    public TString substring(int beginIndex) {
        return substring(beginIndex, length());
    }

    @Override
    public TCharSequence subSequence(int beginIndex, int endIndex) {
        return substring(beginIndex, endIndex);
    }

    public TString concat(TString str) {
        if (str.isEmpty()) {
            return this;
        }
        if (isEmpty()) {
            return str;
        }

        var buffer = new char[length() + str.length()];
        int index = 0;
        for (int i = 0; i < length(); ++i) {
            buffer[index++] = charAt(i);
        }
        for (int i = 0; i < str.length(); ++i) {
            buffer[index++] = str.charAt(i);
        }
        return TString.fromArray(buffer);
    }

    public TString replace(char oldChar, char newChar) {
        if (oldChar == newChar) {
            return this;
        }
        var buffer = new char[length()];
        for (int i = 0; i < length(); ++i) {
            buffer[i] = charAt(i) == oldChar ? newChar : charAt(i);
        }
        return TString.fromArray(buffer);
    }

    public boolean contains(TCharSequence s) {
        int sz = length() - s.length();
        outer:
        for (int i = 0; i <= sz; ++i) {
            for (int j = 0; j < s.length(); ++j) {
                if (charAt(i + j) != s.charAt(j)) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    public String replace(TCharSequence target, TCharSequence replacement) {
        var sb = new StringBuilder();
        int sz = length() - target.length();
        int i = 0;
        outer:
        for (; i <= sz; ++i) {
            for (int j = 0; j < target.length(); ++j) {
                if (charAt(i + j) != target.charAt(j)) {
                    sb.append(charAt(i));
                    continue outer;
                }
            }
            sb.append(replacement);
            i += target.length() - 1;
        }
        sb.append(substring(i));
        return sb.toString();
    }

    public TString trim() {
        int lower = 0;
        int upper = length() - 1;
        while (lower <= upper && charAt(lower) <= ' ') {
            ++lower;
        }
        while (lower <= upper && charAt(upper) <= ' ') {
            --upper;
        }
        return substring(lower, upper + 1);
    }

    public TString strip() {
        if (PlatformDetector.isJavaScript()) {
            var result = stripJS(nativeString());
            return result != nativeString() ? new TString(result) : this;
        }
        var lower = 0;
        var upper = length() - 1;
        while (lower <= upper && Character.isWhitespace(charAt(lower))) {
            ++lower;
        }
        while (lower <= upper && Character.isWhitespace(charAt(upper))) {
            --upper;
        }
        return substring(lower, upper + 1);
    }

    private static native Object stripJS(Object nativeString);

    public TString stripLeading() {
        if (PlatformDetector.isJavaScript()) {
            var result = stripLeadingJS(nativeString());
            return result != nativeString() ? new TString(result) : this;
        }
        var lower = 0;
        while (lower < length() && Character.isWhitespace(charAt(lower))) {
            ++lower;
        }
        return substring(lower, length());
    }

    private static native Object stripLeadingJS(Object nativeString);

    public TString stripTrailing() {
        if (PlatformDetector.isJavaScript()) {
            var result = stripTrailingJS(nativeString());
            return result != nativeString() ? new TString(result) : this;
        }
        var upper = length() - 1;
        while (0 <= upper && Character.isWhitespace(charAt(upper))) {
            --upper;
        }
        return substring(0, upper + 1);
    }

    private static native Object stripTrailingJS(Object nativeString);

    @Override
    public String toString() {
        return (String) (Object) this;
    }

    public char[] toCharArray() {
        char[] array = new char[charactersLength()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = charAt(i);
        }
        return array;
    }

    public static String valueOf(Object obj) {
        return obj != null ? obj.toString() : "null";
    }

    public static TString valueOf(char[] data) {
        return new TString(data);
    }

    public static TString valueOf(char[] data, int offset, int count) {
        return new TString(data, offset, count);
    }

    public static TString copyValueOf(char[] data) {
        return valueOf(data);
    }

    public static TString copyValueOf(char[] data, int offset, int count) {
        return valueOf(data, offset, count);
    }

    public static String valueOf(boolean b) {
        return b ? "true" : "false";
    }

    public static String valueOf(char c) {
        return new String(new char[] { c });
    }

    public static String valueOf(int i) {
        return new TStringBuilder().append(i).toString();
    }

    public static String valueOf(long l) {
        return new TStringBuilder().append(l).toString();
    }

    public static String valueOf(float f) {
        return new TStringBuilder().append(f).toString();
    }

    public static String valueOf(double d) {
        return new TStringBuilder().append(d).toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TString)) {
            return false;
        }
        var str = (TString) other;
        if (PlatformDetector.isJavaScript()) {
            return nativeString() == str.nativeString();
        } else {
            if (str.length() != length()) {
                return false;
            }
            for (int i = 0; i < str.length(); ++i) {
                if (charAt(i) != str.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
    }

    public boolean equalsIgnoreCase(TString other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (length() != other.length()) {
            return false;
        }
        for (int i = 0; i < length(); ++i) {
            if (TCharacter.toLowerCase(charAt(i)) != TCharacter.toLowerCase(other.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public byte[] getBytes(TString charsetName) throws TUnsupportedEncodingException {
        try {
            return getBytes(TCharset.forName(charsetName.toString()));
        } catch (UnsupportedCharsetException | IllegalCharsetNameException x) {
            throw new TUnsupportedEncodingException();
        }
    }

    public byte[] getBytes() {
        return getBytes(TUTF8Charset.INSTANCE);
    }

    public byte[] getBytes(TCharset charset) {
        TByteBuffer buffer = charset.encode(TCharBuffer.wrap(fastCharArray()));
        if (buffer.hasArray() && buffer.position() == 0 && buffer.limit() == buffer.capacity()) {
            return buffer.array();
        } else {
            byte[] result = new byte[buffer.remaining()];
            buffer.get(result);
            return result;
        }
    }

    @NoSideEffects
    private native char[] fastCharArray();

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            for (var i = 0; i < charactersLength(); ++i) {
                hashCode = 31 * hashCode + charactersGet(i);
            }
        }
        return hashCode;
    }

    public TString toLowerCase() {
        if (PlatformDetector.isJavaScript()) {
            var lowerCase = toLowerCaseJS(nativeString());
            return lowerCase != nativeString() ? new TString(lowerCase) : this;
        }

        if (isEmpty()) {
            return this;
        }

        var hasCharsToTransform = false;
        var hasSurrogates = false;
        for (var i = 0; i < charactersLength(); ++i) {
            var c = charactersGet(i);
            if (Character.toLowerCase(c) != c) {
                hasCharsToTransform = true;
                break;
            }
            if (Character.isSurrogate(c)) {
                hasSurrogates = true;
            }
        }
        if (!hasCharsToTransform) {
            return this;
        }
        return hasSurrogates ? toLowerCaseCodePoints() : toLowerCaseChars();
    }

    @NoSideEffects
    private static native Object toLowerCaseJS(Object nativeString);

    @NoSideEffects
    private static native Object toLowerCaseJS(Object nativeString, Object languageTag);

    private TString toLowerCaseChars() {
        var chars = new char[charactersLength()];
        for (int i = 0; i < charactersLength(); ++i) {
            chars[i] = TCharacter.toLowerCase(charactersGet(i));
        }
        return new TString(chars);
    }

    private TString toLowerCaseCodePoints() {
        int[] codePoints = new int[charactersLength()];
        int codePointCount = 0;
        var length = charactersLength();
        for (int i = 0; i < charactersLength(); ++i) {
            if (i == length - 1 || !TCharacter.isHighSurrogate(charactersGet(i))
                    || !TCharacter.isLowSurrogate(charactersGet(i + 1))) {
                codePoints[codePointCount++] = TCharacter.toLowerCase(charactersGet(i));
            } else {
                codePoints[codePointCount++] = TCharacter.toLowerCase(TCharacter.toCodePoint(
                        charactersGet(i), charactersGet(i + 1)));
                ++i;
            }
        }
        return new TString(codePoints, 0, codePointCount);
    }

    public TString toLowerCase(TLocale locale) {
        if (PlatformDetector.isJavaScript()) {
            var upperCase = toLowerCaseJS(nativeString(), locale.toLanguageTag().nativeString());
            return upperCase != nativeString() ? new TString(upperCase) : this;
        }
        return toLowerCase();
    }

    public TString toUpperCase() {
        if (PlatformDetector.isJavaScript()) {
            var upperCase = toUpperCaseJS(nativeString());
            return upperCase != nativeString() ? new TString(upperCase) : this;
        }

        if (isEmpty()) {
            return this;
        }

        var hasCharsToTransform = false;
        var hasSurrogates = false;
        for (var i = 0; i < charactersLength(); ++i) {
            var c = charactersGet(i);
            if (Character.toUpperCase(c) != c) {
                hasCharsToTransform = true;
                break;
            }
            if (Character.isSurrogate(c)) {
                hasSurrogates = true;
            }
        }
        if (!hasCharsToTransform) {
            return this;
        }
        return hasSurrogates ? toUpperCaseCodePoints() : toUpperCaseChars();
    }

    @NoSideEffects
    private static native Object toUpperCaseJS(Object nativeString);

    @NoSideEffects
    private static native Object toUpperCaseJS(Object nativeString, Object languageTag);

    private TString toUpperCaseChars() {
        var chars = new char[charactersLength()];
        for (int i = 0; i < charactersLength(); ++i) {
            chars[i] = TCharacter.toUpperCase(charactersGet(i));
        }
        return new TString(chars);
    }

    private TString toUpperCaseCodePoints() {
        int[] codePoints = new int[charactersLength()];
        int codePointCount = 0;
        for (int i = 0; i < charactersLength(); ++i) {
            if (i == charactersLength() - 1 || !TCharacter.isHighSurrogate(charactersGet(i))
                    || !TCharacter.isLowSurrogate(charactersGet(i + 1))) {
                codePoints[codePointCount++] = TCharacter.toUpperCase(charactersGet(i));
            } else {
                codePoints[codePointCount++] = TCharacter.toUpperCase(TCharacter.toCodePoint(
                        charactersGet(i), charactersGet(i + 1)));
                ++i;
            }
        }
        return new TString(codePoints, 0, codePointCount);
    }

    public TString toUpperCase(TLocale locale) {
        if (PlatformDetector.isJavaScript()) {
            var upperCase = toUpperCaseJS(nativeString(), locale.toLanguageTag().nativeString());
            return upperCase != nativeString() ? new TString(upperCase) : this;
        }
        return toUpperCase();
    }

    @PluggableDependency(StringNativeDependency.class)
    @NoSideEffects
    public native TString intern();

    public boolean matches(String regex) {
        return TPattern.matches(regex, this.toString());
    }

    public String[] split(String regex) {
        return TPattern.compile(regex).split(this.toString());
    }

    public String[] split(String regex, int limit) {
        return TPattern.compile(regex).split(this.toString(), limit);
    }

    public String replaceAll(String regex, String replacement) {
        return TPattern.compile(regex).matcher(toString()).replaceAll(replacement);
    }

    public String replaceFirst(String regex, String replacement) {
        return TPattern.compile(regex).matcher(toString()).replaceFirst(replacement);
    }

    public static String format(String format, Object... args) {
        return new TFormatter().format(format, args).toString();
    }

    public static String format(Locale l, String format, Object... args) {
        return new TFormatter(l).format(format, args).toString();
    }

    public static TString join(CharSequence delimiter, CharSequence... elements) {
        if (elements.length == 0) {
            return EMPTY;
        }
        int resultLength = 0;
        for (var element : elements) {
            resultLength += element.length();
        }
        resultLength += (elements.length - 1) * delimiter.length();

        var chars = new char[resultLength];
        int index = 0;
        var firstElement = elements[0];
        for (int i = 0; i < firstElement.length(); ++i) {
            chars[index++] = firstElement.charAt(i);
        }
        for (int i = 1; i < elements.length; ++i) {
            for (int j = 0; j < delimiter.length(); ++j) {
                chars[index++] = delimiter.charAt(j);
            }
            var element = elements[i];
            for (int j = 0; j < element.length(); ++j) {
                chars[index++] = element.charAt(j);
            }
        }

        return TString.fromArray(chars);
    }

    public static String join(CharSequence delimiter, Iterable<? extends CharSequence> elements) {
        var iter = elements.iterator();
        if (!iter.hasNext()) {
            return "";
        }

        var sb = new StringBuilder();
        sb.append(iter.next());
        while (iter.hasNext()) {
            sb.append(delimiter);
            sb.append(iter.next());
        }
        return sb.toString();
    }

    public TString repeat(int count) {
        if (count < 0) {
            throw new IllegalArgumentException();
        }
        if (count == 1) {
            return this;
        }
        if (charactersLength() == 0 || count == 0) {
            return EMPTY;
        }
        var chars = new char[charactersLength() * count];
        var j = 0;
        for (int i = 0; i < count; i++) {
            getChars(0, length(), chars, j);
            j += length();
        }
        return TString.fromArray(chars);
    }
}
