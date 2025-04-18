/*
 *  Copyright 2013 Alexey Andreev.
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

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import org.teavm.classlib.java.lang.TIllegalArgumentException;
import org.teavm.classlib.java.lang.TMath;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.classlib.java.lang.TStringBuilder;
import org.teavm.classlib.java.util.stream.TDoubleStream;
import org.teavm.classlib.java.util.stream.TIntStream;
import org.teavm.classlib.java.util.stream.TLongStream;
import org.teavm.classlib.java.util.stream.TStream;
import org.teavm.classlib.java.util.stream.doubleimpl.TArrayDoubleStreamImpl;
import org.teavm.classlib.java.util.stream.impl.TArrayStreamImpl;
import org.teavm.classlib.java.util.stream.intimpl.TArrayIntStreamImpl;
import org.teavm.classlib.java.util.stream.longimpl.TArrayLongStreamImpl;

public class TArrays extends TObject {
    public static char[] copyOf(char[] array, int length) {
        char[] result = new char[length];
        int sz = TMath.min(length, array.length);
        for (int i = 0; i < sz; ++i) {
            result[i] = array[i];
        }
        return result;
    }

    public static byte[] copyOf(byte[] array, int length) {
        byte[] result = new byte[length];
        int sz = TMath.min(length, array.length);
        for (int i = 0; i < sz; ++i) {
            result[i] = array[i];
        }
        return result;
    }

    public static short[] copyOf(short[] array, int length) {
        short[] result = new short[length];
        int sz = TMath.min(length, array.length);
        for (int i = 0; i < sz; ++i) {
            result[i] = array[i];
        }
        return result;
    }

    public static int[] copyOf(int[] array, int length) {
        int[] result = new int[length];
        int sz = TMath.min(length, array.length);
        for (int i = 0; i < sz; ++i) {
            result[i] = array[i];
        }
        return result;
    }

    public static long[] copyOf(long[] array, int length) {
        long[] result = new long[length];
        int sz = TMath.min(length, array.length);
        for (int i = 0; i < sz; ++i) {
            result[i] = array[i];
        }
        return result;
    }

    public static float[] copyOf(float[] array, int length) {
        float[] result = new float[length];
        int sz = TMath.min(length, array.length);
        for (int i = 0; i < sz; ++i) {
            result[i] = array[i];
        }
        return result;
    }

    public static double[] copyOf(double[] array, int length) {
        double[] result = new double[length];
        int sz = TMath.min(length, array.length);
        for (int i = 0; i < sz; ++i) {
            result[i] = array[i];
        }
        return result;
    }

    public static boolean[] copyOf(boolean[] array, int length) {
        boolean[] result = new boolean[length];
        int sz = TMath.min(length, array.length);
        for (int i = 0; i < sz; ++i) {
            result[i] = array[i];
        }
        return result;
    }

    public static <T> T[] copyOf(T[] original, int newLength) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(original.getClass().getComponentType(), newLength);
        int sz = TMath.min(newLength, original.length);
        for (int i = 0; i < sz; ++i) {
            result[i] = original[i];
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T, U> T[] copyOf(U[] original, int newLength, Class<? extends T[]> cls) {
        Class<?> componentType = cls.getComponentType();
        T[] result = (T[]) Array.newInstance(componentType, newLength);
        int sz = TMath.min(newLength, original.length);
        for (int i = 0; i < sz; ++i) {
            result[i] = (T) componentType.cast(original[i]);
        }
        return result;
    }

    public static boolean[] copyOfRange(boolean[] array, int from, int to) {
        boolean[] result = new boolean[to - from];
        for (int i = from; i < to; ++i) {
            result[i - from] = array[i];
        }
        return result;
    }

    public static byte[] copyOfRange(byte[] array, int from, int to) {
        byte[] result = new byte[to - from];
        for (int i = from; i < to; ++i) {
            result[i - from] = array[i];
        }
        return result;
    }

    public static char[] copyOfRange(char[] array, int from, int to) {
        char[] result = new char[to - from];
        for (int i = from; i < to; ++i) {
            result[i - from] = array[i];
        }
        return result;
    }

    public static short[] copyOfRange(short[] array, int from, int to) {
        short[] result = new short[to - from];
        for (int i = from; i < to; ++i) {
            result[i - from] = array[i];
        }
        return result;
    }

    public static int[] copyOfRange(int[] array, int from, int to) {
        int[] result = new int[to - from];
        for (int i = from; i < to; ++i) {
            result[i - from] = array[i];
        }
        return result;
    }

    public static long[] copyOfRange(long[] array, int from, int to) {
        long[] result = new long[to - from];
        for (int i = from; i < to; ++i) {
            result[i - from] = array[i];
        }
        return result;
    }

    public static float[] copyOfRange(float[] array, int from, int to) {
        float[] result = new float[to - from];
        for (int i = from; i < to; ++i) {
            result[i - from] = array[i];
        }
        return result;
    }

    public static double[] copyOfRange(double[] array, int from, int to) {
        double[] result = new double[to - from];
        for (int i = from; i < to; ++i) {
            result[i - from] = array[i];
        }
        return result;
    }

    public static <T> T[] copyOfRange(T[] original, int from, int to) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(original.getClass().getComponentType(), to - from);
        for (int i = from; i < to; ++i) {
            result[i - from] = original[i];
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T, U> T[] copyOfRange(U[] original, int from, int to, Class<? extends T[]> newType) {
        Class<?> componentType = newType.getComponentType();
        T[] result = (T[]) (Object) Array.newInstance(componentType, to - from);
        for (int i = from; i < to; ++i) {
            result[i - from] = (T) newType.getComponentType().cast(original[i]);
        }
        return result;
    }

    public static String toString(TObject[] a) {
        if (a == null) {
            return "null";
        }
        TStringBuilder sb = new TStringBuilder();
        sb.append("[");
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toString(boolean[] a) {
        if (a == null) {
            return "null";
        }
        TStringBuilder sb = new TStringBuilder();
        sb.append("[");
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toString(byte[] a) {
        if (a == null) {
            return "null";
        }
        TStringBuilder sb = new TStringBuilder();
        sb.append("[");
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toString(short[] a) {
        if (a == null) {
            return "null";
        }
        TStringBuilder sb = new TStringBuilder();
        sb.append("[");
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toString(char[] a) {
        if (a == null) {
            return "null";
        }
        TStringBuilder sb = new TStringBuilder();
        sb.append("[");
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toString(int[] a) {
        if (a == null) {
            return "null";
        }
        TStringBuilder sb = new TStringBuilder();
        sb.append("[");
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toString(long[] a) {
        if (a == null) {
            return "null";
        }
        TStringBuilder sb = new TStringBuilder();
        sb.append("[");
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toString(float[] a) {
        if (a == null) {
            return "null";
        }
        TStringBuilder sb = new TStringBuilder();
        sb.append("[");
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toString(double[] a) {
        if (a == null) {
            return "null";
        }
        TStringBuilder sb = new TStringBuilder();
        sb.append("[");
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(a[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    public static void fill(long[] a, int fromIndex, int toIndex, long val) {
        if (fromIndex > toIndex) {
            throw new TIllegalArgumentException();
        }
        while (fromIndex < toIndex) {
            a[fromIndex++] = val;
        }
    }

    public static void fill(long[] a, long val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(int[] a, int fromIndex, int toIndex, int val) {
        if (fromIndex > toIndex) {
            throw new TIllegalArgumentException();
        }
        while (fromIndex < toIndex) {
            a[fromIndex++] = val;
        }
    }

    public static void fill(int[] a, int val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(short[] a, int fromIndex, int toIndex, short val) {
        if (fromIndex > toIndex) {
            throw new TIllegalArgumentException();
        }
        while (fromIndex < toIndex) {
            a[fromIndex++] = val;
        }
    }

    public static void fill(short[] a, short val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(char[] a, int fromIndex, int toIndex, char val) {
        if (fromIndex > toIndex) {
            throw new TIllegalArgumentException();
        }
        while (fromIndex < toIndex) {
            a[fromIndex++] = val;
        }
    }

    public static void fill(char[] a, char val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(byte[] a, int fromIndex, int toIndex, byte val) {
        if (fromIndex > toIndex) {
            throw new TIllegalArgumentException();
        }
        while (fromIndex < toIndex) {
            a[fromIndex++] = val;
        }
    }

    public static void fill(byte[] a, byte val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(boolean[] a, int fromIndex, int toIndex, boolean val) {
        if (fromIndex > toIndex) {
            throw new TIllegalArgumentException();
        }
        while (fromIndex < toIndex) {
            a[fromIndex++] = val;
        }
    }

    public static void fill(boolean[] a, boolean val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(float[] a, int fromIndex, int toIndex, float val) {
        if (fromIndex > toIndex) {
            throw new TIllegalArgumentException();
        }
        while (fromIndex < toIndex) {
            a[fromIndex++] = val;
        }
    }

    public static void fill(float[] a, float val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(double[] a, int fromIndex, int toIndex, double val) {
        if (fromIndex > toIndex) {
            throw new TIllegalArgumentException();
        }
        while (fromIndex < toIndex) {
            a[fromIndex++] = val;
        }
    }

    public static void fill(double[] a, double val) {
        fill(a, 0, a.length, val);
    }

    public static void fill(TObject[] a, int fromIndex, int toIndex, TObject val) {
        if (fromIndex > toIndex) {
            throw new TIllegalArgumentException();
        }
        while (fromIndex < toIndex) {
            a[fromIndex++] = val;
        }
    }

    public static void fill(TObject[] a, TObject val) {
        fill(a, 0, a.length, val);
    }

    public static void sort(int[] a, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException();
        }
        int[] subarray = new int[toIndex - fromIndex];
        for (int i = fromIndex; i < toIndex; ++i) {
            subarray[i - fromIndex] = a[i];
        }
        sort(subarray);
        for (int i = fromIndex; i < toIndex; ++i) {
            a[i] = subarray[i - fromIndex];
        }
    }

    public static void sort(int[] a) {
        if (a.length == 0) {
            return;
        }
        int[] first = a;
        int[] second = new int[a.length];
        int chunkSize = 1;
        while (chunkSize < a.length) {
            for (int i = 0; i < first.length; i += chunkSize * 2) {
                merge(first, second, i, Math.min(first.length, i + chunkSize),
                        Math.min(first.length, i + 2 * chunkSize));
            }
            int[] tmp = first;
            first = second;
            second = tmp;
            chunkSize *= 2;
        }
        if (first != a) {
            for (int i = 0; i < first.length; ++i) {
                second[i] = first[i];
            }
        }
    }

    private static void merge(int[] a, int[] b, int from, int split, int to) {
        int index = from;
        int from2 = split;
        while (true) {
            if (from == split) {
                while (from2 < to) {
                    b[index++] = a[from2++];
                }
                break;
            } else if (from2 == to) {
                while (from < split) {
                    b[index++] = a[from++];
                }
                break;
            }
            int p = a[from];
            int q = a[from2];
            if (p <= q) {
                b[index++] = p;
                ++from;
            } else {
                b[index++] = q;
                ++from2;
            }
        }
    }

    public static void sort(long[] a, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException();
        }
        long[] subarray = new long[toIndex - fromIndex];
        for (int i = fromIndex; i < toIndex; ++i) {
            subarray[i - fromIndex] = a[i];
        }
        sort(subarray);
        for (int i = fromIndex; i < toIndex; ++i) {
            a[i] = subarray[i - fromIndex];
        }
    }

    public static void sort(long[] a) {
        if (a.length == 0) {
            return;
        }
        long[] first = a;
        long[] second = new long[a.length];
        int chunkSize = 1;
        while (chunkSize < a.length) {
            for (int i = 0; i < first.length; i += chunkSize * 2) {
                merge(first, second, i, Math.min(first.length, i + chunkSize),
                        Math.min(first.length, i + 2 * chunkSize));
            }
            long[] tmp = first;
            first = second;
            second = tmp;
            chunkSize *= 2;
        }
        if (first != a) {
            for (int i = 0; i < first.length; ++i) {
                second[i] = first[i];
            }
        }
    }

    private static void merge(long[] a, long[] b, int from, int split, int to) {
        int index = from;
        int from2 = split;
        while (true) {
            if (from == split) {
                while (from2 < to) {
                    b[index++] = a[from2++];
                }
                break;
            } else if (from2 == to) {
                while (from < split) {
                    b[index++] = a[from++];
                }
                break;
            }
            long p = a[from];
            long q = a[from2];
            if (p <= q) {
                b[index++] = p;
                ++from;
            } else {
                b[index++] = q;
                ++from2;
            }
        }
    }

    public static void sort(short[] a, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException();
        }
        short[] subarray = new short[toIndex - fromIndex];
        for (int i = fromIndex; i < toIndex; ++i) {
            subarray[i - fromIndex] = a[i];
        }
        sort(subarray);
        for (int i = fromIndex; i < toIndex; ++i) {
            a[i] = subarray[i - fromIndex];
        }
    }

    public static void sort(short[] a) {
        if (a.length == 0) {
            return;
        }
        short[] first = a;
        short[] second = new short[a.length];
        int chunkSize = 1;
        while (chunkSize < a.length) {
            for (int i = 0; i < first.length; i += chunkSize * 2) {
                merge(first, second, i, Math.min(first.length, i + chunkSize),
                        Math.min(first.length, i + 2 * chunkSize));
            }
            short[] tmp = first;
            first = second;
            second = tmp;
            chunkSize *= 2;
        }
        if (first != a) {
            for (int i = 0; i < first.length; ++i) {
                second[i] = first[i];
            }
        }
    }

    private static void merge(short[] a, short[] b, int from, int split, int to) {
        int index = from;
        int from2 = split;
        while (true) {
            if (from == split) {
                while (from2 < to) {
                    b[index++] = a[from2++];
                }
                break;
            } else if (from2 == to) {
                while (from < split) {
                    b[index++] = a[from++];
                }
                break;
            }
            short p = a[from];
            short q = a[from2];
            if (p <= q) {
                b[index++] = p;
                ++from;
            } else {
                b[index++] = q;
                ++from2;
            }
        }
    }

    public static void sort(char[] a, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException();
        }
        char[] subarray = new char[toIndex - fromIndex];
        for (int i = fromIndex; i < toIndex; ++i) {
            subarray[i - fromIndex] = a[i];
        }
        sort(subarray);
        for (int i = fromIndex; i < toIndex; ++i) {
            a[i] = subarray[i - fromIndex];
        }
    }

    public static void sort(char[] a) {
        if (a.length == 0) {
            return;
        }
        char[] first = a;
        char[] second = new char[a.length];
        int chunkSize = 1;
        while (chunkSize < a.length) {
            for (int i = 0; i < first.length; i += chunkSize * 2) {
                merge(first, second, i, Math.min(first.length, i + chunkSize),
                        Math.min(first.length, i + 2 * chunkSize));
            }
            char[] tmp = first;
            first = second;
            second = tmp;
            chunkSize *= 2;
        }
        if (first != a) {
            for (int i = 0; i < first.length; ++i) {
                second[i] = first[i];
            }
        }
    }

    private static void merge(char[] a, char[] b, int from, int split, int to) {
        int index = from;
        int from2 = split;
        while (true) {
            if (from == split) {
                while (from2 < to) {
                    b[index++] = a[from2++];
                }
                break;
            } else if (from2 == to) {
                while (from < split) {
                    b[index++] = a[from++];
                }
                break;
            }
            char p = a[from];
            char q = a[from2];
            if (p <= q) {
                b[index++] = p;
                ++from;
            } else {
                b[index++] = q;
                ++from2;
            }
        }
    }

    public static void sort(byte[] a, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException();
        }
        byte[] subarray = new byte[toIndex - fromIndex];
        for (int i = fromIndex; i < toIndex; ++i) {
            subarray[i - fromIndex] = a[i];
        }
        sort(subarray);
        for (int i = fromIndex; i < toIndex; ++i) {
            a[i] = subarray[i - fromIndex];
        }
    }

    public static void sort(byte[] a) {
        if (a.length == 0) {
            return;
        }
        byte[] first = a;
        byte[] second = new byte[a.length];
        int chunkSize = 1;
        while (chunkSize < a.length) {
            for (int i = 0; i < first.length; i += chunkSize * 2) {
                merge(first, second, i, Math.min(first.length, i + chunkSize),
                        Math.min(first.length, i + 2 * chunkSize));
            }
            byte[] tmp = first;
            first = second;
            second = tmp;
            chunkSize *= 2;
        }
        if (first != a) {
            for (int i = 0; i < first.length; ++i) {
                second[i] = first[i];
            }
        }
    }

    private static void merge(byte[] a, byte[] b, int from, int split, int to) {
        int index = from;
        int from2 = split;
        while (true) {
            if (from == split) {
                while (from2 < to) {
                    b[index++] = a[from2++];
                }
                break;
            } else if (from2 == to) {
                while (from < split) {
                    b[index++] = a[from++];
                }
                break;
            }
            byte p = a[from];
            byte q = a[from2];
            if (p <= q) {
                b[index++] = p;
                ++from;
            } else {
                b[index++] = q;
                ++from2;
            }
        }
    }

    public static void sort(float[] a, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException();
        }
        float[] subarray = new float[toIndex - fromIndex];
        for (int i = fromIndex; i < toIndex; ++i) {
            subarray[i - fromIndex] = a[i];
        }
        sort(subarray);
        for (int i = fromIndex; i < toIndex; ++i) {
            a[i] = subarray[i - fromIndex];
        }
    }

    public static void sort(float[] a) {
        if (a.length == 0) {
            return;
        }
        float[] first = a;
        float[] second = new float[a.length];
        int chunkSize = 1;
        while (chunkSize < a.length) {
            for (int i = 0; i < first.length; i += chunkSize * 2) {
                merge(first, second, i, Math.min(first.length, i + chunkSize),
                        Math.min(first.length, i + 2 * chunkSize));
            }
            float[] tmp = first;
            first = second;
            second = tmp;
            chunkSize *= 2;
        }
        if (first != a) {
            for (int i = 0; i < first.length; ++i) {
                second[i] = first[i];
            }
        }
    }

    private static void merge(float[] a, float[] b, int from, int split, int to) {
        int index = from;
        int from2 = split;
        while (true) {
            if (from == split) {
                while (from2 < to) {
                    b[index++] = a[from2++];
                }
                break;
            } else if (from2 == to) {
                while (from < split) {
                    b[index++] = a[from++];
                }
                break;
            }
            float p = a[from];
            float q = a[from2];
            if (Float.compare(p, q) <= 0) {
                b[index++] = p;
                ++from;
            } else {
                b[index++] = q;
                ++from2;
            }
        }
    }

    public static void sort(double[] a, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException();
        }
        double[] subarray = new double[toIndex - fromIndex];
        for (int i = fromIndex; i < toIndex; ++i) {
            subarray[i - fromIndex] = a[i];
        }
        sort(subarray);
        for (int i = fromIndex; i < toIndex; ++i) {
            a[i] = subarray[i - fromIndex];
        }
    }

    public static void sort(double[] a) {
        if (a.length == 0) {
            return;
        }
        double[] first = a;
        double[] second = new double[a.length];
        int chunkSize = 1;
        while (chunkSize < a.length) {
            for (int i = 0; i < first.length; i += chunkSize * 2) {
                merge(first, second, i, Math.min(first.length, i + chunkSize),
                        Math.min(first.length, i + 2 * chunkSize));
            }
            double[] tmp = first;
            first = second;
            second = tmp;
            chunkSize *= 2;
        }
        if (first != a) {
            for (int i = 0; i < first.length; ++i) {
                second[i] = first[i];
            }
        }
    }

    private static void merge(double[] a, double[] b, int from, int split, int to) {
        int index = from;
        int from2 = split;
        while (true) {
            if (from == split) {
                while (from2 < to) {
                    b[index++] = a[from2++];
                }
                break;
            } else if (from2 == to) {
                while (from < split) {
                    b[index++] = a[from++];
                }
                break;
            }
            double p = a[from];
            double q = a[from2];
            if (Double.compare(p, q) <= 0) {
                b[index++] = p;
                ++from;
            } else {
                b[index++] = q;
                ++from2;
            }
        }
    }

    public static void sort(Object[] a) {
        sort(a, TComparator.NaturalOrder.instance());
    }

    public static void sort(Object[] a, int fromIndex, int toIndex) {
        sort(a, fromIndex, toIndex, TComparator.NaturalOrder.instance());
    }

    public static <T> void sort(T[] a, int fromIndex, int toIndex, TComparator<? super T> c) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException();
        }
        if (c == null) {
            c = TComparator.NaturalOrder.instance();
        }
        @SuppressWarnings("unchecked")
        T[] subarray = (T[]) new Object[toIndex - fromIndex];
        for (int i = fromIndex; i < toIndex; ++i) {
            subarray[i - fromIndex] = a[i];
        }
        sort(subarray, c);
        for (int i = fromIndex; i < toIndex; ++i) {
            a[i] = subarray[i - fromIndex];
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> void sort(T[] a, TComparator<? super T> c) {
        if (a.length == 0) {
            return;
        }
        if (c == null) {
            c = TComparator.NaturalOrder.instance();
        }
        Object[] first = a;
        Object[] second = new Object[a.length];
        int chunkSize = 1;
        while (chunkSize < a.length) {
            for (int i = 0; i < first.length; i += chunkSize * 2) {
                merge(first, second, i, Math.min(first.length, i + chunkSize),
                        Math.min(first.length, i + 2 * chunkSize), (TComparator<Object>) c);
            }
            Object[] tmp = first;
            first = second;
            second = tmp;
            chunkSize *= 2;
        }
        if (first != a) {
            for (int i = 0; i < first.length; ++i) {
                second[i] = first[i];
            }
        }
    }

    private static void merge(Object[] a, Object[] b, int from, int split, int to, TComparator<Object> comp) {
        int index = from;
        int from2 = split;
        while (true) {
            if (from == split) {
                while (from2 < to) {
                    b[index++] = a[from2++];
                }
                break;
            } else if (from2 == to) {
                while (from < split) {
                    b[index++] = a[from++];
                }
                break;
            }
            Object p = a[from];
            Object q = a[from2];
            if (comp.compare(p, q) <= 0) {
                b[index++] = p;
                ++from;
            } else {
                b[index++] = q;
                ++from2;
            }
        }
    }

    public static int binarySearch(int[] a, int key) {
        return binarySearch(a, 0, a.length, key);
    }

    public static int binarySearch(int[] a, int fromIndex, int toIndex, int key) {
        if (fromIndex > toIndex) {
            throw new TIllegalArgumentException();
        }
        int l = fromIndex;
        int u = toIndex - 1;
        while (l <= u) {
            int i = (l + u) / 2;
            int e = a[i];
            if (e == key) {
                return i;
            } else if (key < e) {
                u = i - 1;
            } else {
                l = i + 1;
            }
        }
        return -l - 1;
    }

    public static int binarySearch(long[] a, long key) {
        return binarySearch(a, 0, a.length, key);
    }

    public static int binarySearch(long[] a, int fromIndex, int toIndex, long key) {
        if (fromIndex > toIndex) {
            throw new TIllegalArgumentException();
        }
        int l = fromIndex;
        int u = toIndex - 1;
        while (l <= u) {
            int i = (l + u) / 2;
            long e = a[i];
            if (e == key) {
                return i;
            } else if (e > key) {
                u = i - 1;
            } else {
                l = i + 1;
            }
        }
        return -l - 1;
    }

    public static int binarySearch(short[] a, short key) {
        return binarySearch(a, 0, a.length, key);
    }

    public static int binarySearch(short[] a, int fromIndex, int toIndex, short key) {
        if (fromIndex > toIndex) {
            throw new TIllegalArgumentException();
        }
        int l = fromIndex;
        int u = toIndex - 1;
        while (l <= u) {
            int i = (l + u) / 2;
            short e = a[i];
            if (e == key) {
                return i;
            } else if (e > key) {
                u = i - 1;
            } else {
                l = i + 1;
            }
        }
        return -l - 1;
    }

    public static int binarySearch(char[] a, char key) {
        return binarySearch(a, 0, a.length, key);
    }

    public static int binarySearch(char[] a, int fromIndex, int toIndex, char key) {
        if (fromIndex > toIndex) {
            throw new TIllegalArgumentException();
        }
        int l = fromIndex;
        int u = toIndex - 1;
        while (l <= u) {
            int i = (l + u) / 2;
            char e = a[i];
            if (e == key) {
                return i;
            } else if (e > key) {
                u = i - 1;
            } else {
                l = i + 1;
            }
        }
        return -l - 1;
    }

    public static int binarySearch(byte[] a, byte key) {
        return binarySearch(a, 0, a.length, key);
    }

    public static int binarySearch(byte[] a, int fromIndex, int toIndex, byte key) {
        if (fromIndex > toIndex) {
            throw new TIllegalArgumentException();
        }
        int l = fromIndex;
        int u = toIndex - 1;
        while (l <= u) {
            int i = (l + u) / 2;
            byte e = a[i];
            if (e == key) {
                return i;
            } else if (e > key) {
                u = i - 1;
            } else {
                l = i + 1;
            }
        }
        return -l - 1;
    }

    public static int binarySearch(double[] a, double key) {
        return binarySearch(a, 0, a.length, key);
    }

    public static int binarySearch(double[] a, int fromIndex, int toIndex, double key) {
        if (fromIndex > toIndex) {
            throw new TIllegalArgumentException();
        }
        int l = fromIndex;
        int u = toIndex - 1;
        while (l <= u) {
            int i = (l + u) / 2;
            double e = a[i];
            int cmp = Double.compare(e, key);
            if (cmp < 0) {
                l = i + 1;
            } else if (cmp > 0) {
                u = i - 1;
            } else {
                return i;
            }
        }
        return -l - 1;
    }

    public static int binarySearch(float[] a, float key) {
        return binarySearch(a, 0, a.length, key);
    }

    public static int binarySearch(float[] a, int fromIndex, int toIndex, float key) {
        if (fromIndex > toIndex) {
            throw new TIllegalArgumentException();
        }
        int l = fromIndex;
        int u = toIndex - 1;
        while (l <= u) {
            int i = (l + u) / 2;
            float e = a[i];
            int cmp = Float.compare(e, key);
            if (cmp < 0) {
                l = i + 1;
            } else if (cmp > 0) {
                u = i - 1;
            } else {
                return i;
            }
        }
        return -l - 1;
    }

    public static int binarySearch(Object[] a, Object key) {
        return binarySearch(a, 0, a.length, key);
    }

    public static int binarySearch(Object[] a, int fromIndex, int toIndex, Object key) {
        return binarySearch(a, fromIndex, toIndex, key, TComparator.NaturalOrder.instance());
    }

    public static <T> int binarySearch(T[] a, T key, TComparator<? super T> c) {
        return binarySearch(a, 0, a.length, key, c);
    }

    public static <T> int binarySearch(T[] a, int fromIndex, int toIndex, T key, TComparator<? super T> c) {
        if (c == null) {
            c = TComparator.NaturalOrder.instance();
        }
        if (fromIndex > toIndex) {
            throw new TIllegalArgumentException();
        }
        int l = fromIndex;
        int u = toIndex - 1;
        while (l <= u) {
            int i = (l + u) / 2;
            T e = a[i];
            int cmp = c.compare(key, e);
            if (cmp == 0) {
                return i;
            } else if (cmp < 0) {
                u = i - 1;
            } else {
                l = i + 1;
            }
        }
        return -l - 1;
    }

    private static int mismatchImpl(long[] a, int aStart, long[] a2, int a2Start, int length) {
        for (int i = 0; i < length; ++i) {
            if (a[i + aStart] != a2[i + a2Start]) {
                return i;
            }
        }
        return -1;
    }

    public static int mismatch(long[] a, long[] a2) {
        int length = Math.min(a.length, a2.length);
        if (a == a2) {
            return -1;
        }

        int mismatch = mismatchImpl(a, 0, a2, 0, length);
        return mismatch < 0 && a.length != a2.length ? length : mismatch;
    }

    public static boolean equals(long[] a, long[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length) {
            return false;
        }
        return mismatchImpl(a, 0, a2, 0, a.length) < 0;
    }

    public static int mismatch(long[] a, int aFromIndex, int aToIndex, long[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        int mismatch = mismatchImpl(a, aFromIndex, b, bFromIndex, length);
        return mismatch < 0 && aLength != bLength ? length : mismatch;
    }

    public static boolean equals(long[] a, int aFromIndex, int aToIndex, long[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        return aLength == bLength && mismatchImpl(a, aFromIndex, b, bFromIndex, aLength) < 0;
    }

    private static int mismatchImpl(int[] a, int aStart, int[] a2, int a2Start, int length) {
        for (int i = 0; i < length; ++i) {
            if (a[i + aStart] != a2[i + a2Start]) {
                return i;
            }
        }
        return -1;
    }

    public static int mismatch(int[] a, int[] a2) {
        int length = Math.min(a.length, a2.length);
        if (a == a2) {
            return -1;
        }

        int mismatch = mismatchImpl(a, 0, a2, 0, length);
        return mismatch < 0 && a.length != a2.length ? length : mismatch;
    }

    public static boolean equals(int[] a, int[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length) {
            return false;
        }
        return mismatchImpl(a, 0, a2, 0, a.length) < 0;
    }

    public static int mismatch(int[] a, int aFromIndex, int aToIndex, int[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        int mismatch = mismatchImpl(a, aFromIndex, b, bFromIndex, length);
        return mismatch < 0 && aLength != bLength ? length : mismatch;
    }

    public static boolean equals(int[] a, int aFromIndex, int aToIndex, int[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        return aLength == bLength && mismatchImpl(a, aFromIndex, b, bFromIndex, aLength) < 0;
    }

    private static int mismatchImpl(short[] a, int aStart, short[] a2, int a2Start, int length) {
        for (int i = 0; i < length; ++i) {
            if (a[i + aStart] != a2[i + a2Start]) {
                return i;
            }
        }
        return -1;
    }

    public static int mismatch(short[] a, short[] a2) {
        int length = Math.min(a.length, a2.length);
        if (a == a2) {
            return -1;
        }

        int mismatch = mismatchImpl(a, 0, a2, 0, length);
        return mismatch < 0 && a.length != a2.length ? length : mismatch;
    }

    public static boolean equals(short[] a, short[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length) {
            return false;
        }
        return mismatchImpl(a, 0, a2, 0, a.length) < 0;
    }

    public static int mismatch(short[] a, int aFromIndex, int aToIndex, short[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        int mismatch = mismatchImpl(a, aFromIndex, b, bFromIndex, length);
        return mismatch < 0 && aLength != bLength ? length : mismatch;
    }

    public static boolean equals(short[] a, int aFromIndex, int aToIndex, short[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        return aLength == bLength && mismatchImpl(a, aFromIndex, b, bFromIndex, aLength) < 0;
    }

    private static int mismatchImpl(char[] a, int aStart, char[] a2, int a2Start, int length) {
        for (int i = 0; i < length; ++i) {
            if (a[i + aStart] != a2[i + a2Start]) {
                return i;
            }
        }
        return -1;
    }

    public static int mismatch(char[] a, char[] a2) {
        int length = Math.min(a.length, a2.length);
        if (a == a2) {
            return -1;
        }

        int mismatch = mismatchImpl(a, 0, a2, 0, length);
        return mismatch < 0 && a.length != a2.length ? length : mismatch;
    }

    public static boolean equals(char[] a, char[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length) {
            return false;
        }
        return mismatchImpl(a, 0, a2, 0, a.length) < 0;
    }

    public static int mismatch(char[] a, int aFromIndex, int aToIndex, char[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        int mismatch = mismatchImpl(a, aFromIndex, b, bFromIndex, length);
        return mismatch < 0 && aLength != bLength ? length : mismatch;
    }

    public static boolean equals(char[] a, int aFromIndex, int aToIndex, char[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        return aLength == bLength && mismatchImpl(a, aFromIndex, b, bFromIndex, aLength) < 0;
    }

    private static int mismatchImpl(byte[] a, int aStart, byte[] a2, int a2Start, int length) {
        for (int i = 0; i < length; ++i) {
            if (a[i + aStart] != a2[i + a2Start]) {
                return i;
            }
        }
        return -1;
    }

    public static int mismatch(byte[] a, byte[] a2) {
        int length = Math.min(a.length, a2.length);
        if (a == a2) {
            return -1;
        }

        int mismatch = mismatchImpl(a, 0, a2, 0, length);
        return mismatch < 0 && a.length != a2.length ? length : mismatch;
    }

    public static boolean equals(byte[] a, byte[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length) {
            return false;
        }
        return mismatchImpl(a, 0, a2, 0, a.length) < 0;
    }

    public static int mismatch(byte[] a, int aFromIndex, int aToIndex, byte[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        int mismatch = mismatchImpl(a, aFromIndex, b, bFromIndex, length);
        return mismatch < 0 && aLength != bLength ? length : mismatch;
    }

    public static boolean equals(byte[] a, int aFromIndex, int aToIndex, byte[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        return aLength == bLength && mismatchImpl(a, aFromIndex, b, bFromIndex, aLength) < 0;
    }

    private static int mismatchImpl(float[] a, int aStart, float[] a2, int a2Start, int length) {
        for (int i = 0; i < length; ++i) {
            if (Float.compare(a[i + aStart], a2[i + a2Start]) != 0) {
                return i;
            }
        }
        return -1;
    }

    public static int mismatch(float[] a, float[] a2) {
        int length = Math.min(a.length, a2.length);
        if (a == a2) {
            return -1;
        }

        int mismatch = mismatchImpl(a, 0, a2, 0, length);
        return mismatch < 0 && a.length != a2.length ? length : mismatch;
    }

    public static boolean equals(float[] a, float[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length) {
            return false;
        }
        return mismatchImpl(a, 0, a2, 0, a.length) < 0;
    }

    public static int mismatch(float[] a, int aFromIndex, int aToIndex, float[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        int mismatch = mismatchImpl(a, aFromIndex, b, bFromIndex, length);
        return mismatch < 0 && aLength != bLength ? length : mismatch;
    }

    public static boolean equals(float[] a, int aFromIndex, int aToIndex, float[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        return aLength == bLength && mismatchImpl(a, aFromIndex, b, bFromIndex, aLength) < 0;
    }

    private static int mismatchImpl(double[] a, int aStart, double[] a2, int a2Start, int length) {
        for (int i = 0; i < length; ++i) {
            if (Double.compare(a[i + aStart], a2[i + a2Start]) != 0) {
                return i;
            }
        }
        return -1;
    }

    public static int mismatch(double[] a, double[] a2) {
        int length = Math.min(a.length, a2.length);
        if (a == a2) {
            return -1;
        }

        int mismatch = mismatchImpl(a, 0, a2, 0, length);
        return mismatch < 0 && a.length != a2.length ? length : mismatch;
    }

    public static boolean equals(double[] a, double[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length) {
            return false;
        }
        return mismatchImpl(a, 0, a2, 0, a.length) < 0;
    }

    public static int mismatch(double[] a, int aFromIndex, int aToIndex, double[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        int mismatch = mismatchImpl(a, aFromIndex, b, bFromIndex, length);
        return mismatch < 0 && aLength != bLength ? length : mismatch;
    }

    public static boolean equals(double[] a, int aFromIndex, int aToIndex, double[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        return aLength == bLength && mismatchImpl(a, aFromIndex, b, bFromIndex, aLength) < 0;
    }

    private static int mismatchImpl(boolean[] a, boolean[] a2, int length) {
        for (int i = 0; i < length; ++i) {
            if (a[i] != a2[i]) {
                return i;
            }
        }
        return -1;
    }

    public static int mismatch(boolean[] a, boolean[] a2) {
        int length = Math.min(a.length, a2.length);
        if (a == a2) {
            return -1;
        }

        int mismatch = mismatchImpl(a, a2, length);
        return mismatch < 0 && a.length != a2.length ? length : mismatch;
    }

    public static boolean equals(boolean[] a, boolean[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length) {
            return false;
        }
        return mismatchImpl(a, a2, a.length) < 0;
    }

    private static int mismatchImpl(boolean[] a, int aStart, boolean[] a2, int a2Start, int length) {
        for (int i = 0; i < length; ++i) {
            if (a[i + aStart] != a2[i + a2Start]) {
                return i;
            }
        }
        return -1;
    }

    public static int mismatch(boolean[] a, int aFromIndex, int aToIndex, boolean[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        int mismatch = mismatchImpl(a, aFromIndex, b, bFromIndex, length);
        return mismatch < 0 && aLength != bLength ? length : mismatch;
    }

    public static boolean equals(boolean[] a, int aFromIndex, int aToIndex, boolean[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        return aLength == bLength && mismatchImpl(a, aFromIndex, b, bFromIndex, aLength) < 0;
    }

    private static int mismatchImpl(Object[] a, int aStart, Object[] a2, int a2Start, int length) {
        for (int i = 0; i < length; ++i) {
            if (!Objects.equals(a[i + aStart], a2[i + a2Start])) {
                return i;
            }
        }
        return -1;
    }

    public static int mismatch(Object[] a, Object[] a2) {
        int length = Math.min(a.length, a2.length);
        if (a == a2) {
            return -1;
        }

        int mismatch = mismatchImpl(a, 0, a2, 0, length);
        return mismatch < 0 && a.length != a2.length ? length : mismatch;
    }

    public static boolean equals(Object[] a, Object[] a2) {
        if (a == a2) {
            return true;
        }
        if (a == null || a2 == null || a.length != a2.length) {
            return false;
        }
        return mismatchImpl(a, 0, a2, 0, a.length) < 0;
    }

    public static int mismatch(Object[] a, int aFromIndex, int aToIndex, Object[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        int length = Math.min(aLength, bLength);
        int mismatch = mismatchImpl(a, aFromIndex, b, bFromIndex, length);
        return mismatch < 0 && aLength != bLength ? length : mismatch;
    }

    public static boolean equals(Object[] a, int aFromIndex, int aToIndex, Object[] b, int bFromIndex, int bToIndex) {
        checkInBounds(a.length, aFromIndex, aToIndex);
        checkInBounds(b.length, bFromIndex, bToIndex);

        int aLength = aToIndex - aFromIndex;
        int bLength = bToIndex - bFromIndex;
        return aLength == bLength && mismatchImpl(a, aFromIndex, b, bFromIndex, aLength) < 0;
    }

    public static int hashCode(boolean[] a) {
        if (a == null) {
            return 0;
        }
        int hash = 1;
        for (int i = 0; i < a.length; ++i) {
            hash = 31 * hash + Boolean.hashCode(a[i]);
        }
        return hash;
    }

    public static int hashCode(long[] a) {
        if (a == null) {
            return 0;
        }
        int hash = 1;
        for (int i = 0; i < a.length; ++i) {
            hash = 31 * hash + Long.hashCode(a[i]);
        }
        return hash;
    }

    public static int hashCode(int[] a) {
        if (a == null) {
            return 0;
        }
        int hash = 1;
        for (int i = 0; i < a.length; ++i) {
            hash = 31 * hash + Integer.hashCode(a[i]);
        }
        return hash;
    }

    public static int hashCode(byte[] a) {
        if (a == null) {
            return 0;
        }
        int hash = 1;
        for (int i = 0; i < a.length; ++i) {
            hash = 31 * hash + Byte.hashCode(a[i]);
        }
        return hash;
    }

    public static int hashCode(short[] a) {
        if (a == null) {
            return 0;
        }
        int hash = 1;
        for (int i = 0; i < a.length; ++i) {
            hash = 31 * hash + Short.hashCode(a[i]);
        }
        return hash;
    }

    public static int hashCode(char[] a) {
        if (a == null) {
            return 0;
        }
        int hash = 1;
        for (int i = 0; i < a.length; ++i) {
            hash = 31 * hash + Character.hashCode(a[i]);
        }
        return hash;
    }

    public static int hashCode(float[] a) {
        if (a == null) {
            return 0;
        }
        int hash = 1;
        for (int i = 0; i < a.length; ++i) {
            hash = 31 * hash + Float.hashCode(a[i]);
        }
        return hash;
    }

    public static int hashCode(double[] a) {
        if (a == null) {
            return 0;
        }
        int hash = 1;
        for (int i = 0; i < a.length; ++i) {
            hash = 31 * hash + Double.hashCode(a[i]);
        }
        return hash;
    }

    public static int hashCode(Object[] a) {
        if (a == null) {
            return 0;
        }
        int hash = 1;
        for (int i = 0; i < a.length; ++i) {
            hash = 31 * hash + Objects.hashCode(a[i]);
        }
        return hash;
    }

    public static int deepHashCode(Object[] a) {
        if (a == null) {
            return 0;
        }
        int hash = 1;
        for (int i = 0; i < a.length; ++i) {
            Object el = a[i];
            int h;
            if (a[i] instanceof boolean[]) {
                h = hashCode((boolean[]) el);
            } else if (a[i] instanceof byte[]) {
                h = hashCode((byte[]) el);
            } else if (a[i] instanceof short[]) {
                h = hashCode((short[]) el);
            } else if (a[i] instanceof char[]) {
                h = hashCode((char[]) el);
            } else if (a[i] instanceof int[]) {
                h = hashCode((int[]) el);
            } else if (a[i] instanceof long[]) {
                h = hashCode((long[]) el);
            } else if (a[i] instanceof float[]) {
                h = hashCode((float[]) el);
            } else if (a[i] instanceof double[]) {
                h = hashCode((double[]) el);
            } else if (a[i] instanceof Object[]) {
                h = deepHashCode((Object[]) el);
            } else {
                h = Objects.hashCode(el);
            }
            hash = 31 * hash + h;
        }
        return hash;
    }

    public static boolean deepEquals(Object[] a1, Object[] a2) {
        if (a1 == a2) {
            return true;
        }
        if (a1 == null || a2 == null || a1.length != a2.length) {
            return false;
        }
        for (int i = 0; i < a1.length; ++i) {
            Object e1 = a1[i];
            Object e2 = a2[i];
            if (!TObjects.deepEquals(e1, e2)) {
                return false;
            }
        }
        return true;
    }

    @SafeVarargs
    public static <T> TList<T> asList(final T... a) {
        Objects.requireNonNull(a);
        return new ArrayAsList<>(a);
    }

    static class ArrayAsList<T> extends TAbstractList<T> implements RandomAccess, Serializable {
        private T[] array;

        public ArrayAsList(T[] array) {
            this.array = array;
        }

        @Override public T get(int index) {
            return array[index];
        }
        @Override public T set(int index, T element) {
            T old = array[index];
            array[index] = element;
            return old;
        }
        @Override public int size() {
            return array.length;
        }
    }

    public static String deepToString(Object[] a) {
        if (a == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        deepToString(a, sb, new TArrayList<>());
        return sb.toString();
    }

    private static void deepToString(Object[] a, StringBuilder out, TList<Object[]> visited) {
        out.append('[');
        if (visited.contains(a)) {
            out.append("...");
        } else {
            visited.add(a);
            if (a.length > 0) {
                deepToString(a[0], out, visited);
                for (int i = 1; i < a.length; ++i) {
                    out.append(", ");
                    deepToString(a[i], out, visited);
                }
            }
            visited.remove(visited.size() - 1);
        }
        out.append(']');
    }

    private static void deepToString(Object a, StringBuilder out, TList<Object[]> visited) {
        if (a instanceof Object[]) {
            deepToString((Object[]) a, out, visited);
        } else if (a instanceof boolean[]) {
            out.append(toString((boolean[]) a));
        } else if (a instanceof byte[]) {
            out.append(toString((byte[]) a));
        } else if (a instanceof short[]) {
            out.append(toString((short[]) a));
        } else if (a instanceof char[]) {
            out.append(toString((char[]) a));
        } else if (a instanceof int[]) {
            out.append(toString((int[]) a));
        } else if (a instanceof long[]) {
            out.append(toString((long[]) a));
        } else if (a instanceof float[]) {
            out.append(toString((float[]) a));
        } else if (a instanceof double[]) {
            out.append(toString((double[]) a));
        } else {
            out.append(a);
        }
    }

    public static <T> TStream<T> stream(T[] array) {
        return new TArrayStreamImpl<>(array, 0, array.length);
    }

    public static <T> TStream<T> stream(T[] array, int startInclusive, int endExclusive) {
        checkInBounds(array.length, startInclusive, endExclusive);
        return new TArrayStreamImpl<>(array, startInclusive, endExclusive);
    }

    public static TIntStream stream(int[] array) {
        return new TArrayIntStreamImpl(array, 0, array.length);
    }

    public static TIntStream stream(int[] array, int startInclusive, int endExclusive) {
        checkInBounds(array.length, startInclusive, endExclusive);
        return new TArrayIntStreamImpl(array, startInclusive, endExclusive);
    }

    public static TLongStream stream(long[] array) {
        return new TArrayLongStreamImpl(array, 0, array.length);
    }

    public static TLongStream stream(long[] array, int startInclusive, int endExclusive) {
        checkInBounds(array.length, startInclusive, endExclusive);
        return new TArrayLongStreamImpl(array, startInclusive, endExclusive);
    }

    public static TDoubleStream stream(double[] array) {
        return new TArrayDoubleStreamImpl(array, 0, array.length);
    }

    public static TDoubleStream stream(double[] array, int startInclusive, int endExclusive) {
        checkInBounds(array.length, startInclusive, endExclusive);
        return new TArrayDoubleStreamImpl(array, startInclusive, endExclusive);
    }

    public static <T> void setAll(T[] array, IntFunction<? extends T> generator) {
        for (int i = 0; i < array.length; ++i) {
            array[i] = generator.apply(i);
        }
    }

    public static void setAll(int[] array, IntUnaryOperator generator) {
        for (int i = 0; i < array.length; ++i) {
            array[i] = generator.applyAsInt(i);
        }
    }

    public static void setAll(long[] array, IntToLongFunction generator) {
        for (int i = 0; i < array.length; ++i) {
            array[i] = generator.applyAsLong(i);
        }
    }

    public static void setAll(double[] array, IntToDoubleFunction generator) {
        for (int i = 0; i < array.length; ++i) {
            array[i] = generator.applyAsDouble(i);
        }
    }

    private static void checkInBounds(int length, int startInclusive, int endExclusive) {
        if (startInclusive < 0 || endExclusive < startInclusive || endExclusive > length) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }
}
