package org.teavm.classlib.impl.charset;

/**
 *
 * @author Alexey Andreev
 */
public abstract class Charset {
    public abstract void encode(CharBuffer source, ByteBuffer dest);

    public abstract void decode(ByteBuffer source, CharBuffer dest);

    public static Charset get(String name) {
        if (name.equals("UTF-8")) {
            return new UTF8Charset();
        }
        return null;
    }
}
