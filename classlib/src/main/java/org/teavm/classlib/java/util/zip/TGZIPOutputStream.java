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

import java.io.IOException;
import java.io.OutputStream;

public class TGZIPOutputStream extends TDeflaterOutputStream {
    protected TCRC32 crc = new TCRC32();

    public TGZIPOutputStream(OutputStream os) throws IOException {
        this(os, BUF_SIZE);
    }

    public TGZIPOutputStream(OutputStream os, int size) throws IOException {
        super(os, new TDeflater(java.util.zip.Deflater.DEFAULT_COMPRESSION, true), size);
        writeShort(TGZIPInputStream.GZIP_MAGIC);
        out.write(java.util.zip.Deflater.DEFLATED);
        out.write(0); // flags
        writeLong(0); // mod time
        out.write(0); // extra flags
        out.write(0); // operating system
    }

    @Override
    public void flush() throws IOException {
        int count = def.deflate(buf, 0, buf.length, TDeflater.Z_SYNC_FLUSH);
        out.write(buf, 0, count);
        out.flush();
    }

    @Override
    public void finish() throws IOException {
        super.finish();
        writeLong(crc.getValue());
        writeLong(crc.tbytes);
    }

    @Override
    public void write(byte[] buffer, int off, int nbytes) throws IOException {
        super.write(buffer, off, nbytes);
        crc.update(buffer, off, nbytes);
    }

    private long writeLong(long i) throws IOException {
        // Write out the long value as an unsigned int
        int unsigned = (int) i;
        out.write(unsigned & 0xFF);
        out.write((unsigned >> 8) & 0xFF);
        out.write((unsigned >> 16) & 0xFF);
        out.write((unsigned >> 24) & 0xFF);
        return i;
    }

    private int writeShort(int i) throws IOException {
        out.write(i & 0xFF);
        out.write((i >> 8) & 0xFF);
        return i;
    }
}
