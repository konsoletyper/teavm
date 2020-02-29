/*
 *  Copyright 2014 Alexey Andreev.
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.junit.TeaVMTestRunner;
import org.teavm.junit.WholeClassCompilation;

@RunWith(TeaVMTestRunner.class)
@WholeClassCompilation
public class ArraysTest {
    @Test
    public void arraySorted() {
        Integer[] array = { 2, 5, 7, 3, 5, 6 };
        Arrays.sort(array);
        assertEquals(Integer.valueOf(2), array[0]);
        assertEquals(Integer.valueOf(3), array[1]);
        assertEquals(Integer.valueOf(5), array[2]);
        assertEquals(Integer.valueOf(5), array[3]);
        assertEquals(Integer.valueOf(6), array[4]);
        assertEquals(Integer.valueOf(7), array[5]);
    }

    @Test
    public void binarySearchWorks() {
        Integer[] array = { 2, 4, 6, 8, 10, 12, 14, 16 };
        assertEquals(3, Arrays.binarySearch(array, 8));
        assertEquals(7, Arrays.binarySearch(array, 16));
        assertEquals(0, Arrays.binarySearch(array, 2));
        assertEquals(-1, Arrays.binarySearch(array, 1));
        assertEquals(-2, Arrays.binarySearch(array, 3));
        assertEquals(-3, Arrays.binarySearch(array, 5));
        assertEquals(-8, Arrays.binarySearch(array, 15));
        assertEquals(-9, Arrays.binarySearch(array, 17));
    }

    @Test
    public void arrayExposedAsList() {
        Integer[] array = { 2, 3, 4 };
        List<Integer> list = Arrays.asList(array);
        assertEquals(3, list.size());
        assertEquals(Integer.valueOf(4), list.get(2));
    }

    @Test
    public void arrayExposedAsString() {
        Object[] array = { 1, 2, null, null, "foo" };
        array[3] = array;
        assertEquals("[1, 2, null, [...], foo]", Arrays.deepToString(array));
    }

    @Test
    public void objectStream() {
        String[] array = { "foo", "bar", "baz" };

        String result = Arrays.stream(array).collect(Collectors.joining(","));
        assertEquals("foo,bar,baz", result);

        result = Arrays.stream(array, 1, 3).collect(Collectors.joining(","));
        assertEquals("bar,baz", result);

        result = Arrays.stream(array, 0, 2).collect(Collectors.joining(","));
        assertEquals("foo,bar", result);
    }

    @Test
    public void intStream() {
        int[] array = { 23, 42, 55 };

        String result = Arrays.stream(array).mapToObj(Integer::toString).collect(Collectors.joining(","));
        assertEquals("23,42,55", result);

        result = Arrays.stream(array, 1, 3).mapToObj(Integer::toString).collect(Collectors.joining(","));
        assertEquals("42,55", result);

        result = Arrays.stream(array, 0, 2).mapToObj(Integer::toString).collect(Collectors.joining(","));
        assertEquals("23,42", result);
    }

    @Test
    public void longStream() {
        long[] array = { 23, 42, 55 };

        String result = Arrays.stream(array).mapToObj(Long::toString).collect(Collectors.joining(","));
        assertEquals("23,42,55", result);

        result = Arrays.stream(array, 1, 3).mapToObj(Long::toString).collect(Collectors.joining(","));
        assertEquals("42,55", result);

        result = Arrays.stream(array, 0, 2).mapToObj(Long::toString).collect(Collectors.joining(","));
        assertEquals("23,42", result);
    }

    @Test
    public void doubleStream() {
        double[] array = { 23, 42, 55 };

        String result = Arrays.stream(array).mapToObj(Double::toString).collect(Collectors.joining(","));
        assertEquals("23.0,42.0,55.0", result);

        result = Arrays.stream(array, 1, 3).mapToObj(Double::toString).collect(Collectors.joining(","));
        assertEquals("42.0,55.0", result);

        result = Arrays.stream(array, 0, 2).mapToObj(Double::toString).collect(Collectors.joining(","));
        assertEquals("23.0,42.0", result);
    }
}
