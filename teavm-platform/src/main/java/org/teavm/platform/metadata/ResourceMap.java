package org.teavm.platform.metadata;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface ResourceMap<T> {
    T get(String key);

    void put(String key, T value);
}
