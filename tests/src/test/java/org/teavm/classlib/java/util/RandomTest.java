/*
 *  Copyright 2023 ihromant.
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
package org.teavm.classlib.java.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.Arrays;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.SkipPlatform;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.TestPlatform;

@RunWith(TeaVMTestRunner.class)
@SkipPlatform(TestPlatform.WASI)
public class RandomTest {
    @Test
    public void testDoubles() {
        Random random = new Random();
        double[] doubles = IntStream.range(0, 1000).mapToDouble(i -> random.nextDouble()).toArray();
        for (double d : doubles) {
            assertTrue(d >= 0.0 && d < 1.0);
        }
        try {
            random.nextDouble(-1.0);
            fail();
        } catch (IllegalArgumentException e) {
            // normal
        }
        doubles = IntStream.range(0, 1000).mapToDouble(i -> random.nextDouble(20.0)).toArray();
        for (double d : doubles) {
            assertTrue(d >= 0.0 && d < 20.0);
        }
        try {
            random.nextDouble(-1.0, -2.0);
            fail();
        } catch (IllegalArgumentException e) {
            // normal
        }
        doubles = IntStream.range(0, 1000).mapToDouble(i -> random.nextDouble(-2.0, -1.0)).toArray();
        for (double d : doubles) {
            assertTrue(d >= -2.0 && d < 1.0);
        }
    }

    @Test
    @SkipPlatform(TestPlatform.C)
    public void testIntegers() {
        Random random = new Random();
        int[] ints = IntStream.range(0, 10000).map(i -> random.nextInt())
                .toArray(); // 10 000 enough for almost 100% probability
        int ones = IntStream.of(ints).reduce(0, (id, i) -> id | i);
        Set<Integer> unique = Arrays.stream(ints).boxed().collect(Collectors.toSet());
        assertEquals(-1, ones); // all ones present
        assertTrue(unique.size() > 9900);
        try {
            random.nextInt(-5);
            fail();
        } catch (IllegalArgumentException e) {
            // normal
        }
        ints = IntStream.range(0, 1000).map(i -> random.nextInt(512)).toArray();
        for (int i : ints) {
            assertTrue(i >= 0 && i < 512);
        }
        ints = IntStream.range(0, 1000).map(i -> random.nextInt(20)).toArray();
        for (int i : ints) {
            assertTrue(i >= 0 && i < 20);
        }
        try {
            random.nextInt(-3, -5);
            fail();
        } catch (IllegalArgumentException e) {
            // normal
        }
        ints = IntStream.range(0, 1000).map(i -> random.nextInt(Integer.MIN_VALUE / 3 * 2, Integer.MAX_VALUE / 3 * 2))
                .toArray();
        for (int i : ints) {
            assertTrue(i >= Integer.MIN_VALUE / 3 * 2 && i < Integer.MAX_VALUE / 3 * 2);
        }
        Arrays.stream(ints).anyMatch(i -> i < Integer.MIN_VALUE / 2);
        Arrays.stream(ints).anyMatch(i -> i > Integer.MAX_VALUE / 2);
    }

    @Test
    @SkipPlatform(TestPlatform.C)
    public void testLongs() {
        Random random = new Random();
        long[] longs = IntStream.range(0, 10000).mapToLong(i -> random.nextLong())
                .toArray(); // 10 000 enough for almost 100% probability
        long ones = LongStream.of(longs).reduce(0L, (id, i) -> id | i);
        Set<Long> unique = Arrays.stream(longs).boxed().collect(Collectors.toSet());
        assertEquals(-1L, ones); // all ones present
        assertTrue(unique.size() > 9900);
        try {
            random.nextLong(-5);
            fail();
        } catch (IllegalArgumentException e) {
            // normal
        }
        longs = IntStream.range(0, 1000).mapToLong(i -> random.nextLong(512L)).toArray();
        for (long l : longs) {
            assertTrue(l >= 0L && l < 512L);
        }
        longs = IntStream.range(0, 1000).mapToLong(i -> random.nextLong(20L)).toArray();
        for (long l : longs) {
            assertTrue(l >= 0 && l < 20);
        }
        try {
            random.nextLong(-3, -5);
            fail();
        } catch (IllegalArgumentException e) {
            // normal
        }
        longs = IntStream.range(0, 1000).mapToLong(i -> random.nextLong(Long.MIN_VALUE / 3 * 2, Long.MAX_VALUE / 3 * 2))
                .toArray();
        for (long l : longs) {
            assertTrue(l >= Long.MIN_VALUE / 3 * 2 && l < Long.MAX_VALUE / 3 * 2);
        }
        Arrays.stream(longs).anyMatch(l -> l < Long.MIN_VALUE / 2);
        Arrays.stream(longs).anyMatch(l -> l > Long.MAX_VALUE / 2);
    }

    @Test
    public void testNextBytes() {
        Random rand = new Random();
        rand.nextBytes(new byte[0]);
        assertTrue(IntStream.range(0, 1000).anyMatch(i -> testNonZero(rand, 5)));
        assertTrue(IntStream.range(0, 1000).anyMatch(i -> testNonZero(rand, 6)));
        assertTrue(IntStream.range(0, 1000).anyMatch(i -> testNonZero(rand, 7)));
        assertTrue(IntStream.range(0, 1000).anyMatch(i -> testNonZero(rand, 8)));
        byte[] bytes = new byte[1000];
        rand.nextBytes(bytes);
        assertEquals(0xFF, IntStream.range(0, bytes.length).map(i -> 0xFF & bytes[i]).reduce(0, (id, i) -> id | i));
    }

    private boolean testNonZero(Random rand, int size) {
        byte[] bytes = new byte[size];
        rand.nextBytes(bytes);
        for (byte b : bytes) {
            if (b == 0) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void testGaussian() {
        Random rand = new Random();
        double[] doubles = IntStream.range(0, 10000).mapToDouble(i -> rand.nextGaussian(30, 10)).toArray();
        assertTrue(DoubleStream.of(doubles).filter(d -> d < 0.0 || d > 60.0).count() < 100);
    }
}
