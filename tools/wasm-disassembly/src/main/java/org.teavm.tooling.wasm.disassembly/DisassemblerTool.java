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
package org.teavm.tooling.wasm.disassembly;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.teavm.backend.wasm.disasm.Disassembler;
import org.teavm.backend.wasm.disasm.DisassemblyHTMLWriter;
import org.teavm.backend.wasm.disasm.DisassemblyWriter;
import org.teavm.jso.JSExport;

public class DisassemblerTool {
    private DisassemblerTool() {
    }

    @JSExport
    public static String disassemble(byte[] data) {
        var out = new StringWriter();
        var writer = new PrintWriter(out);
        var htmlWriter = new DisassemblyHTMLWriter(writer) {
            @Override
            public DisassemblyWriter prologue() {
                return this;
            }

            @Override
            public DisassemblyWriter epilogue() {
                return this;
            }
        };
        htmlWriter.setWithAddress(true);
        var disassembler = new Disassembler(htmlWriter);
        disassembler.disassemble(data);
        return out.toString();
    }
}
