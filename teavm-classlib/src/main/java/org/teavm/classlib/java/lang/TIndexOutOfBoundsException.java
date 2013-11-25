package org.teavm.classlib.java.lang;

/**
 *
 * @author Alexey Andreev
 */
public class TIndexOutOfBoundsException extends TRuntimeException {
    private static final long serialVersionUID = -7329782331640782287L;

    public TIndexOutOfBoundsException() {
        super();
    }

    public TIndexOutOfBoundsException(TString message) {
        super(message);
    }
}
