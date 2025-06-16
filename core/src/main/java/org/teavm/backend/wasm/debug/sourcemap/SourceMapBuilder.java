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
package org.teavm.backend.wasm.debug.sourcemap;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import org.teavm.backend.wasm.debug.DebugLines;
import org.teavm.common.JsonUtil;
import org.teavm.debugging.information.SourceFileResolver;
import org.teavm.model.MethodReference;

public class SourceMapBuilder implements DebugLines {
    private static final String BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static final String IGNORE_FILE = "__ignore__";
    private List<String> fileNames = new ArrayList<>();
    private ObjectIntMap<String> fileNameIndexes = new ObjectIntHashMap<>();
    private int ptr;
    private StringBuilder mappings = new StringBuilder();
    private String currentFile;
    private int currentLine;
    private String lastWrittenFile;
    private int lastWrittenLine;
    private boolean pendingLocation;
    private Deque<InlineState> inlineStack = new ArrayDeque<>();
    private List<SourceFileResolver> sourceFileResolvers = new ArrayList<>();

    private String lastFileInMappings;
    private int lastLineInMappings;
    private int lastPtrInMappings;

    public void addSourceResolver(SourceFileResolver sourceFileResolver) {
        sourceFileResolvers.add(sourceFileResolver);
    }

    public void writeSourceMap(Writer output) throws IOException {
        output.write("{\"version\":3");

        var files = resolveFiles();
        if (!files.isEmpty()) {
            var commonPrefix = files.get(0);
            var commonPrefixLength = commonPrefix.length();
            for (var i = 1; i < files.size(); ++i) {
                var file = files.get(i);
                commonPrefixLength = Math.min(file.length(), commonPrefixLength);
                for (var j = 0; j < commonPrefixLength; ++j) {
                    if (commonPrefix.charAt(j) != file.charAt(j)) {
                        commonPrefixLength = j;
                        break;
                    }
                }
                if (commonPrefixLength == 0) {
                    break;
                }
            }
            if (commonPrefixLength > 0) {
                for (var i = 0; i < files.size(); ++i) {
                    files.set(i, files.get(i).substring(commonPrefixLength));
                }
                output.write(",\"sourceRoot\":\"");
                JsonUtil.writeEscapedString(output, commonPrefix.substring(0, commonPrefixLength));
                output.write("\"");
            }
        }
        output.write(",\"sources\":[");
        for (int i = 0; i < files.size(); ++i) {
            if (i > 0) {
                output.write(',');
            }
            output.write("\"");
            var name = files.get(i);
            JsonUtil.writeEscapedString(output, name);
            output.write("\"");
        }
        output.write("]");
        output.write(",\"names\":[]");
        output.write(",\"mappings\":\"");
        output.write(mappings.toString());
        output.write("\"");
        var ignoreFileIndex = fileNameIndexes.getOrDefault(IGNORE_FILE, -1);
        if (ignoreFileIndex > 0) {
            output.write(",\"x_google_ignoreList\":[" + ignoreFileIndex + "]");
        }
        output.write("}");
    }

    private List<String> resolveFiles() throws IOException {
        var result = new ArrayList<String>();
        for (var file : fileNames) {
            var resolvedFile = file;
            for (var resolver : sourceFileResolvers) {
                var candidate = resolver.resolveFile(file);
                if (candidate != null) {
                    resolvedFile = candidate;
                    break;
                }
            }
            result.add(resolvedFile);
        }
        return result;
    }

    @Override
    public void advance(int ptr) {
        if (ptr != this.ptr) {
            if (pendingLocation) {
                pendingLocation = false;
                if (!Objects.equals(currentFile, lastWrittenFile) || currentLine != lastWrittenLine) {
                    lastWrittenFile = currentFile;
                    lastWrittenLine = currentLine;
                    writeMapping(currentFile, currentLine, this.ptr);
                }
            }
            this.ptr = ptr;
        }
    }

    @Override
    public void location(String file, int line) {
        if (file == null) {
            file = IGNORE_FILE;
            line = 1;
        }
        currentFile = file;
        currentLine = line;
        pendingLocation = true;
    }

    @Override
    public void emptyLocation() {
        currentLine = 1;
        currentFile = IGNORE_FILE;
        pendingLocation = true;
    }

    @Override
    public void start(MethodReference methodReference) {
        inlineStack.push(new InlineState(currentFile, currentLine));
    }

    @Override
    public void end() {
        var state = inlineStack.pop();
        location(state.file, state.line);
    }

    private void writeMapping(String file, int line, int ptr) {
        if (mappings.length() > 0) {
            mappings.append(",");
        }
        writeVLQ(ptr - lastPtrInMappings);
        if (file != null && line > 0) {
            --line;
            var lastFileIndex = fileNameIndexes.get(lastFileInMappings);
            var fileIndex = fileNameIndexes.getOrDefault(file, -1);
            if (fileIndex < 0) {
                fileIndex = fileNames.size();
                fileNames.add(file);
                fileNameIndexes.put(file, fileIndex);
            }
            writeVLQ(fileIndex - lastFileIndex);
            writeVLQ(line - lastLineInMappings);
            writeVLQ(0);
            lastLineInMappings = line;
            lastFileInMappings = file;
        }
        lastPtrInMappings = ptr;
    }

    private void writeVLQ(int number) {
        if (number < 0) {
            number = ((-number) << 1) | 1;
        } else {
            number = number << 1;
        }
        do {
            int digit = number & 0x1F;
            int next = number >>> 5;
            if (next != 0) {
                digit |= 0x20;
            }
            mappings.append(BASE64_CHARS.charAt(digit));
            number = next;
        } while (number != 0);
    }

    private static class InlineState {
        String file;
        int line;

        InlineState(String file, int line) {
            this.file = file;
            this.line = line;
        }
    }
}
