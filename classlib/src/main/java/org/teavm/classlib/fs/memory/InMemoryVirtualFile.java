/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.classlib.fs.memory;

import java.io.IOException;
import java.util.Arrays;
import org.teavm.classlib.fs.VirtualFileAccessor;

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
    public String[] listFiles() {
        return null;
    }

    @Override
    public AbstractInMemoryVirtualFile getChildFile(String fileName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VirtualFileAccessor createAccessor(boolean readable, boolean writable, boolean append) {
        if (parent == null) {
            return null;
        }

        if (writable && readOnly) {
            return null;
        }

        return new VirtualFileAccessor() {
            private int pos;

            {
                if (append) {
                    pos = size;
                }
            }

            @Override
            public int read(byte[] buffer, int offset, int limit) {
                limit = Math.max(0, Math.min(size - pos, limit));
                if (limit > 0) {
                    System.arraycopy(data, pos, buffer, offset, limit);
                    pos += limit;
                }
                return limit;
            }

            @Override
            public void write(byte[] buffer, int offset, int limit) {
                expandData(pos + limit);
                System.arraycopy(buffer, offset, data, pos, limit);
                pos += limit;
                if (pos > size) {
                    size = pos;
                }
                modify();
            }

            @Override
            public int tell() throws IOException {
                return pos;
            }

            @Override
            public void seek(int target) throws IOException {
                pos = target;
            }

            @Override
            public void skip(int amount) throws IOException {
                pos += amount;
            }

            @Override
            public int size() {
                return size;
            }

            @Override
            public void resize(int size) {
                expandData(size);
                InMemoryVirtualFile.this.size = size;
                modify();
            }

            @Override
            public void close() {
            }

            @Override
            public void flush() {
            }
        };
    }

    @Override
    public InMemoryVirtualFile createFile(String fileName) throws IOException {
        throw new IOException("Can't create file " + fileName + " since parent path denotes regular file");
    }

    @Override
    public InMemoryVirtualDirectory createDirectory(String fileName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean adopt(AbstractInMemoryVirtualFile file, String fileName) {
        return false;
    }

    @Override
    public int length() {
        return size;
    }

    private void expandData(int newSize) {
        if (newSize > data.length) {
            int newCapacity = Math.max(newSize, data.length) * 3 / 2;
            data = Arrays.copyOf(data, newCapacity);
        }
    }
}
