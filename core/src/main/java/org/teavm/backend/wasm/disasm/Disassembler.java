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

import com.carrotsearch.hppc.IntArrayList;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import org.teavm.backend.wasm.debug.info.LineInfo;
import org.teavm.backend.wasm.debug.parser.DebugClassParser;
import org.teavm.backend.wasm.debug.parser.DebugFileParser;
import org.teavm.backend.wasm.debug.parser.DebugLinesParser;
import org.teavm.backend.wasm.debug.parser.DebugMethodParser;
import org.teavm.backend.wasm.debug.parser.DebugPackageParser;
import org.teavm.backend.wasm.debug.parser.DebugSectionParser;
import org.teavm.backend.wasm.debug.parser.DebugStringParser;
import org.teavm.backend.wasm.parser.AddressListener;
import org.teavm.backend.wasm.parser.CodeSectionParser;
import org.teavm.backend.wasm.parser.FunctionSectionListener;
import org.teavm.backend.wasm.parser.FunctionSectionParser;
import org.teavm.backend.wasm.parser.GlobalSectionParser;
import org.teavm.backend.wasm.parser.ImportSectionParser;
import org.teavm.backend.wasm.parser.ModuleParser;
import org.teavm.backend.wasm.parser.NameSectionListener;
import org.teavm.backend.wasm.parser.NameSectionParser;
import org.teavm.backend.wasm.parser.TypeSectionParser;
import org.teavm.backend.wasm.parser.WasmHollowFunctionType;
import org.teavm.common.AsyncInputStream;
import org.teavm.common.ByteArrayAsyncInputStream;

public final class Disassembler {
    private DisassemblyWriter writer;
    private WasmHollowFunctionType[] functionTypes;
    private int[] functionTypeRefs;
    private int importFunctionCount;
    private int importGlobalCount;
    private Map<String, DebugSectionParser> debugSectionParsers = new HashMap<>();
    private DebugLinesParser debugLines;
    private LineInfo lineInfo;

    public Disassembler(DisassemblyWriter writer) {
        this.writer = writer;
        installDebugParsers();
    }

    private void installDebugParsers() {
        var strings = addDebugSection(new DebugStringParser());
        var files = addDebugSection(new DebugFileParser(strings));
        var packages = addDebugSection(new DebugPackageParser(strings));
        var classes = addDebugSection(new DebugClassParser(strings, packages));
        var methods = addDebugSection(new DebugMethodParser(strings, classes));
        debugLines = addDebugSection(new DebugLinesParser(files, methods));
    }

    private <T extends DebugSectionParser> T addDebugSection(T section) {
        debugSectionParsers.put(section.name(), section);
        return section;
    }

    public void startModule() {
        writer.write("(module").indent().eol();
    }

    public void endModule() {
        writer.write(")").eol();
    }

    public void disassemble(byte[] bytes) {
        writer.prologue();
        startModule();
        read(bytes);
        endModule();
        writer.epilogue();
    }

    public void read(byte[] bytes) {
        var nameAccumulator = new NameAccumulatingSectionListener();
        var input = new ByteArrayAsyncInputStream(bytes);
        var preparationParser = createPreparationParser(input, nameAccumulator);
        input.readFully(preparationParser::parse);
        lineInfo = debugLines.getLineInfo();

        input = new ByteArrayAsyncInputStream(bytes);
        var parser = createParser(input, nameAccumulator.buildProvider());
        input.readFully(parser::parse);
    }

    public ModuleParser createPreparationParser(AsyncInputStream input, NameSectionListener listener) {
        return new ModuleParser(input) {
            @Override
            protected Consumer<byte[]> getSectionConsumer(int code, int pos, String name) {
                if (code == 0) {
                    var debugSection = debugSectionParsers.get(name);
                    if (debugSection != null) {
                        return debugSection::parse;
                    }
                }
                return Disassembler.this.getNameSectionConsumer(code, name, listener);
            }
        };
    }

    public ModuleParser createParser(AsyncInputStream input, NameProvider nameProvider) {
        return new ModuleParser(input) {
            @Override
            protected Consumer<byte[]> getSectionConsumer(int code, int pos, String name) {
                return Disassembler.this.getSectionConsumer(code, pos, nameProvider);
            }
        };
    }

