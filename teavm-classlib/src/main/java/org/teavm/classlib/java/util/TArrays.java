package org.teavm.classlib.java.util;

import org.teavm.classlib.java.lang.TObject;

/**
 *
 * @author Alexey Andreev
 */
public class TArrays extends TObject {
    public static char[] copyOf(char[] array, int length) {
        char[] result = new char[length];
        int sz = Math.min(length, array.length);
        for (int i = 0; i < sz; ++i) {
            result[i] = array[i];
        }
        return result;
    }
}
