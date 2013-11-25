package org.teavm.classlib.java.lang;

/**
 *
 * @author Alexey Andreev
 */
public class TIllegalStateException extends TException {
    private static final long serialVersionUID = 218741044430713159L;

    public TIllegalStateException() {
        super();
    }

    public TIllegalStateException(TString message, TThrowable cause) {
        super(message, cause);
    }

    public TIllegalStateException(TString message) {
        super(message);
    }

    public TIllegalStateException(TThrowable cause) {
        super(cause);
    }
}
