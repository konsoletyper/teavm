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
import org.teavm.classlib.java.lang.TMath;
import org.teavm.classlib.java.lang.TObject;

public abstract class TReader implements TCloseable {
    protected TObject lock;

    protected TReader() {
        this(new TObject());
    }

    protected TReader(TObject lock) {
        this.lock = lock;
    }

    public int read() throws IOException {
        char[] buf = new char[1];
        return read(buf) >= 0 ? buf[0] : -1;
    }

    public int read(char[] cbuf) throws IOException {
        return read(cbuf, 0, cbuf.length);
    }

    public abstract int read(char[] cbuf, int off, int len) throws IOException;

    public long skip(long n) throws IOException {
        char[] buffer = new char[1024];
        long skipped = 0;
        while (skipped < n) {
            int charsRead = read(buffer, 0, (int) TMath.min(n, buffer.length));
            if (charsRead < 0) {
                break;
            }
            skipped += charsRead;
        }
        return skipped;
    }

    public boolean ready() throws IOException {
        return true;
    }

    public boolean markSupported() {
        return false;
    }

    public void mark(@SuppressWarnings("unused") int readAheadLimit) throws IOException {
        throw new IOException();
    }

    public void reset() throws IOException {
        throw new IOException();
    }
}
