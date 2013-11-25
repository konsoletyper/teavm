package org.teavm.classlib.java.lang;

/**
 *
 * @author Alexey Andreev
 */
public class TStringBuilder extends AbstractStringBuilder {
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
}
