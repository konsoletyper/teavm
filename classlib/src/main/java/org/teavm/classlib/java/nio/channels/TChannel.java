package org.teavm.classlib.java.nio.channels;

import org.teavm.classlib.java.io.TCloseable;

import java.io.IOException;

public interface TChannel extends TCloseable {
    boolean isOpen();

    void close() throws IOException;
}
