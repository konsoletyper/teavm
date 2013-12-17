package org.teavm.classlib.java.lang.io;

import org.teavm.classlib.java.lang.TObject;

/**
 *
 * @author Alexey Andreev
 */
public abstract class TOutputStream extends TObject implements TCloseable, TFlushable {
    public abstract void write(int b) throws TIOException;

    public void write(byte[] b) throws TIOException {
        write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws TIOException {
        for (int i = 0; i < len; ++i) {
            write(b[off++]);
        }
    }

    @Override
    public void close() throws TIOException {
    }

    @Override
    public void flush() throws TIOException {
    }
}
