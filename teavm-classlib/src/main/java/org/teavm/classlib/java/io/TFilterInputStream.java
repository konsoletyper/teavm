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

public class TFilterInputStream extends TInputStream {
    protected volatile TInputStream in;

    protected TFilterInputStream(TInputStream in) {
        this.in = in;
    }

    @Override
    public int available() throws TIOException {
        return in.available();
    }

    @Override
    public void close() throws TIOException {
        in.close();
    }

    @Override
    public void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    @Override
    public int read() throws TIOException {
        return in.read();
    }

    @Override
    public int read(byte[] buffer) throws TIOException {
        return read(buffer, 0, buffer.length);
    }

    @Override
    public int read(byte[] buffer, int offset, int count) throws TIOException {
        return in.read(buffer, offset, count);
    }

    @Override
    public synchronized void reset() throws TIOException {
        in.reset();
    }

    @Override
    public long skip(long count) throws TIOException {
        return in.skip(count);
    }
}
