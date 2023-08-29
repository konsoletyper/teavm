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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.teavm.backend.wasm.debug.DebugLines;
import org.teavm.backend.wasm.debug.DebugVariables;
import org.teavm.backend.wasm.generate.DwarfClassGenerator;
import org.teavm.backend.wasm.generate.DwarfGenerator;
import org.teavm.backend.wasm.model.WasmCustomSection;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmMemorySegment;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmExpression;

public class WasmBinaryRenderer {
    private static final int SECTION_UNKNOWN = 0;
    private static final int SECTION_TYPE = 1;
    private static final int SECTION_IMPORT = 2;
    private static final int SECTION_FUNCTION = 3;
    private static final int SECTION_TABLE = 4;
    private static final int SECTION_MEMORY = 5;
    private static final int SECTION_EXPORT = 7;
    private static final int SECTION_START = 8;
    private static final int SECTION_ELEMENT = 9;
    private static final int SECTION_CODE = 10;
    private static final int SECTION_DATA = 11;

    private static final int EXTERNAL_KIND_FUNCTION = 0;
    private static final int EXTERNAL_KIND_MEMORY = 2;

    private WasmBinaryWriter output;
    private WasmBinaryVersion version;
    private List<WasmSignature> signatures = new ArrayList<>();
    private Map<WasmSignature, Integer> signatureIndexes = new HashMap<>();
    private Map<String, Integer> functionIndexes = new HashMap<>();
    private boolean obfuscated;
    private DwarfGenerator dwarfGenerator;
    private DwarfClassGenerator dwarfClassGen;
    private DebugLines debugLines;
    private DebugVariables debugVariables;
    private WasmBinaryStatsCollector statsCollector;

    public WasmBinaryRenderer(WasmBinaryWriter output, WasmBinaryVersion version, boolean obfuscated,
            DwarfGenerator dwarfGenerator, DwarfClassGenerator dwarfClassGen, DebugLines debugLines,
            DebugVariables debugVariables, WasmBinaryStatsCollector statsCollector) {
        this.output = output;
        this.version = version;
        this.obfuscated = obfuscated;
        this.dwarfGenerator = dwarfGenerator;
        this.dwarfClassGen = dwarfClassGen;
        this.debugLines = debugLines;
        this.debugVariables = debugVariables;
        this.statsCollector = statsCollector;
    }

    public void render(WasmModule module) {
        render(module, Collections::emptyList);
    }

    public void render(WasmModule module, Supplier<Collection<? extends WasmCustomSection>> customSectionSupplier) {
        output.writeInt32(0x6d736100);
        switch (version) {
            case V_0x1:
                output.writeInt32(0x01);
                break;
        }

        renderSignatures(module);
        renderImports(module);
        renderFunctions(module);
        renderTable(module);
        renderMemory(module);
        renderExport(module);
        renderStart(module);
        renderElement(module);
        renderCode(module);
        renderData(module);
        if (!obfuscated) {
            renderNames(module);
        }
        renderCustomSections(module, customSectionSupplier);
    }

    private void renderSignatures(WasmModule module) {
        WasmBinaryWriter section = new WasmBinaryWriter();
        WasmSignatureCollector signatureCollector = new WasmSignatureCollector(this::registerSignature);

        for (WasmFunction function : module.getFunctions().values()) {
            registerSignature(WasmSignature.fromFunction(function));
            for (WasmExpression part : function.getBody()) {
                part.acceptVisitor(signatureCollector);
            }
        }

        section.writeLEB(signatures.size());
        for (WasmSignature signature : signatures) {
            section.writeByte(0x60);
            section.writeLEB(signature.types.length - 1);
            for (int i = 1; i < signature.types.length; ++i) {
                section.writeType(signature.types[i], version);
            }
            if (signature.types[0] != null) {
                section.writeByte(1);
                section.writeType(signature.types[0], version);
            } else {
                section.writeByte(0);
            }
        }

        writeSection(SECTION_TYPE, "type", section.getData());
    }

    private void renderImports(WasmModule module) {
        List<WasmFunction> functions = new ArrayList<>();
        for (WasmFunction function : module.getFunctions().values()) {
            if (function.getImportName() == null) {
                continue;
            }
            functionIndexes.put(function.getName(), functions.size());
            functions.add(function);
        }
        if (functions.isEmpty()) {
            return;
        }

        WasmBinaryWriter section = new WasmBinaryWriter();

        section.writeLEB(functions.size());
        for (WasmFunction function : functions) {
            WasmSignature signature = WasmSignature.fromFunction(function);
            int signatureIndex = signatureIndexes.get(signature);

            String moduleName = function.getImportModule();
            if (moduleName == null) {
                moduleName = "";
            }
            section.writeAsciiString(moduleName);

            section.writeAsciiString(function.getImportName());

            section.writeByte(EXTERNAL_KIND_FUNCTION);
            section.writeLEB(signatureIndex);
        }

        writeSection(SECTION_IMPORT, "import", section.getData());
    }

