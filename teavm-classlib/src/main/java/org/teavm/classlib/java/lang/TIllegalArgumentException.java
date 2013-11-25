package org.teavm.classlib.java.lang;

/**
 *
 * @author Alexey Andreev
 */
public class TIllegalArgumentException extends TRuntimeException {
    private static final long serialVersionUID = -1225768288500984373L;

    public TIllegalArgumentException() {
        super();
    }

    public TIllegalArgumentException(TString message, TThrowable cause) {
        super(message, cause);
    }

    public TIllegalArgumentException(TString message) {
        super(message);
    }

    public TIllegalArgumentException(TThrowable cause) {
        super(cause);
    }
}
