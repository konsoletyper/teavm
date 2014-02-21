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

import org.teavm.classlib.java.lang.*;

/**
 *
 * @author Alexey Andreev
 */
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

    public static <T> T[] copyOf(T[] original, int newLength) {
        @SuppressWarnings("unchecked")
        T[] result = (T[])new Object[newLength];
        int sz = TMath.min(newLength, original.length);
        for (int i = 0; i < sz; ++i) {
            result[i] = original[i];
        }
        return result;
    }

    public static TString toString(TObject[] a) {
        TStringBuilder sb = new TStringBuilder();
        sb.append(TString.wrap("["));
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(TString.wrap(", "));
            }
            sb.append(a[i]);
        }
        sb.append(TString.wrap("]"));
        return TString.wrap(sb.toString());
    }

    public static TString toString(boolean[] a) {
        TStringBuilder sb = new TStringBuilder();
        sb.append(TString.wrap("["));
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(TString.wrap(", "));
            }
            sb.append(a[i]);
        }
        sb.append(TString.wrap("]"));
        return TString.wrap(sb.toString());
    }

    public static TString toString(byte[] a) {
        TStringBuilder sb = new TStringBuilder();
        sb.append(TString.wrap("["));
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(TString.wrap(", "));
            }
            sb.append(a[i]);
        }
        sb.append(TString.wrap("]"));
        return TString.wrap(sb.toString());
    }

    public static TString toString(short[] a) {
        TStringBuilder sb = new TStringBuilder();
        sb.append(TString.wrap("["));
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(TString.wrap(", "));
            }
            sb.append(a[i]);
        }
        sb.append(TString.wrap("]"));
        return TString.wrap(sb.toString());
    }

    public static TString toString(char[] a) {
        TStringBuilder sb = new TStringBuilder();
        sb.append(TString.wrap("["));
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(TString.wrap(", "));
            }
            sb.append(a[i]);
        }
        sb.append(TString.wrap("]"));
        return TString.wrap(sb.toString());
    }

    public static TString toString(int[] a) {
        TStringBuilder sb = new TStringBuilder();
        sb.append(TString.wrap("["));
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(TString.wrap(", "));
            }
            sb.append(a[i]);
        }
        sb.append(TString.wrap("]"));
        return TString.wrap(sb.toString());
    }

    public static TString toString(long[] a) {
        TStringBuilder sb = new TStringBuilder();
        sb.append(TString.wrap("["));
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(TString.wrap(", "));
            }
            sb.append(a[i]);
        }
        sb.append(TString.wrap("]"));
        return TString.wrap(sb.toString());
    }

    public static TString toString(float[] a) {
        TStringBuilder sb = new TStringBuilder();
        sb.append(TString.wrap("["));
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(TString.wrap(", "));
            }
            sb.append(a[i]);
        }
        sb.append(TString.wrap("]"));
        return TString.wrap(sb.toString());
    }

    public static TString toString(double[] a) {
        TStringBuilder sb = new TStringBuilder();
        sb.append(TString.wrap("["));
        for (int i = 0; i < a.length; ++i) {
            if (i > 0) {
                sb.append(TString.wrap(", "));
            }
            sb.append(a[i]);
        }
        sb.append(TString.wrap("]"));
        return TString.wrap(sb.toString());
    }

    public static void fill(TObject[] a, int fromIndex, int toIndex, TObject val) {
        if (fromIndex > toIndex) {
            throw new TIllegalArgumentException();
        }
        if (fromIndex < 0 || toIndex > a.length) {
            throw new TIndexOutOfBoundsException();
        }
        while (fromIndex < toIndex) {
            a[fromIndex++] = val;
        }
    }

    public static void fill(TObject[] a, TObject val) {
        fill(a, 0, a.length, val);
    }


    public static void sort(Object[] a) {
        sort(a, new NaturalOrder());
    }

    public static void sort(Object[] a, int fromIndex, int toIndex) {
        sort(a, fromIndex, toIndex, new NaturalOrder());
    }

    private static class NaturalOrder implements TComparator<Object> {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override public int compare(Object o1, Object o2) {
            if (o1 != null) {
                return ((TComparable)o1).compareTo((TComparable)o2);
            } else if (o2 != null) {
                return ((TComparable)o2).compareTo((TComparable)o1);
            } else {
                return 0;
            }
        }
    }

    public static <T> void sort(T[] a, int fromIndex, int toIndex, TComparator<? super T> c) {
        @SuppressWarnings("unchecked")
        T[] subarray = (T[])new Object[toIndex - fromIndex];
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
        Object[] first = a;
        Object[] second = new Object[a.length];
        int chunkSize = 1;
        while (chunkSize < a.length) {
            for (int i = 0; i < first.length; i += chunkSize * 2) {
                merge(first, second, i, Math.min(first.length, i + chunkSize),
                        Math.min(first.length, i + 2 * chunkSize), (TComparator<Object>)c);
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

    @SafeVarargs
    public static <T> TList<T> asList(final T... a) {
        return new TAbstractList<T>() {
            @Override public T get(int index) {
                return a[index];
            }
            @Override public int size() {
                return a.length;
            }
        };
    }
}
