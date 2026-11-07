/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.model.transformation;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class IntegerRangesTest {
    @Test
    public void unionMergesOverlappingAndAdjacent() {
        assertArrayEquals(new long[] { 1, 10 }, IntegerRanges.union(range(1, 5), range(6, 10)));
        assertArrayEquals(new long[] { 1, 10 }, IntegerRanges.union(range(1, 7), range(3, 10)));
        assertArrayEquals(new long[] { 1, 2, 4, 5 }, IntegerRanges.union(range(4, 5), range(1, 2)));
    }

    @Test
    public void unionWithFullAndEmpty() {
        assertNull(IntegerRanges.union(null, range(1, 2)));
        assertArrayEquals(new long[] { 1, 2 }, IntegerRanges.union(IntegerRanges.EMPTY, range(1, 2)));
        assertNull(IntegerRanges.union(range(Long.MIN_VALUE, 0), range(1, Long.MAX_VALUE)));
    }

    @Test
    public void numberOfRangesLimited() {
        long[] r = IntegerRanges.EMPTY;
        r = IntegerRanges.union(r, IntegerRanges.point(0));
        r = IntegerRanges.union(r, IntegerRanges.point(10));
        r = IntegerRanges.union(r, IntegerRanges.point(20));
        r = IntegerRanges.union(r, IntegerRanges.point(21 + 2));
        r = IntegerRanges.union(r, IntegerRanges.point(100));
        // Closest ranges ([20, 20] and [23, 23]) are merged
        assertArrayEquals(new long[] { 0, 0, 10, 10, 20, 23, 100, 100 }, r);
    }

    @Test
    public void mergingHandlesHugeGaps() {
        long[] r = IntegerRanges.EMPTY;
        r = IntegerRanges.union(r, IntegerRanges.point(Long.MIN_VALUE));
        r = IntegerRanges.union(r, IntegerRanges.point(-5));
        r = IntegerRanges.union(r, IntegerRanges.point(5));
        r = IntegerRanges.union(r, IntegerRanges.point(7));
        r = IntegerRanges.union(r, IntegerRanges.point(Long.MAX_VALUE));
        assertArrayEquals(new long[] { Long.MIN_VALUE, Long.MIN_VALUE, -5, -5, 5, 7,
                Long.MAX_VALUE, Long.MAX_VALUE }, r);
    }

    @Test
    public void intersect() {
        long[] a = IntegerRanges.union(range(0, 10), range(20, 30));
        assertArrayEquals(new long[] { 5, 10, 20, 25 }, IntegerRanges.intersect(a, range(5, 25)));
        assertArrayEquals(new long[0], IntegerRanges.intersect(a, range(11, 19)));
        assertArrayEquals(a, IntegerRanges.intersect(a, null));
    }

    @Test
    public void exclude() {
        assertArrayEquals(new long[] { 0, 4, 6, 10 }, IntegerRanges.exclude(range(0, 10), 5));
        assertArrayEquals(new long[] { 1, 10 }, IntegerRanges.exclude(range(0, 10), 0));
        assertArrayEquals(new long[] { Long.MIN_VALUE, -1, 1, Long.MAX_VALUE }, IntegerRanges.exclude(null, 0));
        assertArrayEquals(new long[0], IntegerRanges.exclude(IntegerRanges.point(3), 3));
    }

    @Test
    public void contains() {
        long[] a = IntegerRanges.union(range(0, 10), range(20, 30));
        assertTrue(IntegerRanges.contains(a, 0));
        assertTrue(IntegerRanges.contains(a, 25));
        assertFalse(IntegerRanges.contains(a, 15));
        assertFalse(IntegerRanges.contains(a, 31));
        assertTrue(IntegerRanges.contains(null, 15));
    }

    private static long[] range(long lo, long hi) {
        return IntegerRanges.of(lo, hi);
    }
}
