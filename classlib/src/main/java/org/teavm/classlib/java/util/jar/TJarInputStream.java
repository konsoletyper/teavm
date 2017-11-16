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

import java.io.IOException;
import java.io.InputStream;
import org.teavm.classlib.java.util.zip.TZipEntry;
import org.teavm.classlib.java.util.zip.TZipInputStream;

public class TJarInputStream extends TZipInputStream {
    private TManifest manifest;
    private TJarEntry mEntry;
    private TJarEntry jarEntry;

    public TJarInputStream(InputStream stream, boolean verify) throws IOException {
        super(stream);
        mEntry = getNextJarEntry();
        if (mEntry == null) {
            return;
        }
        String name = mEntry.getName().toUpperCase();
        if (name.equals(TJarFile.META_DIR)) {
            mEntry = null; // modifies behavior of getNextJarEntry()
            closeEntry();
            mEntry = getNextJarEntry();
            name = mEntry.getName().toUpperCase();
        }
        if (name.equals(TJarFile.MANIFEST_NAME)) {
            mEntry = null;
            manifest = new TManifest(this, verify);
            closeEntry();
        } else {
            TAttributes temp = new TAttributes(3);
            temp.map.put("hidden", null);
            mEntry.setAttributes(temp);
        }
    }

    public TJarInputStream(InputStream stream) throws IOException {
        this(stream, true);
    }

    public TManifest getManifest() {
        return manifest;
    }

    public TJarEntry getNextJarEntry() throws IOException {
        return (TJarEntry) getNextEntry();
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (mEntry != null) {
            return -1;
        }
        return super.read(buffer, offset, length);
    }

    @Override
    public TZipEntry getNextEntry() throws IOException {
        if (mEntry != null) {
            jarEntry = mEntry;
            mEntry = null;
            jarEntry.setAttributes(null);
        } else {
            jarEntry = (TJarEntry) super.getNextEntry();
            if (jarEntry == null) {
                return null;
            }
        }
        return jarEntry;
    }

    @Override
    protected TZipEntry createZipEntry(String name) {
        TJarEntry entry = new TJarEntry(name);
        if (manifest != null) {
            entry.setAttributes(manifest.getAttributes(name));
        }
        return entry;
    }
}
