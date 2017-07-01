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

public class RecordArrayBuilder {
    private int recordSize;
    private int arraysPerRecord;
    private int size;
    private IntegerArray data = new IntegerArray(1);
    private IntegerArray substart = new IntegerArray(1);
    private IntegerArray subdata = new IntegerArray(1);
    private IntegerArray subnext = new IntegerArray(1);

    public RecordArrayBuilder(int recordSize, int arraysPerRecord) {
        this.recordSize = recordSize;
        this.arraysPerRecord = arraysPerRecord;
    }

    public Record get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index " + index + " is outside of [0; " + size + ")");
        }
        return new Record(index * recordSize, index * arraysPerRecord);
    }

    public Record add() {
        int offset = data.size();
        for (int i = 0; i < recordSize; ++i) {
            data.add(0);
        }
        int arrayOffset = substart.size();
        for (int i = 0; i < arraysPerRecord; ++i) {
            substart.add(-1);
        }
        ++size;
        return new Record(offset, arrayOffset);
    }

    public int size() {
        return size;
    }

    public int getRecordSize() {
        return recordSize;
    }

    public int getArraysPerRecord() {
        return arraysPerRecord;
    }

    public RecordArray build() {
        int[] builtSubstart = new int[substart.size() + 1];
        IntegerArray builtSubdata = new IntegerArray(1);
        for (int i = 0; i < substart.size(); ++i) {
            int ptr = substart.get(i);
            while (ptr >= 0) {
                builtSubdata.add(subdata.get(ptr));
                ptr = subnext.get(ptr);
            }
            builtSubstart[i + 1] = builtSubdata.size();
        }
        int[] builtSubdataArray = builtSubdata.getAll();
        for (int i = 1; i < builtSubstart.length; ++i) {
            int start = builtSubstart[i - 1];
            int end = builtSubstart[i];
            int h = (builtSubstart[i] - start) / 2;
            for (int j = 0; j < h; ++j) {
                int tmp = builtSubdataArray[start + j];
                builtSubdataArray[start + j] = builtSubdataArray[end - j - 1];
                builtSubdataArray[end - j - 1] = tmp;
            }
        }
        return new RecordArray(recordSize, arraysPerRecord, size, data.getAll(), builtSubstart, builtSubdataArray);
    }

    public class Record {
        private int offset;
        private int arrayOffset;

        public Record(int offset, int arrayOffset) {
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
            return data.get(index + offset);
        }

        public void set(int index, int value) {
            if (index >= recordSize) {
                throw new IndexOutOfBoundsException("Index out of bounds: " + index + " of " + recordSize);
            }
            data.set(index + offset, value);
        }

        public int size() {
            return recordSize;
        }

        public int numArrays() {
            return arraysPerRecord;
        }

        public SubArray getArray(int index) {
            if (index > arraysPerRecord) {
                throw new IndexOutOfBoundsException("Index out of bounds: " + index + " of " + arraysPerRecord);
            }
            return new SubArray(arrayOffset + index);
        }
    }

    public class SubArray {
        private int offset;

        public SubArray(int offset) {
            this.offset = offset;
        }

        public int[] getData() {
            IntegerArray array = new IntegerArray(1);
            int ptr = substart.get(offset);
            while (ptr >= 0) {
                array.add(subdata.get(ptr));
                ptr = subnext.get(ptr);
            }
            int[] result = array.getAll();
            int half = result.length / 2;
            for (int i = 0; i < half; ++i) {
                int tmp = result[i];
                result[i] = result[result.length - i - 1];
                result[result.length - i - 1] = tmp;
            }
            return result;
        }

        public void clear() {
            substart.set(offset, -1);
        }

        public void add(int value) {
            int ptr = substart.get(offset);
            substart.set(offset, subdata.size());
            subdata.add(value);
            subnext.add(ptr);
        }
    }
}
