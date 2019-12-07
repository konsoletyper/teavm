/*
 *  Copyright 2015 Alexey Andreev.
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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SpliteratorsTest {

    @Test
    public void intSpliterator() {
        int[] values = {1, 2, 3, 4, 5};

        TSpliterator.OfInt spliterator = TSpliterators.spliterator(values, 0, 5, 0);

        List<Integer> collected = new ArrayList<>();
        IntConsumer collector = value -> collected.add((Integer) value);

        assertTrue(spliterator.tryAdvance(collector));
        assertTrue(spliterator.tryAdvance(collector));
        assertTrue(spliterator.tryAdvance(collector));
        assertTrue(spliterator.tryAdvance(collector));
        assertTrue(spliterator.tryAdvance(collector));
        assertFalse(spliterator.tryAdvance(collector));

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), collected);
    }

    @Test
    public void intSpliteratorWithSubRange() {
        int[] values = {1, 2, 3, 4, 5};

        TSpliterator.OfInt spliterator = TSpliterators.spliterator(values, 1, 4, 0);

        List<Integer> collected = new ArrayList<>();
        IntConsumer collector = value -> collected.add((Integer) value);

        assertTrue(spliterator.tryAdvance(collector));
        assertTrue(spliterator.tryAdvance(collector));
        assertTrue(spliterator.tryAdvance(collector));
        assertFalse(spliterator.tryAdvance(collector));

        assertEquals(Arrays.asList(2, 3, 4), collected);
    }

    @Test
    public void spliteratorFromIterator() {
        List<Integer> values = Arrays.asList(1, 2, 3);

        TSpliterator<Integer> spliterator = TSpliterators.spliterator(values.iterator(),
            values.size(), 0);

        assertEquals(3L, spliterator.estimateSize());
    }

    @Test
    public void spliteratorFromObjectArray() {
        Object[] array = {1, 2, 3, 4};

        TSpliterator<Integer> spliterator = TSpliterators.spliterator(array, 0);

        List<Object> collected = new ArrayList<>();
        Consumer<Object> collector = value -> collected.add(value);

        spliterator.tryAdvance(collector);
        spliterator.tryAdvance(collector);
        array[2] = 9;
        spliterator.tryAdvance(collector);
        spliterator.tryAdvance(collector);
        spliterator.tryAdvance(collector);

        assertArrayEquals(new Object[] {1, 2, 9, 4}, array);
        assertEquals(4L, spliterator.estimateSize());
    }
}
