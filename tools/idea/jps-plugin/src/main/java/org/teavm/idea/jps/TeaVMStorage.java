/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.idea.jps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.jps.incremental.storage.StorageOwner;

public class TeaVMStorage implements StorageOwner {
    private File file;
    private List<Entry> participatingFiles;
    private boolean dirty;

    TeaVMStorage(File file, String suffix) throws IOException {
        file = new File(file, "teavm-" + suffix + ".storage");
        this.file = file;
        if (file.exists()) {
            participatingFiles = new ArrayList<>();
            try (Reader innerReader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
                    BufferedReader reader = new BufferedReader(innerReader)) {
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    int index = line.lastIndexOf(':');
                    if (index < 0) {
                        participatingFiles = null;
                        file.delete();
                        break;
                    }
                    participatingFiles.add(new Entry(line.substring(0, index),
                            Long.parseLong(line.substring(index + 1))));
                }
            }
        }
    }

    public void setParticipatingFiles(List<Entry> participatingFiles) {
        if (participatingFiles == null) {
            this.participatingFiles = null;
        } else {
            this.participatingFiles = new ArrayList<>(participatingFiles);
        }
        dirty = true;
    }

    public List<Entry> getParticipatingFiles() {
        return participatingFiles != null ? new ArrayList<>(participatingFiles) : null;
    }

    @Override
    public void flush(boolean b) {
    }

    @Override
    public void clean() {
        file.delete();
        participatingFiles = null;
    }

    @Override
    public void close() throws IOException {
        if (dirty) {
            if (participatingFiles == null) {
                if (file.exists()) {
                    file.delete();
                }
            } else {
                try (Writer innerWriter = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
                        BufferedWriter writer = new BufferedWriter(innerWriter)) {
                    for (Entry participatingFile : participatingFiles) {
                        writer.append(participatingFile.path + ":" + participatingFile.timestamp);
                        writer.newLine();
                    }
                }
            }
        }
    }

    public static class Entry {
        public final String path;
        public final long timestamp;

        public Entry(String path, long timestamp) {
            this.path = path;
            this.timestamp = timestamp;
        }
    }
}
