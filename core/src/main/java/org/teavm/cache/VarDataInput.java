/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.cache;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class VarDataInput {
    private static final int DATA = 0x7F;
    private static final int NEXT = 0x80;
    private InputStream input;

    public VarDataInput(InputStream input) {
        this.input = input;
    }

    public int readUnsigned() throws IOException {
        int value = 0;
        int pos = 0;
        int b;
        do {
            b = input.read();
            if (b < 0) {
                throw new EOFException();
            }
            value |= (b & DATA) << pos;
            pos += 7;
        } while ((b & NEXT) != 0);
        return value;
    }

    public int readSigned() throws IOException {
        int value = readUnsigned();
        return (value & 1) == 0 ? (value >>> 1) : ~(value >>> 1);
    }

    public long readUnsignedLong() throws IOException {
        long value = 0;
        int pos = 0;
        int b;
        do {
            b = input.read();
            if (b < 0) {
                throw new EOFException();
            }
            value |= ((long) b & DATA) << pos;
            pos += 7;
        } while ((b & NEXT) != 0);
        return value;
    }

    public long readSignedLong() throws IOException {
        long value = readUnsignedLong();
        return (value & 1) == 0 ? (value >>> 1) : ~(value >>> 1);
    }

    public float readFloat() throws IOException {
        int exponent = readUnsigned();
        if (exponent == 0) {
            return 0;
        }

        exponent--;
        exponent = (exponent & 1) == 0 ? exponent >>> 1 : -(exponent >>> 1);
        exponent += 127;

        int mantissa = Integer.reverse(readUnsigned()) >>> 8;
        boolean sign = (mantissa & (1 << 23)) != 0;

        int bits = mantissa & ((1 << 23) - 1);
        bits |= exponent << 23;
        if (sign) {
            bits |= 1 << 31;
        }

        return Float.intBitsToFloat(bits);
    }

    public double readDouble() throws IOException {
        int exponent = readUnsigned();
        if (exponent == 0) {
            return 0;
        }

        exponent--;
        exponent = (exponent & 1) == 0 ? exponent >>> 1 : -(exponent >>> 1);
        exponent += 1023;

        long mantissa = Long.reverse(readUnsignedLong()) >>> 11;
        boolean sign = (mantissa & (1L << 52)) != 0;

        long bits = mantissa & ((1L << 52) - 1);
        bits |= (long) exponent << 52;
        if (sign) {
            bits |= 1L << 63;
        }

        return Double.longBitsToDouble(bits);
    }

    public String read() throws IOException {
        int sz = readUnsigned();
        if (sz == 0) {
            return null;
        }
        sz--;
        char[] chars = new char[sz];
        for (int i = 0; i < sz; ++i) {
            chars[i] = (char) readUnsigned();
        }
        return new String(chars);
    }

    public byte[] readBytes() throws IOException {
        byte[] buf = new byte[readUnsigned()];
        int off = 0;
        while (true) {
            int toRead = buf.length - off;
            if (toRead == 0) {
                break;
            }
            int read = input.read(buf, off, toRead);
            if (read < 0) {
                throw new IllegalStateException();
            }
            if (read == toRead) {
                break;
            }
            off += read;
        }
        return buf;
    }
}
