/*
 *  Copyright 2019 Alexey Andreev.
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
package org.teavm.vm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

public class IncrementalDirectoryBuildTarget implements BuildTarget {
    private File directory;
    private Set<String> writtenFiles = new HashSet<>();
    private Set<String> formerWrittenFiles = new HashSet<>();

    public IncrementalDirectoryBuildTarget(File directory) {
        this.directory = directory;
    }

    public void reset() {
        for (String fileName : formerWrittenFiles) {
            if (!writtenFiles.contains(fileName)) {
                new File(directory, fileName).delete();
            }
        }
        formerWrittenFiles.clear();
        formerWrittenFiles.addAll(writtenFiles);
        writtenFiles.clear();
    }

    @Override
    public OutputStream createResource(String fileName) {
        writtenFiles.add(fileName);
        return new OutputStreamImpl(new File(directory, fileName));
    }

    static class OutputStreamImpl extends OutputStream {
        private File file;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        OutputStreamImpl(File file) {
            this.file = file;
        }

        @Override
        public void write(int b) throws IOException {
            checkNotClosed();
            bytes.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            checkNotClosed();
            bytes.write(b, off, len);
        }

        @Override
        public void close() throws IOException {
            checkNotClosed();
            byte[] data = bytes.toByteArray();
            bytes = null;
            if (isChanged(file, data)) {
                file.getParentFile().mkdirs();
                try (OutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
                    output.write(data);
                }
            }
        }

        private static boolean isChanged(File file, byte[] data) throws IOException {
            if (!file.exists()) {
                return true;
            }

            InputStream input = new BufferedInputStream(new FileInputStream(file));

            byte[] buffer = new byte[4096];
            int index = 0;
            while (true) {
                int bytesRead = input.read(buffer);
                if (bytesRead < 0) {
                    break;
                }
                if (bytesRead + index > data.length) {
                    return true;
                }
                for (int i = 0; i < bytesRead; ++i) {
                    if (buffer[i] != data[index++]) {
                        return true;
                    }
                }
            }

            return index < data.length;
        }

        private void checkNotClosed() throws IOException {
            if (bytes == null) {
                throw new IOException("Already closed");
            }
        }
    }
}
