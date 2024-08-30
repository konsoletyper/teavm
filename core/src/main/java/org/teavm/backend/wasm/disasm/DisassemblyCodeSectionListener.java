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
        writer.startLinkTarget("f" + index).write("(; " + index + " ;)");
        var name = nameProvider.function(index);
        if (name != null) {
            writer.write(" $").write(name);
        }
        writer.endLinkTarget();

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
                writer.startLinkTarget("l" + index + "." + i).write(" (; " + i + " ;)");
                var paramName = nameProvider.local(index, i);
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
            writer.startLinkTarget("l" + currentFunctionId + "." + id).write("(; " + id + " ;)");
            var name = nameProvider.local(currentFunctionId, id);
            if (name != null) {
                writer.write(" ").write("$").write(name);
            }
            writer.endLinkTarget().write(" ");
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
