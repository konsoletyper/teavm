package org.teavm.classlib.java.nio.channels;

import java.io.IOException;

public interface TSeekableByteChannel extends TByteChannel {
    long position();

    TSeekableByteChannel position(long newPosition) throws IOException;

    long size() throws IOException;

    TSeekableByteChannel truncate(long size) throws IOException;
}
