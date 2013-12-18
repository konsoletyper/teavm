package org.teavm.classlib.java.lang;

import org.teavm.classlib.java.io.TIOException;
import org.teavm.classlib.java.io.TOutputStream;
import org.teavm.javascript.ni.GeneratedBy;

/**
 *
 * @author Alexey Andreev
 */
class TConsoleOutputStream_stderr extends TOutputStream {
    @Override
    @GeneratedBy(ConsoleOutputStreamGenerator.class)
    public native void write(int b) throws TIOException;
}
