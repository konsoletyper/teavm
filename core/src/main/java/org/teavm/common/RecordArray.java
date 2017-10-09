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
package org.teavm.common;

import java.util.Arrays;

public class RecordArray {
    private int recordSize;
    private int arraysPerRecord;
    private int[] data;
    private int[] substart;
    private int[] subdata;
    private int size;

    RecordArray(int recordSize, int arraysPerRecord, int size, int[] data, int[] substart, int[] subdata) {
        this.recordSize = recordSize;
        this.arraysPerRecord = arraysPerRecord;
        this.size = size;
        this.data = data;
        this.substart = substart;
        this.subdata = subdata;
    }

    public Record get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index " + index + " is outside of [0; " + size + ")");
        }
        return new Record(index * recordSize, index * arraysPerRecord);
    }

    public int size() {
        return size;
    }

    public int getRecordSize() {
        return recordSize;
    }

    public int arraysPerRecord() {
        return arraysPerRecord;
    }

    public int[] cut(int index) {
        if (index < 0 || index >= recordSize) {
            throw new IndexOutOfBoundsException("Index " + index + " is outside of [0; " + recordSize + ")");
        }
        int[] result = new int[size];
        for (int i = 0; i < size; ++i) {
            result[i] = data[index];
            index += recordSize;
        }
        return result;
    }

    public class Record {
        int offset;
        int arrayOffset;

        Record(int offset, int arrayOffset) {
            this.offset = offset;
            this.arrayOffset = arrayOffset;
        }

        public int getPosition() {
            return offset / recordSize;
        }

        public int get(int index) {
            if (index >= recordSize) {
                throw new IndexOutOfBoundsException("Index out of bounds: " + index + " of " + recordSize);
            }
            return data[offset + index];
        }

        public int size() {
            return recordSize;
        }

        public int[] getArray(int index) {
            if (index > arraysPerRecord) {
                throw new IndexOutOfBoundsException("Index out of bounds: " + index + " of " + arraysPerRecord);
            }
            int start = substart[arrayOffset + index];
            int end = substart[arrayOffset + index + 1];
            return Arrays.copyOfRange(subdata, start, end);
        }

        public int numArrays() {
            return arraysPerRecord;
        }
    }
}
