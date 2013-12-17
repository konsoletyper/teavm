package org.teavm.classlib.impl.charset;

/**
 *
 * @author Alexey Andreev
 */
@CharsetName("UTF-8")
public class UTF8Charset extends Charset {
    @Override
    public int encode(int[] buffer, int offset, int length, byte[] dest, int destOffset, int destLength) {
        return 0;
    }

    @Override
    public int decode(byte[] buffer, int offset, int length, int[] dest, int destOffset, int destLength) {
        return 0;
    }
}
