/*
 *  Copyright 2013 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
                if (!UTF16Helper.isLowSurrogate(low)) {
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
        while (!source.end() && dest.available() >= 2) {
            int b = source.get() & 0xFF;
            if ((b & 0x80) == 0) {
                dest.put((char)b);
            } else if ((b & 0xE0) == 0xC0) {
                if (source.end()) {
                    dest.put((char)b);
                    return;
                }
                dest.put((char)(((b & 0x1F) << 6) | (source.get() & 0x3F)));
            } else if ((b & 0xF0) == 0xE0) {
                if (source.available() < 2) {
                    source.skip(source.available());
                    dest.put((char)b);
                    return;
                }
                byte b2 = source.get();
                byte b3 = source.get();
                char c = (char)(((b & 0x0F) << 12) | ((b2 & 0x3f) << 6) | (b3 & 0x3F));
                dest.put(!UTF16Helper.isHighSurrogate(c) ? c : '?');
            } else if ((b & 0xF8) == 0xF0) {
                if (source.available() < 3) {
                    source.skip(source.available());
                    dest.put((char)b);
                    return;
                }
                byte b2 = source.get();
                byte b3 = source.get();
                byte b4 = source.get();
                int code = (((b & 0x07) << 18) | ((b2 & 0x3f) << 12) | ((b3 & 0x3F) << 6) | (b4 & 0x3F)) -
                        UTF16Helper.SUPPLEMENTARY_PLANE;
                dest.put(UTF16Helper.highSurrogate(code));
                dest.put(UTF16Helper.lowSurrogate(code));
            }
        }
    }
}
