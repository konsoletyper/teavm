/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.debugging.information;

import java.io.IOException;
import java.io.Writer;
import org.teavm.common.JsonUtil;

class SourceMapsWriter {
    private static final String BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private Writer output;
    private int lastLine;
    private int lastColumn;
    private int lastSourceLine;
    private int lastSourceFile;
    private boolean first;

    public SourceMapsWriter(Writer output) {
        this.output = output;
    }

    public void write(String generatedFile, String sourceRoot, DebugInformation debugInfo) throws IOException {
        output.write("{\"version\":3");
        output.write(",\"file\":\"");
        JsonUtil.writeEscapedString(output, generatedFile);
        output.write("\"");
        output.write(",\"sourceRoot\":\"");
        JsonUtil.writeEscapedString(output, sourceRoot);
        output.write("\"");
        output.write(",\"sources\":[");
        for (int i = 0; i < debugInfo.fileNames.length; ++i) {
            if (i > 0) {
                output.write(',');
            }
            output.write("\"");
            JsonUtil.writeEscapedString(output, debugInfo.fileNames[i]);
            output.write("\"");
        }
        output.write("]");
        output.write(",\"names\":[]");
        output.write(",\"mappings\":\"");
        first = true;
        lastLine = 0;
        lastColumn = 0;
        lastSourceFile = 0;
        lastSourceLine = 0;
        for (SourceLocationIterator iter = debugInfo.iterateOverSourceLocations(); !iter.isEndReached(); iter.next()) {
            writeSegment(iter.getLocation(), iter.getFileNameId(), iter.getLine() - 1);
        }
        output.write("\"}");
    }

    private void writeSegment(GeneratedLocation loc, int sourceFile, int sourceLine)
            throws IOException {
        while (loc.getLine() > lastLine) {
            output.write(';');
            ++lastLine;
            first = true;
            lastColumn = 0;
        }
        if (!first) {
            output.write(',');
        }
        writeVLQ(loc.getColumn() - lastColumn);
        if (sourceFile >= 0 && sourceLine >= 0) {
            writeVLQ(sourceFile - lastSourceFile);
            writeVLQ(sourceLine - lastSourceLine);
            writeVLQ(0);
            lastSourceFile = sourceFile;
            lastSourceLine = sourceLine;
        }
        lastColumn = loc.getColumn();
        first = false;
    }

    private void writeVLQ(int number) throws IOException {
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
            output.write(BASE64_CHARS.charAt(digit));
            number = next;
        } while (number != 0);
    }
}
