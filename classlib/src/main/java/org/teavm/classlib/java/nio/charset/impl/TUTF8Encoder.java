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

import org.teavm.classlib.java.nio.charset.TCharset;
import org.teavm.classlib.java.nio.charset.TCoderResult;

public class TUTF8Encoder extends TBufferedEncoder {
    TUTF8Encoder(TCharset cs) {
        super(cs, 2, 4);
    }

    @Override
    protected TCoderResult arrayEncode(char[] inArray, int inPos, int inSize, byte[] outArray, int outPos, int outSize,
            Controller controller) {
        TCoderResult result = null;
        while (inPos < inSize && outPos < outSize) {
            char ch = inArray[inPos++];
            if (ch < 0x80) {
                outArray[outPos++] = (byte) ch;
            } else if (ch < 0x800) {
                if (outPos + 2 > outSize) {
                    --inPos;
                    if (!controller.hasMoreOutput(2)) {
                        result = TCoderResult.OVERFLOW;
                    }
                    break;
                }
                outArray[outPos++] = (byte) (0xC0 | (ch >> 6));
                outArray[outPos++] = (byte) (0x80 | (ch & 0x3F));
            } else if (!Character.isSurrogate(ch)) {
                if (outPos + 3 > outSize) {
                    --inPos;
                    if (!controller.hasMoreOutput(3)) {
                        result = TCoderResult.OVERFLOW;
                    }
                    break;
                }
                outArray[outPos++] = (byte) (0xE0 | (ch >> 12));
                outArray[outPos++] = (byte) (0x80 | ((ch >> 6) & 0x3F));
                outArray[outPos++] = (byte) (0x80 | (ch & 0x3F));
            } else if (Character.isHighSurrogate(ch)) {
                if (inPos >= inSize) {
                    if (!controller.hasMoreInput()) {
                        result = TCoderResult.UNDERFLOW;
                    }
                    break;
                }
                char low = inArray[inPos++];
                if (!Character.isLowSurrogate(low)) {
                    inPos -= 2;
                    result = TCoderResult.malformedForLength(1);
                    break;
                }
                if (outPos + 4 > outSize) {
                    inPos -= 2;
                    if (!controller.hasMoreOutput(4)) {
                        result = TCoderResult.OVERFLOW;
                    }
                    break;
                }
                int codePoint = Character.toCodePoint(ch, low);
                outArray[outPos++] = (byte) (0xF0 | (codePoint >> 18));
                outArray[outPos++] = (byte) (0x80 | ((codePoint >> 12) & 0x3F));
                outArray[outPos++] = (byte) (0x80 | ((codePoint >> 6) & 0x3F));
                outArray[outPos++] = (byte) (0x80 | (codePoint & 0x3F));
            } else {
                result = TCoderResult.malformedForLength(1);
                break;
            }
        }

        controller.setInPosition(inPos);
        controller.setOutPosition(outPos);
        return result;
    }
}
