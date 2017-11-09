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
import org.teavm.classlib.fs.VirtualFile;

public class TFileInputStream extends InputStream {
    private InputStream underlyingStream;

    public TFileInputStream(TFile file) throws FileNotFoundException {
        VirtualFile virtualFile = file.findVirtualFile();
        if (virtualFile == null || virtualFile.isDirectory()) {
            throw new FileNotFoundException();
        }

        underlyingStream = virtualFile.read();
        if (underlyingStream == null) {
            throw new FileNotFoundException();
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        return underlyingStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return underlyingStream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return underlyingStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return underlyingStream.available();
    }

    @Override
    public void close() throws IOException {
        underlyingStream.close();
    }

    @Override
    public int read() throws IOException {
        return underlyingStream.read();
    }
}
