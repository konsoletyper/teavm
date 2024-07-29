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
package org.teavm.backend.wasm.render;

import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmCompositeTypeVisitor;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmStorageType;
import org.teavm.backend.wasm.model.WasmStructure;

public class WasmCompositeTypeBinaryRenderer implements WasmCompositeTypeVisitor {
    private WasmModule module;
    private WasmBinaryWriter section;
    private boolean reference;

    public WasmCompositeTypeBinaryRenderer(WasmModule module, WasmBinaryWriter section) {
        this.module = module;
        this.section = section;
    }

    @Override
    public void visit(WasmStructure type) {
        section.writeByte(0x5F);
        section.writeLEB(type.getFields().size());
        for (var fieldType : type.getFields()) {
            writeStorageType(fieldType);
            section.writeLEB(0x01); // mutable
        }
    }

    @Override
    public void visit(WasmArray type) {
        section.writeByte(0x5E);
        writeStorageType(type.getElementType());
        section.writeLEB(0x01); // mutable
    }

    @Override
    public void visit(WasmFunctionType type) {
        section.writeByte(0x60);
        section.writeLEB(type.getParameterTypes().size());
        for (var inputType : type.getParameterTypes()) {
            section.writeType(inputType, module);
        }
        if (type.getReturnType() != null) {
            section.writeByte(1);
            section.writeType(type.getReturnType(), module);
        } else {
            section.writeByte(0);
        }
    }

    private void writeStorageType(WasmStorageType storageType) {
        if (storageType instanceof WasmStorageType.Packed) {
            switch (((WasmStorageType.Packed) storageType).type) {
                case INT8:
                    section.writeByte(0x78);
                    break;
                case INT16:
                    section.writeByte(0x77);
                    break;
            }
        } else {
            section.writeType(storageType.asUnpackedType(), module, true);
        }
    }
}
