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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.teavm.backend.wasm.debug.info.ControlFlowInfo;
import org.teavm.backend.wasm.debug.info.DebugInfo;
import org.teavm.backend.wasm.parser.CodeSectionParser;
import org.teavm.backend.wasm.parser.ModuleParser;
import org.teavm.common.AsyncInputStream;
import org.teavm.common.ByteArrayAsyncInputStream;

public class DebugInfoParser extends ModuleParser {
    private Map<String, DebugSectionParser> sectionParsers = new HashMap<>();
    private DebugLinesParser lines;
    private DebugVariablesParser variables;
    private ControlFlowInfo controlFlow;
    private DebugClassLayoutParser classLayoutInfo;
    private int offset;

    public DebugInfoParser(AsyncInputStream reader) {
        super(reader);
        var strings = addSection(new DebugStringParser());
        var files = addSection(new DebugFileParser(strings));
        var packages = addSection(new DebugPackageParser(strings));
        var classes = addSection(new DebugClassParser(strings, packages));
        var methods = addSection(new DebugMethodParser(strings, classes));
        variables = addSection(new DebugVariablesParser(strings));
        lines = addSection(new DebugLinesParser(files, methods));
        classLayoutInfo = addSection(new DebugClassLayoutParser(strings, classes));
    }

    private <T extends DebugSectionParser> T addSection(T section) {
        sectionParsers.put(section.name(), section);
        return section;
    }

    public DebugInfo getDebugInfo() {
        return new DebugInfo(variables.getVariablesInfo(), lines.getLineInfo(), controlFlow,
                classLayoutInfo.getInfo(), offset);
    }

    @Override
    protected Consumer<byte[]> getSectionConsumer(int code, int pos, String name) {
        if (code == 0) {
            var parser = sectionParsers.get(name);
            return parser != null ? parser::parse : null;
        } else if (code == 10) {
            this.offset = pos;
            return this::parseCode;
        }
        return null;
    }

    private void parseCode(byte[] data) {
        var builder = new ControlFlowParser();
        var codeParser = new CodeSectionParser(builder, builder);
        codeParser.parse(data);
        controlFlow = builder.build();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Pass single argument - path to wasm file");
            System.exit(1);
        }
        var file = new File(args[0]);
        var input = new ByteArrayAsyncInputStream(Files.readAllBytes(file.toPath()));
        var parser = new DebugInfoParser(input);
        input.readFully(parser::parse);
        var debugInfo = parser.getDebugInfo();
        if (debugInfo != null) {
            debugInfo.dump(System.out);
        } else {
            System.out.println("No debug information found");
        }
    }
}
