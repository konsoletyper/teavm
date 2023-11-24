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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DirectorySourceFileProvider implements SourceFileProvider {
    private File baseDirectory;

    public DirectorySourceFileProvider(File baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    @Override
    public void open() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public SourceFileInfo getSourceFile(String fullPath) throws IOException {
        File file = new File(baseDirectory, fullPath);
        return file.isFile() ? new DirectorySourceFile(file) : null;
    }

    static class DirectorySourceFile implements SourceFileInfo {
        private File file;

        DirectorySourceFile(File file) {
            this.file = file;
        }

        @Override
        public long lastModified() {
            return file.lastModified();
        }

        @Override
        public InputStream open() throws IOException {
            return new FileInputStream(file);
        }

        @Override
        public File getFile() {
            return file;
        }
    }
}
