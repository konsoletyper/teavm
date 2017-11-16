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
import java.io.OutputStream;
import org.teavm.classlib.java.util.zip.TZipEntry;
import org.teavm.classlib.java.util.zip.TZipOutputStream;

public class TJarOutputStream extends TZipOutputStream {
    private TManifest manifest;

    public TJarOutputStream(OutputStream os, TManifest mf) throws IOException {
        super(os);
        if (mf == null) {
            throw new NullPointerException();
        }
        manifest = mf;
        TZipEntry ze = new TZipEntry(TJarFile.MANIFEST_NAME);
        putNextEntry(ze);
        manifest.write(this);
        closeEntry();
    }

    public TJarOutputStream(OutputStream os) throws IOException {
        super(os);
    }

    @Override
    public void putNextEntry(TZipEntry ze) throws IOException {
        super.putNextEntry(ze);
    }
}
