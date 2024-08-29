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

import org.teavm.backend.wasm.parser.CodeListener;
import org.teavm.backend.wasm.parser.CodeSectionListener;
import org.teavm.backend.wasm.parser.WasmHollowType;

public class DisassemblyCodeSectionListener extends BaseDisassemblyListener implements CodeSectionListener {
    private int currentFunctionId;
    private DisassemblyCodeListener codeListener;

    public DisassemblyCodeSectionListener(DisassemblyWriter writer, NameProvider nameProvider) {
        super(writer, nameProvider);
        codeListener = new DisassemblyCodeListener(writer, nameProvider);
    }

    @Override
    public boolean functionStart(int index, int size) {
        currentFunctionId = index;
        writer.address().write("(func ").write("(; " + index + " ;)");
        var name = nameProvider.function(index);
        if (name != null) {
            writer.write(" $").write(name);
        }
        writer.indent().eol();
        return true;
    }

    @Override
    public void localsStart(int count) {
        writer.address().write("(; locals " + count + " ;)").eol();
    }

    @Override
    public void local(int start, int count, WasmHollowType type) {
        writer.address();
        for (int i = 0; i < count; ++i) {
            writer.write("(local (; " + (i + start) + " ;) ");
            var name = nameProvider.local(currentFunctionId, i + start);
            if (name != null) {
                writer.write("$").write(name).write(" ");
            }
            writeType(type);
            writer.write(")").eol();
        }
    }

    @Override
    public CodeListener code() {
        codeListener.setCurrentFunctionId(currentFunctionId);
        return codeListener;
    }

    @Override
    public void functionEnd() {
        writer.outdent().write(")").eol();
    }

    @Override
    public void sectionEnd() {
        writer.outdent().write(")").eol();
    }
}
