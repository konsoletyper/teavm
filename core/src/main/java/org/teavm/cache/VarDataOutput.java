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

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

public class VarDataOutput implements Closeable {
    private static final int DATA = 0x7F;
    private static final int NEXT = 0x80;
    private OutputStream output;

    public VarDataOutput(OutputStream output) {
        this.output = output;
    }

    public void writeUnsigned(int value) throws IOException {
        while ((value & DATA) != value) {
            output.write((value & DATA) | NEXT);
            value >>>= 7;
        }
        output.write(value);
    }

    public void writeSigned(int value) throws IOException {
        writeUnsigned(value < 0 ? ((~value) << 1) | 1 : value << 1);
    }

    public void writeUnsigned(long value) throws IOException {
        while ((value & DATA) != value) {
            output.write((int) (value & DATA) | NEXT);
            value >>>= 7;
        }
        output.write((int) value);
    }

    public void writeSigned(long value) throws IOException {
        writeUnsigned(value < 0 ? ((~value) << 1) | 1 : value << 1);
    }

    public void writeFloat(float value) throws IOException {
        if (value == 0) {
            writeUnsigned(0);
            return;
        }
        int bits = Float.floatToRawIntBits(value);
        boolean sign = (bits & (1 << 31)) != 0;
        int exponent = (bits >> 23) & ((1 << 8) - 1);
        int mantissa = bits & ((1 << 23) - 1);
        if (sign) {
            mantissa |= 1 << 23;
        }
        exponent -= 127;
        writeUnsigned(1 + (exponent > 0 ? exponent << 1 : 1 | (-exponent << 1)));
        writeUnsigned(Integer.reverse(mantissa << 8));
    }

    public void writeDouble(double value) throws IOException {
        if (value == 0) {
            writeUnsigned(0);
            return;
        }
        long bits = Double.doubleToRawLongBits(value);
        boolean sign = (bits & (1L << 63)) != 0;
        int exponent = (int) (bits >> 52) & ((1 << 11) - 1);
        long mantissa = bits & ((1L << 52) - 1);
        if (sign) {
            mantissa |= 1L << 52;
        }
        exponent -= 1023;
        writeUnsigned(1 + (exponent > 0 ? exponent << 1 : 1 | (-exponent << 1)));
        writeUnsigned(Long.reverse(mantissa << 11));
    }

    public void write(String s) throws IOException {
        if (s == null) {
            writeUnsigned(0);
            return;
        }
        writeUnsigned(s.length() + 1);
        for (int i = 0; i < s.length(); ++i) {
            writeUnsigned(s.charAt(i));
        }
    }

    public void writeBytes(byte[] data) throws IOException {
        writeUnsigned(data.length);
        output.write(data);
    }

    @Override
    public void close() throws IOException {
        output.close();
    }
}
