package org.teavm.classlib.java.lang;

/**
 *
 * @author Alexey Andreev
 */
public interface TCharSequence {
    int length();

    char charAt(int index);

    TCharSequence subSequence(int start, int end);

    TString toString0();
}
