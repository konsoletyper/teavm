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
import org.teavm.classlib.java.lang.TInteger;
import org.teavm.classlib.java.lang.TObject;

public abstract class TInputStream extends TObject implements TCloseable {
    public TInputStream() {
    }

    public abstract int read() throws IOException;

    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        for (int i = 0; i < len; ++i) {
            int bt = read();
            if (bt < 0) {
                return i == 0 ? -1 : i;
            }
            b[off++] = (byte) bt;
        }
        return len > 0 ? len : -1;
    }

    public long skip(long n) throws IOException {
        if (n < TInteger.MAX_VALUE) {
            return skip((int) n);
        } else {
            for (long i = 0; i < n; ++i) {
                if (read() < 0) {
                    return i;
                }
            }
            return n;
        }
    }

    private int skip(int n) throws IOException {
        for (int i = 0; i < n; ++i) {
            if (read() < 0) {
                return i;
            }
        }
        return n;
    }

    public int available() throws IOException {
        return 0;
    }

    @Override
    public void close() throws IOException {
    }

    public void mark(@SuppressWarnings("unused") int readlimit) {
    }

    public void reset() throws IOException {
        throw new IOException();
    }

    public boolean markSupported() {
        return false;
    }
}
