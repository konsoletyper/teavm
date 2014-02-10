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

/**
 *
 * @author Alexey Andreev
 */
public class TInteger extends TNumber implements TComparable<TInteger> {
    public static final int SIZE = 32;
    public static final int MIN_VALUE = 0x80000000;
    public static final int MAX_VALUE = 0x7FFFFFFF;
    public static final TClass<TInteger> TYPE = TClass.intClass();
    private int value;

    public TInteger(int value) {
        this.value = value;
    }

    public TInteger(TString s) throws NumberFormatException {
        this(parseInt(s));
    }

    public static TString toString(int i, int radix) {
        if (radix < MIN_VALUE || radix > MAX_VALUE) {
            radix = 10;
        }
        return TString.wrap(new TAbstractStringBuilder(20).append(i, radix).toString());
    }

    @Override
    public int compareTo(TInteger other) {
        return compare(value, other.value);
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

    public static int compare(int x, int y) {
        return x > y ? 1 : x < y ? -1 : 0;
    }

    public static int parseInt(TString s, int radix) throws TNumberFormatException {
        if (radix < TCharacter.MIN_RADIX || radix > TCharacter.MAX_RADIX) {
            throw new TNumberFormatException(TString.wrap("Illegal radix: " + radix));
        }
        if (s == null || s.isEmpty()) {
            throw new TNumberFormatException(TString.wrap("String is null or empty"));
        }
        boolean negative = false;
        int index = 0;
        switch (s.charAt(0)) {
            case '-':
                negative = true;
                index = 1;
                break;
            case '+':
                index = 1;
                break;
        }
        int value = 0;
        while (index < s.length()) {
            int digit = TCharacter.digit(s.charAt(index++));
            if (digit < 0) {
                throw new TNumberFormatException(TString.wrap("String contains invalid digits: " + s));
            }
            if (digit >= radix) {
                throw new TNumberFormatException(TString.wrap("String contains digits out of radix " + radix +
                        ": " + s));
            }
            value = radix * value + digit;
            if (value < 0) {
                if (index == s.length() && value == MIN_VALUE && negative) {
                    return MIN_VALUE;
                }
                throw new TNumberFormatException(TString.wrap("The value is too big for int type: " + s));
            }
        }
        return negative ? -value : value;
    }

    public static int parseInt(TString s) throws TNumberFormatException {
        return parseInt(s, 10);
    }
}
