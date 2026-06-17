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
import org.teavm.backend.wasm.parser.WasmHollowFunctionType;
import org.teavm.backend.wasm.parser.WasmHollowType;

public class DisassemblyCodeSectionListener extends BaseDisassemblyListener implements CodeSectionListener {
    private int currentFunctionId;
    private int currentFunctionParameterCount;
    private DisassemblyCodeListener codeListener;
    private WasmHollowFunctionType[] functionTypes;
    private int[] functionTypeRefs;

    public DisassemblyCodeSectionListener(DisassemblyWriter writer, NameProvider nameProvider) {
        super(writer, nameProvider);
        codeListener = new DisassemblyCodeListener(writer, nameProvider);
    }

    public void setFunctionTypes(WasmHollowFunctionType[] functionTypes) {
        this.functionTypes = functionTypes;
    }

    public void setFunctionTypeRefs(int[] functionTypeRefs) {
        this.functionTypeRefs = functionTypeRefs;
    }

    @Override
    public boolean functionStart(int index, int size) {
        currentFunctionId = index;
        writer.address().write("(func ");
        writeDeclaration("f" + index, nameProvider.function(index), index);

        var typeRef = functionTypeRefs[index];
        writer.write(" (type ");
        writeTypeRef(typeRef);
        writer.write(")");
        writer.indent().eol();

        var type = typeRef < functionTypes.length ? functionTypes[typeRef] : null;
        if (type != null) {
            currentFunctionParameterCount = type.parameterTypes.length;
            for (var i = 0; i < type.parameterTypes.length; ++i) {
                writer.write("(param ");
                writeDeclaration("l" + index + "." + i, nameProvider.local(index, i), i);
                writer.write(" ");
                writeType(type.parameterTypes[i]);
                writer.write(")").eol();
            }
            for (var i = 0; i < type.returnTypes.length; ++i) {
                writer.write("(result ");
                writeType(type.returnTypes[i]);
                writer.write(")").eol();
            }
        } else {
            currentFunctionParameterCount = 0;
        }

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
            writer.write("(local ");
            var id = i + start + currentFunctionParameterCount;
            writeDeclaration("l" + currentFunctionId + "." + id, nameProvider.local(currentFunctionId, id), id);
            writer.write(" ");
            writeType(type);
            writer.write(")").eol();
        }
    }

    @Override
    public CodeListener code() {
        codeListener.setCurrentFunctionId(currentFunctionId);
        codeListener.reset();
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
