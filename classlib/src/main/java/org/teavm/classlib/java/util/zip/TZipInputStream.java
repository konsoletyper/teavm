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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

public class TZipInputStream extends TInflaterInputStream implements TZipConstants {
    static final int DEFLATED = 8;
    static final int STORED = 0;
    static final int ZIPDataDescriptorFlag = 8;
    static final int ZIPLocalHeaderVersionNeeded = 20;
    private boolean entriesEnd;
    private boolean hasDD;
    private int entryIn;
    private int inRead;
    private int lastRead;
    TZipEntry currentEntry;
    private final byte[] hdrBuf = new byte[LOCHDR - LOCVER];
    private final TCRC32 crc = new TCRC32();
    private byte[] nameBuf = new byte[256];
    private char[] charBuf = new char[256];

    public TZipInputStream(InputStream stream) {
        super(new PushbackInputStream(stream, 512), new TInflater(true));
        if (stream == null) {
            throw new NullPointerException();
        }
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closeEntry(); // Close the current entry
            super.close();
        }
    }

    public void closeEntry() throws IOException {
        if (closed) {
            throw new IOException();
        }
        if (currentEntry == null) {
            return;
        }

        /*
         * The following code is careful to leave the TZipInputStream in a
         * consistent state, even when close() results in an exception. It does
         * so by:
         *  - pushing bytes back into the source stream
         *  - reading a data descriptor footer from the source stream
         *  - resetting fields that manage the entry being closed
         */

        // Ensure all entry bytes are read
        Exception failure = null;
        try {
            skip(Long.MAX_VALUE);
        } catch (Exception e) {
            failure = e;
        }

        int inB;
        int out;
        if (currentEntry.compressionMethod == DEFLATED) {
            inB = inf.getTotalIn();
            out = inf.getTotalOut();
        } else {
            inB = inRead;
            out = inRead;
        }
        int diff = entryIn - inB;
        // Pushback any required bytes
        if (diff != 0) {
            ((PushbackInputStream) in).unread(buf, len - diff, diff);
        }

        try {
            readAndVerifyDataDescriptor(inB, out);
        } catch (Exception e) {
            if (failure == null) { // otherwise we're already going to throw
                failure = e;
            }
        }

        inf.reset();
        lastRead = 0;
        inRead = 0;
        entryIn = 0;
        len = 0;
        crc.reset();
        currentEntry = null;

        if (failure != null) {
            if (failure instanceof IOException) {
                throw (IOException) failure;
            } else if (failure instanceof RuntimeException) {
                throw (RuntimeException) failure;
            }
            throw new AssertionError(failure);
        }
    }

    private void readAndVerifyDataDescriptor(int inB, int out) throws IOException {
        if (hasDD) {
            in.read(hdrBuf, 0, EXTHDR);
            if (getLong(hdrBuf, 0) != EXTSIG) {
                throw new TZipException();
            }
            currentEntry.crc = getLong(hdrBuf, EXTCRC);
            currentEntry.compressedSize = getLong(hdrBuf, EXTSIZ);
            currentEntry.size = getLong(hdrBuf, EXTLEN);
        }
        if (currentEntry.crc != crc.getValue()) {
            throw new TZipException();
        }
        if (currentEntry.compressedSize != inB || currentEntry.size != out) {
            throw new TZipException();
        }
    }

    public TZipEntry getNextEntry() throws IOException {
        closeEntry();
        if (entriesEnd) {
            return null;
        }

        int x = 0;
        int count = 0;
        while (count != 4) {
            x = in.read(hdrBuf, count, 4 - count);
            count += x;
            if (x == -1) {
                return null;
            }
        }
        long hdr = getLong(hdrBuf, 0);
        if (hdr == CENSIG) {
            entriesEnd = true;
            return null;
        }
        if (hdr != LOCSIG) {
            return null;
        }

        // Read the local header
        count = 0;
        while (count != LOCHDR - LOCVER) {
            x = in.read(hdrBuf, count, (LOCHDR - LOCVER) - count);
            count += x;
            if (x == -1) {
                throw new EOFException();
            }
        }
        int version = getShort(hdrBuf, 0) & 0xff;
        if (version > ZIPLocalHeaderVersionNeeded) {
            throw new TZipException();
        }
        int flags = getShort(hdrBuf, LOCFLG - LOCVER);
        hasDD = (flags & ZIPDataDescriptorFlag) == ZIPDataDescriptorFlag;
        int cetime = getShort(hdrBuf, LOCTIM - LOCVER);
        int cemodDate = getShort(hdrBuf, LOCTIM - LOCVER + 2);
        int cecompressionMethod = getShort(hdrBuf, LOCHOW - LOCVER);
        long cecrc = 0;
        long cecompressedSize = 0;
        long cesize = -1;
        if (!hasDD) {
            cecrc = getLong(hdrBuf, LOCCRC - LOCVER);
            cecompressedSize = getLong(hdrBuf, LOCSIZ - LOCVER);
            cesize = getLong(hdrBuf, LOCLEN - LOCVER);
        }
        int flen = getShort(hdrBuf, LOCNAM - LOCVER);
        if (flen == 0) {
            throw new TZipException();
        }
        int elen = getShort(hdrBuf, LOCEXT - LOCVER);

        count = 0;
        if (flen > nameBuf.length) {
            nameBuf = new byte[flen];
            charBuf = new char[flen];
        }
        while (count != flen) {
            x = in.read(nameBuf, count, flen - count);
            count += x;
            if (x == -1) {
                throw new EOFException();
            }
        }
        currentEntry = createZipEntry(new String(nameBuf, 0, flen, "UTF-8"));
        currentEntry.time = cetime;
        currentEntry.modDate = cemodDate;
        currentEntry.setMethod(cecompressionMethod);
        if (cesize != -1) {
            currentEntry.setCrc(cecrc);
            currentEntry.setSize(cesize);
            currentEntry.setCompressedSize(cecompressedSize);
        }
        if (elen > 0) {
            count = 0;
            byte[] e = new byte[elen];
            while (count != elen) {
                x = in.read(e, count, elen - count);
                count += x;
                if (x == -1) {
                    throw new EOFException();
                }
            }
            currentEntry.setExtra(e);
        }
        return currentEntry;
    }

    @Override
    public int read(byte[] buffer, int start, int length) throws IOException {
        if (closed) {
            throw new IOException();
        }
        if (inf.finished() || currentEntry == null) {
            return -1;
        }
        // avoid int overflow, check null buffer
        if (start > buffer.length || length < 0 || start < 0 || buffer.length - start < length) {
            throw new ArrayIndexOutOfBoundsException();
        }

        if (currentEntry.compressionMethod == STORED) {
            int csize = (int) currentEntry.size;
            if (inRead >= csize) {
                return -1;
            }
            if (lastRead >= len) {
                lastRead = 0;
                len = in.read(buf);
                if (len == -1) {
                    eof = true;
                    return -1;
                }
                entryIn += len;
            }
            int toRead = length > len - lastRead ? len - lastRead : length;
            if ((csize - inRead) < toRead) {
                toRead = csize - inRead;
            }
            System.arraycopy(buf, lastRead, buffer, start, toRead);
            lastRead += toRead;
            inRead += toRead;
            crc.update(buffer, start, toRead);
            return toRead;
        }
        if (inf.needsInput()) {
            fill();
            if (len > 0) {
                entryIn += len;
            }
        }
        int read;
        try {
            read = inf.inflate(buffer, start, length);
        } catch (TDataFormatException e) {
            throw new TZipException(e.getMessage());
        }
        if (read == 0 && inf.finished()) {
            return -1;
        }
        crc.update(buffer, start, read);
        return read;
    }

    @Override
    public long skip(long value) throws IOException {
        if (value < 0) {
            throw new IllegalArgumentException();
        }

        long skipped = 0;
        byte[] b = new byte[(int) Math.min(value, 2048L)];
        while (skipped != value) {
            long rem = value - skipped;
            int x = read(b, 0, (int) (b.length > rem ? rem : b.length));
            if (x == -1) {
                return skipped;
            }
            skipped += x;
        }
        return skipped;
    }

    @Override
    public int available() throws IOException {
        if (closed) {
            throw new IOException();
        }
        return (currentEntry == null || inRead < currentEntry.size) ? 1 : 0;
    }

    protected TZipEntry createZipEntry(String name) {
        return new TZipEntry(name);
    }

    private int getShort(byte[] buffer, int off) {
        return (buffer[off] & 0xFF) | ((buffer[off + 1] & 0xFF) << 8);
    }

    private long getLong(byte[] buffer, int off) {
        long l = 0;
        l |= buffer[off] & 0xFF;
        l |= (buffer[off + 1] & 0xFF) << 8;
        l |= (buffer[off + 2] & 0xFF) << 16;
        l |= ((long) (buffer[off + 3] & 0xFF)) << 24;
        return l;
    }
}
