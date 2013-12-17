package org.teavm.classlib.java.lang;

/**
 *
 * @author Alexey Andreev
 */
public interface TAutoCloseable {
    void close() throws TException;
}
