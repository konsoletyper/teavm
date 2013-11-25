package org.teavm.classlib.java.lang;

/**
 *
 * @author Alexey Andreev
 */
public class TException extends TThrowable {
    private static final long serialVersionUID = -2188339106250208952L;

    public TException() {
        super();
    }

    public TException(TString message, TThrowable cause) {
        super(message, cause);
    }

    public TException(TString message) {
        super(message);
    }

    public TException(TThrowable cause) {
        super(cause);
    }
}
