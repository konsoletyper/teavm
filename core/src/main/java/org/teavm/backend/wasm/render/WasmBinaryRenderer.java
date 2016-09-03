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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmMemorySegment;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.expression.WasmExpression;

public class WasmBinaryRenderer {
    private WasmBinaryWriter output;
    private List<WasmSignature> signatures = new ArrayList<>();
    private Map<WasmSignature, Integer> signatureIndexes = new HashMap<>();
    private Map<String, Integer> importIndexes = new HashMap<>();
    private Map<String, Integer> functionIndexes = new HashMap<>();

    public WasmBinaryRenderer(WasmBinaryWriter output) {
        this.output = output;
    }

    public void render(WasmModule module) {
        output.writeInt32(0x6d736100);
        output.writeInt32(11);
        renderSignatures(module);
        renderImports(module);
        renderFunctions(module);
        renderTable(module);
        renderMemory(module);
        renderExport(module);
        renderStart(module);
        renderCode(module);
        renderData(module);
        renderNames(module);
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
            section.writeByte(0x40);
            section.writeLEB(signature.types.length - 1);
            for (int i = 1; i < signature.types.length; ++i) {
                section.writeType(signature.types[i]);
            }
            if (signature.types[0] != null) {
                section.writeByte(1);
                section.writeType(signature.types[0]);
            } else {
                section.writeByte(0);
            }
        }

        writeSection("type", section.getData());
    }

    private void renderImports(WasmModule module) {
        int index = 0;
        List<WasmFunction> functions = new ArrayList<>();
        for (WasmFunction function : module.getFunctions().values()) {
            if (function.getImportName() == null) {
                continue;
            }
            functions.add(function);
            importIndexes.put(function.getName(), index++);
        }
        if (functions.isEmpty()) {
            return;
        }

        WasmBinaryWriter section = new WasmBinaryWriter();

        section.writeLEB(functions.size());
        for (WasmFunction function : functions) {
            WasmSignature signature = WasmSignature.fromFunction(function);
            section.writeLEB(signatureIndexes.get(signature));

            String moduleName = function.getImportModule();
            if (moduleName == null) {
                moduleName = "";
            }
            section.writeAsciiString(moduleName);

            section.writeAsciiString(function.getImportName());
        }

        writeSection("import", section.getData());
    }

    private void renderFunctions(WasmModule module) {
        WasmBinaryWriter section = new WasmBinaryWriter();

        int index = 0;
        List<WasmFunction> functions = module.getFunctions().values().stream()
                .filter(function -> function.getImportName() == null)
                .collect(Collectors.toList());
        for (WasmFunction function : functions) {
            functionIndexes.put(function.getName(), index++);
        }

        section.writeLEB(functions.size());
        for (WasmFunction function : functions) {
            WasmSignature signature = WasmSignature.fromFunction(function);
            section.writeLEB(signatureIndexes.get(signature));
        }

        writeSection("function", section.getData());
    }

    private void renderTable(WasmModule module) {
        if (module.getFunctionTable().isEmpty()) {
            return;
        }

        WasmBinaryWriter section = new WasmBinaryWriter();

        section.writeLEB(module.getFunctionTable().size());
        for (WasmFunction function : module.getFunctionTable()) {
            section.writeLEB(functionIndexes.get(function.getName()));
        }

        writeSection("table", section.getData());
    }

    private void renderMemory(WasmModule module) {
        WasmBinaryWriter section = new WasmBinaryWriter();

        section.writeLEB(module.getMemorySize());
        section.writeLEB(module.getMemorySize());
        section.writeByte(0);

        writeSection("memory", section.getData());
    }

    private void renderExport(WasmModule module) {
        List<WasmFunction> functions = module.getFunctions().values().stream()
                .filter(function -> function.getExportName() != null)
                .collect(Collectors.toList());
        if (functions.isEmpty()) {
            return;
        }

        WasmBinaryWriter section = new WasmBinaryWriter();

        section.writeLEB(functions.size());
        for (WasmFunction function : functions) {
            section.writeLEB(functionIndexes.get(function.getName()));

            section.writeAsciiString(function.getExportName());
        }

        writeSection("export", section.getData());
    }

    private void renderStart(WasmModule module) {
        if (module.getStartFunction() == null) {
            return;
        }

        WasmBinaryWriter section = new WasmBinaryWriter();
        section.writeLEB(functionIndexes.get(module.getStartFunction().getName()));

        writeSection("start", section.getData());
    }

    private void renderCode(WasmModule module) {
        WasmBinaryWriter section = new WasmBinaryWriter();

        List<WasmFunction> functions = module.getFunctions().values().stream()
                .filter(function -> function.getImportName() == null)
                .collect(Collectors.toList());

        section.writeLEB(functions.size());
        for (WasmFunction function : functions) {
            List<WasmLocal> localVariables = function.getLocalVariables();
            localVariables = localVariables.subList(function.getParameters().size(), localVariables.size());
            if (localVariables.isEmpty()) {
                section.writeLEB(0);
            } else {
                List<LocalEntry> localEntries = new ArrayList<>();
                LocalEntry currentEntry = new LocalEntry(localVariables.get(0).getType());
                for (int i = 1; i < localVariables.size(); ++i) {
                    WasmType type = localVariables.get(i).getType();
                    if (currentEntry.type == type) {
                        currentEntry.count++;
                    } else {
                        localEntries.add(currentEntry);
                        currentEntry = new LocalEntry(type);
                    }
                }
                localEntries.add(currentEntry);

                section.writeLEB(localEntries.size());
                for (LocalEntry entry : localEntries) {
                    section.writeLEB(entry.count);
                    section.writeType(entry.type);
                }
            }

            byte[] body = renderFunction(function);
            section.writeLEB(body.length);
            section.writeBytes(body);
        }

        writeSection("code", section.getData());
    }

    private byte[] renderFunction(WasmFunction function) {
        WasmBinaryWriter code = new WasmBinaryWriter();

        WasmBinaryRenderingVisitor visitor = new WasmBinaryRenderingVisitor(code, functionIndexes, importIndexes,
                signatureIndexes);
        for (WasmExpression part : function.getBody()) {
            part.acceptVisitor(visitor);
        }

        return code.getData();
    }

    private void renderData(WasmModule module) {
        if (module.getSegments().isEmpty()) {
            return;
        }

        WasmBinaryWriter section = new WasmBinaryWriter();

        section.writeLEB(module.getSegments().size());
        for (WasmMemorySegment segment : module.getSegments()) {
            section.writeLEB(segment.getOffset());
            section.writeLEB(segment.getLength());
            int chunkSize = 65536;
            for (int i = 0; i < segment.getLength(); i += chunkSize) {
                int next = Math.min(i + chunkSize, segment.getLength());
                section.writeBytes(segment.getData(i, next - i));
            }
        }

        writeSection("data", section.getData());
    }

    private void renderNames(WasmModule module) {
        WasmBinaryWriter section = new WasmBinaryWriter();

        List<WasmFunction> functions = module.getFunctions().values().stream()
                .filter(function -> function.getImportName() == null)
                .collect(Collectors.toList());

        section.writeLEB(functions.size());

        for (WasmFunction function : functions) {
            section.writeAsciiString(function.getName());
            section.writeLEB(0);
        }

        writeSection("name", section.getData());
    }

    static class LocalEntry {
        WasmType type;
        int count = 1;

        public LocalEntry(WasmType type) {
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

    private void writeSection(String id, byte[] data) {
        output.writeAsciiString(id);

        output.writeLEB(data.length);
        output.writeBytes(data);
    }
}
