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

import java.util.ArrayList;
import java.util.List;
import org.teavm.backend.wasm.model.WasmType;

public class CodeSectionParser {
    private AddressListener addressListener;
    private CodeSectionListener listener;
    private byte[] data;
    private int ptr;
    private CodeListener codeListener;
    private int lastReportedPtr = -1;
    private List<Block> blockStack = new ArrayList<>();

    public CodeSectionParser(AddressListener addressListener, CodeSectionListener listener) {
        this.addressListener = addressListener;
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
        codeListener = listener.code();
        if (codeListener != null) {
            parseCode();
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
    }

    private void parseCode() {
        if (!parseExpressions()) {
            codeListener.error(blockStack.size());
            blockStack.clear();
        }
    }

    private boolean parseExpressions() {
        while (data[ptr] != 0x0B) {
            if (!parseExpr()) {
                return false;
            }
        }
        return true;
    }

    private boolean parseExpr() {
        reportAddress();
        switch (data[ptr++]) {
            case 0x00:
                codeListener.opcode(Opcode.UNREACHABLE);
                break;
            case 0x02:
                return parseBlock(false);
            case 0x03:
                return parseBlock(true);
            case 0x04:
                return parseConditional();
            case 0x0C:
                parseBranch(BranchOpcode.BR);
                break;
            case 0x0D:
                parseBranch(BranchOpcode.BR_IF);
                break;
            case 0x0E:
                parseTableBranch();
                break;
            case 0x0F:
                codeListener.opcode(Opcode.UNREACHABLE);
                break;
            case 0x20:
                codeListener.local(LocalOpcode.GET, readLEB());
                break;
            case 0x21:
                codeListener.local(LocalOpcode.SET, readLEB());
                break;
            case 0x41:
                codeListener.int32Constant(readSignedLEB());
                break;
            case 0x42:
                codeListener.int64Constant(readSignedLongLEB());
                break;
            case 0x43:
                codeListener.float32Constant(Float.intBitsToFloat(readFixedInt()));
                break;
            case 0x44:
                codeListener.float64Constant(Double.longBitsToDouble(readFixedLong()));
                break;
            default:
                return false;
        }
        return true;
    }

    private boolean parseBlock(boolean isLoop) {
        var type = readType();
        var token = codeListener.startBlock(isLoop, type);
        blockStack.add(new Block(token));
        if (!parseExpressions()) {
            return false;
        }
        blockStack.remove(blockStack.size() - 1);
        reportAddress();
        codeListener.endBlock(token);
        ++ptr;
        return true;
    }

    private boolean parseConditional() {
        var type = readType();
        var token = codeListener.startConditionalBlock(type);
        blockStack.add(new Block(token));
        var hasElse = false;
        loop: while (true) {
            switch (data[ptr]) {
                case 0x0B:
                    break loop;
                case 0x05:
                    if (hasElse) {
                        return false;
                    }
                    reportAddress();
                    codeListener.startElseSection(blockStack.get(blockStack.size() - 1).token);
                    ++ptr;
                    break;
                default:
                    if (!parseExpr()) {
                        return false;
                    }
                    break;
            }
        }
        blockStack.remove(blockStack.size() - 1);
        reportAddress();
        codeListener.endBlock(token);
        ++ptr;
        return true;
    }

    private void parseBranch(BranchOpcode opcode) {
        var depth = readLEB();
        var target = blockStack.get(blockStack.size() - depth);
        codeListener.branch(opcode, depth, target.token);
    }

    private void parseTableBranch() {
        var count = readLEB();
        var depths = new int[count];
        var targets = new int[count];
        for (var i = 0; i < count; ++i) {
            var depth = readLEB();
            depths[i] = depth;
            targets[i] = blockStack.get(blockStack.size() - depth).token;
        }
        var defaultDepth = readLEB();
        var defaultTarget = blockStack.get(blockStack.size() - defaultDepth).token;
        codeListener.tableBranch(depths, targets, defaultDepth, defaultTarget);
    }

    private WasmType readType() {
        var typeId = data[ptr++];
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
            if (addressListener != null) {
                addressListener.address(ptr);
            }
        }
    }

    private int readSignedLEB() {
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

    private long readSignedLongLEB() {
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

    private long readLongLEB() {
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

    private int readFixedInt() {
        return ((data[ptr] & 0xFF) << 24)
                | ((data[ptr] & 0xFF) << 16)
                | ((data[ptr] & 0xFF) << 8)
                | (data[ptr] & 0xFF);
    }

    private long readFixedLong() {
        return ((data[ptr] & 0xFFL) << 56)
                | ((data[ptr] & 0xFFL) << 48)
                | ((data[ptr] & 0xFFL) << 40)
                | ((data[ptr] & 0xFFL) << 32)
                | ((data[ptr] & 0xFFL) << 24)
                | ((data[ptr] & 0xFF) << 16)
                | ((data[ptr] & 0xFF) << 8)
                | (data[ptr] & 0xFF);
    }

    private static class Block {
        int token;

        Block(int token) {
            this.token = token;
        }
    }
}
