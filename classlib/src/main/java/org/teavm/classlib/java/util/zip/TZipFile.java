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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import org.teavm.classlib.java.util.zip.TZipEntry.LittleEndianReader;

public class TZipFile implements TZipConstants {
    public static final int OPEN_READ = 1;
    public static final int OPEN_DELETE = 4;

    private final String fileName;
    private File fileToDeleteOnClose;
    private RandomAccessFile mRaf;
    private final LittleEndianReader ler = new LittleEndianReader();

    private final LinkedHashMap<String, TZipEntry> mEntries = new LinkedHashMap<>();

    public TZipFile(File file) throws TZipException, IOException {
        this(file, OPEN_READ);
    }

    public TZipFile(File file, int mode) throws IOException {
        fileName = file.getPath();
        if (mode != OPEN_READ && mode != (OPEN_READ | OPEN_DELETE)) {
            throw new IllegalArgumentException();
        }

        if ((mode & OPEN_DELETE) != 0) {
            fileToDeleteOnClose = file; // file.deleteOnExit();
        } else {
            fileToDeleteOnClose = null;
        }

        mRaf = new RandomAccessFile(fileName, "r");

        readCentralDir();
    }

    public TZipFile(String name) throws IOException {
        this(new File(name), OPEN_READ);
    }

    @Override
    protected void finalize() throws IOException {
        close();
    }

    public void close() throws IOException {
        RandomAccessFile raf = mRaf;

        if (raf != null) {
            mRaf = null;
            raf.close();
            if (fileToDeleteOnClose != null) {
                new File(fileName).delete();
                // fileToDeleteOnClose.delete();
                fileToDeleteOnClose = null;
            }
        }
    }

    private void checkNotClosed() {
        if (mRaf == null) {
            throw new IllegalStateException();
        }
    }

    public Enumeration<? extends TZipEntry> entries() {
        checkNotClosed();
        final Iterator<TZipEntry> iterator = mEntries.values().iterator();

        return new Enumeration<TZipEntry>() {
            @Override
            public boolean hasMoreElements() {
                checkNotClosed();
                return iterator.hasNext();
            }

            @Override
            public TZipEntry nextElement() {
                checkNotClosed();
                return iterator.next();
            }
        };
    }

    public TZipEntry getEntry(String entryName) {
        checkNotClosed();
        if (entryName == null) {
            throw new NullPointerException();
        }

        TZipEntry ze = mEntries.get(entryName);
        if (ze == null) {
            ze = mEntries.get(entryName + "/");
        }
        return ze;
    }

    public InputStream getInputStream(TZipEntry entry) throws IOException {
        /*
         * Make sure this TZipEntry is in this Zip file.  We run it through
         * the name lookup.
         */
        entry = getEntry(entry.getName());
        if (entry == null) {
            return null;
        }

        /*
         * Create a TZipInputStream at the right part of the file.
         */
        RandomAccessFile raf = mRaf;
        // We don't know the entry data's start position. All we have is the
        // position of the entry's local header. At position 28 we find the
        // length of the extra data. In some cases this length differs from
        // the one coming in the central header.
        RAFStream rafstrm = new RAFStream(raf, entry.mLocalHeaderRelOffset + 28);
        int localExtraLenOrWhatever = ler.readShortLE(rafstrm);
        // Skip the name and this "extra" data or whatever it is:
        rafstrm.skip(entry.nameLen + localExtraLenOrWhatever);
        rafstrm.mLength = rafstrm.mOffset + entry.compressedSize;
        if (entry.compressionMethod == java.util.zip.ZipEntry.DEFLATED) {
            int bufSize = Math.max(1024, (int) Math.min(entry.getSize(), 65535L));
            return new ZipInflaterInputStream(rafstrm, new TInflater(true), bufSize, entry);
        } else {
            return rafstrm;
        }
    }

    public String getName() {
        return fileName;
    }

    public int size() {
        checkNotClosed();
        return mEntries.size();
    }

