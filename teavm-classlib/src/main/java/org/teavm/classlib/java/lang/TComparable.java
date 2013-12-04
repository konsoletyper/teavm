package org.teavm.classlib.java.lang;

/**
 *
 * @author Alexey Andreev
 */
public interface TComparable<T extends TComparable<T>> {
    int compareTo(T other);
}
