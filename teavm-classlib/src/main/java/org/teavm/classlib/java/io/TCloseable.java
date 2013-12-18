package org.teavm.classlib.java.io;

import org.teavm.classlib.java.lang.TAutoCloseable;

/**
 *
 * @author Alexey Andreev
 */
public interface TCloseable extends TAutoCloseable {
    @Override
    void close() throws TIOException;
}
