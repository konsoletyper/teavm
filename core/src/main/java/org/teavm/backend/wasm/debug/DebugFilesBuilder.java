/*
 *  Copyright 2022 Alexey Andreev.
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
package org.teavm.backend.wasm.debug;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.Objects;

public class DebugFilesBuilder extends DebugSectionBuilder implements DebugFiles {
    private DebugStrings strings;
    private ObjectIntMap<FileData> fileMap = new ObjectIntHashMap<>();

    public DebugFilesBuilder(DebugStrings strings) {
        super(DebugConstants.SECTION_FILES);
        this.strings = strings;
    }

    @Override
    public int filePtr(String fileName) {
        var index = 0;
        var current = 0;
        while (true) {
            var next = fileName.indexOf('/', index);
            if (next < 0) {
                break;
            }
            var dirName = fileName.substring(index, next);
            current = filePtr(current, dirName);
            index = next + 1;
        }
        return filePtr(current, fileName.substring(index));
    }

    private int filePtr(int parent, String fileName) {
        var data = new FileData(parent, fileName);
        var ptr = fileMap.getOrDefault(data, 0);
        if (ptr == 0) {
            ptr = fileMap.size() + 1;
            fileMap.put(data, ptr);
            blob.writeLEB(parent);
            var extensionIndex = fileName.lastIndexOf('.');
            if (extensionIndex < 0) {
                blob.writeLEB(strings.stringPtr(fileName) << 1);
            } else {
                blob.writeLEB(1 | (strings.stringPtr(fileName.substring(0, extensionIndex)) << 1));
                blob.writeLEB(strings.stringPtr(fileName.substring(extensionIndex + 1)));
            }
        }
        return ptr;
    }

    private static class FileData {
        private final int parent;
        private final String name;

        FileData(int parent, String name) {
            this.parent = parent;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FileData fileData = (FileData) o;
            return parent == fileData.parent && name.equals(fileData.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(parent, name);
        }
    }
}
