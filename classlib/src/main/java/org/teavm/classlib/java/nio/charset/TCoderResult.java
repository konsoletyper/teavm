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

public class TCoderResult {
    public static final TCoderResult UNDERFLOW = new TCoderResult((byte) 0, 0);
    public static final TCoderResult OVERFLOW = new TCoderResult((byte) 1, 0);
    private byte kind;
    private int length;

    TCoderResult(byte kind, int length) {
        super();
        this.kind = kind;
        this.length = length;
    }

    public boolean isUnderflow() {
        return kind == 0;
    }

    public boolean isOverflow() {
        return kind == 1;
    }

    public boolean isError() {
        return isMalformed() || isUnmappable();
    }

    public boolean isMalformed() {
        return kind == 2;
    }

    public boolean isUnmappable() {
        return kind == 3;
    }

    public int length() {
        if (!isError()) {
            throw new UnsupportedOperationException();
        }
        return length;
    }

    public static TCoderResult malformedForLength(int length) {
        return new TCoderResult((byte) 2, length);
    }

    public static TCoderResult unmappableForLength(int length) {
        return new TCoderResult((byte) 3, length);
    }

    public void throwException() throws TCharacterCodingException {
        switch (kind) {
            case 0:
                throw new TBufferUnderflowException();
            case 1:
                throw new TBufferOverflowException();
            case 2:
                throw new TMalformedInputException(length);
            case 3:
                throw new TUnmappableCharacterException(length);
        }
    }

    @Override
    public String toString() {
        switch (kind) {
            case 0:
                return "UNDERFLOW";
            case 1:
                return "OVERFLOW";
            case 2:
                return "MALFORMED " + length;
            case 3:
                return "UNMAPPABLE " + length;
            default:
                throw new AssertionError();
        }
    }
}
