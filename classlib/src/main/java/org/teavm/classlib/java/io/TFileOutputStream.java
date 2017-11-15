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
import java.io.OutputStream;
import java.util.Objects;
import org.teavm.classlib.fs.VirtualFile;
import org.teavm.classlib.fs.VirtualFileAccessor;

public class TFileOutputStream extends OutputStream {
    private VirtualFileAccessor accessor;
    private int pos;

    public TFileOutputStream(TFile file) throws FileNotFoundException {
        this(file, false);
    }

    public TFileOutputStream(String path) throws FileNotFoundException {
        this(new TFile(path));
    }

    public TFileOutputStream(String path, boolean append) throws FileNotFoundException {
        this(new TFile(path), append);
    }

    public TFileOutputStream(TFile file, boolean append) throws FileNotFoundException {
        if (file.getName().isEmpty()) {
            throw new FileNotFoundException("Invalid file name");
        }
        VirtualFile virtualFile = file.findVirtualFile();
        if (virtualFile == null) {
            VirtualFile parentVirtualFile = file.findParentFile();
            if (parentVirtualFile != null && parentVirtualFile.isDirectory()) {
                virtualFile = parentVirtualFile.createFile(file.getName());
            }
        }
        if (virtualFile == null || virtualFile.isDirectory()) {
            throw new FileNotFoundException();
        }

        if (!virtualFile.canWrite()) {
            throw new FileNotFoundException("File is read-only");
        }

        accessor = virtualFile.createAccessor();
        if (accessor == null) {
            throw new FileNotFoundException();
        }

        if (append) {
            pos = accessor.size();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        Objects.requireNonNull(b);
        if (off < 0 || len < 0 || off > b.length - len) {
            throw new IndexOutOfBoundsException();
        }
        ensureOpened();
        accessor.write(pos, b, off, len);
        pos += len;
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
        accessor = null;
    }

    @Override
    public void write(int b) throws IOException {
        ensureOpened();
        byte[] buffer = { (byte) b };
        accessor.write(pos, buffer, 0, 1);
        pos++;
    }

    private void ensureOpened() throws IOException {
        if (accessor == null) {
            throw new IOException("This stream is already closed");
        }
    }
}
