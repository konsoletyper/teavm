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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.teavm.common.IntegerArray;

/**
 *
 * @author Alexey Andreev
 */
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
        debugInfo.callSiteMapping = readMapping();
        debugInfo.variableMappings = readVariableMappings(debugInfo.variableNames.length);
        debugInfo.classesMetadata = readClassesMetadata(debugInfo.classNames.length);
        debugInfo.controlFlowGraphs = readCFGs(debugInfo.fileNames.length);
        debugInfo.rebuildFileDescriptions();
        debugInfo.rebuildMaps();
        return debugInfo;
    }

    private DebugInformation.MultiMapping[] readVariableMappings(int count) throws IOException {
        DebugInformation.MultiMapping[] mappings = new DebugInformation.MultiMapping[count];
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

    private DebugInformation.CFG[] readCFGs(int count) throws IOException {
        DebugInformation.CFG[] cfgs = new DebugInformation.CFG[count];
        for (int i = 0; i < count; ++i) {
            cfgs[i] = readCFG(i);
        }
        return cfgs;
    }

    private DebugInformation.CFG readCFG(int index) throws IOException {
        IntegerArray offsets = new IntegerArray(1);
        int[] lines = new int[readUnsignedNumber()];
        int[] files = new int[lines.length];
        int i = 0;
        int line = 0;
        offsets.add(0);
        while (i < lines.length) {
            line += readUnsignedNumber();
            int sz = readUnsignedNumber();
            if (sz == 0) {
                lines[i] = -1;
                files[i++] = -1;
            } else if (sz == 1) {
                lines[i] = line + 1;
                files[i++] = index;
            }
            sz -= 1;
            int last = line;
            for (int j = 0; j < sz; ++j) {
                last += readNumber();
                lines[i] = last;
                files[i++] = index + readNumber();
            }
            offsets.add(i);
        }
        DebugInformation.CFG cfg = new DebugInformation.CFG();
        cfg.offsets = offsets.getAll();
        cfg.lines = lines;
        cfg.files = files;
        return cfg;
    }

    private int processSign(int number) {
        boolean negative = (number & 1) != 0;
        number >>>= 1;
        return !negative ? number : -number;
    }

    private DebugInformation.MultiMapping readMultiMapping() throws IOException {
        int[] lines = readRle();
        int last = 0;
        for (int i = 0; i < lines.length; ++i) {
            last += lines[i];
            lines[i] = last;
        }
        int[] columns = new int[lines.length];
        resetRelativeNumber();
        for (int i = 0; i < columns.length; ++i) {
            columns[i] = readRelativeNumber();
        }
        int[] offsets = new int[lines.length + 1];
        int lastOffset = 0;
        for (int i = 1; i < offsets.length; ++i) {
            lastOffset += readUnsignedNumber();
            offsets[i] = lastOffset;
        }
        int[] data = new int[lastOffset];
        resetRelativeNumber();
        for (int i = 0; i < data.length; ++i) {
            data[i] = readRelativeNumber();
        }
        return new DebugInformation.MultiMapping(lines, columns, offsets, data);
    }

    private DebugInformation.Mapping readMapping() throws IOException {
        int[] lines = readRle();
        int last = 0;
        for (int i = 0; i < lines.length; ++i) {
            last += lines[i];
            lines[i] = last;
        }
        int[] columns = new int[lines.length];
        resetRelativeNumber();
        for (int i = 0; i < columns.length; ++i) {
            columns[i] = readRelativeNumber();
        }
        int[] values = new int[lines.length];
        resetRelativeNumber();
        for (int i = 0; i < values.length; ++i) {
            values[i] = readRelativeNumber();
        }
        return new DebugInformation.Mapping(lines, columns, values);
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
            result[i] = ((long)lastClass << 32) | lastMethod;
        }
        return result;
    }

    private int[] readRle() throws IOException {
        int[] array = new int[readUnsignedNumber()];
        for (int i = 0; i < array.length;) {
            int n = readUnsignedNumber();
            int count = 1;
            if ((n & 1) != 0) {
                count = readUnsignedNumber();
            }
            n = processSign(n >>> 1);
            while (count-- > 0) {
                array[i++] = n;
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
            byte b = (byte)r;
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
        byte[] bytes = new byte[readUnsignedNumber()];
        int pos = 0;
        while (pos < bytes.length) {
            int read = input.read(bytes, pos, bytes.length - pos);
            if (read == -1) {
                throw new EOFException();
            }
            pos += read;
        }
        return new String(bytes, "UTF-8");
    }
}
