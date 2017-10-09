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
package org.teavm.classlib.java.nio.charset;

import java.util.Arrays;
import org.teavm.classlib.java.nio.TByteBuffer;
import org.teavm.classlib.java.nio.TCharBuffer;

public abstract class TCharsetEncoder {
    private static final int READY = 4;
    private static final int ONGOING = 1;
    private static final int END = 2;
    private static final int FLUSH = 3;
    private static final int INIT = 0;
    private TCharset charset;
    private byte[] replacement;
    private float averageBytesPerChar;
    private float maxBytesPerChar;
    private TCodingErrorAction malformedAction = TCodingErrorAction.REPORT;
    private TCodingErrorAction unmappableAction = TCodingErrorAction.REPORT;
    private int status;

    protected TCharsetEncoder(TCharset cs, float averageBytesPerChar, float maxBytesPerChar, byte[] replacement) {
        checkReplacement(replacement);
        this.charset = cs;
        this.replacement = replacement.clone();
        this.averageBytesPerChar = averageBytesPerChar;
        this.maxBytesPerChar = maxBytesPerChar;
    }

    protected TCharsetEncoder(TCharset cs, float averageBytesPerChar, float maxBytesPerChar) {
        this(cs, averageBytesPerChar, maxBytesPerChar, new byte[] { (byte) '?' });
    }

    public final TCharset charset() {
        return charset;
    }

    public final byte[] replacement() {
        return replacement.clone();
    }

    public final TCharsetEncoder replaceWith(byte[] newReplacement) {
        checkReplacement(newReplacement);
        this.replacement = newReplacement.clone();
        implReplaceWith(newReplacement);
        return this;
    }

    private void checkReplacement(byte[] replacement) {
        if (replacement == null || replacement.length == 0 || replacement.length < maxBytesPerChar) {
            throw new IllegalArgumentException("Replacement preconditions do not hold");
        }
    }

    protected void implReplaceWith(@SuppressWarnings("unused") byte[] newReplacement) {
    }

    public TCodingErrorAction malformedInputAction() {
        return malformedAction;
    }

    public final TCharsetEncoder onMalformedInput(TCodingErrorAction newAction) {
        if (newAction == null) {
            throw new IllegalArgumentException("Action must be non-null");
        }
        malformedAction = newAction;
        implOnMalformedInput(newAction);
        return this;
    }

    protected void implOnMalformedInput(@SuppressWarnings("unused") TCodingErrorAction newAction) {
    }

    public TCodingErrorAction unmappableCharacterAction() {
        return unmappableAction;
    }

    public final TCharsetEncoder onUnmappableCharacter(TCodingErrorAction newAction) {
        if (newAction == null) {
            throw new IllegalArgumentException("Action must be non-null");
        }
        unmappableAction = newAction;
        implOnUnmappableCharacter(newAction);
        return this;
    }

    protected void implOnUnmappableCharacter(@SuppressWarnings("unused") TCodingErrorAction newAction) {
    }

    public final float averageBytesPerChar() {
        return averageBytesPerChar;
    }

    public final float maxBytesPerChar() {
        return maxBytesPerChar;
    }

    public final TCoderResult encode(TCharBuffer in, TByteBuffer out, boolean endOfInput) {
        if (status == FLUSH || !endOfInput && status == END) {
            throw new IllegalStateException();
        }

        status = endOfInput ? END : ONGOING;
        TCoderResult result;
        while (true) {
            try {
                result = encodeLoop(in, out);
            } catch (RuntimeException e) {
                throw new TCoderMalfunctionError(e);
            }
            if (result.isUnderflow()) {
                if (endOfInput) {
                    int remaining = in.remaining();
                    if (remaining > 0) {
                        result = TCoderResult.malformedForLength(remaining);
                    } else {
                        return result;
                    }
                } else {
                    return result;
                }
            } else if (result.isOverflow()) {
                return result;
            }
            TCodingErrorAction action = result.isUnmappable() ? unmappableAction : malformedAction;
            if (action == TCodingErrorAction.REPLACE) {
                if (out.remaining() < replacement.length) {
                    return TCoderResult.OVERFLOW;
                }
                out.put(replacement);
            } else {
                if (action != TCodingErrorAction.IGNORE) {
                    return result;
                }
            }
            in.position(in.position() + result.length());
        }
    }

    public final TByteBuffer encode(TCharBuffer in) throws TCharacterCodingException {
        if (in.remaining() == 0) {
            return TByteBuffer.allocate(0);
        }
        reset();
        TByteBuffer output = TByteBuffer.allocate((int) (in.remaining() * averageBytesPerChar));

        TCoderResult result;
        while (true) {
            result = encode(in, output, false);
            if (result == TCoderResult.UNDERFLOW) {
                break;
            } else if (result == TCoderResult.OVERFLOW) {
                output = allocateMore(output);
                continue;
            }
            if (result.isError()) {
                result.throwException();
            }
        }

        result = encode(in, output, true);
        if (result.isError()) {
            result.throwException();
        }

        while (true) {
            result = flush(output);
            if (result.isUnderflow()) {
                break;
            } else if (result.isOverflow()) {
                output = allocateMore(output);
            }
        }
        output.flip();
        return output;
    }

    protected abstract TCoderResult encodeLoop(TCharBuffer in, TByteBuffer out);

    public boolean canEncode(char c) {
        return implCanEncode(TCharBuffer.wrap(new char[] { c }));
    }

    private boolean implCanEncode(TCharBuffer cb) {
        if (status == FLUSH || status == INIT) {
            status = READY;
        }
        if (status != READY) {
            throw new IllegalStateException();
        }
        TCodingErrorAction malformBak = malformedAction;
        TCodingErrorAction unmapBak = unmappableAction;
        onMalformedInput(TCodingErrorAction.REPORT);
        onUnmappableCharacter(TCodingErrorAction.REPORT);
        boolean result = true;
        try {
            encode(cb);
        } catch (TCharacterCodingException e) {
            result = false;
        }
        onMalformedInput(malformBak);
        onUnmappableCharacter(unmapBak);
        reset();
        return result;
    }

    public boolean canEncode(CharSequence sequence) {
        TCharBuffer cb;
        if (sequence instanceof TCharBuffer) {
            cb = ((TCharBuffer) sequence).duplicate();
        } else {
            cb = TCharBuffer.wrap(sequence);
        }
        return implCanEncode(cb);
    }

    private TByteBuffer allocateMore(TByteBuffer buffer) {
        byte[] array = buffer.array();
        array = Arrays.copyOf(array, array.length * 2);
        TByteBuffer result = TByteBuffer.wrap(array);
        result.position(buffer.position());
        return result;
    }

    public final TCoderResult flush(TByteBuffer out) {
        if (status != END && status != READY) {
            throw new IllegalStateException();
        }
        TCoderResult result = implFlush(out);
        if (result == TCoderResult.UNDERFLOW) {
            status = FLUSH;
        }
        return result;
    }

    protected TCoderResult implFlush(@SuppressWarnings("unused") TByteBuffer out) {
        return TCoderResult.UNDERFLOW;
    }

    public final TCharsetEncoder reset() {
        status = INIT;
        implReset();
        return this;
    }

    protected void implReset() {
    }
}
