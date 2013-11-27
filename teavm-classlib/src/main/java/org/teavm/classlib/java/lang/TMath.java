package org.teavm.classlib.java.lang;

/**
 *
 * @author Alexey Andreev
 */
public final class TMath extends TObject {
    private TMath() {
    }

    public static int min(int a, int b) {
        return a < b ? a : b;
    }
}
