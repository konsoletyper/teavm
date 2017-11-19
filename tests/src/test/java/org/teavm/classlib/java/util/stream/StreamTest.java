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
import java.util.Comparator;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
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
        StringBuilder sb = new StringBuilder();
        Stream.of(1, 2, 3).forEach(appendNumbersTo(sb));
        assertEquals("1;2;3;", sb.toString());
    }

    @Test
    public void mapWorks() {
        StringBuilder sb = new StringBuilder();
        Stream.of(1, 2, 3).map(n -> n * n).forEach(appendNumbersTo(sb));
        assertEquals("1;4;9;", sb.toString());
    }

    @Test
    public void mapToIntWorks() {
        StringBuilder sb = new StringBuilder();
        Stream.of(1, 2, 3).mapToInt(n -> n * n).forEach(appendIntNumbersTo(sb));
        assertEquals("1;4;9;", sb.toString());
    }

    @Test
    public void mapToLongWorks() {
        StringBuilder sb = new StringBuilder();
        Stream.of(1, 2, 3).mapToLong(n -> n * n).forEach(appendLongNumbersTo(sb));
        assertEquals("1;4;9;", sb.toString());
    }

    @Test
    public void mapToDoubleWorks() {
        StringBuilder sb = new StringBuilder();
        Stream.of(1, 2, 3).mapToDouble(n -> n * n).forEach(appendDoubleNumbersTo(sb));
        assertEquals("1.0;4.0;9.0;", sb.toString());
    }

    @Test
    public void filterWorks() {
        StringBuilder sb = new StringBuilder();
        Stream.of(1, 2, 3, 4, 5, 6).filter(n -> (n & 1) == 0).forEach(appendNumbersTo(sb));
        assertEquals("2;4;6;", sb.toString());
    }

    @Test
    public void flatMapWorks() {
        StringBuilder sb = new StringBuilder();
        Stream.of(Stream.of(1, 2), Stream.of(3, 4)).flatMap(n -> n).forEach(appendNumbersTo(sb));
        assertEquals("1;2;3;4;", sb.toString());

        sb.setLength(0);
        Stream.of(Stream.of(1, 2), Stream.of(3, 4)).flatMap(n -> n).skip(1).forEach(appendNumbersTo(sb));
        assertEquals("2;3;4;", sb.toString());

        sb.setLength(0);
        Stream.of(Stream.of(1, 2), Stream.of(3, 4, 5)).flatMap(n -> n).skip(3).forEach(appendNumbersTo(sb));
        assertEquals("4;5;", sb.toString());
    }

    @Test
    public void flatMapToIntWorks() {
        StringBuilder sb = new StringBuilder();
        Stream.of(IntStream.of(1, 2), IntStream.of(3, 4)).flatMapToInt(n -> n).forEach(appendIntNumbersTo(sb));
        assertEquals("1;2;3;4;", sb.toString());

        sb.setLength(0);
        Stream.of(IntStream.of(1, 2), IntStream.of(3, 4)).flatMapToInt(n -> n).skip(1).forEach(appendIntNumbersTo(sb));
        assertEquals("2;3;4;", sb.toString());

        sb.setLength(0);
        Stream.of(IntStream.of(1, 2), IntStream.of(3, 4, 5)).flatMapToInt(n -> n).skip(3)
                .forEach(appendIntNumbersTo(sb));
        assertEquals("4;5;", sb.toString());
    }

    @Test
    public void flatMapToLongWorks() {
        StringBuilder sb = new StringBuilder();
        Stream.of(LongStream.of(1, 2), LongStream.of(3, 4)).flatMapToLong(n -> n).forEach(appendLongNumbersTo(sb));
        assertEquals("1;2;3;4;", sb.toString());

        sb.setLength(0);
        Stream.of(LongStream.of(1, 2), LongStream.of(3, 4)).flatMapToLong(n -> n).skip(1)
                .forEach(appendLongNumbersTo(sb));
        assertEquals("2;3;4;", sb.toString());

        sb.setLength(0);
        Stream.of(LongStream.of(1, 2), LongStream.of(3, 4, 5)).flatMapToLong(n -> n).skip(3)
                .forEach(appendLongNumbersTo(sb));
        assertEquals("4;5;", sb.toString());
    }

    @Test
    public void flatMapToDoubleWorks() {
        StringBuilder sb = new StringBuilder();
        Stream.of(DoubleStream.of(1, 2), DoubleStream.of(3, 4)).flatMapToDouble(n -> n)
                .forEach(appendDoubleNumbersTo(sb));
        assertEquals("1.0;2.0;3.0;4.0;", sb.toString());

        sb.setLength(0);
        Stream.of(DoubleStream.of(1, 2), DoubleStream.of(3, 4)).flatMapToDouble(n -> n).skip(1)
                .forEach(appendDoubleNumbersTo(sb));
        assertEquals("2.0;3.0;4.0;", sb.toString());

        sb.setLength(0);
        Stream.of(DoubleStream.of(1, 2), DoubleStream.of(3, 4, 5)).flatMapToDouble(n -> n).skip(3)
                .forEach(appendDoubleNumbersTo(sb));
        assertEquals("4.0;5.0;", sb.toString());
    }

    @Test
    public void skipWorks() {
        for (int i = 0; i <= 6; ++i) {
            StringBuilder sb = new StringBuilder();
            Stream.iterate(1, n -> n + 1).limit(5).skip(i).forEach(appendNumbersTo(sb));

            StringBuilder expected = new StringBuilder();
            for (int j = i; j < 5; ++j) {
                expected.append(j + 1).append(';');
            }
            assertEquals("Error skipping " + i + " elements", expected.toString(), sb.toString());
        }
    }

    @Test
    public void limitWorks() {
        for (int i = 0; i <= 3; ++i) {
            StringBuilder sb = new StringBuilder();
            Stream.iterate(1, n -> n + 1).limit(i).forEach(appendNumbersTo(sb));

            StringBuilder expected = new StringBuilder();
            for (int j = 0; j < i; ++j) {
                expected.append(j + 1).append(';');
            }
            assertEquals("Error limiting to " + i + " elements", expected.toString(), sb.toString());
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
        StringBuilder sb = new StringBuilder();
        Stream.of(5).forEach(appendNumbersTo(sb));
        assertEquals("5;", sb.toString());

        sb.setLength(0);
        Stream.of(5).skip(1).forEach(appendNumbersTo(sb));
        assertEquals("", sb.toString());

        sb.setLength(0);
        Stream.concat(Stream.of(5), Stream.of(6)).forEach(appendNumbersTo(sb));
        assertEquals("5;6;", sb.toString());
    }

    @Test
    public void sortedStream() {
        StringBuilder sb = new StringBuilder();
        Stream.of(5, 7, 1, 2, 4, 3).sorted().forEach(appendNumbersTo(sb));
        assertEquals("1;2;3;4;5;7;", sb.toString());

        sb.setLength(0);
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

    private Consumer<Integer> appendNumbersTo(StringBuilder sb) {
        return n -> sb.append(n).append(';');
    }

    private IntConsumer appendIntNumbersTo(StringBuilder sb) {
        return n -> sb.append(n).append(';');
    }

    private LongConsumer appendLongNumbersTo(StringBuilder sb) {
        return n -> sb.append(n).append(';');
    }

    private DoubleConsumer appendDoubleNumbersTo(StringBuilder sb) {
        return n -> sb.append(n).append(';');
    }
}
