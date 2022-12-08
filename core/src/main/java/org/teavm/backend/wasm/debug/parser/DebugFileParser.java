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
package org.teavm.backend.wasm.debug.parser;

import java.util.ArrayList;
import org.teavm.backend.wasm.debug.DebugConstants;
import org.teavm.backend.wasm.debug.info.FileInfo;

public class DebugFileParser extends DebugSectionParser {
    private DebugStringParser strings;
    private FileInfoImpl[] files;

    public DebugFileParser(DebugStringParser strings) {
        super(DebugConstants.SECTION_FILES, strings);
        this.strings = strings;
    }

    public FileInfo getFile(int index) {
        return index > 0 ? files[index - 1] : null;
    }

    @Override
    protected void doParse() {
        var builders = new ArrayList<FileBuilder>();
        while (ptr < data.length) {
            var parent = readLEB();
            var nameIndex = readLEB();
            var name = (nameIndex & 1) == 0
                    ? strings.getString(nameIndex >>> 1)
                    : strings.getString(nameIndex >>> 1) + "." + strings.getString(readLEB());
            builders.add(new FileBuilder(parent, name));
        }
        this.files = new FileInfoImpl[builders.size()];
        for (var i = 0; i < files.length; ++i) {
            files[i] = new FileInfoImpl(builders.get(i).name);
        }
        for (var i = 0; i < files.length; ++i) {
            files[i].parent = getFile(builders.get(i).parent);
        }
    }

    private static class FileInfoImpl extends FileInfo {
        private FileInfo parent;
        private final String name;

        private FileInfoImpl(String name) {
            this.name = name;
        }

        @Override
        public FileInfo parent() {
            return parent;
        }

        @Override
        public String name() {
            return name;
        }
    }

    private static class FileBuilder {
        int parent;
        String name;

        FileBuilder(int parent, String name) {
            this.parent = parent;
            this.name = name;
        }
    }
}
