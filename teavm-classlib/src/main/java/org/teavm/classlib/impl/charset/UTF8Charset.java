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
