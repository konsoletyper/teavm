/*
 *  Copyright 2013 Alexey Andreev.
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

import org.teavm.javascript.ni.GeneratedBy;
import org.teavm.javascript.ni.Rename;

/**
 *
 * @author Alexey Andreev
 */
public class TDouble extends TNumber {
    private double value;

    public TDouble(double value) {
        this.value = value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    @Override
    public int intValue() {
        return (int)value;
    }

    @Override
    public long longValue() {
        return (long)value;
    }

    @Override
    public float floatValue() {
        return (float)value;
    }

    public static TDouble valueOf(double d) {
        return new TDouble(d);
    }

    @Override
    @Rename("toString")
    public TString toString0() {
        return TString.wrap(new TStringBuilder().append(value).toString());
    }

    @Override
    public boolean equals(TObject other) {
        if (this == other) {
            return true;
        }
        return other instanceof TDouble && ((TDouble)other).value == value;
    }

    @GeneratedBy(DoubleNativeGenerator.class)
    public static native boolean isNaN(double v);

    @GeneratedBy(DoubleNativeGenerator.class)
    public static native boolean isInfinite(double v);
}
