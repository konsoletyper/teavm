package org.teavm.classlib.java.nio.file;

import java.io.IOException;

public class TFileSystemException extends IOException {
    private static final long serialVersionUID = 9093298181737952280L;

    private final String file;
    private final String other;
    private final String reason;

    public TFileSystemException(String file) {
        this.file = file;
        this.other = null;
        this.reason = null;
    }

    public TFileSystemException(String file, String other, String reason) {
        super(reason);
        this.file = file;
        this.other = other;
        this.reason = reason;
    }

    public String getFile() {
        return file;
    }

    public String getOtherFile() {
        return other;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String getMessage() {
        if (file == null && other == null) return reason;

        StringBuilder out = new StringBuilder();
        if (file != null) out.append(file);
        if (other != null) out.append(" -> ").append(other);
        if (reason != null) out.append(": ").append(reason);
        return out.toString();
    }
}
