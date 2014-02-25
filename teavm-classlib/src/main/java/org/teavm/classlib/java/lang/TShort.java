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

import org.teavm.javascript.ni.Rename;

/**
 *
 * @author Alexey Andreev
 */
public class TShort extends TNumber implements TComparable<TShort> {
    private short value;

    public TShort(short value) {
        this.value = value;
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

    public static TString toString(short value) {
        return TString.wrap(new StringBuilder().append(value).toString());
    }

    @Override
    @Rename("toString")
    public TString toString0() {
        return toString(value);
    }

    @Override
    public boolean equals(TObject other) {
        return other instanceof TShort && ((TShort)other).value == value;
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
}
