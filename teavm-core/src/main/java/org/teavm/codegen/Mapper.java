package org.teavm.codegen;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public interface Mapper<T, R> {
    R map(T preimage);
}
