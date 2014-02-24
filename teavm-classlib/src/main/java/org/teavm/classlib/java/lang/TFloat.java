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
public class TFloat extends TNumber {
    private float value;

    public TFloat(float value) {
        this.value = value;
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
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    public static TFloat valueOf(float d) {
        return new TFloat(d);
    }

    public static TString toString(float d) {
        return TString.wrap(new TStringBuilder().append(d).toString());
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
        return other instanceof TFloat && ((TFloat)other).value == value;
    }

    @GeneratedBy(FloatNativeGenerator.class)
    public static native boolean isNaN(float v);

    @GeneratedBy(FloatNativeGenerator.class)
    public static native boolean isInfinite(float v);
}
