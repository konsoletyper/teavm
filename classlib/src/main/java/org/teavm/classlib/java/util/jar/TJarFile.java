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

package org.teavm.classlib.java.util.jar;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.teavm.classlib.java.util.zip.TZipEntry;
import org.teavm.classlib.java.util.zip.TZipFile;

public class TJarFile extends TZipFile {
    public static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";
    static final String META_DIR = "META-INF/";
    private TManifest manifest;
    private TZipEntry manifestEntry;
    private boolean closed;

    static final class JarFileInputStream extends FilterInputStream {
        private long count;
        private TZipEntry zipEntry;

        private boolean done;

        JarFileInputStream(InputStream is, TZipEntry ze) {
            super(is);
            zipEntry = ze;
            count = zipEntry.getSize();
        }

        @Override
        public int read() throws IOException {
            if (done) {
                return -1;
            }
            if (count > 0) {
                int r = super.read();
                if (r != -1) {
                    count--;
                } else {
                    count = 0;
                }
                if (count == 0) {
                    done = true;
                }
                return r;
            } else {
                done = true;
                return -1;
            }
        }

        @Override
        public int read(byte[] buf, int off, int nbytes) throws IOException {
            if (done) {
                return -1;
            }
            if (count > 0) {
                int r = super.read(buf, off, nbytes);
                if (r != -1) {
                    int size = r;
                    if (count < size) {
                        size = (int) count;
                    }
                    count -= size;
                } else {
                    count = 0;
                }
                if (count == 0) {
                    done = true;
                }
                return r;
            } else {
                done = true;
                return -1;
            }
        }

        @Override
        public int available() throws IOException {
            if (done) {
                return 0;
            }
            return super.available();
        }
    }

    public TJarFile(File file) throws IOException {
        this(file, true);
    }

    public TJarFile(File file, boolean verify) throws IOException {
        super(file);
        readMetaEntries();
    }

    public TJarFile(File file, boolean verify, int mode) throws IOException {
        super(file, mode);
        readMetaEntries();
    }

    public TJarFile(String filename) throws IOException {
        this(filename, true);
    }

    public TJarFile(String filename, boolean verify) throws IOException {
        super(filename);
        readMetaEntries();
    }

    @Override
    public Enumeration<TJarEntry> entries() {
        class JarFileEnumerator implements Enumeration<TJarEntry> {
            Enumeration<? extends TZipEntry> ze;

            TJarFile jf;

            JarFileEnumerator(Enumeration<? extends TZipEntry> zenum, TJarFile jf) {
                ze = zenum;
                this.jf = jf;
            }

            @Override
            public boolean hasMoreElements() {
                return ze.hasMoreElements();
            }

            @Override
            public TJarEntry nextElement() {
                TJarEntry je = new TJarEntry(ze.nextElement());
                je.parentJar = jf;
                return je;
            }
        }
        return new JarFileEnumerator(super.entries(), this);
    }

    public TJarEntry getJarEntry(String name) {
        return (TJarEntry) getEntry(name);
    }

    public TManifest getManifest() throws IOException {
        if (closed) {
            throw new IllegalStateException("JarFile has been closed");
        }
        if (manifest != null) {
            return manifest;
        }
        try {
            InputStream is = super.getInputStream(manifestEntry);
            try {
                manifest = new TManifest(is, false);
            } finally {
                is.close();
            }
            manifestEntry = null;  // Can discard the entry now.
        } catch (NullPointerException e) {
            manifestEntry = null;
        }
        return manifest;
    }

    private void readMetaEntries() throws IOException {
        // Get all meta directory entries
        TZipEntry[] metaEntries = getMetaEntriesImpl();
        if (metaEntries == null) {
            return;
        }

        boolean signed = false;

        for (TZipEntry entry : metaEntries) {
            String entryName = entry.getName();
            // Is this the entry for META-INF/MANIFEST.MF ?
            if (manifestEntry == null && MANIFEST_NAME.equalsIgnoreCase(entryName)) {
                manifestEntry = entry;
                break;
            }
        }
    }

    /**
     * Return an {@code InputStream} for reading the decompressed contents of
     * ZIP entry.
     *
     * @param ze
     *            the ZIP entry to be read.
     * @return the input stream to read from.
     * @throws IOException
     *             if an error occurred while creating the input stream.
     */
    @Override
    public InputStream getInputStream(TZipEntry ze) throws IOException {
        if (manifestEntry != null) {
            getManifest();
        }
        return super.getInputStream(ze);
    }

    @Override
    public TZipEntry getEntry(String name) {
        TZipEntry ze = super.getEntry(name);
        if (ze == null) {
            return ze;
        }
        TJarEntry je = new TJarEntry(ze);
        je.parentJar = this;
        return je;
    }

    private TZipEntry[] getMetaEntriesImpl() {
        List<TZipEntry> list = new ArrayList<>(8);
        Enumeration<? extends TZipEntry> allEntries = entries();
        while (allEntries.hasMoreElements()) {
            TZipEntry ze = allEntries.nextElement();
            if (ze.getName().startsWith(META_DIR) && ze.getName().length() > META_DIR.length()) {
                list.add(ze);
            }
        }
        if (list.size() == 0) {
            return null;
        }
        TZipEntry[] result = new TZipEntry[list.size()];
        list.toArray(result);
        return result;
    }

    @Override
    public void close() throws IOException {
        super.close();
        closed = true;
    }
}
