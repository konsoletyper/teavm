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
import org.teavm.classlib.java.util.zip.TZipEntry;

public class TJarEntry extends TZipEntry {
    private TAttributes attributes;
    TJarFile parentJar;

    private boolean isFactoryChecked;

    public TJarEntry(String name) {
        super(name);
    }

    public TJarEntry(TZipEntry entry) {
        super(entry);
    }

    public TAttributes getAttributes() throws IOException {
        if (attributes != null || parentJar == null) {
            return attributes;
        }
        TManifest manifest = parentJar.getManifest();
        if (manifest == null) {
            return null;
        }
        attributes = manifest.getAttributes(getName());
        return attributes;
    }

    void setAttributes(TAttributes attrib) {
        attributes = attrib;
    }

    public TJarEntry(TJarEntry je) {
        super(je);
        parentJar = je.parentJar;
        attributes = je.attributes;
    }
}
