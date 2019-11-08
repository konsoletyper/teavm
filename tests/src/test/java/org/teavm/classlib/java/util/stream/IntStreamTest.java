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
import static org.teavm.classlib.java.util.stream.Helper.appendIntNumbersTo;
import static org.teavm.classlib.java.util.stream.Helper.testDoubleStream;
import static org.teavm.classlib.java.util.stream.Helper.testIntStream;
import static org.teavm.classlib.java.util.stream.Helper.testIntegerStream;
import static org.teavm.classlib.java.util.stream.Helper.testLongStream;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class IntStreamTest {
    @Test
    public void forEachWorks() {
        testIntStream(() -> IntStream.of(1, 2, 3), 1, 2, 3);
        testIntStream(() -> IntStream.concat(IntStream.of(1), IntStream.of(2, 3)), 1, 2, 3);
        testIntStream(() -> IntStream.concat(IntStream.empty(), IntStream.of(1, 2, 3)), 1, 2, 3);
    }

    @Test
    public void mapWorks() {
        testIntStream(() -> IntStream.of(1, 2, 3).map(n -> n * n), 1, 4, 9);
        testIntStream(() -> IntStream.concat(IntStream.of(1), IntStream.of(2, 3)).map(n -> n * n), 1, 4, 9);
        testIntStream(() -> IntStream.concat(IntStream.empty(), IntStream.of(1, 2, 3)).map(n -> n * n), 1, 4, 9);
    }

    @Test
    public void mapToObjWorks() {
        testIntegerStream(() -> IntStream.of(1, 2, 3).mapToObj(n -> n * n), 1, 4, 9);
        testIntegerStream(() -> IntStream.concat(IntStream.of(1), IntStream.of(2, 3)).mapToObj(n -> n * n), 1, 4, 9);
        testIntegerStream(() -> IntStream.concat(IntStream.empty(), IntStream.of(1, 2, 3))
                .mapToObj(n -> n * n), 1, 4, 9);
    }

    @Test
    public void mapToLongWorks() {
        testLongStream(() -> IntStream.of(1, 2, 3).mapToLong(n -> n * n), 1, 4, 9);
        testLongStream(() -> IntStream.concat(IntStream.of(1), IntStream.of(2, 3)).mapToLong(n -> n * n), 1, 4, 9);
        testLongStream(() -> IntStream.concat(IntStream.empty(), IntStream.of(1, 2, 3)).mapToLong(n -> n * n), 1, 4, 9);
    }

    @Test
    public void mapToDoubleWorks() {
        testDoubleStream(() -> IntStream.of(1, 2, 3).mapToDouble(n -> n * n), 1, 4, 9);
        testDoubleStream(() -> IntStream.concat(IntStream.of(1), IntStream.of(2, 3)).mapToDouble(n -> n * n), 1, 4, 9);
        testDoubleStream(() -> IntStream.concat(IntStream.empty(), IntStream.of(1, 2, 3)).mapToDouble(n -> n * n),
                1, 4, 9);
    }

    @Test
    public void filterWorks() {
        testIntStream(() -> IntStream.of(1, 2, 3, 4, 5, 6).filter(n -> (n & 1) == 0), 2, 4, 6);
        testIntStream(() -> IntStream.concat(IntStream.of(1), IntStream.of(2, 3, 4, 5, 6))
                .filter(n -> (n & 1) == 0), 2, 4, 6);
        testIntStream(() -> IntStream.concat(IntStream.empty(), IntStream.of(1, 2, 3, 4, 5, 6))
                .filter(n -> (n & 1) == 0), 2, 4, 6);
    }

    @Test
    public void flatMapWorks() {
        testIntStream(() -> IntStream.of(1, 3).flatMap(n -> IntStream.of(n, n + 1)), 1, 2, 3, 4);
        testIntStream(() -> IntStream.of(1, 3).flatMap(n -> IntStream.of(n, n + 1)).skip(1), 2, 3, 4);
        testIntStream(() -> IntStream.of(1, 4).flatMap(n -> IntStream.of(n, n + 1, n + 2)).skip(3), 4, 5, 6);

        testIntStream(() -> IntStream.of(1, 3, 100)
                .flatMap(n -> n < 100 ? IntStream.of(n, n + 1) : IntStream.empty()), 1, 2, 3, 4);
        testIntStream(() -> IntStream.of(100, 1, 3)
                .flatMap(n -> n < 100 ? IntStream.of(n, n + 1) : IntStream.empty()), 1, 2, 3, 4);
    }

    @Test
    public void skipWorks() {
        for (int i = 0; i <= 5; ++i) {
            int index = i;
            int[] expected = new int[5 - i];
            for (int j = i; j < 5; ++j) {
                expected[j - i] = j + 1;
            }
            testIntStream(() -> IntStream.iterate(1, n -> n + 1).limit(5).skip(index), expected);
            testIntStream(() -> IntStream.concat(IntStream.of(1), IntStream.iterate(2, n -> n + 1).limit(4))
                    .skip(index), expected);
            testIntStream(() -> IntStream.concat(IntStream.empty(), IntStream.iterate(1, n -> n + 1).limit(5))
                    .skip(index), expected);
        }
    }

    @Test
    public void limitWorks() {
        for (int i = 0; i <= 3; ++i) {
            int index = i;
            int[] expected = new int[i];
            for (int j = 0; j < expected.length; ++j) {
                expected[j] = j + 1;
            }

            testIntStream(() -> IntStream.iterate(1, n -> n + 1).limit(index), expected);
        }
    }

    @Test
    public void countWorks() {
        assertEquals(4, IntStream.of(2, 3, 2, 3).count());
        assertEquals(3, IntStream.of(2, 3, 2, 3).limit(3).count());
        assertEquals(4, IntStream.of(2, 3, 2, 3).limit(5).count());
        assertEquals(0, IntStream.of(2, 3, 2, 3).skip(5).count());
        assertEquals(3, IntStream.of(2, 3, 2, 3).skip(1).count());

        assertEquals(2, IntStream.of(2, 3, 2, 3).filter(n -> n == 2).count());

        assertEquals(10, IntStream.generate(() -> 1).limit(10).count());
        assertEquals(10, IntStream.generate(() -> 1).limit(10).count());

        assertEquals(4, IntStream.of(1, 3).flatMap(n -> IntStream.of(n, n + 1)).count());
    }

    @Test
    public void distinctWorks() {
        assertArrayEquals(new int[] { 2, 3 }, IntStream.of(2, 3, 2, 3).distinct().toArray());
        assertArrayEquals(new int[] { 3, 2 }, IntStream.of(2, 3, 2, 3).skip(1).distinct().toArray());
        assertArrayEquals(new int[] { 2, 3 }, IntStream.of(2, 3, 2, 3).limit(2).distinct().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 1 }, IntStream.of(2, 2, 3, 2, 4, 3, 1).distinct().toArray());
    }

    @Test
    public void findFirstWorks() {
        assertEquals(2, IntStream.of(2, 3).findFirst().getAsInt());
        assertEquals(3, IntStream.of(2, 3).skip(1).findFirst().getAsInt());
        assertEquals(4, IntStream.of(2, 3, 4, 5).filter(n -> n > 3).findFirst().getAsInt());
        assertFalse(IntStream.of(2, 3, 4, 5).filter(n -> n > 10).findFirst().isPresent());
        assertFalse(IntStream.of(2, 3).skip(3).findFirst().isPresent());
        assertEquals(20, IntStream.of(2, 3).map(n -> n * 10).findFirst().getAsInt());
    }

    @Test
    public void concatWorks() {
        StringBuilder sb = new StringBuilder();
        IntStream.concat(IntStream.of(1, 2), IntStream.of(3, 4)).forEach(appendIntNumbersTo(sb));
        assertEquals("1;2;3;4;", sb.toString());

        sb.setLength(0);
        IntStream.concat(IntStream.of(1, 2), IntStream.of(3, 4)).skip(1).forEach(appendIntNumbersTo(sb));
        assertEquals("2;3;4;", sb.toString());

        sb.setLength(0);
        IntStream.concat(IntStream.of(1, 2), IntStream.of(3, 4, 5)).skip(3).forEach(appendIntNumbersTo(sb));
        assertEquals("4;5;", sb.toString());
    }

    @Test
    public void peekWorks() {
        StringBuilder sb = new StringBuilder();
        IntStream.of(1, 2, 3).peek(appendIntNumbersTo(sb)).map(n -> n + 10).forEach(appendIntNumbersTo(sb));
        assertEquals("1;11;2;12;3;13;", sb.toString());
    }

    @Test
    public void reduceWorks() {
        assertEquals(10, IntStream.of(1, 2, 3, 4).reduce(0, (a, b) -> a + b));
        assertEquals(0, IntStream.of(1, 2, 3, 4).skip(4).reduce(0, (a, b) -> a + b));
        assertEquals(720, IntStream.iterate(1, n -> n + 1).limit(6).reduce(1, (a, b) -> a * b));

        assertEquals(9, IntStream.of(1, 2, 3, 4).skip(1).reduce((a, b) -> a + b).getAsInt());
        assertFalse(IntStream.of(1, 2, 3, 4).skip(4).reduce((a, b) -> a + b).isPresent());
    }

    @Test
    public void streamOfOneElement() {
        StringBuilder sb = new StringBuilder();
        IntStream.of(5).forEach(appendIntNumbersTo(sb));
        assertEquals("5;", sb.toString());

        sb.setLength(0);
        IntStream.of(5).skip(1).forEach(appendIntNumbersTo(sb));
        assertEquals("", sb.toString());

        sb.setLength(0);
        IntStream.concat(IntStream.of(5), IntStream.of(6)).forEach(appendIntNumbersTo(sb));
        assertEquals("5;6;", sb.toString());
    }

    @Test
    public void sortedStream() {
        StringBuilder sb = new StringBuilder();
        IntStream.of(5, 7, 1, 2, 4, 3).sorted().forEach(appendIntNumbersTo(sb));
        assertEquals("1;2;3;4;5;7;", sb.toString());

        sb.setLength(0);
        IntStream.of(2, 3, 1).peek(appendIntNumbersTo(sb)).sorted().limit(2).map(n -> n + 10)
                .forEach(appendIntNumbersTo(sb));
        assertEquals("2;3;1;11;12;", sb.toString());

        sb.setLength(0);
        IntStream.of(2, 3, 1).peek(appendIntNumbersTo(sb)).sorted().limit(0).forEach(appendIntNumbersTo(sb));
        assertEquals("2;3;1;", sb.toString());
    }

    @Test
    public void minMax() {
        assertEquals(3, IntStream.of(5, 7, 3, 6).min().getAsInt());
        assertEquals(7, IntStream.of(5, 7, 3, 6).max().getAsInt());

        assertEquals(3, IntStream.of(5, 7, 3, 6).skip(1).min().getAsInt());
        assertEquals(7, IntStream.of(5, 7, 3, 6).skip(1).max().getAsInt());

        assertEquals(3, IntStream.of(5, 7, 3, 6).skip(2).min().getAsInt());
        assertEquals(6, IntStream.of(5, 7, 3, 6).skip(2).max().getAsInt());

        assertEquals(6, IntStream.of(5, 7, 3, 6).skip(3).min().getAsInt());
        assertEquals(6, IntStream.of(5, 7, 3, 6).skip(3).max().getAsInt());

        assertFalse(IntStream.empty().min().isPresent());
        assertFalse(IntStream.empty().max().isPresent());
    }

    @Test
    public void allNoneAny() {
        assertTrue(IntStream.of(5, 7, 3, 6).anyMatch(n -> n == 7));
        assertFalse(IntStream.of(5, 7, 3, 6).anyMatch(n -> n == 11));
        assertFalse(IntStream.empty().anyMatch(n -> true));

        assertFalse(IntStream.of(5, 7, 3, 6).noneMatch(n -> n == 7));
        assertTrue(IntStream.of(5, 7, 3, 6).noneMatch(n -> n == 11));
        assertTrue(IntStream.empty().noneMatch(n -> true));

        assertTrue(IntStream.of(5, 7, 3, 6).allMatch(n -> n < 10));
        assertFalse(IntStream.of(5, 7, 3, 6).allMatch(n -> n < 6));
        assertTrue(IntStream.empty().allMatch(n -> false));
    }

    @Test
    public void closeFlatMap() {
        int[] closed = new int[3];

        IntStream.of(0, 1)
                .flatMap(n -> IntStream.of(n, n + 1).onClose(() -> closed[n]++))
                .onClose(() -> closed[2]++)
                .skip(10).close();

        assertArrayEquals(new int[] { 0, 0, 1 }, closed);
    }

    @Test
    public void closeMap() {
        int[] closed = new int[2];
        IntStream.of(1, 2).onClose(() -> closed[0]++).map(x -> x * 2).onClose(() -> closed[1]++).close();
        assertArrayEquals(new int[] { 1, 1 }, closed);
    }

    @Test
    public void closeConcat() {
        int[] closed = new int[3];

        IntStream.concat(
                IntStream.of(1, 2).onClose(() -> closed[0]++),
                IntStream.of(3, 4).onClose(() -> closed[1]++)
        ).onClose(() -> closed[2]++).close();

        assertArrayEquals(new int[] { 1, 1, 1 }, closed);
    }

    @Test
    public void iterator() {
        StringBuilder sb = new StringBuilder();
        PrimitiveIterator.OfInt iterator = IntStream.of(1, 2, 3).iterator();
        while (iterator.hasNext()) {
            sb.append(iterator.next()).append(';');
        }
        assertEquals("1;2;3;", sb.toString());

        sb.setLength(0);
        iterator = IntStream.of(1, 2, 3).iterator();
        iterator.forEachRemaining(appendIntNumbersTo(sb));
        assertEquals("1;2;3;", sb.toString());
    }

    @Test
    public void spliterator() {
        StringBuilder sb = new StringBuilder();
        Spliterator.OfInt spliterator = IntStream.of(1, 2, 3).spliterator();
        while (spliterator.tryAdvance(appendIntNumbersTo(sb))) {
            // continue
        }
        assertEquals("1;2;3;", sb.toString());

        sb.setLength(0);
        spliterator = IntStream.of(1, 2, 3).spliterator();
        spliterator.forEachRemaining(appendIntNumbersTo(sb));
        assertEquals("1;2;3;", sb.toString());
    }

    @Test
    public void average() {
        assertEquals(2.5, IntStream.of(1, 2, 3, 4).average().getAsDouble(), 0.001);
        assertFalse(IntStream.empty().average().isPresent());
    }

    @Test
    public void range() {
        StringBuilder sb = new StringBuilder();
        IntStream.range(1, 4).forEach(appendIntNumbersTo(sb));
        assertEquals("1;2;3;", sb.toString());

        sb.setLength(0);
        IntStream.rangeClosed(1, 4).forEach(appendIntNumbersTo(sb));
        assertEquals("1;2;3;4;", sb.toString());
    }
}
