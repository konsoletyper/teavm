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

import java.util.HashMap;
import java.util.Map;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmBlock;
import org.teavm.backend.wasm.model.expression.WasmBranch;
import org.teavm.backend.wasm.model.expression.WasmBreak;
import org.teavm.backend.wasm.model.expression.WasmCall;
import org.teavm.backend.wasm.model.expression.WasmConditional;
import org.teavm.backend.wasm.model.expression.WasmConversion;
import org.teavm.backend.wasm.model.expression.WasmDrop;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmExpressionVisitor;
import org.teavm.backend.wasm.model.expression.WasmFloat32Constant;
import org.teavm.backend.wasm.model.expression.WasmFloat64Constant;
import org.teavm.backend.wasm.model.expression.WasmFloatBinary;
import org.teavm.backend.wasm.model.expression.WasmFloatUnary;
import org.teavm.backend.wasm.model.expression.WasmGetLocal;
import org.teavm.backend.wasm.model.expression.WasmIndirectCall;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.backend.wasm.model.expression.WasmInt64Constant;
import org.teavm.backend.wasm.model.expression.WasmIntBinary;
import org.teavm.backend.wasm.model.expression.WasmIntUnary;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat32;
import org.teavm.backend.wasm.model.expression.WasmLoadFloat64;
import org.teavm.backend.wasm.model.expression.WasmLoadInt32;
import org.teavm.backend.wasm.model.expression.WasmLoadInt64;
import org.teavm.backend.wasm.model.expression.WasmReturn;
import org.teavm.backend.wasm.model.expression.WasmSetLocal;
import org.teavm.backend.wasm.model.expression.WasmStoreFloat32;
import org.teavm.backend.wasm.model.expression.WasmStoreFloat64;
import org.teavm.backend.wasm.model.expression.WasmStoreInt32;
import org.teavm.backend.wasm.model.expression.WasmStoreInt64;
import org.teavm.backend.wasm.model.expression.WasmSwitch;
import org.teavm.backend.wasm.model.expression.WasmUnreachable;

class WasmBinaryRenderingVisitor implements WasmExpressionVisitor {
    private WasmBinaryWriter writer;
    private WasmBinaryVersion version;
    private Map<String, Integer> functionIndexes;
    private Map<String, Integer> importedIndexes;
    private Map<WasmSignature, Integer> signatureIndexes;
    private int depth;
    private Map<WasmBlock, Integer> blockDepths = new HashMap<>();

    WasmBinaryRenderingVisitor(WasmBinaryWriter writer, WasmBinaryVersion version, Map<String, Integer> functionIndexes,
            Map<String, Integer> importedIndexes, Map<WasmSignature, Integer> signatureIndexes) {
        this.writer = writer;
        this.version = version;
        this.functionIndexes = functionIndexes;
        this.importedIndexes = importedIndexes;
        this.signatureIndexes = signatureIndexes;
    }

