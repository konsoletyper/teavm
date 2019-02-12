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

import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.teavm.common.IntegerArray;
import org.teavm.common.RecordArray;
import org.teavm.debugging.information.DebugInformation.ClassMetadata;

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
        writeExactMethods(debugInfo.exactMethods);

        writeMapping(debugInfo.fileMapping);
        writeMapping(debugInfo.lineMapping);
        writeMapping(debugInfo.classMapping);
        writeMapping(debugInfo.methodMapping);
        writeLinesAndColumns(debugInfo.statementStartMapping);
        writeCallSiteMapping(debugInfo.callSiteMapping);
        writeVariableMappings(debugInfo);
        writeClassMetadata(debugInfo.classesMetadata);
        writeCFGs(debugInfo);
    }

    private void writeVariableMappings(DebugInformation debugInfo) throws IOException {
        int lastVar = 0;
        writeUnsignedNumber(nonNullVariableMappings(debugInfo));
        for (int i = 0; i < debugInfo.variableMappings.length; ++i) {
            RecordArray mapping = debugInfo.variableMappings[i];
            if (mapping == null) {
                continue;
            }
            writeUnsignedNumber(i - lastVar);
            lastVar = i;
            writeMultiMapping(mapping);
        }
    }

    private void writeClassMetadata(List<ClassMetadata> classes) throws IOException {
        for (int i = 0; i < classes.size(); ++i) {
            ClassMetadata cls = classes.get(i);
            writeNullableString(cls.jsName);
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

    private void writeExactMethods(long[] array) throws IOException {
        int lastClass = 0;
        int lastMethod = 0;
        writeUnsignedNumber(array.length);
        for (int i = 0; i < array.length; ++i) {
            long item = array[i];
            int classIndex = (int) (item >> 32);
            int methodIndex = (int) item;
            writeNumber(classIndex - lastClass);
            lastClass = classIndex;
            writeNumber(methodIndex - lastMethod);
            lastMethod = methodIndex;
        }
    }

    private void writeMultiMapping(RecordArray mapping) throws IOException {
        writeLinesAndColumns(mapping);
        for (int i = 0; i < mapping.size(); ++i) {
            int[] array = mapping.get(i).getArray(0);
            writeUnsignedNumber(array.length);
            int lastNumber = 0;
            for (int elem : array) {
                writeNumber(elem - lastNumber);
                lastNumber = elem;
            }
        }
    }

    private void writeMapping(RecordArray mapping) throws IOException {
        writeLinesAndColumns(mapping);
        writeRle(packValues(mapping));
    }

    private void writeCallSiteMapping(RecordArray mapping) throws IOException {
        writeLinesAndColumns(mapping);
        writeRle(packValues(mapping));
        writeRle(packCallSites(mapping));
    }

    private void writeLinesAndColumns(RecordArray mapping) throws IOException {
        writeUnsignedNumber(mapping.size());
        writeRle(packLines(mapping));
        writeRle(packColumns(mapping));
    }

    private int[] packLines(RecordArray mapping) {
        int[] lines = mapping.cut(0);
        int last = 0;
        for (int i = 0; i < lines.length; ++i) {
            int next = lines[i];
            lines[i] -= last;
            last = next;
        }
        return lines;
    }

    private int[] packColumns(RecordArray mapping) {
        int[] columns = mapping.cut(1);
        int lastLine = -1;
        int lastColumn = 0;
        for (int i = 0; i < columns.length; ++i) {
            if (lastLine != mapping.get(i).get(0)) {
                lastColumn = 0;
                lastLine = mapping.get(i).get(0);
            }
            int column = columns[i];
            columns[i] = column - lastColumn;
            lastColumn = column;
        }
        return columns;
    }

    private int[] packValues(RecordArray mapping) {
        int[] values = mapping.cut(2);
        int last = 0;
        for (int i = 0; i < values.length; ++i) {
            int value = values[i];
            if (value == -1) {
                values[i] = 0;
            } else {
                values[i] = 1 + convertToSigned(value - last);
                last = value;
            }
        }
        return values;
    }

    private int[] packCallSites(RecordArray mapping) {
        int[] callSites = mapping.cut(3);
        int last = 0;
        int j = 0;
        for (int i = 0; i < callSites.length; ++i) {
            int type = mapping.get(i).get(2);
            if (type != 0) {
                int callSite = callSites[i];
                callSites[j++] = convertToSigned(callSite - last);
                last = callSite;
            }
        }
        return Arrays.copyOf(callSites, j);
    }

    private void writeCFGs(DebugInformation debugInfo) throws IOException {
        for (int i = 0; i < debugInfo.controlFlowGraphs.length; ++i) {
            writeCFG(debugInfo.controlFlowGraphs[i]);
        }
    }

    private void writeCFG(RecordArray mapping) throws IOException {
        if (mapping == null) {
            writeUnsignedNumber(0);
            return;
        }
        writeUnsignedNumber(mapping.size());
        writeRle(mapping.cut(0));
        IntegerArray sizes = new IntegerArray(1);
        IntegerArray files = new IntegerArray(1);
        IntegerArray lines = new IntegerArray(1);
        int lastFile = 0;
        int lastLine = 0;
        for (int i = 0; i < mapping.size(); ++i) {
            int type = mapping.get(i).get(0);
            if (type == 0) {
                continue;
            }
            int[] data = mapping.get(i).getArray(0);
            sizes.add(data.length / 2);
            for (int j = 0; j < data.length; j += 2) {
                int file = data[j];
                int line = data[j + 1];
                files.add(convertToSigned(file - lastFile));
                lines.add(convertToSigned(line - lastLine));
                lastFile = file;
                lastLine = line;
            }
        }
        writeRle(sizes.getAll());
        writeRle(files.getAll());
        writeRle(lines.getAll());
    }

    private void writeNumber(int number) throws IOException {
        writeUnsignedNumber(convertToSigned(number));
    }

    private int convertToSigned(int number) {
        return number < 0 ? (-number << 1) | 1 : number << 1;
    }

    private void writeUnsignedNumber(int number) throws IOException {
        do {
            byte b = (byte) (number & 0x7F);
            if ((number & 0xFFFFFF80) != 0) {
                b |= 0x80;
            }
            number >>>= 7;
            output.writeByte(b);
        } while (number != 0);
    }

    private void writeRle(int[] array) throws IOException {
        int last = 0;
        for (int i = 0; i < array.length;) {
            int e = array[i];
            int count = 1;
            int current = i;
            ++i;
            while (i < array.length && array[i] == e) {
                ++count;
                ++i;
            }
            if (count > 1) {
                if (current > last) {
                    writeUnsignedNumber((current - last) << 1);
                    while (last < current) {
                        writeUnsignedNumber(array[last++]);
                    }
                }
                writeUnsignedNumber((count << 1) | 1);
                writeUnsignedNumber(e);
                last = i;
            }
        }
        if (array.length > last) {
            writeUnsignedNumber((array.length - last) << 1);
            while (last < array.length) {
                writeUnsignedNumber(array[last++]);
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
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeUnsignedNumber(bytes.length);
        output.write(bytes);
    }

    private void writeNullableString(String str) throws IOException {
        if (str == null) {
            writeUnsignedNumber(0);
            return;
        }
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeUnsignedNumber(bytes.length + 1);
        output.write(bytes);
    }
}
