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
import org.teavm.classlib.fs.VirtualFile;

public class TFileOutputStream extends OutputStream {
    private OutputStream underlyingStream;

    public TFileOutputStream(TFile file) throws FileNotFoundException {
        this(file, false);
    }

    public TFileOutputStream(TFile file, boolean append) throws FileNotFoundException {
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

        underlyingStream = virtualFile.write(append);
        if (underlyingStream == null) {
            throw new FileNotFoundException();
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        underlyingStream.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        underlyingStream.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        underlyingStream.flush();
    }

    @Override
    public void close() throws IOException {
        underlyingStream.close();
    }

    @Override
    public void write(int b) throws IOException {
        underlyingStream.write(b);
    }
}
