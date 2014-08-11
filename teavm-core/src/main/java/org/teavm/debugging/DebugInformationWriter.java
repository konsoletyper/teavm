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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.teavm.debugging.DebugInformation.ClassMetadata;

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
        writeStringArray(debugInfo.fileNames);
        writeStringArray(debugInfo.classNames);
        writeStringArray(debugInfo.fields);
        writeStringArray(debugInfo.methods);
        writeStringArray(debugInfo.variableNames);

        writeMapping(debugInfo.fileMapping);
        writeMapping(debugInfo.lineMapping);
        writeMapping(debugInfo.classMapping);
        writeMapping(debugInfo.methodMapping);
        writeVariableMappings(debugInfo);
        writeClassMetadata(debugInfo.classesMetadata);
        writeCFGs(debugInfo);
    }

    private void writeVariableMappings(DebugInformation debugInfo) throws IOException {
        int lastVar = 0;
        writeUnsignedNumber(nonNullVariableMappings(debugInfo));
        for (int i = 0; i < debugInfo.variableMappings.length; ++i) {
            DebugInformation.MultiMapping mapping = debugInfo.variableMappings[i];
            if (mapping == null) {
                continue;
            }
            writeUnsignedNumber(i - lastVar);
            lastVar = i;
            writeMapping(mapping);
        }
    }

    private void writeClassMetadata(List<ClassMetadata> classes) throws IOException {
        for (int i = 0; i < classes.size(); ++i) {
            ClassMetadata cls = classes.get(i);
            writeUnsignedNumber(cls.parentId != null ? cls.parentId + 1 : 0);
            writeUnsignedNumber(cls.fieldMap.size());
            List<Integer> keys = new ArrayList<>(cls.fieldMap.keySet());
            Collections.sort(keys);
            resetRelativeNumber();
            for (int key : keys) {
                writeRelativeNumber(key);
                writeUnsignedNumber(cls.fieldMap.get(key));
            }
        }
    }

    private int nonNullVariableMappings(DebugInformation debugInfo) {
        int count = 0;
        for (int i = 0; i < debugInfo.variableMappings.length; ++i) {
            if (debugInfo.variableMappings[i] != null) {
                ++count;
            }
        }
        return count;
    }

    private void writeStringArray(String[] array) throws IOException {
        writeUnsignedNumber(array.length);
        for (int i = 0; i < array.length; ++i) {
            writeString(array[i]);
        }
    }

    private void writeMapping(DebugInformation.MultiMapping mapping) throws IOException {
        int[] lines = mapping.lines.clone();
        int last = 0;
        for (int i = 0; i < lines.length; ++i) {
            int next = lines[i];
            lines[i] -= last;
            last = next;
        }
        writeRle(lines);
        resetRelativeNumber();
        for (int i = 0; i < mapping.columns.length; ++i) {
            writeRelativeNumber(mapping.columns[i]);
        }
        int lastOffset = 0;
        for (int i = 1; i < mapping.offsets.length; ++i) {
            writeUnsignedNumber(mapping.offsets[i] - lastOffset);
            lastOffset = mapping.offsets[i];
        }
        resetRelativeNumber();
        for (int i = 0; i < mapping.data.length; ++i) {
            writeRelativeNumber(mapping.data[i]);
        }
    }

    private void writeMapping(DebugInformation.Mapping mapping) throws IOException {
        int[] lines = mapping.lines.clone();
        int last = 0;
        for (int i = 0; i < lines.length; ++i) {
            int next = lines[i];
            lines[i] -= last;
            last = next;
        }
        writeRle(lines);
        resetRelativeNumber();
        for (int i = 0; i < mapping.columns.length; ++i) {
            writeRelativeNumber(mapping.columns[i]);
        }
        resetRelativeNumber();
        for (int i = 0; i < mapping.values.length; ++i) {
            writeRelativeNumber(mapping.values[i]);
        }
    }

    private void writeCFGs(DebugInformation debugInfo) throws IOException {
        for (int i = 0; i < debugInfo.controlFlowGraphs.length; ++i) {
            writeCFG(debugInfo.controlFlowGraphs[i], i);
        }
    }

    private void writeCFG(DebugInformation.CFG mapping, int fileIndex) throws IOException {
        writeUnsignedNumber(mapping.offsets.length - 1);
        for (int i = 0; i < mapping.offsets.length - 1; ++i) {
            int start = mapping.offsets[i];
            int sz = mapping.offsets[i + 1] - start;
            if (sz == 0) {
                writeUnsignedNumber(0);
            } else if (sz == 1 && mapping.lines[start] == -1) {
                writeUnsignedNumber(1);
            } else if (sz == 1 && mapping.lines[start] == i + 1 && mapping.files[start] == fileIndex) {
                writeUnsignedNumber(2);
            } else {
                writeUnsignedNumber(2 + sz);
                int[] lines = Arrays.copyOfRange(mapping.lines, start, start + sz);
                int[] files = Arrays.copyOfRange(mapping.files, start, start + sz);
                int last = i;
                for (int j = 0; j < sz; ++j) {
                    int succ = lines[j];
                    writeNumber(succ - last);
                    writeNumber(files[j] - fileIndex);
                    last = succ;
                }
            }
        }
    }

    private void writeNumber(int number) throws IOException {
        writeUnsignedNumber(convertToSigned(number));
    }

    private int convertToSigned(int number) {
        return number < 0 ? (-number << 1) | 1 : number << 1;
    }

    private void writeUnsignedNumber(int number) throws IOException {
        do {
            byte b = (byte)(number & 0x7F);
            if ((number & 0xFFFFFF80) != 0) {
                b |= 0x80;
            }
            number >>>= 7;
            output.writeByte(b);
        } while (number != 0);
    }

    private void writeRle(int[] array) throws IOException {
        writeUnsignedNumber(array.length);
        for (int i = 0; i < array.length;) {
            int e = array[i];
            int count = 1;
            ++i;
            while (i < array.length && array[i] == e) {
                ++count;
                ++i;
            }
            if (count > 1) {
                writeUnsignedNumber((convertToSigned(e) << 1) | 1);
                writeUnsignedNumber(count);
            } else {
                writeUnsignedNumber(convertToSigned(e) << 1);
            }
        }
    }

    private void writeRelativeNumber(int number) throws IOException {
        writeNumber(number - lastNumber);
        lastNumber = number;
    }

    private void resetRelativeNumber() {
        lastNumber = 0;
    }

    private void writeString(String str) throws IOException {
        byte[] bytes = str.getBytes("UTF-8");
        writeUnsignedNumber(bytes.length);
        output.write(bytes);
    }
}
