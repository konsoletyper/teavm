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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.teavm.classlib.java.util.stream.Helper.appendNumbersTo;
import static org.teavm.classlib.java.util.stream.Helper.testDoubleStream;
import static org.teavm.classlib.java.util.stream.Helper.testIntStream;
import static org.teavm.classlib.java.util.stream.Helper.testIntegerStream;
import static org.teavm.classlib.java.util.stream.Helper.testLongStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class StreamTest {
    @Test
    public void forEachWorks() {
        testIntegerStream(() -> Stream.of(1, 2, 3), 1, 2, 3);
        testIntegerStream(() -> Stream.concat(Stream.of(1), Stream.of(2, 3)), 1, 2, 3);
        testIntegerStream(() -> Stream.concat(Stream.empty(), Stream.of(1, 2, 3)), 1, 2, 3);
    }

    @Test
    public void mapWorks() {
        testIntegerStream(() -> Stream.of(1, 2, 3).map(n -> n * n), 1, 4, 9);
        testIntegerStream(() -> Stream.concat(Stream.of(1), Stream.of(2, 3)).map(n -> n * n), 1, 4, 9);
        testIntegerStream(() -> Stream.concat(Stream.empty(), Stream.of(1, 2, 3)).map(n -> n * n), 1, 4, 9);
    }

    @Test
    public void mapToIntWorks() {
        testIntStream(() -> Stream.of(1, 2, 3).mapToInt(n -> n * n), 1, 4, 9);
        testIntStream(() -> Stream.concat(Stream.of(1), Stream.of(2, 3)).mapToInt(n -> n * n), 1, 4, 9);
        testIntStream(() -> Stream.concat(Stream.empty(), Stream.of(1, 2, 3)).mapToInt(n -> n * n), 1, 4, 9);
    }

    @Test
    public void mapToLongWorks() {
        testLongStream(() -> Stream.of(1, 2, 3).mapToLong(n -> n * n), 1, 4, 9);
        testLongStream(() -> Stream.concat(Stream.of(1), Stream.of(2, 3)).mapToLong(n -> n * n), 1, 4, 9);
        testLongStream(() -> Stream.concat(Stream.empty(), Stream.of(1, 2, 3)).mapToLong(n -> n * n), 1, 4, 9);
    }

    @Test
    public void mapToDoubleWorks() {
        testDoubleStream(() -> Stream.of(1, 2, 3).mapToDouble(n -> n * n), 1, 4, 9);
        testDoubleStream(() -> Stream.concat(Stream.of(1), Stream.of(2, 3)).mapToDouble(n -> n * n), 1, 4, 9);
        testDoubleStream(() -> Stream.concat(Stream.empty(), Stream.of(1, 2, 3)).mapToDouble(n -> n * n), 1, 4, 9);
    }

    @Test
    public void filterWorks() {
        testIntegerStream(() -> Stream.of(1, 2, 3, 4, 5, 6).filter(n -> (n & 1) == 0), 2, 4, 6);
        testIntegerStream(() -> Stream.concat(Stream.of(1), Stream.of(2, 3, 4, 5, 6)).filter(n -> (n & 1) == 0),
                2, 4, 6);
        testIntegerStream(() -> Stream.concat(Stream.empty(), Stream.of(1, 2, 3, 4, 5, 6)).filter(n -> (n & 1) == 0),
                2, 4, 6);
    }

    @Test
    public void flatMapWorks() {
        testIntegerStream(() -> Stream.of(Stream.of(1, 2), Stream.of(3, 4)).flatMap(n -> n), 1, 2, 3, 4);
        testIntegerStream(() -> Stream.of(Stream.<Integer>empty(), Stream.of(1, 2), Stream.of(3, 4)).flatMap(n -> n),
                1, 2, 3, 4);
        testIntegerStream(() -> Stream.of(Stream.of(1, 2), Stream.<Integer>empty(), Stream.of(3, 4)).flatMap(n -> n),
                1, 2, 3, 4);
        testIntegerStream(() -> Stream.of(Stream.of(1, 2), Stream.of(3, 4), Stream.<Integer>empty()).flatMap(n -> n),
                1, 2, 3, 4);

        testIntegerStream(() -> Stream.of(Stream.of(1, 2), Stream.of(3, 4)).flatMap(n -> n).skip(1), 2, 3, 4);

        testIntegerStream(() -> Stream.of(Stream.of(1, 2), Stream.of(3, 4, 5)).flatMap(n -> n).skip(3), 4, 5);
    }

    @Test
    public void flatMapToIntWorks() {
        testIntStream(() -> Stream.of(IntStream.of(1, 2), IntStream.of(3, 4)).flatMapToInt(n -> n), 1, 2, 3, 4);
        testIntStream(() -> Stream.of(IntStream.empty(), IntStream.of(1, 2), IntStream.of(3, 4)).flatMapToInt(n -> n),
                1, 2, 3, 4);
        testIntStream(() -> Stream.of(IntStream.of(1, 2), IntStream.empty(), IntStream.of(3, 4)).flatMapToInt(n -> n),
                1, 2, 3, 4);
        testIntStream(() -> Stream.of(IntStream.of(1, 2), IntStream.of(3, 4), IntStream.empty()).flatMapToInt(n -> n),
                1, 2, 3, 4);

        testIntStream(() -> Stream.of(IntStream.of(1, 2), IntStream.of(3, 4)).flatMapToInt(n -> n).skip(1), 2, 3, 4);

        testIntStream(() -> Stream.of(IntStream.of(1, 2), IntStream.of(3, 4, 5)).flatMapToInt(n -> n).skip(3), 4, 5);
    }

    @Test
    public void flatMapToLongWorks() {
        testLongStream(() -> Stream.of(LongStream.of(1, 2), LongStream.of(3, 4)).flatMapToLong(n -> n), 1, 2, 3, 4);
        testLongStream(() -> Stream.of(LongStream.empty(), LongStream.of(1, 2), LongStream.of(3, 4))
                .flatMapToLong(n -> n), 1, 2, 3, 4);
        testLongStream(() -> Stream.of(LongStream.of(1, 2), LongStream.empty(), LongStream.of(3, 4))
                .flatMapToLong(n -> n), 1, 2, 3, 4);
        testLongStream(() -> Stream.of(LongStream.of(1, 2), LongStream.of(3, 4), LongStream.empty())
                .flatMapToLong(n -> n), 1, 2, 3, 4);

        testLongStream(() -> Stream.of(LongStream.of(1, 2), LongStream.of(3, 4))
                .flatMapToLong(n -> n).skip(1), 2, 3, 4);

        testLongStream(() -> Stream.of(LongStream.of(1, 2), LongStream.of(3, 4, 5))
                .flatMapToLong(n -> n).skip(3), 4, 5);
    }

    @Test
    public void flatMapToDoubleWorks() {
        testDoubleStream(() -> Stream.of(DoubleStream.of(1, 2), DoubleStream.of(3, 4)).flatMapToDouble(n -> n),
                1, 2, 3, 4);
        testDoubleStream(() -> Stream.of(DoubleStream.empty(), DoubleStream.of(1, 2), DoubleStream.of(3, 4))
                .flatMapToDouble(n -> n), 1, 2, 3, 4);
        testDoubleStream(() -> Stream.of(DoubleStream.of(1, 2), DoubleStream.empty(), DoubleStream.of(3, 4))
                .flatMapToDouble(n -> n), 1, 2, 3, 4);
        testDoubleStream(() -> Stream.of(DoubleStream.of(1, 2), DoubleStream.of(3, 4), DoubleStream.empty())
                .flatMapToDouble(n -> n), 1, 2, 3, 4);

        testDoubleStream(() -> Stream.of(DoubleStream.of(1, 2), DoubleStream.of(3, 4))
                .flatMapToDouble(n -> n).skip(1), 2, 3, 4);

        testDoubleStream(() -> Stream.of(DoubleStream.of(1, 2), DoubleStream.of(3, 4, 5))
                .flatMapToDouble(n -> n).skip(3), 4, 5);
    }

    @Test
    public void skipWorks() {
        for (int i = 0; i <= 5; ++i) {
            int index = i;
            Integer[] expected = new Integer[5 - i];
            for (int j = i; j < 5; ++j) {
                expected[j - i] = j + 1;
            }
            testIntegerStream(() -> Stream.iterate(1, n -> n + 1).limit(5).skip(index), expected);
            testIntegerStream(() -> Stream.concat(Stream.of(1), Stream.iterate(2, n -> n + 1).limit(4))
                    .skip(index), expected);
            testIntegerStream(() -> Stream.concat(Stream.empty(), Stream.iterate(1, n -> n + 1).limit(5))
                    .skip(index), expected);
        }
    }

    @Test
    public void limitWorks() {
        for (int i = 0; i <= 3; ++i) {
            int index = i;
            Integer[] expected = new Integer[i];
            for (int j = 0; j < expected.length; ++j) {
                expected[j] = j + 1;
            }

            testIntegerStream(() -> Stream.iterate(1, n -> n + 1).limit(index), expected);
        }
    }

    @Test
    public void countWorks() {
        assertEquals(4, Stream.of(2, 3, 2, 3).count());
        assertEquals(3, Stream.of(2, 3, 2, 3).limit(3).count());
        assertEquals(4, Stream.of(2, 3, 2, 3).limit(5).count());
        assertEquals(0, Stream.of(2, 3, 2, 3).skip(5).count());
        assertEquals(3, Stream.of(2, 3, 2, 3).skip(1).count());

        assertEquals(2, Stream.of(2, 3, 2, 3).filter(n -> n == 2).count());

        assertEquals(10, Stream.generate(() -> 1).limit(10).count());
        assertEquals(10, Stream.generate(() -> 1).limit(10).count());

        assertEquals(4, Stream.of(Stream.of(1, 2), Stream.of(3, 4)).flatMap(n -> n).count());
    }

    @Test
    public void distinctWorks() {
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(2, 3, 2, 3).distinct().toArray(Integer[]::new));
        assertArrayEquals(new Integer[] { 3, 2 }, Stream.of(2, 3, 2, 3).skip(1).distinct().toArray(Integer[]::new));
        assertArrayEquals(new Integer[] { 2, 3 }, Stream.of(2, 3, 2, 3).limit(2).distinct().toArray(Integer[]::new));
        assertArrayEquals(new Integer[] { 2, 3, 4, 1 }, Stream.of(2, 2, 3, 2, 4, 3, 1).distinct()
                .toArray(Integer[]::new));
    }

    @Test
    public void findFirstWorks() {
        assertEquals(2, Stream.of(2, 3).findFirst().get().intValue());
        assertEquals(3, Stream.of(2, 3).skip(1).findFirst().get().intValue());
        assertEquals(4, Stream.of(2, 3, 4, 5).filter(n -> n > 3).findFirst().get().intValue());
        assertFalse(Stream.of(2, 3, 4, 5).filter(n -> n > 10).findFirst().isPresent());
        assertFalse(Stream.of(2, 3).skip(3).findFirst().isPresent());
        assertEquals(20, Stream.of(2, 3).map(n -> n * 10).findFirst().get().intValue());
    }

    @Test
    public void concatWorks() {
        StringBuilder sb = new StringBuilder();
        Stream.concat(Stream.of(1, 2), Stream.of(3, 4)).forEach(appendNumbersTo(sb));
        assertEquals("1;2;3;4;", sb.toString());

        sb.setLength(0);
        Stream.concat(Stream.of(1, 2), Stream.of(3, 4)).skip(1).forEach(appendNumbersTo(sb));
        assertEquals("2;3;4;", sb.toString());

        sb.setLength(0);
        Stream.concat(Stream.of(1, 2), Stream.of(3, 4, 5)).skip(3).forEach(appendNumbersTo(sb));
        assertEquals("4;5;", sb.toString());
    }

    @Test
    public void peekWorks() {
        StringBuilder sb = new StringBuilder();
        Stream.of(1, 2, 3).peek(appendNumbersTo(sb)).map(n -> n + 10).forEach(appendNumbersTo(sb));
        assertEquals("1;11;2;12;3;13;", sb.toString());
    }

    @Test
    public void reduceWorks() {
        assertEquals(10, Stream.of(1, 2, 3, 4).reduce(0, (a, b) -> a + b).intValue());
        assertEquals(0, Stream.of(1, 2, 3, 4).skip(4).reduce(0, (a, b) -> a + b).intValue());
        assertEquals(720, Stream.iterate(1, n -> n + 1).limit(6).reduce(1, (a, b) -> a * b).intValue());

        assertEquals(9, Stream.of(1, 2, 3, 4).skip(1).reduce((a, b) -> a + b).get().intValue());
        assertFalse(Stream.of(1, 2, 3, 4).skip(4).reduce((a, b) -> a + b).isPresent());
    }

    @Test
    public void streamOfOneElement() {
        testIntegerStream(() -> Stream.of(5), 5);
        testIntegerStream(() -> Stream.of(5).skip(1));
        testIntegerStream(() -> Stream.concat(Stream.of(5), Stream.of(6)), 5, 6);
        testIntegerStream(() -> Stream.concat(Stream.empty(), Stream.of(5)), 5);
        testIntegerStream(() -> Stream.concat(Stream.of(5), Stream.empty()), 5);
    }

    @Test
    public void sortedStream() {
        testIntegerStream(() -> Stream.of(5, 7, 1, 2, 4, 3).sorted(), 1, 2, 3, 4, 5, 7);
        testIntegerStream(() -> Stream.concat(Stream.of(5), Stream.of(7, 1, 2, 4, 3)).sorted(), 1, 2, 3, 4, 5, 7);
        testIntegerStream(() -> Stream.concat(Stream.empty(), Stream.of(5, 7, 1, 2, 4, 3)).sorted(), 1, 2, 3, 4, 5, 7);

        StringBuilder sb = new StringBuilder();
        Stream.of(2, 3, 1).peek(appendNumbersTo(sb)).sorted().limit(2).map(n -> n + 10).forEach(appendNumbersTo(sb));
        assertEquals("2;3;1;11;12;", sb.toString());

        sb.setLength(0);
        Stream.of(2, 3, 1).peek(appendNumbersTo(sb)).sorted().limit(0).forEach(appendNumbersTo(sb));
        assertEquals("2;3;1;", sb.toString());

        sb.setLength(0);
        Stream.of("qq", "aaa", "z").sorted(Comparator.comparing(x -> x.length())).forEach(s -> sb.append(s));
        assertEquals("zqqaaa", sb.toString());
    }

    @Test
    public void minMax() {
        assertEquals(3, Stream.of(5, 7, 3, 6).min(Comparator.comparing(x -> x)).get().intValue());
        assertEquals(7, Stream.of(5, 7, 3, 6).max(Comparator.comparing(x -> x)).get().intValue());

        assertEquals(3, Stream.of(5, 7, 3, 6).skip(1).min(Comparator.comparing(x -> x)).get().intValue());
        assertEquals(7, Stream.of(5, 7, 3, 6).skip(1).max(Comparator.comparing(x -> x)).get().intValue());

        assertEquals(3, Stream.of(5, 7, 3, 6).skip(2).min(Comparator.comparing(x -> x)).get().intValue());
        assertEquals(6, Stream.of(5, 7, 3, 6).skip(2).max(Comparator.comparing(x -> x)).get().intValue());

        assertEquals(6, Stream.of(5, 7, 3, 6).skip(3).min(Comparator.comparing(x -> x)).get().intValue());
        assertEquals(6, Stream.of(5, 7, 3, 6).skip(3).max(Comparator.comparing(x -> x)).get().intValue());

        assertFalse(Stream.<Integer>empty().min(Comparator.comparing(x -> x)).isPresent());
        assertFalse(Stream.<Integer>empty().max(Comparator.comparing(x -> x)).isPresent());

        IntPair first = new IntPair(0, 0);
        IntPair second = new IntPair(0, 1);

        Comparator<IntPair> comp = Comparator.comparingInt(p -> p.number);
        assertEquals(0, Stream.of(first, second).max(comp).get().id);
        assertEquals(0, Stream.of(first, second).min(comp).get().id);
        assertSame(Collections.max(Arrays.asList(first, second), comp), Stream.of(first, second).max(comp).get());
        assertSame(Collections.min(Arrays.asList(first, second), comp), Stream.of(first, second).min(comp).get());
    }

    private static class IntPair {
        private final int number;
        private final int id;

        private IntPair(int number, int id) {
            this.number = number;
            this.id = id;
        }
    }

    @Test
    public void allNoneAny() {
        assertTrue(Stream.of(5, 7, 3, 6).anyMatch(n -> n == 7));
        assertFalse(Stream.of(5, 7, 3, 6).anyMatch(n -> n == 11));
        assertFalse(Stream.empty().anyMatch(n -> true));

        assertFalse(Stream.of(5, 7, 3, 6).noneMatch(n -> n == 7));
        assertTrue(Stream.of(5, 7, 3, 6).noneMatch(n -> n == 11));
        assertTrue(Stream.empty().noneMatch(n -> true));

        assertTrue(Stream.of(5, 7, 3, 6).allMatch(n -> n < 10));
        assertFalse(Stream.of(5, 7, 3, 6).allMatch(n -> n < 6));
        assertTrue(Stream.empty().allMatch(n -> false));
    }

    @Test
    public void closeFlatMap() {
        int[] closed = new int[3];

        Stream.of(
                Stream.of(1, 2).onClose(() -> closed[0]++),
                Stream.of(3, 4).onClose(() -> closed[1]++)
        ).flatMap(Function.identity()).onClose(() -> closed[2]++).skip(10).close();

        assertArrayEquals(new int[] { 0, 0, 1 }, closed);
    }

    @Test
    public void closeMap() {
        int[] closed = new int[2];
        Stream.of(1, 2).onClose(() -> closed[0]++).map(x -> x * 2).onClose(() -> closed[1]++).close();
        assertArrayEquals(new int[] { 1, 1 }, closed);
    }

    @Test
    public void closeConcat() {
        int[] closed = new int[3];

        Stream.concat(
                Stream.of(1, 2).onClose(() -> closed[0]++),
                Stream.of(3, 4).onClose(() -> closed[1]++)
        ).onClose(() -> closed[2]++).close();

        assertArrayEquals(new int[] { 1, 1, 1 }, closed);
    }

    @Test
    public void iterator() {
        StringBuilder sb = new StringBuilder();
        Iterator<Integer> iterator = Stream.of(1, 2, 3).iterator();
        while (iterator.hasNext()) {
            sb.append(iterator.next()).append(';');
        }
        assertEquals("1;2;3;", sb.toString());

        sb.setLength(0);
        iterator = Stream.of(1, 2, 3).iterator();
        iterator.forEachRemaining(appendNumbersTo(sb));
        assertEquals("1;2;3;", sb.toString());
    }

    @Test
    public void spliterator() {
        StringBuilder sb = new StringBuilder();
        Spliterator<Integer> spliterator = Stream.of(1, 2, 3).spliterator();
        while (spliterator.tryAdvance(appendNumbersTo(sb))) {
            // continue
        }
        assertEquals("1;2;3;", sb.toString());

        sb.setLength(0);
        spliterator = Stream.of(1, 2, 3).spliterator();
        spliterator.forEachRemaining(appendNumbersTo(sb));
        assertEquals("1;2;3;", sb.toString());
    }
}