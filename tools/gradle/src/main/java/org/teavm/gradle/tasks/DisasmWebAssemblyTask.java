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
package org.teavm.gradle.tasks;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.teavm.backend.wasm.disasm.Disassembler;
import org.teavm.backend.wasm.disasm.DisassemblyHTMLWriter;
import org.teavm.backend.wasm.disasm.DisassemblyTextWriter;

public abstract class DisasmWebAssemblyTask extends DefaultTask {
    public DisasmWebAssemblyTask() {
        getHtml().convention(false);
    }

    @InputFile
    public abstract RegularFileProperty getInputFile();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @Input
    public abstract Property<Boolean> getHtml();

    @TaskAction
    public void disassemble() throws IOException {
        var file = getInputFile().get().getAsFile();
        var bytes = Files.readAllBytes(file.toPath());
        var output = new FileOutputStream(getOutputFile().get().getAsFile());
        var writer = new PrintWriter(output);
        var html = getHtml().get();
        var disassemblyWriter = html
                ? new DisassemblyHTMLWriter(writer)
                : new DisassemblyTextWriter(writer);
        disassemblyWriter.setWithAddress(true);
        if (html) {
            disassemblyWriter.write("<html><body><pre>").eol();
        }
        var disassembler = new Disassembler(disassemblyWriter);
        disassembler.disassemble(bytes);
        if (html) {
            disassemblyWriter.write("</pre></body></html>").eol();
        }
    }
}
