/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.backend.wasm.binary;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class MetadataWriter {
    private byte[] data = new byte[256];
    private int offset;
    private Map<DataStructure, Integer> structureUsages = new HashMap<>();

    void write(DataType[] types) {
        writeLeb(types.length);
        for (DataType type : types) {
            write(type);
        }
    }

    private void write(DataType type) {
        if (type == DataPrimitives.BYTE) {
            writeByte(0);
        } else if (type == DataPrimitives.SHORT) {
            writeByte(1);
        } else if (type == DataPrimitives.INT) {
            writeByte(2);
        } else if (type == DataPrimitives.LONG) {
            writeByte(3);
        } else if (type == DataPrimitives.FLOAT) {
            writeByte(4);
        } else if (type == DataPrimitives.DOUBLE) {
            writeByte(5);
        } else if (type == DataPrimitives.ADDRESS) {
            writeByte(6);
        } else if (type instanceof DataArray) {
            DataArray array = (DataArray) type;
            writeByte(7);
            writeLeb(array.getSize());
            write(array.getComponentType());
        } else if (type instanceof DataStructure) {
            DataStructure structure = (DataStructure) type;
            Integer usage = structureUsages.get(structure);
            if (usage == null) {
                structureUsages.put(structure, offset);
                writeByte(8);
                writeByte(structure.getAlignment());
                DataType[] components = structure.getComponents();
                writeLeb(components.length);
                for (int i = 0; i < components.length; ++i) {
                    write(components[i]);
                }
            } else {
                int start = offset;
                structureUsages.put(structure, offset);
                writeByte(9);
                writeLeb(start - usage);
            }
        }
    }

    private void writeLeb(int value) {
        if (value >>> 7 == 0) {
            writeByte(value);
        } else if (value >>> 14 == 0) {
            byte first = (byte) ((value & 0x7F) | 0x80);
            byte second = (byte) (value >>> 7);
            writeBytes(new byte[] { first, second });
        } else if (value >>> 21 == 0) {
            byte first = (byte) ((value & 0x7F) | 0x80);
            byte second = (byte) (((value >>> 7) & 0x7F) | 0x80);
            byte third =  (byte) (value >>> 14);
            writeBytes(new byte[] { first, second, third });
        } else if (value >>> 28 == 0) {
            byte first = (byte) ((value & 0x7F) | 0x80);
            byte second = (byte) (((value >>> 7) & 0x7F) | 0x80);
            byte third = (byte) (((value >>> 14) & 0x7F) | 0x80);
            byte fourth =  (byte) (value >>> 21);
            writeBytes(new byte[] { first, second, third, fourth });
        } else {
            byte first = (byte) ((value & 0x7F) | 0x80);
            byte second = (byte) (((value >>> 7) & 0x7F) | 0x80);
            byte third = (byte) (((value >>> 14) & 0x7F) | 0x80);
            byte fourth = (byte) (((value >>> 21) & 0x7F) | 0x80);
            byte fifth =  (byte) (value >>> 28);
            writeBytes(new byte[] { first, second, third, fourth, fifth });
        }
    }

    private void writeByte(int value) {
        if (offset >= data.length) {
            data = Arrays.copyOf(data, data.length * 2);
        }
        data[offset++] = (byte) value;
    }

    private void writeBytes(byte[] values) {
        int count = values.length;
        if (offset + count > data.length) {
            data = Arrays.copyOf(data, data.length * 2);
        }
        System.arraycopy(values, 0, data, offset, count);
        offset += count;
    }

    public byte[] getData() {
        return Arrays.copyOf(data, offset);
    }
}
