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
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class DoubleStreamTest {
    @Test
    public void forEachWorks() {
        StringBuilder sb = new StringBuilder();
        DoubleStream.of(1, 2, 3).forEach(appendNumbersTo(sb));
        assertEquals("1.0;2.0;3.0;", sb.toString());
    }

    @Test
    public void mapWorks() {
        StringBuilder sb = new StringBuilder();
        DoubleStream.of(1, 2, 3).map(n -> n * n).forEach(appendNumbersTo(sb));
        assertEquals("1.0;4.0;9.0;", sb.toString());
    }

    @Test
    public void mapToObjWorks() {
        String result = DoubleStream.of(1, 2, 3).mapToObj(n -> String.valueOf(n * n)).collect(Collectors.joining(";"));
        assertEquals("1.0;4.0;9.0", result);
    }

    @Test
    public void mapToIntWorks() {
        StringBuilder sb = new StringBuilder();
        DoubleStream.of(1, 2, 3).mapToInt(n -> (int) (n * n)).forEach(appendIntNumbersTo(sb));
        assertEquals("1;4;9;", sb.toString());
    }

    @Test
    public void mapToLongWorks() {
        StringBuilder sb = new StringBuilder();
        DoubleStream.of(1, 2, 3).mapToLong(n -> (long) (n * n)).forEach(appendLongNumbersTo(sb));
        assertEquals("1;4;9;", sb.toString());
    }

    @Test
    public void filterWorks() {
        StringBuilder sb = new StringBuilder();
        DoubleStream.of(1, 2, 3, 4, 5, 6).filter(n -> (n % 2) == 0).forEach(appendNumbersTo(sb));
        assertEquals("2.0;4.0;6.0;", sb.toString());
    }

    @Test
    public void flatMapWorks() {
        StringBuilder sb = new StringBuilder();
        DoubleStream.of(1, 3).flatMap(n -> DoubleStream.of(n, n + 1)).forEach(appendNumbersTo(sb));
        assertEquals("1.0;2.0;3.0;4.0;", sb.toString());

        sb.setLength(0);
        DoubleStream.of(1, 3).flatMap(n -> DoubleStream.of(n, n + 1)).skip(1).forEach(appendNumbersTo(sb));
        assertEquals("2.0;3.0;4.0;", sb.toString());

        sb.setLength(0);
        DoubleStream.of(1, 4).flatMap(n -> DoubleStream.of(n, n + 1, n + 2)).skip(4).forEach(appendNumbersTo(sb));
        assertEquals("5.0;6.0;", sb.toString());
    }

    @Test
    public void skipWorks() {
        for (int i = 0; i <= 6; ++i) {
            StringBuilder sb = new StringBuilder();
            DoubleStream.iterate(1, n -> n + 1).limit(5).skip(i).forEach(appendNumbersTo(sb));

            StringBuilder expected = new StringBuilder();
            for (int j = i; j < 5; ++j) {
                expected.append((double) j + 1).append(';');
            }
            assertEquals("Error skipping " + i + " elements", expected.toString(), sb.toString());
        }
    }

    @Test
    public void limitWorks() {
        for (int i = 0; i <= 3; ++i) {
            StringBuilder sb = new StringBuilder();
            DoubleStream.iterate(1, n -> n + 1).limit(i).forEach(appendNumbersTo(sb));

            StringBuilder expected = new StringBuilder();
            for (int j = 0; j < i; ++j) {
                expected.append((double) j + 1).append(';');
            }
            assertEquals("Error limiting to " + i + " elements", expected.toString(), sb.toString());
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
        DoubleStream.of(2, 3, 2, 3).distinct().forEach(appendNumbersTo(sb));
        assertEquals("2.0;3.0;", sb.toString());

        sb.setLength(0);
        DoubleStream.of(2, 3, 2, 3).skip(1).distinct().forEach(appendNumbersTo(sb));
        assertEquals("3.0;2.0;", sb.toString());

        sb.setLength(0);
        DoubleStream.of(2, 3, 2, 3).limit(2).distinct().forEach(appendNumbersTo(sb));
        assertEquals("2.0;3.0;", sb.toString());

        sb.setLength(0);
        DoubleStream.of(2, 2, 3, 2, 4, 3, 1).distinct().forEach(appendNumbersTo(sb));
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
        DoubleStream.concat(DoubleStream.of(1, 2), DoubleStream.of(3, 4)).forEach(appendNumbersTo(sb));
        assertEquals("1.0;2.0;3.0;4.0;", sb.toString());

        sb.setLength(0);
        DoubleStream.concat(DoubleStream.of(1, 2), DoubleStream.of(3, 4)).skip(1).forEach(appendNumbersTo(sb));
        assertEquals("2.0;3.0;4.0;", sb.toString());

        sb.setLength(0);
        DoubleStream.concat(DoubleStream.of(1, 2), DoubleStream.of(3, 4, 5)).skip(3).forEach(appendNumbersTo(sb));
        assertEquals("4.0;5.0;", sb.toString());
    }

    @Test
    public void peekWorks() {
        StringBuilder sb = new StringBuilder();
        DoubleStream.of(1, 2, 3).peek(appendNumbersTo(sb)).map(n -> n + 10).forEach(appendNumbersTo(sb));
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
        DoubleStream.of(5).forEach(appendNumbersTo(sb));
        assertEquals("5.0;", sb.toString());

        sb.setLength(0);
        DoubleStream.of(5).skip(1).forEach(appendNumbersTo(sb));
        assertEquals("", sb.toString());

        sb.setLength(0);
        DoubleStream.concat(DoubleStream.of(5), DoubleStream.of(6)).forEach(appendNumbersTo(sb));
        assertEquals("5.0;6.0;", sb.toString());
    }

    @Test
    public void sortedStream() {
        StringBuilder sb = new StringBuilder();
        DoubleStream.of(5, 7, 1, 2, 4, 3).sorted().forEach(appendNumbersTo(sb));
        assertEquals("1.0;2.0;3.0;4.0;5.0;7.0;", sb.toString());

        sb.setLength(0);
        DoubleStream.of(2, 3, 1).peek(appendNumbersTo(sb)).sorted().limit(2).map(n -> n + 10)
                .forEach(appendNumbersTo(sb));
        assertEquals("2.0;3.0;1.0;11.0;12.0;", sb.toString());

        sb.setLength(0);
        DoubleStream.of(2, 3, 1).peek(appendNumbersTo(sb)).sorted().limit(0).forEach(appendNumbersTo(sb));
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
        iterator.forEachRemaining(appendNumbersTo(sb));
        assertEquals("1.0;2.0;3.0;", sb.toString());
    }

    @Test
    public void spliterator() {
        StringBuilder sb = new StringBuilder();
        Spliterator.OfDouble spliterator = DoubleStream.of(1, 2, 3).spliterator();
        while (spliterator.tryAdvance(appendNumbersTo(sb))) {
            // continue
        }
        assertEquals("1.0;2.0;3.0;", sb.toString());

        sb.setLength(0);
        spliterator = DoubleStream.of(1, 2, 3).spliterator();
        spliterator.forEachRemaining(appendNumbersTo(sb));
        assertEquals("1.0;2.0;3.0;", sb.toString());
    }

    @Test
    public void average() {
        assertEquals(2.5, DoubleStream.of(1, 2, 3, 4).average().getAsDouble(), 0.001);
        assertFalse(DoubleStream.empty().average().isPresent());
    }

    private DoubleConsumer appendNumbersTo(StringBuilder sb) {
        return n -> sb.append(n).append(';');
    }

    private IntConsumer appendIntNumbersTo(StringBuilder sb) {
        return n -> sb.append(n).append(';');
    }

    private LongConsumer appendLongNumbersTo(StringBuilder sb) {
        return n -> sb.append(n).append(';');
    }
}
