package org.teavm.classlib.java.lang;

/**
 *
 * @author Alexey Andreev
 */
public class TUnsupportedOperationException extends TRuntimeException {
    private static final long serialVersionUID = 3820374864664609707L;

    public TUnsupportedOperationException() {
        super();
    }

    public TUnsupportedOperationException(TString message, TThrowable cause) {
        super(message, cause);
    }

    public TUnsupportedOperationException(TString message) {
        super(message);
    }

    public TUnsupportedOperationException(TThrowable cause) {
        super(cause);
    }
}
