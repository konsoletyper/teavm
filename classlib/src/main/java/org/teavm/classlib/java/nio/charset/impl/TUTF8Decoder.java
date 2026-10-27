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

/**
 * Follows the JDK's rules for malformed input (sun.nio.cs.UTF_8.Decoder): the malformed length of each bad
 * sequence, and rejection of overlong forms and of code points above U+10FFFF.
 */
public class TUTF8Decoder extends TBufferedDecoder {
    public TUTF8Decoder(TCharset cs) {
        super(cs, 1f / 3, 0.5f);
    }

    @Override
    protected TCoderResult arrayDecode(byte[] inArray, int inPos, int inSize, char[] outArray, int outPos, int outSize,
            Controller controller) {
        TCoderResult result = null;
        // Well-formed sequences are decoded inline. For anything else, the position goes back to the lead byte,
        // and invalidSequence finds the result, so that a result reports the input from there.
        while (inPos < inSize && outPos < outSize) {
            int b = inArray[inPos++];
            if (b >= 0) {
                outArray[outPos++] = (char) b;
            } else if ((b & 0xE0) == 0xC0) {
                int b2 = inPos < inSize ? inArray[inPos] : 0;
                // (b & 0x1E) == 0 is C0 or C1, the lead of an overlong form.
                if ((b & 0x1E) != 0 && isContinuation(b2)) {
                    inPos++;
                    outArray[outPos++] = (char) (((b & 0x1F) << 6) | (b2 & 0x3F));
                } else {
                    --inPos;
                    result = invalidSequence(b & 0xFF, inArray, inPos + 1, inSize, controller);
                    break;
                }
            } else if ((b & 0xF0) == 0xE0) {
                int b2 = inPos < inSize ? inArray[inPos] : 0;
                int b3 = inPos + 1 < inSize ? inArray[inPos + 1] : 0;
                char c = (char) (((b & 0x0F) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F));
                // c < 0x800 is an overlong form.
                if (isContinuation(b2) && isContinuation(b3) && c >= 0x800 && !Character.isSurrogate(c)) {
                    inPos += 2;
                    outArray[outPos++] = c;
                } else {
                    --inPos;
                    result = invalidSequence(b & 0xFF, inArray, inPos + 1, inSize, controller);
                    break;
                }
            } else if ((b & 0xF8) == 0xF0) {
                int b2 = inPos < inSize ? inArray[inPos] : 0;
                int b3 = inPos + 1 < inSize ? inArray[inPos + 1] : 0;
                int b4 = inPos + 2 < inSize ? inArray[inPos + 2] : 0;
                int code = ((b & 0x07) << 18) | ((b2 & 0x3F) << 12) | ((b3 & 0x3F) << 6) | (b4 & 0x3F);
                // Below U+10000 is an overlong form, and F5-F7 give code points above U+10FFFF.
                if (isContinuation(b2) && isContinuation(b3) && isContinuation(b4)
                        && code >= 0x10000 && code <= 0x10FFFF) {
                    if (outPos + 2 > outSize) {
                        --inPos;
                        if (!controller.hasMoreOutput(2)) {
                            result = TCoderResult.OVERFLOW;
                        }
                        break;
                    }
                    inPos += 3;
                    outArray[outPos++] = Character.highSurrogate(code);
                    outArray[outPos++] = Character.lowSurrogate(code);
                } else {
                    --inPos;
                    result = invalidSequence(b & 0xFF, inArray, inPos + 1, inSize, controller);
                    break;
                }
            } else {
                // 80-BF (a continuation byte) and F8-FF.
                --inPos;
                result = TCoderResult.malformedForLength(1);
                break;
            }
        }

        controller.setInPosition(inPos);
        controller.setOutPosition(outPos);
        return result;
    }

    /**
     * The result for a lead byte that does not start a well-formed sequence: the JDK's malformed length, or null
     * (refill and retry) or UNDERFLOW when the sequence is cut off but may still be valid.
     */
    private static TCoderResult invalidSequence(int b, byte[] inArray, int next, int inSize, Controller controller) {
        int avail = inSize - next;
        if (b >= 0xC2 && b <= 0xDF) {
            if (avail >= 1) {
                return TCoderResult.malformedForLength(1);
            }
        } else if ((b & 0xF0) == 0xE0) {
            if (avail >= 1 && isMalformed3Prefix(b, inArray[next])) {
                return TCoderResult.malformedForLength(1);
            }
            if (avail >= 2) {
                // The third byte is not a continuation byte, or the sequence encodes a surrogate.
                return TCoderResult.malformedForLength(isContinuation(inArray[next + 1]) ? 3 : 2);
            }
        } else if (b >= 0xF0 && b <= 0xF4) {
            if (avail >= 1 && isMalformed4Prefix(b, inArray[next] & 0xFF)) {
                return TCoderResult.malformedForLength(1);
            }
            if (avail >= 2 && !isContinuation(inArray[next + 1])) {
                return TCoderResult.malformedForLength(2);
            }
            if (avail >= 3) {
                // With F0-F4 and a valid second byte, the code point is always in U+10000..U+10FFFF, so only
                // the fourth byte can be wrong.
                return TCoderResult.malformedForLength(3);
            }
        } else {
            // 80-C1 (a continuation byte, or the lead of an overlong 2-byte form) and F5-FF (no code point
            // above U+10FFFF has a valid form).
            return TCoderResult.malformedForLength(1);
        }
        return controller.hasMoreInput() ? null : TCoderResult.UNDERFLOW;
    }

    private static boolean isContinuation(int b) {
        return (b & 0xC0) == 0x80;
    }

    /** The JDK's isMalformed3_2: an overlong E0 form, or a second byte that is not a continuation byte. */
    private static boolean isMalformed3Prefix(int b1, int b2) {
        return (b1 == 0xE0 && (b2 & 0xE0) == 0x80) || !isContinuation(b2);
    }

    /** The JDK's isMalformed4_2: F0 needs 90-BF, F4 needs 80-8F, every second byte must be a continuation. */
    private static boolean isMalformed4Prefix(int b1, int b2) {
        return (b1 == 0xF0 && (b2 < 0x90 || b2 > 0xBF)) || (b1 == 0xF4 && (b2 & 0xF0) != 0x80) || !isContinuation(b2);
    }
}
