/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.vm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
public class NumericConversionTest {
    @Test
    public void floatOverflow() {
        assertEquals(2147483647, (int) (floatOne() * (1 << 30) * (1 << 3)));
        assertEquals(2147483647, (int) (floatOne() * Float.POSITIVE_INFINITY));
        assertEquals(-2147483648, (int) (-floatOne() * (1 << 30) * (1 << 3)));
        assertEquals(-2147483648, (int) (-floatOne() * Float.POSITIVE_INFINITY));
        assertEquals(0, (int) (floatOne() * Float.NaN));

        assertEquals((1L << 63) - 1, (long) (floatOne() * (1L << 60) * (1 << 5)));
        assertEquals((1L << 63) - 1, (long) (floatOne() * Float.POSITIVE_INFINITY));
        assertEquals(1L << 63, (long) (-floatOne() * (1L << 60) * (1 << 5)));
        assertEquals(1L << 63, (long) (-floatOne() * Float.POSITIVE_INFINITY));
        assertEquals(0, (long) (floatOne() * Float.NaN));
    }

    @Test
    public void doubleOverflow() {
        assertEquals(2147483647, (int) (doubleOne() * (1 << 30) * (1 << 3)));
        assertEquals(2147483647, (int) (doubleOne() * Float.POSITIVE_INFINITY));
        assertEquals(-2147483648, (int) (-doubleOne() * (1 << 30) * (1 << 3)));
        assertEquals(-2147483648, (int) (-doubleOne() * Float.POSITIVE_INFINITY));
        assertEquals(0, (int) (doubleOne() * Double.NaN));

        assertEquals((1L << 63) - 1, (long) (doubleOne() * (1L << 60) * (1 << 5)));
        assertEquals((1L << 63) - 1, (long) (doubleOne() * Double.POSITIVE_INFINITY));
        assertEquals(1L << 63, (long) (-doubleOne() * (1L << 60) * (1 << 5)));
        assertEquals(1L << 63, (long) (-doubleOne() * Double.POSITIVE_INFINITY));
        assertEquals(0, (long) (doubleOne() * Double.NaN));
    }

    @Test
    public void doubleToIntSaturates() {
        double[] values = { 1e16, -1e16, 2147483648.0, -2147483649.0, 4294967296.0, 4294967297.5,
                Double.MAX_VALUE, -Double.MAX_VALUE, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.NaN };
        int[] expected = { Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE,
                Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE,
                Integer.MIN_VALUE, 0 };
        for (int i = 0; i < values.length; ++i) {
            assertEquals(expected[i], (int) values[i], "value: " + values[i]);
            assertEquals(expected[i], (int) (float) values[i], "value: " + (float) values[i]);
        }
    }

    @Test
    public void doubleToLongSaturates() {
        double[] values = { 1e19, -1e19, 9223372036854775808.0, -9223372036854777856.0, 1e300, -1e300,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN };
        long[] expected = { Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE,
                Long.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, 0 };
        for (int i = 0; i < values.length; ++i) {
            assertEquals(expected[i], (long) values[i], "value: " + values[i]);
            assertEquals(expected[i], (long) (float) values[i], "value: " + (float) values[i]);
        }
    }

    @Test
    public void doubleToIntInRange() {
        double[] values = { 0.0, -0.0, 0.5, -0.5, 1.9, -1.9, 123456.789, -123456.789, 2147483647.0,
                2147483646.9, 2147483647.9, -2147483648.0, -2147483648.9, -2147483647.5 };
        int[] expected = { 0, 0, 0, 0, 1, -1, 123456, -123456, 2147483647, 2147483646, 2147483647,
                -2147483648, -2147483648, -2147483647 };
        for (int i = 0; i < values.length; ++i) {
            assertEquals(expected[i], (int) values[i], "value: " + values[i]);
        }
        float[] floatValues = { 0.5f, -0.5f, 1.9f, -1.9f, 16777216f, -16777216f, 2147483520f, -2147483648f };
        int[] floatExpected = { 0, 0, 1, -1, 16777216, -16777216, 2147483520, -2147483648 };
        for (int i = 0; i < floatValues.length; ++i) {
            assertEquals(floatExpected[i], (int) floatValues[i], "value: " + floatValues[i]);
        }
    }

    @Test
    public void doubleToLongInRange() {
        double[] values = { 0.0, -0.0, 0.5, -0.5, 1.9, -1.9, 1e16 + 0.0, -1e16, 4294967296.5, -4294967296.5,
                9223372036854774784.0, -9223372036854775808.0 };
        long[] expected = { 0, 0, 0, 0, 1, -1, 10000000000000000L, -10000000000000000L, 4294967296L,
                -4294967296L, 9223372036854774784L, Long.MIN_VALUE };
        for (int i = 0; i < values.length; ++i) {
            assertEquals(expected[i], (long) values[i], "value: " + values[i]);
        }
        float[] floatValues = { 0.5f, -0.5f, 1.9f, -1.9f, 1e18f, -9223372036854775808f };
        long[] floatExpected = { 0, 0, 1, -1, 999999984306749440L, Long.MIN_VALUE };
        for (int i = 0; i < floatValues.length; ++i) {
            assertEquals(floatExpected[i], (long) floatValues[i], "value: " + floatValues[i]);
        }
    }

    private float floatOne() {
        return 1;
    }

    private double doubleOne() {
        return 1;
    }
}
