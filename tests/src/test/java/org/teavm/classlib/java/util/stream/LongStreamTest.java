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
import java.util.stream.LongStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
public class LongStreamTest {
    @Test
    public void forEachWorks() {
        StringBuilder sb = new StringBuilder();
        LongStream.of(1, 2, 3).forEach(appendNumbersTo(sb));
        assertEquals("1;2;3;", sb.toString());
    }

    @Test
    public void mapWorks() {
        StringBuilder sb = new StringBuilder();
        LongStream.of(1, 2, 3).map(n -> n * n).forEach(appendNumbersTo(sb));
        assertEquals("1;4;9;", sb.toString());
    }

    @Test
    public void mapToObjWorks() {
        String result = LongStream.of(1, 2, 3).mapToObj(n -> String.valueOf(n * n)).collect(Collectors.joining(";"));
        assertEquals("1;4;9", result);
    }

    @Test
    public void mapToIntWorks() {
        StringBuilder sb = new StringBuilder();
        LongStream.of(1, 2, 3).mapToInt(n -> (int) (n * n)).forEach(appendIntNumbersTo(sb));
        assertEquals("1;4;9;", sb.toString());
    }

    @Test
    public void mapToDoubleWorks() {
        StringBuilder sb = new StringBuilder();
        LongStream.of(1, 2, 3).mapToDouble(n -> n * n).forEach(appendDoubleNumbersTo(sb));
        assertEquals("1.0;4.0;9.0;", sb.toString());
    }

    @Test
    public void filterWorks() {
        StringBuilder sb = new StringBuilder();
        LongStream.of(1, 2, 3, 4, 5, 6).filter(n -> (n & 1) == 0).forEach(appendNumbersTo(sb));
        assertEquals("2;4;6;", sb.toString());
    }

    @Test
    public void flatMapWorks() {
        StringBuilder sb = new StringBuilder();
        LongStream.of(1, 3).flatMap(n -> LongStream.of(n, n + 1)).forEach(appendNumbersTo(sb));
        assertEquals("1;2;3;4;", sb.toString());

        sb.setLength(0);
        LongStream.of(1, 3).flatMap(n -> LongStream.of(n, n + 1)).skip(1).forEach(appendNumbersTo(sb));
        assertEquals("2;3;4;", sb.toString());

        sb.setLength(0);
        LongStream.of(1, 4).flatMap(n -> LongStream.of(n, n + 1, n + 2)).skip(4).forEach(appendNumbersTo(sb));
        assertEquals("5;6;", sb.toString());
    }

    @Test
    public void skipWorks() {
        for (int i = 0; i <= 6; ++i) {
            StringBuilder sb = new StringBuilder();
            LongStream.iterate(1, n -> n + 1).limit(5).skip(i).forEach(appendNumbersTo(sb));

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
            LongStream.iterate(1, n -> n + 1).limit(i).forEach(appendNumbersTo(sb));

            StringBuilder expected = new StringBuilder();
            for (int j = 0; j < i; ++j) {
                expected.append(j + 1).append(';');
            }
            assertEquals("Error limiting to " + i + " elements", expected.toString(), sb.toString());
        }
    }

    @Test
    public void countWorks() {
        assertEquals(4, LongStream.of(2, 3, 2, 3).count());
        assertEquals(3, LongStream.of(2, 3, 2, 3).limit(3).count());
        assertEquals(4, LongStream.of(2, 3, 2, 3).limit(5).count());
        assertEquals(0, LongStream.of(2, 3, 2, 3).skip(5).count());
        assertEquals(3, LongStream.of(2, 3, 2, 3).skip(1).count());

        assertEquals(2, LongStream.of(2, 3, 2, 3).filter(n -> n == 2).count());

        assertEquals(10, LongStream.generate(() -> 1).limit(10).count());
        assertEquals(10, LongStream.generate(() -> 1).limit(10).count());

        assertEquals(4, LongStream.of(1, 3).flatMap(n -> LongStream.of(n, n + 1)).count());
    }

    @Test
    public void distinctWorks() {
        assertArrayEquals(new long[] { 2, 3 }, LongStream.of(2, 3, 2, 3).distinct().toArray());
        assertArrayEquals(new long[] { 3, 2 }, LongStream.of(2, 3, 2, 3).skip(1).distinct().toArray());
        assertArrayEquals(new long[] { 2, 3 }, LongStream.of(2, 3, 2, 3).limit(2).distinct().toArray());
        assertArrayEquals(new long[] { 2, 3, 4, 1 }, LongStream.of(2, 2, 3, 2, 4, 3, 1).distinct().toArray());
    }

