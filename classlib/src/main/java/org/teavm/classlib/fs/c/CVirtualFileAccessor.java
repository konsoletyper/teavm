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
package org.teavm.classlib.fs.c;

import java.io.IOException;
import org.teavm.classlib.fs.VirtualFileAccessor;

public class CVirtualFileAccessor implements VirtualFileAccessor {
    private long file;
    private int position;

    public CVirtualFileAccessor(long file, int position) {
        this.file = file;
        this.position = position;
    }

    @Override
    public int read(int pos, byte[] buffer, int offset, int limit) throws IOException {
        ensurePosition(pos);
        int bytesRead = CFileSystem.read(file, buffer, offset, limit);
        position += bytesRead;
        return bytesRead;
    }

    @Override
    public void write(int pos, byte[] buffer, int offset, int limit) throws IOException {
        ensurePosition(pos);
        int bytesWritten = CFileSystem.write(file, buffer, offset, limit);
        if (bytesWritten < limit) {
            throw new IOException();
        }
        position += bytesWritten;
    }

    @Override
    public int size() throws IOException {
        if (!CFileSystem.seek(file, 2, 0)) {
            throw new IOException();
        }
        position = CFileSystem.tell(file);
        return position;
    }

    @Override
    public void resize(int size) throws IOException {
        if (!CFileSystem.seek(file, 2, 0)) {
            throw new IOException();
        }
        position = CFileSystem.tell(file);
        if (position < size) {
            byte[] zeros = new byte[4096];
            while (position < size) {
                write(position, zeros, 0, Math.min(zeros.length, size - position));
            }
        }
    }

    @Override
    public void close() throws IOException {
        long file = this.file;
        this.file = 0;
        if (!CFileSystem.close(file)) {
            throw new IOException();
        }
    }

    @Override
    public void flush() throws IOException {
        if (!CFileSystem.flush(file)) {
            throw new IOException();
        }
    }

    private void ensurePosition(int pos) throws IOException {
        if (position != pos) {
            if (!CFileSystem.seek(file, 0, pos)) {
                throw new IOException();
            }
            position = pos;
        }
    }
}
