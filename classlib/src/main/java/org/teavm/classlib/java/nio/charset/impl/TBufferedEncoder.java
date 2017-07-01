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

import org.teavm.classlib.java.nio.TByteBuffer;
import org.teavm.classlib.java.nio.TCharBuffer;
import org.teavm.classlib.java.nio.charset.TCharset;
import org.teavm.classlib.java.nio.charset.TCharsetEncoder;
import org.teavm.classlib.java.nio.charset.TCoderResult;

public abstract class TBufferedEncoder extends TCharsetEncoder {
    public TBufferedEncoder(TCharset cs, float averageBytesPerChar, float maxBytesPerChar, byte[] replacement) {
        super(cs, averageBytesPerChar, maxBytesPerChar, replacement);
    }

    public TBufferedEncoder(TCharset cs, float averageBytesPerChar, float maxBytesPerChar) {
        super(cs, averageBytesPerChar, maxBytesPerChar);
    }

    @Override
    protected TCoderResult encodeLoop(TCharBuffer in, TByteBuffer out) {
        // Use intermediate array to batch buffer operations
        int outPos = 0;
        char[] inArray = new char[Math.min(in.remaining(), 512)];
        int inPos = 0;
        int inSize = 0;
        byte[] outArray = new byte[Math.min(out.remaining(), 512)];
        TCoderResult result = null;

        while (true) {
            // If there were remaining bytes in input buffer, copy them to the beginning of input array
            // so the next iteration will process these bytes again
            if (inPos + 32 > inSize && in.hasRemaining()) {
                for (int i = inPos; i < inSize; ++i) {
                    inArray[i - inPos] = inArray[i];
                }
                inPos = inSize - inPos;
                inSize = Math.min(in.remaining() + inPos, inArray.length);
                in.get(inArray, inPos, inSize - inPos);
                inPos = 0;
            }

            if (!out.hasRemaining()) {
                result = !in.hasRemaining() && inPos >= inSize ? TCoderResult.UNDERFLOW : TCoderResult.OVERFLOW;
                break;
            }

            // Perform iteration
            outPos = 0;
            int outSize = Math.min(out.remaining(), outArray.length);
            Controller controller = new Controller(in, out);
            result = arrayEncode(inArray, inPos, inSize, outArray, outPos, outSize, controller);
            inPos = controller.inPosition;
            if (result == null && outPos == controller.outPosition) {
                result = TCoderResult.UNDERFLOW;
            }
            outPos = controller.outPosition;

            // Write any output characters to out buffer
            out.put(outArray, 0, outPos);
            if (result != null) {
                break;
            }
        }

        in.position(in.position() - (inSize - inPos));

        return result;
    }

    protected abstract TCoderResult arrayEncode(char[] inArray, int inPos, int inSize,
            byte[] outArray, int outPos, int outSize,
            Controller controller);

    public static class Controller {
        private TCharBuffer in;
        private TByteBuffer out;
        int inPosition;
        int outPosition;

        Controller(TCharBuffer in, TByteBuffer out) {
            super();
            this.in = in;
            this.out = out;
        }

        public boolean hasMoreInput() {
            return in.hasRemaining();
        }

        public boolean hasMoreInput(int sz) {
            return in.remaining() >= sz;
        }

        public boolean hasMoreOutput() {
            return out.hasRemaining();
        }

        public boolean hasMoreOutput(int sz) {
            return out.remaining() >= sz;
        }

        public void setInPosition(int inPosition) {
            this.inPosition = inPosition;
        }

        public void setOutPosition(int outPosition) {
            this.outPosition = outPosition;
        }
    }
}