    private void renderFunctions(WasmModule module) {
        WasmBinaryWriter section = new WasmBinaryWriter();

        List<WasmFunction> functions = module.getFunctions().values().stream()
                .filter(function -> function.getImportName() == null)
                .collect(Collectors.toList());
        for (WasmFunction function : functions) {
            functionIndexes.put(function.getName(), functionIndexes.size());
        }

        section.writeLEB(functions.size());
        for (WasmFunction function : functions) {
            WasmSignature signature = WasmSignature.fromFunction(function);
            section.writeLEB(signatureIndexes.get(signature));
        }

        writeSection(SECTION_FUNCTION, "function", section.getData());
    }

    private void renderTable(WasmModule module) {
        if (module.getFunctionTable().isEmpty()) {
            return;
        }

        WasmBinaryWriter section = new WasmBinaryWriter();

        section.writeByte(1);
        section.writeByte(0x70);
        section.writeByte(0);
        section.writeLEB(functionIndexes.size());

        writeSection(SECTION_TABLE, "table", section.getData());
    }

    private void renderMemory(WasmModule module) {
        WasmBinaryWriter section = new WasmBinaryWriter();

        section.writeByte(1);
        section.writeByte(1);
        section.writeLEB(module.getMinMemorySize());
        section.writeLEB(module.getMaxMemorySize());

        writeSection(SECTION_MEMORY, "memory", section.getData());
    }

    private void renderExport(WasmModule module) {

        // https://github.com/WebAssembly/design/blob/master/BinaryEncoding.md#export-section

        WasmBinaryWriter section = new WasmBinaryWriter();

        List<WasmFunction> functions = module.getFunctions().values().stream()
                .filter(function -> function.getExportName() != null)
                .collect(Collectors.toList());

        section.writeLEB(functions.size() + 1);
        for (WasmFunction function : functions) {
            int functionIndex = functionIndexes.get(function.getName());

            section.writeAsciiString(function.getExportName());

            section.writeByte(EXTERNAL_KIND_FUNCTION);
            section.writeLEB(functionIndex);
        }

        // We also need to export the memory to make it accessible
        section.writeAsciiString("memory");
        section.writeByte(EXTERNAL_KIND_MEMORY);
        section.writeLEB(0);

        writeSection(SECTION_EXPORT, "export", section.getData());
    }

    private void renderStart(WasmModule module) {
        if (module.getStartFunction() == null) {
            return;
        }

        WasmBinaryWriter section = new WasmBinaryWriter();
        section.writeLEB(functionIndexes.get(module.getStartFunction().getName()));

        writeSection(SECTION_START, "start", section.getData());
    }

    private void renderElement(WasmModule module) {
        if (module.getFunctionTable().isEmpty()) {
            return;
        }

        WasmBinaryWriter section = new WasmBinaryWriter();
        section.writeLEB(1);
        section.writeLEB(0);

        renderInitializer(section, 0);

        section.writeLEB(module.getFunctionTable().size());
        for (WasmFunction function : module.getFunctionTable()) {
            section.writeLEB(functionIndexes.get(function.getName()));
        }

        writeSection(SECTION_ELEMENT, "element", section.getData());
    }

    private void renderCode(WasmModule module) {
        var section = new WasmBinaryWriter();

        var functions = module.getFunctions().values().stream()
                .filter(function -> function.getImportName() == null)
                .collect(Collectors.toList());

        section.writeLEB(functions.size());
        for (var function : functions) {
            var body = renderFunction(function, section.getPosition() + 4);
            var startPos = section.getPosition();
            section.writeLEB4(body.length);
            section.writeBytes(body);
            var size = section.getPosition() - startPos;
            if (function.getJavaMethod() != null) {
                statsCollector.addClassCodeSize(function.getJavaMethod().getClassName(), size);
            }
        }

        if (dwarfGenerator != null) {
            dwarfGenerator.setCodeSize(section.getPosition());
        }

        writeSection(SECTION_CODE, "code", section.getData());
    }

