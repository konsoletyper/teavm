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

public class TUTF8Decoder extends TBufferedDecoder {
    public TUTF8Decoder(TCharset cs) {
        super(cs, 1f / 3, 0.5f);
    }

    @Override
    protected TCoderResult arrayDecode(byte[] inArray, int inPos, int inSize, char[] outArray, int outPos, int outSize,
            Controller controller) {
        TCoderResult result = null;
        while (inPos < inSize && outPos < outSize) {
            int b = inArray[inPos++] & 0xFF;
            if ((b & 0x80) == 0) {
                outArray[outPos++] = (char) b;
            } else if ((b & 0xE0) == 0xC0) {
                if (inPos >= inSize) {
                    --inPos;
                    if (!controller.hasMoreInput()) {
                        result = TCoderResult.UNDERFLOW;
                    }
                    break;
                }
                byte b2 = inArray[inPos++];
                if (!checkMidByte(b2)) {
                    inPos -= 2;
                    result = TCoderResult.malformedForLength(1);
                    break;
                }
                outArray[outPos++] = (char) (((b & 0x1F) << 6) | (b2 & 0x3F));
            } else if ((b & 0xF0) == 0xE0) {
                if (inPos + 2 > inSize) {
                    --inPos;
                    if (!controller.hasMoreInput()) {
                        result = TCoderResult.UNDERFLOW;
                    }
                    break;
                }
                byte b2 = inArray[inPos++];
                byte b3 = inArray[inPos++];
                if (!checkMidByte(b2) || !checkMidByte(b3)) {
                    inPos -= 3;
                    result = TCoderResult.malformedForLength(1);
                    break;
                }
                char c = (char) (((b & 0x0F) << 12) | ((b2 & 0x3f) << 6) | (b3 & 0x3F));
                if (Character.isSurrogate(c)) {
                    inPos -= 3;
                    result = TCoderResult.malformedForLength(3);
                    break;
                }
                outArray[outPos++] = c;
            } else if ((b & 0xF8) == 0xF0) {
                if (inPos + 3 > inSize) {
                    --inPos;
                    if (!controller.hasMoreInput()) {
                        result = TCoderResult.UNDERFLOW;
                    }
                    break;
                }
                if (outPos + 2 > outSize) {
                    --inPos;
                    if (!controller.hasMoreOutput(2)) {
                        result = TCoderResult.OVERFLOW;
                    }
                    break;
                }
                byte b2 = inArray[inPos++];
                byte b3 = inArray[inPos++];
                byte b4 = inArray[inPos++];
                if (!checkMidByte(b2) || !checkMidByte(b3) || !checkMidByte(b4)) {
                    inPos -= 3;
                    result = TCoderResult.malformedForLength(1);
                    break;
                }
                int code = ((b & 0x07) << 18) | ((b2 & 0x3f) << 12) | ((b3 & 0x3F) << 6) | (b4 & 0x3F);
                outArray[outPos++] = Character.highSurrogate(code);
                outArray[outPos++] = Character.lowSurrogate(code);
            } else {
                --inPos;
                result = TCoderResult.malformedForLength(1);
                break;
            }
        }

        controller.setInPosition(inPos);
        controller.setOutPosition(outPos);
        return result;
    }

    private boolean checkMidByte(byte b) {
        return (b & 0xC0) == 0x80;
    }
}
