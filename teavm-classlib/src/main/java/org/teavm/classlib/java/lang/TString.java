package org.teavm.classlib.java.lang;

import org.teavm.classlib.java.lang.io.TSerializable;
import org.teavm.javascript.ni.GeneratedBy;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TString extends TObject implements TSerializable {
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

    public char charAt(int index) {
        if (index < 0 || index >= characters.length) {
            throw new StringIndexOutOfBoundsException(null);
        }
        return characters[index];
    }

    public int length() {
        return characters.length;
    }

    public static TString valueOf(int index) {
        return new TStringBuilder().append(index).toString0();
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