    @Test
    public void findFirstWorks() {
        assertEquals(2, LongStream.of(2, 3).findFirst().getAsLong());
        assertEquals(3, LongStream.of(2, 3).skip(1).findFirst().getAsLong());
        assertEquals(4, LongStream.of(2, 3, 4, 5).filter(n -> n > 3).findFirst().getAsLong());
        assertFalse(LongStream.of(2, 3, 4, 5).filter(n -> n > 10).findFirst().isPresent());
        assertFalse(LongStream.of(2, 3).skip(3).findFirst().isPresent());
        assertEquals(20, LongStream.of(2, 3).map(n -> n * 10).findFirst().getAsLong());
    }

    @Test
    public void concatWorks() {
        StringBuilder sb = new StringBuilder();
        LongStream.concat(LongStream.of(1, 2), LongStream.of(3, 4)).forEach(appendNumbersTo(sb));
        assertEquals("1;2;3;4;", sb.toString());

        sb.setLength(0);
        LongStream.concat(LongStream.of(1, 2), LongStream.of(3, 4)).skip(1).forEach(appendNumbersTo(sb));
        assertEquals("2;3;4;", sb.toString());

        sb.setLength(0);
        LongStream.concat(LongStream.of(1, 2), LongStream.of(3, 4, 5)).skip(3).forEach(appendNumbersTo(sb));
        assertEquals("4;5;", sb.toString());
    }

    @Test
    public void peekWorks() {
        StringBuilder sb = new StringBuilder();
        LongStream.of(1, 2, 3).peek(appendNumbersTo(sb)).map(n -> n + 10).forEach(appendNumbersTo(sb));
        assertEquals("1;11;2;12;3;13;", sb.toString());
    }

    @Test
    public void reduceWorks() {
        assertEquals(10, LongStream.of(1, 2, 3, 4).reduce(0, (a, b) -> a + b));
        assertEquals(0, LongStream.of(1, 2, 3, 4).skip(4).reduce(0, (a, b) -> a + b));
        assertEquals(720, LongStream.iterate(1, n -> n + 1).limit(6).reduce(1, (a, b) -> a * b));

        assertEquals(9, LongStream.of(1, 2, 3, 4).skip(1).reduce((a, b) -> a + b).getAsLong());
        assertFalse(LongStream.of(1, 2, 3, 4).skip(4).reduce((a, b) -> a + b).isPresent());
    }

    @Test
    public void streamOfOneElement() {
        StringBuilder sb = new StringBuilder();
        LongStream.of(5).forEach(appendNumbersTo(sb));
        assertEquals("5;", sb.toString());

        sb.setLength(0);
        LongStream.of(5).skip(1).forEach(appendNumbersTo(sb));
        assertEquals("", sb.toString());

        sb.setLength(0);
        LongStream.concat(LongStream.of(5), LongStream.of(6)).forEach(appendNumbersTo(sb));
        assertEquals("5;6;", sb.toString());
    }

    @Test
    public void sortedStream() {
        StringBuilder sb = new StringBuilder();
        LongStream.of(5, 7, 1, 2, 4, 3).sorted().forEach(appendNumbersTo(sb));
        assertEquals("1;2;3;4;5;7;", sb.toString());

        sb.setLength(0);
        LongStream.of(2, 3, 1).peek(appendNumbersTo(sb)).sorted().limit(2).map(n -> n + 10)
                .forEach(appendNumbersTo(sb));
        assertEquals("2;3;1;11;12;", sb.toString());

        sb.setLength(0);
        LongStream.of(2, 3, 1).peek(appendNumbersTo(sb)).sorted().limit(0).forEach(appendNumbersTo(sb));
        assertEquals("2;3;1;", sb.toString());
    }

    @Test
    public void minMax() {
        assertEquals(3, LongStream.of(5, 7, 3, 6).min().getAsLong());
        assertEquals(7, LongStream.of(5, 7, 3, 6).max().getAsLong());

        assertEquals(3, LongStream.of(5, 7, 3, 6).skip(1).min().getAsLong());
        assertEquals(7, LongStream.of(5, 7, 3, 6).skip(1).max().getAsLong());

        assertEquals(3, LongStream.of(5, 7, 3, 6).skip(2).min().getAsLong());
        assertEquals(6, LongStream.of(5, 7, 3, 6).skip(2).max().getAsLong());

        assertEquals(6, LongStream.of(5, 7, 3, 6).skip(3).min().getAsLong());
        assertEquals(6, LongStream.of(5, 7, 3, 6).skip(3).max().getAsLong());

        assertFalse(LongStream.empty().min().isPresent());
        assertFalse(LongStream.empty().max().isPresent());
    }

