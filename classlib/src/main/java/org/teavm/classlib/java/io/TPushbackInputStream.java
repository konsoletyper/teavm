/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.java.io;

import java.io.IOException;
import org.teavm.classlib.java.lang.TArrayIndexOutOfBoundsException;

public class TPushbackInputStream extends TFilterInputStream {
    protected byte[] buf;
    protected int pos;

    public TPushbackInputStream(TInputStream in) {
        super(in);
        buf = (in == null) ? null : new byte[1];
        pos = 1;
    }

    public TPushbackInputStream(TInputStream in, int size) {
        super(in);
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        buf = in == null ? null : new byte[size];
        pos = size;
    }

    @Override
    public int available() throws IOException {
        if (buf == null) {
            throw new IOException();
        }
        return buf.length - pos + in.available();
    }

    @Override
    public void close() throws IOException {
        if (in != null) {
            in.close();
            in = null;
            buf = null;
        }
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read() throws IOException {
        if (buf == null) {
            throw new IOException();
        }
        // Is there a pushback byte available?
        if (pos < buf.length) {
            return buf[pos++] & 0xFF;
        }
        // Assume read() in the InputStream will return low-order byte or -1
        // if end of stream.
        return in.read();
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (buf == null) {
            throw new IOException("Stream is closed");
        }
        // Force buffer null check first!
        if (offset > buffer.length || offset < 0) {
            throw new TArrayIndexOutOfBoundsException("Offset out of bounds: " + offset);
        }
        if (length < 0 || length > buffer.length - offset) {
            throw new TArrayIndexOutOfBoundsException("Length out of bounds: " + length);
        }

        int copiedBytes = 0;
        int copyLength = 0;
        int newOffset = offset;
        // Are there pushback bytes available?
        if (pos < buf.length) {
            copyLength = (buf.length - pos >= length) ? length : buf.length - pos;
            System.arraycopy(buf, pos, buffer, newOffset, copyLength);
            newOffset += copyLength;
            copiedBytes += copyLength;
            // Use up the bytes in the local buffer
            pos += copyLength;
        }
        // Have we copied enough?
        if (copyLength == length) {
            return length;
        }
        int inCopied = in.read(buffer, newOffset, length - copiedBytes);
        if (inCopied > 0) {
            return inCopied + copiedBytes;
        }
        if (copiedBytes == 0) {
            return inCopied;
        }
        return copiedBytes;
    }

    @Override
    public long skip(long count) throws IOException {
        if (in == null) {
            throw new IOException();
        }
        if (count <= 0) {
            return 0;
        }
        int numSkipped = 0;
        if (pos < buf.length) {
            numSkipped += (count < buf.length - pos) ? count : buf.length - pos;
            pos += numSkipped;
        }
        if (numSkipped < count) {
            numSkipped += in.skip(count - numSkipped);
        }
        return numSkipped;
    }

    public void unread(byte[] buffer) throws IOException {
        unread(buffer, 0, buffer.length);
    }

    public void unread(byte[] buffer, int offset, int length) throws IOException {
        if (length > pos) {
            throw new IOException("Pushback buffer full");
        }
        if (offset > buffer.length || offset < 0) {
            throw new TArrayIndexOutOfBoundsException("Offset out of bounds: " + offset);
        }
        if (length < 0 || length > buffer.length - offset) {
            throw new TArrayIndexOutOfBoundsException("Length out of bounds: " + length);
        }
        if (buf == null) {
            throw new IOException("Stream is closed");
        }
        System.arraycopy(buffer, offset, buf, pos - length, length);
        pos = pos - length;
    }

    public void unread(int oneByte) throws IOException {
        if (buf == null) {
            throw new IOException();
        }
        if (pos == 0) {
            throw new IOException();
        }
        buf[--pos] = (byte) oneByte;
    }

    @Override
    public void mark(int readlimit) {
    }

    @Override
    public void reset() throws IOException {
        throw new IOException();
    }
}
