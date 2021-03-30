/*
 *  Copyright 2021 Alexey Andreev.
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

public class TUTF16Decoder extends TBufferedDecoder {
    private boolean bom;
    private boolean littleEndian;

    public TUTF16Decoder(TCharset cs, boolean bom, boolean littleEndian) {
        super(cs, 0.5f, 0.5f);
        this.bom = bom;
        this.littleEndian = littleEndian;
    }

    @Override
    protected TCoderResult arrayDecode(byte[] inArray, int inPos, int inSize, char[] outArray, int outPos, int outSize,
            Controller controller) {
        if (bom) {
            if (inPos + 2 > inSize) {
                return controller.hasMoreInput() ? null : TCoderResult.UNDERFLOW;
            }
            bom = false;
            byte b = inArray[inPos++];
            if (b == (byte) 0xFF) {
                if (inArray[inPos] == (byte) 0xFE) {
                    inPos++;
                    littleEndian = true;
                } else {
                    inPos--;
                }
            } else if (b == (byte) 0xFE) {
                if (inArray[inPos] == (byte) 0xFF) {
                    inPos++;
                    littleEndian = false;
                } else {
                    inPos--;
                }
            } else {
                inPos--;
            }
        }

        return littleEndian
                ? decodeLE(inArray, inPos, inSize, outArray, outPos, outSize, controller)
                : decodeBE(inArray, inPos, inSize, outArray, outPos, outSize, controller);
    }

    private TCoderResult decodeLE(byte[] inArray, int inPos, int inSize, char[] outArray, int outPos, int outSize,
            Controller controller) {
        TCoderResult result = null;
        while (inPos < inSize && outPos < outSize) {
            if (inPos + 2 > inSize) {
                if (!controller.hasMoreInput(2)) {
                    result = TCoderResult.UNDERFLOW;
                }
                break;
            }
            int b1 = inArray[inPos++] & 0xFF;
            int b2 = inArray[inPos++] & 0xFF;
            char c = (char) (b1 | (b2 << 8));
            if (Character.isHighSurrogate(c)) {
                if (inPos + 2 >= inSize) {
                    if (!controller.hasMoreInput(4)) {
                        result = TCoderResult.UNDERFLOW;
                    }
                    inPos -= 2;
                    break;
                }
                b1 = inArray[inPos++] & 0xFF;
                b2 = inArray[inPos++] & 0xFF;
                char next = (char) (b1 | (b2 << 8));
                if (!Character.isLowSurrogate(next)) {
                    inPos -= 4;
                    result = TCoderResult.malformedForLength(4);
                    break;
                } else {
                    if (outPos + 2 > outSize) {
                        if (!controller.hasMoreOutput(2)) {
                            result = TCoderResult.OVERFLOW;
                        }
                        break;
                    } else {
                        outArray[outPos++] = c;
                        outArray[outPos++] = next;
                    }
                }
            } else if (Character.isLowSurrogate(c)) {
                inPos -= 2;
                result = TCoderResult.malformedForLength(2);
                break;
            } else {
                outArray[outPos++] = c;
            }
        }
        controller.setInPosition(inPos);
        controller.setOutPosition(outPos);
        return result;
    }

    private TCoderResult decodeBE(byte[] inArray, int inPos, int inSize, char[] outArray, int outPos, int outSize,
            Controller controller) {
        TCoderResult result = null;
        while (inPos < inSize && outPos < outSize) {
            if (inPos + 2 > inSize) {
                if (!controller.hasMoreInput(2)) {
                    result = TCoderResult.UNDERFLOW;
                }
                break;
            }
            int b1 = inArray[inPos++] & 0xFF;
            int b2 = inArray[inPos++] & 0xFF;
            char c = (char) (b2 | (b1 << 8));
            if (Character.isHighSurrogate(c)) {
                if (inPos + 2 >= inSize) {
                    if (!controller.hasMoreInput(4)) {
                        result = TCoderResult.UNDERFLOW;
                    }
                    inPos -= 2;
                    break;
                }
                b1 = inArray[inPos++] & 0xFF;
                b2 = inArray[inPos++] & 0xFF;
                char next = (char) (b2 | (b1 << 8));
                if (!Character.isLowSurrogate(next)) {
                    inPos -= 4;
                    result = TCoderResult.malformedForLength(4);
                    break;
                } else {
                    if (outPos + 2 > outSize) {
                        if (!controller.hasMoreOutput(2)) {
                            result = TCoderResult.OVERFLOW;
                        }
                        break;
                    } else {
                        outArray[outPos++] = c;
                        outArray[outPos++] = next;
                    }
                }
            } else if (Character.isLowSurrogate(c)) {
                inPos -= 2;
                result = TCoderResult.malformedForLength(2);
                break;
            } else {
                outArray[outPos++] = c;
            }
        }
        controller.setInPosition(inPos);
        controller.setOutPosition(outPos);
        return result;
    }
}
