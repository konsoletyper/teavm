/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.classlib.java.util.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.carrotsearch.hppc.DoubleArrayList;
import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.cursors.DoubleCursor;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.cursors.LongCursor;
import java.util.Iterator;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

final class Helper {
    private Helper() {
    }

    static void testIntegerStream(Supplier<Stream<Integer>> streamSupplier, Integer... expected) {
        StringBuilder sb = new StringBuilder();
        for (Integer e : expected) {
            sb.append(e).append(';');
        }
        String expectedText = sb.toString();

        sb.setLength(0);
        streamSupplier.get().forEach(appendNumbersTo(sb));
        assertEquals(expectedText, sb.toString());

        sb.setLength(0);
        Iterator<Integer> iter = streamSupplier.get().iterator();
        while (iter.hasNext()) {
            sb.append(iter.next()).append(';');
        }
        assertEquals(expectedText, sb.toString());

        sb.setLength(0);
        List<Integer> list = streamSupplier.get().collect(Collectors.toList());
        for (Integer e : list) {
            sb.append(e).append(';');
        }
        assertEquals(expectedText, sb.toString());

        assertEquals(expected.length, streamSupplier.get().count());

        if (expected.length > 0) {
            int max = expected[0];
            for (Integer e : expected) {
                max = Math.max(max, e);
            }
            int notInCollection = max + 1;
            int inCollection = expected[0];
            assertTrue(streamSupplier.get().allMatch(e -> e < notInCollection));
            assertFalse(streamSupplier.get().allMatch(e -> e < inCollection));
            assertTrue(streamSupplier.get().anyMatch(e -> e == inCollection));
            assertFalse(streamSupplier.get().anyMatch(e -> e == notInCollection));
        } else {
            assertTrue(streamSupplier.get().allMatch(e -> false));
            assertTrue(streamSupplier.get().allMatch(e -> true));
            assertFalse(streamSupplier.get().anyMatch(e -> true));
            assertFalse(streamSupplier.get().anyMatch(e -> false));
        }
    }

    static void testIntStream(Supplier<IntStream> streamSupplier, int... expected) {
        StringBuilder sb = new StringBuilder();
        for (int e : expected) {
            sb.append(e).append(';');
        }
        String expectedText = sb.toString();

        sb.setLength(0);
        streamSupplier.get().forEach(appendIntNumbersTo(sb));
        assertEquals(expectedText, sb.toString());

        sb.setLength(0);
        PrimitiveIterator.OfInt iter = streamSupplier.get().iterator();
        while (iter.hasNext()) {
            sb.append(iter.next()).append(';');
        }
        assertEquals(expectedText, sb.toString());

        sb.setLength(0);
        IntArrayList list = streamSupplier.get().collect(IntArrayList::new, IntArrayList::add, IntArrayList::addAll);
        for (IntCursor cursor : list) {
            sb.append(cursor.value).append(';');
        }
        assertEquals(expectedText, sb.toString());

        assertEquals(expected.length, streamSupplier.get().count());

        if (expected.length > 0) {
            int max = expected[0];
            for (int e : expected) {
                max = Math.max(max, e);
            }
            int notInCollection = max + 1;
            int inCollection = expected[0];
            assertTrue(streamSupplier.get().allMatch(e -> e < notInCollection));
            assertFalse(streamSupplier.get().allMatch(e -> e < inCollection));
            assertTrue(streamSupplier.get().anyMatch(e -> e == inCollection));
            assertFalse(streamSupplier.get().anyMatch(e -> e == notInCollection));
        } else {
            assertTrue(streamSupplier.get().allMatch(e -> false));
            assertTrue(streamSupplier.get().allMatch(e -> true));
            assertFalse(streamSupplier.get().anyMatch(e -> true));
            assertFalse(streamSupplier.get().anyMatch(e -> false));
        }
    }

