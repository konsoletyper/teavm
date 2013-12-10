package org.teavm.common;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface Mapper<T, R> {
    R map(T preimage);
}
