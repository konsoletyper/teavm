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
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TLong extends TNumber {
    private long value;

    public TLong(long value) {
        this.value = value;
    }

    public static TLong valueOf(long value) {
        return new TLong(value);
    }

    @Override
    public int intValue() {
        return (int)value;
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

    public TString toString(long value) {
        return TString.wrap(new TStringBuilder().append(value).toString());
    }

    @Override
    @Rename("toString")
    public TString toString0() {
        return toString(value);
    }

    @Override
    public boolean equals(TObject other) {
        if (this == other) {
            return true;
        }
        return other instanceof TLong && ((TLong)other).value == value;
    }
}