    public Consumer<byte[]> getSectionConsumer(int code, int pos, NameProvider nameProvider) {
        if (code == 1) {
            return bytes -> {
                writer.write("(; type section size: " + bytes.length + " ;)").eol();
                var typeWriter = new DisassemblyTypeSectionListener(writer, nameProvider);
                writer.setAddressOffset(pos);
                var sectionParser = new TypeSectionParser(typeWriter);
                sectionParser.parse(writer.addressListener, bytes);
                functionTypes = typeWriter.getFunctionTypes();
                writer.flush();
            };
        } else if (code == 2) {
            return bytes -> {
                var importListener = new DisassemblyImportSectionListener(writer, nameProvider, functionTypes);
                var parser = new ImportSectionParser(importListener);
                parser.parse(AddressListener.EMPTY, bytes);
                importFunctionCount = importListener.functionCount();
                importGlobalCount = importListener.globalCount();
            };
        } else if (code == 3) {
            return bytes -> {
                var signatures = new IntArrayList();
                for (var i = 0; i < importFunctionCount; ++i) {
                    signatures.add(0);
                }
                var parser = new FunctionSectionParser(new FunctionSectionListener() {
                    @Override
                    public void function(int index, int typeIndex) {
                        signatures.add(typeIndex);
                    }
                });
                parser.parse(AddressListener.EMPTY, bytes);
                functionTypeRefs = signatures.toArray();
            };
        } else if (code == 6) {
            return bytes -> {
                writer.write("(; global section size: " + bytes.length + " ;)").eol();
                var globalWriter = new DisassemblyGlobalSectionListener(writer, nameProvider);
                writer.setAddressOffset(pos);
                var sectionParser = new GlobalSectionParser(globalWriter);
                sectionParser.setGlobalIndexOffset(importGlobalCount);
                sectionParser.parse(writer.addressListener, bytes);
                writer.flush();
            };
        } else if (code == 10) {
            return bytes -> {
                var disassembler = new DisassemblyCodeSectionListener(writer, nameProvider);
                disassembler.setFunctionTypes(functionTypes);
                disassembler.setFunctionTypeRefs(functionTypeRefs);
                writer.setAddressOffset(pos);
                writer.setDebugLines(lineInfo);
                writer.startSection();
                writer.write("(; code section size: " + bytes.length + " ;)").eol();
                var sectionParser = new CodeSectionParser(disassembler);
                sectionParser.setFunctionIndexOffset(importFunctionCount);
                sectionParser.parse(writer.addressListener, bytes);
                writer.flush();
                writer.setDebugLines(null);
            };
        } else {
            return null;
        }
    }

    public Consumer<byte[]> getNameSectionConsumer(int code, String name, NameSectionListener listener) {
        if (code == 0 && name.equals("name")) {
            return bytes -> {
                var parser = new NameSectionParser(listener);
                parser.parse(AddressListener.EMPTY, bytes);
            };
        } else {
            return null;
        }
    }

    public static void main(String[] args) throws IOException {
        String fileName = null;
        String outFileName = null;
        var htmlMode = false;
        for (var i = 0; i < args.length; i++) {
            var arg = args[i];
            if (arg.equals("--html")) {
                htmlMode = true;
            } else if (arg.equals("--output") || arg.equals("-o")) {
                outFileName = args[++i];
            } else {
                fileName = arg;
            }
        }
        var file = new File(fileName);
        var bytes = Files.readAllBytes(file.toPath());
        var output = outFileName != null ? new FileOutputStream(outFileName) : System.out;
        var writer = new PrintWriter(output);
        var disassemblyWriter = htmlMode
                ? new DisassemblyHTMLWriter(writer)
                : new DisassemblyTextWriter(writer);
        disassemblyWriter.setWithAddress(true);
        if (htmlMode) {
            disassemblyWriter.write("<html><body><pre>").eol();
        }
        var disassembler = new Disassembler(disassemblyWriter);
        disassembler.disassemble(bytes);
        if (htmlMode) {
            disassemblyWriter.write("</pre></body></html>").eol();
        }
    }
}
