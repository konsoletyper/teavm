/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

public class TByte extends TNumber implements TComparable<TByte> {
    public static final byte MIN_VALUE = -128;
    public static final byte MAX_VALUE = 127;
    public static final Class<Byte> TYPE = byte.class;
    public static final int SIZE = 8;
    private byte value;

    public TByte(byte value) {
        this.value = value;
    }

    public TByte(TString value) {
        this.value = parseByte(value, 10);
    }

    @Override
    public int intValue() {
        return value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    @Override
    public byte byteValue() {
        return value;
    }

    public static TByte valueOf(byte value) {
        // TODO: add caching
        return new TByte(value);
    }

    public static String toString(byte value) {
        return new StringBuilder().append(value).toString();
    }

    @Override
    public String toString() {
        return toString(value);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TByte && ((TByte) other).value == value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    public static int compare(byte a, byte b) {
        return a - b;
    }

    @Override
    public int compareTo(TByte other) {
        return compare(value, other.value);
    }

    public static byte parseByte(TString s) throws TNumberFormatException {
        return parseByte(s, 10);
    }

    public static byte parseByte(TString s, int radix) throws TNumberFormatException {
        int value = TInteger.parseInt(s, radix);
        if (value < MIN_VALUE || value >= MAX_VALUE) {
            throw new TNumberFormatException();
        }
        return (byte) value;
    }

    public static TByte valueOf(TString s, int radix) throws TNumberFormatException {
        return valueOf(parseByte(s, radix));
    }

    public static TByte valueOf(TString s) throws TNumberFormatException {
        return valueOf(parseByte(s));
    }

    public static TByte decode(TString nm) throws TNumberFormatException {
        TInteger value = TInteger.decode(nm);
        if (value.intValue() < MIN_VALUE || value.intValue() >= MAX_VALUE) {
            throw new TNumberFormatException();
        }
        return TByte.valueOf((byte) value.intValue());
    }
}
