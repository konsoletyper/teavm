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

package org.teavm.classlib.java.util.zip;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TInflaterInputStream extends FilterInputStream {
    protected TInflater inf;
    protected byte[] buf;
    protected int len;
    boolean closed;
    boolean eof;
    static final int BUF_SIZE = 512;

    public TInflaterInputStream(InputStream is) {
        this(is, new TInflater(), BUF_SIZE);
    }

    public TInflaterInputStream(InputStream is, TInflater inf) {
        this(is, inf, BUF_SIZE);
    }

    public TInflaterInputStream(InputStream is, TInflater inf, int bsize) {
        super(is);
        if (is == null || inf == null) {
            throw new NullPointerException();
        }
        if (bsize <= 0) {
            throw new IllegalArgumentException();
        }
        this.inf = inf;
        buf = new byte[bsize];
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        if (read(b, 0, 1) == -1) {
            return -1;
        }
        return b[0] & 0xff;
    }

    @Override
    public int read(byte[] buffer, int off, int nbytes) throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }

        if (null == buffer) {
            throw new NullPointerException();
        }

        if (off < 0 || nbytes < 0 || off + nbytes > buffer.length) {
            throw new IndexOutOfBoundsException();
        }

        if (nbytes == 0) {
            return 0;
        }

        if (eof) {
            return -1;
        }

        // avoid int overflow, check null buffer
        if (off > buffer.length || nbytes < 0 || off < 0 || buffer.length - off < nbytes) {
            throw new ArrayIndexOutOfBoundsException();
        }

        do {
            if (inf.needsInput()) {
                fill();
            }
            // Invariant: if reading returns -1 or throws, eof must be true.
            // It may also be true if the next read() should return -1.
            try {
                int result = inf.inflate(buffer, off, nbytes);
                eof = inf.finished();
                if (result > 0) {
                    return result;
                } else if (eof) {
                    return -1;
                } else if (inf.needsDictionary()) {
                    eof = true;
                    return -1;
                } else if (len == -1) {
                    eof = true;
                    throw new EOFException();
                    // If result == 0, fill() and try again
                }
            } catch (TDataFormatException e) {
                eof = true;
                if (len == -1) {
                    throw new EOFException();
                }
                throw new IOException(e);
            }
        } while (true);
    }

    protected void fill() throws IOException {
        if (closed) {
            throw new IOException();
        }
        len = in.read(buf);
        if (len > 0) {
            inf.setInput(buf, 0, len);
        }
    }

    @Override
    public long skip(long nbytes) throws IOException {
        if (nbytes >= 0) {
            if (buf == null) {
                buf = new byte[(int) Math.min(nbytes, BUF_SIZE)];
            }
            long count = 0;
            long rem;
            while (count < nbytes) {
                rem = nbytes - count;
                int x = read(buf, 0, rem > buf.length ? buf.length : (int) rem);
                if (x == -1) {
                    return count;
                }
                count += x;
            }
            return count;
        }
        throw new IllegalArgumentException();
    }

    @Override
    public int available() throws IOException {
        if (closed) {
            throw new IOException("Stream is closed");
        }
        if (eof) {
            return 0;
        }
        return 1;
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            inf.end();
            closed = true;
            eof = true;
            super.close();
        }
    }

    @Override
    public void mark(int readlimit) {
        // do nothing
    }

    @Override
    public void reset() throws IOException {
        throw new IOException();
    }

    @Override
    public boolean markSupported() {
        return false;
    }
}
