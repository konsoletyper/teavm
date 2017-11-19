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

import org.teavm.classlib.java.lang.TObject;

public final class TObjects extends TObject {
    public static boolean equals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        return a == null ? b == null : a.equals(b);
    }

    public static int hashCode(Object o) {
        return o == null ? 0 : o.hashCode();
    }

    public static String toString(Object o) {
        return toString(o, "null");
    }

    public static String toString(Object o, String nullDefault) {
        return o != null ? o.toString() : nullDefault;
    }

    public static <T> int compare(T a, T b, TComparator<? super T> c) {
        return a == null && b == null ? 0 : c.compare(a, b);
    }

    public static <T> T requireNonNull(T obj) {
        return requireNonNull(obj, "");
    }

    public static <T> T requireNonNull(T obj, String message) {
        if (obj == null) {
            throw new NullPointerException(message);
        }
        return obj;
    }

    public static boolean nonNull(Object obj) {
        return obj != null;
    }

    public static boolean deepEquals(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null) {
            return b == null;
        }
        if (a instanceof boolean[]) {
            return b instanceof boolean[] && TArrays.equals((boolean[]) a, (boolean[]) b);
        } else if (b instanceof boolean[]) {
            return false;
        } else if (a instanceof byte[]) {
            return b instanceof byte[] && TArrays.equals((byte[]) a, (byte[]) b);
        } else if (b instanceof byte[]) {
            return false;
        } else if (a instanceof short[]) {
            return b instanceof short[] && TArrays.equals((short[]) a, (short[]) b);
        } else if (b instanceof short[]) {
            return false;
        } else if (a instanceof int[]) {
            return b instanceof int[] && TArrays.equals((int[]) a, (int[]) b);
        } else if (b instanceof int[]) {
            return false;
        } else if (a instanceof char[]) {
            return b instanceof char[] && TArrays.equals((char[]) a, (char[]) b);
        } else if (b instanceof char[]) {
            return false;
        } else if (a instanceof float[]) {
            return b instanceof float[] && TArrays.equals((float[]) a, (float[]) b);
        } else if (b instanceof float[]) {
            return false;
        } else if (a instanceof double[]) {
            return b instanceof double[] && TArrays.equals((double[]) a, (double[]) b);
        } else if (b instanceof double[]) {
            return false;
        } else if (a instanceof Object[]) {
            return b instanceof Object[] && TArrays.deepEquals((Object[]) a, (Object[]) b);
        } else if (b instanceof Object[]) {
            return false;
        } else {
            return a.equals(b);
        }
    }

    public static int hash(Object... values) {
        return TArrays.hashCode(values);
    }
}
