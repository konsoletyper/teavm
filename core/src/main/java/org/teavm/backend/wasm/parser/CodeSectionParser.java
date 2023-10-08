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
import org.teavm.backend.wasm.model.expression.WasmFloatBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmFloatType;
import org.teavm.backend.wasm.model.expression.WasmFloatUnaryOperation;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmInt64Subtype;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnaryOperation;

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
            codeListener = listener.code();
            if (codeListener != null) {
                parseCode();
            }
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
        switch (data[ptr++] & 0xFF) {
            case 0x00:
                codeListener.opcode(Opcode.UNREACHABLE);
                break;
            case 0x01:
                codeListener.opcode(Opcode.NOP);
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
                codeListener.opcode(Opcode.RETURN);
                break;
            case 0x10:
                codeListener.call(readLEB());
                break;
            case 0x11:
                codeListener.indirectCall(readLEB(), readLEB());
                break;

            case 0x1A:
                codeListener.opcode(Opcode.DROP);
                break;

            case 0x20:
                codeListener.local(LocalOpcode.GET, readLEB());
                break;
            case 0x21:
                codeListener.local(LocalOpcode.SET, readLEB());
                break;

            case 0x28:
                codeListener.loadInt32(WasmInt32Subtype.INT32, 1 << readLEB(), readLEB());
                break;
            case 0x29:
                codeListener.loadInt64(WasmInt64Subtype.INT64, 1 << readLEB(), readLEB());
                break;
            case 0x2A:
                codeListener.loadFloat32(1 << readLEB(), readLEB());
                break;
            case 0x2B:
                codeListener.loadFloat64(1 << readLEB(), readLEB());
                break;
            case 0x2C:
                codeListener.loadInt32(WasmInt32Subtype.INT8, 1 << readLEB(), readLEB());
                break;
            case 0x2D:
                codeListener.loadInt32(WasmInt32Subtype.UINT8, 1 << readLEB(), readLEB());
                break;
            case 0x2E:
                codeListener.loadInt32(WasmInt32Subtype.INT16, 1 << readLEB(), readLEB());
                break;
            case 0x2F:
                codeListener.loadInt32(WasmInt32Subtype.UINT16, 1 << readLEB(), readLEB());
                break;
            case 0x30:
                codeListener.loadInt64(WasmInt64Subtype.INT8, 1 << readLEB(), readLEB());
                break;
            case 0x31:
                codeListener.loadInt64(WasmInt64Subtype.UINT8, 1 << readLEB(), readLEB());
                break;
            case 0x32:
                codeListener.loadInt64(WasmInt64Subtype.INT16, 1 << readLEB(), readLEB());
                break;
            case 0x33:
                codeListener.loadInt64(WasmInt64Subtype.UINT16, 1 << readLEB(), readLEB());
                break;
            case 0x34:
                codeListener.loadInt64(WasmInt64Subtype.INT32, 1 << readLEB(), readLEB());
                break;
            case 0x35:
                codeListener.loadInt64(WasmInt64Subtype.UINT32, 1 << readLEB(), readLEB());
                break;
            case 0x36:
                codeListener.storeInt32(WasmInt32Subtype.INT32, 1 << readLEB(), readLEB());
                break;
            case 0x37:
                codeListener.storeInt64(WasmInt64Subtype.INT64, 1 << readLEB(), readLEB());
                break;
            case 0x38:
                codeListener.storeFloat32(1 << readLEB(), readLEB());
                break;
            case 0x39:
                codeListener.storeFloat64(1 << readLEB(), readLEB());
                break;
            case 0x3A:
                codeListener.storeInt32(WasmInt32Subtype.INT8, 1 << readLEB(), readLEB());
                break;
            case 0x3B:
                codeListener.storeInt32(WasmInt32Subtype.INT16, 1 << readLEB(), readLEB());
                break;
            case 0x3C:
                codeListener.storeInt64(WasmInt64Subtype.INT8, 1 << readLEB(), readLEB());
                break;
            case 0x3D:
                codeListener.storeInt64(WasmInt64Subtype.INT16, 1 << readLEB(), readLEB());
                break;
            case 0x3E:
                codeListener.storeInt64(WasmInt64Subtype.INT32, 1 << readLEB(), readLEB());
                break;
            case 0x40:
                readLEB();
                codeListener.memoryGrow();
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

            case 0x45:
                codeListener.unary(WasmIntUnaryOperation.EQZ, WasmIntType.INT32);
                break;
            case 0x46:
                codeListener.binary(WasmIntBinaryOperation.EQ, WasmIntType.INT32);
                break;
            case 0x47:
                codeListener.binary(WasmIntBinaryOperation.NE, WasmIntType.INT32);
                break;
            case 0x48:
                codeListener.binary(WasmIntBinaryOperation.LT_SIGNED, WasmIntType.INT32);
                break;
            case 0x49:
                codeListener.binary(WasmIntBinaryOperation.LT_UNSIGNED, WasmIntType.INT32);
                break;
            case 0x4A:
                codeListener.binary(WasmIntBinaryOperation.GT_SIGNED, WasmIntType.INT32);
                break;
            case 0x4B:
                codeListener.binary(WasmIntBinaryOperation.GT_UNSIGNED, WasmIntType.INT32);
                break;
            case 0x4C:
                codeListener.binary(WasmIntBinaryOperation.LE_SIGNED, WasmIntType.INT32);
                break;
            case 0x4D:
                codeListener.binary(WasmIntBinaryOperation.LE_UNSIGNED, WasmIntType.INT32);
                break;
            case 0x4E:
                codeListener.binary(WasmIntBinaryOperation.GE_SIGNED, WasmIntType.INT32);
                break;
            case 0x4F:
                codeListener.binary(WasmIntBinaryOperation.GE_UNSIGNED, WasmIntType.INT32);
                break;

            case 0x50:
                codeListener.unary(WasmIntUnaryOperation.EQZ, WasmIntType.INT64);
                break;
            case 0x51:
                codeListener.binary(WasmIntBinaryOperation.EQ, WasmIntType.INT64);
                break;
            case 0x52:
                codeListener.binary(WasmIntBinaryOperation.NE, WasmIntType.INT64);
                break;
            case 0x53:
                codeListener.binary(WasmIntBinaryOperation.LT_SIGNED, WasmIntType.INT64);
                break;
            case 0x54:
                codeListener.binary(WasmIntBinaryOperation.LT_UNSIGNED, WasmIntType.INT64);
                break;
            case 0x55:
                codeListener.binary(WasmIntBinaryOperation.GT_SIGNED, WasmIntType.INT64);
                break;
            case 0x56:
                codeListener.binary(WasmIntBinaryOperation.GT_UNSIGNED, WasmIntType.INT64);
                break;
            case 0x57:
                codeListener.binary(WasmIntBinaryOperation.LE_SIGNED, WasmIntType.INT64);
                break;
            case 0x58:
                codeListener.binary(WasmIntBinaryOperation.LE_UNSIGNED, WasmIntType.INT64);
                break;
            case 0x59:
                codeListener.binary(WasmIntBinaryOperation.GE_SIGNED, WasmIntType.INT64);
                break;
            case 0x5A:
                codeListener.binary(WasmIntBinaryOperation.GE_UNSIGNED, WasmIntType.INT64);
                break;

            case 0x5B:
                codeListener.binary(WasmFloatBinaryOperation.EQ, WasmFloatType.FLOAT32);
                break;
            case 0x5C:
                codeListener.binary(WasmFloatBinaryOperation.NE, WasmFloatType.FLOAT32);
                break;
            case 0x5D:
                codeListener.binary(WasmFloatBinaryOperation.LT, WasmFloatType.FLOAT32);
                break;
            case 0x5E:
                codeListener.binary(WasmFloatBinaryOperation.GT, WasmFloatType.FLOAT32);
                break;
            case 0x5F:
                codeListener.binary(WasmFloatBinaryOperation.LE, WasmFloatType.FLOAT32);
                break;
            case 0x60:
                codeListener.binary(WasmFloatBinaryOperation.GE, WasmFloatType.FLOAT32);
                break;

            case 0x61:
                codeListener.binary(WasmFloatBinaryOperation.EQ, WasmFloatType.FLOAT64);
                break;
            case 0x62:
                codeListener.binary(WasmFloatBinaryOperation.NE, WasmFloatType.FLOAT64);
                break;
            case 0x63:
                codeListener.binary(WasmFloatBinaryOperation.LT, WasmFloatType.FLOAT64);
                break;
            case 0x64:
                codeListener.binary(WasmFloatBinaryOperation.GT, WasmFloatType.FLOAT64);
                break;
            case 0x65:
                codeListener.binary(WasmFloatBinaryOperation.LE, WasmFloatType.FLOAT64);
                break;
            case 0x66:
                codeListener.binary(WasmFloatBinaryOperation.GE, WasmFloatType.FLOAT64);
                break;

            case 0x67:
                codeListener.unary(WasmIntUnaryOperation.CLZ, WasmIntType.INT32);
                break;
            case 0x68:
                codeListener.unary(WasmIntUnaryOperation.CTZ, WasmIntType.INT32);
                break;
            case 0x69:
                codeListener.unary(WasmIntUnaryOperation.POPCNT, WasmIntType.INT32);
                break;
            case 0x6A:
                codeListener.binary(WasmIntBinaryOperation.ADD, WasmIntType.INT32);
                break;
            case 0x6B:
                codeListener.binary(WasmIntBinaryOperation.SUB, WasmIntType.INT32);
                break;
            case 0x6C:
                codeListener.binary(WasmIntBinaryOperation.MUL, WasmIntType.INT32);
                break;
            case 0x6D:
                codeListener.binary(WasmIntBinaryOperation.DIV_SIGNED, WasmIntType.INT32);
                break;
            case 0x6E:
                codeListener.binary(WasmIntBinaryOperation.DIV_UNSIGNED, WasmIntType.INT32);
                break;
            case 0x6F:
                codeListener.binary(WasmIntBinaryOperation.REM_SIGNED, WasmIntType.INT32);
                break;
            case 0x70:
                codeListener.binary(WasmIntBinaryOperation.REM_UNSIGNED, WasmIntType.INT32);
                break;
            case 0x71:
                codeListener.binary(WasmIntBinaryOperation.AND, WasmIntType.INT32);
                break;
            case 0x72:
                codeListener.binary(WasmIntBinaryOperation.OR, WasmIntType.INT32);
                break;
            case 0x73:
                codeListener.binary(WasmIntBinaryOperation.XOR, WasmIntType.INT32);
                break;
            case 0x74:
                codeListener.binary(WasmIntBinaryOperation.SHL, WasmIntType.INT32);
                break;
            case 0x75:
                codeListener.binary(WasmIntBinaryOperation.SHR_SIGNED, WasmIntType.INT32);
                break;
            case 0x76:
                codeListener.binary(WasmIntBinaryOperation.SHR_UNSIGNED, WasmIntType.INT32);
                break;
            case 0x77:
                codeListener.binary(WasmIntBinaryOperation.ROTL, WasmIntType.INT32);
                break;
            case 0x78:
                codeListener.binary(WasmIntBinaryOperation.ROTR, WasmIntType.INT32);
                break;

            case 0x79:
                codeListener.unary(WasmIntUnaryOperation.CLZ, WasmIntType.INT64);
                break;
            case 0x7A:
                codeListener.unary(WasmIntUnaryOperation.CTZ, WasmIntType.INT64);
                break;
            case 0x7B:
                codeListener.unary(WasmIntUnaryOperation.POPCNT, WasmIntType.INT64);
                break;
            case 0x7C:
                codeListener.binary(WasmIntBinaryOperation.ADD, WasmIntType.INT64);
                break;
            case 0x7D:
                codeListener.binary(WasmIntBinaryOperation.SUB, WasmIntType.INT64);
                break;
            case 0x7E:
                codeListener.binary(WasmIntBinaryOperation.MUL, WasmIntType.INT64);
                break;
            case 0x7F:
                codeListener.binary(WasmIntBinaryOperation.DIV_SIGNED, WasmIntType.INT64);
                break;
            case 0x80:
                codeListener.binary(WasmIntBinaryOperation.DIV_UNSIGNED, WasmIntType.INT64);
                break;
            case 0x81:
                codeListener.binary(WasmIntBinaryOperation.REM_SIGNED, WasmIntType.INT64);
                break;
            case 0x82:
                codeListener.binary(WasmIntBinaryOperation.REM_UNSIGNED, WasmIntType.INT64);
                break;
            case 0x83:
                codeListener.binary(WasmIntBinaryOperation.AND, WasmIntType.INT64);
                break;
            case 0x84:
                codeListener.binary(WasmIntBinaryOperation.OR, WasmIntType.INT64);
                break;
            case 0x85:
                codeListener.binary(WasmIntBinaryOperation.XOR, WasmIntType.INT64);
                break;
            case 0x86:
                codeListener.binary(WasmIntBinaryOperation.SHL, WasmIntType.INT64);
                break;
            case 0x87:
                codeListener.binary(WasmIntBinaryOperation.SHR_UNSIGNED, WasmIntType.INT64);
                break;
            case 0x88:
                codeListener.binary(WasmIntBinaryOperation.SHR_UNSIGNED, WasmIntType.INT64);
                break;
            case 0x89:
                codeListener.binary(WasmIntBinaryOperation.ROTL, WasmIntType.INT64);
                break;
            case 0x8A:
                codeListener.binary(WasmIntBinaryOperation.ROTR, WasmIntType.INT64);
                break;

            case 0x8B:
                codeListener.unary(WasmFloatUnaryOperation.ABS, WasmFloatType.FLOAT32);
                break;
            case 0x8C:
                codeListener.unary(WasmFloatUnaryOperation.NEG, WasmFloatType.FLOAT32);
                break;
            case 0x8D:
                codeListener.unary(WasmFloatUnaryOperation.CEIL, WasmFloatType.FLOAT32);
                break;
            case 0x8E:
                codeListener.unary(WasmFloatUnaryOperation.FLOOR, WasmFloatType.FLOAT32);
                break;
            case 0x8F:
                codeListener.unary(WasmFloatUnaryOperation.TRUNC, WasmFloatType.FLOAT32);
                break;
            case 0x90:
                codeListener.unary(WasmFloatUnaryOperation.NEAREST, WasmFloatType.FLOAT32);
                break;
            case 0x91:
                codeListener.unary(WasmFloatUnaryOperation.SQRT, WasmFloatType.FLOAT32);
                break;
            case 0x92:
                codeListener.binary(WasmFloatBinaryOperation.ADD, WasmFloatType.FLOAT32);
                break;
            case 0x93:
                codeListener.binary(WasmFloatBinaryOperation.SUB, WasmFloatType.FLOAT32);
                break;
            case 0x94:
                codeListener.binary(WasmFloatBinaryOperation.MUL, WasmFloatType.FLOAT32);
                break;
            case 0x95:
                codeListener.binary(WasmFloatBinaryOperation.DIV, WasmFloatType.FLOAT32);
                break;
            case 0x96:
                codeListener.binary(WasmFloatBinaryOperation.MIN, WasmFloatType.FLOAT32);
                break;
            case 0x97:
                codeListener.binary(WasmFloatBinaryOperation.MAX, WasmFloatType.FLOAT32);
                break;
            case 0x98:
                codeListener.unary(WasmFloatUnaryOperation.COPYSIGN, WasmFloatType.FLOAT32);
                break;

            case 0x99:
                codeListener.unary(WasmFloatUnaryOperation.ABS, WasmFloatType.FLOAT64);
                break;
            case 0x9A:
                codeListener.unary(WasmFloatUnaryOperation.NEG, WasmFloatType.FLOAT64);
                break;
            case 0x9B:
                codeListener.unary(WasmFloatUnaryOperation.CEIL, WasmFloatType.FLOAT64);
                break;
            case 0x9C:
                codeListener.unary(WasmFloatUnaryOperation.FLOOR, WasmFloatType.FLOAT64);
                break;
            case 0x9D:
                codeListener.unary(WasmFloatUnaryOperation.TRUNC, WasmFloatType.FLOAT64);
                break;
            case 0x9E:
                codeListener.unary(WasmFloatUnaryOperation.NEAREST, WasmFloatType.FLOAT64);
                break;
            case 0x9F:
                codeListener.unary(WasmFloatUnaryOperation.SQRT, WasmFloatType.FLOAT64);
                break;
            case 0xA0:
                codeListener.binary(WasmFloatBinaryOperation.ADD, WasmFloatType.FLOAT64);
                break;
            case 0xA1:
                codeListener.binary(WasmFloatBinaryOperation.SUB, WasmFloatType.FLOAT64);
                break;
            case 0xA2:
                codeListener.binary(WasmFloatBinaryOperation.MUL, WasmFloatType.FLOAT64);
                break;
            case 0xA3:
                codeListener.binary(WasmFloatBinaryOperation.DIV, WasmFloatType.FLOAT64);
                break;
            case 0xA4:
                codeListener.binary(WasmFloatBinaryOperation.MIN, WasmFloatType.FLOAT64);
                break;
            case 0xA5:
                codeListener.binary(WasmFloatBinaryOperation.MAX, WasmFloatType.FLOAT64);
                break;
            case 0xA6:
                codeListener.unary(WasmFloatUnaryOperation.COPYSIGN, WasmFloatType.FLOAT64);
                break;

            case 0xA7:
                codeListener.convert(WasmType.INT64, WasmType.INT32, false, false);
                break;
            case 0xA8:
                codeListener.convert(WasmType.FLOAT32, WasmType.INT32, false, false);
                break;
            case 0xA9:
                codeListener.convert(WasmType.FLOAT32, WasmType.INT32, true, false);
                break;
            case 0xAA:
                codeListener.convert(WasmType.FLOAT64, WasmType.INT32, false, false);
                break;
            case 0xAB:
                codeListener.convert(WasmType.FLOAT64, WasmType.INT32, true, false);
                break;
            case 0xAC:
                codeListener.convert(WasmType.INT32, WasmType.INT64, false, false);
                break;
            case 0xAD:
                codeListener.convert(WasmType.INT32, WasmType.INT64, true, false);
                break;
            case 0xAE:
                codeListener.convert(WasmType.FLOAT32, WasmType.INT64, false, false);
                break;
            case 0xAF:
                codeListener.convert(WasmType.FLOAT32, WasmType.INT64, true, false);
                break;
            case 0xB0:
                codeListener.convert(WasmType.FLOAT64, WasmType.INT64, false, false);
                break;
            case 0xB1:
                codeListener.convert(WasmType.FLOAT64, WasmType.INT64, true, false);
                break;
            case 0xB2:
                codeListener.convert(WasmType.INT32, WasmType.FLOAT32, false, false);
                break;
            case 0xB3:
                codeListener.convert(WasmType.INT32, WasmType.FLOAT32, true, false);
                break;
            case 0xB4:
                codeListener.convert(WasmType.INT64, WasmType.FLOAT32, false, false);
                break;
            case 0xB5:
                codeListener.convert(WasmType.INT64, WasmType.FLOAT32, true, false);
                break;
            case 0xB6:
                codeListener.convert(WasmType.FLOAT64, WasmType.FLOAT32, true, false);
                break;
            case 0xB7:
                codeListener.convert(WasmType.INT32, WasmType.FLOAT64, false, false);
                break;
            case 0xB8:
                codeListener.convert(WasmType.INT32, WasmType.FLOAT64, true, false);
                break;
            case 0xB9:
                codeListener.convert(WasmType.INT64, WasmType.FLOAT64, false, false);
                break;
            case 0xBA:
                codeListener.convert(WasmType.INT64, WasmType.FLOAT64, true, false);
                break;
            case 0xBC:
                codeListener.convert(WasmType.FLOAT32, WasmType.INT32, false, true);
                break;
            case 0xBD:
                codeListener.convert(WasmType.FLOAT64, WasmType.INT64, false, true);
                break;
            case 0xBE:
                codeListener.convert(WasmType.INT32, WasmType.FLOAT32, false, true);
                break;
            case 0xBF:
                codeListener.convert(WasmType.INT64, WasmType.FLOAT64, false, true);
                break;

            case 0xFC:
                return parseExtExpr();

            default:
                return false;
        }
        return true;
    }

    private boolean parseExtExpr() {
        switch (readLEB()) {
            case 10: {
                if (data[ptr++] != 0 || data[ptr++] != 0) {
                    return false;
                }
                codeListener.memoryCopy();
                return true;
            }
            case 11: {
                if (data[ptr++] != 0) {
                    return false;
                }
                codeListener.memoryFill();
                return true;
            }

            default:
                return false;
        }
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
        codeListener.endBlock(token, isLoop);
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
        codeListener.endBlock(token, false);
        ++ptr;
        return true;
    }

    private void parseBranch(BranchOpcode opcode) {
        var depth = readLEB();
        var target = blockStack.get(blockStack.size() - depth - 1);
        codeListener.branch(opcode, depth, target.token);
    }

    private void parseTableBranch() {
        var count = readLEB();
        var depths = new int[count];
        var targets = new int[count];
        for (var i = 0; i < count; ++i) {
            var depth = readLEB();
            depths[i] = depth;
            targets[i] = blockStack.get(blockStack.size() - depth - 1).token;
        }
        var defaultDepth = readLEB();
        var defaultTarget = blockStack.get(blockStack.size() - defaultDepth - 1).token;
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
        return ((data[ptr++] & 0xFF) << 24)
                | ((data[ptr++] & 0xFF) << 16)
                | ((data[ptr++] & 0xFF) << 8)
                | (data[ptr++] & 0xFF);
    }

    private long readFixedLong() {
        return ((data[ptr++] & 0xFFL) << 56)
                | ((data[ptr++] & 0xFFL) << 48)
                | ((data[ptr++] & 0xFFL) << 40)
                | ((data[ptr++] & 0xFFL) << 32)
                | ((data[ptr++] & 0xFFL) << 24)
                | ((data[ptr++] & 0xFF) << 16)
                | ((data[ptr++] & 0xFF) << 8)
                | (data[ptr++] & 0xFF);
    }

    private static class Block {
        int token;

        Block(int token) {
            this.token = token;
        }
    }
}
