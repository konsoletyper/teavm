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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.teavm.common.RecordArray;
import org.teavm.common.RecordArrayBuilder;

class DebugInformationReader {
    private InputStream input;
    private int lastNumber;

    public DebugInformationReader(InputStream input) {
        this.input = input;
    }

    public DebugInformation read() throws IOException {
        DebugInformation debugInfo = new DebugInformation();
        debugInfo.fileNames = readStrings();
        debugInfo.classNames = readStrings();
        debugInfo.fields = readStrings();
        debugInfo.methods = readStrings();
        debugInfo.variableNames = readStrings();
        debugInfo.exactMethods = readExactMethods();
        debugInfo.fileMapping = readMapping();
        debugInfo.lineMapping = readMapping();
        debugInfo.classMapping = readMapping();
        debugInfo.methodMapping = readMapping();
        debugInfo.statementStartMapping = readBooleanMapping();
        debugInfo.callSiteMapping = readCallSiteMapping();
        debugInfo.variableMappings = readVariableMappings(debugInfo.variableNames.length);
        debugInfo.classesMetadata = readClassesMetadata(debugInfo.classNames.length);
        debugInfo.controlFlowGraphs = readCFGs(debugInfo.fileNames.length);
        debugInfo.rebuild();
        return debugInfo;
    }

    private RecordArray[] readVariableMappings(int count) throws IOException {
        RecordArray[] mappings = new RecordArray[count];
        int varCount = readUnsignedNumber();
        int lastVar = 0;
        while (varCount-- > 0) {
            lastVar += readUnsignedNumber();
            mappings[lastVar] = readMultiMapping();
        }
        return mappings;
    }

    private List<DebugInformation.ClassMetadata> readClassesMetadata(int count) throws IOException {
        List<DebugInformation.ClassMetadata> classes = new ArrayList<>(count);
        for (int i = 0; i < count; ++i) {
            DebugInformation.ClassMetadata cls = new DebugInformation.ClassMetadata();
            classes.add(cls);
            cls.id = i;
            cls.jsName = readNullableString();
            cls.parentId = readUnsignedNumber() - 1;
            if (cls.parentId.equals(-1)) {
                cls.parentId = null;
            }
            int entryCount = readUnsignedNumber();
            resetRelativeNumber();
            for (int j = 0; j < entryCount; ++j) {
                int key = readRelativeNumber();
                int value = readUnsignedNumber();
                cls.fieldMap.put(key, value);
            }
        }
        return classes;
    }

    private RecordArray[] readCFGs(int count) throws IOException {
        RecordArray[] cfgs = new RecordArray[count];
        for (int i = 0; i < count; ++i) {
            cfgs[i] = readCFG();
        }
        return cfgs;
    }

    private RecordArray readCFG() throws IOException {
        RecordArrayBuilder builder = new RecordArrayBuilder(1, 1);
        int size = readUnsignedNumber();
        for (int i = 0; i < size; ++i) {
            builder.add();
        }
        int[] types = readRle(size);
        int nonEmptyItems = 0;
        for (int i = 0; i < size; ++i) {
            int type = types[i];
            builder.get(i).set(0, type);
            if (type != 0) {
                ++nonEmptyItems;
            }
        }
        int[] sizes = readRle(nonEmptyItems);
        int j = 0;
        int totalSize = 0;
        for (int sz : sizes) {
            totalSize += sz;
        }
        int[] files = readRle(totalSize);
        int[] lines = readRle(totalSize);
        int lastFile = 0;
        int lastLine = 0;
        int index = 0;
        for (int i = 0; i < sizes.length; ++i) {
            while (types[j] == 0) {
                ++j;
            }
            size = sizes[i];
            RecordArrayBuilder.SubArray array = builder.get(j++).getArray(0);
            for (int k = 0; k < size; ++k) {
                lastFile += processSign(files[index]);
                lastLine += processSign(lines[index]);
                array.add(lastFile);
                array.add(lastLine);
                ++index;
            }
        }
        return builder.build();
    }

    private int processSign(int number) {
        boolean negative = (number & 1) != 0;
        number >>>= 1;
        return !negative ? number : -number;
    }

    private RecordArray readMultiMapping() throws IOException {
        RecordArrayBuilder builder = readLinesAndColumns(2, 1);
        for (int i = 0; i < builder.size(); ++i) {
            int count = readUnsignedNumber();
            RecordArrayBuilder.SubArray array = builder.get(i).getArray(0);
            int last = 0;
            for (int j = 0; j < count; ++j) {
                last += readNumber();
                array.add(last);
            }
        }
        return builder.build();
    }

    private RecordArray readBooleanMapping() throws IOException {
        RecordArrayBuilder builder = readLinesAndColumns(2, 0);
        return builder.build();
    }

