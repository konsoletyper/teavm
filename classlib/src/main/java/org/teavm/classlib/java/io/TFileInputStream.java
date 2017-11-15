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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.teavm.classlib.fs.VirtualFile;
import org.teavm.classlib.fs.VirtualFileAccessor;

public class TFileInputStream extends InputStream {
    private VirtualFileAccessor accessor;
    private int pos;
    private boolean eof;

    public TFileInputStream(TFile file) throws FileNotFoundException {
        VirtualFile virtualFile = file.findVirtualFile();
        if (virtualFile == null || virtualFile.isDirectory()) {
            throw new FileNotFoundException();
        }

        accessor = virtualFile.createAccessor();
        if (accessor == null) {
            throw new FileNotFoundException();
        }
    }

    public TFileInputStream(String path) throws FileNotFoundException {
        this(new TFile(path));
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        Objects.requireNonNull(b);
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        if (eof) {
            return -1;
        }
        ensureOpened();
        int result = accessor.read(pos, b, off, len);
        pos += result;
        if (pos == accessor.size()) {
            eof = true;
        }
        return result;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n < 0) {
            throw new IOException("Value must be positive: " + n);
        }
        ensureOpened();
        if (eof) {
            return 0;
        }
        int newPos = Math.max(pos, Math.min(accessor.size(), pos + (int) n));
        int result = newPos - pos;
        pos = newPos;
        if (result == 0) {
            eof = true;
        }
        return result;
    }

    @Override
    public int available() throws IOException {
        ensureOpened();
        if (eof) {
            return 0;
        }
        return Math.max(0, accessor.size() - pos);
    }

    @Override
    public void close() throws IOException {
        accessor = null;
    }

    @Override
    public int read() throws IOException {
        ensureOpened();
        if (eof) {
            return -1;
        }
        byte[] buffer = new byte[1];
        int read = accessor.read(pos, buffer, 0, 1);
        if (read == 0) {
            eof = true;
        } else {
            pos++;
        }
        return !eof ? buffer[0] : -1;
    }

    private void ensureOpened() throws IOException {
        if (accessor == null) {
            throw new IOException("This stream is already closed");
        }
    }
}
