/*
 *  Copyright 2024 Alexey Andreev.
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
package org.teavm.backend.wasm.parser;

import java.nio.charset.StandardCharsets;
import org.teavm.backend.wasm.model.WasmType;

public class WasmBinaryReader {
    private final AddressListener addressListener;
    public final byte[] data;
    public int ptr;
    private int lastReportedPtr = -1;

    public WasmBinaryReader(AddressListener addressListener, byte[] data) {
        this.addressListener = addressListener;
        this.data = data;
    }

    public void reportAddress() {
        if (ptr != lastReportedPtr) {
            lastReportedPtr = ptr;
            if (addressListener != null) {
                addressListener.address(ptr);
            }
        }
    }

    public WasmHollowStorageType readStorageType() {
        var typeId = data[ptr];
        switch (typeId) {
            case 0x78:
                ++ptr;
                return WasmHollowStorageType.INT8;
            case 0x77:
                ++ptr;
                return WasmHollowStorageType.INT16;
            default:
                return new WasmHollowStorageType.Regular(readType());
        }
    }

    public WasmHollowBlockType readBlockType() {
        if (data[ptr] < 0x40) {
            return new WasmHollowBlockType.Function(readSignedLEB());
        } else {
            var result = readType();
            return result != null ? new WasmHollowBlockType.Value(result) : null;
        }
    }

    public WasmHollowType readType() {
        var typeId = data[ptr++];
        switch (typeId) {
            case 0x7F:
                return WasmHollowType.INT32;
            case 0x7E:
                return WasmHollowType.INT64;
            case 0x7D:
                return WasmHollowType.FLOAT32;
            case 0x7C:
                return WasmHollowType.FLOAT64;
            case 0x63:
                return readHeapType(true);
            case 0x64:
                return readHeapType(false);
            case 0x40:
                return null;
            default:
                return readAbsHeapType(typeId, true);
        }
    }

    public WasmHollowType.Reference readHeapType(boolean nullable) {
        var typeId = data[ptr];
        if ((typeId & 0xC0) == 0x40) {
            var result = readAbsHeapType(typeId, nullable);
            ++ptr;
            return result;
        }
        return new WasmHollowType.CompositeReference(readLEB(), nullable);
    }

    public WasmHollowType.SpecialReference readAbsHeapType(int typeId, boolean nullable) {
        switch (typeId) {
            case 0x70:
                return special(WasmType.SpecialReferenceKind.FUNC, nullable);
            case 0x69:
                return special(WasmType.SpecialReferenceKind.EXN, nullable);
            case 0x6F:
                return special(WasmType.SpecialReferenceKind.EXTERN, nullable);
            case 0x6E:
                return special(WasmType.SpecialReferenceKind.ANY, nullable);
            case 0x6D:
                return special(WasmType.SpecialReferenceKind.EQ, nullable);
            case 0x6C:
                return special(WasmType.SpecialReferenceKind.I31, nullable);
            case 0x6B:
                return special(WasmType.SpecialReferenceKind.STRUCT, nullable);
            case 0x6A:
                return special(WasmType.SpecialReferenceKind.ARRAY, nullable);
            default:
                throw new ParseException("Unknown type", ptr);
        }
    }

    private static WasmHollowType.SpecialReference special(WasmType.SpecialReferenceKind kind, boolean nullable) {
        return nullable
                ? WasmHollowType.Reference.special(kind)
                : WasmHollowType.SpecialReference.nonNullSpecial(kind);
    }

    public int readSignedLEB() {
        var result = 0;
        var shift = 0;
        while (true) {
            var digit = data[ptr++];
            result |= (digit & 0x7F) << shift;
            if ((digit & 0x80) == 0) {
                if ((digit & 0x40) != 0) {
                    result |= -1 << (shift + 7);
                }
                break;
            }
            shift += 7;
        }
        return result;
    }

    public int readLEB() {
        var result = 0;
        var shift = 0;
        while (true) {
            var digit = data[ptr++];
            result |= (digit & 0x7F) << shift;
            if ((digit & 0x80) == 0) {
                break;
            }
            shift += 7;
        }
        return result;
    }

    public long readSignedLongLEB() {
        var result = 0L;
        var shift = 0;
        while (true) {
            var digit = data[ptr++];
            result |= (digit & 0x7FL) << shift;
            if ((digit & 0x80) == 0) {
                if ((digit & 0x40) != 0) {
                    result |= -1L << (shift + 7);
                }
                break;
            }
            shift += 7;
        }
        return result;
    }

    public long readLongLEB() {
        var result = 0L;
        var shift = 0;
        while (true) {
            var digit = data[ptr++];
            result |= (digit & 0x7FL) << shift;
            if ((digit & 0x80) == 0) {
                break;
            }
            shift += 7;
        }
        return result;
    }

    public int readInt32() {
        return (data[ptr++] & 0xFF)
                | ((data[ptr++] & 0xFF) << 8)
                | ((data[ptr++] & 0xFF) << 16)
                | ((data[ptr++] & 0xFF) << 24);
    }

    public int readFixedInt() {
        return ((data[ptr++] & 0xFF) << 24)
                | ((data[ptr++] & 0xFF) << 16)
                | ((data[ptr++] & 0xFF) << 8)
                | (data[ptr++] & 0xFF);
    }

    public long readFixedLong() {
        return ((data[ptr++] & 0xFFL) << 56)
                | ((data[ptr++] & 0xFFL) << 48)
                | ((data[ptr++] & 0xFFL) << 40)
                | ((data[ptr++] & 0xFFL) << 32)
                | ((data[ptr++] & 0xFFL) << 24)
                | ((data[ptr++] & 0xFF) << 16)
                | ((data[ptr++] & 0xFF) << 8)
                | (data[ptr++] & 0xFF);
    }

    public String readString() {
        var size = readLEB();
        var result = new String(data, ptr, size, StandardCharsets.UTF_8);
        ptr += size;
        return result;
    }
}
