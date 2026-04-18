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
package org.teavm.backend.wasm.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.teavm.backend.wasm.debug.DebugLines;
import org.teavm.backend.wasm.generate.DwarfGenerator;
import org.teavm.backend.wasm.model.WasmBlockType;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.expression.WasmSignedType;
import org.teavm.backend.wasm.model.instruction.WasmArrayCopyInstruction;
import org.teavm.backend.wasm.model.instruction.WasmArrayGetInstruction;
import org.teavm.backend.wasm.model.instruction.WasmArrayLengthInstruction;
import org.teavm.backend.wasm.model.instruction.WasmArrayNewDefaultInstruction;
import org.teavm.backend.wasm.model.instruction.WasmArrayNewFixedInstruction;
import org.teavm.backend.wasm.model.instruction.WasmArraySetInstruction;
import org.teavm.backend.wasm.model.instruction.WasmBlockInstruction;
import org.teavm.backend.wasm.model.instruction.WasmBranchInstruction;
import org.teavm.backend.wasm.model.instruction.WasmBreakInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCallInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCallReferenceInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCastBranchInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCastInstruction;
import org.teavm.backend.wasm.model.instruction.WasmConditionalInstruction;
import org.teavm.backend.wasm.model.instruction.WasmConversionInstruction;
import org.teavm.backend.wasm.model.instruction.WasmCopyInstruction;
import org.teavm.backend.wasm.model.instruction.WasmDropInstruction;
import org.teavm.backend.wasm.model.instruction.WasmExternConversionInstruction;
import org.teavm.backend.wasm.model.instruction.WasmFillInstruction;
import org.teavm.backend.wasm.model.instruction.WasmFloat32ConstantInstruction;
import org.teavm.backend.wasm.model.instruction.WasmFloat64ConstantInstruction;
import org.teavm.backend.wasm.model.instruction.WasmFloatBinaryInstruction;
import org.teavm.backend.wasm.model.instruction.WasmFloatUnaryInstruction;
import org.teavm.backend.wasm.model.instruction.WasmFunctionReferenceInstruction;
import org.teavm.backend.wasm.model.instruction.WasmGetGlobalInstruction;
import org.teavm.backend.wasm.model.instruction.WasmGetLocalInstruction;
import org.teavm.backend.wasm.model.instruction.WasmIndirectCallInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmInstructionVisitor;
import org.teavm.backend.wasm.model.instruction.WasmInt31GetInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInt31ReferenceInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInt32ConstantInstruction;
import org.teavm.backend.wasm.model.instruction.WasmInt64ConstantInstruction;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryInstruction;
import org.teavm.backend.wasm.model.instruction.WasmIntUnaryInstruction;
import org.teavm.backend.wasm.model.instruction.WasmIsNullInstruction;
import org.teavm.backend.wasm.model.instruction.WasmLoadFloat32Instruction;
import org.teavm.backend.wasm.model.instruction.WasmLoadFloat64Instruction;
import org.teavm.backend.wasm.model.instruction.WasmLoadInt32Instruction;
import org.teavm.backend.wasm.model.instruction.WasmLoadInt64Instruction;
import org.teavm.backend.wasm.model.instruction.WasmMemoryGrowInstruction;
import org.teavm.backend.wasm.model.instruction.WasmNullBranchInstruction;
import org.teavm.backend.wasm.model.instruction.WasmNullConstantInstruction;
import org.teavm.backend.wasm.model.instruction.WasmReferencesEqualInstruction;
import org.teavm.backend.wasm.model.instruction.WasmReturnInstruction;
import org.teavm.backend.wasm.model.instruction.WasmSetGlobalInstruction;
import org.teavm.backend.wasm.model.instruction.WasmSetLocalInstruction;
import org.teavm.backend.wasm.model.instruction.WasmStoreFloat32Instruction;
import org.teavm.backend.wasm.model.instruction.WasmStoreFloat64Instruction;
import org.teavm.backend.wasm.model.instruction.WasmStoreInt32Instruction;
import org.teavm.backend.wasm.model.instruction.WasmStoreInt64Instruction;
import org.teavm.backend.wasm.model.instruction.WasmStructGetInstruction;
import org.teavm.backend.wasm.model.instruction.WasmStructNewDefaultInstruction;
import org.teavm.backend.wasm.model.instruction.WasmStructNewInstruction;
import org.teavm.backend.wasm.model.instruction.WasmStructSetInstruction;
import org.teavm.backend.wasm.model.instruction.WasmSwitchInstruction;
import org.teavm.backend.wasm.model.instruction.WasmTeeLocalInstruction;
import org.teavm.backend.wasm.model.instruction.WasmTestInstruction;
import org.teavm.backend.wasm.model.instruction.WasmThrowInstruction;
import org.teavm.backend.wasm.model.instruction.WasmTryInstruction;
import org.teavm.backend.wasm.model.instruction.WasmUnreachableInstruction;
import org.teavm.model.InliningInfo;
import org.teavm.model.TextLocation;

