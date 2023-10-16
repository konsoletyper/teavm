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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.teavm.classlib.java.lang.DoubleTest;
import org.teavm.junit.TeaVMTestRunner;

@RunWith(TeaVMTestRunner.class)
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
        Arrays.sort(array, null); // NPE check
        double[] dSpecials1 = new double[] { Double.NaN, Double.MAX_VALUE,
                Double.MIN_VALUE, 0d, -0d, Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY };
        double[] dSpecials2 = new double[] { 0d, Double.POSITIVE_INFINITY, -0d,
                Double.NEGATIVE_INFINITY, Double.MIN_VALUE, Double.NaN,
                Double.MAX_VALUE };
        double[] dSorted = new double[] { Double.NEGATIVE_INFINITY, -0d, 0d,
                Double.MIN_VALUE, Double.MAX_VALUE, Double.POSITIVE_INFINITY,
                Double.NaN };
        Arrays.sort(dSpecials1);
        assertTrue("specials sort incorrectly 1: " + Arrays.toString(dSpecials1),
                Arrays.equals(dSpecials1, dSorted));
        Arrays.sort(dSpecials2);
        assertTrue("specials sort incorrectly 2: " + Arrays.toString(dSpecials2),
                Arrays.equals(dSpecials2, dSorted));
        float[] fSpecials1 = new float[] { Float.NaN, Float.MAX_VALUE,
                Float.MIN_VALUE, 0f, -0f, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY };
        float[] fSpecials2 = new float[] { 0f, Float.POSITIVE_INFINITY, -0f,
                Float.NEGATIVE_INFINITY, Float.MIN_VALUE, Float.NaN,
                Float.MAX_VALUE };
        float[] fSorted = new float[] { Float.NEGATIVE_INFINITY, -0f, 0f,
                Float.MIN_VALUE, Float.MAX_VALUE, Float.POSITIVE_INFINITY,
                Float.NaN };
        Arrays.sort(fSpecials1);
        assertTrue("specials sort incorrectly 1: " + Arrays.toString(fSpecials1),
                Arrays.equals(fSpecials1, fSorted));
        Arrays.sort(fSpecials2);
        assertTrue("specials sort incorrectly 2: " + Arrays.toString(fSpecials2),
                Arrays.equals(fSpecials2, fSorted));
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
        assertEquals(3, Arrays.binarySearch(array, 8, null)); // NPE check
        assertEquals(-6, Arrays.binarySearch(array, 5, 5, 10));
        float[] floatSpecials = new float[] { Float.NEGATIVE_INFINITY,
                -Float.MAX_VALUE, -2f, -Float.MIN_VALUE, -0f, 0f,
                Float.MIN_VALUE, 2f, Float.MAX_VALUE, Float.POSITIVE_INFINITY,
                Float.NaN };
        for (int i = 0; i < floatSpecials.length; i++) {
            int result = Arrays.binarySearch(floatSpecials, floatSpecials[i]);
            assertEquals(floatSpecials[i] + " invalid: " + result, result, i);
        }
        double[] doubleSpecials = new double[] { Double.NEGATIVE_INFINITY,
                -Double.MAX_VALUE, -2d, -Double.MIN_VALUE, -0d, 0d,
                Double.MIN_VALUE, 2d, Double.MAX_VALUE,
                Double.POSITIVE_INFINITY, Double.NaN };
        for (int i = 0; i < doubleSpecials.length; i++) {
            int result = Arrays.binarySearch(doubleSpecials, doubleSpecials[i]);
            assertEquals(doubleSpecials[i] + " invalid: " + result, result, i);
        }
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

    @Test
    public void testHashcode() {
        assertEquals(List.of(1, 2, 3).hashCode(), Arrays.hashCode(new int[] { 1, 2, 3 }));
        assertEquals(List.of(1L, 2L, 3L).hashCode(), Arrays.hashCode(new long[] { 1L, 2L, 3L }));
        assertEquals(List.of(1.0f, 2.0f, 3.0f).hashCode(), Arrays.hashCode(new float[] { 1.0f, 2.0f, 3.0f }));
        assertEquals(List.of(1.0, 2.0, 3.0).hashCode(), Arrays.hashCode(new double[] { 1.0, 2.0, 3.0 }));
        assertEquals(List.of(false, true, false).hashCode(), Arrays.hashCode(new boolean[] { false, true, false }));
        assertEquals(List.of((byte) 1, (byte) 2).hashCode(), Arrays.hashCode(new byte[] { (byte) 1, (byte) 2 }));
        assertEquals(List.of((short) 1, (short) 2).hashCode(), Arrays.hashCode(new short[] { (short) 1, (short) 2 }));
        assertEquals(List.of('a', 'b', 'c').hashCode(), Arrays.hashCode(new char[] { 'a', 'b', 'c' }));
        assertEquals(List.of(List.of('a', 'b'), List.of('c')).hashCode(),
                Arrays.deepHashCode(new char[][] { { 'a', 'b' }, { 'c' } }));
        List<Object> testList = new ArrayList<>();
        List<Optional<Integer>> innerList = new ArrayList<>();
        innerList.add(Optional.empty());
        innerList.add(null);
        innerList.add(Optional.of(5));
        testList.add(innerList);
        testList.add(null);
        testList.add(List.of(Optional.of(3), Optional.empty()));
        testList.add(6);
        assertEquals(testList.hashCode(), Arrays.deepHashCode(new Object[] {
                new Object[] { Optional.empty(), null, Optional.of(5) },
                null,
                new Object[] { Optional.of(3), Optional.empty() },
                6
        }));
    }

    @Test
    public void testArrayEquals() {
        int[] array = { 1, 2, 3 };
        int[] equal = { 1, 2, 3 };
        int[] shorter = { 1, 2 };
        int[] different = { 3, 1, 2 };
        double[] withNaN = { 1.0, Double.NaN };
        double[] withOtherNaN = { 1.0, DoubleTest.OTHER_NAN };

        // Simple equals
        assertTrue(Arrays.equals(array, array));
        assertTrue(Arrays.equals(array, equal));
        assertTrue(Arrays.equals(withNaN, withNaN));
        assertTrue(Arrays.equals(withNaN, withOtherNaN));

        // Equal to null
        assertTrue(Arrays.equals((int[]) null, null));
        assertFalse(Arrays.equals(null, array));
        assertFalse(Arrays.equals(array, null));

        // Not equal
        assertFalse(Arrays.equals(array, shorter));
        assertFalse(Arrays.equals(array, different));

        // Slices
        assertTrue(Arrays.equals(array, 0, 1, shorter, 0, 1));
        assertTrue(Arrays.equals(array, 0, 1, different, 1, 2));
    }

    @Test
    public void testMismatch() {
        int[] array = { 1, 2, 3 };
        int[] equal = { 1, 2, 3 };
        int[] shorter = { 1, 2 };
        int[] different = { 3, 1, 2 };

        // Simple equals
        assertEquals(-1, Arrays.mismatch(array, array));
        assertEquals(-1, Arrays.mismatch(array, equal));

        // Not equal
        assertEquals(2, Arrays.mismatch(array, shorter));
        assertEquals(0, Arrays.mismatch(array, different));

        // Slices
        assertEquals(-1, Arrays.mismatch(array, 0, 1, shorter, 0, 1));
        assertEquals(-1, Arrays.mismatch(array, 0, 1, different, 1, 2));
    }
}
