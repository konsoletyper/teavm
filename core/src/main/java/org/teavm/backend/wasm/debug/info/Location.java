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
package org.teavm.backend.wasm.debug.info;

public class Location {
    private FileInfo file;
    private int line;
    private InliningLocation inlining;

    public Location(FileInfo file, int line, InliningLocation inlining) {
        this.file = file;
        this.line = line;
        this.inlining = inlining;
    }

    public FileInfo file() {
        return file;
    }

    public int line() {
        return line;
    }

    public InliningLocation inlining() {
        return inlining;
    }
}