    private RecordArray readMapping() throws IOException {
        RecordArrayBuilder builder = readLinesAndColumns(3, 0);
        readValues(builder);
        return builder.build();
    }

    private RecordArray readCallSiteMapping() throws IOException {
        RecordArrayBuilder builder = readLinesAndColumns(4, 0);
        readValues(builder);
        readCallSites(builder);
        return builder.build();
    }

    private RecordArrayBuilder readLinesAndColumns(int fields, int arrays) throws IOException {
        RecordArrayBuilder builder = new RecordArrayBuilder(fields, arrays);
        int size = readUnsignedNumber();
        for (int i = 0; i < size; ++i) {
            builder.add();
        }
        int[] lines = extractLines(readRle(builder.size()));
        int[] columns = extractColumns(readRle(builder.size()), lines);
        for (int i = 0; i < builder.size(); ++i) {
            RecordArrayBuilder.Record record = builder.get(i);
            record.set(0, lines[i]);
            record.set(1, columns[i]);
        }
        return builder;
    }

    private void readValues(RecordArrayBuilder builder) throws IOException {
        int[] values = extractValues(readRle(builder.size()));
        for (int i = 0; i < builder.size(); ++i) {
            builder.get(i).set(2, values[i]);
        }
    }

    private void readCallSites(RecordArrayBuilder builder) throws IOException {
        int sz = 0;
        for (int i = 0; i < builder.size(); ++i) {
            if (builder.get(i).get(2) != 0) {
                ++sz;
            }
        }
        int[] data = readRle(sz);
        int j = 0;
        int last = 0;
        for (int i = 0; i < builder.size(); ++i) {
            if (builder.get(i).get(2) != 0) {
                last += processSign(data[j++]);
                builder.get(i).set(3, last);
            }
        }
    }

    private int[] extractLines(int[] lines) {
        int last = 0;
        for (int i = 0; i < lines.length; ++i) {
            last += lines[i];
            lines[i] = last;
        }
        return lines;
    }

    private int[] extractColumns(int[] columns, int[] lines) {
        int last = 0;
        int lastLine = -1;
        for (int i = 0; i < columns.length; ++i) {
            if (lines[i] != lastLine) {
                lastLine = lines[i];
                last = 0;
            }
            last += columns[i];
            columns[i] = last;
        }
        return columns;
    }

    private int[] extractValues(int[] values) {
        int last = 0;
        for (int i = 0; i < values.length; ++i) {
            int value = values[i];
            if (value == 0) {
                values[i] = -1;
            } else {
                last += processSign(value - 1);
                values[i] = last;
            }
        }
        return values;
    }

    private String[] readStrings() throws IOException {
        String[] array = new String[readUnsignedNumber()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = readString();
        }
        return array;
    }

    private long[] readExactMethods() throws IOException {
        long[] result = new long[readUnsignedNumber()];
        int lastClass = 0;
        int lastMethod = 0;
        for (int i = 0; i < result.length; ++i) {
            lastClass += readNumber();
            lastMethod += readNumber();
            result[i] = ((long) lastClass << 32) | lastMethod;
        }
        return result;
    }

    private int[] readRle(int size) throws IOException {
        int[] array = new int[size];
        for (int i = 0; i < size;) {
            int count = readUnsignedNumber();
            boolean repeat = (count & 1) != 0;
            count >>>= 1;
            if (!repeat) {
                while (count-- > 0) {
                    array[i++] = readUnsignedNumber();
                }
            } else {
                int n = readUnsignedNumber();
                while (count-- > 0) {
                    array[i++] = n;
                }
            }
        }
        return array;
    }

    private int readNumber() throws IOException {
        return processSign(readUnsignedNumber());
    }

    private int readUnsignedNumber() throws IOException {
        int number = 0;
        int shift = 0;
        while (true) {
            int r = input.read();
            if (r < 0) {
                throw new EOFException();
            }
            byte b = (byte) r;
            number |= (b & 0x7F) << shift;
            shift += 7;
            if ((b & 0x80) == 0) {
                break;
            }
        }
        return number;
    }

    private int readRelativeNumber() throws IOException {
        lastNumber += readNumber();
        return lastNumber;
    }

    private void resetRelativeNumber() {
        lastNumber = 0;
    }

    private String readString() throws IOException {
        return readStringChars(readUnsignedNumber());
    }

    private String readNullableString() throws IOException {
        int size = readUnsignedNumber();
        return size > 0 ? readStringChars(size - 1) : null;
    }

    private String readStringChars(int size) throws IOException {
        byte[] bytes = new byte[size];
        int pos = 0;
        while (pos < bytes.length) {
            int read = input.read(bytes, pos, bytes.length - pos);
            if (read == -1) {
                throw new EOFException();
            }
            pos += read;
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
