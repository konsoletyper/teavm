package org.teavm.classlib.java.io;

import org.teavm.classlib.java.lang.TException;
import org.teavm.classlib.java.lang.TString;
import org.teavm.classlib.java.lang.TThrowable;

/**
 *
 * @author Alexey Andreev
 */
public class TIOException extends TException {
    private static final long serialVersionUID = 3626109154700059455L;

    public TIOException() {
        super();
    }

    public TIOException(TString message, TThrowable cause) {
        super(message, cause);
    }

    public TIOException(TString message) {
        super(message);
    }

    public TIOException(TThrowable cause) {
        super(cause);
    }
}
