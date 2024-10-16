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
package org.teavm.backend.wasm.disasm;

import org.teavm.backend.wasm.parser.ImportSectionListener;
import org.teavm.backend.wasm.parser.WasmHollowFunctionType;
import org.teavm.backend.wasm.parser.WasmHollowType;

public class DisassemblyImportSectionListener extends BaseDisassemblyListener implements ImportSectionListener {
    private WasmHollowFunctionType[] functionTypes;
    private String currentModule;
    private String currentName;
    private int functionIndex;
    private int globalIndex;

    public DisassemblyImportSectionListener(DisassemblyWriter writer, NameProvider nameProvider,
            WasmHollowFunctionType[] functionTypes) {
        super(writer, nameProvider);
        this.functionTypes = functionTypes;
    }

    public int functionCount() {
        return functionIndex;
    }

    public int globalCount() {
        return globalIndex;
    }

    @Override
    public void startEntry(String module, String name) {
        currentModule = module;
        currentName = name;
    }

    @Override
    public void function(int typeIndex) {
        writer.address().write("(import \"").write(currentModule).write("\" \"")
                .write(currentName).write("\" ");
        writer.write("(func ");
        writer.startLinkTarget("f" + functionIndex).write("(; " + functionIndex + " ;)");
        var name = nameProvider.function(functionIndex);
        if (name != null) {
            writer.write(" $").write(name);
        }
        writer.endLinkTarget();

        writer.write(" (type ");
        writeTypeRef(typeIndex);
        writer.write(")");
        writer.indent().eol();

        var type = typeIndex < functionTypes.length ? functionTypes[typeIndex] : null;
        if (type != null) {
            for (var i = 0; i < type.parameterTypes.length; ++i) {
                writer.write("(param ");
                writer.startLinkTarget("l" + functionIndex + "." + i).write(" (; " + i + " ;)");
                var paramName = nameProvider.local(functionIndex, i);
                if (paramName != null) {
                    writer.write(" $").write(paramName);
                }
                writer.endLinkTarget();
                writer.write(" ");
                writeType(type.parameterTypes[i]);
                writer.write(")").eol();
            }
            for (var i = 0; i < type.returnTypes.length; ++i) {
                writer.write("(result ");
                writeType(type.returnTypes[i]);
                writer.write(")").eol();
            }
        }
        writer.outdent().write("))").eol();

        functionIndex++;
    }

    @Override
    public void global(WasmHollowType type) {
        writer.address().write("(import \"").write(currentModule).write("\" \"")
                .write(currentName).write("\" ");
        writer.write("(global ");
        writer.startLinkTarget("g" + globalIndex).write("(; " + globalIndex + " ;)");
        var name = nameProvider.global(globalIndex);
        if (name != null) {
            writer.write(" $").write(name);
        }
        writer.endLinkTarget();
        writer.write(" (type ");
        writeType(type);
        writer.write("))").eol();
        ++globalIndex;
    }
}
