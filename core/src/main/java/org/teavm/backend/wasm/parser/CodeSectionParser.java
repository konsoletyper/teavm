/*
 *  Copyright 2022 Alexey Andreev.
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

import org.teavm.backend.wasm.model.WasmType;

public class CodeSectionParser {
    private CodeSectionListener listener;
    private byte[] data;
    private int ptr;
    private int lastReportedPtr = -1;

    public CodeSectionParser(CodeSectionListener listener) {
        this.listener = listener;
    }

    public void parse(byte[] data) {
        this.data = data;
        ptr = 0;
        try {
            parseFunctions();
        } finally {
            this.data = null;
        }
    }

    private void parseFunctions() {
        reportAddress();
        int count = readLEB();
        listener.sectionStart(count);
        for (var i = 0; i < count; ++i) {
            parseFunction(i);
        }
        reportAddress();
        listener.sectionEnd();
    }

    private void parseFunction(int index) {
        reportAddress();
        var functionSize = readLEB();
        var end = ptr + functionSize;
        if (listener.functionStart(index, functionSize)) {
            parseLocals();
        }
        ptr = end;
        reportAddress();
        listener.functionEnd();
    }

    private void parseLocals() {
        reportAddress();
        var localEntries = readLEB();
        listener.localsStart(localEntries);
        var localIndex = 0;
        for (int i = 0; i < localEntries; ++i) {
            reportAddress();
            var countInGroup = readLEB();
            var type = readType();
            listener.local(localIndex, countInGroup, type);
            localIndex += countInGroup;
        }
        reportAddress();
        listener.localsEnd();
    }

    private WasmType readType() {
        var typeId = data[ptr];
        switch (typeId) {
            case 0x7F:
                return WasmType.INT32;
            case 0x7E:
                return WasmType.INT64;
            case 0x7D:
                return WasmType.FLOAT32;
            case 0x7C:
                return WasmType.FLOAT64;
            default:
                return null;
        }
    }

    private void reportAddress() {
        if (ptr != lastReportedPtr) {
            lastReportedPtr = ptr;
            listener.address(ptr);
        }
    }

    private int readLEB() {
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
}
