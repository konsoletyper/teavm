package org.teavm.classlib.impl.charset;

/**
 *
 * @author Alexey Andreev
 */
public abstract class Charset {
    public abstract int encode(int[] buffer, int offset, int length, byte[] dest, int destOffset, int destLength);

    public abstract int decode(byte[] buffer, int offset, int length, int[] dest, int destOffset, int destLength);

    public static native Charset get(String name);
}
