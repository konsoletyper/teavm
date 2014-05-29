package org.teavm.platform.metadata;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface ResourceArray<T> {
    int size();

    T get(int i);

    void add(T elem);
}
