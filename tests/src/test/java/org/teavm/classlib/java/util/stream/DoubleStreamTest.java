/*
 *  Copyright 2017 Alexey Andreev.
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.teavm.classlib.java.util.stream.Helper.appendDoubleNumbersTo;
import static org.teavm.classlib.java.util.stream.Helper.testDoubleStream;
import static org.teavm.classlib.java.util.stream.Helper.testIntStream;
import static org.teavm.classlib.java.util.stream.Helper.testIntegerStream;
import static org.teavm.classlib.java.util.stream.Helper.testLongStream;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.stream.DoubleStream;
import java.util.stream.LongStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class DoubleStreamTest {
    @Test
    public void forEachWorks() {
        testDoubleStream(() -> DoubleStream.of(1, 2, 3), 1, 2, 3);
        testDoubleStream(() -> DoubleStream.concat(DoubleStream.of(1), DoubleStream.of(2, 3)), 1, 2, 3);
        testDoubleStream(() -> DoubleStream.concat(DoubleStream.empty(), DoubleStream.of(1, 2, 3)), 1, 2, 3);
    }

    @Test
    public void mapWorks() {
        testDoubleStream(() -> DoubleStream.of(1, 2, 3).map(n -> n * n), 1, 4, 9);
        testDoubleStream(() -> DoubleStream.concat(DoubleStream.of(1), DoubleStream.of(2, 3))
                .map(n -> n * n), 1, 4, 9);
        testDoubleStream(() -> DoubleStream.concat(DoubleStream.empty(), DoubleStream.of(1, 2, 3))
                .map(n -> n * n), 1, 4, 9);
    }

    @Test
    public void mapToObjWorks() {
        testIntegerStream(() -> DoubleStream.of(1, 2, 3).mapToObj(n -> (int) (n * n)), 1, 4, 9);
        testIntegerStream(() -> DoubleStream.concat(DoubleStream.of(1), DoubleStream.of(2, 3))
                .mapToObj(n -> (int) (n * n)), 1, 4, 9);
        testIntegerStream(() -> DoubleStream.concat(DoubleStream.empty(), DoubleStream.of(1, 2, 3))
                .mapToObj(n -> (int) (n * n)), 1, 4, 9);
    }

    @Test
    public void mapToIntWorks() {
        testIntStream(() -> DoubleStream.of(1, 2, 3).mapToInt(n -> (int) (n * n)), 1, 4, 9);
        testIntStream(() -> DoubleStream.concat(DoubleStream.of(1), DoubleStream.of(2, 3))
                .mapToInt(n -> (int) (n * n)), 1, 4, 9);
        testIntStream(() -> DoubleStream.concat(DoubleStream.empty(), DoubleStream.of(1, 2, 3))
                .mapToInt(n -> (int) (n * n)), 1, 4, 9);
    }

    @Test
    public void mapToLongWorks() {
        testLongStream(() -> DoubleStream.of(1, 2, 3).mapToLong(n -> (long) (n * n)), 1, 4, 9);
        testLongStream(() -> DoubleStream.concat(DoubleStream.of(1), DoubleStream.of(2, 3))
                .mapToLong(n -> (long) (n * n)), 1, 4, 9);
        testLongStream(() -> DoubleStream.concat(DoubleStream.empty(), DoubleStream.of(1, 2, 3))
                .mapToLong(n -> (long) (n * n)), 1, 4, 9);
    }

    @Test
    public void filterWorks() {
        testDoubleStream(() -> DoubleStream.of(1, 2, 3, 4, 5, 6).filter(n -> (n % 2) == 0), 2, 4, 6);
        testDoubleStream(() -> DoubleStream.concat(DoubleStream.of(1), DoubleStream.of(2, 3, 4, 5, 6))
                .filter(n -> ((int) n & 1) == 0), 2, 4, 6);
        testDoubleStream(() -> DoubleStream.concat(DoubleStream.empty(), DoubleStream.of(1, 2, 3, 4, 5, 6))
                .filter(n -> ((int) n & 1) == 0), 2, 4, 6);
    }

    @Test
    public void flatMapWorks() {
        testDoubleStream(() -> DoubleStream.of(1, 3).flatMap(n -> DoubleStream.of(n, n + 1)), 1, 2, 3, 4);
        testDoubleStream(() -> DoubleStream.of(1, 3).flatMap(n -> DoubleStream.of(n, n + 1)).skip(1), 2, 3, 4);
        testDoubleStream(() -> DoubleStream.of(1, 4).flatMap(n -> DoubleStream.of(n, n + 1, n + 2)).skip(3), 4, 5, 6);

        testDoubleStream(() -> DoubleStream.of(1, 3, 100)
                .flatMap(n -> n < 100 ? DoubleStream.of(n, n + 1) : DoubleStream.empty()), 1, 2, 3, 4);
        testDoubleStream(() -> DoubleStream.of(100, 1, 3)
                .flatMap(n -> n < 100 ? DoubleStream.of(n, n + 1) : DoubleStream.empty()), 1, 2, 3, 4);
    }

    @Test
    public void skipWorks() {
        for (int i = 0; i <= 5; ++i) {
            int index = i;
            double[] expected = new double[5 - i];
            for (int j = i; j < 5; ++j) {
                expected[j - i] = j + 1;
            }
            testDoubleStream(() -> DoubleStream.iterate(1, n -> n + 1).limit(5).skip(index), expected);
            testDoubleStream(() -> DoubleStream.concat(DoubleStream.of(1), DoubleStream.iterate(2, n -> n + 1)
                    .limit(4)).skip(index), expected);
            testDoubleStream(() -> DoubleStream.concat(DoubleStream.empty(), DoubleStream.iterate(1, n -> n + 1)
                    .limit(5)).skip(index), expected);
        }
    }

    @Test
    public void limitWorks() {
        for (int i = 0; i <= 3; ++i) {
            int index = i;
            long[] expected = new long[i];
            for (int j = 0; j < expected.length; ++j) {
                expected[j] = j + 1;
            }

            testLongStream(() -> LongStream.iterate(1, n -> n + 1).limit(index), expected);
        }
    }

    @Test
    public void countWorks() {
        assertEquals(4, DoubleStream.of(2, 3, 2, 3).count());
        assertEquals(3, DoubleStream.of(2, 3, 2, 3).limit(3).count());
        assertEquals(4, DoubleStream.of(2, 3, 2, 3).limit(5).count());
        assertEquals(0, DoubleStream.of(2, 3, 2, 3).skip(5).count());
        assertEquals(3, DoubleStream.of(2, 3, 2, 3).skip(1).count());

        assertEquals(2, DoubleStream.of(2, 3, 2, 3).filter(n -> n == 2).count());

        assertEquals(10, DoubleStream.generate(() -> 1).limit(10).count());
        assertEquals(10, DoubleStream.generate(() -> 1).limit(10).count());

        assertEquals(4, DoubleStream.of(1, 3).flatMap(n -> DoubleStream.of(n, n + 1)).count());
    }

    @Test
    public void distinctWorks() {
        StringBuilder sb = new StringBuilder();
        DoubleStream.of(2, 3, 2, 3).distinct().forEach(appendDoubleNumbersTo(sb));
        assertEquals("2.0;3.0;", sb.toString());

        sb.setLength(0);
        DoubleStream.of(2, 3, 2, 3).skip(1).distinct().forEach(appendDoubleNumbersTo(sb));
        assertEquals("3.0;2.0;", sb.toString());

        sb.setLength(0);
        DoubleStream.of(2, 3, 2, 3).limit(2).distinct().forEach(appendDoubleNumbersTo(sb));
        assertEquals("2.0;3.0;", sb.toString());

        sb.setLength(0);
        DoubleStream.of(2, 2, 3, 2, 4, 3, 1).distinct().forEach(appendDoubleNumbersTo(sb));
        assertEquals("2.0;3.0;4.0;1.0;", sb.toString());
    }

    @Test
    public void findFirstWorks() {
        assertEquals(2, DoubleStream.of(2, 3).findFirst().getAsDouble(), 0.001);
        assertEquals(3, DoubleStream.of(2, 3).skip(1).findFirst().getAsDouble(), 0.001);
        assertEquals(4, DoubleStream.of(2, 3, 4, 5).filter(n -> n > 3).findFirst().getAsDouble(), 0.001);
        assertFalse(DoubleStream.of(2, 3, 4, 5).filter(n -> n > 10).findFirst().isPresent());
        assertFalse(DoubleStream.of(2, 3).skip(3).findFirst().isPresent());
        assertEquals(20, DoubleStream.of(2, 3).map(n -> n * 10).findFirst().getAsDouble(), 0.001);
    }

    @Test
    public void concatWorks() {
        StringBuilder sb = new StringBuilder();
        DoubleStream.concat(DoubleStream.of(1, 2), DoubleStream.of(3, 4)).forEach(appendDoubleNumbersTo(sb));
        assertEquals("1.0;2.0;3.0;4.0;", sb.toString());

        sb.setLength(0);
        DoubleStream.concat(DoubleStream.of(1, 2), DoubleStream.of(3, 4)).skip(1).forEach(appendDoubleNumbersTo(sb));
        assertEquals("2.0;3.0;4.0;", sb.toString());

        sb.setLength(0);
        DoubleStream.concat(DoubleStream.of(1, 2), DoubleStream.of(3, 4, 5)).skip(3).forEach(appendDoubleNumbersTo(sb));
        assertEquals("4.0;5.0;", sb.toString());
    }

    @Test
    public void peekWorks() {
        StringBuilder sb = new StringBuilder();
        DoubleStream.of(1, 2, 3).peek(appendDoubleNumbersTo(sb)).map(n -> n + 10).forEach(appendDoubleNumbersTo(sb));
        assertEquals("1.0;11.0;2.0;12.0;3.0;13.0;", sb.toString());
    }

    @Test
    public void reduceWorks() {
        assertEquals(10, DoubleStream.of(1, 2, 3, 4).reduce(0, (a, b) -> a + b), 0.001);
        assertEquals(0, DoubleStream.of(1, 2, 3, 4).skip(4).reduce(0, (a, b) -> a + b), 0.001);
        assertEquals(720, DoubleStream.iterate(1, n -> n + 1).limit(6).reduce(1, (a, b) -> a * b), 0.001);

        assertEquals(9, DoubleStream.of(1, 2, 3, 4).skip(1).reduce((a, b) -> a + b).getAsDouble(), 0.001);
        assertFalse(DoubleStream.of(1, 2, 3, 4).skip(4).reduce((a, b) -> a + b).isPresent());
    }

    @Test
    public void streamOfOneElement() {
        StringBuilder sb = new StringBuilder();
        DoubleStream.of(5).forEach(appendDoubleNumbersTo(sb));
        assertEquals("5.0;", sb.toString());

        sb.setLength(0);
        DoubleStream.of(5).skip(1).forEach(appendDoubleNumbersTo(sb));
        assertEquals("", sb.toString());

        sb.setLength(0);
        DoubleStream.concat(DoubleStream.of(5), DoubleStream.of(6)).forEach(appendDoubleNumbersTo(sb));
        assertEquals("5.0;6.0;", sb.toString());
    }

    @Test
    public void sortedStream() {
        StringBuilder sb = new StringBuilder();
        DoubleStream.of(5, 7, 1, 2, 4, 3).sorted().forEach(appendDoubleNumbersTo(sb));
        assertEquals("1.0;2.0;3.0;4.0;5.0;7.0;", sb.toString());

        sb.setLength(0);
        DoubleStream.of(2, 3, 1).peek(appendDoubleNumbersTo(sb)).sorted().limit(2).map(n -> n + 10)
                .forEach(appendDoubleNumbersTo(sb));
        assertEquals("2.0;3.0;1.0;11.0;12.0;", sb.toString());

        sb.setLength(0);
        DoubleStream.of(2, 3, 1).peek(appendDoubleNumbersTo(sb)).sorted().limit(0).forEach(appendDoubleNumbersTo(sb));
        assertEquals("2.0;3.0;1.0;", sb.toString());
    }

    @Test
    public void minMax() {
        assertEquals(3, DoubleStream.of(5, 7, 3, 6).min().getAsDouble(), 0.001);
        assertEquals(7, DoubleStream.of(5, 7, 3, 6).max().getAsDouble(), 0.001);

        assertEquals(3, DoubleStream.of(5, 7, 3, 6).skip(1).min().getAsDouble(), 0.001);
        assertEquals(7, DoubleStream.of(5, 7, 3, 6).skip(1).max().getAsDouble(), 0.001);

        assertEquals(3, DoubleStream.of(5, 7, 3, 6).skip(2).min().getAsDouble(), 0.001);
        assertEquals(6, DoubleStream.of(5, 7, 3, 6).skip(2).max().getAsDouble(), 0.001);

        assertEquals(6, DoubleStream.of(5, 7, 3, 6).skip(3).min().getAsDouble(), 0.001);
        assertEquals(6, DoubleStream.of(5, 7, 3, 6).skip(3).max().getAsDouble(), 0.001);

        assertFalse(DoubleStream.empty().min().isPresent());
        assertFalse(DoubleStream.empty().max().isPresent());
    }

    @Test
    public void allNoneAny() {
        assertTrue(DoubleStream.of(5, 7, 3, 6).anyMatch(n -> n == 7));
        assertFalse(DoubleStream.of(5, 7, 3, 6).anyMatch(n -> n == 11));
        assertFalse(DoubleStream.empty().anyMatch(n -> true));

        assertFalse(DoubleStream.of(5, 7, 3, 6).noneMatch(n -> n == 7));
        assertTrue(DoubleStream.of(5, 7, 3, 6).noneMatch(n -> n == 11));
        assertTrue(DoubleStream.empty().noneMatch(n -> true));

        assertTrue(DoubleStream.of(5, 7, 3, 6).allMatch(n -> n < 10));
        assertFalse(DoubleStream.of(5, 7, 3, 6).allMatch(n -> n < 6));
        assertTrue(DoubleStream.empty().allMatch(n -> false));
    }

    @Test
    public void closeFlatMap() {
        int[] closed = new int[3];

        DoubleStream.of(0, 1)
                .flatMap(n -> DoubleStream.of(n, n + 1).onClose(() -> closed[(int) n]++))
                .onClose(() -> closed[2]++)
                .skip(10).close();

        assertArrayEquals(new int[] { 0, 0, 1 }, closed);
    }

    @Test
    public void closeMap() {
        int[] closed = new int[2];
        DoubleStream.of(1, 2).onClose(() -> closed[0]++).map(x -> x * 2).onClose(() -> closed[1]++).close();
        assertArrayEquals(new int[] { 1, 1 }, closed);
    }

    @Test
    public void closeConcat() {
        int[] closed = new int[3];

        DoubleStream.concat(
                DoubleStream.of(1, 2).onClose(() -> closed[0]++),
                DoubleStream.of(3, 4).onClose(() -> closed[1]++)
        ).onClose(() -> closed[2]++).close();

        assertArrayEquals(new int[] { 1, 1, 1 }, closed);
    }

    @Test
    public void iterator() {
        StringBuilder sb = new StringBuilder();
        PrimitiveIterator.OfDouble iterator = DoubleStream.of(1, 2, 3).iterator();
        while (iterator.hasNext()) {
            sb.append(iterator.next()).append(';');
        }
        assertEquals("1.0;2.0;3.0;", sb.toString());

        sb.setLength(0);
        iterator = DoubleStream.of(1, 2, 3).iterator();
        iterator.forEachRemaining(appendDoubleNumbersTo(sb));
        assertEquals("1.0;2.0;3.0;", sb.toString());
    }

    @Test
    public void spliterator() {
        StringBuilder sb = new StringBuilder();
        Spliterator.OfDouble spliterator = DoubleStream.of(1, 2, 3).spliterator();
        while (spliterator.tryAdvance(appendDoubleNumbersTo(sb))) {
            // continue
        }
        assertEquals("1.0;2.0;3.0;", sb.toString());

        sb.setLength(0);
        spliterator = DoubleStream.of(1, 2, 3).spliterator();
        spliterator.forEachRemaining(appendDoubleNumbersTo(sb));
        assertEquals("1.0;2.0;3.0;", sb.toString());
    }

    @Test
    public void average() {
        assertEquals(2.5, DoubleStream.of(1, 2, 3, 4).average().getAsDouble(), 0.001);
        assertFalse(DoubleStream.empty().average().isPresent());
    }
}
