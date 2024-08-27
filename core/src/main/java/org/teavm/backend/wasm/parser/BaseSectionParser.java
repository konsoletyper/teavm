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

public abstract class BaseSectionParser {
    protected AddressListener addressListener;
    protected byte[] data;
    protected int ptr;
    private int lastReportedPtr = -1;

    public BaseSectionParser(AddressListener addressListener) {
        this.addressListener = addressListener;
    }

    public void parse(byte[] data) {
        this.data = data;
        ptr = 0;
        try {
            parseContent();
        } finally {
            this.data = null;
        }
    }

    protected abstract void parseContent();

    protected WasmHollowStorageType readStorageType() {
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

    protected WasmHollowType readType() {
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
                return readHeapType();
            case 0x40:
                return null;
            default:
                return readAbsHeapType(typeId);
        }
    }

    protected WasmHollowType.Reference readHeapType() {
        var typeId = data[ptr];
        if ((typeId & 0xC0) == 0x40) {
            var result = readAbsHeapType(typeId);
            ++ptr;
            return result;
        }
        return new WasmHollowType.CompositeReference(readLEB());
    }

    protected WasmHollowType.SpecialReference readAbsHeapType(int typeId) {
        switch (typeId) {
            case 0x70:
                return WasmHollowType.Reference.FUNC;
            case 0x6F:
                return WasmHollowType.Reference.EXTERN;
            case 0x6E:
                return WasmHollowType.Reference.ANY;
            case 0x6C:
                return WasmHollowType.Reference.I31;
            case 0x6B:
                return WasmHollowType.Reference.STRUCT;
            case 0x6A:
                return WasmHollowType.Reference.ARRAY;
            default:
                throw new ParseException("Unknown type", ptr);
        }
    }

    protected void reportAddress() {
        if (ptr != lastReportedPtr) {
            lastReportedPtr = ptr;
            if (addressListener != null) {
                addressListener.address(ptr);
            }
        }
    }

    protected int readSignedLEB() {
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

    protected int readLEB() {
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

    protected long readSignedLongLEB() {
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

    protected long readLongLEB() {
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

    protected int readFixedInt() {
        return ((data[ptr++] & 0xFF) << 24)
                | ((data[ptr++] & 0xFF) << 16)
                | ((data[ptr++] & 0xFF) << 8)
                | (data[ptr++] & 0xFF);
    }

    protected long readFixedLong() {
        return ((data[ptr++] & 0xFFL) << 56)
                | ((data[ptr++] & 0xFFL) << 48)
                | ((data[ptr++] & 0xFFL) << 40)
                | ((data[ptr++] & 0xFFL) << 32)
                | ((data[ptr++] & 0xFFL) << 24)
                | ((data[ptr++] & 0xFF) << 16)
                | ((data[ptr++] & 0xFF) << 8)
                | (data[ptr++] & 0xFF);
    }
}
