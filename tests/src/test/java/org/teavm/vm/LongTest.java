/*
 *  Copyright 2016 Alexey Andreev.
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
import org.teavm.junit.EachTestCompiledSeparately;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
@EachTestCompiledSeparately
public class LongTest {
    @Test
    public void longIntegersMultipied() {
        long a = id(1199747L);
        long b = id(1062911L);
        assertEquals(1275224283517L, a * b);
        assertEquals(-1275224283517L, a * -b);
        a = id(229767376164L);
        b = id(907271478890L);
        assertEquals(-5267604004427634456L, a * b);
        assertEquals(5267604004427634456L, a * -b);
    }

    @Test
    public void longIntegersDivided() {
        long a = id(12752242835177213L);
        long b = id(1062912L);
        assertEquals(11997458712L, a / b);
        assertEquals(-11997458712L, a / -b);
    }

    @Test
    public void longAdditionWorks() {
        long a = id(1134903170);
        long b = id(1836311903);
        assertEquals(2971215073L, a + b);
    }

    @Test
    public void smallLongDivision() {
        long a = id(-1);
        long b = 3;
        assertEquals(0, a / b);
    }

    @Test
    public void shiftByConstant() {
        long[] values = { 0x123456789ABCDEF0L, -0x123456789ABCDEF0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 1L };
        long[][] expected = {
            { 2623536934927580640L, 5575015949918011392L, -7296712173873528832L, 0L,
                655884233731895160L, 305419896L, 0L,
                655884233731895160L, 305419896L, 0L },
            { -2623536934927580640L, -5575015949918011392L, 7296712173873528832L, 0L,
                -655884233731895160L, -305419897L, -1L,
                8567487803122880648L, 3989547399L, 1L },
            { -2L, -2147483648L, -4294967296L, Long.MIN_VALUE, -1L, -1L, -1L,
                Long.MAX_VALUE, 4294967295L, 1L },
            { 0L, 0L, 0L, 0L, -4611686018427387904L, -2147483648L, -1L,
                4611686018427387904L, 2147483648L, 1L },
            { -2L, -2147483648L, -4294967296L, Long.MIN_VALUE, 4611686018427387903L, 2147483647L, 0L,
                4611686018427387903L, 2147483647L, 0L },
            { 2L, 2147483648L, 4294967296L, Long.MIN_VALUE, 0L, 0L, 0L, 0L, 0L, 0L }
        };
        for (var i = 0; i < values.length; ++i) {
            var v = values[i];
            var e = expected[i];
            assertEquals(e[0], v << 1);
            assertEquals(e[1], v << 31);
            assertEquals(e[2], v << 32);
            assertEquals(e[3], v << 63);
            assertEquals(e[4], v >> 1);
            assertEquals(e[5], v >> 32);
            assertEquals(e[6], v >> 63);
            assertEquals(e[7], v >>> 1);
            assertEquals(e[8], v >>> 32);
            assertEquals(e[9], v >>> 63);

            assertEquals(v, v << 0);
            assertEquals(v, v >> 0);
            assertEquals(v, v >>> 0);
            assertEquals(v << 1, v << 65);
            assertEquals(v >> 1, v >> 65);
            assertEquals(v >>> 1, v >>> 65);
            assertEquals(v << 63, v << -1);
            assertEquals(v >> 63, v >> -1);
            assertEquals(v >>> 63, v >>> -1);

            for (var shift : new int[] { 0, 1, 31, 32, 33, 63, 64, 65, -1 }) {
                var s = idInt(shift);
                assertEquals(v << shift, v << s);
                assertEquals(v >> shift, v >> s);
                assertEquals(v >>> shift, v >>> s);
            }
        }
    }

    @Test
    public void longToInt() {
        long[] values = { 0x123456789ABCDEF0L, -0x123456789ABCDEF0L, -1L, Long.MIN_VALUE, Long.MAX_VALUE, 1L };
        int[] lo = { -1698898192, 1698898192, -1, 0, -1, 1 };
        int[] hi = { 305419896, -305419897, -1, -2147483648, 2147483647, 0 };
        int[] hash = { -2004318072, -2004318057, 0, -2147483648, -2147483648, 1 };
        for (var i = 0; i < values.length; ++i) {
            assertEquals(lo[i], (int) values[i]);
            assertEquals(hi[i], (int) (values[i] >> 32));
            assertEquals(hi[i], (int) (values[i] >>> 32));
            assertEquals(hash[i], Long.hashCode(values[i]));
        }
    }

    @Test
    public void longToDouble() {
        assertEquals(1.3117684674637903E18, (double) id(0x123456789ABCDEF0L));
        assertEquals(-1.3117684674637903E18, (double) id(-0x123456789ABCDEF0L));
        assertEquals(-1.0, (double) id(-1));
        assertEquals(-9223372036854775808.0, (double) id(Long.MIN_VALUE));
        assertEquals(9223372036854775808.0, (double) id(Long.MAX_VALUE));
        assertEquals(4294967295.0, (double) id(0xFFFFFFFFL));
        assertEquals(-4294967295.0, (double) id(-0xFFFFFFFFL));

        // rounding
        assertEquals(9007199254740992.0, (double) id(9007199254740993L));
        assertEquals(-9007199254740996.0, (double) id(-9007199254740995L));
        assertEquals(9223372036854774784.0, (double) id(0x7FFFFFFFFFFFFDFFL));
        assertEquals(9223372036854775808.0, (double) id(0x7FFFFFFFFFFFFE01L));
        assertEquals(-9223372036854775808.0, (double) id(-0x7FFFFFFFFFFFFE01L));

        assertEquals(1.5e9f, (float) id(1500000000L));
    }

    @Test
    public void longConstants() {
        long[] values = { 0L, 1L, -1L, 2147483647L, 2147483648L, -2147483648L, -2147483649L, 4294967295L,
                4294967296L, Long.MAX_VALUE, Long.MIN_VALUE, 0x123456789ABCDEF0L, -0x123456789ABCDEF0L };
        int[] lo = { 0, 1, -1, 2147483647, -2147483648, -2147483648, 2147483647, -1, 0, -1, 0,
                -1698898192, 1698898192 };
        int[] hi = { 0, 0, -1, 0, 0, -1, -1, 0, 1, 2147483647, -2147483648, 305419896, -305419897 };
        for (var i = 0; i < values.length; ++i) {
            assertEquals(lo[i], (int) values[i]);
            assertEquals(hi[i], (int) (values[i] >> 32));
        }
        assertEquals(-1L, id(Long.MAX_VALUE) + Long.MIN_VALUE);
        assertEquals(Long.MIN_VALUE, -id(Long.MIN_VALUE));
        assertEquals(1L, id(-0x123456789ABCDEF0L) + 0x123456789ABCDEF1L);
    }

    @Test
    public void doubleToLong() {
        assertEquals(1500000000000000000L, (long) idDouble(1.5e18));
        assertEquals(-1500000000000000000L, (long) idDouble(-1.5e18));
        assertEquals(4294967296L, (long) idDouble(4294967296.7));
        assertEquals(-4294967297L, (long) idDouble(-4294967297.3));
        assertEquals(-1L, (long) idDouble(-1.9));
        assertEquals(0L, (long) idDouble(-0.9));
        assertEquals(9223372036854774784L, (long) idDouble(9223372036854774784.0));
        assertEquals(Long.MIN_VALUE, (long) idDouble(-9223372036854775808.0));
    }

    private static int idInt(int value) {
        return value;
    }

    private static double idDouble(double value) {
        return value;
    }

    private static long id(long value) {
        return value;
    }
}
