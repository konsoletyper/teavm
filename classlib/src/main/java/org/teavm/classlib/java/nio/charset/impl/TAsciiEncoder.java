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

public class TAsciiEncoder extends TBufferedEncoder {
    public TAsciiEncoder(TCharset cs) {
        super(cs, 1, 1);
    }

    @Override
    protected TCoderResult arrayEncode(char[] inArray, int inPos, int inSize, byte[] outArray, int outPos, int outSize,
            Controller controller) {
        TCoderResult result = null;
        while (inPos < inSize && outPos < outSize) {
            char c = inArray[inPos++];
            if (Character.isHighSurrogate(c)) {
                if (inPos >= inSize) {
                    if (!controller.hasMoreInput(2)) {
                        result = TCoderResult.UNDERFLOW;
                    } else {
                        inPos--;
                    }
                    break;
                } else {
                    char next = inArray[inPos];
                    if (!Character.isLowSurrogate(next)) {
                        result = TCoderResult.malformedForLength(1);
                    } else {
                        --inPos;
                        result = TCoderResult.unmappableForLength(2);
                    }
                    break;
                }
            } else if (Character.isLowSurrogate(c)) {
                result = TCoderResult.malformedForLength(1);
            }
            if (c < 128) {
                outArray[outPos++] = (byte) c;
            } else {
                result = TCoderResult.unmappableForLength(1);
                --inPos;
                break;
            }
        }
        controller.setInPosition(inPos);
        controller.setOutPosition(outPos);
        return result;
    }
}
