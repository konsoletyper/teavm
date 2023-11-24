/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.tooling.sources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarSourceFileProvider implements SourceFileProvider {
    private File file;
    private ZipFile zipFile;
    private Set<String> sourceFiles = new HashSet<>();

    public JarSourceFileProvider(File file) {
        this.file = file;
    }

    @Override
    public void open() throws IOException {
        zipFile = new ZipFile(file);
        for (Enumeration<? extends ZipEntry> enumeration = zipFile.entries(); enumeration.hasMoreElements();) {
            ZipEntry entry = enumeration.nextElement();
            sourceFiles.add(entry.getName());
        }
    }

    @Override
    public void close() throws IOException {
        if (this.zipFile == null) {
            return;
        }
        ZipFile zipFile = this.zipFile;
        this.zipFile = null;
        sourceFiles.clear();
        zipFile.close();
    }

    @Override
    public SourceFileInfo getSourceFile(String fullPath) throws IOException {
        if (zipFile == null || !sourceFiles.contains(fullPath)) {
            return null;
        }
        ZipEntry entry = zipFile.getEntry(fullPath);
        return entry != null ? new JarSourceFile(zipFile, entry) : null;
    }

    static class JarSourceFile implements SourceFileInfo {
        private ZipFile file;
        private ZipEntry entry;

        JarSourceFile(ZipFile file, ZipEntry entry) {
            this.file = file;
            this.entry = entry;
        }

        @Override
        public long lastModified() {
            return entry.getTime();
        }

        @Override
        public InputStream open() throws IOException {
            return file.getInputStream(entry);
        }

        @Override
        public File getFile() {
            return null;
        }
    }
}
