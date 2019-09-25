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

    public CVirtualFileAccessor(long file) {
        this.file = file;
    }

    @Override
    public int read(byte[] buffer, int offset, int limit) throws IOException {
        return CFileSystem.read(file, buffer, offset, limit);
    }

    @Override
    public void write(byte[] buffer, int offset, int limit) throws IOException {
        int bytesWritten = CFileSystem.write(file, buffer, offset, limit);
        if (bytesWritten < limit) {
            throw new IOException();
        }
    }

    @Override
    public int tell() throws IOException {
        return CFileSystem.tell(file);
    }

    @Override
    public void skip(int amount) throws IOException {
        CFileSystem.seek(file, 0, amount);
    }

    @Override
    public void seek(int target) throws IOException {
        CFileSystem.seek(file, 0, target);
    }

    @Override
    public int size() throws IOException {
        int current = CFileSystem.tell(file);
        if (!CFileSystem.seek(file, 2, 0)) {
            throw new IOException();
        }
        int result = CFileSystem.tell(file);
        if (!CFileSystem.seek(file, 0, current)) {
            throw new IOException();
        }
        return result;
    }

    @Override
    public void resize(int size) throws IOException {
        if (!CFileSystem.seek(file, 2, 0)) {
            throw new IOException();
        }
        int position = CFileSystem.tell(file);
        if (position < size) {
            byte[] zeros = new byte[4096];
            while (position < size) {
                int bytesToWrite = Math.min(zeros.length, size - position);
                write(zeros, 0, bytesToWrite);
                position += bytesToWrite;
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
}
