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
package org.teavm.classlib.java.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class SpliteratorsTest {

    @Test
    public void intSpliterator() {
        int[] values = { 1, 2, 3, 4, 5 };

        Spliterator.OfInt spliterator = Spliterators.spliterator(values, 0, 5, 0);
        assertEquals(5L, spliterator.estimateSize());

        List<Integer> collected = new ArrayList<>();
        IntConsumer collector = collected::add;

        assertTrue(spliterator.tryAdvance(collector));

        assertEquals(4L, spliterator.estimateSize());
        assertEquals(Arrays.asList(1), collected);

        assertTrue(spliterator.tryAdvance(collector));
        assertTrue(spliterator.tryAdvance(collector));
        assertTrue(spliterator.tryAdvance(collector));
        assertTrue(spliterator.tryAdvance(collector));
        assertFalse(spliterator.tryAdvance(collector));

        assertEquals(0L, spliterator.estimateSize());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), collected);
    }

    @Test
    public void intSpliteratorWithSubRange() {
        int[] values = { 1, 2, 3, 4, 5 };

        Spliterator.OfInt spliterator = Spliterators.spliterator(values, 1, 4, 0);
        assertEquals(3L, spliterator.estimateSize());

        List<Integer> collected = new ArrayList<>();
        IntConsumer collector = collected::add;

        assertTrue(spliterator.tryAdvance(collector));
        assertTrue(spliterator.tryAdvance(collector));
        assertTrue(spliterator.tryAdvance(collector));
        assertFalse(spliterator.tryAdvance(collector));

        assertEquals(0L, spliterator.estimateSize());
        assertEquals(Arrays.asList(2, 3, 4), collected);
    }

    @Test
    public void spliteratorFromIterator() {
        List<Integer> values = Arrays.asList(1, 2, 3);

        Spliterator<Integer> spliterator = Spliterators.spliterator(values.iterator(), values.size(), 0);

        assertEquals(3L, spliterator.estimateSize());

        List<Integer> collected = new ArrayList<>();
        Consumer<Integer> collector = collected::add;

        assertTrue(spliterator.tryAdvance(collector));
        assertTrue(spliterator.tryAdvance(collector));
        assertTrue(spliterator.tryAdvance(collector));
        assertFalse(spliterator.tryAdvance(collector));

        assertEquals(3L, spliterator.estimateSize());
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void spliteratorFromObjectArray() {
        Object[] array = { 1, 2, 3, 4 };

        Spliterator<Integer> spliterator = Spliterators.spliterator(array, 0);
        assertEquals(4L, spliterator.estimateSize());

        List<Object> collected = new ArrayList<>();
        Consumer<Object> collector = collected::add;

        spliterator.tryAdvance(collector);
        spliterator.tryAdvance(collector);
        array[2] = 9;
        spliterator.tryAdvance(collector);
        spliterator.tryAdvance(collector);
        spliterator.tryAdvance(collector);

        assertArrayEquals(new Object[] { 1, 2, 9, 4 }, collected.toArray());
        assertEquals(0, spliterator.estimateSize());
    }
}