    private byte[] renderFunction(WasmFunction function, int offset) {
        var code = new WasmBinaryWriter();

        var dwarfSubprogram = dwarfClassGen != null ? dwarfClassGen.getSubprogram(function.getName()) : null;
        if (dwarfSubprogram != null) {
            dwarfSubprogram.startOffset = offset - 4;
            dwarfSubprogram.function = function;
        }
        if (debugLines != null && function.getJavaMethod() != null) {
            debugLines.start(function.getJavaMethod());
        }

        var localVariables = function.getLocalVariables();
        int parameterCount = Math.min(function.getParameters().size(), localVariables.size());
        localVariables = localVariables.subList(parameterCount, localVariables.size());
        if (localVariables.isEmpty()) {
            code.writeLEB(0);
        } else {
            var localEntries = new ArrayList<LocalEntry>();
            var currentEntry = new LocalEntry(localVariables.get(0).getType());
            for (int i = 1; i < localVariables.size(); ++i) {
                var type = localVariables.get(i).getType();
                if (currentEntry.type == type) {
                    currentEntry.count++;
                } else {
                    localEntries.add(currentEntry);
                    currentEntry = new LocalEntry(type);
                }
            }
            localEntries.add(currentEntry);

            code.writeLEB(localEntries.size());
            for (var entry : localEntries) {
                code.writeLEB(entry.count);
                code.writeType(entry.type, version);
            }
        }

        var importIndexes = this.functionIndexes;
        var visitor = new WasmBinaryRenderingVisitor(code, version, functionIndexes, importIndexes,
                signatureIndexes, dwarfGenerator, function.getJavaMethod() != null ? debugLines : null, offset);
        for (var part : function.getBody()) {
            part.acceptVisitor(visitor);
        }
        visitor.endLocation();

        code.writeByte(0x0B);

        if (dwarfSubprogram != null) {
            dwarfSubprogram.endOffset = code.getPosition() + offset;
        }
        if (debugVariables != null) {
            writeDebugVariables(function, offset, code.getPosition());
        }

        return code.getData();
    }

    private void writeDebugVariables(WasmFunction function, int offset, int size) {
        debugVariables.startSequence(offset);
        for (var local : function.getLocalVariables()) {
            if (local.getName() != null && local.getJavaType() != null) {
                debugVariables.type(local.getName(), local.getJavaType());
                debugVariables.range(local.getName(), offset, offset + size, local.getIndex());
            }
        }
        debugVariables.endSequence();
    }

    private void renderInitializer(WasmBinaryWriter output, int value) {
        output.writeByte(0x41);
        output.writeLEB(value);
        output.writeByte(0x0B);
    }

    private void renderData(WasmModule module) {
        if (module.getSegments().isEmpty()) {
            return;
        }

        WasmBinaryWriter section = new WasmBinaryWriter();

        section.writeLEB(module.getSegments().size());
        for (WasmMemorySegment segment : module.getSegments()) {
            section.writeByte(0);
            renderInitializer(section, segment.getOffset());

            section.writeLEB(segment.getLength());
            int chunkSize = 65536;
            for (int i = 0; i < segment.getLength(); i += chunkSize) {
                int next = Math.min(i + chunkSize, segment.getLength());
                section.writeBytes(segment.getData(i, next - i));
            }
        }

        writeSection(SECTION_DATA, "data", section.getData());
    }

    private void renderNames(WasmModule module) {
        WasmBinaryWriter section = new WasmBinaryWriter();

        WasmBinaryWriter functionsSubsection = new WasmBinaryWriter();
        Collection<WasmFunction> functions = module.getFunctions().values();
        functions = functions.stream().filter(f -> f.getImportName() == null).collect(Collectors.toList());
        functionsSubsection.writeLEB(functions.size());

        for (WasmFunction function : functions) {
            functionsSubsection.writeLEB(functionIndexes.get(function.getName()));
            functionsSubsection.writeAsciiString(function.getName());
        }

        byte[] payload = functionsSubsection.getData();
        section.writeLEB(1);
        section.writeLEB(payload.length);
        section.writeBytes(payload);

        writeSection(SECTION_UNKNOWN, "name", section.getData());
    }

    private void renderCustomSections(WasmModule module,
            Supplier<Collection<? extends WasmCustomSection>> sectionSupplier) {
        for (var customSection : module.getCustomSections().values()) {
            renderCustomSection(customSection);
        }
        if (sectionSupplier != null) {
            for (var customSection : sectionSupplier.get()) {
                renderCustomSection(customSection);
            }
        }
    }

    private void renderCustomSection(WasmCustomSection customSection) {
        writeSection(SECTION_UNKNOWN, customSection.getName(), customSection.getData());
    }

    static class LocalEntry {
        WasmType type;
        int count = 1;

        LocalEntry(WasmType type) {
            this.type = type;
        }
    }

    private void registerSignature(WasmSignature signature) {
        signatureIndexes.computeIfAbsent(signature, key -> {
            int result = signatures.size();
            signatures.add(key);
            return result;
        });
    }

    private void writeSection(int id, String name, byte[] data) {
        var start = output.getPosition();
        output.writeByte(id);
        int length = data.length;
        if (id == 0) {
            length += name.length() + 1;
        }
        output.writeLEB(length);
        if (id == 0) {
            output.writeAsciiString(name);
        }

        output.writeBytes(data);

        statsCollector.addSectionSize(name, output.getPosition() - start);
    }
}
