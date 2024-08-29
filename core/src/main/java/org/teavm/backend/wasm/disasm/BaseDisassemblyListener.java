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

import org.teavm.backend.wasm.parser.WasmHollowStorageType;
import org.teavm.backend.wasm.parser.WasmHollowType;

public abstract class BaseDisassemblyListener  {
    protected final DisassemblyWriter writer;
    protected final NameProvider nameProvider;

    public BaseDisassemblyListener(DisassemblyWriter writer, NameProvider nameProvider) {
        this.writer = writer;
        this.nameProvider = nameProvider;
    }

    protected void writeBlockType(WasmHollowType type) {
        if (type != null) {
            writer.write(" ");
            writeType(type);
        }
    }

    protected void writeType(WasmHollowStorageType type) {
        if (type instanceof WasmHollowStorageType.Packed) {
            switch (((WasmHollowStorageType.Packed) type).type) {
                case INT8:
                    writer.write("i8");
                    return;
                case INT16:
                    writer.write("i16");
                    return;
            }
        } else {
            writeType(type.asUnpackedType());
        }
    }

    protected void writeType(WasmHollowType type) {
        if (type != null) {
            if (type instanceof WasmHollowType.Number) {
                switch (((WasmHollowType.Number) type).number) {
                    case INT32:
                        writer.write("i32");
                        return;
                    case INT64:
                        writer.write("i64");
                        return;
                    case FLOAT32:
                        writer.write("f32");
                        return;
                    case FLOAT64:
                        writer.write("f64");
                        return;
                    default:
                        break;
                }
            } else if (type instanceof WasmHollowType.SpecialReference) {
                switch (((WasmHollowType.SpecialReference) type).kind) {
                    case ANY:
                        writer.write("anyref");
                        return;
                    case FUNC:
                        writer.write("funcref");
                        return;
                    case ARRAY:
                        writer.write("arrayref");
                        return;
                    case EXTERN:
                        writer.write("externref");
                        return;
                    case STRUCT:
                        writer.write("structref");
                        return;
                    case I31:
                        writer.write("i31ref");
                        return;
                    default:
                        throw new IllegalArgumentException();
                }
            } else if (type instanceof WasmHollowType.CompositeReference) {
                writer.write("(ref null ");
                writeTypeRef(((WasmHollowType.CompositeReference) type).index);
                writer.write(")");
                return;
            }
        }
        writer.write("unknown");
    }

    protected void writeGlobalRef(int index) {
        writer.startLink("g" + index);
        writeRef(nameProvider.global(index), index);
        writer.endLink();
    }

    protected void writeFunctionRef(int index) {
        writer.startLink("f" + index);
        writeRef(nameProvider.function(index), index);
        writer.endLink();
    }

    protected void writeTypeRef(int index) {
        writer.startLink("t" + index);
        writeRef(nameProvider.type(index), index);
        writer.endLink();
    }

    protected void writeFieldRef(int typeIndex, int index) {
        writer.startLink("f" + typeIndex + "." + index);
        writeRef(nameProvider.field(typeIndex, index), index);
        writer.endLink();
    }

    protected void writeLocalRef(int functionIndex, int index) {
        writer.startLink("l" + functionIndex + "." + index);
        writeRef(nameProvider.local(functionIndex, index), index);
        writer.endLink();
    }

    private void writeRef(String name, int index) {
        if (name == null) {
            writer.write(Integer.toString(index));
        } else {
            writer.write("(; ").write(Integer.toString(index)).write(" ;) $").write(name);
        }
    }
}
