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

package org.teavm.classlib.java.util.zip;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TDeflaterOutputStream extends FilterOutputStream {
    static final int BUF_SIZE = 512;
    protected byte[] buf;
    protected TDeflater def;
    boolean done;

    public TDeflaterOutputStream(OutputStream os, TDeflater def) {
        this(os, def, BUF_SIZE);
    }

    public TDeflaterOutputStream(OutputStream os) {
        this(os, new TDeflater());
    }

    public TDeflaterOutputStream(OutputStream os, TDeflater def, int bsize) {
        super(os);
        if (os == null || def == null) {
            throw new NullPointerException();
        }
        if (bsize <= 0) {
            throw new IllegalArgumentException();
        }
        this.def = def;
        buf = new byte[bsize];
    }

    protected void deflate() throws IOException {
        int x;
        do {
            x = def.deflate(buf);
            out.write(buf, 0, x);
        } while (!def.needsInput());
    }

    @Override
    public void close() throws IOException {
        if (!def.finished()) {
            finish();
        }
        def.end();
        out.close();
    }

    public void finish() throws IOException {
        if (done) {
            return;
        }
        def.finish();
        int x = 0;
        while (!def.finished()) {
            if (def.needsInput()) {
                def.setInput(buf, 0, 0);
            }
            x = def.deflate(buf);
            out.write(buf, 0, x);
        }
        done = true;
    }

    @Override
    public void write(int i) throws IOException {
        byte[] b = new byte[1];
        b[0] = (byte) i;
        write(b, 0, 1);
    }

    @Override
    public void write(byte[] buffer, int off, int nbytes) throws IOException {
        if (done) {
            throw new IOException();
        }
        // avoid int overflow, check null buf
        if (off <= buffer.length && nbytes >= 0 && off >= 0 && buffer.length - off >= nbytes) {
            if (!def.needsInput()) {
                throw new IOException();
            }
            def.setInput(buffer, off, nbytes);
            deflate();
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }
}
