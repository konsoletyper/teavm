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

import com.carrotsearch.hppc.ObjectByteHashMap;
import com.carrotsearch.hppc.ObjectByteMap;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IncrementalDirectoryBuildTarget implements BuildTarget {
    private File directory;
    private Set<String> writtenFiles = new HashSet<>();
    private Set<String> formerWrittenFiles = new HashSet<>();
    private ObjectByteMap<String> knownExistingFiles = new ObjectByteHashMap<>();
    private Map<String, FileDescriptor> knownDescriptors = new HashMap<>();

    public IncrementalDirectoryBuildTarget(File directory) {
        this.directory = directory;
    }

    public void reset() {
        for (String fileName : formerWrittenFiles) {
            if (!writtenFiles.contains(fileName)) {
                new File(directory, fileName).delete();
                knownExistingFiles.put(fileName, (byte) 0);
                knownDescriptors.remove(fileName);
            }
        }
        formerWrittenFiles.clear();
        formerWrittenFiles.addAll(writtenFiles);
        writtenFiles.clear();
    }

    @Override
    public OutputStream createResource(String fileName) {
        writtenFiles.add(fileName);
        return new OutputStreamImpl(new File(directory, fileName), fileName);
    }

    class OutputStreamImpl extends OutputStream {
        private File file;
        private String name;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        OutputStreamImpl(File file, String name) {
            this.file = file;
            this.name = name;
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
            byte cachedExisting = knownExistingFiles.getOrDefault(name, (byte) -1);
            if (cachedExisting < 0) {
                cachedExisting = file.exists() ? (byte) 1 : 0;
                knownExistingFiles.put(name, cachedExisting);
            }
            boolean exists = cachedExisting != 0;
            if (!exists || isChanged(file, data)) {
                file.getParentFile().mkdirs();
                try (OutputStream output = new BufferedOutputStream(new FileOutputStream(file))) {
                    output.write(data);
                }
            }
        }

        private boolean isChanged(File file, byte[] data) throws IOException {
            FileDescriptor descriptor = knownDescriptors.get(name);
            long hash = hash(data);
            if (descriptor != null) {
                if (descriptor.hash != hash || descriptor.length != data.length) {
                    descriptor.hash = hash;
                    descriptor.length = data.length;
                    return true;
                }
                return false;
            } else {
                descriptor = new FileDescriptor();
                knownDescriptors.put(name, descriptor);
                descriptor.hash = hash;
                descriptor.length = data.length;
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

    static class FileDescriptor {
        long hash;
        int length;
    }

    private static long hash(byte[] data) {
        long hash = 0xcbf29ce484222325L;
        for (byte b : data) {
            hash *= 1099511628211L;
            hash ^= b & 255;
        }
        return hash;
    }
}
