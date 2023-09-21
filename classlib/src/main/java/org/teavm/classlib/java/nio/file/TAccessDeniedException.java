package org.teavm.classlib.java.nio.file;

public class TAccessDeniedException extends TFileSystemException {
    private static final long serialVersionUID = 6428707745039365878L;

    public TAccessDeniedException(String file) {
        super(file);
    }

    public TAccessDeniedException(String file, String other, String reason) {
        super(file, other, reason);
    }
}
