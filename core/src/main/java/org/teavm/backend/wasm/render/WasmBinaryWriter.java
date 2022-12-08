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
package org.teavm.backend.wasm.render;

import java.util.Arrays;
import org.teavm.backend.wasm.model.WasmType;

public class WasmBinaryWriter {
    private byte[] data = new byte[1024];
    private int pointer;

    public void writeByte(int v) {
        alloc(1);
        data[pointer++] = (byte) v;
    }

    public void writeType(WasmType type, WasmBinaryVersion version) {
        if (type == null) {
            writeByte(0x40);
            return;
        }
        switch (type) {
            case INT32:
                writeByte(0x7F);
                break;
            case INT64:
                writeByte(0x7E);
                break;
            case FLOAT32:
                writeByte(0x7D);
                break;
            case FLOAT64:
                writeByte(0x7C);
                break;
        }
    }

    public int getPosition() {
        return pointer;
    }

    public void writeBytes(byte[] bytes) {
        alloc(bytes.length);
        System.arraycopy(bytes, 0, data, pointer, bytes.length);
        pointer += bytes.length;
    }

    public void writeAsciiString(String str) {
        writeLEB(str.length());
        byte[] bytes = new byte[str.length()];
        for (int i = 0; i < str.length(); ++i) {
            bytes[i] = (byte) str.charAt(i);
        }
        writeBytes(bytes);
    }

    public void writeInt32(int v) {
        alloc(4);
        for (int i = 0; i < 4; ++i) {
            data[pointer++] = (byte) (v & 0xFF);
            v >>>= 8;
        }
    }

    public void writeLEB(int v) {
        alloc(5);
        while (true) {
            int digit = v & 0x7F;
            int next = v >>> 7;
            boolean last = next == 0;
            if (!last) {
                digit |= 0xFFFFFF80;
            }
            data[pointer++] = (byte) digit;
            if (last) {
                break;
            }
            v = next;
        }
    }

    public void writeLEB4(int v) {
        alloc(4);
        for (int i = 0; i < 3; ++i) {
            int digit = v & 0x7F;
            data[pointer++] = (byte) (digit | 0x80);
            v >>>= 7;
        }
        data[pointer++] = (byte) (v & 0x7F);
    }

    public void writeSignedLEB(int v) {
        alloc(5);
        while (true) {
            int digit = v & 0x7F;
            v >>= 7;
            boolean last = (v == 0 && (digit & 0x40) == 0) || (v == -1 && (digit & 0x40) != 0);
            if (!last) {
                digit |= 0xFFFFFF80;
            }
            data[pointer++] = (byte) digit;
            if (last) {
                break;
            }
        }
    }

    public void writeLEB(long v) {
        alloc(10);
        while (true) {
            int digit = (int) (v & 0x7F);
            long next = v >>> 7;
            boolean last = next == 0;
            if (!last) {
                digit |= 0xFFFFFF80;
            }
            data[pointer++] = (byte) digit;
            if (last) {
                break;
            }
            v = next;
        }
    }

    public void writeSignedLEB(long v) {
        alloc(10);
        while (true) {
            int digit = (int) (v & 0x7F);
            v >>= 7;
            boolean last = (v == 0 && (digit & 0x40) == 0) || (v == -1 && (digit & 0x40) != 0);
            if (!last) {
                digit |= 0xFFFFFF80;
            }
            data[pointer++] = (byte) digit;
            if (last) {
                break;
            }
        }
    }

    public void writeFixed(int v) {
        alloc(4);
        for (int i = 0; i < 4; ++i) {
            data[pointer++] = (byte) v;
            v >>>= 8;
        }
    }

    public void writeFixed(long v) {
        alloc(8);
        for (int i = 0; i < 8; ++i) {
            data[pointer++] = (byte) v;
            v >>>= 8;
        }
    }

    private void alloc(int size) {
        if (data.length - pointer < size) {
            int newLength = data.length * 2;
            if (newLength < pointer + size) {
                newLength = (pointer + size) * 2;
            }
            data = Arrays.copyOf(data, newLength);
        }
    }

    public byte[] getData() {
        return Arrays.copyOf(data, pointer);
    }
}
