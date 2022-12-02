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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.function.Consumer;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmFloatBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmFloatType;
import org.teavm.backend.wasm.model.expression.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.expression.WasmIntType;
import org.teavm.backend.wasm.parser.AddressListener;
import org.teavm.backend.wasm.parser.BranchOpcode;
import org.teavm.backend.wasm.parser.CodeListener;
import org.teavm.backend.wasm.parser.CodeSectionListener;
import org.teavm.backend.wasm.parser.CodeSectionParser;
import org.teavm.backend.wasm.parser.LocalOpcode;
import org.teavm.backend.wasm.parser.ModuleParser;
import org.teavm.backend.wasm.parser.Opcode;
import org.teavm.common.ByteArrayAsyncInputStream;

public class DisassemblyCodeSectionListener implements AddressListener, CodeSectionListener, CodeListener {
    private DisassemblyWriter writer;
    private int address;
    private int blockIdGen;

    public DisassemblyCodeSectionListener(DisassemblyWriter writer) {
        this.writer = writer;
    }

    @Override
    public void address(int address) {
        this.address = address;
    }

    @Override
    public void sectionStart(int functionCount) {
        writer.address(address).write("(; code section ;)").eol();
    }

    @Override
    public boolean functionStart(int index, int size) {
        writer.address(address).write("(func $fun_" + index).indent().eol();
        return true;
    }

    @Override
    public void localsStart(int count) {
        writer.address(address).write("(; locals " + count + " ;)").eol();
    }

    @Override
    public void local(int start, int count, WasmType type) {
        writer.address(address);
        for (int i = 0; i < count; ++i) {
            writer.write("(local $loc_" + (i + start) + " " + typeToString(type) + ")").eol();
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

    private String blockTypeToString(WasmType type) {
        if (type == null) {
            return "";
        } else {
            return " " + typeToString(type);
        }
    }

    private String typeToString(WasmType type) {
        if (type != null) {
            switch (type) {
                case INT32:
                    return "i32";
                case INT64:
                    return "i64";
                case FLOAT32:
                    return "f32";
                case FLOAT64:
                    return "f64";
                default:
                    break;
            }
        }
        return "unknown";
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
    public int startBlock(boolean loop, WasmType type) {
        writer.address(address);
        var label = blockIdGen++;
        writer.write(loop ? "loop" : "block").write(" $label_" + label).write(blockTypeToString(type))
                .indent().eol();
        return label;
    }

    @Override
    public int startConditionalBlock(WasmType type) {
        writer.address(address);
        var label = blockIdGen++;
        writer.write("if ").write(" $label_" + label).write(blockTypeToString(type)).indent().eol();
        return label;
    }

    @Override
    public void startElseSection(int token) {
        writer.address(address);
        writer.outdent().write("else  (; $label_" + token + " ;)").indent().eol();
    }

    @Override
    public void endBlock(int token) {
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
    public void opcode(Opcode opcode) {
        writer.address(address);
        switch (opcode) {
            case UNREACHABLE:
                writer.write("unreachable");
                break;
            case RETURN:
                writer.write("return");
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
        writer.write(" $loc_" + index).eol();
    }

    @Override
    public void binary(WasmIntBinaryOperation opcode, WasmIntType type) {

    }

    @Override
    public void binary(WasmFloatBinaryOperation opcode, WasmFloatType type) {

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

    public static void main(String[] args) throws IOException {
        var file = new File(args[0]);
        var bytes = Files.readAllBytes(file.toPath());
        var input = new ByteArrayAsyncInputStream(bytes);
        var parser = new ModuleParser(input) {
            @Override
            protected Consumer<byte[]> getSectionConsumer(int code, int pos, String name) {
                if (code == 10) {
                    return bytes -> {
                        var out = new PrintWriter(System.out);
                        var writer = new DisassemblyWriter(out, true);
                        var disassembler = new DisassemblyCodeSectionListener(writer);
                        var sectionParser = new CodeSectionParser(disassembler, disassembler);
                        sectionParser.parse(bytes);
                        out.flush();
                    };
                }
                return null;
            }
        };
        input.readFully(parser::parse);
    }
}
