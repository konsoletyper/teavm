package org.teavm.classlib.java.lang;

import org.teavm.javascript.ni.Superclass;

/**
 *
 * @author Alexey Andreev
 */
@Superclass("java.lang.Exception")
public class TRuntimeException extends TException {
    private static final long serialVersionUID = 3506083061304642891L;

    public TRuntimeException() {
        super();
    }

    public TRuntimeException(TString message, TThrowable cause) {
        super(message, cause);
    }

    public TRuntimeException(TString message) {
        super(message);
    }

    public TRuntimeException(TThrowable cause) {
        super(cause);
    }
}
