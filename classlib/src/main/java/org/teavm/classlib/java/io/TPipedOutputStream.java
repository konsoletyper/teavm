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
import java.io.OutputStream;

public class TPipedOutputStream extends OutputStream {
    private TPipedInputStream dest;

    public TPipedOutputStream() {
        super();
    }

    public TPipedOutputStream(TPipedInputStream dest) throws IOException {
        super();
        connect(dest);
    }

    @Override
    public void close() throws IOException {
        // Is the pipe connected?
        if (dest != null) {
            dest.done();
            dest = null;
        }
    }

    public void connect(TPipedInputStream stream) throws IOException {
        if (null == stream) {
            throw new NullPointerException();
        }
        if (this.dest != null) {
            throw new IOException();
        }
        synchronized (stream) {
            if (stream.isConnected) {
                throw new IOException();
            }
            stream.buffer = new byte[TPipedInputStream.PIPE_SIZE];
            stream.isConnected = true;
            this.dest = stream;
        }
    }

    @Override
    public void flush() throws IOException {
        if (dest != null) {
            synchronized (dest) {
                dest.notifyAll();
            }
        }
    }

    @Override
    public void write(byte[] buffer, int offset, int count) throws IOException {
        super.write(buffer, offset, count);
    }

    @Override
    public void write(int oneByte) throws IOException {
        if (dest == null) {
            throw new IOException();
        }
        dest.receive(oneByte);
    }
}
