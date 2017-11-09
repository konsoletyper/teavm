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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class TZipOutputStream extends TDeflaterOutputStream implements TZipConstants {
    public static final int DEFLATED = 8;
    public static final int STORED = 0;

    static final int ZIPDataDescriptorFlag = 8;
    static final int ZIPLocalHeaderVersionNeeded = 20;
    private String comment;
    private final List<String> entries = new ArrayList<>();
    private int compressMethod = DEFLATED;
    private int compressLevel = TDeflater.DEFAULT_COMPRESSION;
    private ByteArrayOutputStream cDir = new ByteArrayOutputStream();
    private TZipEntry currentEntry;
    private final TCRC32 crc = new TCRC32();
    private int offset;
    private int curOffset;
    private int nameLength;
    private byte[] nameBytes;

    public TZipOutputStream(OutputStream p1) {
        super(p1, new TDeflater(TDeflater.DEFAULT_COMPRESSION, true));
    }

    @Override
    public void close() throws IOException {
        if (out != null) {
            finish();
            out.close();
            out = null;
        }
    }

    public void closeEntry() throws IOException {
        if (cDir == null) {
            throw new IOException();
        }
        if (currentEntry == null) {
            return;
        }
        if (currentEntry.getMethod() == DEFLATED) {
            super.finish();
        }

        // Verify values for STORED types
        if (currentEntry.getMethod() == STORED) {
            if (crc.getValue() != currentEntry.crc) {
                throw new TZipException();
            }
            if (currentEntry.size != crc.tbytes) {
                throw new TZipException();
            }
        }
        curOffset = LOCHDR;

        // Write the DataDescriptor
        if (currentEntry.getMethod() != STORED) {
            curOffset += EXTHDR;
            writeLong(out, EXTSIG);
            currentEntry.crc = crc.getValue();
            writeLong(out, currentEntry.crc);
            currentEntry.compressedSize = def.getTotalOut();
            writeLong(out, currentEntry.compressedSize);
            currentEntry.size = def.getTotalIn();
            writeLong(out, currentEntry.size);
        }
        // Update the CentralDirectory
        writeLong(cDir, CENSIG);
        writeShort(cDir, ZIPLocalHeaderVersionNeeded); // Version created
        writeShort(cDir, ZIPLocalHeaderVersionNeeded); // Version to extract
        writeShort(cDir, currentEntry.getMethod() == STORED ? 0 : ZIPDataDescriptorFlag);
        writeShort(cDir, currentEntry.getMethod());
        writeShort(cDir, currentEntry.time);
        writeShort(cDir, currentEntry.modDate);
        writeLong(cDir, crc.getValue());
        if (currentEntry.getMethod() == DEFLATED) {
            curOffset += writeLong(cDir, def.getTotalOut());
            writeLong(cDir, def.getTotalIn());
        } else {
            curOffset += writeLong(cDir, crc.tbytes);
            writeLong(cDir, crc.tbytes);
        }
        curOffset += writeShort(cDir, nameLength);
        if (currentEntry.extra != null) {
            curOffset += writeShort(cDir, currentEntry.extra.length);
        } else {
            writeShort(cDir, 0);
        }
        String c = currentEntry.getComment();
        writeShort(cDir, c != null ? c.length() : 0);
        writeShort(cDir, 0); // Disk Start
        writeShort(cDir, 0); // Internal File Attributes
        writeLong(cDir, 0); // External File Attributes
        writeLong(cDir, offset);
        cDir.write(nameBytes);
        nameBytes = null;
        if (currentEntry.extra != null) {
            cDir.write(currentEntry.extra);
        }
        offset += curOffset;
        if (c != null) {
            cDir.write(c.getBytes());
        }
        currentEntry = null;
        crc.reset();
        def.reset();
        done = false;
    }

    @Override
    public void finish() throws IOException {
        if (out == null) {
            throw new IOException();
        }
        if (cDir == null) {
            return;
        }
        if (entries.size() == 0) {
            throw new TZipException();
        }
        if (currentEntry != null) {
            closeEntry();
        }
        int cdirSize = cDir.size();
        // Write Central Dir End
        writeLong(cDir, ENDSIG);
        writeShort(cDir, 0); // Disk Number
        writeShort(cDir, 0); // Start Disk
        writeShort(cDir, entries.size()); // Number of entries
        writeShort(cDir, entries.size()); // Number of entries
        writeLong(cDir, cdirSize); // Size of central dir
        writeLong(cDir, offset); // Offset of central dir
        if (comment != null) {
            writeShort(cDir, comment.length());
            cDir.write(comment.getBytes());
        } else {
            writeShort(cDir, 0);
        }
        // Write the central dir
        out.write(cDir.toByteArray());
        cDir = null;

    }

    public void putNextEntry(TZipEntry ze) throws IOException {
        if (currentEntry != null) {
            closeEntry();
        }
        if (ze.getMethod() == STORED
                || (compressMethod == STORED && ze.getMethod() == -1)) {
            if (ze.crc == -1) {
                throw new TZipException("Crc mismatch");
            }
            if (ze.size == -1 && ze.compressedSize == -1) {
                throw new TZipException("Size mismatch");
            }
            if (ze.size != ze.compressedSize && ze.compressedSize != -1 && ze.size != -1) {
                throw new TZipException("Size mismatch");
            }
        }
        if (cDir == null) {
            throw new IOException("Stream is closed");
        }
        if (entries.contains(ze.name)) {
            throw new TZipException("Entry already exists: " + ze.name);
        }
        nameLength = utf8Count(ze.name);
        if (nameLength > 0xffff) {
            throw new IllegalArgumentException("Name too long: " + ze.name);
        }

        def.setLevel(compressLevel);
        currentEntry = ze;
        entries.add(currentEntry.name);
        if (currentEntry.getMethod() == -1) {
            currentEntry.setMethod(compressMethod);
        }
        writeLong(out, LOCSIG); // Entry header
        writeShort(out, ZIPLocalHeaderVersionNeeded); // Extraction version
        writeShort(out, currentEntry.getMethod() == STORED ? 0 : ZIPDataDescriptorFlag);
        writeShort(out, currentEntry.getMethod());
        if (currentEntry.getTime() == -1) {
            currentEntry.setTime(System.currentTimeMillis());
        }
        writeShort(out, currentEntry.time);
        writeShort(out, currentEntry.modDate);

        if (currentEntry.getMethod() == STORED) {
            if (currentEntry.size == -1) {
                currentEntry.size = currentEntry.compressedSize;
            } else if (currentEntry.compressedSize == -1) {
                currentEntry.compressedSize = currentEntry.size;
            }
            writeLong(out, currentEntry.crc);
            writeLong(out, currentEntry.size);
            writeLong(out, currentEntry.size);
        } else {
            writeLong(out, 0);
            writeLong(out, 0);
            writeLong(out, 0);
        }
        writeShort(out, nameLength);
        writeShort(out, currentEntry.extra != null ? currentEntry.extra.length : 0);
        nameBytes = toUTF8Bytes(currentEntry.name, nameLength);
        out.write(nameBytes);
        if (currentEntry.extra != null) {
            out.write(currentEntry.extra);
        }
    }

    public void setComment(String comment) {
        if (comment.length() > 0xFFFF) {
            throw new IllegalArgumentException();
        }
        this.comment = comment;
    }

    public void setLevel(int level) {
        if (level < TDeflater.DEFAULT_COMPRESSION || level > TDeflater.BEST_COMPRESSION) {
            throw new IllegalArgumentException();
        }
        compressLevel = level;
    }

    public void setMethod(int method) {
        if (method != STORED && method != DEFLATED) {
            throw new IllegalArgumentException();
        }
        compressMethod = method;

    }

    private long writeLong(OutputStream os, long i) throws IOException {
        // Write out the long value as an unsigned int
        os.write((int) (i & 0xFF));
        os.write((int) (i >> 8) & 0xFF);
        os.write((int) (i >> 16) & 0xFF);
        os.write((int) (i >> 24) & 0xFF);
        return i;
    }

    private int writeShort(OutputStream os, int i) throws IOException {
        os.write(i & 0xFF);
        os.write((i >> 8) & 0xFF);
        return i;

    }

    /**
     * Writes data for the current entry to the underlying stream.
     *
     * @exception IOException
     *                If an error occurs writing to the stream
     */
    @Override
    public void write(byte[] buffer, int off, int nbytes)
            throws IOException {
        // avoid int overflow, check null buf
        if ((off < 0 || (nbytes < 0) || off > buffer.length) || (buffer.length - off < nbytes)) {
            throw new IndexOutOfBoundsException();
        }

        if (currentEntry == null) {
            throw new TZipException("No active entry");
        }

        if (currentEntry.getMethod() == STORED) {
            out.write(buffer, off, nbytes);
        } else {
            super.write(buffer, off, nbytes);
        }
        crc.update(buffer, off, nbytes);
    }

    static int utf8Count(String value) {
        int total = 0;
        for (int i = value.length(); --i >= 0;) {
            char ch = value.charAt(i);
            if (ch < 0x80) {
                total++;
            } else if (ch < 0x800) {
                total += 2;
            } else {
                total += 3;
            }
        }
        return total;
    }

    static byte[] toUTF8Bytes(String value, int length) {
        byte[] result = new byte[length];
        int pos = result.length;
        for (int i = value.length(); --i >= 0;) {
            char ch = value.charAt(i);
            if (ch < 0x80) {
                result[--pos] = (byte) ch;
            } else if (ch < 0x800) {
                result[--pos] = (byte) (0x80 | (ch & 0x3f));
                result[--pos] = (byte) (0xc0 | (ch >> 6));
            } else {
                result[--pos] = (byte) (0x80 | (ch & 0x3f));
                result[--pos] = (byte) (0x80 | ((ch >> 6) & 0x3f));
                result[--pos] = (byte) (0xe0 | (ch >> 12));
            }
        }
        return result;
    }
}
