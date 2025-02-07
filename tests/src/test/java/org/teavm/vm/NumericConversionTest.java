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

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
public class NumericConversionTest {
    @Test
    @SkipPlatform({TestPlatform.JAVASCRIPT, TestPlatform.C})
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
    @SkipPlatform({TestPlatform.JAVASCRIPT, TestPlatform.C})
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

    private float floatOne() {
        return 1;
    }

    private double doubleOne() {
        return 1;
    }
}
