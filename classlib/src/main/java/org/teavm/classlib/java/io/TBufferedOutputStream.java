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

public class TBufferedOutputStream extends TFilterOutputStream {
    protected byte[] buf;
    protected int count;

    public TBufferedOutputStream(TOutputStream out) {
        super(out);
        buf = new byte[8192];
    }

    public TBufferedOutputStream(TOutputStream out, int size) {
        super(out);
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        buf = new byte[size];
    }

    @Override
    public void flush() throws TIOException {
        flushInternal();
        out.flush();
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws TIOException {
        byte[] internalBuffer = buf;

        if (internalBuffer != null && length >= internalBuffer.length) {
            flushInternal();
            out.write(buffer, offset, length);
            return;
        }

        if (buffer == null) {
            throw new NullPointerException("buffer is null");
        }
        
        if (offset < 0 || offset > buffer.length - length) {
            throw new ArrayIndexOutOfBoundsException("Offset out of bounds: " + offset);
        
        }
        if (length < 0) {
            throw new ArrayIndexOutOfBoundsException("Length out of bounds: " + length);
        }

        if (internalBuffer == null) {
            throw new TIOException();
        }

        // flush the internal buffer first if we have not enough space left
        if (length >= (internalBuffer.length - count)) {
            flushInternal();
        }

        // the length is always less than (internalBuffer.length - count) here so arraycopy is safe
        System.arraycopy(buffer, offset, internalBuffer, count, length);
        count += length;
    }

    @Override
    public void close() throws TIOException {
        if (buf == null) {
            return;
        }
        
        try {
            super.close();
        } finally {
            buf = null;
        }
    }

    @Override
    public void write(int oneByte) throws TIOException {
        byte[] internalBuffer = buf;
        if (internalBuffer == null) {
            throw new TIOException();
        }

        if (count == internalBuffer.length) {
            out.write(internalBuffer, 0, count);
            count = 0;
        }
        internalBuffer[count++] = (byte) oneByte;
    }

    private void flushInternal() throws TIOException {
        if (count > 0) {
            out.write(buf, 0, count);
            count = 0;
        }
    }
}
