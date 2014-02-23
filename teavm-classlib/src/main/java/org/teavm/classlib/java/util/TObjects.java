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

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
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
}
