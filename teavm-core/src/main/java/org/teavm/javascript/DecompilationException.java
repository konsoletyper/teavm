package org.teavm.javascript;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class DecompilationException extends RuntimeException {
    private static final long serialVersionUID = -1400142974526572669L;

    public DecompilationException() {
        super();
    }

    public DecompilationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DecompilationException(String message) {
        super(message);
    }

    public DecompilationException(Throwable cause) {
        super(cause);
    }
}
