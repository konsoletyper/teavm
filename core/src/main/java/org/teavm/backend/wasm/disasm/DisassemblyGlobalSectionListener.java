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

import org.teavm.backend.wasm.parser.CodeListener;
import org.teavm.backend.wasm.parser.GlobalSectionListener;
import org.teavm.backend.wasm.parser.WasmHollowType;

public class DisassemblyGlobalSectionListener extends BaseDisassemblyListener implements GlobalSectionListener {
    private DisassemblyCodeListener codeListener;

    public DisassemblyGlobalSectionListener(DisassemblyWriter writer, NameProvider nameProvider) {
        super(writer, nameProvider);
        codeListener = new DisassemblyCodeListener(writer, nameProvider);
    }

    @Override
    public CodeListener startGlobal(int index, WasmHollowType type, boolean mutable) {
        writer.address().write("(global ");
        writer.startLinkTarget("g" + index).write("(; ").write(String.valueOf(index)).write(" ;)");
        var name = nameProvider.global(index);
        if (name != null) {
            writer.write("$").write(name);
        }
        writer.endLinkTarget().write(" ");
        if (mutable) {
            writer.write("(mut ");
            writeType(type);
            writer.write(")");
        } else {
            writeType(type);
        }
        writer.indent().eol();
        codeListener.reset();
        return codeListener;
    }

    @Override
    public void endGlobal() {
        writer.outdent().write(")").eol();
    }
}
