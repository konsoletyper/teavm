/*
 *  Copyright 2024 konsoletyper.
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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.function.Consumer;
import org.teavm.backend.wasm.parser.CodeSectionParser;
import org.teavm.backend.wasm.parser.ModuleParser;
import org.teavm.backend.wasm.parser.TypeSectionParser;
import org.teavm.common.AsyncInputStream;
import org.teavm.common.ByteArrayAsyncInputStream;

public final class Disassembler {
    private PrintWriter out;
    private DisassemblyWriter writer;

    public Disassembler(Writer writer) {
        out = new PrintWriter(writer);
        this.writer = new DisassemblyWriter(out, true);
    }

    public void startModule() {
        writer.write("(module").indent().eol();
    }

    public void endModule() {
        writer.write(")").eol();
    }

    public void disassemble(byte[] bytes) {
        startModule();
        read(bytes);
        endModule();
    }

    public void read(byte[] bytes) {
        var input = new ByteArrayAsyncInputStream(bytes);
        var parser = createParser(input);
        input.readFully(parser::parse);
    }

    public ModuleParser createParser(AsyncInputStream input) {
        return new ModuleParser(input) {
            @Override
            protected Consumer<byte[]> getSectionConsumer(int code, int pos, String name) {
                return Disassembler.this.getSectionConsumer(code, pos, name);
            }
        };
    }

    public Consumer<byte[]> getSectionConsumer(int code, int pos, String name) {
        if (code == 1) {
            return bytes -> {
                var disassembler = new DisassemblyTypeSectionListener(writer);
                disassembler.setAddressOffset(pos);
                var sectionParser = new TypeSectionParser(disassembler, disassembler);
                sectionParser.parse(bytes);
                out.flush();
            };
        } else if (code == 10) {
            return bytes -> {
                var disassembler = new DisassemblyCodeSectionListener(writer);
                disassembler.setAddressOffset(pos);
                var sectionParser = new CodeSectionParser(disassembler, disassembler);
                sectionParser.parse(bytes);
                out.flush();
            };
        } else {
            return null;
        }
    }

    public static void main(String[] args) throws IOException {
        var file = new File(args[0]);
        var bytes = Files.readAllBytes(file.toPath());
        var disassembler = new Disassembler(new OutputStreamWriter(System.out));
        disassembler.disassemble(bytes);
    }
}
