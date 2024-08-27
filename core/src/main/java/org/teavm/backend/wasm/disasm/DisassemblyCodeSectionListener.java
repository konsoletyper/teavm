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
package org.teavm.backend.wasm.disasm;

import org.teavm.backend.wasm.model.WasmNumType;
import org.teavm.backend.wasm.model.expression.WasmFloatBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmFloatType;
import org.teavm.backend.wasm.model.expression.WasmFloatUnaryOperation;
import org.teavm.backend.wasm.model.expression.WasmInt32Subtype;
import org.teavm.backend.wasm.model.expression.WasmInt64Subtype;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.model.expression.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.expression.WasmSignedType;
import org.teavm.backend.wasm.parser.AddressListener;
import org.teavm.backend.wasm.parser.BranchOpcode;
import org.teavm.backend.wasm.parser.CodeListener;
import org.teavm.backend.wasm.parser.CodeSectionListener;
import org.teavm.backend.wasm.parser.LocalOpcode;
import org.teavm.backend.wasm.parser.Opcode;
import org.teavm.backend.wasm.parser.WasmHollowType;

public class DisassemblyCodeSectionListener extends BaseDisassemblyListener implements AddressListener,
        CodeSectionListener, CodeListener {
    private int blockIdGen;

    public DisassemblyCodeSectionListener(DisassemblyWriter writer) {
        super(writer);
    }

    @Override
    public boolean functionStart(int index, int size) {
        writer.address(address).write("(func (; " + index + " ;)").indent().eol();
        return true;
    }

    @Override
    public void localsStart(int count) {
        writer.address(address).write("(; locals " + count + " ;)").eol();
    }

    @Override
    public void local(int start, int count, WasmHollowType type) {
        writer.address(address);
        for (int i = 0; i < count; ++i) {
            writer.write("(local (; " + (i + start) + " ;) ");
            writeType(type);
            writer.write(")").eol();
        }
    }

    @Override
    public CodeListener code() {
        blockIdGen = 0;
        return this;
    }

    @Override
    public void functionEnd() {
        writer.outdent().write(")").eol();
    }

    @Override
    public void sectionEnd() {
        writer.outdent().write(")").eol();
    }


    @Override
    public void error(int depth) {
        writer.address(address);
        writer.write("error").eol();
        for (int i = 0; i < depth; ++i) {
            writer.outdent();
        }
    }

    @Override
    public int startBlock(boolean loop, WasmHollowType type) {
        writer.address(address);
        var label = blockIdGen++;
        writer.write(loop ? "loop" : "block").write(" $label_" + label);
        writeBlockType(type);
        writer.indent().eol();
        return label;
    }

    @Override
    public int startConditionalBlock(WasmHollowType type) {
        writer.address(address);
        var label = blockIdGen++;
        writer.write("if ").write(" $label_" + label);
        writeBlockType(type);
        writer.indent().eol();
        return label;
    }

    @Override
    public void startElseSection(int token) {
        writer.address(address);
        writer.outdent().write("else  (; $label_" + token + " ;)").indent().eol();
    }

    @Override
    public int startTry(WasmHollowType type) {
        writer.address(address);
        var label = blockIdGen++;
        writer.write("try ").write(" $label_" + label);
        writeBlockType(type);
        writer.indent().eol();
        return label;
    }

    @Override
    public void startCatch(int tagIndex) {
        writer.outdent().address(address);
        writer.write("catch ").write(String.valueOf(tagIndex)).indent().eol();
    }

    @Override
    public void endBlock(int token, boolean loop) {
        writer.address(address).outdent().write("end  (; $label_" + token + " ;)").eol();
    }

    @Override
    public void branch(BranchOpcode opcode, int depth, int target) {
        writer.address(address);
        switch (opcode) {
            case BR:
                writer.write("br");
                break;
            case BR_IF:
                writer.write("br_if");
                break;
        }
        writer.write(" $label_" + target).eol();
    }

    @Override
    public void tableBranch(int[] depths, int[] targets, int defaultDepth, int defaultTarget) {
        writer.address(address);
        writer.write("br_table");
        for (var target : targets) {
            writer.write(" $label_" + target);
        }
        writer.write(" $label_" + defaultTarget).eol();
    }

    @Override
    public void throwInstruction(int tagIndex) {
        writer.address(address);
        writer.write("throw ").write(String.valueOf(tagIndex)).eol();
    }

    @Override
    public void opcode(Opcode opcode) {
        writer.address(address);
        switch (opcode) {
            case UNREACHABLE:
                writer.write("unreachable");
                break;
            case NOP:
                writer.write("nop");
                break;
            case RETURN:
                writer.write("return");
                break;
            case DROP:
                writer.write("drop");
                break;
            case REF_EQ:
                writer.write("ref.eq");
                break;
            case ARRAY_LENGTH:
                writer.write("array.length");
                break;
        }
        writer.eol();
    }

    @Override
    public void local(LocalOpcode opcode, int index) {
        writer.address(address);
        switch (opcode) {
            case GET:
                writer.write("local.get");
                break;
            case SET:
                writer.write("local.set");
                break;
        }
        writer.write(" " + index).eol();
    }

    @Override
    public void getGlobal(int globalIndex) {
        writer.address(address).write("global.get ").write(Integer.toString(globalIndex)).eol();
    }

    @Override
    public void setGlobal(int globalIndex) {
        writer.address(address).write("global.set ").write(Integer.toString(globalIndex)).eol();
    }

    @Override
    public void call(int functionIndex) {
        writer.address(address);
        writer.write("call " + functionIndex).eol();
    }

    @Override
    public void indirectCall(int typeIndex, int tableIndex) {
        writer.address(address);
        writer.write("call_indirect " + tableIndex + " " + typeIndex).eol();
    }

    @Override
    public void callReference(int typeIndex) {
        writer.address(address);
        writer.write("call_ref " + typeIndex).eol();
    }

    @Override
    public void loadInt32(WasmInt32Subtype convertFrom, int align, int offset) {
        writer.address(address);
        var defaultAlign = 0;
        switch (convertFrom) {
            case INT8:
                writer.write("i32.load8_s");
                defaultAlign = 1;
                break;
            case UINT8:
                writer.write("i32.load8_u");
                defaultAlign = 1;
                break;
            case INT16:
                writer.write("i32.load16_s");
                defaultAlign = 2;
                break;
            case UINT16:
                writer.write("i32.load16_u");
                defaultAlign = 2;
                break;
            case INT32:
                writer.write("i32.load");
                defaultAlign = 4;
                break;
        }
        writeMemArg(align, defaultAlign, offset);
        writer.eol();
    }

    @Override
    public void storeInt32(WasmInt32Subtype convertTo, int align, int offset) {
        writer.address(address);
        var defaultAlign = 0;
        switch (convertTo) {
            case INT8:
            case UINT8:
                writer.write("i32.store8");
                defaultAlign = 1;
                break;
            case INT16:
            case UINT16:
                writer.write("i32.store16");
                defaultAlign = 2;
                break;
            case INT32:
                writer.write("i32.store");
                defaultAlign = 4;
                break;
        }
        writeMemArg(align, defaultAlign, offset);
        writer.eol();
    }

    @Override
    public void loadInt64(WasmInt64Subtype convertFrom, int align, int offset) {
        writer.address(address);
        var defaultAlign = 0;
        switch (convertFrom) {
            case INT8:
                writer.write("i64.load8_s");
                defaultAlign = 1;
                break;
            case UINT8:
                writer.write("i64.load8_u");
                defaultAlign = 1;
                break;
            case INT16:
                writer.write("i64.load16_s");
                defaultAlign = 2;
                break;
            case UINT16:
                writer.write("i64.load16_u");
                defaultAlign = 2;
                break;
            case INT32:
                writer.write("i64.load32_s");
                defaultAlign = 4;
                break;
            case UINT32:
                writer.write("i64.load32_u");
                defaultAlign = 4;
                break;
            case INT64:
                writer.write("i64.load");
                defaultAlign = 8;
                break;
        }
        writeMemArg(align, defaultAlign, offset);
        writer.eol();
    }

    @Override
    public void storeInt64(WasmInt64Subtype convertTo, int align, int offset) {
        writer.address(address);
        var defaultAlign = 0;
        switch (convertTo) {
            case INT8:
            case UINT8:
                writer.write("i64.store8");
                defaultAlign = 1;
                break;
            case INT16:
            case UINT16:
                writer.write("i64.store16");
                defaultAlign = 2;
                break;
            case INT32:
            case UINT32:
                writer.write("i64.store32");
                defaultAlign = 4;
                break;
            case INT64:
                writer.write("i64.store");
                defaultAlign = 8;
                break;
        }
        writeMemArg(align, defaultAlign, offset);
        writer.eol();
    }

    @Override
    public void loadFloat32(int align, int offset) {
        writer.address(address).write("f32.load");
        writeMemArg(align, 4, offset);
        writer.eol();
    }

    @Override
    public void storeFloat32(int align, int offset) {
        writer.address(address).write("f32.store");
        writeMemArg(align, 4, offset);
        writer.eol();
    }


    @Override
    public void loadFloat64(int align, int offset) {
        writer.address(address).write("f64.load");
        writeMemArg(align, 8, offset);
        writer.eol();
    }

    @Override
    public void storeFloat64(int align, int offset) {
        writer.address(address).write("f64.store");
        writeMemArg(align, 8, offset);
        writer.eol();
    }

    @Override
    public void memoryGrow() {
        writer.address(address).write("memory.grow").eol();
    }

    @Override
    public void memoryFill() {
        writer.address(address).write("memory.fill").eol();
    }

    @Override
    public void memoryCopy() {
        writer.address(address).write("memory.copy").eol();
    }

    private void writeMemArg(int align, int defaultAlign, int offset) {
        var needsComma = false;
        if (align != defaultAlign) {
            writer.write(" align=" + align);
            needsComma = true;
        }
        if (offset != 0) {
            if (needsComma) {
                writer.write(",");
            }
            writer.write(" offset=" + offset);
        }
    }

    @Override
    public void unary(WasmIntUnaryOperation opcode, WasmIntType type) {
        writer.address(address);
        switch (type) {
            case INT32:
                writer.write("i32.");
                break;
            case INT64:
                writer.write("i64.");
                break;
        }
        switch (opcode) {
            case CLZ:
                writer.write("clz");
                break;
            case CTZ:
                writer.write("ctz");
                break;
            case EQZ:
                writer.write("eqz");
                break;
            case POPCNT:
                writer.write("popcnt");
                break;
        }
        writer.eol();
    }

    @Override
    public void unary(WasmFloatUnaryOperation opcode, WasmFloatType type) {
        writer.address(address);
        switch (type) {
            case FLOAT32:
                writer.write("f32.");
                break;
            case FLOAT64:
                writer.write("f64.");
                break;
        }
        switch (opcode) {
            case ABS:
                writer.write("abs");
                break;
            case NEG:
                writer.write("neg");
                break;
            case FLOOR:
                writer.write("floor");
                break;
            case CEIL:
                writer.write("ceil");
                break;
            case TRUNC:
                writer.write("trunc");
                break;
            case NEAREST:
                writer.write("nearest");
                break;
            case SQRT:
                writer.write("sqrt");
                break;
            case COPYSIGN:
                writer.write("copysign");
                break;
        }
        writer.eol();
    }

    @Override
    public void binary(WasmIntBinaryOperation opcode, WasmIntType type) {
        writer.address(address);
        switch (type) {
            case INT32:
                writer.write("i32.");
                break;
            case INT64:
                writer.write("i64.");
                break;
        }
        switch (opcode) {
            case ADD:
                writer.write("add");
                break;
            case SUB:
                writer.write("sub");
                break;
            case MUL:
                writer.write("mul");
                break;
            case DIV_SIGNED:
                writer.write("div_s");
                break;
            case DIV_UNSIGNED:
                writer.write("div_u");
                break;
            case REM_SIGNED:
                writer.write("rem_s");
                break;
            case REM_UNSIGNED:
                writer.write("rem_u");
                break;
            case AND:
                writer.write("and");
                break;
            case OR:
                writer.write("or");
                break;
            case XOR:
                writer.write("xor");
                break;
            case SHL:
                writer.write("shl");
                break;
            case SHR_SIGNED:
                writer.write("shr_s");
                break;
            case SHR_UNSIGNED:
                writer.write("shr_u");
                break;
            case ROTL:
                writer.write("rotl");
                break;
            case ROTR:
                writer.write("rotr");
                break;
            case EQ:
                writer.write("eq");
                break;
            case NE:
                writer.write("ne");
                break;
            case LT_SIGNED:
                writer.write("lt_s");
                break;
            case LT_UNSIGNED:
                writer.write("lt_u");
                break;
            case GT_SIGNED:
                writer.write("gt_s");
                break;
            case GT_UNSIGNED:
                writer.write("gt_u");
                break;
            case LE_SIGNED:
                writer.write("le_s");
                break;
            case LE_UNSIGNED:
                writer.write("le_u");
                break;
            case GE_SIGNED:
                writer.write("ge_s");
                break;
            case GE_UNSIGNED:
                writer.write("ge_u");
                break;
        }
        writer.eol();
    }

    @Override
    public void binary(WasmFloatBinaryOperation opcode, WasmFloatType type) {
        writer.address(address);
        switch (type) {
            case FLOAT32:
                writer.write("f32.");
                break;
            case FLOAT64:
                writer.write("f64.");
                break;
        }
        switch (opcode) {
            case ADD:
                writer.write("add");
                break;
            case SUB:
                writer.write("sub");
                break;
            case MUL:
                writer.write("mul");
                break;
            case DIV:
                writer.write("div");
                break;
            case MIN:
                writer.write("min");
                break;
            case MAX:
                writer.write("max");
                break;
            case EQ:
                writer.write("eq");
                break;
            case NE:
                writer.write("ne");
                break;
            case LT:
                writer.write("lt");
                break;
            case GT:
                writer.write("gt");
                break;
            case LE:
                writer.write("le");
                break;
            case GE:
                writer.write("ge");
                break;
        }
        writer.eol();
    }

    @Override
    public void convert(WasmNumType sourceType, WasmNumType targetType, boolean signed, boolean reinterpret) {
        switch (targetType) {
            case INT32:
                writer.write("i32.");
                switch (sourceType) {
                    case FLOAT32:
                        if (reinterpret) {
                            writer.write("reinterpret_f32");
                        } else if (signed) {
                            writer.write("trunc_f32_s");
                        } else {
                            writer.write("trunc_f32_u");
                        }
                        break;
                    case FLOAT64:
                        if (signed) {
                            writer.write("trunc_f64_s");
                        } else {
                            writer.write("trunc_f64_u");
                        }
                        break;
                    case INT64:
                        writer.write("wrap_i64");
                        break;
                    default:
                        writer.write("error");
                        break;
                }
                break;
            case INT64:
                writer.write("i64.");
                switch (sourceType) {
                    case FLOAT32:
                        if (signed) {
                            writer.write("trunc_f32_s");
                        } else {
                            writer.write("trunc_f32_u");
                        }
                        break;
                    case FLOAT64:
                        if (reinterpret) {
                            writer.write("reinterpret_f64");
                        } else if (signed) {
                            writer.write("trunc_f64_s");
                        } else {
                            writer.write("trunc_f64_u");
                        }
                        break;
                    case INT32:
                        if (signed) {
                            writer.write("extend_i32_s");
                        } else {
                            writer.write("extend_i32_u");
                        }
                        break;
                    default:
                        writer.write("error");
                        break;
                }
                break;
            case FLOAT32:
                writer.write("f32.");
                switch (sourceType) {
                    case INT32:
                        if (reinterpret) {
                            writer.write("reinterpret_i32");
                        } else if (signed) {
                            writer.write("convert_i32_s");
                        } else {
                            writer.write("convert_i32_u");
                        }
                        break;
                    case INT64:
                        if (signed) {
                            writer.write("convert_i64_s");
                        } else {
                            writer.write("convert_i64_u");
                        }
                        break;
                    case FLOAT64:
                        writer.write("demote_f64");
                        break;
                    default:
                        writer.write("error");
                        break;
                }
                break;
            case FLOAT64:
                writer.write("f64.");
                switch (sourceType) {
                    case INT32:
                        if (signed) {
                            writer.write("convert_i32_s");
                        } else {
                            writer.write("convert_i32_u");
                        }
                        break;
                    case INT64:
                        if (reinterpret) {
                            writer.write("reinterpret_i64");
                        } else if (signed) {
                            writer.write("convert_i64_s");
                        } else {
                            writer.write("convert_i64_u");
                        }
                        break;
                    case FLOAT32:
                        writer.write("promote_f32");
                        break;
                    default:
                        writer.write("error");
                        break;
                }
                break;
        }
        writer.eol();
    }

    @Override
    public void int32Constant(int value) {
        writer.address(address).write("i32.const " + value).eol();
    }

    @Override
    public void int64Constant(long value) {
        writer.address(address).write("i64.const " + value).eol();
    }

    @Override
    public void float32Constant(float value) {
        writer.address(address).write("f32.const " + Float.toHexString(value)).eol();
    }

    @Override
    public void float64Constant(double value) {
        writer.address(address).write("f64.const " + Double.toHexString(value)).eol();
    }

    @Override
    public void nullConstant(WasmHollowType.Reference type) {
        writer.address(address).write("ref.null ");
        writeType(type);
        writer.eol();
    }

    @Override
    public void cast(WasmHollowType.Reference type, boolean nullable) {
        writer.address(address).write("ref.cast (ref ");
        if (!nullable) {
            writer.write("null ");
        }
        writeType(type);
        writer.write(")").eol();
    }

    @Override
    public void test(WasmHollowType.Reference type, boolean nullable) {
        writer.address(address).write("ref.test (ref ");
        if (!nullable) {
            writer.write("null ");
        }
        writeType(type);
        writer.write(")").eol();
    }

    @Override
    public void structNew(int typeIndex) {
        writer.address(address).write("struct.new ").write(Integer.toString(typeIndex)).eol();
    }

    @Override
    public void structNewDefault(int typeIndex) {
        writer.address(address).write("struct.new_default ").write(Integer.toString(typeIndex)).eol();
    }

    @Override
    public void structGet(WasmSignedType signedType, int typeIndex, int fieldIndex) {
        writer.address(address);
        if (signedType == null) {
            writer.write("struct.get");
        } else if (signedType == WasmSignedType.SIGNED) {
            writer.write("struct.get_s");
        } else {
            writer.write("struct.get_u");
        }
        writer.write(" ").write(Integer.toString(typeIndex)).write(" ").write(Integer.toString(fieldIndex)).eol();
    }

    @Override
    public void structSet(int typeIndex, int fieldIndex) {
        writer.address(address).write("struct.set ").write(Integer.toString(typeIndex)).write(" ")
                .write(Integer.toString(fieldIndex)).eol();
    }

    @Override
    public void arrayNewDefault(int typeIndex) {
        writer.address(address).write("array.new_default ").write(Integer.toString(typeIndex)).eol();
    }

    @Override
    public void arrayGet(WasmSignedType signedType, int typeIndex) {
        writer.address(address);
        if (signedType == null) {
            writer.write("array.get");
        } else if (signedType == WasmSignedType.SIGNED) {
            writer.write("array.get_s");
        } else {
            writer.write("array.get_u");
        }
        writer.write(" ").write(Integer.toString(typeIndex)).eol();
    }

    @Override
    public void arraySet(int typeIndex) {
        writer.address(address).write("array.set ").write(Integer.toString(typeIndex)).eol();
    }

    @Override
    public void functionReference(int functionIndex) {
        writer.address(address).write("ref.func ").write(Integer.toString(functionIndex)).eol();
    }

    @Override
    public void int31Reference() {
        writer.address(address).write("ref.i31").eol();
    }

    @Override
    public void int31Get(WasmSignedType signedType) {
        writer.address(address).write("ref.i31_").write(signedType == WasmSignedType.SIGNED ? "s" : "u").eol();
    }
}
