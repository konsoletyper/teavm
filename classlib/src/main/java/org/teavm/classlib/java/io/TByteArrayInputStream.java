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

import org.teavm.classlib.java.lang.TMath;

public class TByteArrayInputStream extends TInputStream {
    protected byte[] buf;
    protected int pos;
    protected int mark;
    protected int count;

    public TByteArrayInputStream(byte[] buf, int offset, int length) {
        this.buf = buf;
        pos = offset;
        mark = offset;
        count = offset + length;
    }

    public TByteArrayInputStream(byte[] buf) {
        this(buf, 0, buf.length);
    }

    @Override
    public int read() {
        return pos < count ? buf[pos++] & 0xFF : -1;
    }

    @Override
    public int read(byte[] b, int off, int len) {
        int bytesToRead = TMath.min(len, count - pos);
        for (int i = 0; i < bytesToRead; ++i) {
            b[off++] = buf[pos++];
        }
        return bytesToRead > 0 ? bytesToRead : -1;
    }

    @Override
    public long skip(long n) {
        int bytesSkipped = (int) TMath.min(n, count - pos);
        pos += bytesSkipped;
        return bytesSkipped;
    }

    @Override
    public int available() {
        return count - pos;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void mark(int readAheadLimit) {
        mark = pos;
    }

    @Override
    public void reset() {
        pos = mark;
    }

    @Override
    public void close() {
    }
}
