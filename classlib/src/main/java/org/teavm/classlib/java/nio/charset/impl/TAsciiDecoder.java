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

public class TAsciiDecoder extends TBufferedDecoder {
    public TAsciiDecoder(TCharset cs) {
        super(cs, 1, 1);
    }

    @Override
    protected TCoderResult arrayDecode(byte[] inArray, int inPos, int inSize, char[] outArray, int outPos, int outSize,
            Controller controller) {
        TCoderResult result = null;
        while (inPos < inSize && outPos < outSize) {
            int b = inArray[inPos++] & 0xFf;
            if ((b & 0x80) != 0) {
                result = TCoderResult.malformedForLength(1);
                --inPos;
                break;
            } else {
                outArray[outPos++] = (char) b;
            }
        }

        controller.setInPosition(inPos);
        controller.setOutPosition(outPos);

        return result;
    }
}
