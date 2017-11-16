/*
 *  Copyright 2017 Alexey Andreev.
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
import java.io.InputStream;
import java.io.InterruptedIOException;

public class TPipedInputStream extends InputStream {
    private Thread lastReader;
    private Thread lastWriter;
    private boolean isClosed;

    protected byte[] buffer;
    protected int in = -1;
    protected int out;
    protected static final int PIPE_SIZE = 1024;

    boolean isConnected;

    public TPipedInputStream() {
        /* empty */
    }
    public TPipedInputStream(TPipedOutputStream out) throws IOException {
        connect(out);
    }

    @Override
    public int available() throws IOException {
        if (buffer == null || in == -1) {
            return 0;
        }
        return in <= out ? buffer.length - out + in : in - out;
    }

    @Override
    public void close() throws IOException {
        /* No exception thrown if already closed */
        if (buffer != null) {
            /* Release buffer to indicate closed. */
            buffer = null;
        }
    }

    public void connect(TPipedOutputStream src) throws IOException {
        src.connect(this);
    }

    @Override
    public synchronized int read() throws IOException {
        if (!isConnected) {
            throw new IOException("Not connected");
        }
        if (buffer == null) {
            throw new IOException("InputStream is closed");
        }

        if (isClosed && in == -1) {
            // write end closed and no more need to read
            return -1;
        }

        if (lastWriter != null && !lastWriter.isAlive() && in < 0) {
            throw new IOException("Write end dead");
        }
        /*
         * Set the last thread to be reading on this PipedInputStream. If
         * lastReader dies while someone is waiting to write an IOException of
         * "Pipe broken" will be thrown in receive()
         */
        lastReader = Thread.currentThread();
        try {
            int attempts = 3;
            while (in == -1) {
                // Are we at end of stream?
                if (isClosed) {
                    return -1;
                }
                if ((attempts-- <= 0) && lastWriter != null && !lastWriter.isAlive()) {
                    throw new IOException("Pipe broken");
                }
                // Notify callers of receive()
                notifyAll();
                wait(1000);
            }
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }

        byte result = buffer[out++];
        if (out == buffer.length) {
            out = 0;
        }
        if (out == in) {
            // empty buffer
            in = -1;
            out = 0;
        }
        return result & 0xff;
    }

    @Override
    public synchronized int read(byte[] bytes, int offset, int count) throws IOException {
        if (bytes == null) {
            throw new NullPointerException();
        }

        if (offset < 0 || offset > bytes.length || count < 0
                || count > bytes.length - offset) {
            throw new IndexOutOfBoundsException();
        }

        if (count == 0) {
            return 0;
        }

        if (isClosed && in == -1) {
            // write end closed and no more need to read
            return -1;
        }

        if (!isConnected) {
            throw new IOException("Not connected");
        }

        if (buffer == null) {
            throw new IOException("InputStream is closed");
        }

        if (lastWriter != null && !lastWriter.isAlive() && (in < 0)) {
            throw new IOException("Write end dead");
        }

        /*
         * Set the last thread to be reading on this PipedInputStream. If
         * lastReader dies while someone is waiting to write an IOException of
         * "Pipe broken" will be thrown in receive()
         */
        lastReader = Thread.currentThread();
        try {
            int attempts = 3;
            while (in == -1) {
                // Are we at end of stream?
                if (isClosed) {
                    return -1;
                }
                if ((attempts-- <= 0) && lastWriter != null && !lastWriter.isAlive()) {
                    throw new IOException("Pipe broken");
                }
                // Notify callers of receive()
                notifyAll();
                wait(1000);
            }
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }

        int copyLength = 0;
        /* Copy bytes from out to end of buffer first */
        if (out >= in) {
            copyLength = count > (buffer.length - out) ? buffer.length - out : count;
            System.arraycopy(buffer, out, bytes, offset, copyLength);
            out += copyLength;
            if (out == buffer.length) {
                out = 0;
            }
            if (out == in) {
                // empty buffer
                in = -1;
                out = 0;
            }
        }

        if (copyLength == count || in == -1) {
            return copyLength;
        }

        int bytesCopied = copyLength;
        /* Copy bytes from 0 to the number of available bytes */
        copyLength = in - out > (count - bytesCopied) ? count - bytesCopied : in - out;
        System.arraycopy(buffer, out, bytes, offset + bytesCopied, copyLength);
        out += copyLength;
        if (out == in) {
            // empty buffer
            in = -1;
            out = 0;
        }
        return bytesCopied + copyLength;
    }

    protected synchronized void receive(int oneByte) throws IOException {
        if (buffer == null || isClosed) {
            throw new IOException();
        }
        if (lastReader != null && !lastReader.isAlive()) {
            throw new IOException();
        }
        /*
         * Set the last thread to be writing on this PipedInputStream. If
         * lastWriter dies while someone is waiting to read an IOException of
         * "Pipe broken" will be thrown in read()
         */
        lastWriter = Thread.currentThread();
        try {
            while (buffer != null && out == in) {
                notifyAll();
                wait(1000);
                if (lastReader != null && !lastReader.isAlive()) {
                    throw new IOException();
                }
            }
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
        if (buffer != null) {
            if (in == -1) {
                in = 0;
            }
            buffer[in++] = (byte) oneByte;
            if (in == buffer.length) {
                in = 0;
            }
        }
    }

    synchronized void done() {
        isClosed = true;
        notifyAll();
    }
}
