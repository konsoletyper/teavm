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

public class TShort extends TNumber implements TComparable<TShort> {
    public static final short MIN_VALUE = -32768;
    public static final short MAX_VALUE = 32767;
    public static final Class<Short> TYPE = short.class;
    public static final int SIZE = 16;
    private short value;

    public TShort(short value) {
        this.value = value;
    }

    public TShort(TString s) throws TNumberFormatException {
        this(parseShort(s));
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
    public short shortValue() {
        return value;
    }

    public static TShort valueOf(short value) {
        return new TShort(value);
    }

    public static String toString(short value) {
        return new StringBuilder().append(value).toString();
    }

    @Override
    public String toString() {
        return toString(value);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TShort && ((TShort) other).value == value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    public static int compare(short a, short b) {
        return a - b;
    }

    @Override
    public int compareTo(TShort other) {
        return compare(value, other.value);
    }

    public static short parseShort(TString s, int radix) throws TNumberFormatException {
        int value = TInteger.parseInt(s, radix);
        if (value < MIN_VALUE || value > MAX_VALUE) {
            throw new TNumberFormatException();
        }
        return (short) value;
    }

    public static short parseShort(TString s) throws TNumberFormatException {
        return parseShort(s, 10);
    }

    public static TShort valueOf(TString s, int radix) throws TNumberFormatException {
        return valueOf(parseShort(s, radix));
    }

    public static TShort valueOf(TString s) throws TNumberFormatException {
        return valueOf(parseShort(s));
    }

    public static TShort decode(TString s) throws TNumberFormatException {
        int value = TInteger.decode(s).intValue();
        if (value < MIN_VALUE || value > MAX_VALUE) {
            throw new TNumberFormatException();
        }
        return valueOf((short) value);
    }

    public static short reverseBytes(short i) {
        return (short) ((i << 8) | (i >>> 8));
    }
}
