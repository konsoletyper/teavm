package org.teavm.classlib.java.nio.channels;

import org.teavm.classlib.java.nio.TByteBuffer;

import java.io.IOException;

public interface TReadableByteChannel extends TChannel {
    int read(TByteBuffer dst) throws IOException;
}
