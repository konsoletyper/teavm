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

import org.teavm.classlib.impl.charset.*;
import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.io.TUnsupportedEncodingException;
import org.teavm.classlib.java.util.TArrays;
import org.teavm.classlib.java.util.TComparator;
import org.teavm.classlib.java.util.THashMap;
import org.teavm.classlib.java.util.TMap;
import org.teavm.dependency.PluggableDependency;
import org.teavm.javascript.ni.InjectedBy;
import org.teavm.javascript.ni.Rename;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TString extends TObject implements TSerializable, TComparable<TString>, TCharSequence {
    public static final TComparator<TString> CASE_INSENSITIVE_ORDER = new TComparator<TString>() {
        @Override public int compare(TString o1, TString o2) {
            return o1.compareToIgnoreCase(o2);
        }
    };
    private char[] characters;
    private transient int hashCode;
    private static TMap<TString, TString> pool = new THashMap<>();

    public TString() {
        this.characters = new char[0];
    }

    public TString(TString other) {
        characters = other.characters;
    }

    public TString(char[] characters) {
        this.characters = new char[characters.length];
        for (int i = 0; i < characters.length; ++i) {
            this.characters[i] = characters[i];
        }
    }

    public TString(char[] value, int offset, int count) {
        this.characters = new char[count];
        for (int i = 0; i < count; ++i) {
            this.characters[i] = value[i + offset];
        }
    }

    public TString(byte[] bytes, int offset, int length, TString charsetName) throws TUnsupportedEncodingException {
        Charset charset = Charset.get(charsetName.toString());
        if (charset == null) {
            throw new TUnsupportedEncodingException(TString.wrap("Unknown encoding:" + charsetName));
        }
        initWithBytes(bytes, offset, length, charset);
    }

    public TString(byte[] bytes, int offset, int length) {
        initWithBytes(bytes, offset, length, new UTF8Charset());
    }

    public TString(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    public TString(byte[] bytes, TString charsetName) throws TUnsupportedEncodingException {
        this(bytes, 0, bytes.length, charsetName);
    }

    public TString(int[] codePoints, int offset, int count) {
        characters = new char[count * 2];
        int charCount = 0;
        for (int i = 0; i < count; ++i) {
            int codePoint = codePoints[offset++];
            if (codePoint >= UTF16Helper.SUPPLEMENTARY_PLANE) {
                characters[charCount++] = UTF16Helper.highSurrogate(codePoint);
                characters[charCount++] = UTF16Helper.lowSurrogate(codePoint);
            } else {
                characters[charCount++] = (char)codePoint;
            }
        }
        if (charCount < characters.length) {
            characters = TArrays.copyOf(characters, charCount);
        }
    }

    private void initWithBytes(byte[] bytes, int offset, int length, Charset charset) {
        TStringBuilder sb = new TStringBuilder(bytes.length * 2);
        this.characters = new char[sb.length()];
        ByteBuffer source = new ByteBuffer(bytes, offset, offset + length);
        char[] destChars = new char[TMath.max(8, TMath.min(length * 2, 1024))];
        CharBuffer dest = new CharBuffer(destChars, 0, destChars.length);
        while (!source.end()) {
            charset.decode(source, dest);
            sb.append(destChars, 0, dest.position());
            dest.rewind(0);
        }
        characters = new char[sb.length()];
        sb.getChars(0, sb.length(), characters, 0);
    }

    public TString(TStringBuilder sb) {
        this(sb.buffer, 0, sb.length());
    }

    @Override
    public char charAt(int index) {
        if (index < 0 || index >= characters.length) {
            throw new TStringIndexOutOfBoundsException();
        }
        return characters[index];
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
        return characters.length;
    }

    public boolean isEmpty() {
        return characters.length == 0;
    }

    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        if (srcBegin < 0 || srcBegin > srcEnd || srcEnd > length() || dstBegin < 0 ||
                dstBegin + (srcEnd - srcBegin) > dst.length) {
            throw new TIndexOutOfBoundsException();
        }
        while (srcBegin < srcEnd) {
            dst[dstBegin++] = charAt(srcBegin++);
        }
    }

    public boolean contentEquals(TCharSequence charSeq) {
        if (this == charSeq) {
            return true;
        }
        if (characters.length != charSeq.length()) {
            return false;
        }
        for (int i = 0; i < characters.length; ++i) {
            if (characters[i] != charSeq.charAt(i)) {
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
        if (ch < UTF16Helper.SUPPLEMENTARY_PLANE) {
            char bmpChar = (char)ch;
            for (int i = fromIndex; i < characters.length; ++i) {
                if (characters[i] == bmpChar) {
                    return i;
                }
            }
            return -1;
        } else {
            char hi = UTF16Helper.highSurrogate(ch);
            char lo = UTF16Helper.lowSurrogate(ch);
            for (int i = fromIndex; i < characters.length - 1; ++i) {
                if (characters[i] == hi && characters[i + 1] == lo) {
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
        if (ch < UTF16Helper.SUPPLEMENTARY_PLANE) {
            char bmpChar = (char)ch;
            for (int i = fromIndex; i >= 0; --i) {
                if (characters[i] == bmpChar) {
                    return i;
                }
            }
            return -1;
        } else {
            char hi = UTF16Helper.highSurrogate(ch);
            char lo = UTF16Helper.lowSurrogate(ch);
            for (int i = fromIndex; i >= 1; --i) {
                if (characters[i] == lo && characters[i - 1] == hi) {
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
        int toIndex = length() - str.length();
        outer:
        for (int i = fromIndex; i < toIndex; ++i) {
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
        if (beginIndex > endIndex) {
            throw new TIndexOutOfBoundsException();
        }
        return new TString(characters, beginIndex, endIndex - beginIndex);
    }

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
        char[] buffer = new char[length() + str.length()];
        int index = 0;
        for (int i = 0; i < length(); ++i) {
            buffer[index++] = charAt(i);
        }
        for (int i = 0; i < str.length(); ++i) {
            buffer[index++] = str.charAt(i);
        }
        return new TString(buffer);
    }

    public TString replace(char oldChar, char newChar) {
        if (oldChar == newChar) {
            return this;
        }
        char[] buffer = new char[length()];
        for (int i = 0; i < length(); ++i) {
            buffer[i] = charAt(i) == oldChar ? newChar : charAt(i);
        }
        return new TString(buffer);
    }

    public boolean contains(TCharSequence s) {
        outer:
        for (int i = 0; i < length(); ++i) {
            for (int j = 0; j < s.length(); ++j) {
                if (charAt(i + j) != s.charAt(j)) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    public TString replace(TCharSequence target, TCharSequence replacement) {
        TStringBuilder sb = new TStringBuilder();
        int sz = length() - target.length();
        int i = 0;
        outer:
        for (; i < sz; ++i) {
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
        return TString.wrap(sb.toString());
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

    @Override
    @Rename("toString")
    public TString toString0() {
        return this;
    }

    public char[] toCharArray() {
        char[] array = new char[characters.length];
        for (int i = 0; i < array.length; ++i) {
            array[i] = characters[i];
        }
        return array;
    }

    public static TString valueOf(TObject obj) {
        return obj != null ? TString.wrap(obj.toString()) : TString.wrap("null");
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

    public static TString valueOf(boolean b) {
        return b ? TString.wrap("true") : TString.wrap("false");
    }

    public static TString valueOf(char c) {
        return new TString(new char[] { c });
    }

    public static TString valueOf(int i) {
        return TString.wrap(new TStringBuilder().append(i).toString());
    }

    public static TString valueOf(long l) {
        return TString.wrap(new TStringBuilder().append(l).toString());
    }

    public static TString valueOf(float f) {
        return TString.wrap(new TStringBuilder().append(f).toString());
    }

    public static TString valueOf(double d) {
        return TString.wrap(new TStringBuilder().append(d).toString());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TString)) {
            return false;
        }
        TString str = (TString)other;
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
        Charset charset = Charset.get(charsetName.toString());
        if (charset == null) {
            throw new TUnsupportedEncodingException(TString.wrap("Unsupported encoding: " + charsetName));
        }
        return getBytes(charset);
    }

    public byte[] getBytes() {
        return getBytes(new UTF8Charset());
    }

    private byte[] getBytes(Charset charset) {
        byte[] result = new byte[length() * 2];
        int resultLength = 0;
        byte[] destArray = new byte[TMath.max(16, TMath.min(length() * 2, 4096))];
        ByteBuffer dest = new ByteBuffer(destArray);
        CharBuffer src = new CharBuffer(characters);
        while (!src.end()) {
            charset.encode(src, dest);
            if (resultLength + dest.position() > result.length) {
                result = TArrays.copyOf(result, result.length * 2);
            }
            for (int i = 0; i < dest.position(); ++i) {
                result[resultLength++] = destArray[i];
            }
            dest.rewind(0);
        }
        return TArrays.copyOf(result, resultLength);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode ^= 734262231;
            for (char c : characters) {
                hashCode = (hashCode << 4) | (hashCode >>> 28);
                hashCode ^= 347236277 ^ c;
                if (hashCode == 0) {
                    ++hashCode;
                }
            }
        }
        return hashCode;
    }

    @InjectedBy(StringNativeGenerator.class)
    @PluggableDependency(StringNativeGenerator.class)
    public static native TString wrap(String str);

    public TString toLowerCase() {
        if (isEmpty()) {
            return this;
        }
        int[] codePoints = new int[characters.length];
        int codePointCount = 0;
        for (int i = 0; i < characters.length; ++i) {
            if (i == characters.length - 1 || !UTF16Helper.isHighSurrogate(characters[i]) ||
                    !UTF16Helper.isLowSurrogate(characters[i + 1])) {
                codePoints[codePointCount++] = TCharacter.toLowerCase(characters[i]);
            } else {
                codePoints[codePointCount++] = TCharacter.toLowerCase(UTF16Helper.buildCodePoint(
                        characters[i], characters[i + 1]));
                ++i;
            }
        }
        return new TString(codePoints, 0, codePointCount);
    }

    public TString toUpperCase() {
        if (isEmpty()) {
            return this;
        }
        int[] codePoints = new int[characters.length];
        int codePointCount = 0;
        for (int i = 0; i < characters.length; ++i) {
            if (i == characters.length - 1 || !UTF16Helper.isHighSurrogate(characters[i]) ||
                    !UTF16Helper.isLowSurrogate(characters[i + 1])) {
                codePoints[codePointCount++] = TCharacter.toUpperCase(characters[i]);
            } else {
                codePoints[codePointCount++] = TCharacter.toUpperCase(UTF16Helper.buildCodePoint(
                        characters[i], characters[i + 1]));
                ++i;
            }
        }
        return new TString(codePoints, 0, codePointCount);
    }

    public TString intern() {
        TString interned = pool.get(this);
        if (interned == null) {
            interned = this;
            pool.put(interned, interned);
        }
        return interned;
    }
}
