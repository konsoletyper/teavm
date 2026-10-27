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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.teavm.classlib.java.lang.DoubleTest;
import org.teavm.junit.TeaVMTest;

@TeaVMTest
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
        assertTrue(Arrays.equals(dSpecials1, dSorted), "specials sort incorrectly 1: " + Arrays.toString(dSpecials1));
        Arrays.sort(dSpecials2);
        assertTrue(Arrays.equals(dSpecials2, dSorted), "specials sort incorrectly 2: " + Arrays.toString(dSpecials2));
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
        assertTrue(Arrays.equals(fSpecials1, fSorted), "specials sort incorrectly 1: " + Arrays.toString(fSpecials1));
        Arrays.sort(fSpecials2);
        assertTrue(Arrays.equals(fSpecials2, fSorted), "specials sort incorrectly 2: " + Arrays.toString(fSpecials2));
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
            assertEquals(result, i, floatSpecials[i] + " invalid: " + result);
        }
        double[] doubleSpecials = new double[] { Double.NEGATIVE_INFINITY,
                -Double.MAX_VALUE, -2d, -Double.MIN_VALUE, -0d, 0d,
                Double.MIN_VALUE, 2d, Double.MAX_VALUE,
                Double.POSITIVE_INFINITY, Double.NaN };
        for (int i = 0; i < doubleSpecials.length; i++) {
            int result = Arrays.binarySearch(doubleSpecials, doubleSpecials[i]);
            assertEquals(result, i, doubleSpecials[i] + " invalid: " + result);
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

    @Test
    public void deepToString() {
        int[][] array = { { 1, 2, 3 }, { 4, 5, 6 } };
        assertEquals("[[1, 2, 3], [4, 5, 6]]", Arrays.deepToString(array));
    }

    @Test
    public void copyOfRangePadsPastEndOfArray() {
        String[] strings = Arrays.copyOfRange(new String[] { "a", "b" }, 0, 4);
        assertEquals(4, strings.length);
        assertEquals("a", strings[0]);
        assertEquals("b", strings[1]);
        assertNull(strings[2]);
        assertNull(strings[3]);

        int[] ints = Arrays.copyOfRange(new int[] { 1, 2 }, 1, 4);
        assertEquals(3, ints.length);
        assertEquals(2, ints[0]);
        assertEquals(0, ints[1]);
        assertEquals(0, ints[2]);
    }

    @Test
    public void copyOfRangeWithFromGreaterThanToFails() {
        assertThrows(IllegalArgumentException.class, () -> Arrays.copyOfRange(new String[] { "a", "b" }, 2, 1));
    }

    @Test
    public void copyOfRangeWithFromPastEndFails() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> Arrays.copyOfRange(new String[] { "a", "b" }, 3, 4));
    }

    @Test
    public void compare() {
        assertEquals(0, Arrays.compare(new int[] { 1, 2 }, new int[] { 1, 2 }));
        assertEquals(-1, Integer.signum(Arrays.compare(new int[] { 1, 2 }, new int[] { 1, 3 })));
        assertEquals(-1, Integer.signum(Arrays.compare(new int[] { 1, 2 }, new int[] { 1, 2, 0 })));
        assertEquals(1, Integer.signum(Arrays.compare(new long[] { 5 }, new long[] { -5 })));
        assertEquals(-1, Integer.signum(Arrays.compare((byte[]) null, new byte[0])));
        assertEquals(1, Integer.signum(Arrays.compare(new double[] { Double.NaN }, new double[] { 1 })));
        assertEquals(-1, Integer.signum(Arrays.compare(new String[] { "a" }, new String[] { "b" })));
        assertEquals(1, Integer.signum(Arrays.compare(new boolean[] { true }, new boolean[] { false })));
        assertEquals(-1, Integer.signum(Arrays.compare(new char[] { 'a' }, new char[] { 'b' })));
    }

    @Test
    public void compareUnsigned() {
        assertEquals(1, Integer.signum(Arrays.compareUnsigned(new byte[] { (byte) 0xff }, new byte[] { 1 })));
        assertEquals(1, Integer.signum(Arrays.compareUnsigned(new short[] { -1 }, new short[] { 1 })));
        assertEquals(1, Integer.signum(Arrays.compareUnsigned(new int[] { -1 }, new int[] { 1 })));
        assertEquals(1, Integer.signum(Arrays.compareUnsigned(new long[] { -1 }, new long[] { 1 })));
        assertEquals(0, Arrays.compareUnsigned(new byte[] { 1, 2 }, new byte[] { 1, 2 }));
    }

    // Arrays.compare(int[], int[]): "If the two arrays share a common prefix then the lexicographic comparison is
    // the result of comparing two elements, as if by Integer.compare(int, int), at an index within the respective
    // arrays that is the prefix length. Otherwise, one array is a proper prefix of the other and, lexicographic
    // comparison is the result of comparing the two array lengths. [...] A null array reference is considered
    // lexicographically less than a non-null array reference. Two null array references are considered equal."
    // The other element types compare "as if by" Byte.compare, Float.compare, Boolean.compare and so on, and
    // compareUnsigned "as if by" Byte.compareUnsigned and so on. For Comparable[]: "A null array element is
    // considered lexicographically less than a non-null array element." Only the sign of the result is specified.
    //
    // The range forms: "IllegalArgumentException - if aFromIndex > aToIndex or if bFromIndex > bToIndex;
    // ArrayIndexOutOfBoundsException - if aFromIndex < 0 or aToIndex > a.length or if bFromIndex < 0 or
    // bToIndex > b.length; NullPointerException - if either array is null". The range forms of mismatch and
    // equals throw the same exceptions.
    //
    // The tests below check these rules against a reference implementation written from that text, on random
    // arrays over small pools of edge values, so common prefixes are frequent. On the JVM the reference is
    // checked against the JDK itself.

    @FunctionalInterface
    interface ElementComparator<A> {
        int compare(A a, int i, A b, int j);
    }

    @FunctionalInterface
    interface RangeCompare<A> {
        int compare(A a, int aFrom, int aTo, A b, int bFrom, int bTo);
    }

    @FunctionalInterface
    interface RangeEquals<A> {
        boolean equals(A a, int aFrom, int aTo, A b, int bFrom, int bTo);
    }

    @FunctionalInterface
    interface WholeCompare<A> {
        int compare(A a, A b);
    }

    static final class Kind<A> {
        final String name;
        final A pool;
        final IntFunction<A> create;
        final ElementComparator<A> element;
        final WholeCompare<A> compare;
        final RangeCompare<A> compareRange;
        // Set only for the signed kinds, whose element order also defines mismatch and equals.
        WholeCompare<A> mismatch;
        RangeCompare<A> mismatchRange;
        RangeEquals<A> equalsRange;

        Kind(String name, A pool, IntFunction<A> create, ElementComparator<A> element, WholeCompare<A> compare,
                RangeCompare<A> compareRange) {
            this.name = name;
            this.pool = pool;
            this.create = create;
            this.element = element;
            this.compare = compare;
            this.compareRange = compareRange;
        }

        Kind<A> withMismatch(WholeCompare<A> mismatch, RangeCompare<A> mismatchRange, RangeEquals<A> equalsRange) {
            this.mismatch = mismatch;
            this.mismatchRange = mismatchRange;
            this.equalsRange = equalsRange;
            return this;
        }

        A random(Random random, int length) {
            A result = create.apply(length);
            int poolSize = Array.getLength(pool);
            for (int i = 0; i < length; ++i) {
                System.arraycopy(pool, random.nextInt(poolSize), result, i, 1);
            }
            return result;
        }

        String describe(A a) {
            return a == null ? "null" : Arrays.deepToString(new Object[] { a });
        }
    }

    private static final float NAN_WITH_PAYLOAD = Float.intBitsToFloat(0x7FC00123);
    private static final double DOUBLE_NAN_WITH_PAYLOAD = Double.longBitsToDouble(0x7FF8000000000123L);

    private static List<Kind<?>> compareKinds() {
        var byteKind = new Kind<>("byte[]", new byte[] { Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE },
                byte[]::new, (a, i, b, j) -> Byte.compare(a[i], b[j]), Arrays::compare, Arrays::compare)
                .withMismatch(Arrays::mismatch, Arrays::mismatch, Arrays::equals);
        var shortKind = new Kind<>("short[]", new short[] { Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE },
                short[]::new, (a, i, b, j) -> Short.compare(a[i], b[j]), Arrays::compare, Arrays::compare)
                .withMismatch(Arrays::mismatch, Arrays::mismatch, Arrays::equals);
        var charKind = new Kind<>("char[]", new char[] { 0, 'a', 'b', 0x7FFF, 0x8000, 0xFFFF },
                char[]::new, (a, i, b, j) -> Character.compare(a[i], b[j]), Arrays::compare, Arrays::compare)
                .withMismatch(Arrays::mismatch, Arrays::mismatch, Arrays::equals);
        var intKind = new Kind<>("int[]", new int[] { Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE },
                int[]::new, (a, i, b, j) -> Integer.compare(a[i], b[j]), Arrays::compare, Arrays::compare)
                .withMismatch(Arrays::mismatch, Arrays::mismatch, Arrays::equals);
        var longKind = new Kind<>("long[]", new long[] { Long.MIN_VALUE, Integer.MIN_VALUE, -1, 0, 1,
                0x1_0000_0000L, Long.MAX_VALUE },
                long[]::new, (a, i, b, j) -> Long.compare(a[i], b[j]), Arrays::compare, Arrays::compare)
                .withMismatch(Arrays::mismatch, Arrays::mismatch, Arrays::equals);
        var floatKind = new Kind<>("float[]", new float[] { Float.NaN, NAN_WITH_PAYLOAD, Float.NEGATIVE_INFINITY,
                -1, -0.0f, 0.0f, Float.MIN_VALUE, 1, Float.MAX_VALUE, Float.POSITIVE_INFINITY },
                float[]::new, (a, i, b, j) -> Float.compare(a[i], b[j]), Arrays::compare, Arrays::compare)
                .withMismatch(Arrays::mismatch, Arrays::mismatch, Arrays::equals);
        var doubleKind = new Kind<>("double[]", new double[] { Double.NaN, DOUBLE_NAN_WITH_PAYLOAD,
                Double.NEGATIVE_INFINITY, -1, -0.0, 0.0, Double.MIN_VALUE, 1, Double.MAX_VALUE,
                Double.POSITIVE_INFINITY },
                double[]::new, (a, i, b, j) -> Double.compare(a[i], b[j]), Arrays::compare, Arrays::compare)
                .withMismatch(Arrays::mismatch, Arrays::mismatch, Arrays::equals);
        var booleanKind = new Kind<>("boolean[]", new boolean[] { false, true },
                boolean[]::new, (a, i, b, j) -> Boolean.compare(a[i], b[j]), Arrays::compare, Arrays::compare)
                .withMismatch(Arrays::mismatch, Arrays::mismatch, Arrays::equals);
        WholeCompare<String[]> comparableCompare = Arrays::compare;
        RangeCompare<String[]> comparableCompareRange = Arrays::compare;
        var comparableKind = new Kind<>("Comparable[]", new String[] { null, "", "a", "ab", "b" },
                String[]::new, (a, i, b, j) -> compareNullsFirst(a[i], b[j]), comparableCompare,
                comparableCompareRange)
                .withMismatch(Arrays::mismatch, Arrays::mismatch, Arrays::equals);
        // A comparator receives null elements itself; this one orders them last, unlike the Comparable rule.
        Comparator<String> cmp = Comparator.nullsLast(Comparator.<String>reverseOrder());
        var comparatorKind = new Kind<>("Comparator", new String[] { null, "", "a", "ab", "b" },
                String[]::new, (a, i, b, j) -> cmp.compare(a[i], b[j]), (a, b) -> Arrays.compare(a, b, cmp),
                (a, af, at, b, bf, bt) -> Arrays.compare(a, af, at, b, bf, bt, cmp));

        var unsignedByteKind = new Kind<>("unsigned byte[]", new byte[] { Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE },
                byte[]::new, (a, i, b, j) -> Integer.compare(a[i] & 0xFF, b[j] & 0xFF), Arrays::compareUnsigned,
                Arrays::compareUnsigned);
        var unsignedShortKind = new Kind<>("unsigned short[]",
                new short[] { Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE },
                short[]::new, (a, i, b, j) -> Integer.compare(a[i] & 0xFFFF, b[j] & 0xFFFF), Arrays::compareUnsigned,
                Arrays::compareUnsigned);
        var unsignedIntKind = new Kind<>("unsigned int[]", new int[] { Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE },
                int[]::new, (a, i, b, j) -> Long.compare(a[i] & 0xFFFF_FFFFL, b[j] & 0xFFFF_FFFFL),
                Arrays::compareUnsigned, Arrays::compareUnsigned);
        var unsignedLongKind = new Kind<>("unsigned long[]", new long[] { Long.MIN_VALUE, Integer.MIN_VALUE, -1, 0, 1,
                0x1_0000_0000L, Long.MAX_VALUE },
                long[]::new, (a, i, b, j) -> unsignedLongReference(a[i], b[j]), Arrays::compareUnsigned,
                Arrays::compareUnsigned);
        return List.of(byteKind, shortKind, charKind, intKind, longKind, floatKind, doubleKind, booleanKind,
                comparableKind, comparatorKind, unsignedByteKind, unsignedShortKind, unsignedIntKind,
                unsignedLongKind);
    }

    private static int compareNullsFirst(String a, String b) {
        if (a == null || b == null) {
            return a == b ? 0 : a == null ? -1 : 1;
        }
        return a.compareTo(b);
    }

    // Compares the two longs as unsigned numbers, written independently of Long.compareUnsigned.
    private static int unsignedLongReference(long a, long b) {
        if ((a < 0) != (b < 0)) {
            return a < 0 ? 1 : -1;
        }
        return Long.compare(a, b);
    }

    private static <A> int referenceCompare(Kind<A> kind, A a, int aFrom, int aTo, A b, int bFrom, int bTo) {
        int length = Math.min(aTo - aFrom, bTo - bFrom);
        for (int i = 0; i < length; ++i) {
            int c = kind.element.compare(a, aFrom + i, b, bFrom + i);
            if (c != 0) {
                return Integer.signum(c);
            }
        }
        return Integer.signum((aTo - aFrom) - (bTo - bFrom));
    }

    private static <A> int referenceMismatch(Kind<A> kind, A a, int aFrom, int aTo, A b, int bFrom, int bTo) {
        int length = Math.min(aTo - aFrom, bTo - bFrom);
        for (int i = 0; i < length; ++i) {
            if (kind.element.compare(a, aFrom + i, b, bFrom + i) != 0) {
                return i;
            }
        }
        return aTo - aFrom == bTo - bFrom ? -1 : length;
    }

    @Test
    public void compareMatchesLexicographicOrderOnRandomArrays() {
        var random = new Random(0x5EED);
        for (var kind : compareKinds()) {
            checkRandomCompare(kind, random);
        }
    }

    private static <A> void checkRandomCompare(Kind<A> kind, Random random) {
        for (int trial = 0; trial < 1500; ++trial) {
            A a = kind.random(random, random.nextInt(7));
            int aLength = Array.getLength(a);
            A b;
            switch (random.nextInt(4)) {
                case 0:
                    b = kind.random(random, random.nextInt(7));
                    break;
                case 1: {
                    // An exact copy, or a copy with one element replaced.
                    b = kind.create.apply(aLength);
                    System.arraycopy(a, 0, b, 0, aLength);
                    if (aLength > 0 && random.nextBoolean()) {
                        A replacement = kind.random(random, 1);
                        System.arraycopy(replacement, 0, b, random.nextInt(aLength), 1);
                    }
                    break;
                }
                default: {
                    // Shares a random-length prefix with a, then continues with random elements.
                    b = kind.random(random, random.nextInt(7));
                    System.arraycopy(a, 0, b, 0, Math.min(random.nextInt(aLength + 1), Array.getLength(b)));
                    break;
                }
            }
            int bLength = Array.getLength(b);
            String description = kind.name + " " + kind.describe(a) + " vs " + kind.describe(b);

            int expected = referenceCompare(kind, a, 0, aLength, b, 0, bLength);
            assertEquals(expected, Integer.signum(kind.compare.compare(a, b)), description);
            assertEquals(-expected, Integer.signum(kind.compare.compare(b, a)), description);
            assertEquals(0, kind.compare.compare(a, a), description);
            if (kind.mismatch != null) {
                int expectedMismatch = referenceMismatch(kind, a, 0, aLength, b, 0, bLength);
                assertEquals(expectedMismatch, kind.mismatch.compare(a, b), "mismatch " + description);
            }

            int aFrom = random.nextInt(aLength + 1);
            int aTo = aFrom + random.nextInt(aLength - aFrom + 1);
            int bFrom = random.nextInt(bLength + 1);
            int bTo = bFrom + random.nextInt(bLength - bFrom + 1);
            String rangeDescription = description + " ranges [" + aFrom + ", " + aTo + ") and [" + bFrom + ", "
                    + bTo + ")";
            int expectedRange = referenceCompare(kind, a, aFrom, aTo, b, bFrom, bTo);
            assertEquals(expectedRange, Integer.signum(kind.compareRange.compare(a, aFrom, aTo, b, bFrom, bTo)),
                    rangeDescription);
            assertEquals(-expectedRange, Integer.signum(kind.compareRange.compare(b, bFrom, bTo, a, aFrom, aTo)),
                    rangeDescription);
            if (kind.mismatch != null) {
                int expectedMismatch = referenceMismatch(kind, a, aFrom, aTo, b, bFrom, bTo);
                assertEquals(expectedMismatch, kind.mismatchRange.compare(a, aFrom, aTo, b, bFrom, bTo),
                        "mismatch " + rangeDescription);
                assertEquals(expectedMismatch < 0, kind.equalsRange.equals(a, aFrom, aTo, b, bFrom, bTo),
                        "equals " + rangeDescription);
            }
        }
    }

    @Test
    public void compareOrdersNullArraysFirst() {
        for (var kind : compareKinds()) {
            checkNullArrays(kind);
        }
    }

    private static <A> void checkNullArrays(Kind<A> kind) {
        A empty = kind.create.apply(0);
        A one = kind.random(new Random(1), 1);
        assertEquals(0, kind.compare.compare(null, null), kind.name);
        assertTrue(kind.compare.compare(null, empty) < 0, kind.name);
        assertTrue(kind.compare.compare(null, one) < 0, kind.name);
        assertTrue(kind.compare.compare(empty, null) > 0, kind.name);
        assertTrue(kind.compare.compare(one, null) > 0, kind.name);
        assertThrows(NullPointerException.class, () -> kind.compareRange.compare(null, 0, 0, empty, 0, 0), kind.name);
        assertThrows(NullPointerException.class, () -> kind.compareRange.compare(empty, 0, 0, null, 0, 0), kind.name);
    }

    @Test
    public void compareRangeChecksIndexes() {
        for (var kind : compareKinds()) {
            checkRangeExceptions(kind);
        }
    }

    private static <A> void checkRangeExceptions(Kind<A> kind) {
        checkRangeExceptions(kind, kind.compareRange);
        if (kind.mismatch != null) {
            checkRangeExceptions(kind, kind.mismatchRange);
            RangeEquals<A> equals = kind.equalsRange;
            checkRangeExceptions(kind, (a, af, at, b, bf, bt) -> equals.equals(a, af, at, b, bf, bt) ? 1 : 0);
        }
    }

    private static <A> void checkRangeExceptions(Kind<A> kind, RangeCompare<A> op) {
        A a = kind.random(new Random(2), 3);
        A b = kind.random(new Random(3), 3);
        String name = kind.name;
        // Each case breaks exactly one rule.
        assertThrows(IllegalArgumentException.class, () -> op.compare(a, 2, 1, b, 0, 3), name);
        assertThrows(IllegalArgumentException.class, () -> op.compare(a, 0, 3, b, 3, 2), name);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> op.compare(a, -1, 2, b, 0, 3), name);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> op.compare(a, 0, 4, b, 0, 3), name);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> op.compare(a, 0, 3, b, -1, 3), name);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> op.compare(a, 0, 3, b, 0, 4), name);
        // Empty ranges at either end are fine.
        op.compare(a, 0, 0, b, 3, 3);
        op.compare(a, 3, 3, b, 0, 0);
    }

    @Test
    public void compareWithComparatorRequiresComparator() {
        // "NullPointerException - if the comparator is null"
        String[] a = { "a" };
        assertThrows(NullPointerException.class, () -> Arrays.compare(a, a, null));
        assertThrows(NullPointerException.class, () -> Arrays.compare((String[]) null, null, null));
        assertThrows(NullPointerException.class, () -> Arrays.compare(a, 0, 1, a, 0, 1, null));
    }

    @Test
    public void compareFloatingPointEdgeCases() {
        // Float.compare: "0.0f is considered by this method to be greater than -0.0f" and "Float.NaN is
        // considered by this method to be equal to itself and greater than all other float values (including
        // Float.POSITIVE_INFINITY)".
        assertTrue(Arrays.compare(new float[] { -0.0f }, new float[] { 0.0f }) < 0);
        assertTrue(Arrays.compare(new float[] { Float.NaN }, new float[] { Float.POSITIVE_INFINITY }) > 0);
        assertEquals(0, Arrays.compare(new float[] { Float.NaN }, new float[] { NAN_WITH_PAYLOAD }));
        assertTrue(Arrays.compare(new double[] { -0.0 }, new double[] { 0.0 }) < 0);
        assertTrue(Arrays.compare(new double[] { Double.NaN }, new double[] { Double.POSITIVE_INFINITY }) > 0);
        assertEquals(0, Arrays.compare(new double[] { Double.NaN }, new double[] { DOUBLE_NAN_WITH_PAYLOAD }));
        assertTrue(Arrays.compare(new double[] { 1, Double.NaN }, new double[] { 1, Double.NaN, 0 }) < 0);
    }

    @Test
    public void compareUnsignedEdgeCases() {
        assertTrue(Arrays.compareUnsigned(new byte[] { (byte) 0x80 }, new byte[] { 0x7F }) > 0);
        assertTrue(Arrays.compare(new byte[] { (byte) 0x80 }, new byte[] { 0x7F }) < 0);
        assertTrue(Arrays.compareUnsigned(new short[] { (short) 0x8000 }, new short[] { 0x7FFF }) > 0);
        assertTrue(Arrays.compareUnsigned(new int[] { Integer.MIN_VALUE }, new int[] { Integer.MAX_VALUE }) > 0);
        assertTrue(Arrays.compareUnsigned(new long[] { Long.MIN_VALUE }, new long[] { Long.MAX_VALUE }) > 0);
        assertTrue(Arrays.compareUnsigned(new long[] { -1 }, new long[] { -2 }) > 0);
    }
}
