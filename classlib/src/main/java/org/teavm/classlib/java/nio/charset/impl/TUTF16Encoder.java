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

public class TUTF16Encoder extends TBufferedEncoder {
    private boolean bom;
    private boolean littleEndian;

    public TUTF16Encoder(TCharset cs, boolean bom, boolean littleEndian) {
        super(cs, 2, 4);
        this.bom = bom;
        this.littleEndian = littleEndian;
    }

    @Override
    protected TCoderResult arrayEncode(char[] inArray, int inPos, int inSize, byte[] outArray, int outPos, int outSize,
            Controller controller) {
        if (bom) {
            if (outPos + 2 > outSize) {
                return controller.hasMoreOutput() ? null : TCoderResult.OVERFLOW;
            }
            bom = false;
            if (littleEndian) {
                outArray[outPos++] = (byte) 0xFF;
                outArray[outPos++] = (byte) 0xFE;
            } else {
                outArray[outPos++] = (byte) 0xFE;
                outArray[outPos++] = (byte) 0xFF;
            }
        }

        return littleEndian
                ? arrayEncodeLE(inArray, inPos, inSize, outArray, outPos, outSize, controller)
                : arrayEncodeBE(inArray, inPos, inSize, outArray, outPos, outSize, controller);
    }

    private TCoderResult arrayEncodeLE(char[] inArray, int inPos, int inSize, byte[] outArray, int outPos, int outSize,
            Controller controller) {
        TCoderResult result = null;
        while (inPos < inSize && outPos < outSize) {
            char c = inArray[inPos++];
            if (Character.isHighSurrogate(c)) {
                if (inPos == inSize) {
                    inPos--;
                    if (!controller.hasMoreInput(2)) {
                        result = TCoderResult.UNDERFLOW;
                    }
                    break;
                }
                char next = inArray[inPos++];
                if (Character.isLowSurrogate(next)) {
                    if (outPos + 4 <= outSize) {
                        outArray[outPos++] = (byte) (c & 0xFF);
                        outArray[outPos++] = (byte) (c >> 8);
                        outArray[outPos++] = (byte) (next & 0xFF);
                        outArray[outPos++] = (byte) (next >> 8);
                    } else {
                        inPos -= 2;
                        if (!controller.hasMoreOutput(4)) {
                            result = TCoderResult.OVERFLOW;
                        }
                        break;
                    }
                } else {
                    inPos -= 2;
                    result = TCoderResult.malformedForLength(1);
                    break;
                }
            } else if (Character.isLowSurrogate(c)) {
                inPos--;
                result = TCoderResult.malformedForLength(1);
                break;
            } else {
                if (outPos + 2 <= outSize) {
                    outArray[outPos++] = (byte) (c & 0xFF);
                    outArray[outPos++] = (byte) (c >> 8);
                } else {
                    inPos--;
                    if (!controller.hasMoreOutput(2)) {
                        result = TCoderResult.OVERFLOW;
                    }
                    break;
                }
            }
        }

        controller.setInPosition(inPos);
        controller.setOutPosition(outPos);
        return result;
    }

    private TCoderResult arrayEncodeBE(char[] inArray, int inPos, int inSize, byte[] outArray, int outPos, int outSize,
            Controller controller) {
        TCoderResult result = null;
        while (inPos < inSize && outPos < outSize) {
            char c = inArray[inPos++];
            if (Character.isHighSurrogate(c)) {
                if (inPos == inSize) {
                    inPos--;
                    if (!controller.hasMoreInput(2)) {
                        result = TCoderResult.UNDERFLOW;
                    }
                    break;
                }
                char next = inArray[inPos++];
                if (Character.isLowSurrogate(next)) {
                    if (outPos + 4 <= outSize) {
                        outArray[outPos++] = (byte) (c >> 8);
                        outArray[outPos++] = (byte) (c & 0xFF);
                        outArray[outPos++] = (byte) (next >> 8);
                        outArray[outPos++] = (byte) (next & 0xFF);
                    } else {
                        inPos -= 2;
                        if (!controller.hasMoreOutput(4)) {
                            result = TCoderResult.OVERFLOW;
                        }
                        break;
                    }
                } else {
                    inPos -= 2;
                    result = TCoderResult.malformedForLength(1);
                    break;
                }
            } else if (Character.isLowSurrogate(c)) {
                inPos--;
                result = TCoderResult.malformedForLength(1);
                break;
            } else {
                if (outPos + 2 <= outSize) {
                    outArray[outPos++] = (byte) (c >> 8);
                    outArray[outPos++] = (byte) (c & 0xFF);
                } else {
                    inPos--;
                    if (!controller.hasMoreOutput(2)) {
                        result = TCoderResult.OVERFLOW;
                    }
                    break;
                }
            }
        }

        controller.setInPosition(inPos);
        controller.setOutPosition(outPos);
        return result;
    }
}
