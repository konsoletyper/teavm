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

public abstract class TCharsetDecoder {
    private static final int INIT = 0;
    private static final int IN_PROGRESS = 1;
    private static final int END = 2;
    private static final int FLUSH = 3;
    private TCharset charset;
    private float averageCharsPerByte;
    private float maxCharsPerByte;
    private String replacement = "\uFFFD";
    private TCodingErrorAction malformedAction = TCodingErrorAction.REPORT;
    private TCodingErrorAction unmappableAction = TCodingErrorAction.REPORT;
    private int state;

    protected TCharsetDecoder(TCharset cs, float averageCharsPerByte, float maxCharsPerByte) {
        if (averageCharsPerByte <= 0) {
            throw new IllegalArgumentException("averageCharsPerByte must be positive. Actual value is "
                    + averageCharsPerByte);
        }
        if (maxCharsPerByte <= 0) {
            throw new IllegalArgumentException("maxCharsPerByte must be positive. Actual value is "
                    + maxCharsPerByte);
        }
        this.charset = cs;
        this.averageCharsPerByte = averageCharsPerByte;
        this.maxCharsPerByte = maxCharsPerByte;
    }

    public final TCharset charset() {
        return charset;
    }

    public final String replacement() {
        return replacement;
    }

    public final TCharsetDecoder replaceWith(String newReplacement) {
        if (newReplacement == null || newReplacement.isEmpty()) {
            throw new IllegalArgumentException("New replacement is null or empty");
        }
        this.replacement = newReplacement;
        implReplaceWith(newReplacement);
        return this;
    }

    protected void implReplaceWith(@SuppressWarnings("unused") String newReplacement) {
    }

    public TCodingErrorAction malformedInputAction() {
        return malformedAction;
    }

    public final TCharsetDecoder onMalformedInput(TCodingErrorAction newAction) {
        if (newAction == null) {
            throw new IllegalArgumentException("newAction must be non-null");
        }
        this.malformedAction = newAction;
        implOnMalformedInput(newAction);
        return this;
    }

    protected void implOnMalformedInput(@SuppressWarnings("unused") TCodingErrorAction newAction) {
    }

    public TCodingErrorAction unmappableCharacterAction() {
        return unmappableAction;
    }

    public final TCharsetDecoder onUnmappableCharacter(TCodingErrorAction newAction) {
        if (newAction == null) {
            throw new IllegalArgumentException("newAction must be non-null");
        }
        this.unmappableAction = newAction;
        implOnUnmappableCharacter(newAction);
        return this;
    }

    protected void implOnUnmappableCharacter(@SuppressWarnings("unused") TCodingErrorAction newAction) {
    }

    public final float averageCharsPerByte() {
        return averageCharsPerByte;
    }

    public final float maxCharsPerByte() {
        return maxCharsPerByte;
    }

    public final TCoderResult decode(TByteBuffer in, TCharBuffer out, boolean endOfInput) {
        if (state == END && !endOfInput || state == FLUSH) {
            throw new IllegalStateException();
        }
        state = !endOfInput ? IN_PROGRESS : END;
        while (true) {
            TCoderResult result;
            try {
                result = decodeLoop(in, out);
            } catch (RuntimeException e) {
                throw new TCoderMalfunctionError(e);
            }
            if (result.isOverflow()) {
                return result;
            } else if (result.isUnderflow()) {
                if (endOfInput && in.hasRemaining()) {
                    if (malformedAction == TCodingErrorAction.REPORT) {
                        return TCoderResult.malformedForLength(in.remaining());
                    } else {
                        if (out.remaining() > replacement.length()) {
                            in.position(in.position() + in.remaining());
                            if (malformedAction == TCodingErrorAction.REPLACE) {
                                out.put(replacement);
                            }
                        } else {
                            return TCoderResult.OVERFLOW;
                        }
                    }
                }
                return result;
            } else if (result.isMalformed()) {
                if (malformedAction == TCodingErrorAction.REPORT) {
                    return result;
                }
                if (malformedAction == TCodingErrorAction.REPLACE) {
                    if (out.remaining() < replacement.length()) {
                        return TCoderResult.OVERFLOW;
                    }
                    out.put(replacement);
                }
                in.position(in.position() + result.length());
            } else if (result.isUnmappable()) {
                if (unmappableAction == TCodingErrorAction.REPORT) {
                    return result;
                }
                if (unmappableAction == TCodingErrorAction.REPLACE) {
                    if (out.remaining() < replacement.length()) {
                        return TCoderResult.OVERFLOW;
                    }
                    out.put(replacement);
                }
                in.position(in.position() + result.length());
            }
        }
    }

    public final TCoderResult flush(TCharBuffer out) {
        if (state != FLUSH && state != END) {
            throw new IllegalStateException();
        }
        state = FLUSH;
        return implFlush(out);
    }

    public final TCharsetDecoder reset() {
        state = INIT;
        implReset();
        return this;
    }

    public final TCharBuffer decode(TByteBuffer in) throws TCharacterCodingException {
        if (state != INIT && state != FLUSH) {
            throw new IllegalStateException();
        }
        if (in.remaining() == 0) {
            return TCharBuffer.allocate(0);
        }
        if (state != INIT) {
            reset();
        }

        TCharBuffer out = TCharBuffer.allocate(Math.max(8, (int) (in.remaining() * averageCharsPerByte)));
        TCoderResult result;
        while (true) {
            result = decode(in, out, false);
            if (result.isUnderflow()) {
                break;
            } else if (result.isOverflow()) {
                out = expand(out);
            }
            if (result.isError()) {
                result.throwException();
            }
        }

        result = decode(in, out, true);
        if (result.isError()) {
            result.throwException();
        }

        while (true) {
            result = flush(out);
            if (result.isUnderflow()) {
                break;
            } else {
                out = expand(out);
            }
        }

        out.flip();
        return out;
    }

    public boolean isAutoDetecting() {
        return false;
    }

    public boolean isCharsetDetected() {
        throw new UnsupportedOperationException();
    }

    public TCharset detectedCharset() {
        throw new UnsupportedOperationException();
    }

    private TCharBuffer expand(TCharBuffer buffer) {
        char[] array = buffer.array();
        array = Arrays.copyOf(array, Math.max(8, array.length * 2));
        TCharBuffer result = TCharBuffer.wrap(array);
        result.position(buffer.position());
        return result;
    }

    protected abstract TCoderResult decodeLoop(TByteBuffer in, TCharBuffer out);

    protected TCoderResult implFlush(@SuppressWarnings("unused") TCharBuffer out) {
        return TCoderResult.UNDERFLOW;
    }

    protected void implReset() {
    }
}