    static void testLongStream(Supplier<LongStream> streamSupplier, long... expected) {
        StringBuilder sb = new StringBuilder();
        for (long e : expected) {
            sb.append(e).append(';');
        }
        String expectedText = sb.toString();

        sb.setLength(0);
        streamSupplier.get().forEach(appendLongNumbersTo(sb));
        assertEquals(expectedText, sb.toString());

        sb.setLength(0);
        PrimitiveIterator.OfLong iter = streamSupplier.get().iterator();
        while (iter.hasNext()) {
            sb.append(iter.next()).append(';');
        }
        assertEquals(expectedText, sb.toString());

        sb.setLength(0);
        LongArrayList list = streamSupplier.get().collect(LongArrayList::new, LongArrayList::add,
                LongArrayList::addAll);
        for (LongCursor cursor : list) {
            sb.append(cursor.value).append(';');
        }
        assertEquals(expectedText, sb.toString());

        assertEquals(expected.length, streamSupplier.get().count());

        if (expected.length > 0) {
            long max = expected[0];
            for (long e : expected) {
                max = Math.max(max, e);
            }
            long notInCollection = max + 1;
            long inCollection = expected[0];
            assertTrue(streamSupplier.get().allMatch(e -> e < notInCollection));
            assertFalse(streamSupplier.get().allMatch(e -> e < inCollection));
            assertTrue(streamSupplier.get().anyMatch(e -> e == inCollection));
            assertFalse(streamSupplier.get().anyMatch(e -> e == notInCollection));
        } else {
            assertTrue(streamSupplier.get().allMatch(e -> false));
            assertTrue(streamSupplier.get().allMatch(e -> true));
            assertFalse(streamSupplier.get().anyMatch(e -> true));
            assertFalse(streamSupplier.get().anyMatch(e -> false));
        }
    }

    static void testDoubleStream(Supplier<DoubleStream> streamSupplier, double... expected) {
        StringBuilder sb = new StringBuilder();
        for (double e : expected) {
            sb.append(e).append(';');
        }
        String expectedText = sb.toString();

        sb.setLength(0);
        streamSupplier.get().forEach(appendDoubleNumbersTo(sb));
        assertEquals(expectedText, sb.toString());

        sb.setLength(0);
        PrimitiveIterator.OfDouble iter = streamSupplier.get().iterator();
        while (iter.hasNext()) {
            sb.append(iter.next()).append(';');
        }
        assertEquals(expectedText, sb.toString());

        sb.setLength(0);
        DoubleArrayList list = streamSupplier.get().collect(DoubleArrayList::new, DoubleArrayList::add,
                DoubleArrayList::addAll);
        for (DoubleCursor cursor : list) {
            sb.append(cursor.value).append(';');
        }
        assertEquals(expectedText, sb.toString());

        assertEquals(expected.length, streamSupplier.get().count());

        if (expected.length > 0) {
            double max = expected[0];
            for (double e : expected) {
                max = Math.max(max, e);
            }
            double notInCollection = max + 1;
            double inCollection = expected[0];
            assertTrue(streamSupplier.get().allMatch(e -> e < notInCollection));
            assertFalse(streamSupplier.get().allMatch(e -> e < inCollection));
            assertTrue(streamSupplier.get().anyMatch(e -> e == inCollection));
            assertFalse(streamSupplier.get().anyMatch(e -> e == notInCollection));
        } else {
            assertTrue(streamSupplier.get().allMatch(e -> false));
            assertTrue(streamSupplier.get().allMatch(e -> true));
            assertFalse(streamSupplier.get().anyMatch(e -> true));
            assertFalse(streamSupplier.get().anyMatch(e -> false));
        }
    }

    static Consumer<Integer> appendNumbersTo(StringBuilder sb) {
        return n -> sb.append(n).append(';');
    }

    static IntConsumer appendIntNumbersTo(StringBuilder sb) {
        return n -> sb.append(n).append(';');
    }

    static LongConsumer appendLongNumbersTo(StringBuilder sb) {
        return n -> sb.append(n).append(';');
    }

    static DoubleConsumer appendDoubleNumbersTo(StringBuilder sb) {
        return n -> sb.append(n).append(';');
    }
}
