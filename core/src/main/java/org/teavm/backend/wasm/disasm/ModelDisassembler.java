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
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.parser.InstructionListParser;
import org.teavm.backend.wasm.parser.WasmHollowFunctionType;
import org.teavm.backend.wasm.parser.WasmHollowType;

/**
 * <p>Produces disassembly of functions of Wasm model, without rendering them to binary. Useful for tests
 * and diagnostics.</p>
 *
 * <p>Since indices of entities are assigned in order of their first occurrence and don't match indices
 * in rendered module, {@link #toString(WasmFunction)} and {@link #bodyToString(WasmFunction)} only write
 * indices of entities that don't have names.</p>
 */
public class ModelDisassembler {
    private final DisassemblyWriter writer;
    private final ModelNameProvider names;

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
        new ModelDisassembler(writer, false).disassemble(function);
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
        new InstructionListParser(names, sectionListener.code()).parse(function.getBody());
        sectionListener.functionEnd();
    }

    /**
     * Writes only instructions of function body.
     */
    public void disassembleBody(WasmFunction function) {
        var listener = new DisassemblyCodeListener(writer, names);
        listener.setCurrentFunctionId(names.function(function));
        new InstructionListParser(names, listener).parse(function.getBody());
    }
}
