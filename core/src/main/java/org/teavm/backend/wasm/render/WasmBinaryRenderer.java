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
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.teavm.backend.wasm.debug.DebugLines;
import org.teavm.backend.wasm.debug.DebugVariables;
import org.teavm.backend.wasm.generate.DwarfClassGenerator;
import org.teavm.backend.wasm.generate.DwarfGenerator;
import org.teavm.backend.wasm.model.WasmCustomSection;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmMemorySegment;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmType;

public class WasmBinaryRenderer {
    private static final int SECTION_UNKNOWN = 0;
    private static final int SECTION_TYPE = 1;
    private static final int SECTION_IMPORT = 2;
    private static final int SECTION_FUNCTION = 3;
    private static final int SECTION_TABLE = 4;
    private static final int SECTION_MEMORY = 5;
    private static final int SECTION_GLOBAL = 6;
    private static final int SECTION_EXPORT = 7;
    private static final int SECTION_START = 8;
    private static final int SECTION_ELEMENT = 9;
    private static final int SECTION_CODE = 10;
    private static final int SECTION_DATA = 11;
    private static final int SECTION_TAGS = 13;

    private static final int EXTERNAL_KIND_FUNCTION = 0;
    private static final int EXTERNAL_KIND_MEMORY = 2;
    private static final int EXTERNAL_KIND_GLOBAL = 3;
    private static final int EXTERNAL_KIND_TAG = 4;

    private WasmBinaryWriter output;
    private WasmBinaryVersion version;
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

        renderTypes(module);
        renderImports(module);
        renderFunctions(module);
        renderTable(module);
        if (module.memoryImportName == null) {
            renderMemory(module);
        }
        renderTags(module);
        renderGlobals(module);
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

    private void renderTypes(WasmModule module) {
        var section = new WasmBinaryWriter();

        var typeRenderer = new WasmCompositeTypeBinaryRenderer(module, section);
        var recTypeCount = 0;
        for (var i = 0; i < module.types.size();) {
            var type = module.types.get(i);
            if (type.getRecursiveTypeCount() > 0) {
                i += type.getRecursiveTypeCount();
            } else {
                ++i;
            }
            recTypeCount++;
        }
        section.writeLEB(recTypeCount);
        for (var i = 0; i < module.types.size();) {
            var type = module.types.get(i);
            if (type.getRecursiveTypeCount() > 0) {
                section.writeByte(0x4E);
                section.writeLEB(type.getRecursiveTypeCount());
                for (var j = 0; j < type.getRecursiveTypeCount(); ++j) {
                    var subtype = module.types.get(i++);
                    subtype.acceptVisitor(typeRenderer);
                }
            } else {
                type.acceptVisitor(typeRenderer);
                ++i;
            }
        }

        writeSection(SECTION_TYPE, "type", section.getData());
    }

    private void renderImports(WasmModule module) {
        var functions = new ArrayList<WasmFunction>();
        for (var function : module.functions) {
            if (function.getImportName() == null) {
                continue;
            }
            functions.add(function);
        }

        var globals = new ArrayList<WasmGlobal>();
        for (var global : module.globals) {
            if (global.getImportName() == null) {
                continue;
            }
            globals.add(global);
        }

        if (functions.isEmpty() && globals.isEmpty() && module.memoryImportName == null) {
            return;
        }

        WasmBinaryWriter section = new WasmBinaryWriter();

        var total = functions.size() + globals.size();
        if (module.memoryImportName != null) {
            ++total;
        }
        section.writeLEB(total);
        if (module.memoryImportName != null) {
            String moduleName = module.memoryImportModule;
            if (moduleName == null) {
                moduleName = "";
            }
            section.writeAsciiString(moduleName);
            section.writeAsciiString(module.memoryImportName);
            section.writeByte(EXTERNAL_KIND_MEMORY);
            section.writeByte(3);
            section.writeLEB(module.getMinMemorySize());
            section.writeLEB(module.getMaxMemorySize());
        }
        for (WasmFunction function : functions) {
            int signatureIndex = module.types.indexOf(function.getType());
            String moduleName = function.getImportModule();
            if (moduleName == null) {
                moduleName = "";
            }
            section.writeAsciiString(moduleName);
            section.writeAsciiString(function.getImportName());

            section.writeByte(EXTERNAL_KIND_FUNCTION);
            section.writeLEB(signatureIndex);
        }
        for (var global : globals) {
            var moduleName = global.getImportModule();
            if (moduleName == null) {
                moduleName = "";
            }
            section.writeAsciiString(moduleName);
            section.writeAsciiString(global.getImportName());
            section.writeByte(EXTERNAL_KIND_GLOBAL);
            section.writeType(global.getType(), module);
            section.writeByte(global.isImmutable() ? 0 : 1);
        }

        writeSection(SECTION_IMPORT, "import", section.getData());
    }

