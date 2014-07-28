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
        writeStringArray(debugInfo.methods);

        writeMapping(debugInfo.fileMapping);
        writeMapping(debugInfo.lineMapping);
        writeMapping(debugInfo.classMapping);
        writeMapping(debugInfo.methodMapping);
    }

    private void writeStringArray(String[] array) throws IOException {
        writeUnsignedNumber(array.length);
        for (int i = 0; i < array.length; ++i) {
            writeString(array[i]);
        }
    }

    private void writeMapping(DebugInformation.Mapping mapping) throws IOException {
        writeUnsignedNumber(mapping.lines.length);
        int[] lines = mapping.lines.clone();
        int last = 0;
        for (int i = 0; i < lines.length; ++i) {
            last = lines[i];
            lines[i] -= last;
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