    @Test
    public void allNoneAny() {
        assertTrue(LongStream.of(5, 7, 3, 6).anyMatch(n -> n == 7));
        assertFalse(LongStream.of(5, 7, 3, 6).anyMatch(n -> n == 11));
        assertFalse(LongStream.empty().anyMatch(n -> true));

        assertFalse(LongStream.of(5, 7, 3, 6).noneMatch(n -> n == 7));
        assertTrue(LongStream.of(5, 7, 3, 6).noneMatch(n -> n == 11));
        assertTrue(LongStream.empty().noneMatch(n -> true));

        assertTrue(LongStream.of(5, 7, 3, 6).allMatch(n -> n < 10));
        assertFalse(LongStream.of(5, 7, 3, 6).allMatch(n -> n < 6));
        assertTrue(LongStream.empty().allMatch(n -> false));
    }

    @Test
    public void closeFlatMap() {
        int[] closed = new int[3];

        LongStream.of(0, 1)
                .flatMap(n -> LongStream.of(n, n + 1).onClose(() -> closed[(int) n]++))
                .onClose(() -> closed[2]++)
                .skip(10).close();

        assertArrayEquals(new int[] { 0, 0, 1 }, closed);
    }

    @Test
    public void closeMap() {
        int[] closed = new int[2];
        LongStream.of(1, 2).onClose(() -> closed[0]++).map(x -> x * 2).onClose(() -> closed[1]++).close();
        assertArrayEquals(new int[] { 1, 1 }, closed);
    }

    @Test
    public void closeConcat() {
        int[] closed = new int[3];

        LongStream.concat(
                LongStream.of(1, 2).onClose(() -> closed[0]++),
                LongStream.of(3, 4).onClose(() -> closed[1]++)
        ).onClose(() -> closed[2]++).close();

        assertArrayEquals(new int[] { 1, 1, 1 }, closed);
    }

    @Test
    public void iterator() {
        StringBuilder sb = new StringBuilder();
        PrimitiveIterator.OfLong iterator = LongStream.of(1, 2, 3).iterator();
        while (iterator.hasNext()) {
            sb.append(iterator.next()).append(';');
        }
        assertEquals("1;2;3;", sb.toString());

        sb.setLength(0);
        iterator = LongStream.of(1, 2, 3).iterator();
        iterator.forEachRemaining(appendNumbersTo(sb));
        assertEquals("1;2;3;", sb.toString());
    }

    @Test
    public void spliterator() {
        StringBuilder sb = new StringBuilder();
        Spliterator.OfLong spliterator = LongStream.of(1, 2, 3).spliterator();
        while (spliterator.tryAdvance(appendNumbersTo(sb))) {
            // continue
        }
        assertEquals("1;2;3;", sb.toString());

        sb.setLength(0);
        spliterator = LongStream.of(1, 2, 3).spliterator();
        spliterator.forEachRemaining(appendNumbersTo(sb));
        assertEquals("1;2;3;", sb.toString());
    }

    @Test
    public void average() {
        assertEquals(2.5, LongStream.of(1, 2, 3, 4).average().getAsDouble(), 0.001);
        assertFalse(LongStream.empty().average().isPresent());
    }

    @Test
    public void range() {
        StringBuilder sb = new StringBuilder();
        LongStream.range(1, 4).forEach(appendNumbersTo(sb));
        assertEquals("1;2;3;", sb.toString());

        sb.setLength(0);
        LongStream.rangeClosed(1, 4).forEach(appendNumbersTo(sb));
        assertEquals("1;2;3;4;", sb.toString());
    }

    private LongConsumer appendNumbersTo(StringBuilder sb) {
        return n -> sb.append(n).append(';');
    }

    private IntConsumer appendIntNumbersTo(StringBuilder sb) {
        return n -> sb.append(n).append(';');
    }

    private DoubleConsumer appendDoubleNumbersTo(StringBuilder sb) {
        return n -> sb.append(n).append(';');
    }
}
