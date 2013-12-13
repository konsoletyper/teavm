package org.teavm.classlib.java.lang;

import org.teavm.classlib.java.lang.io.TSerializable;
import org.teavm.javascript.ni.GeneratedBy;
import org.teavm.javascript.ni.Rename;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TString extends TObject implements TSerializable, TComparable<TString>,
        TCharSequence {
    static int SURROGATE_BIT_MASK = 0xFC00;
    static int SURROGATE_BIT_INV_MASK = 0x03FF;
    static int HIGH_SURROGATE_BITS = 0xF800;
    static int LOW_SURROGATE_BITS = 0xF800;
    static int MEANINGFUL_SURROGATE_BITS = 10;
    static int SUPPLEMENTARY_PLANE = 0x10000;
    private char[] characters;
    private transient int hashCode;

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

    public TString(TStringBuilder sb) {
        this(sb.buffer, 0, sb.length);
    }

    @Override
    public char charAt(int index) {
        if (index < 0 || index >= characters.length) {
            throw new TStringIndexOutOfBoundsException();
        }
        return characters[index];
    }

    public int codePointAt(int index) {
        if (index == characters.length - 1 || (characters[index] & SURROGATE_BIT_MASK) != HIGH_SURROGATE_BITS ||
                (characters[index + 1] & SURROGATE_BIT_MASK) != LOW_SURROGATE_BITS) {
            return characters[index];
        }
        return ((characters[index] & SURROGATE_BIT_INV_MASK) << MEANINGFUL_SURROGATE_BITS) |
                (characters[index + 1] & SURROGATE_BIT_INV_MASK) + SUPPLEMENTARY_PLANE;
    }

    public int codePointBefore(int index) {
        if (index == 1 || (characters[index] & SURROGATE_BIT_MASK) != LOW_SURROGATE_BITS ||
                (characters[index - 1] & SURROGATE_BIT_MASK) != HIGH_SURROGATE_BITS) {
            return characters[index - 1];
        }
        return ((characters[index - 1] & SURROGATE_BIT_INV_MASK) << MEANINGFUL_SURROGATE_BITS) |
                (characters[index] & SURROGATE_BIT_INV_MASK) + SUPPLEMENTARY_PLANE;
    }

    public int codePointCount(int beginIndex, int endIndex) {
        int count = endIndex;
        --endIndex;
        for (int i = beginIndex; i < endIndex; ++i) {
            if ((characters[i] & SURROGATE_BIT_MASK) == HIGH_SURROGATE_BITS &&
                    (characters[i + 1] & SURROGATE_BIT_MASK) == LOW_SURROGATE_BITS) {
                --count;
            }
        }
        return count;
    }

    public int offsetByCodePoints(int index, int codePointOffset) {
        for (int i = 0; i < codePointOffset; ++i) {
            if (index < characters.length - 1 && (characters[index] & SURROGATE_BIT_MASK) == HIGH_SURROGATE_BITS &&
                    (characters[index + 1] & SURROGATE_BIT_MASK) == LOW_SURROGATE_BITS) {
                index += 2;
            } else {
                index++;
            }
        }
        return index;
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

    static char highSurrogate(int codePoint) {
        return (char)(TString.HIGH_SURROGATE_BITS | (codePoint >> TString.MEANINGFUL_SURROGATE_BITS) &
                TString.SURROGATE_BIT_INV_MASK);
    }

    static char lowSurrogate(int codePoint) {
        return (char)(TString.HIGH_SURROGATE_BITS | codePoint & TString.SURROGATE_BIT_INV_MASK);
    }

    public int indexOf(int ch, int fromIndex) {
        if (ch < SUPPLEMENTARY_PLANE) {
            char bmpChar = (char)ch;
            for (int i = fromIndex; i < characters.length; ++i) {
                if (characters[i] == bmpChar) {
                    return i;
                }
            }
            return -1;
        } else {
            char hi = highSurrogate(ch);
            char lo = lowSurrogate(ch);
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
        if (ch < SUPPLEMENTARY_PLANE) {
            char bmpChar = (char)ch;
            for (int i = fromIndex; i >= 0; --i) {
                if (characters[i] == bmpChar) {
                    return i;
                }
            }
            return -1;
        } else {
            char hi = highSurrogate(ch);
            char lo = lowSurrogate(ch);
            for (int i = fromIndex; i >= 1; --i) {
                if (characters[i] == lo && characters[i - 1] == hi) {
                    return i;
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
        return obj != null ? obj.toString0() : TString.wrap("null");
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
        return new TStringBuilder().append(i).toString0();
    }

    public static TString valueOf(long l) {
        return new TStringBuilder().append(l).toString0();
    }

    public static TString valueOf(float f) {
        return new TStringBuilder().append(f).toString0();
    }

    @Override
    public boolean equals(TObject other) {
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

    @GeneratedBy(StringNativeGenerator.class)
    public static native TString wrap(String str);
}
