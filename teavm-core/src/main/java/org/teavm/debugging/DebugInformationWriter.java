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
package org.teavm.debugging;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.teavm.common.IntegerArray;
import org.teavm.debugging.DebugInformation.FileDescription;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev
 */
class DebugInformationWriter {
    private DataOutput output;
    private int lastNumber;

    public DebugInformationWriter(DataOutput output) {
        this.output = output;
    }

    public void write(DebugInformation debugInfo) throws IOException {
        writeNumber(debugInfo.fileNames.length);
        for (int i = 0; i < debugInfo.fileNames.length; ++i) {
            String fileName = debugInfo.fileNames[i];
            writeString(fileName);
            writeMethods(debugInfo.fileDescriptions[i]);
        }

        writeNumber(debugInfo.fileNameKeys.length);
        resetRelativeNumber();
        for (int i = 0; i < debugInfo.fileNameKeys.length; ++i) {
            writeRelativeNumber(debugInfo.fileNameKeys[i].getLine());
        }
        resetRelativeNumber();
        for (int i = 0; i < debugInfo.fileNameKeys.length; ++i) {
            writeRelativeNumber(debugInfo.fileNameKeys[i].getColumn());
        }
        resetRelativeNumber();
        for (int i = 0; i < debugInfo.fileNameValues.length; ++i) {
            writeRelativeNumber(debugInfo.fileNameValues[i]);
        }

        writeNumber(debugInfo.lineNumberKeys.length);
        resetRelativeNumber();
        resetRelativeNumber();
        for (int i = 0; i < debugInfo.lineNumberKeys.length; ++i) {
            writeRelativeNumber(debugInfo.lineNumberKeys[i].getLine());
        }
        resetRelativeNumber();
        for (int i = 0; i < debugInfo.lineNumberKeys.length; ++i) {
            writeRelativeNumber(debugInfo.lineNumberKeys[i].getColumn());
        }
        resetRelativeNumber();
        for (int i = 0; i < debugInfo.fileNameValues.length; ++i) {
            writeRelativeNumber(debugInfo.lineNumberValues[i]);
        }
    }

    private void writeMethods(FileDescription fileDesc) throws IOException {
        Map<MethodReference, IntegerArray> methodLineMap = new HashMap<>();
        for (int i = 0; i < fileDesc.methodMap.length; ++i) {
            MethodReference method = fileDesc.methodMap[i];
            if (method == null) {
                continue;
            }
            IntegerArray lines = methodLineMap.get(method);
            if (lines == null) {
                lines = new IntegerArray(1);
                methodLineMap.put(method, lines);
            }
            lines.add(i);
        }
        writeNumber(methodLineMap.size());
        for (MethodReference method : methodLineMap.keySet()) {
            writeString(method.toString());
            int[] lines = methodLineMap.get(method).getAll();
            Arrays.sort(lines);
            for (int i = 0; i < lines.length;) {
                writeRelativeNumber(i);
                int j = i;
                int last = lines[i];
                ++i;
                while (i < lines.length && lines[i] == last + 1) {
                    ++i;
                    ++last;
                }
                writeNumber(i - j);
            }
            writeRelativeNumber(-1);
        }
    }

    private void writeNumber(int number) throws IOException {
        do {
            number = (number << 1) | (number >>> 31);
            byte b = (byte)(number & 0x7F);
            if ((number & 0xFFFFFF80) != 0) {
                b |= 0x80;
            }
            number >>>= 7;
            output.writeByte(b);
        } while (number != 0);
    }

    private void writeRelativeNumber(int number) throws IOException {
        writeNumber(number - lastNumber);
        lastNumber = number;
    }

    private void resetRelativeNumber() {
        lastNumber = 0;
    }

    private void writeString(String str) throws IOException {
        writeNumber(str.length());
        output.write(str.getBytes("UTF-8"));
    }
}
