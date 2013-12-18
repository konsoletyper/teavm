package org.teavm.classlib.impl.charset;

/**
 *
 * @author Alexey Andreev
 */
public class UTF8Charset extends Charset {
    @Override
    public void encode(CharBuffer source, ByteBuffer dest) {
        while (!source.end() && dest.available() >= 4) {
            char ch = source.get();
            if (ch < 0x80) {
                dest.put((byte)ch);
            } else if (ch < 0x400) {
                dest.put((byte)(0xC0 | (ch >> 6)));
                dest.put((byte)(0x80 | (ch & 0x3F)));
            } else if (!UTF16Helper.isSurrogate(ch)) {
                dest.put((byte)(0xE0 | (ch >> 12)));
                dest.put((byte)(0x80 | ((ch >> 6) & 0x3F)));
                dest.put((byte)(0x80 | (ch & 0x3F)));
            } else if (UTF16Helper.isHighSurrogate(ch)) {
                char low = source.get();
                if (!UTF16Helper.isLowSurrogate(ch)) {
                    source.back(1);
                    dest.put((byte)'?');
                } else {
                    int codePoint = UTF16Helper.buildCodePoint(ch, low);
                    dest.put((byte)(0xF0 | (codePoint >> 18)));
                    dest.put((byte)(0x80 | ((codePoint >> 12) & 0x3F)));
                    dest.put((byte)(0x80 | ((codePoint >> 6) & 0x3F)));
                    dest.put((byte)(0x80 | (codePoint & 0x3F)));
                }
            } else {
                dest.put((byte)'?');
            }
        }
    }

    @Override
    public void decode(ByteBuffer source, CharBuffer dest) {

    }
}
