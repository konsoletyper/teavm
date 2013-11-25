package org.teavm.classlib.java.lang;

import org.teavm.classlib.java.lang.io.TSerializable;
import org.teavm.javascript.ni.GeneratedBy;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TString extends TObject implements TSerializable {
    private char[] characters;

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

    @GeneratedBy(StringNativeGenerator.class)
    public static native TString wrap(String str);
}
