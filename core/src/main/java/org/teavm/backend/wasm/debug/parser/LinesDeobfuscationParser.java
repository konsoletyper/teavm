/*
 *  Copyright 2024 Alexey Andreev.
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import org.teavm.backend.wasm.debug.info.LineInfo;

public class LinesDeobfuscationParser {
    private Map<String, DebugSectionParser> sectionParsers = new LinkedHashMap<>();
    private DebugLinesParser lines;

    public LinesDeobfuscationParser() {
        var strings = addSection(new DebugStringParser());
        var files = addSection(new DebugFileParser(strings));
        var packages = addSection(new DebugPackageParser(strings));
        var classes = addSection(new DebugClassParser(strings, packages));
        var methods = addSection(new DebugMethodParser(strings, classes));
        lines = addSection(new DebugLinesParser(files, methods));
    }

    private <T extends DebugSectionParser> T addSection(T section) {
        sectionParsers.put(section.name(), section);
        return section;
    }

    public boolean canHandleSection(String name) {
        return sectionParsers.keySet().contains(name);
    }

    public void applySection(String name, byte[] data) {
        var parser = sectionParsers.get(name);
        if (parser != null) {
            parser.parse(data);
        }
    }

    public void pullSections(Function<String, byte[]> provider) {
        for (var parser : sectionParsers.values()) {
            var section = provider.apply(parser.name());
            if (section != null) {
                parser.parse(section);
            }
        }
    }

    public LineInfo getLineInfo() {
        return lines.getLineInfo();
    }
}
