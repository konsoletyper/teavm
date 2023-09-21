package org.teavm.classlib.java.nio.channels;

import org.teavm.classlib.java.nio.TByteBuffer;

import java.io.IOException;

public interface TWritableByteChannel extends TChannel {
    int write(TByteBuffer src) throws IOException;
}