class WasmBinaryInstructionRenderingVisitor implements WasmInstructionVisitor {
    private final WasmBinaryWriter writer;
    private final WasmModule module;
    private final DebugLines debugLines;
    private final DwarfGenerator dwarfGenerator;
    private int depth;
    private final Map<WasmInstruction, Integer> blockDepths = new HashMap<>();

    private TextLocation lastEmittedLocation;
    private int addressOffset;
    private List<InliningInfo> methodStack = new ArrayList<>();
    private List<InliningInfo> currentMethodStack = new ArrayList<>();

    WasmBinaryInstructionRenderingVisitor(WasmBinaryWriter writer, WasmModule module, DebugLines debugLines,
            DwarfGenerator dwarfGenerator, int addressOffset) {
        this.writer = writer;
        this.module = module;
        this.debugLines = debugLines;
        this.dwarfGenerator = dwarfGenerator;
        this.addressOffset = addressOffset;
    }


    void render(WasmInstructionList list) {
        for (var instruction : list) {
            instruction.acceptVisitor(this);
        }
    }

    @Override
    public void visit(WasmUnreachableInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x00);
    }

    @Override
    public void visit(WasmBlockInstruction instruction) {
        emitLocation(instruction);
        ++depth;
        blockDepths.put(instruction, depth);
        writer.writeByte(instruction.isLoop() ? 0x03 : 0x02);
        writeBlockType(instruction.getType());
        render(instruction.getBody());
        writer.writeByte(0x0B);
        blockDepths.remove(instruction);
        --depth;
    }

    @Override
    public void visit(WasmConditionalInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x04);
        writeBlockType(instruction.getType());
        ++depth;
        blockDepths.put(instruction, depth);
        render(instruction.getThenBlock());
        if (!instruction.getElseBlock().isEmpty()) {
            writer.writeByte(0x05);
            render(instruction.getElseBlock());
        }
        blockDepths.remove(instruction);
        --depth;
        writer.writeByte(0x0B);
    }

    @Override
    public void visit(WasmBranchInstruction instruction) {
        writer.writeByte(0x0D);
        writeLabel(instruction, instruction.getTarget());
    }

    @Override
    public void visit(WasmNullBranchInstruction instruction) {
        emitLocation(instruction);
        switch (instruction.getCondition()) {
            case NULL:
                writer.writeByte(0xD5);
                break;
            case NOT_NULL:
                writer.writeByte(0xD6);
                break;
        }
        writeLabel(instruction, instruction.getTarget());
    }

    @Override
    public void visit(WasmCastBranchInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xFB);
        switch (instruction.getCondition()) {
            case SUCCESS:
                writer.writeByte(24);
                break;
            case FAILURE:
                writer.writeByte(25);
                break;
        }
        var flags = 0;
        if (instruction.getSourceType().isNullable()) {
            flags |= 1;
        }
        if (instruction.getTargetType().isNullable()) {
            flags |= 2;
        }
        writer.writeByte(flags);
        writeLabel(instruction, instruction.getTarget());
        writer.writeHeapType(instruction.getSourceType(), module);
        writer.writeHeapType(instruction.getTargetType(), module);
    }

    @Override
    public void visit(WasmBreakInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x0C);
        writeLabel(instruction, instruction.getTarget());
    }

    @Override
    public void visit(WasmSwitchInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x0E);
        writer.writeLEB(instruction.getTargets().size());
        for (var target : instruction.getTargets()) {
            writer.writeLEB(depth - blockDepths.get(target.getBreakTarget()));
        }
        writer.writeLEB(depth - blockDepths.get(instruction.getDefaultTarget().getBreakTarget()));
    }

    @Override
    public void visit(WasmReturnInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x0F);
    }

    @Override
    public void visit(WasmInt32ConstantInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x41);
        writer.writeSignedLEB(instruction.getValue());
    }

    @Override
    public void visit(WasmInt64ConstantInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x42);
        writer.writeSignedLEB(instruction.getValue());
    }

    @Override
    public void visit(WasmFloat32ConstantInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x43);
        writer.writeFixed(Float.floatToRawIntBits(instruction.getValue()));
    }

    @Override
    public void visit(WasmFloat64ConstantInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x44);
        writer.writeFixed(Double.doubleToRawLongBits(instruction.getValue()));
    }

    @Override
    public void visit(WasmNullConstantInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xD0);
        writer.writeHeapType(instruction.getType(), module);
    }

    @Override
    public void visit(WasmIsNullInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xD1);
    }

    @Override
    public void visit(WasmGetLocalInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x20);
        writer.writeLEB(instruction.getLocal().getIndex());
    }

    @Override
    public void visit(WasmSetLocalInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x21);
        writer.writeLEB(instruction.getLocal().getIndex());
    }

    @Override
    public void visit(WasmTeeLocalInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x22);
        writer.writeLEB(instruction.getLocal().getIndex());
    }

    @Override
    public void visit(WasmGetGlobalInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x23);
        writer.writeLEB(module.globals.indexOf(instruction.getGlobal()));
    }

    @Override
    public void visit(WasmSetGlobalInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x24);
        writer.writeLEB(module.globals.indexOf(instruction.getGlobal()));
    }

    @Override
    public void visit(WasmIntBinaryInstruction instruction) {
        emitLocation(instruction);
        switch (instruction.getType()) {
            case INT32:
                switch (instruction.getOperation()) {
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
                    case SHR_SIGNED:
                        writer.writeByte(0x75);
                        break;
                    case SHR_UNSIGNED:
                        writer.writeByte(0x76);
                        break;
                    case ROTL:
                        writer.writeByte(0x77);
                        break;
                    case ROTR:
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
                switch (instruction.getOperation()) {
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
                    case SHR_SIGNED:
                        writer.writeByte(0x87);
                        break;
                    case SHR_UNSIGNED:
                        writer.writeByte(0x88);
                        break;
                    case ROTL:
                        writer.writeByte(0x89);
                        break;
                    case ROTR:
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

    @Override
    public void visit(WasmFloatBinaryInstruction instruction) {
        emitLocation(instruction);
        switch (instruction.getType()) {
            case FLOAT32:
                switch (instruction.getOperation()) {
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
                switch (instruction.getOperation()) {
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

    @Override
    public void visit(WasmIntUnaryInstruction instruction) {
        emitLocation(instruction);
        switch (instruction.getType()) {
            case INT32:
                switch (instruction.getOperation()) {
                    case EQZ:
                        writer.writeByte(0x45);
                        break;
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
                switch (instruction.getOperation()) {
                    case EQZ:
                        writer.writeByte(0x50);
                        break;
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
    }

    @Override
    public void visit(WasmFloatUnaryInstruction instruction) {
        emitLocation(instruction);
        switch (instruction.getType()) {
            case FLOAT32:
                switch (instruction.getOperation()) {
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
                switch (instruction.getOperation()) {
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

    @Override
    public void visit(WasmConversionInstruction instruction) {
        emitLocation(instruction);
        switch (instruction.getSourceType()) {
            case INT32:
                switch (instruction.getTargetType()) {
                    case INT32:
                        break;
                    case INT64:
                        writer.writeByte(instruction.isSigned() ? 0xAC : 0xAD);
                        break;
                    case FLOAT32:
                        if (instruction.isReinterpret()) {
                            writer.writeByte(0xBE);
                        } else {
                            writer.writeByte(instruction.isSigned() ? 0xB2 : 0xB3);
                        }
                        break;
                    case FLOAT64:
                        writer.writeByte(instruction.isSigned() ? 0xB7 : 0xB8);
                        break;
                }
                break;
            case INT64:
                switch (instruction.getTargetType()) {
                    case INT32:
                        writer.writeByte(0xA7);
                        break;
                    case INT64:
                        break;
                    case FLOAT32:
                        writer.writeByte(instruction.isSigned() ? 0xB4 : 0xB5);
                        break;
                    case FLOAT64:
                        if (instruction.isReinterpret()) {
                            writer.writeByte(0xBF);
                        } else {
                            writer.writeByte(instruction.isSigned() ? 0xB9 : 0xBA);
                        }
                        break;
                }
                break;
            case FLOAT32:
                switch (instruction.getTargetType()) {
                    case INT32:
                        if (instruction.isReinterpret()) {
                            writer.writeByte(0xBC);
                        } else if (instruction.isNonTrapping()) {
                            writer.writeByte(0xFC);
                            writer.writeByte(instruction.isSigned() ? 0 : 1);
                        } else {
                            writer.writeByte(instruction.isSigned() ? 0xA8 : 0xA9);
                        }
                        break;
                    case INT64:
                        if (instruction.isNonTrapping()) {
                            writer.writeByte(0xFC);
                            writer.writeByte(instruction.isSigned() ? 4 : 5);
                        } else {
                            writer.writeByte(instruction.isSigned() ? 0xAE : 0xAF);
                        }
                        break;
                    case FLOAT32:
                        break;
                    case FLOAT64:
                        writer.writeByte(0xBB);
                        break;
                }
                break;
            case FLOAT64:
                switch (instruction.getTargetType()) {
                    case INT32:
                        if (instruction.isNonTrapping()) {
                            writer.writeByte(0xFC);
                            writer.writeByte(instruction.isSigned() ? 2 : 3);
                        } else {
                            writer.writeByte(instruction.isSigned() ? 0xAA : 0xAB);
                        }
                        break;
                    case INT64:
                        if (instruction.isReinterpret()) {
                            writer.writeByte(0xBD);
                        } else if (instruction.isNonTrapping()) {
                            writer.writeByte(0xFC);
                            writer.writeByte(instruction.isSigned() ? 6 : 7);
                        } else {
                            writer.writeByte(instruction.isSigned() ? 0xB0 : 0xB1);
                        }
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

    @Override
    public void visit(WasmCallInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x10);
        writer.writeLEB(module.functions.indexOf(instruction.getFunction()));
    }

    @Override
    public void visit(WasmIndirectCallInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x11);
        writer.writeLEB(module.types.indexOf(instruction.getType()));
        writer.writeByte(0);
    }

    @Override
    public void visit(WasmCallReferenceInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x14);
        writer.writeLEB(module.types.indexOf(instruction.getType()));
    }

    @Override
    public void visit(WasmDropInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x1A);
    }

    @Override
    public void visit(WasmLoadInt32Instruction instruction) {
        emitLocation(instruction);
        switch (instruction.getConvertFrom()) {
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
        writer.writeByte(alignment(instruction.getAlignment()));
        writer.writeLEB(instruction.getOffset());
    }

    @Override
    public void visit(WasmLoadInt64Instruction instruction) {
        emitLocation(instruction);
        switch (instruction.getConvertFrom()) {
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
                writer.writeByte(0x35);
                break;
            case INT64:
                writer.writeByte(0x29);
                break;
        }
        writer.writeByte(alignment(instruction.getAlignment()));
        writer.writeLEB(instruction.getOffset());
    }

    @Override
    public void visit(WasmLoadFloat32Instruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x2A);
        writer.writeByte(alignment(instruction.getAlignment()));
        writer.writeLEB(instruction.getOffset());
    }

    @Override
    public void visit(WasmLoadFloat64Instruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x2B);
        writer.writeByte(alignment(instruction.getAlignment()));
        writer.writeLEB(instruction.getOffset());
    }

    @Override
    public void visit(WasmStoreInt32Instruction instruction) {
        emitLocation(instruction);
        switch (instruction.getConvertTo()) {
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
        writer.writeByte(alignment(instruction.getAlignment()));
        writer.writeLEB(instruction.getOffset());
    }

    @Override
    public void visit(WasmStoreInt64Instruction instruction) {
        emitLocation(instruction);
        switch (instruction.getConvertTo()) {
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
        writer.writeByte(alignment(instruction.getAlignment()));
        writer.writeLEB(instruction.getOffset());
    }

    @Override
    public void visit(WasmStoreFloat32Instruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x38);
        writer.writeByte(alignment(instruction.getAlignment()));
        writer.writeLEB(instruction.getOffset());
    }

    @Override
    public void visit(WasmStoreFloat64Instruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x39);
        writer.writeByte(alignment(instruction.getAlignment()));
        writer.writeLEB(instruction.getOffset());
    }

    @Override
    public void visit(WasmMemoryGrowInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x40);
        writer.writeByte(0);
    }

    @Override
    public void visit(WasmFillInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xFC);
        writer.writeLEB(11);
        writer.writeByte(0);
    }

    @Override
    public void visit(WasmCopyInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xFC);
        writer.writeLEB(10);
        writer.writeByte(0);
        writer.writeByte(0);
    }

    @Override
    public void visit(WasmTryInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x06);
        writer.writeType(instruction.getType(), module);
        ++depth;
        blockDepths.put(instruction, depth);
        render(instruction.getBody());
        blockDepths.remove(instruction);
        --depth;
        for (var catchClause : instruction.getCatches()) {
            writer.writeByte(0x07);
            writer.writeLEB(catchClause.getTag().getIndex());
            render(catchClause);
        }
        writer.writeByte(0x0B);
    }

    @Override
    public void visit(WasmThrowInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0x08);
        writer.writeLEB(instruction.getTag().getIndex());
    }

    @Override
    public void visit(WasmReferencesEqualInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xD3);
    }

    @Override
    public void visit(WasmCastInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xFB);
        writer.writeByte(instruction.getTargetType().isNullable() ? 23 : 22);
        writer.writeHeapType(instruction.getTargetType(), module);
    }

    @Override
    public void visit(WasmTestInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xFB);
        writer.writeByte(instruction.getTestType().isNullable() ? 21 : 20);
        writer.writeHeapType(instruction.getTestType(), module);
    }

    @Override
    public void visit(WasmExternConversionInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xFB);
        switch (instruction.getType()) {
            case EXTERN_TO_ANY:
                writer.writeByte(26);
                break;
            case ANY_TO_EXTERN:
                writer.writeByte(27);
                break;
        }
    }

    @Override
    public void visit(WasmStructNewInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xFB);
        writer.writeByte(0);
        writer.writeLEB(module.types.indexOf(instruction.getType()));
    }

    @Override
    public void visit(WasmStructNewDefaultInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xFB);
        writer.writeByte(1);
        writer.writeLEB(module.types.indexOf(instruction.getType()));
    }

    @Override
    public void visit(WasmStructGetInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xFB);
        if (instruction.getSignedType() == null) {
            writer.writeByte(2);
        } else {
            switch (instruction.getSignedType()) {
                case SIGNED:
                    writer.writeByte(3);
                    break;
                case UNSIGNED:
                    writer.writeByte(4);
                    break;
            }
        }
        writer.writeLEB(module.types.indexOf(instruction.getType()));
        writer.writeLEB(instruction.getFieldIndex());
    }

    @Override
    public void visit(WasmStructSetInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xFB);
        writer.writeByte(5);
        writer.writeLEB(module.types.indexOf(instruction.getType()));
        writer.writeLEB(instruction.getFieldIndex());
    }

    @Override
    public void visit(WasmArrayNewDefaultInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xFB);
        writer.writeByte(7);
        writer.writeLEB(module.types.indexOf(instruction.getType()));
    }

    @Override
    public void visit(WasmArrayNewFixedInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xFB);
        writer.writeByte(8);
        writer.writeLEB(module.types.indexOf(instruction.getType()));
        writer.writeLEB(instruction.getSize());
    }

    @Override
    public void visit(WasmArrayGetInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xFB);
        if (instruction.getSignedType() == null) {
            writer.writeByte(11);
        } else {
            switch (instruction.getSignedType()) {
                case SIGNED:
                    writer.writeByte(12);
                    break;
                case UNSIGNED:
                    writer.writeByte(13);
                    break;
            }
        }
        writer.writeLEB(module.types.indexOf(instruction.getType()));
    }

    @Override
    public void visit(WasmArraySetInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xFB);
        writer.writeByte(14);
        writer.writeLEB(module.types.indexOf(instruction.getType()));
    }

    @Override
    public void visit(WasmArrayLengthInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xFB);
        writer.writeByte(15);
    }

    @Override
    public void visit(WasmArrayCopyInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xFB);
        writer.writeByte(17);
        writer.writeLEB(module.types.indexOf(instruction.getTargetArrayType()));
        writer.writeLEB(module.types.indexOf(instruction.getSourceArrayType()));
    }

    @Override
    public void visit(WasmFunctionReferenceInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xD2);
        writer.writeLEB(module.functions.indexOf(instruction.getFunction()));
    }

    @Override
    public void visit(WasmInt31ReferenceInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xFB);
        writer.writeByte(28);
    }

    @Override
    public void visit(WasmInt31GetInstruction instruction) {
        emitLocation(instruction);
        writer.writeByte(0xFB);
        writer.writeByte(instruction.getSignedType() == WasmSignedType.SIGNED ? 29 : 30);
    }

    private void writeBlockType(WasmBlockType type) {
        if (type == null) {
            writer.writeType(null, module);
        } else if (type instanceof WasmBlockType.Function) {
            var functionType = ((WasmBlockType.Function) type).ref;
            writer.writeSignedLEB(module.types.indexOf(functionType));
        } else {
            var valueType = ((WasmBlockType.Value) type).type;
            writer.writeType(valueType, module);
        }
    }

    private void writeLabel(WasmInstruction source, WasmInstructionList target) {
        var targetDepth = blockDepths.get(target.getBreakTarget());
        if (targetDepth == null) {
            var sb = new StringBuilder("Instruction (path to root):\n");
            var insn = source;
            while (insn != null) {
                sb.append("- ").append(insn).append("\n");
                insn = insn.getOwner().getBreakTarget();
            }
            sb.append("Target (path to root):\n");
            insn = target.getBreakTarget();
            while (insn != null) {
                sb.append("- ").append(insn).append("\n");
                insn = insn.getOwner().getBreakTarget();
            }
            throw new IllegalStateException("Invalid break target for instruction\n" + sb);
        }
        writer.writeLEB(depth - targetDepth);
    }

    private int alignment(int value) {
        return 31 - Integer.numberOfLeadingZeros(Math.max(1, value));
    }

    private void emitLocation(WasmInstruction instruction) {
        if (!Objects.equals(instruction.getLocation(), lastEmittedLocation)) {
            lastEmittedLocation = instruction.getLocation();
            doEmitLocation();
        }
    }

    private void doEmitLocation() {
        var address = writer.getPosition() + addressOffset;
        if (dwarfGenerator != null) {
            if (lastEmittedLocation == null || lastEmittedLocation.getFileName() == null) {
                dwarfGenerator.endLineNumberSequence(address);
            } else {
                dwarfGenerator.lineNumber(address, lastEmittedLocation.getFileName(), lastEmittedLocation.getLine());
            }
        }
        if (debugLines != null) {
            debugLines.advance(address);
            var loc = lastEmittedLocation;
            var inlining = loc != null ? loc.getInlining() : null;
            while (inlining != null) {
                currentMethodStack.add(inlining);
                inlining = inlining.getParent();
            }
            Collections.reverse(currentMethodStack);
            var commonPart = 0;
            while (commonPart < currentMethodStack.size() && commonPart < methodStack.size()
                    && currentMethodStack.get(commonPart) == methodStack.get(commonPart)) {
                ++commonPart;
            }
            while (methodStack.size() > commonPart) {
                debugLines.end();
                methodStack.remove(methodStack.size() - 1);
            }
            while (commonPart < currentMethodStack.size()) {
                var method = currentMethodStack.get(commonPart++);
                methodStack.add(method);
                debugLines.location(method.getFileName(), method.getLine());
                debugLines.start(method.getMethod());
            }
            currentMethodStack.clear();
            if (loc != null) {
                debugLines.location(loc.getFileName(), loc.getLine());
            } else {
                debugLines.emptyLocation();
            }
        }
    }
}
