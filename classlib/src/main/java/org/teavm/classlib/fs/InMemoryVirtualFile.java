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
package org.teavm.classlib.fs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class InMemoryVirtualFile extends AbstractInMemoryVirtualFile {
    byte[] data = new byte[0];
    int size;

    InMemoryVirtualFile(String name) {
        super(name);
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isFile() {
        return true;
    }

    @Override
    public VirtualFile[] listFiles() {
        throw new UnsupportedOperationException();
    }

    @Override
    public VirtualFile getChildFile(String fileName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream read() {
        if (parent == null) {
            return null;
        }
        return new ByteArrayInputStream(data, 0, size);
    }

    @Override
    public OutputStream write(boolean append) {
        if (parent == null) {
            return null;
        }
        if (!append) {
            data = new byte[0];
            size = 0;
        }
        return new OutputStreamImpl(data, size);
    }

    @Override
    public VirtualFile createFile(String fileName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VirtualFile createDirectory(String fileName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int length() {
        return size;
    }

    class OutputStreamImpl extends OutputStream {
        byte[] data;
        int pos;

        OutputStreamImpl(byte[] data, int pos) {
            this.data = data;
            this.pos = pos;
        }

        private void ensureIO() throws IOException {
            if (data == null) {
                throw new IOException("Stream was closed");
            }
        }

        @Override
        public void write(int b) throws IOException {
            ensureIO();
            expandData(pos + 1);
            data[pos++] = (byte) b;
            sync();
        }

        private void expandData(int newSize) {
            if (newSize > data.length) {
                int newCapacity = Math.max(newSize, data.length) * 3 / 2;
                boolean actual = data == InMemoryVirtualFile.this.data;
                data = Arrays.copyOf(data, newCapacity);
                if (actual) {
                    InMemoryVirtualFile.this.data = data;
                }
            }
        }

        private void sync() {
            if (data == InMemoryVirtualFile.this.data) {
                size = pos;
                modify();
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            ensureIO();
            if (len == 0) {
                return;
            }

            if (off < 0 || len < 0 || off + len >= b.length) {
                throw new IndexOutOfBoundsException();
            }

            expandData(pos + len);
            while (len-- > 0) {
                data[pos++] = b[off++];
            }

            sync();
        }

        @Override
        public void close() throws IOException {
            data = null;
        }

        @Override
        public void flush() throws IOException {
        }
    }
}
