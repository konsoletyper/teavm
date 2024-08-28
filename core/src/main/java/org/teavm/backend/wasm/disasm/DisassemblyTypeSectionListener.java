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

import org.teavm.backend.wasm.parser.AddressListener;
import org.teavm.backend.wasm.parser.TypeSectionListener;
import org.teavm.backend.wasm.parser.WasmHollowStorageType;
import org.teavm.backend.wasm.parser.WasmHollowType;

public class DisassemblyTypeSectionListener extends BaseDisassemblyListener implements AddressListener,
        TypeSectionListener {
    private boolean currentTypeNeedsClosing;
    private boolean emittingReturn;
    private int currentTypeIndex;
    private int fieldIndex;
    private boolean needsFieldIndex;

    public DisassemblyTypeSectionListener(DisassemblyWriter writer, NameProvider nameProvider) {
        super(writer, nameProvider);
    }

    @Override
    public void startRecType(int count) {
        writer.address(address).write("(rec ").indent().eol();
    }

    @Override
    public void endRecType() {
        writer.outdent().write(")").eol();
    }

    @Override
    public void startType(int index, boolean open, int[] supertypes) {
        currentTypeIndex = index;
        writer.address(address).write("(type (; ").write(String.valueOf(index)).write(" ;) ");
        var name = nameProvider.type(index);
        if (name != null) {
            writer.write("$").write(name).write(" ");
        }
        if (!open || supertypes.length > 0) {
            currentTypeNeedsClosing = true;
            writer.write("(sub ");
            if (!open) {
                writer.write("final");
            }
            for (var supertype : supertypes) {
                writer.write(supertype + " ");
            }
        }
        writer.indent().eol();
    }

    @Override
    public void startArrayType() {
        writer.address(address).write("(array ").indent().eol();
    }

    @Override
    public void endArrayType() {
        writer.outdent().write(")").eol();
    }

    @Override
    public void startStructType(int fieldCount) {
        needsFieldIndex = true;
        writer.address(address).write("(struct ").indent().eol();
    }

    @Override
    public void field(WasmHollowStorageType hollowType, boolean mutable) {
        writer.address(address).write("(field ");
        if (needsFieldIndex) {
            writer.write("(; " + fieldIndex++ + " ;) ");
            var name = nameProvider.field(currentTypeIndex, fieldIndex);
            if (name != null) {
                writer.write("$").write(name);
            }
        }
        if (mutable) {
            writer.write("(mut ");
        }
        writeType(hollowType);
        if (mutable) {
            writer.write(")");
        }
        writer.write(")").eol();
    }

    @Override
    public void endStructType() {
        writer.outdent().write(")").eol();
        fieldIndex = 0;
    }

    @Override
    public void funcType(int paramCount) {
        writer.address(address).write("(func ").indent().eol();
    }

    @Override
    public void funcTypeResults(int returnCount) {
        emittingReturn = true;
    }

    @Override
    public void resultType(WasmHollowType type) {
        writer.address(address).write("(").write(emittingReturn ? "result" : "param").write(" ");
        writeType(type);
        writer.write(")").eol();
    }

    @Override
    public void endFuncType() {
        emittingReturn = false;
        writer.outdent().write(")").eol();
    }

    @Override
    public void endType() {
        writer.outdent();
        if (currentTypeNeedsClosing) {
            writer.write(")");
            currentTypeNeedsClosing = false;
        }
        writer.write(")").eol();
    }
}
