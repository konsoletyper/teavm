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
    public static final int MIN_VALUE = (2 << (SIZE - 1));
    public static final int MAX_VALUE = (2 << (SIZE - 1)) - 1;
    public static final TClass<TInteger> TYPE = TClass.integerClass();
    private int value;

    public TInteger(int value) {
        this.value = value;
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

    /*public static int parseInt(TString s, int radix) throws TNumberFormatException {
        return 0;
    }*/
}
