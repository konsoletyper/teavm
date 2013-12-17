package org.teavm.classlib.java.lang.io;

/**
 *
 * @author Alexey Andreev
 */
public class TFilterOutputStream extends TOutputStream {
    protected TOutputStream out;

    public TFilterOutputStream(TOutputStream out) {
        this.out = out;
    }

    @Override
    public void write(int b) throws TIOException {
        out.write(b);
    }

    @Override
    public void close() throws TIOException {
        try {
            out.flush();
        } catch (TIOException e) {
            // do nothing
        }
        close();
    }

    @Override
    public void flush() throws TIOException {
        out.flush();
    }
}
