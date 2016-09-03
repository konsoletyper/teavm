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
package org.teavm.backend.wasm.render;

import java.util.Objects;
import org.teavm.model.TextLocation;

class CSingleLine extends CLine {
    private String text;
    private TextLocation location;

    public CSingleLine(String text) {
        this(text, null);
    }

    public CSingleLine(String text, TextLocation location) {
        this.text = text;
        this.location = location;
    }

    public String getText() {
        return text;
    }

    public TextLocation getLocation() {
        return location;
    }

    public void setLocation(TextLocation location) {
        this.location = location;
    }

    @Override
    void render(WasmCRenderer target) {
        if (target.lineNumbersEmitted) {
            TextLocation location = this.location;
            if (location == null) {
                location = target.lastReportedLocation;
            }
            if (location != null) {
                if (!Objects.equals(target.currentFile, location.getFileName())) {
                    target.line("#line " + (location.getLine() - 1) + " \"" + location.getFileName() + "\"");
                } else if (location.getLine() != target.currentLine) {
                    target.line("#line " + (location.getLine() - 1));
                }
                target.currentFile = location.getFileName();
                target.currentLine = location.getLine() + 1;
                target.lastReportedLocation = location;
            }
        }
        target.line(text);
    }
}
