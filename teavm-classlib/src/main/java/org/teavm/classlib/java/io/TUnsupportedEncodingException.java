package org.teavm.classlib.java.io;

import org.teavm.classlib.java.lang.TString;

/**
 *
 * @author Alexey Andreev
 */
public class TUnsupportedEncodingException extends TIOException {
    private static final long serialVersionUID = 2403781130729330252L;

    public TUnsupportedEncodingException() {
        super();
    }

    public TUnsupportedEncodingException(TString message) {
        super(message);
    }
}
