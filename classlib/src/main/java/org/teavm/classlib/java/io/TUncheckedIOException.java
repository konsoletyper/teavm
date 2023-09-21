package org.teavm.classlib.java.io;

import java.io.IOException;

public class TUncheckedIOException extends RuntimeException {
    private static final long serialVersionUID = 1645785175445590213L;

    public TUncheckedIOException(IOException cause) {
        super(cause);
    }

    public TUncheckedIOException(String message, IOException cause) {
        super(message, cause);
    }
}