    /**
     * Find the central directory and read the contents.
     *
     * <p>The central directory can be followed by a variable-length comment
     * field, so we have to scan through it backwards.  The comment is at
     * most 64K, plus we have 18 bytes for the end-of-central-dir stuff
     * itself, plus apparently sometimes people throw random junk on the end
     * just for the fun of it.
     *
     * <p>This is all a little wobbly.  If the wrong value ends up in the EOCD
     * area, we're hosed. This appears to be the way that everybody handles
     * it though, so we're in good company if this fails.
     */
    private void readCentralDir() throws IOException {
        /*
         * Scan back, looking for the End Of Central Directory field.  If
         * the archive doesn't have a comment, we'll hit it on the first
         * try.
         *
         * No need to synchronize mRaf here -- we only do this when we
         * first open the Zip file.
         */
        long scanOffset = mRaf.length() - ENDHDR;
        if (scanOffset < 0) {
            throw new TZipException();
        }

        long stopOffset = scanOffset - 65536;
        if (stopOffset < 0) {
            stopOffset = 0;
        }

        while (true) {
            mRaf.seek(scanOffset);
            if (TZipEntry.readIntLE(mRaf) == 101010256L) {
                break;
            }

            scanOffset--;
            if (scanOffset < stopOffset) {
                throw new TZipException();
            }
        }

        /*
         * Found it, read the EOCD.
         *
         * For performance we want to use buffered I/O when reading the
         * file.  We wrap a buffered stream around the random-access file
         * object.  If we just read from the RandomAccessFile we'll be
         * doing a read() system call every time.
         */
        RAFStream rafs = new RAFStream(mRaf, mRaf.getFilePointer());
        BufferedInputStream bin = new BufferedInputStream(rafs, ENDHDR);

        int diskNumber = ler.readShortLE(bin);
        int diskWithCentralDir = ler.readShortLE(bin);
        int numEntries = ler.readShortLE(bin);
        int totalNumEntries = ler.readShortLE(bin);
        /*centralDirSize =*/ ler.readIntLE(bin);
        long centralDirOffset = ler.readIntLE(bin);
        /*commentLen =*/ ler.readShortLE(bin);

        if (numEntries != totalNumEntries || diskNumber != 0 || diskWithCentralDir != 0) {
            throw new TZipException();
        }

        /*
         * Seek to the first CDE and read all entries.
         * However, when Z_SYNC_FLUSH is used the offset may not point directly
         * to the CDE so skip over until we find it. 
         * At most it will be 6 bytes away (one or two bytes for empty block, 4 bytes for
         * empty block signature).  
         */
        scanOffset = centralDirOffset;
        stopOffset = scanOffset + 6;
        
        while (true) {
            mRaf.seek(scanOffset);
            if (TZipEntry.readIntLE(mRaf) == CENSIG) {
                break;
            }

            scanOffset++;
            if (scanOffset > stopOffset) {
                throw new TZipException();
            }
        }
        
        // If CDE is found then go and read all the entries
        rafs = new RAFStream(mRaf, scanOffset);
        bin = new BufferedInputStream(rafs, 4096);
        for (int i = 0; i < numEntries; i++) {
            TZipEntry newEntry = new TZipEntry(ler, bin);
            mEntries.put(newEntry.getName(), newEntry);
        }
    }

    static class RAFStream extends InputStream {

        RandomAccessFile mSharedRaf;
        long mOffset;
        long mLength;

        public RAFStream(RandomAccessFile raf, long pos) throws IOException {
            mSharedRaf = raf;
            mOffset = pos;
            mLength = raf.length();
        }

        @Override
        public int available() throws IOException {
            if (mLength > mOffset) {
                if (mLength - mOffset < Integer.MAX_VALUE) {
                    return (int) (mLength - mOffset);
                } else {
                    return Integer.MAX_VALUE;
                }
            } else {
                return 0;
            }
        }

        @Override
        public int read() throws IOException {
            byte[] singleByteBuf = new byte[1];
            if (read(singleByteBuf, 0, 1) == 1) {
                return singleByteBuf[0] & 0XFF;
            } else {
                return -1;
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            mSharedRaf.seek(mOffset);
            if (len > mLength - mOffset) {
                len = (int) (mLength - mOffset);
            }
            int count = mSharedRaf.read(b, off, len);
            if (count > 0) {
                mOffset += count;
                return count;
            } else {
                return -1;
            }
        }

        @Override
        public long skip(long n) throws IOException {
            if (n > mLength - mOffset) {
                n = mLength - mOffset;
            }
            mOffset += n;
            return n;
        }
    }
    
    static class ZipInflaterInputStream extends TInflaterInputStream {
        TZipEntry entry;
        long bytesRead;

        public ZipInflaterInputStream(InputStream is, TInflater inf, int bsize, TZipEntry entry) {
            super(is, inf, bsize);
            this.entry = entry;
        }

        @Override
        public int read(byte[] buffer, int off, int nbytes) throws IOException {
            int i = super.read(buffer, off, nbytes);
            if (i != -1) {
                bytesRead += i;
            }
            return i;
        }

        @Override
        public int available() throws IOException {
            return super.available() == 0 ? 0 : (int) (entry.getSize() - bytesRead);
        }
    }
}
