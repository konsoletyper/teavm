/*
 *  Copyright 2015 Alexey Andreev.
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

import org.teavm.classlib.java.lang.TString;

public class TBufferedInputStream extends TFilterInputStream {
    protected volatile byte[] buf;
    protected int count;
    protected int marklimit;
    protected int markpos = -1;
    protected int pos;

    public TBufferedInputStream(TInputStream in) {
        super(in);
        buf = new byte[8192];
    }

    public TBufferedInputStream(TInputStream in, int size) {
        super(in);
        if (size <= 0) {
            throw new IllegalArgumentException("Size " + size + " is negative");
        }
        buf = new byte[size];
    }

    @Override
    public int available() throws TIOException {
        TInputStream localIn = in;
        if (buf == null || localIn == null) {
            throw new TIOException(TString.wrap("Stream is closed"));
        }
        return count - pos + localIn.available();
    }

    @Override
    public void close() throws TIOException {
        buf = null;
        TInputStream localIn = in;
        in = null;
        if (localIn != null) {
            localIn.close();
        }
    }

    private int fillbuf(TInputStream localIn, byte[] localBuf) throws TIOException {
        if (markpos == -1 || (pos - markpos >= marklimit)) {
            /* Mark position not set or exceeded readlimit */
            int result = localIn.read(localBuf);
            if (result > 0) {
                markpos = -1;
                pos = 0;
                count = result;
            }
            return result;
        }
        if (markpos == 0 && marklimit > localBuf.length) {
            /* Increase buffer size to accommodate the readlimit */
            int newLength = localBuf.length * 2;
            if (newLength > marklimit) {
                newLength = marklimit;
            }
            byte[] newbuf = new byte[newLength];
            System.arraycopy(localBuf, 0, newbuf, 0, localBuf.length);
            // Reassign buf, which will invalidate any local references
            // FIXME: what if buf was null?
            buf = newbuf;
            localBuf = buf;
        } else if (markpos > 0) {
            System.arraycopy(localBuf, markpos, localBuf, 0, localBuf.length - markpos);
        }
        /* Set the new position and mark position */
        pos -= markpos;
        count = 0;
        markpos = 0;
        int bytesread = localIn.read(localBuf, pos, localBuf.length - pos);
        count = bytesread <= 0 ? pos : pos + bytesread;
        return bytesread;
    }

    @Override
    public synchronized void mark(int readlimit) {
        marklimit = readlimit;
        markpos = pos;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public synchronized int read() throws TIOException {
        // Use local refs since buf and in may be invalidated by an
        // unsynchronized close()
        byte[] localBuf = buf;
        TInputStream localIn = in;
        if (localBuf == null || localIn == null) {
            throw new TIOException(TString.wrap("Stream is closed"));
        }

        /* Are there buffered bytes available? */
        if (pos >= count && fillbuf(localIn, localBuf) == -1) {
            return -1; /* no, fill buffer */
        }
        // localBuf may have been invalidated by fillbuf
        if (localBuf != buf) {
            localBuf = buf;
            if (localBuf == null) {
                throw new TIOException(TString.wrap("Stream is closed"));
            }
        }

        /* Did filling the buffer fail with -1 (EOF)? */
        if (count - pos > 0) {
            return localBuf[pos++] & 0xFF;
        }
        return -1;
    }

    @Override
    public synchronized int read(byte[] buffer, int offset, int length) throws TIOException {
        byte[] localBuf = buf;
        if (localBuf == null) {
            throw new TIOException(TString.wrap("Stream is closed"));
        }
        // avoid int overflow
        if (offset > buffer.length - length || offset < 0 || length < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (length == 0) {
            return 0;
        }
        TInputStream localIn = in;
        if (localIn == null) {
            throw new TIOException(TString.wrap("Stream is closed"));
        }

        int required;
        if (pos < count) {
            /* There are bytes available in the buffer. */
            int copylength = count - pos >= length ? length : count - pos;
            System.arraycopy(localBuf, pos, buffer, offset, copylength);
            pos += copylength;
            if (copylength == length || localIn.available() == 0) {
                return copylength;
            }
            offset += copylength;
            required = length - copylength;
        } else {
            required = length;
        }

        while (true) {
            int read;
            /*
             * If we're not marked and the required size is greater than the
             * buffer, simply read the bytes directly bypassing the buffer.
             */
            if (markpos == -1 && required >= localBuf.length) {
                read = localIn.read(buffer, offset, required);
                if (read == -1) {
                    return required == length ? -1 : length - required;
                }
            } else {
                if (fillbuf(localIn, localBuf) == -1) {
                    return required == length ? -1 : length - required;
                }
                // localBuf may have been invalidated by fillbuf
                if (localBuf != buf) {
                    localBuf = buf;
                    if (localBuf == null) {
                        throw new TIOException(TString.wrap("Stream is closed"));
                    }
                }

                read = count - pos >= required ? required : count - pos;
                System.arraycopy(localBuf, pos, buffer, offset, read);
                pos += read;
            }
            required -= read;
            if (required == 0) {
                return length;
            }
            if (localIn.available() == 0) {
                return length - required;
            }
            offset += read;
        }
    }

    @Override
    public synchronized void reset() throws TIOException {
        if (buf == null) {
            throw new TIOException(TString.wrap("Stream is closed"));
        }
        if (-1 == markpos) {
            throw new TIOException(TString.wrap("Mark has been invalidated."));
        }
        pos = markpos;
    }

    @Override
    public synchronized long skip(long amount) throws TIOException {
        // Use local refs since buf and in may be invalidated by an
        // unsynchronized close()
        byte[] localBuf = buf;
        TInputStream localIn = in;
        if (localBuf == null) {
            throw new TIOException(TString.wrap("Stream is closed"));
        }
        if (amount < 1) {
            return 0;
        }
        if (localIn == null) {
            throw new TIOException(TString.wrap("Stream is closed"));
        }

        if (count - pos >= amount) {
            pos += amount;
            return amount;
        }
        long read = count - pos;
        pos = count;

        if (markpos != -1) {
            if (amount <= marklimit) {
                if (fillbuf(localIn, localBuf) == -1) {
                    return read;
                }
                if (count - pos >= amount - read) {
                    pos += amount - read;
                    return amount;
                }
                // Couldn't get all the bytes, skip what we read
                read += count - pos;
                pos = count;
                return read;
            }
        }
        return read + localIn.skip(amount - read);
    }
}
