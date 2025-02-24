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

import java.util.ArrayList;
import java.util.List;
import org.teavm.backend.wasm.parser.TypeSectionListener;
import org.teavm.backend.wasm.parser.WasmHollowFunctionType;
import org.teavm.backend.wasm.parser.WasmHollowStorageType;
import org.teavm.backend.wasm.parser.WasmHollowType;

public class DisassemblyTypeSectionListener extends BaseDisassemblyListener implements TypeSectionListener {
    private boolean currentTypeNeedsClosing;
    private boolean emittingReturn;
    private int currentTypeIndex;
    private int fieldIndex;
    private boolean needsFieldIndex;
    private List<WasmHollowFunctionType> functionTypes = new ArrayList<>();
    private List<WasmHollowType> parameterTypes = new ArrayList<>();
    private List<WasmHollowType> resultTypes = new ArrayList<>();

    public DisassemblyTypeSectionListener(DisassemblyWriter writer, NameProvider nameProvider) {
        super(writer, nameProvider);
    }

    @Override
    public void startRecType(int count) {
        writer.address().write("(rec ").indent().eol();
    }

    @Override
    public void endRecType() {
        writer.outdent().write(")").eol();
    }

    @Override
    public void startType(int index, boolean open, int[] supertypes) {
        functionTypes.add(null);
        currentTypeIndex = index;
        writer.address().write("(type ");
        writer.startLinkTarget("t" + index).write("(; ").write(String.valueOf(index)).write(" ;) ");
        var name = nameProvider.type(index);
        if (name != null) {
            writer.write("$").write(name);
        }
        writer.endLinkTarget().write(" ");
        if (!open || supertypes.length > 0) {
            currentTypeNeedsClosing = true;
            writer.write("(sub ");
            if (!open) {
                writer.write("final");
            }
            for (var supertype : supertypes) {
                writeTypeRef(supertype);
                writer.write(" ");
            }
        }
        writer.indent().eol();
    }

    @Override
    public void startArrayType() {
        writer.address().write("(array ").indent().eol();
    }

    @Override
    public void endArrayType() {
        writer.outdent().write(")").eol();
    }

    @Override
    public void startStructType(int fieldCount) {
        needsFieldIndex = true;
        writer.address().write("(struct ").indent().eol();
    }

    @Override
    public void field(WasmHollowStorageType hollowType, boolean mutable) {
        writer.address().write("(field ");
        if (needsFieldIndex) {
            var index = fieldIndex++;
            writer.startLinkTarget("f" + currentTypeIndex + "." + index).write("(; " + index + " ;)");
            var name = nameProvider.field(currentTypeIndex, index);
            if (name != null) {
                writer.write(" ").write("$").write(name);
            }
            writer.endLinkTarget().write(" ");
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
        needsFieldIndex = false;
    }

    @Override
    public void funcType(int paramCount) {
        writer.address().write("(func ").indent().eol();
    }

    @Override
    public void funcTypeResults(int returnCount) {
        emittingReturn = true;
    }

    @Override
    public void resultType(WasmHollowType type) {
        if (emittingReturn) {
            resultTypes.add(type);
        } else {
            parameterTypes.add(type);
        }
        writer.address().write("(").write(emittingReturn ? "result" : "param").write(" ");
        writeType(type);
        writer.write(")").eol();
    }

    @Override
    public void endFuncType() {
        var type = new WasmHollowFunctionType(parameterTypes.toArray(new WasmHollowType[0]),
                resultTypes.toArray(new WasmHollowType[0]));
        functionTypes.set(currentTypeIndex, type);
        parameterTypes.clear();
        resultTypes.clear();
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

    public WasmHollowFunctionType[] getFunctionTypes() {
        return functionTypes.toArray(new WasmHollowFunctionType[0]);
    }
}