    private void renderFunctions(WasmModule module) {
        WasmBinaryWriter section = new WasmBinaryWriter();

        List<WasmFunction> functions = module.functions.stream()
                .filter(function -> function.getImportName() == null)
                .collect(Collectors.toList());

        section.writeLEB(functions.size());
        for (var function : functions) {
            section.writeLEB(module.types.indexOf(function.getType()));
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
        section.writeLEB(module.functions.size());

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

    private void renderGlobals(WasmModule module) {
        var globals = module.globals.stream()
                .filter(global -> global.getImportName() == null)
                .collect(Collectors.toList());
        if (globals.isEmpty()) {
            return;
        }

        var section = new WasmBinaryWriter();
        var visitor = new WasmBinaryRenderingVisitor(section, module, null, null, 0);
        section.writeLEB(globals.size());
        for (var global : globals) {
            section.writeType(global.getType(), module);
            section.writeByte(global.isImmutable() ? 0 : 1);
            global.getInitialValue().acceptVisitor(visitor);
            section.writeByte(0x0b);
        }

        writeSection(SECTION_GLOBAL, "global", section.getData());
    }

    private void renderExport(WasmModule module) {

        // https://github.com/WebAssembly/design/blob/master/BinaryEncoding.md#export-section

        List<WasmFunction> functions = module.functions.stream()
                .filter(function -> function.getExportName() != null)
                .collect(Collectors.toList());

        var tags = module.tags.stream()
                .filter(tag -> tag.getExportName() != null)
                .collect(Collectors.toList());

        var globals = module.globals.stream()
                .filter(global -> global.getExportName() != null)
                .collect(Collectors.toList());

        var total = functions.size() + tags.size() + globals.size();
        if (module.memoryExportName != null) {
            ++total;
        }
        if (total == 0) {
            return;
        }

        WasmBinaryWriter section = new WasmBinaryWriter();

        section.writeLEB(total);
        for (var function : functions) {
            int functionIndex = module.functions.indexOf(function);

            section.writeAsciiString(function.getExportName());

            section.writeByte(EXTERNAL_KIND_FUNCTION);
            section.writeLEB(functionIndex);
        }
        for (var tag : tags) {
            var tagIndex = module.tags.indexOf(tag);
            section.writeAsciiString(tag.getExportName());

            section.writeByte(EXTERNAL_KIND_TAG);
            section.writeLEB(tagIndex);
        }
        for (var global : globals) {
            var index = module.globals.indexOf(global);
            section.writeAsciiString(global.getExportName());

            section.writeByte(EXTERNAL_KIND_GLOBAL);
            section.writeLEB(index);
        }

        // We also need to export the memory to make it accessible
        if (module.memoryExportName != null) {
            section.writeAsciiString(module.memoryExportName);
            section.writeByte(EXTERNAL_KIND_MEMORY);
            section.writeLEB(0);
        }

        writeSection(SECTION_EXPORT, "export", section.getData());
    }

    private void renderStart(WasmModule module) {
        if (module.getStartFunction() == null) {
            return;
        }

        WasmBinaryWriter section = new WasmBinaryWriter();
        section.writeLEB(module.functions.indexOf(module.getStartFunction()));

        writeSection(SECTION_START, "start", section.getData());
    }

    private void renderElement(WasmModule module) {
        var count = 0;
        if (!module.getFunctionTable().isEmpty()) {
            ++count;
        }
        if (module.functions.stream().anyMatch(WasmFunction::isReferenced)) {
            ++count;
        }
        if (count == 0) {
            return;
        }

        WasmBinaryWriter section = new WasmBinaryWriter();
        section.writeLEB(count);

        if (!module.getFunctionTable().isEmpty()) {
            section.writeLEB(0);
            renderInitializer(section, 0);
            section.writeLEB(module.getFunctionTable().size());
            for (var function : module.getFunctionTable()) {
                section.writeLEB(module.functions.indexOf(function));
            }
        }

        var referencedFunctions = module.functions.stream()
                .filter(WasmFunction::isReferenced)
                .collect(Collectors.toList());
        if (!referencedFunctions.isEmpty()) {
            section.writeLEB(3);
            section.writeByte(0);
            section.writeLEB(referencedFunctions.size());
            for (var function : referencedFunctions) {
                section.writeLEB(module.functions.indexOf(function));
            }
        }

        writeSection(SECTION_ELEMENT, "element", section.getData());
    }

    private void renderCode(WasmModule module) {
        var section = new WasmBinaryWriter();

        var functions = module.functions.stream()
                .filter(function -> function.getImportName() == null)
                .collect(Collectors.toList());

        section.writeLEB(functions.size());
        var sectionOffset = output.getPosition() + 4;
        for (var function : functions) {
            var body = renderFunction(module, function, section.getPosition() + 4, sectionOffset);
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

        writeSection(SECTION_CODE, "code", section.getData(), true);
    }

    private byte[] renderFunction(WasmModule module, WasmFunction function, int offset, int sectionOffset) {
        var code = new WasmBinaryWriter();

        var dwarfSubprogram = dwarfClassGen != null ? dwarfClassGen.getSubprogram(function.getName()) : null;
        if (dwarfSubprogram != null) {
            dwarfSubprogram.startOffset = offset - 4;
            dwarfSubprogram.function = function;
        }
        if (debugLines != null && function.getJavaMethod() != null) {
            debugLines.advance(offset + sectionOffset);
            debugLines.start(function.getJavaMethod());
        }

        var localVariables = function.getLocalVariables();
        int parameterCount = Math.min(function.getType().getParameterTypes().size(), localVariables.size());
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
                code.writeType(entry.type, module);
            }
        }

        var visitor = new WasmBinaryRenderingVisitor(code, module, dwarfGenerator,
                function.getJavaMethod() != null ? debugLines : null, offset + sectionOffset);
        for (var part : function.getBody()) {
            visitor.preprocess(part);
        }
        visitor.setPositionToEmit(code.getPosition());
        for (var i = 0; i < function.getBody().size(); ++i) {
            var part = function.getBody().get(i);
            if (i == function.getBody().size() - 1) {
                visitor.pushLocation(part);
            }
            part.acceptVisitor(visitor);
        }

        code.writeByte(0x0B);
        if (!function.getBody().isEmpty()) {
            visitor.popLocation();
        }
        visitor.endLocation();

        if (dwarfSubprogram != null) {
            dwarfSubprogram.endOffset = code.getPosition() + offset;
        }
        if (debugVariables != null) {
            writeDebugVariables(function, offset + sectionOffset, code.getPosition());
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

    private void renderTags(WasmModule module) {
        if (module.tags.isEmpty()) {
            return;
        }

        var section = new WasmBinaryWriter();
        section.writeLEB(module.tags.size());
        for (var tag : module.tags) {
            section.writeByte(0);
            section.writeLEB(module.types.indexOf(tag.getType()));
        }

        writeSection(SECTION_TAGS, "tags", section.getData());
    }

    private void renderNames(WasmModule module) {
        WasmBinaryWriter section = new WasmBinaryWriter();

        WasmBinaryWriter functionsSubsection = new WasmBinaryWriter();
        var functions = module.functions.stream()
                .filter(f -> f.getName() != null)
                .collect(Collectors.toList());
        functionsSubsection.writeLEB(functions.size());

        for (WasmFunction function : functions) {
            functionsSubsection.writeLEB(module.functions.indexOf(function));
            functionsSubsection.writeAsciiString(function.getName());
        }

        byte[] payload = functionsSubsection.getData();
        section.writeLEB(1);
        section.writeLEB(payload.length);
        section.writeBytes(payload);

        var functionsWithLocalNames = module.functions.stream()
                .filter(fn -> fn.getLocalVariables().stream().anyMatch(v -> v.getName() != null))
                .collect(Collectors.toList());
        if (!functionsWithLocalNames.isEmpty()) {
            var subsection = new WasmBinaryWriter();
            subsection.writeLEB(functionsWithLocalNames.size());
            for (var function : functionsWithLocalNames) {
                subsection.writeLEB(module.functions.indexOf(function));
                var locals = function.getLocalVariables().stream()
                        .filter(t -> t.getName() != null)
                        .collect(Collectors.toList());
                subsection.writeLEB(locals.size());
                for (var local : locals) {
                    subsection.writeLEB(local.getIndex());
                    subsection.writeAsciiString(local.getName());
                }
            }

            payload = subsection.getData();
            section.writeLEB(2);
            section.writeLEB(payload.length);
            section.writeBytes(payload);
        }

        var types = module.types.stream()
                .filter(t -> t.getName() != null)
                .collect(Collectors.toList());
        if (!types.isEmpty()) {
            var typesSubsection = new WasmBinaryWriter();
            typesSubsection.writeLEB(types.size());
            for (var type : types) {
                typesSubsection.writeLEB(module.types.indexOf(type));
                typesSubsection.writeAsciiString(type.getName());
            }

            payload = typesSubsection.getData();
            section.writeLEB(4);
            section.writeLEB(payload.length);
            section.writeBytes(payload);
        }

        var globals = module.globals.stream()
                .filter(g -> g.getName() != null)
                .collect(Collectors.toList());
        if (!globals.isEmpty()) {
            var globalsSubsection = new WasmBinaryWriter();
            globalsSubsection.writeLEB(globals.size());
            for (var global : globals) {
                globalsSubsection.writeLEB(module.globals.indexOf(global));
                globalsSubsection.writeAsciiString(global.getName());
            }

            payload = globalsSubsection.getData();
            section.writeLEB(7);
            section.writeLEB(payload.length);
            section.writeBytes(payload);
        }

        var typesWithNamedFields = module.types.stream()
                .filter(t -> t instanceof WasmStructure)
                .filter(t -> ((WasmStructure) t).getFields().stream().anyMatch(f -> f.getName() != null))
                .collect(Collectors.toList());
        if (!typesWithNamedFields.isEmpty()) {
            var subsection = new WasmBinaryWriter();
            subsection.writeLEB(typesWithNamedFields.size());
            for (var type : typesWithNamedFields) {
                subsection.writeLEB(module.types.indexOf(type));
                var fields = ((WasmStructure) type).getFields().stream()
                        .filter(t -> t.getName() != null)
                        .collect(Collectors.toList());
                subsection.writeLEB(fields.size());
                for (var field : fields) {
                    subsection.writeLEB(field.getIndex());
                    subsection.writeAsciiString(field.getName());
                }
            }

            payload = subsection.getData();
            section.writeLEB(10);
            section.writeLEB(payload.length);
            section.writeBytes(payload);
        }

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

    private void writeSection(int id, String name, byte[] data) {
        writeSection(id, name, data, false);
    }

    private void writeSection(int id, String name, byte[] data, boolean constantSizeLength) {
        var start = output.getPosition();
        output.writeByte(id);
        int length = data.length;
        if (id == 0) {
            length += name.length() + 1;
        }
        if (constantSizeLength) {
            output.writeLEB4(length);
        } else {
            output.writeLEB(length);
        }
        if (id == 0) {
            output.writeAsciiString(name);
        }

        output.writeBytes(data);

        statsCollector.addSectionSize(name, output.getPosition() - start);
    }
}
