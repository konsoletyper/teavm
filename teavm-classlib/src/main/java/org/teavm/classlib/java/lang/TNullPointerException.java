package org.teavm.classlib.java.lang;

/**
 *
 * @author Alexey Andreev
 */
public class TNullPointerException extends TRuntimeException {
    private static final long serialVersionUID = 2639861320773057190L;

    public TNullPointerException(TString message) {
        super(message);
    }

    public TNullPointerException() {
        super();
    }
}
