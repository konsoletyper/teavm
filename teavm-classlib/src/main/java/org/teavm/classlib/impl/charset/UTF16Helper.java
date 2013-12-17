package org.teavm.classlib.impl.charset;

/**
 *
 * @author Alexey Andreev
 */
public class UTF16Helper {
    public static final int SURROGATE_BIT_MASK = 0xFC00;
    public static final int SURROGATE_BIT_INV_MASK = 0x03FF;
    public static final int HIGH_SURROGATE_BITS = 0xF800;
    public static final int LOW_SURROGATE_BITS = 0xF800;
    public static final int MEANINGFUL_SURROGATE_BITS = 10;
    public static final int SUPPLEMENTARY_PLANE = 0x10000;

    public static char highSurrogate(int codePoint) {
        return (char)(HIGH_SURROGATE_BITS | (codePoint >> MEANINGFUL_SURROGATE_BITS) & SURROGATE_BIT_INV_MASK);
    }

    public static char lowSurrogate(int codePoint) {
        return (char)(HIGH_SURROGATE_BITS | codePoint & SURROGATE_BIT_INV_MASK);
    }

    public static boolean isHighSurrogate(char c) {
        return (c & SURROGATE_BIT_MASK) == HIGH_SURROGATE_BITS;
    }

    public static boolean isLowSurrogate(char c) {
        return (c & SURROGATE_BIT_MASK) == LOW_SURROGATE_BITS;
    }

    public static boolean isSurrogatePair(char a, char b) {
        return isHighSurrogate(a) && isLowSurrogate(b);
    }

    public static int buildCodePoint(char a, char b) {
        return ((a & SURROGATE_BIT_INV_MASK) << MEANINGFUL_SURROGATE_BITS) |
            (b & SURROGATE_BIT_INV_MASK) + SUPPLEMENTARY_PLANE;
    }
}
