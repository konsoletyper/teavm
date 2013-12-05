package org.teavm.classlib.java.lang;

/**
 *
 * @author Alexey Andreev
 */
public class TStringBuilder extends TAbstractStringBuilder {
    @Override
    public TStringBuilder append(TString string) {
        super.append(string);
        return this;
    }

    @Override
    public TStringBuilder append(int value) {
        super.append(value);
        return this;
    }

    @Override
    public TStringBuilder append(char c) {
        super.append(c);
        return this;
    }

    @Override
    public TStringBuilder appendCodePoint(int codePoint) {
        super.appendCodePoint(codePoint);
        return this;
    }

    @Override
    public TStringBuilder append(TCharSequence s, int start, int end) {
        super.append(s, start, end);
        return this;
    }

    @Override
    public TStringBuilder append(TCharSequence s) {
        super.append(s);
        return this;
    }
}
