/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.classlib.java.nio.charset.impl;

import org.teavm.classlib.impl.charset.UTF16Helper;
import org.teavm.classlib.java.nio.TByteBuffer;
import org.teavm.classlib.java.nio.TCharBuffer;
import org.teavm.classlib.java.nio.charset.TCharset;
import org.teavm.classlib.java.nio.charset.TCharsetDecoder;
import org.teavm.classlib.java.nio.charset.TCoderResult;

/**
 *
 * @author Alexey Andreev
 */
public class UTF8Decoder extends TCharsetDecoder {
    public UTF8Decoder(TCharset cs) {
        super(cs, 1f / 3, 0.5f);
    }

    @Override
    protected TCoderResult decodeLoop(TByteBuffer in, TCharBuffer out) {
        while (true) {
            if (in.remaining() < 4) {
                return TCoderResult.UNDERFLOW;
            }
            if (!out.hasRemaining()) {
                return TCoderResult.OVERFLOW;
            }
            int b = in.get() & 0xFF;
            if ((b & 0x80) == 0) {
                out.put((char)b);
            } else if ((b & 0xE0) == 0xC0) {
                if (!in.hasRemaining()) {
                    in.position(in.position() - 1);
                    return TCoderResult.UNDERFLOW;
                }
                out.put((char)(((b & 0x1F) << 6) | (in.get() & 0x3F)));
            } else if ((b & 0xF0) == 0xE0) {
                if (in.remaining() < 2) {
                    in.position(in.position() - 1);
                    return TCoderResult.UNDERFLOW;
                }
                byte b2 = in.get();
                byte b3 = in.get();
                char c = (char)(((b & 0x0F) << 12) | ((b2 & 0x3f) << 6) | (b3 & 0x3F));
                if (Character.isSurrogate(c)) {
                    in.position(in.position() - 2);
                    return TCoderResult.malformedForLength(3);
                }
                out.put(c);
            } else if ((b & 0xF8) == 0xF0) {
                if (in.remaining() < 3) {
                    in.position(in.position() - 1);
                    return TCoderResult.UNDERFLOW;
                }
                if (out.remaining() < 3) {
                    in.position(in.position() - 1);
                    return TCoderResult.OVERFLOW;
                }
                byte b2 = in.get();
                byte b3 = in.get();
                byte b4 = in.get();
                int code = ((b & 0x07) << 18) | ((b2 & 0x3f) << 12) | ((b3 & 0x3F) << 6) | (b4 & 0x3F);
                out.put(UTF16Helper.highSurrogate(code));
                out.put(UTF16Helper.lowSurrogate(code));
            }
        }
    }
}
