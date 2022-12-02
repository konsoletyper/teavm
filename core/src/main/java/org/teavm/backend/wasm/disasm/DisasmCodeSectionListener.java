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
import org.teavm.backend.wasm.parser.CodeSectionListener;
import org.teavm.backend.wasm.parser.CodeSectionParser;
import org.teavm.backend.wasm.parser.ModuleParser;
import org.teavm.common.ByteArrayAsyncInputStream;

public class DisasmCodeSectionListener implements CodeSectionListener {
    private PrintWriter writer;

    public DisasmCodeSectionListener(PrintWriter writer) {
        this.writer = writer;
    }

    @Override
    public void address(int address) {
        writer.print("(; ");
        for (int i = 7; i >= 0; --i) {
            var digit = (address >>> (i * 4)) & 0xF;
            writer.print(Character.forDigit(digit, 16));
        }
        writer.print(" ;)");
        writer.println();
    }

    @Override
    public void sectionStart(int functionCount) {
        writer.println("  .code functions=" + functionCount);
    }

    @Override
    public boolean functionStart(int index, int size) {
        writer.println("  function fn_" + index);
        return true;
    }

    @Override
    public void localsStart(int count) {
        writer.println("  locals " + count);
    }

    @Override
    public void local(int start, int count, WasmType type) {
        for (int i = 0; i < count; ++i) {
            writer.println("  local " + (i + start) + ": " + type);
        }
    }

    @Override
    public void localsEnd() {
        writer.println("  end_locals");
    }

    @Override
    public void functionEnd() {
        writer.println("  end_function");
    }

    @Override
    public void sectionEnd() {
        writer.println("  end_code");
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
                        var writer = new PrintWriter(System.out);
                        var sectionParser = new CodeSectionParser(new DisasmCodeSectionListener(writer));
                        sectionParser.parse(bytes);
                        writer.flush();
                    };
                }
                return null;
            }
        };
        input.readFully(parser::parse);
    }
}