    @Override
    public void visit(WasmBlock expression) {
        int blockDepth = expression.isLoop() && version == WasmBinaryVersion.V_0xB ? 2 : 1;
        depth += blockDepth;
        blockDepths.put(expression, depth);
        if (version == WasmBinaryVersion.V_0xD) {
            writer.writeByte(expression.isLoop() ? 0x03 : 0x02);
        } else {
            writer.writeByte(expression.isLoop() ? 0x02 : 0x01);
        }
        writeBlockType(expression.getType());
        for (WasmExpression part : expression.getBody()) {
            part.acceptVisitor(this);
        }
        writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x0B : 0x0F);
        blockDepths.remove(expression);
        depth -= blockDepth;
    }

    private void writeBlockType(WasmType type) {
        if (version != WasmBinaryVersion.V_0xB) {
            writer.writeType(type, version);
        }
    }

    @Override
    public void visit(WasmBranch expression) {
        if (expression.getResult() != null) {
            expression.getResult().acceptVisitor(this);
        }
        expression.getCondition().acceptVisitor(this);

        writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x0D : 0x07);

        if (version == WasmBinaryVersion.V_0xB) {
            writer.writeByte(expression.getResult() != null ? 1 : 0);
        }
        writeLabel(expression.getTarget());
    }

    @Override
    public void visit(WasmBreak expression) {
        if (expression.getResult() != null) {
            expression.getResult().acceptVisitor(this);
        }

        writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x0C : 0x06);

        if (version == WasmBinaryVersion.V_0xB) {
            writer.writeByte(expression.getResult() != null ? 1 : 0);
        }
        writeLabel(expression.getTarget());
    }

    @Override
    public void visit(WasmSwitch expression) {
        expression.getSelector().acceptVisitor(this);

        writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x0E : 0x08);

        if (version == WasmBinaryVersion.V_0xB) {
            writer.writeByte(0);
        }

        writer.writeLEB(expression.getTargets().size());
        for (WasmBlock target : expression.getTargets()) {
            int targetDepth = blockDepths.get(target);
            int relativeDepth = depth - targetDepth;
            if (version != WasmBinaryVersion.V_0xB) {
                writer.writeLEB(relativeDepth);
            } else {
                writer.writeFixed(relativeDepth);
            }
        }

        int defaultDepth = blockDepths.get(expression.getDefaultTarget());
        int relativeDepth = depth - defaultDepth;
        if (version != WasmBinaryVersion.V_0xB) {
            writer.writeLEB(relativeDepth);
        } else {
            writer.writeFixed(relativeDepth);
        }
    }

    @Override
    public void visit(WasmConditional expression) {
        expression.getCondition().acceptVisitor(this);
        writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x04 : 0x03);
        writeBlockType(expression.getType());

        ++depth;
        blockDepths.put(expression.getThenBlock(), depth);
        for (WasmExpression part : expression.getThenBlock().getBody()) {
            part.acceptVisitor(this);
        }
        blockDepths.remove(expression.getThenBlock());

        if (!expression.getElseBlock().getBody().isEmpty()) {
            writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x05 : 0x04);
            blockDepths.put(expression.getElseBlock(), depth);
            for (WasmExpression part : expression.getElseBlock().getBody()) {
                part.acceptVisitor(this);
            }
            blockDepths.remove(expression.getElseBlock());
        }
        --depth;

        writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x0B : 0x0F);
    }

    @Override
    public void visit(WasmReturn expression) {
        if (expression.getValue() != null) {
            expression.getValue().acceptVisitor(this);
        }
        writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x0F : 0x09);
        if (version == WasmBinaryVersion.V_0xB) {
            writer.writeByte(expression.getValue() != null ? 1 : 0);
        }
    }

    @Override
    public void visit(WasmUnreachable expression) {
        if (version == WasmBinaryVersion.V_0xB) {
            writer.writeByte(0x0A);
        } else {
            writer.writeByte(0x0);
        }
    }

    @Override
    public void visit(WasmInt32Constant expression) {
        writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x41 : 0x10);
        writer.writeSignedLEB(expression.getValue());
    }

    @Override
    public void visit(WasmInt64Constant expression) {
        writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x42 : 0x11);
        writer.writeSignedLEB(expression.getValue());
    }

    @Override
    public void visit(WasmFloat32Constant expression) {
        writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x43 : 0x13);
        writer.writeFixed(Float.floatToRawIntBits(expression.getValue()));
    }

    @Override
    public void visit(WasmFloat64Constant expression) {
        writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x44 : 0x12);
        writer.writeFixed(Double.doubleToRawLongBits(expression.getValue()));
    }

    @Override
    public void visit(WasmGetLocal expression) {
        writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x20 : 0x14);
        writer.writeLEB(expression.getLocal().getIndex());
    }

    @Override
    public void visit(WasmSetLocal expression) {
        expression.getValue().acceptVisitor(this);
        writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x21 : 0x15);
        writer.writeLEB(expression.getLocal().getIndex());
    }

    @Override
    public void visit(WasmIntBinary expression) {
        expression.getFirst().acceptVisitor(this);
        expression.getSecond().acceptVisitor(this);
        if (version == WasmBinaryVersion.V_0xD) {
            render0xD(expression);
        } else {
            renderOld(expression);
        }
    }

    private void render0xD(WasmIntBinary expression) {
        switch (expression.getType()) {
            case INT32:
                switch (expression.getOperation()) {
                    case ADD:
                        writer.writeByte(0x6A);
                        break;
                    case SUB:
                        writer.writeByte(0x6B);
                        break;
                    case MUL:
                        writer.writeByte(0x6C);
                        break;
                    case DIV_SIGNED:
                        writer.writeByte(0x6D);
                        break;
                    case DIV_UNSIGNED:
                        writer.writeByte(0x6E);
                        break;
                    case REM_SIGNED:
                        writer.writeByte(0x6F);
                        break;
                    case REM_UNSIGNED:
                        writer.writeByte(0x70);
                        break;
                    case AND:
                        writer.writeByte(0x71);
                        break;
                    case OR:
                        writer.writeByte(0x72);
                        break;
                    case XOR:
                        writer.writeByte(0x73);
                        break;
                    case SHL:
                        writer.writeByte(0x74);
                        break;
                    case SHR_UNSIGNED:
                        writer.writeByte(0x75);
                        break;
                    case SHR_SIGNED:
                        writer.writeByte(0x76);
                        break;
                    case ROTR:
                        writer.writeByte(0x77);
                        break;
                    case ROTL:
                        writer.writeByte(0x78);
                        break;
                    case EQ:
                        writer.writeByte(0x46);
                        break;
                    case NE:
                        writer.writeByte(0x47);
                        break;
                    case LT_SIGNED:
                        writer.writeByte(0x48);
                        break;
                    case LT_UNSIGNED:
                        writer.writeByte(0x49);
                        break;
                    case GT_SIGNED:
                        writer.writeByte(0x4A);
                        break;
                    case GT_UNSIGNED:
                        writer.writeByte(0x4B);
                        break;
                    case LE_SIGNED:
                        writer.writeByte(0x4C);
                        break;
                    case LE_UNSIGNED:
                        writer.writeByte(0x4D);
                        break;
                    case GE_SIGNED:
                        writer.writeByte(0x4E);
                        break;
                    case GE_UNSIGNED:
                        writer.writeByte(0x4F);
                        break;
                }
                break;
            case INT64:
                switch (expression.getOperation()) {
                    case ADD:
                        writer.writeByte(0x7C);
                        break;
                    case SUB:
                        writer.writeByte(0x7D);
                        break;
                    case MUL:
                        writer.writeByte(0x7E);
                        break;
                    case DIV_SIGNED:
                        writer.writeByte(0x7F);
                        break;
                    case DIV_UNSIGNED:
                        writer.writeByte(0x80);
                        break;
                    case REM_SIGNED:
                        writer.writeByte(0x81);
                        break;
                    case REM_UNSIGNED:
                        writer.writeByte(0x82);
                        break;
                    case AND:
                        writer.writeByte(0x83);
                        break;
                    case OR:
                        writer.writeByte(0x84);
                        break;
                    case XOR:
                        writer.writeByte(0x85);
                        break;
                    case SHL:
                        writer.writeByte(0x86);
                        break;
                    case SHR_UNSIGNED:
                        writer.writeByte(0x87);
                        break;
                    case SHR_SIGNED:
                        writer.writeByte(0x88);
                        break;
                    case ROTR:
                        writer.writeByte(0x89);
                        break;
                    case ROTL:
                        writer.writeByte(0x8A);
                        break;
                    case EQ:
                        writer.writeByte(0x51);
                        break;
                    case NE:
                        writer.writeByte(0x52);
                        break;
                    case LT_SIGNED:
                        writer.writeByte(0x53);
                        break;
                    case LT_UNSIGNED:
                        writer.writeByte(0x54);
                        break;
                    case GT_SIGNED:
                        writer.writeByte(0x55);
                        break;
                    case GT_UNSIGNED:
                        writer.writeByte(0x56);
                        break;
                    case LE_SIGNED:
                        writer.writeByte(0x57);
                        break;
                    case LE_UNSIGNED:
                        writer.writeByte(0x58);
                        break;
                    case GE_SIGNED:
                        writer.writeByte(0x59);
                        break;
                    case GE_UNSIGNED:
                        writer.writeByte(0x5A);
                        break;
                }
                break;
        }
    }

    private void renderOld(WasmIntBinary expression) {
        switch (expression.getType()) {
            case INT32:
                switch (expression.getOperation()) {
                    case ADD:
                        writer.writeByte(0x40);
                        break;
                    case SUB:
                        writer.writeByte(0x41);
                        break;
                    case MUL:
                        writer.writeByte(0x42);
                        break;
                    case DIV_SIGNED:
                        writer.writeByte(0x43);
                        break;
                    case DIV_UNSIGNED:
                        writer.writeByte(0x44);
                        break;
                    case REM_SIGNED:
                        writer.writeByte(0x45);
                        break;
                    case REM_UNSIGNED:
                        writer.writeByte(0x46);
                        break;
                    case AND:
                        writer.writeByte(0x47);
                        break;
                    case OR:
                        writer.writeByte(0x48);
                        break;
                    case XOR:
                        writer.writeByte(0x49);
                        break;
                    case SHL:
                        writer.writeByte(0x4A);
                        break;
                    case SHR_UNSIGNED:
                        writer.writeByte(0x4B);
                        break;
                    case SHR_SIGNED:
                        writer.writeByte(0x4C);
                        break;
                    case ROTR:
                        writer.writeByte(0xB6);
                        break;
                    case ROTL:
                        writer.writeByte(0xB7);
                        break;
                    case EQ:
                        writer.writeByte(0x4D);
                        break;
                    case NE:
                        writer.writeByte(0x4E);
                        break;
                    case LT_SIGNED:
                        writer.writeByte(0x4F);
                        break;
                    case LE_SIGNED:
                        writer.writeByte(0x50);
                        break;
                    case LT_UNSIGNED:
                        writer.writeByte(0x51);
                        break;
                    case LE_UNSIGNED:
                        writer.writeByte(0x52);
                        break;
                    case GT_SIGNED:
                        writer.writeByte(0x53);
                        break;
                    case GE_SIGNED:
                        writer.writeByte(0x54);
                        break;
                    case GT_UNSIGNED:
                        writer.writeByte(0x55);
                        break;
                    case GE_UNSIGNED:
                        writer.writeByte(0x56);
                        break;
                }
                break;
            case INT64:
                switch (expression.getOperation()) {
                    case ADD:
                        writer.writeByte(0x5B);
                        break;
                    case SUB:
                        writer.writeByte(0x5C);
                        break;
                    case MUL:
                        writer.writeByte(0x5D);
                        break;
                    case DIV_SIGNED:
                        writer.writeByte(0x5E);
                        break;
                    case DIV_UNSIGNED:
                        writer.writeByte(0x5F);
                        break;
                    case REM_SIGNED:
                        writer.writeByte(0x60);
                        break;
                    case REM_UNSIGNED:
                        writer.writeByte(0x61);
                        break;
                    case AND:
                        writer.writeByte(0x62);
                        break;
                    case OR:
                        writer.writeByte(0x63);
                        break;
                    case XOR:
                        writer.writeByte(0x64);
                        break;
                    case SHL:
                        writer.writeByte(0x65);
                        break;
                    case SHR_UNSIGNED:
                        writer.writeByte(0x66);
                        break;
                    case SHR_SIGNED:
                        writer.writeByte(0x67);
                        break;
                    case ROTR:
                        writer.writeByte(0xB8);
                        break;
                    case ROTL:
                        writer.writeByte(0xB9);
                        break;
                    case EQ:
                        writer.writeByte(0x68);
                        break;
                    case NE:
                        writer.writeByte(0x69);
                        break;
                    case LT_SIGNED:
                        writer.writeByte(0x6A);
                        break;
                    case LE_SIGNED:
                        writer.writeByte(0x6B);
                        break;
                    case LT_UNSIGNED:
                        writer.writeByte(0x6C);
                        break;
                    case LE_UNSIGNED:
                        writer.writeByte(0x6D);
                        break;
                    case GT_SIGNED:
                        writer.writeByte(0x6E);
                        break;
                    case GE_SIGNED:
                        writer.writeByte(0x6F);
                        break;
                    case GT_UNSIGNED:
                        writer.writeByte(0x70);
                        break;
                    case GE_UNSIGNED:
                        writer.writeByte(0x71);
                        break;
                }
                break;
        }
    }

    @Override
    public void visit(WasmFloatBinary expression) {
        expression.getFirst().acceptVisitor(this);
        expression.getSecond().acceptVisitor(this);
        if (version == WasmBinaryVersion.V_0xD) {
            render0xD(expression);
        } else {
            renderOld(expression);
        }
    }

    private void render0xD(WasmFloatBinary expression) {
        switch (expression.getType()) {
            case FLOAT32:
                switch (expression.getOperation()) {
                    case ADD:
                        writer.writeByte(0x92);
                        break;
                    case SUB:
                        writer.writeByte(0x93);
                        break;
                    case MUL:
                        writer.writeByte(0x94);
                        break;
                    case DIV:
                        writer.writeByte(0x95);
                        break;
                    case MIN:
                        writer.writeByte(0x96);
                        break;
                    case MAX:
                        writer.writeByte(0x97);
                        break;
                    case EQ:
                        writer.writeByte(0x5B);
                        break;
                    case NE:
                        writer.writeByte(0x5C);
                        break;
                    case LT:
                        writer.writeByte(0x5D);
                        break;
                    case GT:
                        writer.writeByte(0x5E);
                        break;
                    case LE:
                        writer.writeByte(0x5F);
                        break;
                    case GE:
                        writer.writeByte(0x60);
                        break;
                }
                break;
            case FLOAT64:
                switch (expression.getOperation()) {
                    case ADD:
                        writer.writeByte(0xA0);
                        break;
                    case SUB:
                        writer.writeByte(0xA1);
                        break;
                    case MUL:
                        writer.writeByte(0xA2);
                        break;
                    case DIV:
                        writer.writeByte(0xA3);
                        break;
                    case MIN:
                        writer.writeByte(0xA4);
                        break;
                    case MAX:
                        writer.writeByte(0xA5);
                        break;
                    case EQ:
                        writer.writeByte(0x61);
                        break;
                    case NE:
                        writer.writeByte(0x62);
                        break;
                    case LT:
                        writer.writeByte(0x63);
                        break;
                    case GT:
                        writer.writeByte(0x64);
                        break;
                    case LE:
                        writer.writeByte(0x65);
                        break;
                    case GE:
                        writer.writeByte(0x66);
                        break;
                }
                break;
        }
    }

    private void renderOld(WasmFloatBinary expression) {
        switch (expression.getType()) {
            case FLOAT32:
                switch (expression.getOperation()) {
                    case ADD:
                        writer.writeByte(0x75);
                        break;
                    case SUB:
                        writer.writeByte(0x76);
                        break;
                    case MUL:
                        writer.writeByte(0x77);
                        break;
                    case DIV:
                        writer.writeByte(0x78);
                        break;
                    case MIN:
                        writer.writeByte(0x79);
                        break;
                    case MAX:
                        writer.writeByte(0x7A);
                        break;
                    case EQ:
                        writer.writeByte(0x83);
                        break;
                    case NE:
                        writer.writeByte(0x84);
                        break;
                    case LT:
                        writer.writeByte(0x85);
                        break;
                    case LE:
                        writer.writeByte(0x86);
                        break;
                    case GT:
                        writer.writeByte(0x87);
                        break;
                    case GE:
                        writer.writeByte(0x88);
                        break;
                }
                break;
            case FLOAT64:
                switch (expression.getOperation()) {
                    case ADD:
                        writer.writeByte(0x89);
                        break;
                    case SUB:
                        writer.writeByte(0x8A);
                        break;
                    case MUL:
                        writer.writeByte(0x8B);
                        break;
                    case DIV:
                        writer.writeByte(0x8C);
                        break;
                    case MIN:
                        writer.writeByte(0x8D);
                        break;
                    case MAX:
                        writer.writeByte(0x8E);
                        break;
                    case EQ:
                        writer.writeByte(0x97);
                        break;
                    case NE:
                        writer.writeByte(0x98);
                        break;
                    case LT:
                        writer.writeByte(0x99);
                        break;
                    case LE:
                        writer.writeByte(0x9A);
                        break;
                    case GT:
                        writer.writeByte(0x9B);
                        break;
                    case GE:
                        writer.writeByte(0x9C);
                        break;
                }
                break;
        }
    }

    @Override
    public void visit(WasmIntUnary expression) {
        expression.getOperand().acceptVisitor(this);
        if (version == WasmBinaryVersion.V_0xD) {
            switch (expression.getType()) {
                case INT32:
                    switch (expression.getOperation()) {
                        case CLZ:
                            writer.writeByte(0x67);
                            break;
                        case CTZ:
                            writer.writeByte(0x68);
                            break;
                        case POPCNT:
                            writer.writeByte(0x69);
                            break;
                    }
                    break;
                case INT64:
                    switch (expression.getOperation()) {
                        case CLZ:
                            writer.writeByte(0x79);
                            break;
                        case CTZ:
                            writer.writeByte(0x7A);
                            break;
                        case POPCNT:
                            writer.writeByte(0x7B);
                            break;
                    }
                    break;
            }
        } else {
            switch (expression.getType()) {
                case INT32:
                    switch (expression.getOperation()) {
                        case CLZ:
                            writer.writeByte(0x57);
                            break;
                        case CTZ:
                            writer.writeByte(0x58);
                            break;
                        case POPCNT:
                            writer.writeByte(0x59);
                            break;
                    }
                    break;
                case INT64:
                    switch (expression.getOperation()) {
                        case CLZ:
                            writer.writeByte(0x72);
                            break;
                        case CTZ:
                            writer.writeByte(0x73);
                            break;
                        case POPCNT:
                            writer.writeByte(0x74);
                            break;
                    }
                    break;
            }
        }
    }

    @Override
    public void visit(WasmFloatUnary expression) {
        expression.getOperand().acceptVisitor(this);
        if (version == WasmBinaryVersion.V_0xD) {
            render0xD(expression);
        } else {
            renderOld(expression);
        }
    }

    private void render0xD(WasmFloatUnary expression) {
        switch (expression.getType()) {
            case FLOAT32:
                switch (expression.getOperation()) {
                    case ABS:
                        writer.writeByte(0x8B);
                        break;
                    case NEG:
                        writer.writeByte(0x8C);
                        break;
                    case CEIL:
                        writer.writeByte(0x8D);
                        break;
                    case FLOOR:
                        writer.writeByte(0x8E);
                        break;
                    case TRUNC:
                        writer.writeByte(0x8F);
                        break;
                    case NEAREST:
                        writer.writeByte(0x90);
                        break;
                    case SQRT:
                        writer.writeByte(0x91);
                        break;
                    case COPYSIGN:
                        writer.writeByte(0x98);
                        break;
                }
                break;
            case FLOAT64:
                switch (expression.getOperation()) {
                    case ABS:
                        writer.writeByte(0x99);
                        break;
                    case NEG:
                        writer.writeByte(0x9A);
                        break;
                    case CEIL:
                        writer.writeByte(0x9B);
                        break;
                    case FLOOR:
                        writer.writeByte(0x9C);
                        break;
                    case TRUNC:
                        writer.writeByte(0x9D);
                        break;
                    case NEAREST:
                        writer.writeByte(0x9E);
                        break;
                    case SQRT:
                        writer.writeByte(0x9F);
                        break;
                    case COPYSIGN:
                        writer.writeByte(0xA6);
                        break;
                }
                break;
        }
    }

    private void renderOld(WasmFloatUnary expression) {
        switch (expression.getType()) {
            case FLOAT32:
                switch (expression.getOperation()) {
                    case ABS:
                        writer.writeByte(0x7B);
                        break;
                    case NEG:
                        writer.writeByte(0x7C);
                        break;
                    case COPYSIGN:
                        writer.writeByte(0x7D);
                        break;
                    case CEIL:
                        writer.writeByte(0x7E);
                        break;
                    case FLOOR:
                        writer.writeByte(0x7F);
                        break;
                    case TRUNC:
                        writer.writeByte(0x80);
                        break;
                    case NEAREST:
                        writer.writeByte(0x81);
                        break;
                    case SQRT:
                        writer.writeByte(0x82);
                        break;
                }
                break;
            case FLOAT64:
                switch (expression.getOperation()) {
                    case ABS:
                        writer.writeByte(0x8F);
                        break;
                    case NEG:
                        writer.writeByte(0x90);
                        break;
                    case COPYSIGN:
                        writer.writeByte(0x91);
                        break;
                    case CEIL:
                        writer.writeByte(0x92);
                        break;
                    case FLOOR:
                        writer.writeByte(0x93);
                        break;
                    case TRUNC:
                        writer.writeByte(0x94);
                        break;
                    case NEAREST:
                        writer.writeByte(0x95);
                        break;
                    case SQRT:
                        writer.writeByte(0x96);
                        break;
                }
                break;
        }
    }

    @Override
    public void visit(WasmConversion expression) {
        expression.getOperand().acceptVisitor(this);
        if (version == WasmBinaryVersion.V_0xD) {
            render0xD(expression);
        } else {
            renderOld(expression);
        }
    }

    private void render0xD(WasmConversion expression) {
        switch (expression.getSourceType()) {
            case INT32:
                switch (expression.getTargetType()) {
                    case INT32:
                        break;
                    case INT64:
                        writer.writeByte(expression.isSigned() ? 0xAC : 0xAD);
                        break;
                    case FLOAT32:
                        writer.writeByte(expression.isSigned() ? 0xB2 : 0xB3);
                        break;
                    case FLOAT64:
                        writer.writeByte(expression.isSigned() ? 0xB7 : 0xB8);
                        break;
                }
                break;
            case INT64:
                switch (expression.getTargetType()) {
                    case INT32:
                        writer.writeByte(0xA7);
                        break;
                    case INT64:
                        break;
                    case FLOAT32:
                        writer.writeByte(expression.isSigned() ? 0xB4 : 0xB5);
                        break;
                    case FLOAT64:
                        writer.writeByte(expression.isSigned() ? 0xB9 : 0xBA);
                        break;
                }
                break;
            case FLOAT32:
                switch (expression.getTargetType()) {
                    case INT32:
                        writer.writeByte(expression.isSigned() ? 0xA8 : 0xA9);
                        break;
                    case INT64:
                        writer.writeByte(expression.isSigned() ? 0xAE : 0xAF);
                        break;
                    case FLOAT32:
                        break;
                    case FLOAT64:
                        writer.writeByte(0xBB);
                        break;
                }
                break;
            case FLOAT64:
                switch (expression.getTargetType()) {
                    case INT32:
                        writer.writeByte(expression.isSigned() ? 0xAA : 0xAB);
                        break;
                    case INT64:
                        writer.writeByte(expression.isSigned() ? 0xB0 : 0xB1);
                        break;
                    case FLOAT32:
                        writer.writeByte(0xB6);
                        break;
                    case FLOAT64:
                        break;
                }
                break;
        }
    }

    private void renderOld(WasmConversion expression) {
        switch (expression.getSourceType()) {
            case INT32:
                switch (expression.getTargetType()) {
                    case INT32:
                        break;
                    case INT64:
                        writer.writeByte(expression.isSigned() ? 0xA6 : 0xA7);
                        break;
                    case FLOAT32:
                        writer.writeByte(expression.isSigned() ? 0xA8 : 0xA9);
                        break;
                    case FLOAT64:
                        writer.writeByte(expression.isSigned() ? 0xAE : 0xAF);
                        break;
                }
                break;
            case INT64:
                switch (expression.getTargetType()) {
                    case INT32:
                        writer.writeByte(0xA1);
                        break;
                    case INT64:
                        break;
                    case FLOAT32:
                        writer.writeByte(expression.isSigned() ? 0xAA : 0xAB);
                        break;
                    case FLOAT64:
                        writer.writeByte(expression.isSigned() ? 0xB0 : 0xB1);
                        break;
                }
                break;
            case FLOAT32:
                switch (expression.getTargetType()) {
                    case INT32:
                        writer.writeByte(expression.isSigned() ? 0x9D : 0x9F);
                        break;
                    case INT64:
                        writer.writeByte(expression.isSigned() ? 0xA2 : 0xA4);
                        break;
                    case FLOAT32:
                        break;
                    case FLOAT64:
                        writer.writeByte(0xB2);
                        break;
                }
                break;
            case FLOAT64:
                switch (expression.getTargetType()) {
                    case INT32:
                        writer.writeByte(expression.isSigned() ? 0x9E : 0xA0);
                        break;
                    case INT64:
                        writer.writeByte(expression.isSigned() ? 0xA3 : 0xA5);
                        break;
                    case FLOAT32:
                        writer.writeByte(0xAC);
                        break;
                    case FLOAT64:
                        break;
                }
                break;
        }
    }

    @Override
    public void visit(WasmCall expression) {
        for (WasmExpression argument : expression.getArguments()) {
            argument.acceptVisitor(this);
        }
        Integer functionIndex = !expression.isImported()
                ? functionIndexes.get(expression.getFunctionName())
                : importedIndexes.get(expression.getFunctionName());
        if (functionIndex == null) {
            writer.writeByte(0x0A);
            return;
        }

        if (version == WasmBinaryVersion.V_0xB) {
            writer.writeByte(!expression.isImported() ? 0x16 : 0x18);
            writer.writeLEB(expression.getArguments().size());
        } else {
            writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x10 : 0x16);
        }
        writer.writeLEB(functionIndex);
    }

    @Override
    public void visit(WasmIndirectCall expression) {
        if (version == WasmBinaryVersion.V_0xB) {
            expression.getSelector().acceptVisitor(this);
        }
        for (WasmExpression argument : expression.getArguments()) {
            argument.acceptVisitor(this);
        }
        if (version != WasmBinaryVersion.V_0xB) {
            expression.getSelector().acceptVisitor(this);
        }
        writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x11 : 0x17);
        if (version == WasmBinaryVersion.V_0xB) {
            writer.writeLEB(expression.getArguments().size());
        }

        WasmType[] signatureTypes = new WasmType[expression.getParameterTypes().size() + 1];
        signatureTypes[0] = expression.getReturnType();
        for (int i = 0; i < expression.getParameterTypes().size(); ++i) {
            signatureTypes[i + 1] = expression.getParameterTypes().get(i);
        }
        writer.writeLEB(signatureIndexes.get(new WasmSignature(signatureTypes)));

        if (version == WasmBinaryVersion.V_0xD) {
            writer.writeByte(0);
        }
    }

    @Override
    public void visit(WasmDrop expression) {
        expression.getOperand().acceptVisitor(this);
        if (version == WasmBinaryVersion.V_0xC) {
            writer.writeByte(0x0B);
        } else if (version == WasmBinaryVersion.V_0xD) {
            writer.writeByte(0x1A);
        }
    }

    @Override
    public void visit(WasmLoadInt32 expression) {
        expression.getIndex().acceptVisitor(this);
        if (version == WasmBinaryVersion.V_0xD) {
            switch (expression.getConvertFrom()) {
                case INT8:
                    writer.writeByte(0x2C);
                    break;
                case UINT8:
                    writer.writeByte(0x2D);
                    break;
                case INT16:
                    writer.writeByte(0x2E);
                    break;
                case UINT16:
                    writer.writeByte(0x2F);
                    break;
                case INT32:
                    writer.writeByte(0x28);
                    break;
            }
        } else {
            switch (expression.getConvertFrom()) {
                case INT8:
                    writer.writeByte(0x20);
                    break;
                case UINT8:
                    writer.writeByte(0x21);
                    break;
                case INT16:
                    writer.writeByte(0x22);
                    break;
                case UINT16:
                    writer.writeByte(0x23);
                    break;
                case INT32:
                    writer.writeByte(0x2A);
                    break;
            }
        }
        writer.writeByte(alignment(expression.getAlignment()));
        writer.writeLEB(expression.getOffset());
    }

    @Override
    public void visit(WasmLoadInt64 expression) {
        expression.getIndex().acceptVisitor(this);
        if (version == WasmBinaryVersion.V_0xD) {
            switch (expression.getConvertFrom()) {
                case INT8:
                    writer.writeByte(0x30);
                    break;
                case UINT8:
                    writer.writeByte(0x31);
                    break;
                case INT16:
                    writer.writeByte(0x32);
                    break;
                case UINT16:
                    writer.writeByte(0x33);
                    break;
                case INT32:
                    writer.writeByte(0x34);
                    break;
                case UINT32:
                    writer.writeByte(0x25);
                    break;
                case INT64:
                    writer.writeByte(0x29);
                    break;
            }
        } else {
            switch (expression.getConvertFrom()) {
                case INT8:
                    writer.writeByte(0x24);
                    break;
                case UINT8:
                    writer.writeByte(0x25);
                    break;
                case INT16:
                    writer.writeByte(0x26);
                    break;
                case UINT16:
                    writer.writeByte(0x27);
                    break;
                case INT32:
                    writer.writeByte(0x28);
                    break;
                case UINT32:
                    writer.writeByte(0x29);
                    break;
                case INT64:
                    writer.writeByte(0x2B);
                    break;
            }
        }
        writer.writeByte(alignment(expression.getAlignment()));
        writer.writeLEB(expression.getOffset());
    }

    @Override
    public void visit(WasmLoadFloat32 expression) {
        expression.getIndex().acceptVisitor(this);
        writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x2A : 0x2C);
        writer.writeByte(alignment(expression.getAlignment()));
        writer.writeLEB(expression.getOffset());
    }

    @Override
    public void visit(WasmLoadFloat64 expression) {
        expression.getIndex().acceptVisitor(this);
        writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x2B : 0x2D);
        writer.writeByte(alignment(expression.getAlignment()));
        writer.writeLEB(expression.getOffset());
    }

    @Override
    public void visit(WasmStoreInt32 expression) {
        expression.getIndex().acceptVisitor(this);
        expression.getValue().acceptVisitor(this);
        if (version == WasmBinaryVersion.V_0xD) {
            switch (expression.getConvertTo()) {
                case INT8:
                case UINT8:
                    writer.writeByte(0x3A);
                    break;
                case INT16:
                case UINT16:
                    writer.writeByte(0x3B);
                    break;
                case INT32:
                    writer.writeByte(0x36);
                    break;
            }
        } else {
            switch (expression.getConvertTo()) {
                case INT8:
                case UINT8:
                    writer.writeByte(0x2E);
                    break;
                case INT16:
                case UINT16:
                    writer.writeByte(0x2F);
                    break;
                case INT32:
                    writer.writeByte(0x33);
                    break;
            }
        }
        writer.writeByte(alignment(expression.getAlignment()));
        writer.writeLEB(expression.getOffset());
    }

    @Override
    public void visit(WasmStoreInt64 expression) {
        expression.getIndex().acceptVisitor(this);
        expression.getValue().acceptVisitor(this);
        if (version == WasmBinaryVersion.V_0xD) {
            switch (expression.getConvertTo()) {
                case INT8:
                case UINT8:
                    writer.writeByte(0x3C);
                    break;
                case INT16:
                case UINT16:
                    writer.writeByte(0x3D);
                    break;
                case INT32:
                case UINT32:
                    writer.writeByte(0x3E);
                    break;
                case INT64:
                    writer.writeByte(0x37);
                    break;
            }
        } else {
            switch (expression.getConvertTo()) {
                case INT8:
                case UINT8:
                    writer.writeByte(0x30);
                    break;
                case INT16:
                case UINT16:
                    writer.writeByte(0x31);
                    break;
                case INT32:
                case UINT32:
                    writer.writeByte(0x32);
                    break;
                case INT64:
                    writer.writeByte(0x34);
                    break;
            }
        }
        writer.writeByte(alignment(expression.getAlignment()));
        writer.writeLEB(expression.getOffset());
    }

    @Override
    public void visit(WasmStoreFloat32 expression) {
        expression.getIndex().acceptVisitor(this);
        expression.getValue().acceptVisitor(this);
        writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x38 : 0x35);
        writer.writeByte(alignment(expression.getAlignment()));
        writer.writeLEB(expression.getOffset());
    }

    @Override
    public void visit(WasmStoreFloat64 expression) {
        expression.getIndex().acceptVisitor(this);
        expression.getValue().acceptVisitor(this);
        writer.writeByte(version == WasmBinaryVersion.V_0xD ? 0x39 : 0x36);
        writer.writeByte(alignment(expression.getAlignment()));
        writer.writeLEB(expression.getOffset());
    }

    private int alignment(int value) {
        return 31 - Integer.numberOfLeadingZeros(Math.max(1, value));
    }

    private void writeLabel(WasmBlock target) {
        int blockDepth = blockDepths.get(target);
        writer.writeLEB(depth - blockDepth);
    }
}
