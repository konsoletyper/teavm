/*
 *  Copyright 2026 Alexey Andreev.
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.parser.CodeListener;
import org.teavm.backend.wasm.parser.InstructionListParser;
import org.teavm.backend.wasm.parser.WasmHollowFunctionType;
import org.teavm.backend.wasm.parser.WasmHollowType;
import org.teavm.model.InliningInfo;
import org.teavm.model.TextLocation;

/**
 * <p>Produces disassembly of functions of Wasm model, without rendering them to binary. Useful for tests
 * and diagnostics.</p>
 *
 * <p>Since indices of entities are assigned in order of their first occurrence and don't match indices
 * in rendered module, {@link #toString(WasmFunction)} and {@link #bodyToString(WasmFunction)} only write
 * indices of entities that don't have names.</p>
 *
 * <p>Locations of instructions are written as annotations, similar to ones that {@link Disassembler} writes
 * for TeaVM debug information.</p>
 */
public class ModelDisassembler {
    private final DisassemblyWriter writer;
    private final ModelNameProvider names;
    private TextLocation lastLocation;
    private final List<InliningInfo> inliningStack = new ArrayList<>();
    private final List<String> fileStack = new ArrayList<>();
    private final List<Integer> lineStack = new ArrayList<>();
    private String currentFile;
    private int currentLine;

    public ModelDisassembler(DisassemblyWriter writer) {
        this(writer, true);
    }

    public ModelDisassembler(DisassemblyWriter writer, boolean withLocalNames) {
        this.writer = writer;
        this.names = new ModelNameProvider(withLocalNames);
    }

    public static String toString(WasmFunction function) {
        var buffer = new StringWriter();
        var writer = new DisassemblyTextWriter(new PrintWriter(buffer));
        writer.setWithIndices(false);
        new ModelDisassembler(writer, true).disassemble(function);
        writer.flush();
        return buffer.toString();
    }

    public static String bodyToString(WasmFunction function) {
        var buffer = new StringWriter();
        var writer = new DisassemblyTextWriter(new PrintWriter(buffer));
        writer.setWithIndices(false);
        new ModelDisassembler(writer).disassembleBody(function);
        writer.flush();
        return buffer.toString();
    }

    /**
     * Writes entire function, including declarations of parameters and locals.
     */
    public void disassemble(WasmFunction function) {
        var functionIndex = names.function(function);
        var functionType = function.getType();
        var typeIndex = names.type(functionType);
        var sectionListener = new DisassemblyCodeSectionListener(writer, names);

        var parameterTypes = functionType.getParameterTypes().stream()
                .map(type -> InstructionListParser.type(type, names))
                .toArray(WasmHollowType[]::new);
        var returnTypes = functionType.getReturnTypes().stream()
                .map(type -> InstructionListParser.type(type, names))
                .toArray(WasmHollowType[]::new);
        var functionTypes = new WasmHollowFunctionType[typeIndex + 1];
        functionTypes[typeIndex] = new WasmHollowFunctionType(parameterTypes, returnTypes);
        sectionListener.setFunctionTypes(functionTypes);
        var functionTypeRefs = new int[functionIndex + 1];
        functionTypeRefs[functionIndex] = typeIndex;
        sectionListener.setFunctionTypeRefs(functionTypeRefs);

        sectionListener.functionStart(functionIndex, 0);
        var locals = function.getLocalVariables();
        var parameterCount = parameterTypes.length;
        sectionListener.localsStart(locals.size() - parameterCount);
        for (var i = parameterCount; i < locals.size(); ++i) {
            sectionListener.local(i - parameterCount, 1, InstructionListParser.type(locals.get(i).getType(), names));
        }
        parse(function, sectionListener.code());
        sectionListener.functionEnd();
    }

    /**
     * Writes only instructions of function body.
     */
    public void disassembleBody(WasmFunction function) {
        var listener = new DisassemblyCodeListener(writer, names);
        listener.setCurrentFunctionId(names.function(function));
        parse(function, listener);
    }

    private void parse(WasmFunction function, CodeListener listener) {
        resetLocation();
        var parser = new InstructionListParser(names, listener);
        parser.setLocationListener(this::location);
        parser.parse(function.getBody());
        exitInlining(0);
    }

    private void resetLocation() {
        lastLocation = null;
        inliningStack.clear();
        fileStack.clear();
        lineStack.clear();
        currentFile = null;
        currentLine = -1;
    }

    private void location(TextLocation location) {
        if (Objects.equals(location, lastLocation)) {
            return;
        }
        lastLocation = location;

        var path = location != null ? location.getInliningPath() : new InliningInfo[0];
        var commonPart = 0;
        while (commonPart < path.length && commonPart < inliningStack.size()
                && path[commonPart] == inliningStack.get(commonPart)) {
            ++commonPart;
        }
        exitInlining(commonPart);
        while (commonPart < path.length) {
            var inlining = path[commonPart++];
            inliningStack.add(inlining);
            writeLocation(inlining.getFileName(), inlining.getLine());
            fileStack.add(currentFile);
            lineStack.add(currentLine);
            currentFile = null;
            currentLine = -1;
            var method = inlining.getMethod();
            writer.debugEnterInline(method.getClassName(), method.getName());
        }

        if (location != null) {
            writeLocation(location.getFileName(), location.getLine());
        } else {
            writeLocation(null, -1);
        }
    }

    private void exitInlining(int depth) {
        while (inliningStack.size() > depth) {
            inliningStack.remove(inliningStack.size() - 1);
            currentFile = fileStack.remove(fileStack.size() - 1);
            currentLine = lineStack.remove(lineStack.size() - 1);
            writer.debugExitInline();
        }
    }

    private void writeLocation(String file, int line) {
        if (!Objects.equals(file, currentFile)) {
            currentFile = file;
            currentLine = line;
            writer.debugFileLocation(file, line);
        } else if (line != currentLine) {
            currentLine = line;
            writer.debugLineLocation(line);
        }
    }
}
